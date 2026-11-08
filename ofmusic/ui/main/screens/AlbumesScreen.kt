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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.haziel.ofmusic.domain.model.Song
import com.haziel.ofmusic.ui.main.MusicUiState
import com.haziel.ofmusic.ui.main.MusicViewModel
import com.haziel.ofmusic.ui.main.components.ProfileAvatar
import com.haziel.ofmusic.ui.theme.liquidGlass

data class AlbumItemData(
    val title: String,
    val artistName: String,
    val songs: List<Song>,
    val coverUrl: String,
    val isDownloaded: Boolean = false,
    val qualityTag: String = "LOSSLESS ALAC"
)

@Composable
fun AlbumesScreen(
    state: MusicUiState,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onExpandPlayer: () -> Unit,
    onShowDetails: (Song) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Todos") }
    var sortOption by remember { mutableStateOf("Añadidos recientemente") }
    var showSortMenu by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(true) }

    val primaryColor = MaterialTheme.colorScheme.primary

    // Extraer álbumes de las canciones de la biblioteca
    val albumGroups = remember(state.songs, state.downloadedSongMap) {
        val grouped = state.songs.groupBy { it.album }
        grouped.map { (albumTitle, songs) ->
            val hasDownloaded = songs.all { state.downloadedSongMap.containsKey(it.id) || it.isDownloaded }
            val cover = songs.firstOrNull()?.getEffectiveArtworkUrl() ?: ""
            val artist = songs.firstOrNull()?.artist ?: "Artista"

            AlbumItemData(
                title = albumTitle.ifBlank { "Álbum Desconocido" },
                artistName = artist,
                songs = songs,
                coverUrl = cover,
                isDownloaded = hasDownloaded,
                qualityTag = if (hasDownloaded) "LOSSLESS ALAC" else "HI-RES 24-BIT"
            )
        }
    }

    val filteredAlbums = remember(albumGroups, searchQuery, selectedFilter) {
        albumGroups.filter { album ->
            val matchesQuery = searchQuery.isBlank() ||
                    album.title.contains(searchQuery, ignoreCase = true) ||
                    album.artistName.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "Descargados completos" -> album.isDownloaded
                "Hi-Res" -> album.qualityTag.contains("HI-RES") || album.qualityTag.contains("LOSSLESS")
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp, top = 12.dp)
        ) {
            // ─── Header Exacto a la Foto 2 ───
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Álbumes",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Spacer(Modifier.width(6.dp))
                            Box(modifier = Modifier.size(8.dp).background(primaryColor, CircleShape))
                        }
                        Text(
                            text = "${albumGroups.size} álbumes completos",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Cast, contentDescription = null, tint = Color.White)
                        }
                        ProfileAvatar(
                            photoUri = if (state.isLoggedIn) state.userProfile.photoUri else null,
                            name = if (state.isLoggedIn) state.userProfile.name else "U",
                            size = 36.dp
                        )
                    }
                }
            }

            // ─── Sub-Barra con Orden y Vista ───
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .liquidGlass(RoundedCornerShape(50), alpha = 0.10f)
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                                .clickable { showSortMenu = true }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(sortOption, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }

                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Añadidos recientemente") },
                                onClick = { sortOption = "Añadidos recientemente"; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Por Nombre") },
                                onClick = { sortOption = "Por Nombre"; showSortMenu = false }
                            )
                        }
                    }

                    IconButton(
                        onClick = { isGridView = !isGridView },
                        modifier = Modifier.liquidGlass(CircleShape, alpha = 0.1f).size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.GridView else Icons.Default.FormatListBulleted,
                            contentDescription = "Cambiar vista",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ─── Buscador de Álbumes ───
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar álbumes...", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.5f)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color.White.copy(alpha = 0.5f))
                            }
                        } else {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedContainerColor = Color.White.copy(alpha = 0.04f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            // ─── Chips de Filtro ───
            item {
                val filterChips = listOf("Todos", "Descargados completos", "Hi-Res")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filterChips) { chipLabel ->
                        val isSelected = selectedFilter == chipLabel
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .liquidGlass(RoundedCornerShape(50), alpha = if (isSelected) 0.22f else 0.08f)
                                .border(1.dp, if (isSelected) primaryColor else Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                                .clickable { selectedFilter = chipLabel }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (isSelected) "✓ $chipLabel" else chipLabel,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ─── Cuadrícula de 2 Columnas de Álbumes (Foto 2) ───
            if (filteredAlbums.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No hay álbumes guardados en tu biblioteca.", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                    }
                }
            } else {
                val chunkedAlbums = filteredAlbums.chunked(2)
                items(chunkedAlbums) { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (album in pair) {
                            Box(modifier = Modifier.weight(1f)) {
                                AlbumGridCard(
                                    album = album,
                                    primaryColor = primaryColor,
                                    onClick = {
                                        if (album.songs.isNotEmpty()) {
                                            viewModel.playSongsFromList(album.songs)
                                            onExpandPlayer()
                                        }
                                    }
                                )
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumGridCard(
    album: AlbumItemData,
    primaryColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .liquidGlass(RoundedCornerShape(22.dp), alpha = 0.08f)
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            // Portada de Álbum
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                if (album.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = album.coverUrl,
                        contentDescription = album.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Album, contentDescription = null, tint = primaryColor, modifier = Modifier.size(48.dp).align(Alignment.Center))
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = album.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
            }

            Spacer(Modifier.height(2.dp))

            Text(
                text = album.artistName,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${album.songs.size} canciones",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )

                Box(
                    modifier = Modifier
                        .background(primaryColor.copy(alpha = 0.20f), RoundedCornerShape(4.dp))
                        .border(1.dp, primaryColor.copy(alpha = 0.40f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = album.qualityTag,
                        color = primaryColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}