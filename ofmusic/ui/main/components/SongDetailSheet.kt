package com.haziel.ofmusic.ui.main.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.haziel.ofmusic.ui.theme.liquidGlass
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.haziel.ofmusic.domain.model.Playlist
import com.haziel.ofmusic.domain.model.Song
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun SongDetailSheet(
    song: Song,
    isLiked: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onLikeToggle: () -> Unit,
    onPlayNow: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: (Long) -> Unit,
    onDownload: () -> Unit,
    onRepair: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var showPlaylistPicker by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var glassColors by remember { mutableStateOf<com.haziel.ofmusic.ui.theme.GlassColors?>(null) }
    LaunchedEffect(song.getEffectiveArtworkUrl()) {
        glassColors = com.haziel.ofmusic.ui.theme.PaletteHelper.extractFromUrl(context, song.getEffectiveArtworkUrl())
    }

    val colors = glassColors ?: com.haziel.ofmusic.ui.theme.GlassColors(
        dominant = Color(0xFF2E1065),
        vibrant = MaterialTheme.colorScheme.primary,
        background = Color(0xFF0D0A14),
        surfaceTint = MaterialTheme.colorScheme.primary
    )

    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = colors.vibrant

    LaunchedEffect(Unit) { isVisible = true }
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            delay(320)
            onDismiss()
        }
    }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (isVisible) (1f - (dragOffsetY / 1000f)).coerceIn(0f, 1f) * 0.7f else 0f,
        animationSpec = tween(200),
        label = "scrimAlpha"
    )

    Dialog(
        onDismissRequest = { /* Solo cerramos con el handle */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
            )

            // Sheet
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(380, easing = FastOutSlowInEasing)),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeOut(tween(200)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.90f)
                        .offset { IntOffset(0, dragOffsetY.roundToInt()) }
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .background(colors.background)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    colors.vibrant.copy(alpha = 0.85f),
                                    colors.dominant.copy(alpha = 0.50f),
                                    colors.background,
                                    Color(0xFF07070A)
                                )
                            )
                        )
                ) {
                    // Degradado sutil superior (Púrpura)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.4f)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(primaryColor.copy(alpha = 0.15f), Color.Transparent)
                                )
                            )
                    )

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Drag handle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onDragEnd = { if (dragOffsetY > 150f) { isVisible = false; dragOffsetY = 0f } else { dragOffsetY = 0f } },
                                        onDragCancel = { dragOffsetY = 0f },
                                        onVerticalDrag = { change, delta -> change.consume(); dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f) }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(2.5.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                            )
                        }

                        // Portada con sombra (Redondeada)
                        Box(
                            modifier = Modifier
                                .padding(top = 16.dp, bottom = 24.dp)
                                .size(280.dp)
                                .graphicsLayer {
                                    shadowElevation = 30f
                                    shape = RoundedCornerShape(24.dp)
                                    clip = true
                                }
                                .clip(RoundedCornerShape(24.dp))
                                .background(surfaceColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (song.getEffectiveArtworkUrl().isNotBlank()) {
                                AsyncImage(
                                    model = song.getEffectiveArtworkUrl(),
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(80.dp), tint = primaryColor)
                            }
                        }

                        // Info canción
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(24.dp))

                        // Lista de Acciones
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            item {
                                DetailActionRow(
                                    icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    label = if (isLiked) "Quitar de Me gusta" else "Me gusta",
                                    tint = if (isLiked) Color(0xFFE91E63) else primaryColor,
                                    onClick = onLikeToggle
                                )
                            }
                            item {
                                DetailActionRow(
                                    icon = Icons.Default.PlayArrow,
                                    label = "Reproducir ahora",
                                    tint = primaryColor,
                                    onClick = onPlayNow
                                )
                            }
                            item {
                                DetailActionRow(
                                    icon = Icons.Default.QueueMusic,
                                    label = "Añadir a la cola",
                                    tint = primaryColor,
                                    onClick = onAddToQueue
                                )
                            }
                            item {
                                DetailActionRow(
                                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                                    label = "Añadir a lista de reproducción",
                                    tint = primaryColor,
                                    onClick = { showPlaylistPicker = true }
                                )
                            }
                            item {
                                when {
                                    isDownloading -> DetailActionRow(
                                        icon = Icons.Default.Download,
                                        label = "Descargando...",
                                        tint = primaryColor.copy(alpha = 0.5f),
                                        enabled = false,
                                        onClick = {}
                                    )
                                    isDownloaded -> DetailActionRow(
                                        icon = Icons.Default.DownloadDone,
                                        label = "Descargado",
                                        tint = primaryColor,
                                        onClick = onDownload
                                    )
                                    else -> DetailActionRow(
                                        icon = Icons.Default.Download,
                                        label = "Descargar",
                                        tint = primaryColor,
                                        onClick = onDownload
                                    )
                                }
                            }
                            item {
                                DetailActionRow(
                                    icon = Icons.Default.Build,
                                    label = "Reparar canción",
                                    tint = primaryColor,
                                    onClick = onRepair
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPlaylistPicker) {
        PlaylistPickerDialog(
            playlists = playlists,
            onDismiss = { showPlaylistPicker = false },
            onSelect = { playlistId -> onAddToPlaylist(playlistId); showPlaylistPicker = false }
        )
    }
}

// ─── Componentes ───

@Composable
private fun DetailActionRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .liquidGlass(RoundedCornerShape(18.dp), alpha = 0.12f)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.25f),
                        tint.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.18f))
                    .border(1.dp, tint.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) tint else tint.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PlaylistPickerDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Añadir a playlist",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))

                if (playlists.isEmpty()) {
                    Text(
                        text = "No tienes playlists creadas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(playlists, key = { it.id }) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.background)
                                    .clickable { onSelect(playlist.id) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.05f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val cover = playlist.coverUrl.ifBlank { playlist.songs.firstOrNull()?.getEffectiveArtworkUrl() ?: "" }
                                    if (cover.isNotBlank()) {
                                        AsyncImage(model = cover, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    } else {
                                        Icon(Icons.Default.Album, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(playlist.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${playlist.songs.size} canciones", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}