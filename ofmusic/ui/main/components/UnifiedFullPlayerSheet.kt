package com.haziel.ofmusic.ui.main.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.haziel.ofmusic.domain.model.Song
import com.haziel.ofmusic.domain.tier.TierLimits
import com.haziel.ofmusic.domain.tier.UserTier
import com.haziel.ofmusic.ui.theme.GlassColors
import com.haziel.ofmusic.ui.theme.PaletteHelper
import com.haziel.ofmusic.ui.theme.liquidGlass
import com.haziel.ofmusic.ui.video.MutedVideoPlayer
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UnifiedFullPlayerSheet(
    song: Song?,
    userTier: UserTier,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    queue: List<Song>,
    volume: Float = 1f,
    onVolumeChange: (Float) -> Unit = {},
    queueIndex: Int = -1,
    isShuffleEnabled: Boolean = false,
    repeatMode: Int = 0,
    videoUrlMap: Map<Long, String> = emptyMap(),
    isLiked: Boolean = false,
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false,
    lyrics: List<com.haziel.ofmusic.data.lyrics.LyricLine> = emptyList(),
    isLoadingLyrics: Boolean = false,
    showLyrics: Boolean = true,
    onToggleLyrics: () -> Unit = {},
    onCollapse: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
    onLikeToggle: (Song) -> Unit,
    onDownloadToggle: (Song) -> Unit,
    onSelectQueueSong: (Song) -> Unit,
    onFetchVideoUrl: (Song) -> Unit = {},
    onForceRefreshVideoUrl: (Song) -> Unit = {},
    onToggleAudioPro: () -> Unit = {},
    sharedTransitionScope: SharedTransitionScope, // 👈 NUEVO
    animatedVisibilityScope: AnimatedVisibilityScope // 👈 NUEVO
) {
    if (song == null) return

    val context = LocalContext.current
    var showQueueSheet by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }

    // Estado para el Drag (animación de cierre)
    var dragOffsetY by remember { mutableStateOf(0f) }

    // Video states
    val isYouTubeSong = !song.youtubeId.isNullOrBlank()
    var showVideo by remember(song.id) { mutableStateOf(false) }
    var pendingVideoRequest by remember(song.id) { mutableStateOf(false) }
    val resolvedVideoUrl = videoUrlMap[song.id] ?: ""
    val hasValidVideoUrl = resolvedVideoUrl.isNotBlank() && resolvedVideoUrl.contains("googlevideo.com")
    var videoReady by remember(song.id, resolvedVideoUrl) { mutableStateOf(false) }

    // Extraer colores
    var glassColors by remember { mutableStateOf<GlassColors?>(null) }
    LaunchedEffect(song.artworkUrl) {
        glassColors = PaletteHelper.extractFromUrl(context, song.artworkUrl)
    }

    val colors = glassColors ?: GlassColors(
        dominant = Color(0xFF2E1065),
        vibrant = MaterialTheme.colorScheme.primary,
        background = Color(0xFF0A0713),
        surfaceTint = MaterialTheme.colorScheme.primary
    )

    val accentColor = colors.vibrant

    LaunchedEffect(song.id) {
        videoReady = false; showVideo = false; pendingVideoRequest = false
    }
    LaunchedEffect(hasValidVideoUrl, pendingVideoRequest) {
        if (pendingVideoRequest && hasValidVideoUrl) { showVideo = true; pendingVideoRequest = false }
    }

    val formatText = if (isDownloaded) "Hi-Res / AAC" else "320 kbps / AAC"

    val dragProgress = (dragOffsetY / 450f).coerceIn(0f, 1f)
    val controlsAlpha = (1f - (dragProgress * 2.2f)).coerceIn(0f, 1f)
    val sheetBgAlpha = (1f - (dragProgress * 1.5f)).coerceIn(0f, 1f)
    val artworkScale = 1f - (dragProgress * 0.35f)
    val artworkCornerRadius = (24 + (dragProgress * 26)).dp

    with(sharedTransitionScope) {
        // 👇 EL CONTENEDOR PRINCIPAL USA sharedBounds PARA QUE TODA LA TARJETA SE TRANSFORME
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = ((1f - dragProgress) * 0.6f).coerceIn(0f, 0.6f)))
                .clickable(enabled = false) {}
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, dragOffsetY.roundToInt()) }
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "player_bounds"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                        boundsTransform = { _, _ -> tween(durationMillis = 550, easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0.0f, 0.2f, 1.0f)) }
                    )
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .graphicsLayer { alpha = sheetBgAlpha }
                    .background(colors.background)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                colors.vibrant.copy(alpha = 0.85f),
                                colors.dominant.copy(alpha = 0.60f),
                                colors.background,
                                Color(0xFF07070A)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Zona de arrastre y Píldora Central (Se desvanecen con controlsAlpha)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = controlsAlpha }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onDragEnd = {
                                            if (dragOffsetY > 100f) onCollapse() else dragOffsetY = 0f
                                        },
                                        onDragCancel = { dragOffsetY = 0f },
                                        onVerticalDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.width(48.dp).height(5.dp).clip(RoundedCornerShape(2.5.dp)).background(Color.White.copy(alpha = 0.15f)))
                        }

                        // Píldora Central
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.liquidGlass(RoundedCornerShape(50), alpha = 0.08f).padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("REPRODUCIENDO DESDE", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.28f), letterSpacing = 1.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Lossless 24-bit / 96kHz", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Portada / Video (Ligeramente reducido de alto + Bordes desvanecidos + Transición suave)
                    val isVideoActive = showVideo && hasValidVideoUrl && videoReady
                    val videoAlpha by animateFloatAsState(
                        targetValue = if (isVideoActive) 1f else 0f,
                        animationSpec = tween(800, easing = FastOutSlowInEasing),
                        label = "VideoAlpha"
                    )
                    val artworkAlpha by animateFloatAsState(
                        targetValue = if (isVideoActive) 0f else 1f,
                        animationSpec = tween(800, easing = FastOutSlowInEasing),
                        label = "ArtworkAlpha"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.22f)
                            .graphicsLayer {
                                scaleX = artworkScale
                                scaleY = artworkScale
                            }
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(key = "album_art_${song.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                            .clip(RoundedCornerShape(artworkCornerRadius))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .drawWithContent {
                                drawContent()
                                // Difuminado sutil de bordes para integrarse con el fondo
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            colors.background.copy(alpha = 0.5f),
                                            Color.Transparent,
                                            Color.Transparent,
                                            colors.background.copy(alpha = 0.6f)
                                        )
                                    )
                                )
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            colors.background.copy(alpha = 0.4f),
                                            Color.Transparent,
                                            Color.Transparent,
                                            colors.background.copy(alpha = 0.4f)
                                        )
                                    )
                                )
                            }
                    ) {
                        // Portada (Se oculta suavemente mediante artworkAlpha cuando el video se activa)
                        if (song.artworkUrl.isNotBlank()) {
                            AsyncImage(
                                model = song.artworkUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = artworkAlpha }
                            )
                        }

                        // Video (Se desvanece suavemente al 100% cuando videoReady es verdadero)
                        if (showVideo && hasValidVideoUrl) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = videoAlpha }
                            ) {
                                key(song.id, resolvedVideoUrl) {
                                    MutedVideoPlayer(
                                        videoUrlOrId = resolvedVideoUrl,
                                        isPlayingAudio = isPlaying,
                                        audioPositionMs = currentPositionMs,
                                        onReady = { videoReady = true },
                                        onError = { videoReady = false; onForceRefreshVideoUrl(song) },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        if (isDownloaded) {
                            Box(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .align(Alignment.TopStart)
                                    .liquidGlass(RoundedCornerShape(50), alpha = 0.2f)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Offline", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = controlsAlpha },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(20.dp))

                        // Título y Artista
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(text = song.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(text = song.artist, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        Spacer(Modifier.height(16.dp))

                        // Píldoras
                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isDownloaded) { Box(modifier = Modifier.liquidGlass(RoundedCornerShape(50), alpha = 0.08f).padding(horizontal = 12.dp, vertical = 6.dp)) { Text("Descargado", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall) } }
                            Box(modifier = Modifier.liquidGlass(RoundedCornerShape(50), alpha = 0.08f).padding(horizontal = 12.dp, vertical = 6.dp)) { Text(formatText, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall) }

                            if (isYouTubeSong) {
                                val canUseVideo = TierLimits.forTier(userTier).syncedVideo
                                Box(modifier = Modifier.liquidGlass(RoundedCornerShape(50), alpha = 0.08f).clickable { if (showVideo) showVideo = false else { pendingVideoRequest = true; onFetchVideoUrl(song) } }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = if (!canUseVideo && !showVideo) Icons.Default.Lock else Icons.Default.Videocam, contentDescription = "Video", tint = if (showVideo) accentColor else if (!canUseVideo) Color(0xFFFFD700) else Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text(text = if (showVideo) "Portada" else "Video", color = if (showVideo) accentColor else Color.White, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            Box(modifier = Modifier.liquidGlass(RoundedCornerShape(50), alpha = 0.08f).clickable { onLikeToggle(song) }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Me gusta", tint = if (isLiked) accentColor else Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text(text = "Me gusta", color = if (isLiked) accentColor else Color.White, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Slider
                        val safeDuration = if (durationMs > 0L) durationMs else song.durationMs.coerceAtLeast(1L)
                        val sliderPosition = (currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
                        Slider(value = sliderPosition, onValueChange = { onSeekTo((it * safeDuration).toLong()) }, modifier = Modifier.fillMaxWidth(), colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = accentColor, inactiveTrackColor = Color.White.copy(alpha = 0.2f)))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatDuration(currentPositionMs), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                            Text(formatDuration(safeDuration), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                        }

                        Spacer(Modifier.height(24.dp))

                        // Controles
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onToggleShuffle) { Icon(Icons.Default.Shuffle, contentDescription = "Aleatorio", tint = if (isShuffleEnabled) accentColor else Color.White.copy(alpha = 0.5f)) }
                            IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) { Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior", tint = Color.White, modifier = Modifier.size(36.dp)) }
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .liquidGlass(CircleShape, alpha = 0.25f)
                                    .background(accentColor.copy(alpha = 0.25f), CircleShape)
                                    .clickable(onClick = onTogglePlayPause),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                    modifier = Modifier.size(40.dp),
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) { Icon(Icons.Default.SkipNext, contentDescription = "Siguiente", tint = Color.White, modifier = Modifier.size(36.dp)) }
                            IconButton(onClick = onToggleRepeat) { Icon(imageVector = if (repeatMode == 1) Icons.Default.RepeatOne else Icons.Default.Repeat, contentDescription = "Repetir", tint = if (repeatMode > 0) accentColor else Color.White.copy(alpha = 0.5f)) }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Volumen
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeMute, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                            Slider(value = volume, onValueChange = onVolumeChange, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White.copy(alpha = 0.8f), inactiveTrackColor = Color.White.copy(alpha = 0.2f)))
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                        }

                        Spacer(Modifier.height(24.dp))

                        // Botones inferiores
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.weight(1f).liquidGlass(RoundedCornerShape(50), alpha = 0.08f).clickable { showLyricsSheet = true }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Subtitles, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Letras", color = Color.White, style = MaterialTheme.typography.labelLarge) }
                            }
                            Box(modifier = Modifier.weight(1.2f).liquidGlass(RoundedCornerShape(50), alpha = 0.1f).clickable { onToggleAudioPro() }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Headphones, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Audio Pro", color = accentColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
                            }
                            Box(modifier = Modifier.weight(1f).liquidGlass(RoundedCornerShape(50), alpha = 0.08f).clickable { showQueueSheet = true }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.FormatListBulleted, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Cola", color = Color.White, style = MaterialTheme.typography.labelLarge) }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }


    // ─── Bottom Sheets (Letras y Cola) ───
    if (showLyricsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLyricsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) }
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(400.dp).padding(16.dp)) {
                LyricsView(
                    lyrics = lyrics,
                    currentPositionMs = currentPositionMs,
                    isLoading = isLoadingLyrics,
                    accentColor = accentColor,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) }
        ) {
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f).padding(16.dp)) {
                Text("Cola de reproducción", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                LazyColumn {
                    items(queue, key = { it.id }) { item ->
                        QueueItemGlass(
                            song = item,
                            isCurrent = item.id == song.id,
                            tint = accentColor,
                            onClick = { onSelectQueueSong(item); showQueueSheet = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueItemGlass(song: Song, isCurrent: Boolean, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) Color.White.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.3f))) {
            if (song.getEffectiveArtworkUrl().isNotBlank()) {
                AsyncImage(model = song.getEffectiveArtworkUrl(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, color = if (isCurrent) tint else Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun formatDuration(ms: Long): String {
    val s = ms.coerceAtLeast(0L) / 1000
    return "%d:%02d".format(s / 60, s % 60)
}