package com.haziel.ofmusic.ui.main.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.haziel.ofmusic.ui.main.components.DeletePlaylistConfirmDialog
import com.haziel.ofmusic.ui.main.components.PlaylistNameDialog
import com.haziel.ofmusic.ui.theme.liquidGlass

enum class PlaylistSortOption { RECENT, TITLE, SONG_COUNT }

@Composable
fun PlaylistsScreenContent(
    state: MusicUiState,
    viewModel: MusicViewModel,
    onBack: () -> Unit = {},
    onExpandPlayer: () -> Unit,
    onShowDetails: (Song) -> Unit,
    onOpenNewPlaylist: () -> Unit = {},
    onOpenEditPlaylist: (Playlist) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Todas") }
    var sortOption by remember { mutableStateOf(PlaylistSortOption.RECENT) }
    var showSortMenu by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(true) }

    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var playlistToAddSongs by remember { mutableStateOf<Playlist?>(null) }
    var menuPlaylistId by remember { mutableStateOf<Long?>(null) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.background

    // ─── Filtrado y Orden de Playlists ───
    val filteredPlaylists = remember(state.playlists, searchQuery, selectedFilter) {
        state.playlists.filter { playlist ->
            val matchesQuery = searchQuery.isBlank() ||
                    playlist.title.contains(searchQuery, ignoreCase = true) ||
                    playlist.description.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "Creadas por mí" -> playlist.isUserCreated
                "♥ Favoritas" -> playlist.title.contains("favorit", ignoreCase = true) ||
                        playlist.songs.any { state.likedSongIds.contains(it.id) }
                "✓ Descargadas" -> playlist.downloadOffline || playlist.songs.all { it.isDownloaded }
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    val displayPlaylists = remember(filteredPlaylists, sortOption) {
        when (sortOption) {
            PlaylistSortOption.RECENT -> filteredPlaylists.sortedByDescending { it.updatedAt }
            PlaylistSortOption.TITLE -> filteredPlaylists.sortedBy { it.title }
            PlaylistSortOption.SONG_COUNT -> filteredPlaylists.sortedByDescending { it.songs.size }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp, top = 12.dp)
        ) {
            // ─── Top Bar Exacta a Figma ───
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.liquidGlass(CircleShape, alpha = 0.1f).size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Regresar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(
                            text = "BIBLIOTECA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Playlists",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .liquidGlass(RoundedCornerShape(50), alpha = 0.15f)
                            .border(
                                width = 1.dp,
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.35f),
                                        primaryColor.copy(alpha = 0.40f),
                                        Color.White.copy(alpha = 0.10f)
                                    )
                                ),
                                shape = RoundedCornerShape(50)
                            )
                            .clickable(onClick = onOpenNewPlaylist)
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Nueva", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // ─── Barra de Sincronización y Contador ───
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(primaryColor, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${state.playlists.size} listas creadas y guardadas",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CloudDone,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Sincronizado",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // ─── Buscador y Botón de Orden ───
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar en mis playlists...", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.5f)) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedContainerColor = Color.White.copy(alpha = 0.04f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(Modifier.width(10.dp))

                    Box {
                        Box(
                            modifier = Modifier
                                .height(50.dp)
                                .liquidGlass(RoundedCornerShape(24.dp), alpha = 0.08f)
                                .clickable { showSortMenu = true }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Orden reciente", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Reciente") },
                                onClick = { sortOption = PlaylistSortOption.RECENT; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Por Nombre") },
                                onClick = { sortOption = PlaylistSortOption.TITLE; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Número de canciones") },
                                onClick = { sortOption = PlaylistSortOption.SONG_COUNT; showSortMenu = false }
                            )
                        }
                    }
                }
            }

            // ─── Chips de Filtro ───
            item {
                val filterList = listOf("Todas", "Creadas por mí", "♥ Favoritas", "✓ Descargadas")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filterList) { filterLabel ->
                        val isSelected = selectedFilter == filterLabel
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .liquidGlass(RoundedCornerShape(50), alpha = if (isSelected) 0.22f else 0.08f)
                                .border(1.dp, if (isSelected) primaryColor else Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                                .clickable { selectedFilter = filterLabel }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = filterLabel,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ─── Sub-encabezado "Mis Playlists" ───
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mis Playlists",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isGridView) "Ver lista" else "Ver cuadrícula",
                            fontSize = 12.sp,
                            color = primaryColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { isGridView = !isGridView }
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = if (isGridView) Icons.Default.GridView else Icons.Default.FormatListBulleted,
                            contentDescription = "Cambiar vista",
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ─── Lista / Cuadrícula de Playlists ───
            if (displayPlaylists.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay playlists en esta categoría.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else if (isGridView) {
                val chunkedPlaylists = displayPlaylists.chunked(2)
                items(chunkedPlaylists) { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (playlist in pair) {
                            Box(modifier = Modifier.weight(1f)) {
                                PlaylistGridCard(
                                    playlist = playlist,
                                    primaryColor = primaryColor,
                                    onCardClick = { onOpenEditPlaylist(playlist) },
                                    onPlay = {
                                        if (playlist.songs.isNotEmpty()) {
                                            viewModel.playPlaylist(playlist)
                                            onExpandPlayer()
                                        }
                                    },
                                    onMenuClick = { menuPlaylistId = playlist.id }
                                )
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                items(displayPlaylists, key = { it.id }) { playlist ->
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        PlaylistRowCard(
                            playlist = playlist,
                            primaryColor = primaryColor,
                            onCardClick = { onOpenEditPlaylist(playlist) },
                            onPlay = {
                                if (playlist.songs.isNotEmpty()) {
                                    viewModel.playPlaylist(playlist)
                                    onExpandPlayer()
                                }
                            },
                            onMenuClick = { menuPlaylistId = playlist.id }
                        )
                    }
                }
            }
        }
    }

    // ─── Dropdown Menu de Opciones de Playlist ───
    menuPlaylistId?.let { targetId ->
        val playlist = state.playlists.find { it.id == targetId }
        playlist?.let { targetPlaylist ->
            DropdownMenu(
                expanded = true,
                onDismissRequest = { menuPlaylistId = null }
            ) {
                DropdownMenuItem(
                    text = { Text("Editar / Ver detalles") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = {
                        menuPlaylistId = null
                        onOpenEditPlaylist(targetPlaylist)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Reproducir") },
                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    onClick = {
                        menuPlaylistId = null
                        if (targetPlaylist.songs.isNotEmpty()) {
                            viewModel.playPlaylist(targetPlaylist)
                            onExpandPlayer()
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Agregar canciones") },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = {
                        menuPlaylistId = null
                        playlistToAddSongs = targetPlaylist
                    }
                )
                DropdownMenuItem(
                    text = { Text("Renombrar") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = {
                        menuPlaylistId = null
                        playlistToRename = targetPlaylist
                    }
                )
                DropdownMenuItem(
                    text = { Text("Eliminar") },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    onClick = {
                        menuPlaylistId = null
                        playlistToDelete = targetPlaylist
                    }
                )
            }
        }
    }

    playlistToRename?.let { playlist ->
        PlaylistNameDialog(
            title = "Renombrar playlist",
            initialName = playlist.title,
            confirmLabel = "Guardar",
            onDismiss = { playlistToRename = null },
            onConfirm = { newName ->
                viewModel.renamePlaylist(playlist.id, newName)
                playlistToRename = null
            }
        )
    }

    playlistToDelete?.let { playlist ->
        DeletePlaylistConfirmDialog(
            playlistTitle = playlist.title,
            onDismiss = { playlistToDelete = null },
            onConfirm = {
                viewModel.deletePlaylist(playlist.id)
                playlistToDelete = null
            }
        )
    }

    playlistToAddSongs?.let { playlist ->
        AddSongsToPlaylistDialog(
            playlist = playlist,
            allSongs = state.songs,
            likedSongIds = state.likedSongIds,
            downloadedSongMap = state.downloadedSongMap,
            onDismiss = { playlistToAddSongs = null },
            onAddSong = { song ->
                viewModel.addSongsToPlaylist(playlist.id, listOf(song))
            },
            onConfirm = { songs ->
                viewModel.addSongsToPlaylist(playlist.id, songs)
            }
        )
    }
}

@Composable
private fun PlaylistGridCard(
    playlist: Playlist,
    primaryColor: Color,
    onCardClick: () -> Unit,
    onPlay: () -> Unit,
    onMenuClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(RoundedCornerShape(20.dp), alpha = 0.08f)
            .clickable(onClick = onCardClick)
            .padding(10.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                val cover = playlist.coverUrl.ifBlank { playlist.songs.firstOrNull()?.getEffectiveArtworkUrl() ?: "" }
                if (cover.isNotBlank()) {
                    AsyncImage(
                        model = cover,
                        contentDescription = playlist.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.QueueMusic,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.align(Alignment.Center).size(36.dp)
                    )
                }

                if (playlist.downloadOffline) {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopStart)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                            .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = primaryColor, modifier = Modifier.size(10.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("OFFLINE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .liquidGlass(CircleShape, alpha = 0.1f)
                        .clickable(onClick = onPlay),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproducir",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = playlist.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onMenuClick, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Opciones",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = "${playlist.songs.size} canciones • ${if (playlist.downloadOffline) "Descargado" else "En línea"}",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PlaylistRowCard(
    playlist: Playlist,
    primaryColor: Color,
    onCardClick: () -> Unit,
    onPlay: () -> Unit,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(RoundedCornerShape(16.dp), alpha = 0.08f)
            .clickable(onClick = onCardClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            val cover = playlist.coverUrl.ifBlank { playlist.songs.firstOrNull()?.getEffectiveArtworkUrl() ?: "" }
            if (cover.isNotBlank()) {
                AsyncImage(model = cover, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Default.QueueMusic, contentDescription = null, tint = primaryColor, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = playlist.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (playlist.downloadOffline) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = primaryColor, modifier = Modifier.size(14.dp))
                }
            }
            Text(
                text = "${playlist.songs.size} canciones • ${if (playlist.description.isNotBlank()) playlist.description else "Lossless Audio"}",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onMenuClick) {
            Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.White.copy(alpha = 0.6f))
        }
    }
}