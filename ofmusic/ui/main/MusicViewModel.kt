package com.haziel.ofmusic.ui.main

import android.app.Application
import android.util.Log
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.haziel.ofmusic.ConnectionState
import com.haziel.ofmusic.data.repository.MusicRepository
import com.haziel.ofmusic.data.update.ApkDownloader
import com.haziel.ofmusic.data.update.ApkInstaller
import com.haziel.ofmusic.data.update.UpdateChecker
import com.haziel.ofmusic.domain.model.Playlist
import com.haziel.ofmusic.domain.model.Song
import com.haziel.ofmusic.playback.PlaybackConnection
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.haziel.ofmusic.data.preferences.DisplayPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import com.haziel.ofmusic.data.supabase.AuthRepository
import com.haziel.ofmusic.data.supabase.ProfileDto
import com.haziel.ofmusic.data.supabase.SyncRepository
import com.haziel.ofmusic.domain.tier.TierGate
import com.haziel.ofmusic.domain.tier.TierGateResult
import com.haziel.ofmusic.domain.tier.TierLimits
import com.haziel.ofmusic.domain.tier.UpgradeReason
import com.haziel.ofmusic.domain.tier.UserTier
import com.haziel.ofmusic.observeConnectivityAsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    val repository = MusicRepository(application)

    private val updateChecker = UpdateChecker(application)
    val authRepository = AuthRepository()
    private val syncRepository = SyncRepository()
    private val storageRepository = com.haziel.ofmusic.data.supabase.StorageRepository(application)
    private val apkDownloader = ApkDownloader(application)
    private val displayPreferences = DisplayPreferences(application)

    // ── Canal de eventos one-shot (Toasts, etc.) ──
    private val _events = Channel<MusicUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    val playbackConnection = PlaybackConnection(
        context = application,
        scope = viewModelScope
    ) { song ->
        // Resolver URL bajo demanda
        repository.getAudioUrlForSong(song)
    }

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    var conexion by mutableStateOf(false)
        private set

    init {
        val appContext = getApplication<Application>().applicationContext
        conexion = checkInitialConnectivity(appContext)

        viewModelScope.launch(Dispatchers.IO) {
            appContext.observeConnectivityAsFlow().collect { state ->
                conexion = state == ConnectionState.Available
            }
        }
        // ── Verificar si hay sesión activa ──
        viewModelScope.launch {
            checkAuthStatus()
        }

        // Cargar preferencia de refresh rate, tema y descarga automática de favoritos
        _uiState.value = _uiState.value.copy(
            refreshRate = displayPreferences.getRefreshRate(),
            selectedTheme = displayPreferences.getSelectedTheme(),
            autoDownloadFavorites = displayPreferences.getAutoDownloadFavorites()
        )

        playbackConnection.connect()

        // ── Repositorio ──
        viewModelScope.launch {
            repository.allSongs.collectLatest { songs ->
                _uiState.value = _uiState.value.copy(songs = songs)
            }
        }
        viewModelScope.launch {
            repository.playlists.collectLatest { playlists ->
                _uiState.value = _uiState.value.copy(playlists = playlists)
            }
        }
        viewModelScope.launch {
            repository.likedSongIds.collectLatest { likedIds ->
                _uiState.value = _uiState.value.copy(likedSongIds = likedIds)
            }
        }
        viewModelScope.launch {
            repository.downloadedSongMap.collectLatest { map ->
                _uiState.value = _uiState.value.copy(downloadedSongMap = map)
            }
        }
        viewModelScope.launch {
            repository.downloadingSongIds.collectLatest { ids ->
                _uiState.value = _uiState.value.copy(downloadingSongIds = ids)
            }
        }

        // ── Reproducción (desde el servicio) ──
        viewModelScope.launch {
            playbackConnection.currentSong.collectLatest { song ->
                _uiState.value = _uiState.value.copy(currentSong = song)
                //song?.let { loadVideoStreamUrl(it) }
            }
        }

        // ── Prefetch de la siguiente canción ──
        viewModelScope.launch {
            playbackConnection.currentSong.collectLatest { song ->
                if (song == null) return@collectLatest
                delay(500) // Esperar a que la canción actual empiece a sonar

                val queue = _uiState.value.queue
                val currentIndex = queue.indexOfFirst { it.id == song.id }

                if (currentIndex >= 0) {
                    // Lanzar prefetch para las siguientes 3 canciones en paralelo
                    for (offset in 1..3) {
                        val nextIndex = currentIndex + offset
                        if (nextIndex < queue.size) {
                            val nextSong = queue[nextIndex]
                            launch {
                                repository.prefetchSong(nextSong)
                            }
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            playbackConnection.isPlaying.collectLatest { playing ->
                _uiState.value = _uiState.value.copy(isPlaying = playing)
            }
        }
        viewModelScope.launch {
            playbackConnection.currentPositionMs.collectLatest { pos ->
                _uiState.value = _uiState.value.copy(currentPositionMs = pos)
            }
        }
        viewModelScope.launch {
            playbackConnection.durationMs.collectLatest { dur ->
                _uiState.value = _uiState.value.copy(durationMs = dur)
            }
        }
        viewModelScope.launch {
            playbackConnection.queue.collectLatest { q ->
                _uiState.value = _uiState.value.copy(queue = q)
            }
        }
        viewModelScope.launch {
            playbackConnection.queueIndex.collectLatest { idx ->
                _uiState.value = _uiState.value.copy(queueIndex = idx)
            }
        }
        viewModelScope.launch {
            playbackConnection.repeatMode.collectLatest { r ->
                _uiState.value = _uiState.value.copy(repeatMode = r)
            }
        }
        viewModelScope.launch {
            playbackConnection.shuffleEnabled.collectLatest { s ->
                _uiState.value = _uiState.value.copy(isShuffleEnabled = s)
            }
        }

        // ── Letras al cambiar de canción ──
        viewModelScope.launch {
            playbackConnection.currentSong.collectLatest { song ->
                if (song != null) {
                    // 👇 Gate de letras
                    if (TierGate.canUseSyncedLyrics(currentTier()) is TierGateResult.Blocked) {
                        _uiState.value = _uiState.value.copy(
                            currentLyrics = emptyList(),
                            isLoadingLyrics = false
                        )
                        return@collectLatest
                    }

                    _uiState.value = _uiState.value.copy(
                        currentLyrics = emptyList(),
                        isLoadingLyrics = true
                    )
                    val lyrics = repository.getLyricsForSong(song)
                    _uiState.value = _uiState.value.copy(
                        currentLyrics = lyrics,
                        isLoadingLyrics = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        currentLyrics = emptyList(),
                        isLoadingLyrics = false
                    )
                }
            }
        }

        // ── Verificar actualización al arrancar ──
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000) // esperar a que la UI se asiente
            checkForUpdate()
        }

        viewModelScope.launch {
            _uiState
                .map { it.pendingUpgrade }
                .distinctUntilChanged()
                .collect { reason ->
                    if (reason != null) {
                        _events.send(MusicUiEvent.ShowUpgrade(reason))
                    }
                }
        }

        viewModelScope.launch {
            playbackConnection.isBuffering.collectLatest { buffering ->
                _uiState.value = _uiState.value.copy(isBuffering = buffering)
            }
        }

    }

    fun setRefreshRate(hz: Int) {
        displayPreferences.setRefreshRate(hz)
        _uiState.value = _uiState.value.copy(refreshRate = hz)
    }

    fun setAppTheme(theme: com.haziel.ofmusic.data.preferences.AppThemeOption) {
        displayPreferences.setSelectedTheme(theme)
        _uiState.value = _uiState.value.copy(selectedTheme = theme)
        viewModelScope.launch {
            _events.send(MusicUiEvent.ShowToast("✓ Tema ${theme.title} aplicado"))
        }
    }

    // ── Reproducción ──

    fun playSongSingle(song: Song) {
        playSongsFromList(listOf(song), 0)
    }

    fun playSongsFromList(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        playbackConnection.playQueue(songs, startIndex)
    }

    fun generateQueueByGenreAndTaste(genreName: String) {
        viewModelScope.launch {
            _events.send(MusicUiEvent.ShowToast("⚡ Generando cola para $genreName basada en tus Me Gusta..."))

            val allSongs = repository.allSongs.value
            val likedIds = repository.likedSongIds.value
            val likedSongs = allSongs.filter { it.id in likedIds }

            val cleanGenre = genreName.lowercase()
            val keywords = when {
                cleanGenre.contains("urbano") || cleanGenre.contains("trap") || cleanGenre.contains("reggaeton") ->
                    listOf("trap", "reggaeton", "urbano", "bad bunny", "karol g", "feid", "rauw", "ozuna", "daddy", "anuel", "eladio", "myke")
                cleanGenre.contains("electrónica") || cleanGenre.contains("edm") || cleanGenre.contains("dance") ->
                    listOf("electronic", "edm", "dance", "remix", "tiesto", "guetta", "avicii", "marshmello", "martin garrix", "dj", "club")
                cleanGenre.contains("pop") ->
                    listOf("pop", "taylor", "duan", "the weeknd", "shakira", "rosalia", "ariana", "harry", "snoop", "billie", "swift")
                cleanGenre.contains("rock") || cleanGenre.contains("alternativo") ->
                    listOf("rock", "metal", "indie", "queen", "nirvana", "coldplay", "imagine dragons", "linkin", "arctic", "maná", "soda")
                cleanGenre.contains("éxitos") || cleanGenre.contains("global") || cleanGenre.contains("top") ->
                    listOf("top", "hit", "viral", "2025", "global", "trending", "éxitos")
                else -> listOf(cleanGenre)
            }

            val matchingLiked = likedSongs.filter { song ->
                val titleArtist = "${song.title} ${song.artist} ${song.album}".lowercase()
                keywords.any { titleArtist.contains(it) }
            }.shuffled()

            val searchQuery = "$genreName hits oficiales"
            val fetchedGenreSongs = try {
                repository.searchYouTube(searchQuery)
            } catch (e: Exception) {
                emptyList()
            }

            val combinedQueue = mutableListOf<Song>()
            val addedIds = mutableSetOf<Long>()

            matchingLiked.forEach { song ->
                if (addedIds.add(song.id)) combinedQueue.add(song)
            }

            if (combinedQueue.size < 5) {
                likedSongs.shuffled().take(10).forEach { song ->
                    if (addedIds.add(song.id)) combinedQueue.add(song)
                }
            }

            fetchedGenreSongs.forEach { song ->
                if (addedIds.add(song.id)) combinedQueue.add(song)
            }

            if (combinedQueue.isNotEmpty()) {
                playSongsFromList(combinedQueue, 0)
                _events.send(MusicUiEvent.ShowToast("▶ Reproduciendo cola de $genreName (${combinedQueue.size} canciones)"))
            } else {
                _events.send(MusicUiEvent.ShowToast("⚠ No se encontraron canciones para $genreName"))
            }
        }
    }

    fun generateLikedBasedRadio() {
        viewModelScope.launch {
            _events.send(MusicUiEvent.ShowToast("⚡ Generando Radio basada en tus Me Gusta..."))

            val allSongs = repository.allSongs.value
            val likedIds = repository.likedSongIds.value
            val likedSongs = allSongs.filter { it.id in likedIds }

            val likedArtists = likedSongs.map { it.artist }.filter { it.isNotBlank() && it != "Artista" && it != "Desconocido" }.distinct()

            val combinedQueue = mutableListOf<Song>()
            val addedIds = mutableSetOf<Long>()

            likedSongs.shuffled().forEach { song ->
                if (addedIds.add(song.id)) combinedQueue.add(song)
            }

            if (likedArtists.isNotEmpty()) {
                val targetArtist = likedArtists.random()
                val officialArtistSongs = try {
                    repository.searchYouTube("$targetArtist canciones oficiales")
                } catch (e: Exception) {
                    emptyList()
                }
                officialArtistSongs.forEach { song ->
                    if (addedIds.add(song.id)) combinedQueue.add(song)
                }
            } else {
                val topHits = try {
                    repository.searchYouTube("Top Exitos Artistas Oficiales 2025")
                } catch (e: Exception) {
                    emptyList()
                }
                topHits.forEach { song ->
                    if (addedIds.add(song.id)) combinedQueue.add(song)
                }
            }

            if (combinedQueue.isNotEmpty()) {
                playSongsFromList(combinedQueue, 0)
                _events.send(MusicUiEvent.ShowToast("▶ Emisora de Radio iniciada (${combinedQueue.size} canciones)"))
            } else {
                _events.send(MusicUiEvent.ShowToast("⚠ No se encontraron canciones oficiales para la Radio"))
            }
        }
    }

    fun playPlaylist(playlist: Playlist, startIndex: Int = 0) {
        playSongsFromList(playlist.songs, startIndex)
    }

    fun addToQueue(song: Song) {
        playbackConnection.addToQueue(listOf(song))
    }

    fun togglePlayPause() = playbackConnection.togglePlayPause()
    fun playNext() = playbackConnection.next()
    fun playPrevious() = playbackConnection.previous()
    fun seekTo(positionMs: Long) = playbackConnection.seekTo(positionMs)
    fun seekToQueueIndex(index: Int) = playbackConnection.seekToQueueIndex(index)
    fun toggleShuffle() = playbackConnection.toggleShuffle()
    fun toggleRepeat() = playbackConnection.toggleRepeat()

    // ── Like / Descarga ──

    fun toggleLike(song: Song) {
        val isCurrentlyLiked = _uiState.value.likedSongIds.contains(song.id)

        if (isCurrentlyLiked) {
            // Quitar like
            viewModelScope.launch { repository.toggleLike(song) }
            return
        }

        // Dar like
        val currentDownloads = repository.downloadedSongMap.value.size
        handleGate(TierGate.canDownload(currentTier(), currentDownloads)) {
            viewModelScope.launch {
                repository.toggleLike(song)
                if (_uiState.value.autoDownloadFavorites && !song.isDownloaded && !repository.downloadedSongMap.value.containsKey(song.id)) {
                    downloadSong(song)
                }
            }
        }
    }

    fun setAutoDownloadFavorites(enabled: Boolean) {
        displayPreferences.setAutoDownloadFavorites(enabled)
        _uiState.value = _uiState.value.copy(autoDownloadFavorites = enabled)
    }

    fun downloadSong(song: Song) {
        if (repository.downloadedSongMap.value.containsKey(song.id) || song.isDownloaded) {
            return
        }
        val currentDownloads = repository.downloadedSongMap.value.size
        handleGate(TierGate.canDownload(currentTier(), currentDownloads)) {
            viewModelScope.launch { repository.downloadSong(song) }
        }
    }

    fun downloadPlaylistSequentially(playlistId: Long, songs: List<Song>) {
        viewModelScope.launch {
            val downloadedMap = repository.downloadedSongMap.value
            val toDownload = songs.filterNot { song ->
                downloadedMap.containsKey(song.id) || song.isDownloaded
            }

            if (toDownload.isEmpty()) {
                _events.send(MusicUiEvent.ShowToast("Todas las canciones de la playlist ya están descargadas"))
                return@launch
            }

            val total = toDownload.size
            _uiState.value = _uiState.value.copy(playlistDownloadProgress = Pair(playlistId, 0))

            for ((index, song) in toDownload.withIndex()) {
                val progress = ((index.toFloat() / total.toFloat()) * 100).toInt()
                _uiState.value = _uiState.value.copy(playlistDownloadProgress = Pair(playlistId, progress))

                try {
                    repository.downloadSong(song)
                } catch (e: Exception) {
                    Log.e("MusicViewModel", "Error descargando ${song.title}: ${e.message}")
                }
            }

            _uiState.value = _uiState.value.copy(playlistDownloadProgress = Pair(playlistId, 100))
            delay(1200)
            _uiState.value = _uiState.value.copy(playlistDownloadProgress = null)
            _events.send(MusicUiEvent.ShowToast("✅ Descarga de playlist completada"))
        }
    }

    fun deleteDownload(song: Song) {
        viewModelScope.launch {
            repository.deleteDownload(song)
        }
    }

    // ── Búsqueda con debounce + cache ──

    fun search(query: String) {
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                searchQuery = "",
                searchResults = emptyList(),
                isSearching = false
            )
            return
        }

        _uiState.value = _uiState.value.copy(searchQuery = query, isSearching = true)

        searchJob = viewModelScope.launch {
            // ─── Debounce adaptativo ───
            // Menos de 4 letras: 2000 ms (el usuario probablemente sigue escribiendo)
            // 4 letras o más: 500 ms (respuesta rápida cuando ya sabe qué busca)
            val delayMs = if (query.length < 4) 2000L else 500L
            delay(delayMs)

            // Verificar que el usuario no siguió escribiendo
            if (_uiState.value.searchQuery != query) return@launch

            val results = repository.searchYouTube(query)

            // Volver a verificar (por si cambió durante la búsqueda)
            if (_uiState.value.searchQuery != query) return@launch

            _uiState.value = _uiState.value.copy(
                searchResults = results,
                isSearching = false
            )
        }
    }
    fun simulateTierPurchase(newTier: UserTier) {
        val currentProfile = _uiState.value.authProfile ?: return

        // 1. Actualizar el estado local
        val updatedProfile = currentProfile.copy(tier = newTier.rawValue)
        _uiState.value = _uiState.value.copy(authProfile = updatedProfile)

        // 2. Mostrar confirmación
        viewModelScope.launch {
            _events.send(MusicUiEvent.ShowToast("¡Plan cambiado a ${newTier.displayName}! (Simulado)"))
        }

        // 3. (Opcional) Intentar actualizar en Supabase.
        // NOTA: Esto fallará con un error 403 por el trigger `prevent_tier_self_update`.
        // En producción, el webhook de Stripe hará esto con `service_role`.
        viewModelScope.launch {
            try {
                // Asumiendo que tienes un AuthRepository que puede actualizar el tier (aunque sea bloqueado)
                // authRepository.updateTier(newTier.rawValue)
            } catch (e: Exception) {
                Log.w("MusicViewModel", "Simulación: No se pudo actualizar en Supabase (esperado)")
            }
        }
    }

    fun setVolume(volume: Float) {
        _uiState.value = _uiState.value.copy(volume = volume)
        playbackConnection.setVolume(volume)
    }


    @OptIn(ExperimentalSharedTransitionApi::class)
    @Composable
    fun TestSharedTransition() {
        SharedTransitionLayout {
            // ...
        }
    }

    // ── Video ──

    fun loadVideoStreamUrl(song: Song, forceRefresh: Boolean = false) {
        // 👇 Gate primero
        when (val gate = TierGate.canUseSyncedVideo(currentTier())) {
            is TierGateResult.Blocked -> {
                _uiState.value = _uiState.value.copy(pendingUpgrade = gate.reason)
                return
            }
            is TierGateResult.Allowed -> Unit
        }

        val currentMap = _uiState.value.videoUrlMap

        // Si ya está en cache Y no forzamos refresh, salir
        if (!forceRefresh && currentMap.containsKey(song.id)) {
            return
        }

        // Si forzamos refresh, primero borrar la entrada del map
        if (forceRefresh) {
            _uiState.value = _uiState.value.copy(
                videoUrlMap = _uiState.value.videoUrlMap - song.id
            )
        }

        viewModelScope.launch {
            val videoUrl = repository.getVideoStreamUrlForSong(song)
            if (videoUrl.isNotBlank()) {
                _uiState.value = _uiState.value.copy(
                    videoUrlMap = _uiState.value.videoUrlMap + (song.id to videoUrl)
                )
            }
        }
    }

    fun forceRefreshVideoStreamUrl(song: Song) {
        loadVideoStreamUrl(song, forceRefresh = true)
    }

    // ── Letras ──

    fun toggleLyricsVisibility() {
        _uiState.value = _uiState.value.copy(showLyrics = !_uiState.value.showLyrics)
    }

    // ── Playlists ──

    fun createPlaylist(
        title: String,
        description: String = "",
        coverUrl: String = "",
        isPublic: Boolean = true,
        downloadOffline: Boolean = false,
        onCreated: (Long) -> Unit = {}
    ) {
        val currentCount = repository.playlists.value.size
        handleGate(TierGate.canCreatePlaylist(currentTier(), currentCount)) {
            viewModelScope.launch {
                val publicCoverUrl = if (coverUrl.isNotBlank()) {
                    storageRepository.uploadImageIfNeeded(coverUrl, folder = "playlist_covers")
                } else coverUrl

                val playlist = repository.createPlaylist(
                    title = title,
                    description = description,
                    coverUrl = publicCoverUrl,
                    isPublic = isPublic,
                    downloadOffline = downloadOffline
                )
                onCreated(playlist.id)

                val userId = authRepository.currentUserId()
                if (userId != null) {
                    syncRepository.uploadPlaylists(userId, repository.playlists.value)
                }
            }
        }
    }

    fun updatePlaylistCover(id: Long, coverUrl: String) {
        viewModelScope.launch {
            val publicCoverUrl = storageRepository.uploadImageIfNeeded(coverUrl, folder = "playlist_covers")
            repository.updatePlaylistCover(id, publicCoverUrl)

            val userId = authRepository.currentUserId()
            if (userId != null) {
                syncRepository.uploadPlaylists(userId, repository.playlists.value)
            }
        }
    }

    fun renamePlaylist(id: Long, newTitle: String) {
        repository.renamePlaylist(id, newTitle)
    }

    fun deletePlaylist(id: Long) {
        repository.deletePlaylist(id)
    }

    fun addSongsToPlaylist(playlistId: Long, songs: List<Song>) {
        val playlist = repository.playlists.value.firstOrNull { it.id == playlistId }
        val currentSize = playlist?.songs?.size ?: 0
        var simulatedSize = currentSize
        for (song in songs) {
            when (val result = TierGate.canAddSongToPlaylist(currentTier(), simulatedSize)) {
                is TierGateResult.Allowed -> simulatedSize++
                is TierGateResult.Blocked -> {
                    _uiState.value = _uiState.value.copy(pendingUpgrade = result.reason)
                    return
                }
            }
        }
        repository.addSongsToPlaylist(playlistId, songs)
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        repository.removeSongFromPlaylist(playlistId, songId)
    }

    // ── Perfil / Ajustes ──

    fun updateProfile(
        name: String,
        email: String,
        birthDate: String = "14 de Agosto, 1998",
        country: String = "México (MX)",
        photoUri: String? = null
    ) {
        viewModelScope.launch {
            val publicPhotoUrl = if (!photoUri.isNullOrBlank()) {
                storageRepository.uploadImageIfNeeded(photoUri, folder = "avatars")
            } else photoUri

            val updated = _uiState.value.userProfile.copy(
                name = name,
                email = email,
                birthDate = birthDate,
                country = country,
                photoUri = publicPhotoUrl
            )
            _uiState.value = _uiState.value.copy(userProfile = updated)

            repository.saveUserProfileLocal(updated)

            authRepository.updateProfile(
                displayName = name,
                photoUrl = publicPhotoUrl,
                birthDate = birthDate,
                country = country
            )
            _events.send(MusicUiEvent.ShowToast("✓ Perfil guardado en la cuenta"))

            val userId = authRepository.currentUserId()
            if (userId != null) {
                syncRepository.uploadPlaylists(userId, repository.playlists.value)
            }
        }
    }

    fun updatePassword(newPassword: String) {
        viewModelScope.launch {
            val result = authRepository.updatePassword(newPassword)
            result.fold(
                onSuccess = { _events.send(MusicUiEvent.ShowToast("✓ Contraseña actualizada")) },
                onFailure = { error -> _events.send(MusicUiEvent.ShowToast("⚠ Error actualizando contraseña")) }
            )
        }
    }

    fun resetAccountStorage() {
        viewModelScope.launch {
            repository.clearAllUserData()
            clearAppCache()
            val userId = authRepository.currentUserId()
            if (userId != null) {
                syncRepository.clearCloudUserData(userId)
            }
            _events.send(MusicUiEvent.ShowToast("✓ Almacenamiento e historial reiniciados a 0"))
        }
    }

    fun deleteAccountPermanent() {
        viewModelScope.launch {
            val userId = authRepository.currentUserId()
            if (userId != null) {
                authRepository.deleteAccountPermanent()
            }
            repository.clearAllUserData()
            clearAppCache()
            signOut()
            _events.send(MusicUiEvent.ShowToast("✓ Cuenta eliminada permanentemente"))
        }
    }

    fun setDownloadQuality(quality: String) {
        _uiState.value = _uiState.value.copy(downloadQuality = quality)
    }

    fun setDownloadPreferences(quality: String, wifiOnly: Boolean, autoFav: Boolean) {
        _uiState.value = _uiState.value.copy(
            downloadQuality = quality,
            wifiOnlyDownload = wifiOnly,
            autoDownloadFavorites = autoFav
        )
        // Aquí a futuro guardarías esto en SharedPreferences para que persista
        viewModelScope.launch {
            _events.send(MusicUiEvent.ShowToast("Preferencias de descarga guardadas"))
        }
    }

    // Llama a esto en el init o cuando abra la pantalla de almacenamiento
    fun calculateStorage() {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Calcular peso de descargas (simulado por ahora, * 4.5MB por canción)
            val musicBytes = repository.downloadedSongMap.value.size * 4_500_000L

            // 2. Calcular peso del caché (puedes usar context.cacheDir)
            var cacheBytes = 0L
            try {
                val cacheDir = getApplication<Application>().cacheDir
                cacheDir.walkTopDown().forEach { if (it.isFile) cacheBytes += it.length() }
            } catch (e: Exception) { }

            _uiState.value = _uiState.value.copy(
                downloadedStorageBytes = musicBytes,
                cacheStorageBytes = cacheBytes
            )
        }
    }

    fun clearAppCache() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().cacheDir.deleteRecursively()
                // Aquí también podrías limpiar el UrlCache (SharedPreferences)
                calculateStorage()
                _events.send(MusicUiEvent.ShowToast("Caché liberado con éxito"))
            } catch (e: Exception) {
                _events.send(MusicUiEvent.ShowToast("Error al liberar caché"))
            }
        }
    }

    fun deleteAllDownloads() {
        viewModelScope.launch {
            _events.send(MusicUiEvent.ShowToast("Eliminando descargas..."))

            val success = repository.deleteAllDownloads()

            if (success) {
                _events.send(MusicUiEvent.ShowToast("✅ Todas las descargas eliminadas"))
                // Recalcular el almacenamiento para actualizar la vista
                calculateStorage()
            } else {
                _events.send(MusicUiEvent.ShowToast("⚠️ Hubo un problema al eliminar algunas descargas"))
            }
        }
    }

    fun setStorageLimit(gb: Int) {
        _uiState.value = _uiState.value.copy(storageLimitGb = gb)
    }

    // ── Actualizaciones in-app ──

    fun checkForUpdate() {
        viewModelScope.launch {
            val info = updateChecker.checkForUpdate()
            if (info != null) {
                _uiState.value = _uiState.value.copy(updateInfo = info)
            }
        }
    }

    fun dismissUpdateDialog() {
        _uiState.value = _uiState.value.copy(
            updateInfo = null,
            updateErrorMessage = null
        )
    }

    fun downloadAndInstallUpdate() {
        val info = _uiState.value.updateInfo ?: return

        _uiState.value = _uiState.value.copy(
            isDownloadingUpdate = true,
            updateDownloadProgress = 0,
            updateErrorMessage = null
        )

        viewModelScope.launch {
            val apkFile = apkDownloader.downloadWithFallback(info.apkUrl, info.versionName) { progress ->
                _uiState.value = _uiState.value.copy(updateDownloadProgress = progress)
            }

            if (apkFile != null) {
                _uiState.value = _uiState.value.copy(
                    isDownloadingUpdate = false,
                    updateInfo = null,
                    apkReadyToInstall = apkFile
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isDownloadingUpdate = false,
                    updateErrorMessage = "No se pudo descargar la actualización. Verifica tu conexión."
                )
            }
        }
    }

    fun toggleSpatialAudio(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isSpatialAudioEnabled = enabled)
        // Aquí conectarías con el motor de audio real en el futuro
        viewModelScope.launch {
            _events.send(MusicUiEvent.ShowToast(if (enabled) "Audio Espacial Activado" else "Audio Espacial Desactivado"))
        }
    }

    fun toggleDarkTheme(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isDarkTheme = enabled)
        // La app actualmente está forzada a modo oscuro, pero aquí se manejaría el cambio de tema.
    }

    /**
     * Se llama desde MainScreen cuando hay un APK listo.
     * Lanza el instalador del sistema.
     */
    fun installApk(context: android.content.Context) {
        val file = _uiState.value.apkReadyToInstall ?: return
        ApkInstaller.install(context, file)
        _uiState.value = _uiState.value.copy(apkReadyToInstall = null)
    }

    fun clearUpdateError() {
        _uiState.value = _uiState.value.copy(updateErrorMessage = null)
    }

    fun repairSong(song: Song) {
        viewModelScope.launch {
            Log.d("MusicViewModel", "🔧 Iniciando reparación: ${song.title}")

            _events.send(MusicUiEvent.ShowToast("🔧 Reparando canción..."))

            val success = try {
                repository.repairSong(song)
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Error reparando: ${e.message}", e)
                false
            }

            if (success) {
                _events.send(MusicUiEvent.ShowToast("✅ Canción reparada"))
            } else {
                _events.send(MusicUiEvent.ShowToast("⚠ No se pudo reparar la canción"))
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Autenticación (Fase 6.3)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verifica si hay una sesión activa al arrancar la app.
     * Si la hay, carga el perfil desde Supabase.
     */
    private suspend fun checkAuthStatus() {
        try {
            val localProfile = repository.loadUserProfileLocal()
            if (localProfile != null) {
                _uiState.value = _uiState.value.copy(userProfile = localProfile)
            }

            val loggedIn = authRepository.isLoggedIn()
            if (loggedIn) {
                val profile = authRepository.fetchProfile()
                if (profile != null) {
                    _uiState.value = _uiState.value.copy(
                        authProfile = profile,
                        isLoggedIn = true,
                        userProfile = _uiState.value.userProfile.copy(
                            name = profile.displayName ?: localProfile?.name ?: "Usuario",
                            email = profile.email ?: localProfile?.email ?: "",
                            birthDate = profile.birthDate ?: localProfile?.birthDate ?: "14 de Agosto, 1998",
                            country = profile.country ?: localProfile?.country ?: "México (MX)",
                            photoUri = profile.photoUrl ?: localProfile?.photoUri
                        )
                    )
                }
            } else {
                // 👇 SI NO HAY SESIÓN, FORZAMOS AL MODO INVITADO O PERFIL LOCAL
                _uiState.value = _uiState.value.copy(
                    authProfile = null,
                    isLoggedIn = false,
                    userProfile = localProfile ?: com.haziel.ofmusic.domain.model.UserProfile(
                        name = "Invitado",
                        email = "sin-cuenta@ofmusic.com",
                        isAdmin = false
                    )
                )
                Log.d("MusicViewModel", "Sin sesión activa → modo invitado")
            }
        } catch (e: Exception) {
            Log.e("MusicViewModel", "Error verificando sesión: ${e.message}", e)
        }
    }

    /**
     * Inicia sesión con email y contraseña.
     */
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAuthLoading = true,
                authError = null
            )

            val result = authRepository.signIn(email.trim(), password)

            result.fold(
                onSuccess = { profile ->
                    _uiState.value = _uiState.value.copy(
                        authProfile = profile,
                        isLoggedIn = true,
                        isAuthLoading = false,
                        authError = null,
                        userProfile = _uiState.value.userProfile.copy(
                            name = profile.displayName ?: email.substringBefore("@"),
                            email = profile.email ?: email,
                            photoUri = profile.photoUrl ?: _uiState.value.userProfile.photoUri
                        )
                    )
                    Log.d("MusicViewModel", "✓ Login exitoso")
                    syncLocalDataWithCloud(profile.id)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isAuthLoading = false,
                        authError = mapAuthError(error.message ?: "Error desconocido")
                    )
                    Log.e("MusicViewModel", "✗ Login falló: ${error.message}")
                }
            )
        }
    }

    /**
     * Registra un nuevo usuario.
     */
    fun signUp(email: String, password: String, displayName: String, photoUri: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAuthLoading = true,
                authError = null
            )


            val result = authRepository.signUp(
                email = email.trim(),
                password = password,
                displayName = displayName.trim().ifBlank { email.substringBefore("@") }
            )

            result.fold(
                onSuccess = { profile ->
                    // Si el registro requiere confirmación de email,
                    // el profile puede venir con tier 'free' pero sin sesión activa.
                    if (authRepository.isLoggedIn()) {
                        _uiState.value = _uiState.value.copy(
                            authProfile = profile,
                            isLoggedIn = true,
                            isAuthLoading = false,
                            authError = null,
                            userProfile = _uiState.value.userProfile.copy(
                                name = profile.displayName ?: displayName,
                                email = profile.email ?: email,
                                photoUri = photoUri ?: profile.photoUrl ?: _uiState.value.userProfile.photoUri
                            )
                        )
                        Log.d("MusicViewModel", "✓ Registro exitoso")
                        syncLocalDataWithCloud(profile.id)
                    } else {
                        // Requiere confirmación de email
                        _uiState.value = _uiState.value.copy(
                            isAuthLoading = false,
                            authError = "Revisa tu correo para confirmar la cuenta"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isAuthLoading = false,
                        authError = mapAuthError(error.message ?: "Error desconocido")
                    )
                }
            )
        }
    }

    /**
     * Sincroniza los datos locales con la nube después de un login exitoso.
     * No borra nada local. Solo sube lo que falta y avisa al usuario.
     */
    private fun syncLocalDataWithCloud(userId: String) {
        viewModelScope.launch {
            try {
                Log.d("MusicViewModel", "▶ Sincronizando datos con la nube...")

                val result = syncRepository.migrateLocalToCloud(
                    userId = userId,
                    localFavoriteIds = repository.likedSongIds.value,
                    localPlaylists = repository.playlists.value,
                    songs = repository.allSongs.value
                )

                result.fold(
                    onSuccess = { migration ->
                        val message = buildString {
                            if (migration.favoritesUploaded > 0 || migration.playlistsUploaded > 0) {
                                append("☁ ")
                                if (migration.favoritesUploaded > 0) {
                                    append("${migration.favoritesUploaded} favoritos")
                                }
                                if (migration.favoritesUploaded > 0 && migration.playlistsUploaded > 0) {
                                    append(" y ")
                                }
                                if (migration.playlistsUploaded > 0) {
                                    append("${migration.playlistsUploaded} playlists")
                                }
                                append(" subidos")
                            } else {
                                append("✓ Todo sincronizado")
                            }
                        }
                        _events.send(MusicUiEvent.ShowToast(message))
                        Log.d("MusicViewModel", "✓ Sync completada: $message")
                    },
                    onFailure = { error ->
                        _events.send(MusicUiEvent.ShowToast("⚠ Error sincronizando: ${error.message}"))
                        Log.e("MusicViewModel", "✗ Sync falló: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Error en sync: ${e.message}", e)
            }
        }
    }

    /**
     * Cierra la sesión actual.
     * Los datos locales (favoritos, playlists, descargas) SE CONSERVAN.
     */
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            // 👇 Al cerrar sesión, lo regresamos a Invitado
            _uiState.value = _uiState.value.copy(
                authProfile = null,
                isLoggedIn = false,
                authError = null,
                userProfile = com.haziel.ofmusic.domain.model.UserProfile(
                    name = "Invitado",
                    email = "sin-cuenta@ofmusic.com",
                    isAdmin = false
                )
            )
            Log.d("MusicViewModel", "✓ Sesión cerrada → modo invitado")
        }
    }

    fun clearAuthError() {
        _uiState.value = _uiState.value.copy(authError = null)
    }

    /**
     * Traduce errores de Supabase a mensajes amigables.
     */
    private fun mapAuthError(message: String): String {
        return when {
            message.contains("Invalid login credentials", ignoreCase = true) ->
                "Correo o contraseña incorrectos"
            message.contains("User already registered", ignoreCase = true) ->
                "Este correo ya está registrado"
            message.contains("Password should be at least", ignoreCase = true) ->
                "La contraseña debe tener al menos 6 caracteres"
            message.contains("Email not confirmed", ignoreCase = true) ->
                "Confirma tu correo antes de iniciar sesión"
            message.contains("Unable to validate email", ignoreCase = true) ->
                "El correo no es válido"
            message.contains("network", ignoreCase = true) ->
                "Sin conexión a internet"
            else -> message
        }
    }

    private fun currentTier(): UserTier =
        _uiState.value.authProfile?.asUserTier() ?: UserTier.FREE

    private fun handleGate(result: TierGateResult, onAllowed: () -> Unit) {
        when (result) {
            is TierGateResult.Allowed -> onAllowed()
            is TierGateResult.Blocked -> {
                _uiState.value = _uiState.value.copy(pendingUpgrade = result.reason)
            }
        }
    }

    fun dismissUpgrade() {
        _uiState.value = _uiState.value.copy(pendingUpgrade = null)
    }

    private fun checkInitialConnectivity(context: android.content.Context): Boolean {
        return runCatching {
            val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val active = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(active)
            caps?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        }.getOrDefault(false)
    }

    override fun onCleared() {
        playbackConnection.release()
        super.onCleared()
    }
}