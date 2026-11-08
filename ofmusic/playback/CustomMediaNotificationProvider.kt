package com.haziel.ofmusic.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.LruCache
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.google.common.collect.ImmutableList
import com.haziel.ofmusic.MainActivity
import com.haziel.ofmusic.R

@UnstableApi
class CustomMediaNotificationProvider(
    private val context: Context
) : MediaNotification.Provider {

    companion object {
        const val NOTIFICATION_ID = 1001

        // Cache de bitmaps compartido — llenado por AudioPlaybackService
        internal val bitmapCache = LruCache<String, Bitmap>(10)

        fun cacheBitmap(url: String, bitmap: Bitmap) {
            bitmapCache.put(url, bitmap)
        }
    }

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification {
        val player = mediaSession.player
        val mediaMetadata = player.mediaMetadata

        val title = mediaMetadata.title?.toString().orEmpty().ifBlank { "Reproduciendo" }
        val artist = mediaMetadata.artist?.toString().orEmpty()
        val artworkUri = mediaMetadata.artworkUri?.toString()

        val colors = MediaNotificationColors.get(artworkUri)

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val smallIcon = IconCompat.createWithResource(context, R.drawable.ic_notification)

        // ← NUNCA bloquea. Solo lee del cache.
        val largeIconBitmap: Bitmap? = artworkUri?.let { bitmapCache.get(it) }

        val prevIntent = actionFactory.createMediaActionPendingIntent(
            mediaSession,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM.toLong()
        )
        val toggleIntent = actionFactory.createMediaActionPendingIntent(
            mediaSession,
            Player.COMMAND_PLAY_PAUSE.toLong()
        )
        val nextIntent = actionFactory.createMediaActionPendingIntent(
            mediaSession,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM.toLong()
        )

        val isPlaying = player.isPlaying
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause_notif else R.drawable.ic_play_notif
        val playPauseTitle = if (isPlaying) "Pausar" else "Reproducir"

        val prevAction = NotificationCompat.Action.Builder(
            IconCompat.createWithResource(context, R.drawable.ic_prev_notif),
            "Anterior",
            prevIntent
        ).build()

        val toggleAction = NotificationCompat.Action.Builder(
            IconCompat.createWithResource(context, playPauseIcon),
            playPauseTitle,
            toggleIntent
        ).build()

        val nextAction = NotificationCompat.Action.Builder(
            IconCompat.createWithResource(context, R.drawable.ic_next_notif),
            "Siguiente",
            nextIntent
        ).build()

        val mediaStyle = MediaStyleNotificationHelper.MediaStyle(mediaSession)
            .setShowActionsInCompactView(0, 1, 2)

        val builder = NotificationCompat.Builder(context, AudioPlaybackService.CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(contentIntent)
            .setColor(colors.primary)
            .setColorized(true)
            .setShowWhen(false)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setStyle(mediaStyle)
            .addAction(prevAction)
            .addAction(toggleAction)
            .addAction(nextAction)

        if (largeIconBitmap != null) {
            builder.setLargeIcon(largeIconBitmap)
        }

        return MediaNotification(NOTIFICATION_ID, builder.build())
    }

    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: Bundle
    ): Boolean = false
}