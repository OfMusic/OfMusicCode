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

data class ArtistItemData(
    val name: String,
    val genreText: String,
    val songs: List<Song>,
    val avatarUrl: String,
    val isVerified: Boolean = true,
    val statusTag: String = ""
)

@Composable
fun ArtistasScreen(
    state: MusicUiState,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onExpandPlayer: () -> Unit,
    onShowDetails: (Song) -> Unit = {},
    onOpenArtistDetail: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Todos") }
    var isSortAscending by remember { mutableStateOf(true) }

    val primaryColor = MaterialTheme.colorScheme.primary

    // Extraer artistas reales de las canciones de la biblioteca
    val artistGroups = remember(state.songs, state.downloadedSongMap, state.currentSong) {
        val grouped = state.songs.groupBy { it.artist }
        grouped.map { (artistName, songs) ->
            val hasDownloaded = songs.any { state.downloadedSongMap.containsKey(it.id) || it.isDownloaded }
            val isCurrentlyPlaying = state.currentSong?.artist == artistName && state.isPlaying

            val tag = when {
                isCurrentlyPlaying -> "● SONANDO"
                hasDownloaded -> "✓ Descargado"
                songs.size >= 3 -> "✓ ${songs.map { it.album }.distinct().size} álbumes en tu biblioteca"
                else -> "✓ ${songs.size} canciones guardadas"
            }

            val artwork = songs.firstOrNull()?.getEffectiveArtworkUrl() ?: ""

            ArtistItemData(
                name = artistName,
                genreText = "Top 10 Global • ${songs.size} canciones guardadas",
                songs = songs,
                avatarUrl = artwork,
                isVerified = true,
                statusTag = tag
            )
        }
    }

    val filteredArtists = remember(artistGroups, searchQuery, selectedFilter) {
        artistGroups.filter { artist ->
            val matchesQuery = searchQuery.isBlank() || artist.name.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "Verificados" -> artist.isVerified
                "Con descargas" -> artist.statusTag.contains("Descargado") || artist.statusTag.contains("completa")
                else -> true
            }
            matchesQuery && matchesFilter
        }.let { list ->
            if (isSortAscending) list.sortedBy { it.name } else list.sortedByDescending { it.name }
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
            // ─── Header Exacto a la Foto ───
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
                        Text(
                            text = "Artistas",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "${artistGroups.size} artistas seguidos • Actualizado hoy",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .liquidGlass(RoundedCornerShape(50), alpha = 0.12f)
                            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
                            .clickable { isSortAscending = !isSortAscending }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SwapVert, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (isSortAscending) "A-Z" else "Z-A", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ─── Buscador de Artistas ───
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar artistas seguidos...", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp) },
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
                val filterChips = listOf("Todos ${artistGroups.size}", "Verificados", "Con descargas")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filterChips) { chipLabel ->
                        val isSelected = selectedFilter.startsWith(chipLabel.substringBefore(" ")) || selectedFilter == chipLabel
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .liquidGlass(RoundedCornerShape(50), alpha = if (isSelected) 0.22f else 0.08f)
                                .border(1.dp, if (isSelected) primaryColor else Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                                .clickable {
                                    selectedFilter = if (chipLabel.startsWith("Todos")) "Todos" else chipLabel
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (chipLabel.startsWith("Todos")) "✓ $chipLabel" else chipLabel,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ─── Lista de Artistas ───
            if (filteredArtists.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No se encontraron artistas seguidos.", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                    }
                }
            } else {
                items(filteredArtists, key = { it.name }) { artist ->
                    ArtistCardRow(
                        artist = artist,
                        primaryColor = primaryColor,
                        onCardClick = { onOpenArtistDetail(artist.name) },
                        onPlay = {
                            if (artist.songs.isNotEmpty()) {
                                viewModel.playSongsFromList(artist.songs)
                                onExpandPlayer()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistCardRow(
    artist: ArtistItemData,
    primaryColor: Color,
    onCardClick: () -> Unit,
    onPlay: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .liquidGlass(RoundedCornerShape(20.dp), alpha = 0.08f)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .clickable(onClick = onCardClick)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Avatar con insignia verificada
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.2f))
                        .border(1.dp, primaryColor.copy(alpha = 0.4f), CircleShape)
                ) {
                    if (artist.avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = artist.avatarUrl,
                            contentDescription = artist.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        ProfileAvatar(photoUri = null, name = artist.name, size = 56.dp)
                    }
                }

                if (artist.isVerified) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(primaryColor)
                            .border(1.5.dp, Color.Black, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Verificado", tint = Color.White, modifier = Modifier.size(11.dp))
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = artist.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (artist.statusTag.contains("SONANDO")) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(primaryColor, RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("● SONANDO", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = artist.genreText,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (artist.statusTag.isNotBlank() && !artist.statusTag.contains("SONANDO")) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = artist.statusTag,
                        color = if (artist.statusTag.contains("Descargad")) primaryColor else Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(primaryColor)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Reproducir artista", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}