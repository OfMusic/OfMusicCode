package com.haziel.ofmusic.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.haziel.ofmusic.domain.model.Song
import org.json.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PlaybackConnection(
    private val context: Context,
    private val scope: CoroutineScope,
    private val urlResolver: suspend (Song) -> String
) {
    private val tag = "PlaybackConnection"

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var positionJob: Job? = null

    private var currentQueue: List<Song> = emptyList()
    private var currentIndex: Int = -1

    private val resolverMutex = Mutex()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(-1)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                _isBuffering.value = false
                startPositionUpdates()
            } else {
                stopPositionUpdates()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val c = controller ?: return
            when (playbackState) {
                Player.STATE_BUFFERING -> _isBuffering.value = true
                Player.STATE_READY -> {
                    _isBuffering.value = false
                    val dur = c.duration
                    if (dur > 0) _durationMs.value = dur
                }
                Player.STATE_ENDED, Player.STATE_IDLE -> _isBuffering.value = false
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            Log.d(tag, "Transición de media item (razón=$reason)")
            handleMediaItemTransition(mediaItem)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            Log.d(tag, "Shuffle cambió a: $shuffleModeEnabled")
            _shuffleEnabled.value = shuffleModeEnabled
            // Re-preload del siguiente item (que ahora es distinto con shuffle)
            controller?.let { c ->
                val nextIdx = c.nextMediaItemIndex
                if (nextIdx != androidx.media3.common.C.INDEX_UNSET) {
                    preloadAt(nextIdx)
                }
            }
        }
    }

    fun connect() {
        if (controller != null || controllerFuture != null) return

        val token = SessionToken(context, ComponentName(context, AudioPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future

        future.addListener({
            try {
                val c = future.get()
                controller = c
                c.addListener(playerListener)
                _isConnected.value = true
                syncFromController()
                startPositionUpdates()
                Log.d(tag, "✓ Conectado al AudioPlaybackService")
            } catch (e: Exception) {
                Log.e(tag, "Error conectando al servicio: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                controller?.let { c ->
                    _currentPositionMs.value = c.currentPosition.coerceAtLeast(0L)
                    val d = c.duration
                    if (d > 0) _durationMs.value = d
                }
                delay(500)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionJob?.cancel()
        positionJob = null
    }

    private fun syncFromController() {
        val c = controller ?: return
        val idx = c.currentMediaItemIndex
        _queueIndex.value = idx
        _currentSong.value = currentQueue.getOrNull(idx)
        _queue.value = currentQueue
    }

    /**
     * Reproduce una lista completa. Solo resuelve la URL del startIndex.
     * Las demás quedan como placeholders y se resuelven al vuelo.
     */
    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        val c = controller ?: run {
            Log.w(tag, "Controller no conectado")
            return
        }
        if (songs.isEmpty()) return

        val start = startIndex.coerceIn(0, songs.size - 1)
        currentQueue = songs
        currentIndex = start
        _queue.value = songs
        _queueIndex.value = start

        // 1. DETENER EL AUDIO ANTERIOR INMEDIATAMENTE
        c.pause()
        c.clearMediaItems() // Esto apaga el audio anterior, pero mantiene la notificación viva

        // 2. ACTUALIZAR LA UI INMEDIATAMENTE
        val startSong = songs[start]
        _currentSong.value = startSong
        _isBuffering.value = true // Esto activa el spinner en la UI

        scope.launch {
            // 3. RESOLVER URL EN BACKGROUND
            val startUrl = resolveUrlSafe(startSong)
            if (startUrl.isBlank()) {
                Log.e(tag, "No se pudo resolver URL para ${startSong.title}")
                _isBuffering.value = false
                return@launch
            }

            val items = songs.mapIndexed { idx, song ->
                val url = if (idx == start) startUrl else PLACEHOLDER_SCHEME + song.id
                song.toMediaItem(url)
            }

            // 4. REPRODUCIR LA NUEVA
            c.setMediaItems(items, start, 0L)
            c.prepare()
            c.play()

            Log.d(tag, "▶ Cola preparada: ${items.size} canciones, inicio=$start (${startSong.title})")

            val nextIdx = c.nextMediaItemIndex
            if (nextIdx != androidx.media3.common.C.INDEX_UNSET) {
                preloadAt(nextIdx)
            }
        }
    }

    /**
     * Preload de la canción en `index`. Reemplaza el MediaItem placeholder
     * por uno con URL real.
     */
    private fun preloadAt(index: Int) {
        if (index !in currentQueue.indices) return
        val song = currentQueue[index]
        scope.launch {
            val url = resolveUrlSafe(song)
            if (url.isBlank()) return@launch
            val c = controller ?: return@launch

            for (i in 0 until c.mediaItemCount) {
                val item = c.getMediaItemAt(i)
                if (item.mediaId == song.id.toString() &&
                    item.localConfiguration?.uri.toString().startsWith(PLACEHOLDER_SCHEME)) {
                    c.replaceMediaItem(i, song.toMediaItem(url))
                    Log.d(tag, "↓ Preloaded [$i]: ${song.title}")
                    break
                }
            }
        }
    }

    /**
     * Se dispara cuando ExoPlayer cambia de item (auto-avance, seek, next, shuffle).
     */
    private fun handleMediaItemTransition(mediaItem: MediaItem?) {
        val c = controller ?: return
        val newIndex = c.currentMediaItemIndex
        currentIndex = newIndex
        _queueIndex.value = newIndex
        _currentSong.value = currentQueue.getOrNull(newIndex)
        _isBuffering.value = true

        // Si el item es placeholder, resolver ahora
        val uri = mediaItem?.localConfiguration?.uri?.toString().orEmpty()
        if (uri.startsWith(PLACEHOLDER_SCHEME)) {
            val songId = mediaItem?.mediaId?.toLongOrNull() ?: return
            val song = currentQueue.find { it.id == songId } ?: return
            scope.launch {
                val url = resolveUrlSafe(song)
                if (url.isBlank()) return@launch
                val ctrl = controller ?: return@launch
                ctrl.replaceMediaItem(ctrl.currentMediaItemIndex, song.toMediaItem(url))
                ctrl.prepare()
                ctrl.play()
            }
        }

        // Preload del "siguiente" según el shuffle actual
        val nextIdx = c.nextMediaItemIndex
        if (nextIdx != androidx.media3.common.C.INDEX_UNSET) {
            preloadAt(nextIdx)
        }
    }

    private suspend fun resolveUrlSafe(song: Song): String {
        val cached = song.audioUrl
        if (!cached.isNullOrBlank()) return cached

        return try {
            resolverMutex.withLock {
                song.audioUrl?.takeIf { !it.isNullOrBlank() } ?: urlResolver(song)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error resolviendo URL para ${song.title}: ${e.message}")
            ""
        }
    }

    fun play() { controller?.play() }
    fun pause() { controller?.pause() }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }

    /**
     * Siguiente. Usa seekToNextMediaItem() para respetar el orden aleatorio.
     */
    fun next() {
        val c = controller ?: return
        if (currentQueue.isEmpty()) return

        if (c.hasNextMediaItem()) {
            c.seekToNextMediaItem()
            c.play()
        } else if (c.repeatMode == Player.REPEAT_MODE_ALL) {
            c.seekTo(0, 0L)
            c.play()
        }
    }

    /**
     * Anterior. Usa seekToPreviousMediaItem() para respetar el orden aleatorio.
     */
    fun previous() {
        val c = controller ?: return
        if (c.currentPosition > 3000L) {
            c.seekTo(0)
            return
        }
        if (c.hasPreviousMediaItem()) {
            c.seekToPreviousMediaItem()
            c.play()
        } else if (c.repeatMode == Player.REPEAT_MODE_ALL) {
            c.seekTo(c.mediaItemCount - 1, 0L)
            c.play()
        }
    }

    fun seekToQueueIndex(index: Int) {
        if (index in currentQueue.indices) goToIndex(index)
    }

    private fun goToIndex(index: Int) {
        val song = currentQueue.getOrNull(index) ?: return
        val c = controller ?: return

        // 1. Detener audio anterior
        c.pause()

        // 2. Actualizar UI
        currentIndex = index
        _queueIndex.value = index
        _currentSong.value = song
        _isBuffering.value = true

        scope.launch {
            if (index < c.mediaItemCount) {
                val item = c.getMediaItemAt(index)
                if (item.localConfiguration?.uri.toString().startsWith(PLACEHOLDER_SCHEME)) {
                    val url = resolveUrlSafe(song)
                    if (url.isBlank()) {
                        _isBuffering.value = false
                        return@launch
                    }
                    c.replaceMediaItem(index, song.toMediaItem(url))
                }
            }
            c.seekTo(index, 0L)
            c.play()
        }
    }

    fun setVolume(volume: Float) {
        controller?.volume = volume
    }

    /**
     * Alterna shuffle. Si se activa mientras suena, salta a una canción
     * aleatoria inmediatamente para que el cambio sea perceptible.
     */
    fun toggleShuffle() {
        val c = controller ?: return
        val newValue = !c.shuffleModeEnabled
        c.shuffleModeEnabled = newValue
        Log.d(tag, "Shuffle: $newValue")

        if (newValue && c.mediaItemCount > 1 && c.isPlaying) {
            // Saltar a una canción aleatoria distinta de la actual
            var randomIndex = (0 until c.mediaItemCount).random()
            var attempts = 0
            while (randomIndex == c.currentMediaItemIndex && attempts < 10) {
                randomIndex = (0 until c.mediaItemCount).random()
                attempts++
            }
            if (randomIndex != c.currentMediaItemIndex) {
                goToIndex(randomIndex)
                Log.d(tag, "Shuffle → saltando a índice aleatorio: $randomIndex")
            }
        }
    }

    /**
     * Alterna entre los 3 modos de repetición:
     * OFF → ONE → ALL → OFF
     */
    fun toggleRepeat() {
        val c = controller ?: return
        val next = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        c.repeatMode = next
        Log.d(tag, "Repeat mode: $next")
    }

    /**
     * Añade canciones sin duplicar.
     */
    fun addToQueue(songs: List<Song>): Int {
        val c = controller ?: return 0
        val existingIds = currentQueue.map { it.id }.toSet()
        val newSongs = songs.filterNot { it.id in existingIds }
        if (newSongs.isEmpty()) {
            Log.d(tag, "addToQueue: todas ya estaban en la cola")
            return 0
        }

        val wasEmpty = currentQueue.isEmpty()

        val items = newSongs.map { song ->
            val url = song.audioUrl?.takeIf { !it.isNullOrBlank() }
                ?: (PLACEHOLDER_SCHEME + song.id)
            song.toMediaItem(url)
        }

        c.addMediaItems(items)
        currentQueue = currentQueue + newSongs
        _queue.value = currentQueue

        if (wasEmpty && currentQueue.isNotEmpty()) {
            goToIndex(0)
        } else {
            val nextIdx = c.nextMediaItemIndex
            if (nextIdx != androidx.media3.common.C.INDEX_UNSET) {
                preloadAt(nextIdx)
            }
        }

        Log.d(tag, "addToQueue: +${newSongs.size} items. Total=${currentQueue.size}")
        return newSongs.size
    }

    fun release() {
        stopPositionUpdates()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
        _isConnected.value = false
    }

    // ── Serialización Song ↔ MediaItem ──────────────────────────

    private fun Song.toMediaItem(audioUrl: String): MediaItem {
        val extras = Bundle().apply {
            putString("song_json", toJson())
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(artworkUrl.takeIf { it.isNotBlank() }?.let { Uri.parse(it) })
            .setExtras(extras)
            .build()

        return MediaItem.Builder()
            .setUri(audioUrl)
            .setMediaId(id.toString())
            .setMediaMetadata(metadata)
            .build()
    }

    private fun Song.toJson(): String = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("artist", artist)
        put("album", album)
        put("artworkUrl", artworkUrl)
        put("durationMs", durationMs)
        put("youtubeId", youtubeId)
        put("audioUrl", audioUrl)
        put("videoUrl", videoUrl)
        put("isLiked", isLiked)
        put("isDownloaded", isDownloaded)
        put("localPath", localPath)
    }.toString()

    companion object {
        private const val PLACEHOLDER_SCHEME = "ofmusic://pending/"
    }
}