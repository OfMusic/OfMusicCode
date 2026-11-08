package com.haziel.ofmusic.ui.main.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.haziel.ofmusic.domain.model.Song
import com.haziel.ofmusic.ui.theme.GlassColors
import com.haziel.ofmusic.ui.theme.PaletteHelper
import com.haziel.ofmusic.ui.theme.liquidGlass

@Composable
fun UnifiedMiniPlayer(
    song: Song,
    isPlaying: Boolean,
    isBuffering: Boolean = false,
    playbackPositionMs: Long,
    playbackDurationMs: Long,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLiked: Boolean = song.isLiked,
    onLikeToggle: (() -> Unit)? = null,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val context = LocalContext.current

    // Extraer colores dinámicos de la portada
    var glassColors by remember { mutableStateOf<GlassColors?>(null) }
    LaunchedEffect(song.artworkUrl) {
        glassColors = PaletteHelper.extractFromUrl(context, song.getEffectiveArtworkUrl())
    }

    val dominantColor = glassColors?.dominant ?: Color(0xFF1E1035)
    val accentColor = glassColors?.vibrant ?: MaterialTheme.colorScheme.primary

    val safeDuration = if (playbackDurationMs > 0L) playbackDurationMs else song.durationMs.coerceAtLeast(1L)
    val progressFraction = (playbackPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    with(sharedTransitionScope) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(24.dp))
                .background(dominantColor.copy(alpha = 0.88f))
                .liquidGlass(RoundedCornerShape(24.dp), alpha = 0.12f)
                .border(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(24.dp)
                )
                .clickable(onClick = onClick)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ─── Portada tipo Vinilo Giratorio ───
                    VinylArtwork(
                        imageUrl = song.getEffectiveArtworkUrl(),
                        isPlaying = isPlaying,
                        size = 46.dp,
                        modifier = Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState(key = "album_art_${song.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Título y Artista
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${song.artist} • ${if (song.isDownloaded) "Offline" else "Hi-Fi"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Me gusta
                    if (onLikeToggle != null) {
                        IconButton(onClick = onLikeToggle, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                tint = if (isLiked) accentColor else Color.White.copy(alpha = 0.8f),
                                contentDescription = "Me gusta",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Play/Pausa (LiquidGlass)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .shadow(elevation = 8.dp, shape = CircleShape, ambientColor = accentColor, spotColor = accentColor)
                            .liquidGlass(CircleShape, alpha = 0.25f)
                            .background(accentColor.copy(alpha = 0.25f), CircleShape)
                            .clickable(onClick = onTogglePlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isBuffering && !isPlaying) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Siguiente
                    IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Siguiente",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Barra de progreso (Acento)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction)
                            .background(accentColor)
                    )
                }
            }
        }
    }
}