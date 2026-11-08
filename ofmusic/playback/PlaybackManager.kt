package com.haziel.ofmusic.playback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.haziel.ofmusic.data.repository.MusicRepository
import com.haziel.ofmusic.domain.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * @Deprecated Esta clase queda obsoleta.
 *
 * Motivo: el ExoPlayer vivía aquí (en el ViewModel), por lo que al destruirse
 * el ViewModel se detenía el audio. La reproducción en segundo plano debe
 * gestionarla exclusivamente [AudioPlaybackService], y la UI debe conectarse
 * a través de [PlaybackConnection].
 *
 * NO BORRAR: se conserva como referencia histórica y para facilitar rollback.
 */
@Deprecated("Usar PlaybackConnection + AudioPlaybackService en su lugar")
class PlaybackManager(
    private val context: Context,
    private val repository: MusicRepository
) {
    private val tag = "PlaybackManager"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private var positionJob: Job? = null

    companion object {
        @Volatile
        @Deprecated("Ya no se usa. La MediaSession ahora vive en AudioPlaybackService")
        var mediaSession: MediaSession? = null
    }

    init {
        initMediaSession()

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                Log.d(tag, "Estado de reproducción cambiado: playing=$playing")
                if (playing) {
                    startPositionUpdates()
                } else {
                    stopPositionUpdates()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        val dur = exoPlayer.duration
                        if (dur > 0L) _durationMs.value = dur
                        Log.d(tag, "ExoPlayer LISTO (STATE_READY). Duración: ${dur}ms")
                    }
                    Player.STATE_ENDED -> {
                        Log.d(tag, "ExoPlayer TERMINADO (STATE_ENDED). Avanzando siguiente...")
                        onSongEnded()
                    }
                    Player.STATE_BUFFERING -> {
                        Log.d(tag, "ExoPlayer CARGANDO (STATE_BUFFERING)...")
                    }
                    Player.STATE_IDLE -> {
                        Log.d(tag, "ExoPlayer INACTIVO (STATE_IDLE)")
                    }
                }
            }
        })
    }

    private fun initMediaSession() {
        try {
            if (mediaSession == null) {
                mediaSession = MediaSession.Builder(context, exoPlayer).build()
                Log.d(tag, "✓ MediaSession creada con éxito")
            }
            startPlaybackService()
        } catch (e: Exception) {
            Log.e(tag, "Error creando MediaSession: ${e.localizedMessage}", e)
        }
    }

    private fun startPlaybackService() {
        try {
            val intent = Intent(context, AudioPlaybackService::class.java)
            context.startService(intent)
            Log.d(tag, "✓ Servicio AudioPlaybackService iniciado para reproducción en segundo plano")
        } catch (e: Exception) {
            Log.w(tag, "No se pudo iniciar AudioPlaybackService: ${e.localizedMessage}")
        }
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                val pos = exoPlayer.currentPosition
                val dur = exoPlayer.duration
                _currentPositionMs.value = if (pos > 0L) pos else 0L
                if (dur > 0L) _durationMs.value = dur
                delay(500)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionJob?.cancel()
        positionJob = null
    }

    fun playSingleSong(song: Song) {
        scope.launch {
            Log.d(tag, "► Reproduciendo canción individual: ${song.title} (${song.artist})")
            _currentSong.value = song

            // Al buscar y seleccionar una canción individual, no se borra la cola ni se mete toda la búsqueda.
            if (_queue.value.isEmpty()) {
                _queue.value = listOf(song)
            } else if (_queue.value.none { it.id == song.id }) {
                _queue.value = _queue.value + song
            }

            val mediaUrl = repository.getAudioUrlForSong(song)
            prepareAndPlayUrl(mediaUrl, song)
        }
    }

    fun addToQueue(song: Song) {
        val currentList = _queue.value.toMutableList()
        if (currentList.none { it.id == song.id }) {
            currentList.add(song)
            _queue.value = currentList
            Log.d(tag, "➕ Añadida canción a la cola sin borrar existente: ${song.title}. Tamaño actual de cola: ${currentList.size}")
        } else {
            Log.d(tag, "La canción ${song.title} ya estaba en la cola")
        }
    }

    fun playPlaylist(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        scope.launch {
            Log.d(tag, "↺ Reproduciendo Álbum/Playlist. Reiniciando cola con ${songs.size} canciones...")
            _queue.value = songs
            val targetSong = songs.getOrElse(startIndex) { songs.first() }
            _currentSong.value = targetSong

            val mediaUrl = repository.getAudioUrlForSong(targetSong)
            prepareAndPlayUrl(mediaUrl, targetSong)
        }
    }

    private fun prepareAndPlayUrl(url: String, song: Song) {
        if (url.isBlank()) {
            Log.e(tag, "URL de audio vacía para ${song.title}")
            return
        }
        Log.d(tag, "Preparando URL de audio para ${song.title}: $url")

        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setArtworkUri(if (song.artworkUrl.isNotBlank()) Uri.parse(song.artworkUrl) else null)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(url))
            .setMediaMetadata(mediaMetadata)
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        _isPlaying.value = true
        startPlaybackService()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            Log.d(tag, "Pausando audio...")
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.seekTo(0)
            }
            Log.d(tag, "Reanudando audio...")
            exoPlayer.play()
        }
    }

    fun playNext() {
        val current = _currentSong.value ?: return
        val currentList = _queue.value
        if (currentList.isEmpty()) return

        val currentIndex = currentList.indexOfFirst { it.id == current.id }
        val nextIndex = if (currentIndex != -1 && currentIndex + 1 < currentList.size) {
            currentIndex + 1
        } else {
            0
        }

        val nextSong = currentList[nextIndex]
        Log.d(tag, "⏭ Siguiente de la cola: ${nextSong.title} (Índice $nextIndex de ${currentList.size})")
        playSingleSongFromQueue(nextSong)
    }

    fun playPrevious() {
        if (exoPlayer.currentPosition > 3000L) {
            Log.d(tag, "⏮ Reiniciando canción actual...")
            exoPlayer.seekTo(0)
            return
        }

        val current = _currentSong.value ?: return
        val currentList = _queue.value
        if (currentList.isEmpty()) return

        val currentIndex = currentList.indexOfFirst { it.id == current.id }
        val prevIndex = if (currentIndex > 0) {
            currentIndex - 1
        } else {
            currentList.size - 1
        }

        val prevSong = currentList[prevIndex]
        Log.d(tag, "⏮ Anterior en la cola: ${prevSong.title} (Índice $prevIndex de ${currentList.size})")
        playSingleSongFromQueue(prevSong)
    }

    private fun playSingleSongFromQueue(song: Song) {
        scope.launch {
            _currentSong.value = song
            val mediaUrl = repository.getAudioUrlForSong(song)
            prepareAndPlayUrl(mediaUrl, song)
        }
    }

    fun seekTo(positionMs: Long) {
        Log.d(tag, "Buscando posición en la línea de tiempo: ${positionMs}ms")
        exoPlayer.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    private fun onSongEnded() {
        Log.d(tag, "✓ Canción completada. Pasando al siguiente tema de la cola automáticamente...")
        playNext()
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
        Log.d(tag, "Modo aleatorio: ${_isShuffleEnabled.value}")
        if (_isShuffleEnabled.value) {
            _queue.value = _queue.value.shuffled()
        }
    }

    fun toggleRepeat() {
        val nextMode = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        _repeatMode.value = nextMode
        exoPlayer.repeatMode = nextMode
        Log.d(tag, "Modo repetición: $nextMode")
    }

    fun getCurrentPosition(): Long = exoPlayer.currentPosition
    fun getDuration(): Long = if (exoPlayer.duration > 0L) exoPlayer.duration else 0L

    fun release() {
        stopPositionUpdates()
        mediaSession?.release()
        mediaSession = null
        exoPlayer.release()
        scope.cancel()
        Log.d(tag, "PlaybackManager liberado")
    }
}
