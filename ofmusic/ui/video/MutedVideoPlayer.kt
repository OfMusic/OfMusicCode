package com.haziel.ofmusic.ui.video

import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import kotlin.math.abs

@Composable
fun MutedVideoPlayer(
    videoUrlOrId: String,
    isPlayingAudio: Boolean,
    audioPositionMs: Long,
    modifier: Modifier = Modifier,
    onReady: (() -> Unit)? = null,
    onError: (() -> Unit)? = null
) {
    val isDirectVideoUrl = videoUrlOrId.startsWith("http") &&
            (videoUrlOrId.contains("googlevideo.com") ||
                    videoUrlOrId.endsWith(".mp4") ||
                    videoUrlOrId.endsWith(".webm"))

    if (videoUrlOrId.isBlank() || !isDirectVideoUrl) {
        Box(modifier = modifier.background(Color.Transparent))
        return
    }

    DirectMutedVideoExoPlayer(
        videoUrl = videoUrlOrId,
        isPlayingAudio = isPlayingAudio,
        audioPositionMs = audioPositionMs,
        onReady = onReady,
        onError = onError,
        modifier = modifier
    )
}

@UnstableApi
@Composable
private fun DirectMutedVideoExoPlayer(
    videoUrl: String,
    isPlayingAudio: Boolean,
    audioPositionMs: Long,
    onReady: (() -> Unit)?,
    onError: (() -> Unit)?,
    modifier: Modifier
) {
    val context = LocalContext.current
    var isReady by remember { mutableStateOf(false) }

    val videoExoPlayer = remember {
        // TrackSelector que DESHABILITA el audio completamente
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            )
        }
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setHandleAudioBecomingNoisy(false)
            .build().apply {
                volume = 0f
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = false
            }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        if (!isReady) {
                            isReady = true
                            onReady?.invoke()
                        }
                    }
                    Player.STATE_IDLE -> {}
                    Player.STATE_BUFFERING -> {}
                    Player.STATE_ENDED -> {}
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isReady = false
                onError?.invoke()
            }
        }
        videoExoPlayer.addListener(listener)

        onDispose {
            videoExoPlayer.removeListener(listener)
            videoExoPlayer.release()
        }
    }

    LaunchedEffect(videoUrl) {
        if (videoUrl.isNotBlank()) {
            isReady = false
            videoExoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
            videoExoPlayer.prepare()
            if (isPlayingAudio) videoExoPlayer.play()
        }
    }

    LaunchedEffect(isPlayingAudio) {
        if (isPlayingAudio) videoExoPlayer.play() else videoExoPlayer.pause()
    }

    LaunchedEffect(audioPositionMs) {
        if (audioPositionMs > 0L && videoExoPlayer.playbackState != Player.STATE_IDLE) {
            val diff = abs(videoExoPlayer.currentPosition - audioPositionMs)
            if (diff > 1500) {
                videoExoPlayer.seekTo(audioPositionMs)
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = videoExoPlayer
                useController = false
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier
    )
}