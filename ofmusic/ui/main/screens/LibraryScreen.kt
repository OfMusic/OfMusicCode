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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haziel.ofmusic.domain.model.Song
import com.haziel.ofmusic.ui.main.MusicUiState
import com.haziel.ofmusic.ui.main.MusicViewModel
import com.haziel.ofmusic.ui.main.components.SongItemRow
import com.haziel.ofmusic.ui.theme.liquidGlass

enum class LibrarySortMode { TITLE, ARTIST, ALBUM }

private data class LibraryCategory(
    val title: String,
    val countText: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun LibraryScreenContent(
    state: MusicUiState,
    viewModel: MusicViewModel,
    onExpandPlayer: () -> Unit,
    onShowDetails: (Song) -> Unit = {},
    onOpenPlaylists: () -> Unit = {},
    onOpenArtistas: () -> Unit = {},
    onOpenAlbumes: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Todas") }
    var sortMode by remember { mutableStateOf(LibrarySortMode.TITLE) }
    var showSortMenu by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary

    val likedOrDownloadedSongs = remember(state.songs, state.likedSongIds, state.downloadedSongMap) {
        state.songs.filter {
            state.likedSongIds.contains(it.id) || state.downloadedSongMap.containsKey(it.id) || it.isDownloaded
        }
    }

    val baseList = remember(selectedFilter, likedOrDownloadedSongs) {
        when (selectedFilter) {
            "♥ Solo Me Gusta" -> likedOrDownloadedSongs.filter { state.likedSongIds.contains(it.id) }
            "✓ Solo Descargas" -> likedOrDownloadedSongs.filter { state.downloadedSongMap.containsKey(it.id) || it.isDownloaded }
            "Artistas" -> likedOrDownloadedSongs.distinctBy { it.artist }
            "Álbumes" -> likedOrDownloadedSongs.distinctBy { it.album }
            else -> likedOrDownloadedSongs
        }
    }

    val filteredSongs = remember(baseList, searchQuery) {
        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.artist.contains(searchQuery, ignoreCase = true) ||
                        it.album.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val displaySongs = remember(filteredSongs, sortMode) {
        when (sortMode) {
            LibrarySortMode.TITLE -> filteredSongs.sortedBy { it.title }
            LibrarySortMode.ARTIST -> filteredSongs.sortedBy { it.artist }
            LibrarySortMode.ALBUM -> filteredSongs.sortedBy { it.album }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // ─── Header ───
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Biblioteca",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "TU MÚSICA • Me gusta & Descargas",
                        style = MaterialTheme.typography.labelSmall,
                        color = primaryColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box {
                    TextButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Ordenar", color = primaryColor, fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Por Título") },
                            onClick = { sortMode = LibrarySortMode.TITLE; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Por Artista") },
                            onClick = { sortMode = LibrarySortMode.ARTIST; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Por Álbum") },
                            onClick = { sortMode = LibrarySortMode.ALBUM; showSortMenu = false }
                        )
                    }
                }
            }
        }

        // ─── Categorías Principales (Playlists, Artistas, Álbumes) ───
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CategoryGridCard(
                        title = "Playlists",
                        subtitle = "${state.playlists.size} listas creadas",
                        icon = Icons.Default.QueueMusic,
                        primaryColor = primaryColor,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenPlaylists
                    )
                    CategoryGridCard(
                        title = "Artistas",
                        subtitle = "${state.songs.map { it.artist }.distinct().size} seguidos",
                        icon = Icons.Default.Person,
                        primaryColor = primaryColor,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenArtistas
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CategoryGridCard(
                        title = "Álbumes",
                        subtitle = "${state.songs.map { it.album }.distinct().size} completos",
                        icon = Icons.Default.Album,
                        primaryColor = primaryColor,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenAlbumes
                    )
                }
            }
        }

        // ─── Buscador Único para Me Gusta & Descargas ───
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                placeholder = { Text("Buscar en tus me gusta y descargas...", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.6f)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color.White.copy(alpha = 0.6f))
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedBorderColor = primaryColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )
        }

        // ─── Chips de Filtro de Biblioteca ───
        item {
            val filterChips = listOf("Todas", "♥ Solo Me Gusta", "✓ Solo Descargas", "Artistas", "Álbumes")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterChips) { chipLabel ->
                    val isSelected = selectedFilter == chipLabel
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .liquidGlass(CircleShape, alpha = if (isSelected) 0.25f else 0.08f)
                            .border(1.dp, if (isSelected) primaryColor else Color.White.copy(alpha = 0.15f), CircleShape)
                            .clickable { selectedFilter = chipLabel }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = chipLabel,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // ─── Sub-encabezado de Lista ───
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (selectedFilter) {
                        "♥ Solo Me Gusta" -> "Canciones favoritas (${displaySongs.size})"
                        "✓ Solo Descargas" -> "Canciones descargadas (${displaySongs.size})"
                        "Artistas" -> "Canciones por artista (${displaySongs.size})"
                        "Álbumes" -> "Canciones por álbum (${displaySongs.size})"
                        else -> "Todas las canciones (${displaySongs.size})"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // ─── Lista de Canciones ───
        if (displaySongs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron canciones en este filtro.",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(displaySongs, key = { it.id }) { song ->
                val isCurrent = state.currentSong?.id == song.id

                SongItemRow(
                    song = song,
                    isCurrent = isCurrent,
                    isPlaying = isCurrent && state.isPlaying,
                    isLiked = state.likedSongIds.contains(song.id),
                    isDownloaded = song.isDownloaded,
                    isDownloading = state.downloadingSongIds.contains(song.id),
                    onClick = {
                        val idx = displaySongs.indexOfFirst { it.id == song.id }
                        viewModel.playSongsFromList(displaySongs, idx.coerceAtLeast(0))
                    },
                    onShowDetails = { onShowDetails(song) },
                    onLikeToggle = { viewModel.toggleLike(song) },
                    onDownloadToggle = { viewModel.downloadSong(song) }
                )
            }
        }
    }
}

@Composable
private fun CategoryGridCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    primaryColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(84.dp)
            .liquidGlass(RoundedCornerShape(18.dp), alpha = 0.10f)
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = 0.20f))
                    .border(1.dp, primaryColor.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            }
        }
    }
}