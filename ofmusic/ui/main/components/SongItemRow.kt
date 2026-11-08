package com.haziel.ofmusic.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.haziel.ofmusic.domain.model.Song

@Composable
fun SongItemRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isLiked: Boolean = song.isLiked,
    isDownloaded: Boolean = song.isDownloaded,
    isDownloading: Boolean = false,
    showDeleteDownload: Boolean = false,
    onShowDetails: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onClick: () -> Unit,
    onLikeToggle: () -> Unit = {},
    onAddToQueue: (() -> Unit)? = null,
    onDownloadToggle: (() -> Unit)? = null,
    onDeleteDownload: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isCurrent) MaterialTheme.colorScheme.surface
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ─── Portada tipo Vinilo ───
        VinylArtwork(
            imageUrl = song.getEffectiveArtworkUrl(),
            isPlaying = isPlaying, // Solo gira si es la canción actual y está reproduciéndose
            size = 54.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        // ─── Título y Artista ───
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Badge de "Offline" si está descargada (como en tu Figma)
                if (isDownloaded) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• Offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ─── Duración (3:42) ───
        if (song.durationMs > 0) {
            Text(
                text = formatDuration(song.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        // ─── Botones de acción ───
        if (onRemoveFromPlaylist != null) {
            IconButton(onClick = onRemoveFromPlaylist) {
                Icon(
                    imageVector = Icons.Default.RemoveCircleOutline,
                    contentDescription = "Quitar de la playlist",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(22.dp)
                )
            }
        } else if (onShowDetails != null) {
            IconButton(onClick = onShowDetails) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Ver detalles",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}