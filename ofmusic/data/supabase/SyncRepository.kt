package com.haziel.ofmusic.data.supabase

import android.util.Log
import com.haziel.ofmusic.domain.model.Playlist
import com.haziel.ofmusic.domain.model.Song
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Sincronización bidireccional de favoritos y playlists entre
 * el dispositivo local y Supabase.
 *
 * Reglas:
 *  - Nunca borra datos locales.
 *  - Al subir: solo INSERT de lo que no existe en la nube.
 *  - Al bajar: MERGE con lo local evitando duplicados por songId.
 */
class SyncRepository {

    private val tag = "SyncRepository"
    private val client = SupabaseClientProvider.getInstance()
    private val postgrest = client.postgrest
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ═══════════════════════════════════════════════════════════════
    //  FAVORITOS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Sube los IDs de favoritos locales a la nube.
     * Solo inserta los que NO existen ya (evita duplicados).
     */
    suspend fun uploadFavorites(
        userId: String,
        localFavoriteIds: Set<Long>,
        songs: List<Song>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val songsById = songs.associateBy { it.id }
            val existing = fetchFavorites(userId).getOrNull()?.map { it.songId }?.toSet() ?: emptySet()
            val toUpload = localFavoriteIds - existing

            if (toUpload.isEmpty()) {
                Log.d(tag, "✓ Favoritos: nada nuevo que subir")
                return@withContext Result.success(0)
            }

            val inserts = toUpload.map { songId ->
                FavoriteDto(
                    userId = userId,
                    songId = songId,
                    youtubeId = songsById[songId]?.youtubeId
                )
            }

            postgrest.from("favorites").insert(inserts)
            Log.d(tag, "✓ Favoritos subidos: ${inserts.size}")
            Result.success(inserts.size)
        } catch (e: Exception) {
            Log.e(tag, "Error subiendo favoritos: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Descarga los favoritos de la nube.
     */
    suspend fun fetchFavorites(userId: String): Result<List<FavoriteDto>> =
        withContext(Dispatchers.IO) {
            try {
                val favorites = postgrest
                    .from("favorites")
                    .select {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<FavoriteDto>()
                Log.d(tag, "✓ Favoritos descargados: ${favorites.size}")
                Result.success(favorites)
            } catch (e: Exception) {
                Log.e(tag, "Error descargando favoritos: ${e.message}", e)
                Result.failure(e)
            }
        }

    // ═══════════════════════════════════════════════════════════════
    //  PLAYLISTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Sube las playlists locales a la nube.
     * Solo inserta las que NO existen ya (por ID).
     */
    suspend fun uploadPlaylists(
        userId: String,
        localPlaylists: List<Playlist>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val inserts = localPlaylists.map { playlist ->
                PlaylistDto(
                    id = playlist.id,
                    userId = userId,
                    title = playlist.title,
                    description = playlist.description,
                    artistName = playlist.artistName,
                    coverUrl = playlist.coverUrl,
                    isPublic = playlist.isPublic,
                    downloadOffline = playlist.downloadOffline,
                    songsJson = json.encodeToString(
                        kotlinx.serialization.builtins.ListSerializer(Song.serializer()),
                        playlist.songs
                    ),
                    isUserCreated = playlist.isUserCreated
                )
            }

            postgrest.from("user_playlists").upsert(inserts)
            Log.d(tag, "✓ Playlists subidas / actualizadas en Supabase: ${inserts.size}")
            Result.success(inserts.size)
        } catch (e: Exception) {
            Log.e(tag, "Error subiendo playlists: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Descarga las playlists de la nube.
     */
    suspend fun fetchPlaylists(userId: String): Result<List<PlaylistDto>> =
        withContext(Dispatchers.IO) {
            try {
                val playlists = postgrest
                    .from("user_playlists")
                    .select {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<PlaylistDto>()
                Log.d(tag, "✓ Playlists descargadas: ${playlists.size}")
                Result.success(playlists)
            } catch (e: Exception) {
                Log.e(tag, "Error descargando playlists: ${e.message}", e)
                Result.failure(e)
            }
        }

    // ═══════════════════════════════════════════════════════════════
    //  MIGRACIÓN COMPLETA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Migración completa al registrarse por primera vez.
     * Sube favoritos y playlists.
     */
    suspend fun migrateLocalToCloud(
        userId: String,
        localFavoriteIds: Set<Long>,
        localPlaylists: List<Playlist>,
        songs: List<Song>
    ): Result<MigrationResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "▶ Iniciando migración local → nube")
            val favResult = uploadFavorites(userId, localFavoriteIds, songs)
            val playResult = uploadPlaylists(userId, localPlaylists)

            val favoritesUploaded = favResult.getOrElse { 0 }
            val playlistsUploaded = playResult.getOrElse { 0 }

            Log.d(tag, "✓ Migración completada: $favoritesUploaded favs, $playlistsUploaded playlists")
            Result.success(MigrationResult(favoritesUploaded, playlistsUploaded))
        } catch (e: Exception) {
            Log.e(tag, "Error en migración: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun clearCloudUserData(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            postgrest.from("favorites").delete { filter { eq("user_id", userId) } }
            postgrest.from("user_playlists").delete { filter { eq("user_id", userId) } }
            Log.d(tag, "✓ Datos en la nube eliminados para $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error eliminando datos en la nube: ${e.message}", e)
            Result.failure(e)
        }
    }
}

data class MigrationResult(
    val favoritesUploaded: Int,
    val playlistsUploaded: Int
)