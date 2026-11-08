package com.haziel.ofmusic.ui.main.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.haziel.ofmusic.domain.model.Playlist
import com.haziel.ofmusic.domain.model.Song
import com.haziel.ofmusic.ui.main.MusicUiState
import com.haziel.ofmusic.ui.main.MusicViewModel
import com.haziel.ofmusic.ui.main.components.AddSongsToPlaylistDialog
import com.haziel.ofmusic.ui.main.components.PlaylistNameDialog
import com.haziel.ofmusic.ui.theme.GlassColors
import com.haziel.ofmusic.ui.theme.PaletteHelper
import com.haziel.ofmusic.ui.theme.liquidGlass

@Composable
fun EditarPlaylistScreen(
    playlistId: Long,
    state: MusicUiState,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onExpandPlayer: () -> Unit
) {
    val context = LocalContext.current
    val playlist = state.playlists.find { it.id == playlistId } ?: return

    var showRenameDialog by remember { mutableStateOf(false) }
    var showAddSongsDialog by remember { mutableStateOf(false) }

    // ─── Extraer Paleta Dinámica de la Portada ───
    var glassColors by remember { mutableStateOf<GlassColors?>(null) }
    val effectiveCover = playlist.coverUrl.ifBlank { playlist.songs.firstOrNull()?.getEffectiveArtworkUrl() ?: "" }

    LaunchedEffect(effectiveCover) {
        if (effectiveCover.isNotBlank()) {
            glassColors = PaletteHelper.extractFromUrl(context, effectiveCover)
        } else {
            glassColors = null
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.repository.updatePlaylistCover(playlist.id, uri.toString())
        }
    }

    val defaultBg = Color(0xFF0D0A14)
    val bgColor = glassColors?.background ?: defaultBg
    val dominantColor = glassColors?.dominant ?: Color(0xFF2E1065)
    val accentColor = glassColors?.vibrant ?: MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        dominantColor.copy(alpha = 0.8f),
                        bgColor,
                        Color(0xFF07070A)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // ─── 1. Top Bar Exacta a Figma ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.liquidGlass(CircleShape, alpha = 0.1f).size(40.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                }

                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Editar Playlist",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(modifier = Modifier.size(8.dp).background(accentColor, CircleShape))
                    }
                    Text(
                        text = "Modo reordenación manual",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Button(
                    onClick = onBack,
                    modifier = Modifier.liquidGlass(RoundedCornerShape(50), alpha = 0.15f),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.1f),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text("Listo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 140.dp, start = 16.dp, end = 16.dp, top = 8.dp)
            ) {
                // ─── 2. Tarjeta de Encabezado con Portada e Información ───
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(RoundedCornerShape(24.dp), alpha = 0.1f)
                            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Portada de Playlist
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (effectiveCover.isNotBlank()) {
                                    AsyncImage(
                                        model = effectiveCover,
                                        contentDescription = playlist.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(16.dp))

                            // Nombre, Conteo e Insignia
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { showRenameDialog = true }
                                ) {
                                    Text(
                                        text = playlist.title,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Editar nombre",
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${playlist.songs.size} canciones • ${formatPlaylistDuration(playlist.songs)}",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(50))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (playlist.isPublic) "Pública" else "Privada",
                                            color = accentColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Botón Elegir Color / Portada
                            IconButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.liquidGlass(CircleShape, alpha = 0.1f).size(40.dp)
                            ) {
                                Icon(Icons.Outlined.Palette, contentDescription = "Cambiar portada/color", tint = Color.White)
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }

                // ─── 3. Botones de Acción (+ Agregar Música, Descargar todo) ───
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Botón + Agregar Música
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .liquidGlass(RoundedCornerShape(50), alpha = 0.1f)
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                                .clickable { showAddSongsDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Agregar música", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        val downloadProgress = state.playlistDownloadProgress?.let {
                            if (it.first == playlist.id) it.second else null
                        }
                        val isDownloadingPlaylist = downloadProgress != null

                        // Botón Descargar Todo
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .height(50.dp)
                                .liquidGlass(RoundedCornerShape(50), alpha = 0.12f)
                                .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(50))
                                .clickable(enabled = !isDownloadingPlaylist) {
                                    viewModel.downloadPlaylistSequentially(playlist.id, playlist.songs)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isDownloadingPlaylist) {
                                    CircularProgressIndicator(
                                        color = accentColor,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Descargando $downloadProgress%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                } else {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Descargar todo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }

                // ─── 4. Sub-encabezado Informativo ───
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "ARRASTRA DESDE EL EXTREMO DERECHO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.5f),
                                letterSpacing = 0.8.sp
                            )
                        }
                        Text(
                            text = "${playlist.songs.size} PISTAS MOSTRADAS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.5f),
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }

                // ─── 5. Lista de Pistas con Eliminar y Arrastrar ───
                if (playlist.songs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Esta playlist aún no tiene canciones.",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    itemsIndexed(playlist.songs, key = { _, song -> song.id }) { idx, song ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .liquidGlass(RoundedCornerShape(20.dp), alpha = 0.08f)
                                    .clickable {
                                        viewModel.playPlaylist(playlist, idx)
                                        onExpandPlayer()
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Botón Rojo de Eliminar (-)
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE53935).copy(alpha = 0.2f))
                                        .border(1.dp, Color(0xFFE53935), CircleShape)
                                        .clickable {
                                            viewModel.removeSongFromPlaylist(playlist.id, song.id)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Eliminar de playlist",
                                        tint = Color(0xFFE53935),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(Modifier.width(12.dp))

                                // Portada de Canción
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.3f))
                                ) {
                                    if (song.getEffectiveArtworkUrl().isNotBlank()) {
                                        AsyncImage(
                                            model = song.getEffectiveArtworkUrl(),
                                            contentDescription = song.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Spacer(Modifier.width(12.dp))

                                // Título, Artista e Insignia
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = song.artist,
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (song.isDownloaded) "Lossless Hi-Res" else "HQ",
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.width(8.dp))

                                // Ícono Tirador de Reordenamiento (::)
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Reordenar",
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        PlaylistNameDialog(
            title = "Editar nombre de playlist",
            initialName = playlist.title,
            confirmLabel = "Guardar",
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                viewModel.renamePlaylist(playlist.id, newName)
                showRenameDialog = false
            }
        )
    }

    if (showAddSongsDialog) {
        AddSongsToPlaylistDialog(
            playlist = playlist,
            allSongs = state.songs,
            likedSongIds = state.likedSongIds,
            downloadedSongMap = state.downloadedSongMap,
            onDismiss = { showAddSongsDialog = false },
            onAddSong = { song ->
                viewModel.addSongsToPlaylist(playlist.id, listOf(song))
            },
            onConfirm = { songs ->
                viewModel.addSongsToPlaylist(playlist.id, songs)
            }
        )
    }
}

private fun formatPlaylistDuration(songs: List<Song>): String {
    val totalMs = songs.sumOf { it.durationMs }
    val totalSec = totalMs / 1000
    val min = totalSec / 60
    val hr = min / 60
    return if (hr > 0) {
        "${hr}h ${min % 60}m"
    } else {
        "${min}m"
    }
}