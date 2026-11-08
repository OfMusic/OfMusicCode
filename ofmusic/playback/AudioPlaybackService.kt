package com.haziel.ofmusic.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import coil.imageLoader
import coil.request.ImageRequest
import com.haziel.ofmusic.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@UnstableApi
class AudioPlaybackService : MediaSessionService() {

    private val tag = "AudioPlaybackService"
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var colorJob: Job? = null
    private var bitmapJob: Job? = null
    private var lastArtworkUrl: String? = null

    companion object {
        const val CHANNEL_ID = "ofmusic_playback_channel"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "✓ AudioPlaybackService onCreate")

        createNotificationChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

// Busca donde creas el ExoPlayer y reemplázalo por:
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000, // minBufferMs
                30_000, // maxBufferMs
                500,    // bufferForPlaybackMs (¡Clave para la latencia inicial!)
                500     // bufferForPlaybackAfterRebufferMs
            ).build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .build()

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        try {
            setMediaNotificationProvider(
                CustomMediaNotificationProvider(applicationContext)
            )
            Log.d(tag, "✓ Custom notification provider registrado")
        } catch (e: Exception) {
            Log.e(tag, "✗ Error registrando notification provider: ${e.message}", e)
        }

        // ─── Listener de cambio de canción ───
        exoPlayer.addListener(object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                val artworkUrl = mediaMetadata.artworkUri?.toString()
                if (artworkUrl != lastArtworkUrl) {
                    lastArtworkUrl = artworkUrl
                    precomputeColors(artworkUrl)
                    preloadBitmap(artworkUrl)
                }
            }
        })

        player = exoPlayer
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reproducción",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles de reproducción de música"
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                enableVibration(false)
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
            Log.d(tag, "✓ Canal de notificación creado: $CHANNEL_ID")
        }
    }

    private fun precomputeColors(artworkUrl: String?) {
        colorJob?.cancel()
        colorJob = serviceScope.launch {
            try {
                MediaNotificationColors.precompute(applicationContext, artworkUrl)
                Log.d(tag, "✓ Colores listos para notificación")
            } catch (e: Exception) {
                Log.w(tag, "Error precalculando colores: ${e.message}")
            }
        }
    }

    /**
     * Carga el bitmap de la portada en background para que el
     * CustomMediaNotificationProvider lo lea del cache.
     * Esta es la pieza clave: el provider NUNCA bloquea, y el
     * bitmap aparece en la siguiente reconstrucción.
     */
    private fun preloadBitmap(url: String?) {
        if (url.isNullOrBlank()) return
        bitmapJob?.cancel()
        bitmapJob = serviceScope.launch {
            try {
                val request = ImageRequest.Builder(applicationContext)
                    .data(url)
                    .allowHardware(false)
                    .size(256, 256)
                    .build()
                val result = applicationContext.imageLoader.execute(request)
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    CustomMediaNotificationProvider.cacheBitmap(url, bitmap)
                    Log.d(tag, "✓ Bitmap preloaded para notificación")
                }
            } catch (e: Exception) {
                Log.w(tag, "Error precargando bitmap: ${e.message}")
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = mediaSession?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        Log.d(tag, "✕ AudioPlaybackService onDestroy")
        colorJob?.cancel()
        bitmapJob?.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        super.onDestroy()
    }
}