package com.haziel.ofmusic.data.repository

import android.content.Context
import android.util.Log
import com.haziel.ofmusic.OfMusicApplication
import com.haziel.ofmusic.data.download.AudioDownloadManager
import com.haziel.ofmusic.data.extractor.CascadingStreamExtractor
import com.haziel.ofmusic.data.extractor.MediaExtractionException
import com.haziel.ofmusic.data.extractor.MediaStreamExtractor
import com.haziel.ofmusic.data.extractor.NewPipeStreamExtractor
import com.haziel.ofmusic.data.extractor.YtDlpStreamExtractor
import com.haziel.ofmusic.data.search.InnerTubeYouTubeSearcher
import com.haziel.ofmusic.data.search.YtDlpYouTubeSearcher
import com.haziel.ofmusic.domain.model.Playlist
import com.haziel.ofmusic.domain.model.Song
import com.haziel.ofmusic.domain.search.YouTubeSearcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MusicRepository(private val context: Context) {

    private val tag = "MusicRepository"

    private val searchCache = object : LinkedHashMap<String, List<Song>>(20, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<Song>>?): Boolean {
            return size > 20
        }
    }
    private val repoScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + Dispatchers.IO
    )
    val lyricsRepository = com.haziel.ofmusic.data.lyrics.LyricsRepository()

    private val ytDlpExtractor = YtDlpStreamExtractor(context)
    private val newPipeExtractor = NewPipeStreamExtractor(context)
    val extractor: MediaStreamExtractor = CascadingStreamExtractor(
        newPipeExtractor = newPipeExtractor,
        fallbackExtractor = ytDlpExtractor
    )

    val youtubeSearcher: YouTubeSearcher = InnerTubeYouTubeSearcher()
    val downloadManager = AudioDownloadManager(context)

    private val prefs = context.getSharedPreferences("ofmusic_repository_prefs", Context.MODE_PRIVATE)

    private val _likedSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val likedSongIds: StateFlow<Set<Long>> = _likedSongIds.asStateFlow()

    private val _downloadedSongMap = MutableStateFlow<Map<Long, String>>(emptyMap())
    val downloadedSongMap: StateFlow<Map<Long, String>> = _downloadedSongMap.asStateFlow()

    private val _downloadingSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val downloadingSongIds: StateFlow<Set<Long>> = _downloadingSongIds.asStateFlow()

    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    init {
        loadPersistedData()
        syncDownloadedSongs()
    }

    private fun loadPersistedData() {
        try {
            val likedSetString = prefs.getStringSet("liked_song_ids", emptySet()) ?: emptySet()
            val likedLongSet = likedSetString.mapNotNull { it.toLongOrNull() }.toSet()
            _likedSongIds.value = likedLongSet

            val songsJsonStr = prefs.getString("saved_songs_json", null)
            if (!songsJsonStr.isNullOrBlank()) {
                val jsonArray = JSONArray(songsJsonStr)
                val loadedSongs = mutableListOf<Song>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)

                    // ─── Extraer youtubeId primero ───
                    val youtubeId = obj.optString("youtubeId", null)
                        .takeIf { it != "null" && it.isNotBlank() }

                    // ─── REGENERAR la portada desde youtubeId (migración automática) ───
                    val artworkUrl = if (!youtubeId.isNullOrBlank() && youtubeId.length == 11) {
                        "https://i.ytimg.com/vi/$youtubeId/hqdefault.jpg"
                    } else {
                        // Sin youtubeId válido: usar la URL guardada (aunque puede estar rota)
                        obj.optString("artworkUrl", "")
                    }

                    loadedSongs.add(
                        Song(
                            id = obj.getLong("id"),
                            title = obj.optString("title", "Desconocido"),
                            artist = obj.optString("artist", "Artista"),
                            album = obj.optString("album", "Sencillo"),
                            artworkUrl = artworkUrl,
                            durationMs = obj.optLong("durationMs", 0L),
                            youtubeId = youtubeId,
                            audioUrl = obj.optString("audioUrl", null).takeIf { it != "null" },
                            videoUrl = obj.optString("videoUrl", null).takeIf { it != "null" },
                            isLiked = obj.optBoolean("isLiked", false),
                            isDownloaded = obj.optBoolean("isDownloaded", false),
                            localPath = obj.optString("localPath", null).takeIf { it != "null" }
                        )
                    )
                }
                _allSongs.value = loadedSongs
            }
            // Cargar playlists
            val playlistsJson = prefs.getString("user_playlists_json", null)
            if (!playlistsJson.isNullOrBlank()) {
                val arr = JSONArray(playlistsJson)
                val loaded = mutableListOf<Playlist>()
                for (i in 0 until arr.length()) {
                    runCatching { Playlist.fromJson(arr.getString(i)) }
                        .getOrNull()?.let { loaded.add(it) }
                }
                _playlists.value = loaded
            }
            // Persistir los cambios para que la migración sea permanente
            persistData()
        } catch (e: Exception) {
            Log.e(tag, "Error al cargar datos guardados: ${e.localizedMessage}")
            }
    }

    fun saveUserProfileLocal(profile: com.haziel.ofmusic.domain.model.UserProfile) {
        try {
            val obj = JSONObject().apply {
                put("name", profile.name)
                put("email", profile.email)
                put("birthDate", profile.birthDate)
                put("country", profile.country)
                put("photoUri", profile.photoUri)
            }
            prefs.edit().putString("user_profile_json", obj.toString()).apply()
        } catch (e: Exception) {
            Log.e(tag, "Error guardando perfil local: ${e.message}")
        }
    }

    fun loadUserProfileLocal(): com.haziel.ofmusic.domain.model.UserProfile? {
        return try {
            val jsonStr = prefs.getString("user_profile_json", null) ?: return null
            val obj = JSONObject(jsonStr)
            com.haziel.ofmusic.domain.model.UserProfile(
                name = obj.optString("name", "Usuario OfMusic"),
                email = obj.optString("email", "usuario@ofmusic.com"),
                birthDate = obj.optString("birthDate", "14 de Agosto, 1998"),
                country = obj.optString("country", "México (MX)"),
                photoUri = obj.optString("photoUri", null).takeIf { it != "null" && it.isNotBlank() }
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun persistData() {
        try {
            val likedSetString = _likedSongIds.value.map { it.toString() }.toSet()
            prefs.edit().putStringSet("liked_song_ids", likedSetString).apply()

            val jsonArray = JSONArray()
            _allSongs.value.forEach { song ->
                val obj = JSONObject().apply {
                    put("id", song.id)
                    put("title", song.title)
                    put("artist", song.artist)
                    put("album", song.album)
                    put("artworkUrl", song.artworkUrl)
                    put("durationMs", song.durationMs)
                    put("youtubeId", song.youtubeId)
                    put("audioUrl", song.audioUrl)
                    put("videoUrl", song.videoUrl)
                    put("isLiked", song.isLiked)
                    put("isDownloaded", song.isDownloaded)
                    put("localPath", song.localPath)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("saved_songs_json", jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e(tag, "Error al guardar datos: ${e.localizedMessage}")
        }
    }

    fun syncDownloadedSongs() {
        val currentMap = _downloadedSongMap.value.toMutableMap()
        _allSongs.value.forEach { song ->
            val localPath = downloadManager.getDownloadedFilePath(song)
            if (localPath != null) {
                currentMap[song.id] = localPath
            }
        }
        _downloadedSongMap.value = currentMap
        updateSongsState()
    }

    private fun updateSongsState() {
        val likedSet = _likedSongIds.value
        val dlMap = _downloadedSongMap.value

        _allSongs.value = _allSongs.value.map { song ->
            val isLiked = song.id in likedSet
            val localPath = dlMap[song.id] ?: downloadManager.getDownloadedFilePath(song)
            val isDownloaded = localPath != null
            song.copy(isLiked = isLiked, isDownloaded = isDownloaded, localPath = localPath)
        }
        persistData()
    }

    fun registerSongsInCache(songs: List<Song>) {
        val existingMap = _allSongs.value.associateBy { it.id }.toMutableMap()
        val likedSet = _likedSongIds.value
        val dlMap = _downloadedSongMap.value

        songs.forEach { song ->
            val isLiked = song.id in likedSet
            val localPath = dlMap[song.id] ?: downloadManager.getDownloadedFilePath(song)
            val isDownloaded = localPath != null
            val fullSong = song.copy(isLiked = isLiked, isDownloaded = isDownloaded, localPath = localPath)
            existingMap[song.id] = fullSong
        }

        _allSongs.value = existingMap.values.toList()
        persistData()
    }

    // ── Playlists ───────────────────────────────────────────────

    private fun persistPlaylists() {
        try {
            val arr = JSONArray()
            _playlists.value.forEach { arr.put(it.toJson()) }
            prefs.edit().putString("user_playlists_json", arr.toString()).apply()
        } catch (e: Exception) {
            Log.e(tag, "Error persistiendo playlists: ${e.message}")
        }
    }

    fun getPlaylist(id: Long): Playlist? = _playlists.value.find { it.id == id }

    fun createPlaylist(
        title: String,
        description: String = "",
        coverUrl: String = "",
        isPublic: Boolean = true,
        downloadOffline: Boolean = false
    ): Playlist {
        val clean = title.trim().ifBlank { "Nueva Playlist" }
        val newPlaylist = Playlist(
            title = clean,
            description = description.trim(),
            coverUrl = coverUrl,
            isPublic = isPublic,
            downloadOffline = downloadOffline
        )
        _playlists.value = _playlists.value + newPlaylist
        persistPlaylists()
        Log.d(tag, "Playlist creada: ${newPlaylist.title} (id=${newPlaylist.id})")
        return newPlaylist
    }

    fun renamePlaylist(id: Long, newTitle: String) {
        val clean = newTitle.trim().ifBlank { return }
        _playlists.value = _playlists.value.map {
            if (it.id == id) it.copy(title = clean, updatedAt = System.currentTimeMillis()) else it
        }
        persistPlaylists()
    }

    fun deletePlaylist(id: Long) {
        _playlists.value = _playlists.value.filterNot { it.id == id }
        persistPlaylists()
    }

    fun updatePlaylistCover(id: Long, coverUrl: String) {
        _playlists.value = _playlists.value.map {
            if (it.id == id) it.copy(coverUrl = coverUrl, updatedAt = System.currentTimeMillis()) else it
        }
        persistPlaylists()
    }

    fun addSongsToPlaylist(playlistId: Long, songs: List<Song>): Int {
        val playlist = _playlists.value.find { it.id == playlistId } ?: return 0
        val existingIds = playlist.songs.map { it.id }.toSet()
        val newSongs = songs.filterNot { it.id in existingIds }
        if (newSongs.isEmpty()) return 0
        val updated = playlist.copy(
            songs = playlist.songs + newSongs,
            coverUrl = playlist.coverUrl.ifBlank { newSongs.first().artworkUrl },
            updatedAt = System.currentTimeMillis()
        )
        _playlists.value = _playlists.value.map { if (it.id == playlistId) updated else it }
        persistPlaylists()
        return newSongs.size
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        _playlists.value = _playlists.value.map {
            if (it.id == playlistId) {
                it.copy(
                    songs = it.songs.filterNot { s -> s.id == songId },
                    updatedAt = System.currentTimeMillis()
                )
            } else it
        }
        persistPlaylists()
    }

    fun reorderPlaylistSongs(playlistId: Long, newOrder: List<Song>) {
        _playlists.value = _playlists.value.map {
            if (it.id == playlistId) it.copy(songs = newOrder, updatedAt = System.currentTimeMillis()) else it
        }
        persistPlaylists()
    }

    suspend fun clearAllUserData() {
        _likedSongIds.value = emptySet()
        _playlists.value = emptyList()
        prefs.edit().remove("liked_songs_json").remove("user_playlists_json").apply()
        deleteAllDownloads()
    }

    suspend fun searchYouTube(query: String): List<Song> = withContext(Dispatchers.IO) {
        val userQuery = query.trim()
        if (userQuery.isBlank()) return@withContext emptyList()

        // ─── Esperar a que yt-dlp esté listo ───
        awaitYtDlpReady()

        val cacheKey = userQuery.lowercase()

        // Cache hit → retorno instantáneo
        searchCache[cacheKey]?.let { cached ->
            Log.d(tag, "✓ Cache hit para búsqueda: $userQuery (${cached.size} resultados)")
            return@withContext cached
        }

        Log.d(tag, "⏳ Ejecutando búsqueda InnerTube para: $userQuery")

        val searchResults = youtubeSearcher.search(userQuery, maxResults = 30)
        val likedSet = _likedSongIds.value
        val dlMap = _downloadedSongMap.value

        val songs = searchResults.map { result ->
            val songId = (result.videoId.hashCode().toLong() and 0x7FFFFFFF) + 100000L
            val isLiked = songId in likedSet
            val localPath = dlMap[songId]
            val isDownloaded = localPath != null

            // Limpiar el título y, si se puede, extraer artista
            val cleanedTitle = com.haziel.ofmusic.data.TitleUtils.cleanYouTubeTitle(result.title)
            val artistSplit = com.haziel.ofmusic.data.TitleUtils.splitArtistFromTitle(cleanedTitle)
            val finalTitle = artistSplit?.second ?: cleanedTitle
            val finalArtist = artistSplit?.first ?: result.channelName

            Song(
                id = songId,
                title = finalTitle,
                artist = finalArtist,
                album = "YouTube Single",
                artworkUrl = buildThumbnailUrl(result.videoId),
                durationMs = result.durationSeconds * 1000L,
                youtubeId = result.videoId,
                audioUrl = null,
                videoUrl = null,
                isLiked = isLiked,
                isDownloaded = isDownloaded,
                localPath = localPath
            )
        }

        if (songs.isNotEmpty()) {
            registerSongsInCache(songs)
        }

        // Guardar en cache
        searchCache[cacheKey] = songs
        Log.d(tag, "✓ Búsqueda completada: $userQuery (${songs.size} resultados, ${searchCache.size} en cache)")

        songs
    }

    /**
     * Construye la URL de la portada más confiable.
     * Siempre usa hqdefault.jpg generada desde el videoId, que existe
     * para el 99.9% de los videos de YouTube.
     */
    private fun buildThumbnailUrl(videoId: String): String {
        return "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
    }

    suspend fun getLyricsForSong(song: Song): List<com.haziel.ofmusic.data.lyrics.LyricLine> {
        if (song.title.isBlank() || song.artist.isBlank()) return emptyList()
        return lyricsRepository.getLyrics(
            artist = song.artist,
            title = song.title,
            durationMs = song.durationMs
        )
    }

    /**
     * Alterna el "Me gusta". Optimistic update: el estado se cambia al
     * instante para que el UI reaccione. Si pasa a liked, se descarga
     * en background sin bloquear.
     */
    suspend fun toggleLike(song: Song): Boolean {
        val currentlyLiked = song.id in _likedSongIds.value
        val newLikedSet = _likedSongIds.value.toMutableSet()

        if (currentlyLiked) {
            newLikedSet.remove(song.id)
            Log.d(tag, "♥ Removido de Me Gusta: ${song.title}")
        } else {
            newLikedSet.add(song.id)
            Log.d(tag, "♥ Añadido a Me Gusta: ${song.title}")
        }

        // Actualización inmediata del estado
        _likedSongIds.value = newLikedSet
        registerSongsInCache(listOf(song.copy(isLiked = !currentlyLiked)))

        return !currentlyLiked
    }

    /**
     * Descarga una canción. Si no está en Me Gusta, la añade.
     * Reporta el estado de descarga en `downloadingSongIds`.
     */
    suspend fun downloadSong(song: Song): String? {
        // Si ya está descargada, no hacer nada
        downloadManager.getDownloadedFilePath(song)?.let { path ->
            val updated = _downloadedSongMap.value + (song.id to path)
            _downloadedSongMap.value = updated
            updateSongsState()
            return path
        }

        // Añadir a Me Gusta automáticamente si no lo está
        if (song.id !in _likedSongIds.value) {
            _likedSongIds.value = _likedSongIds.value + song.id
            registerSongsInCache(listOf(song.copy(isLiked = true)))
        }

        // Marcar como descargando
        _downloadingSongIds.value = _downloadingSongIds.value + song.id

        return try {
            registerSongsInCache(listOf(song))
            val downloadedPath = downloadManager.downloadAudio(song)
            if (downloadedPath != null) {
                val updatedMap = _downloadedSongMap.value.toMutableMap()
                updatedMap[song.id] = downloadedPath
                _downloadedSongMap.value = updatedMap
                updateSongsState()
            }
            downloadedPath
        } finally {
            _downloadingSongIds.value = _downloadingSongIds.value - song.id
        }
    }

    /**
     * Elimina la descarga de una canción. Actualiza los estados y persiste.
     */
    suspend fun deleteDownload(song: Song): Boolean {
        val deleted = downloadManager.deleteDownload(song)
        if (deleted || downloadManager.getDownloadedFilePath(song) == null) {
            // Actualizar el map
            val newMap = _downloadedSongMap.value.toMutableMap()
            newMap.remove(song.id)
            _downloadedSongMap.value = newMap

            // Actualizar el Song en cache (isDownloaded=false, localPath=null)
            val existingMap = _allSongs.value.associateBy { it.id }.toMutableMap()
            existingMap[song.id]?.let { existing ->
                existingMap[song.id] = existing.copy(
                    isDownloaded = false,
                    localPath = null
                )
            }
            _allSongs.value = existingMap.values.toList()

            persistData()
            Log.d(tag, "🗑 Descarga eliminada: ${song.title}")
            return true
        }
        return false
    }

    /**
     * Elimina todas las canciones descargadas del dispositivo.
     */
    suspend fun deleteAllDownloads(): Boolean = withContext(Dispatchers.IO) {
        try {
            val downloadedSongs = _allSongs.value.filter { it.isDownloaded }

            // 1. Borrar cada archivo físico
            downloadedSongs.forEach { song ->
                downloadManager.deleteDownload(song)
            }

            // 2. Limpiar el mapa de descargas
            _downloadedSongMap.value = emptyMap()

            // 3. Actualizar las canciones para que ya no salgan como descargadas
            _allSongs.value = _allSongs.value.map { song ->
                song.copy(isDownloaded = false, localPath = null)
            }

            // 4. Guardar los cambios
            persistData()

            Log.d(tag, "🗑 Todas las descargas han sido eliminadas")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error eliminando todas las descargas: ${e.message}", e)
            false
        }
    }

    suspend fun getAudioUrlForSong(song: Song): String {
        // ─── Esperar a que yt-dlp esté listo ───
        awaitYtDlpReady()
        val local = downloadManager.getDownloadedFilePath(song)
        if (local != null) {
            Log.d(tag, "Usando audio local descargado: $local")
            return local
        }

        val youtubeId = song.youtubeId
        if (!youtubeId.isNullOrBlank()) {
            try {
                val extracted = extractor.getAudioUrl("https://www.youtube.com/watch?v=$youtubeId")
                if (extracted.isNotBlank() && extracted.startsWith("http")) {
                    Log.d(tag, "Usando stream cascada para $youtubeId")
                    return extracted
                }
            } catch (e: MediaExtractionException) {
                Log.e(tag, "Error extrayendo stream audio yt-dlp: ${e.message}")
            }
        }

        if (!song.audioUrl.isNullOrBlank() && song.audioUrl.contains("googlevideo.com")) {
            return song.audioUrl
        }

        return ""
    }

    suspend fun getVideoStreamUrlForSong(song: Song): String {
        // ─── Esperar a que yt-dlp esté listo ───
        awaitYtDlpReady()
        val youtubeId = song.youtubeId
        if (!youtubeId.isNullOrBlank()) {
            try {
                val extracted = extractor.getVideoUrl("https://www.youtube.com/watch?v=$youtubeId")
                if (extracted.isNotBlank()) return extracted
            } catch (e: MediaExtractionException) {
                Log.e(tag, "Error extrayendo stream video yt-dlp: ${e.message}")
            }
        }
        if (!song.videoUrl.isNullOrBlank() && song.videoUrl.contains("googlevideo.com")) {
            return song.videoUrl
        }
        return ""
    }
    /**
     * Pre-descarga las URLs de audio y video para una canción, sin
     * reproducirla. Se usa para calentar el cache de la siguiente canción.
     */
    suspend fun prefetchSong(song: Song) {
        val youtubeId = song.youtubeId ?: return
        val url = "https://www.youtube.com/watch?v=$youtubeId"
        try {
            extractor.getAudioUrl(url)
            Log.d(tag, "✓ Prefetched audio: ${song.title}")
        } catch (e: Exception) {
            Log.w(tag, "Prefetch audio falló: ${e.message}")
        }
        try {
            extractor.getVideoUrl(url)
            Log.d(tag, "✓ Prefetched video: ${song.title}")
        } catch (e: Exception) {
            // Ya no es un warning grave, algunos videos no tienen video-only
            Log.d(tag, "Prefetch video no disponible para ${song.title}")
        }
    }

    /**
     * Repara una canción automáticamente:
     * 1. Verifica que el archivo local exista y sea válido (si está descargada).
     * 2. Regenera la URL de la portada desde el youtubeId.
     * 3. Limpia el cache de URL de video y audio.
     *
     * Todo automático, sin intervención del usuario.
     */
    suspend fun repairSong(song: Song): Boolean {
        Log.d(tag, "🔧 Reparando: ${song.title}")
        var repaired = false

        val youtubeId = song.youtubeId
        if (youtubeId.isNullOrBlank()) {
            Log.w(tag, "  ✗ Sin youtubeId, no se puede reparar")
            return false
        }

        // ─── 1. Verificar archivo local ───
        if (song.isDownloaded) {
            val localPath = downloadManager.getDownloadedFilePath(song)
            val file = localPath?.let { File(it) }
            val fileIsValid = file != null && file.exists() && file.length() > 1024

            if (!fileIsValid) {
                Log.d(tag, "  ⚠ Archivo local inválido, borrando entrada y re-descargando")
                // Borrar entrada de descarga
                val newMap = _downloadedSongMap.value.toMutableMap()
                newMap.remove(song.id)
                _downloadedSongMap.value = newMap
                // Re-descargar en background
                try {
                    downloadSong(song)
                    repaired = true
                    Log.d(tag, "  ✓ Re-descarga completada")
                } catch (e: Exception) {
                    Log.e(tag, "  ✗ Falló re-descarga: ${e.message}")
                }
            } else {
                Log.d(tag, "  ✓ Archivo local OK (${file.length()} bytes)")
            }
        }

        // ─── 2. Regenerar portada ───
        val newArtworkUrl = "https://i.ytimg.com/vi/$youtubeId/hqdefault.jpg"
        Log.d(tag, "  → Portada regenerada: $newArtworkUrl")

        // ─── 3. Limpiar cache de URLs de video y audio ───
        try {
            val urlCachePrefs = context.getSharedPreferences(
                "ofmusic_url_cache",
                Context.MODE_PRIVATE
            )
            urlCachePrefs.edit()
                .remove("video_$youtubeId")
                .remove("audio_$youtubeId")
                .apply()
            Log.d(tag, "  ✓ Cache de URLs limpiado")
        } catch (e: Exception) {
            Log.w(tag, "  ⚠ No se pudo limpiar cache: ${e.message}")
        }

        // ─── 4. Actualizar la canción en memoria ───
        val existingMap = _allSongs.value.associateBy { it.id }.toMutableMap()
        existingMap[song.id]?.let { existing ->
            existingMap[song.id] = existing.copy(
                artworkUrl = newArtworkUrl,
                videoUrl = null,  // forzar re-extracción
                audioUrl = null   // forzar re-extracción
            )
            Log.d(tag, "  ✓ Canción actualizada en memoria")
        }
        _allSongs.value = existingMap.values.toList()

        // Persistir cambios
        persistData()

        Log.d(tag, if (repaired || true) "✅ Reparación completada" else "⚠ Sin cambios")
        return true
    }

    /**
     * Espera a que yt-dlp termine de inicializar (máximo 30 segundos).
     * Si no está listo en ese tiempo, continúa de todas formas.
     */
    private suspend fun awaitYtDlpReady() {
        try {
            val app = context.applicationContext as? OfMusicApplication
            kotlinx.coroutines.withTimeoutOrNull(30_000L) {
                app?.awaitYtDlpReady()
            }
        } catch (e: Exception) {
            Log.w(tag, "Timeout esperando yt-dlp: ${e.message}")
        }
    }

}