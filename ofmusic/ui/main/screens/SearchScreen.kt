package com.haziel.ofmusic.ui.main.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haziel.ofmusic.ui.theme.liquidGlass
import coil.compose.AsyncImage
import com.haziel.ofmusic.domain.model.Song
import com.haziel.ofmusic.ui.main.MusicUiState
import com.haziel.ofmusic.ui.main.MusicViewModel
import com.haziel.ofmusic.ui.main.components.SongItemRow
import com.haziel.ofmusic.ui.theme.liquidGlass

@Composable
fun SearchScreenContent(
    state: MusicUiState,
    viewModel: MusicViewModel,
    onExpandPlayer: () -> Unit,
    onShowDetails: (Song) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<String?>(null) }

    // Manejador de búsqueda con Debounce básico
    LaunchedEffect(searchQuery) {
        viewModel.search(searchQuery)
    }

    val rawResults = state.searchResults
    val filteredResults = remember(rawResults, selectedFilter, state.downloadedSongMap) {
        when (selectedFilter) {
            "Solo Descargados" -> rawResults.filter { state.downloadedSongMap.containsKey(it.id) || it.isDownloaded }
            else -> rawResults
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // ─── Header y Barra de Búsqueda ───
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = "Explorar catálogo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Buscar",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Artistas oficiales, canciones...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    singleLine = true
                )

                Spacer(Modifier.height(16.dp))

                // Chips de Filtro
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SearchChip("Solo Descargados", selectedFilter == "Solo Descargados") {
                        selectedFilter = if (selectedFilter == "Solo Descargados") null else "Solo Descargados"
                    }
                    SearchChip("Con Letras", selectedFilter == "Con Letras") {
                        selectedFilter = if (selectedFilter == "Con Letras") null else "Con Letras"
                        if (selectedFilter == "Con Letras" && searchQuery.isNotBlank() && !searchQuery.contains("letra", ignoreCase = true)) {
                            searchQuery = "$searchQuery letra"
                        }
                    }
                    SearchChip("Hi-Res Audio", selectedFilter == "Hi-Res Audio") {
                        selectedFilter = if (selectedFilter == "Hi-Res Audio") null else "Hi-Res Audio"
                    }
                }
            }
        }

        // ─── Resultados o Categorías ───
        if (searchQuery.isNotBlank()) {
            if (state.isSearching) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (filteredResults.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No se encontraron resultados oficiales para \"$searchQuery\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(filteredResults) { song ->
                    val isCurrent = state.currentSong?.id == song.id
                    SongItemRow(
                        song = song,
                        isCurrent = isCurrent,
                        isPlaying = isCurrent && state.isPlaying,
                        onClick = { viewModel.playSongsFromList(filteredResults, filteredResults.indexOf(song)) },
                        onShowDetails = { onShowDetails(song) }
                    )
                }
            }
        } else {
            // Mostrar géneros si no hay búsqueda
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Explora por Géneros",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Box(
                        modifier = Modifier
                            .liquidGlass(RoundedCornerShape(50), alpha = 0.2f)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(50))
                            .clickable { viewModel.generateQueueByGenreAndTaste("Top Éxitos") }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("Cola Inteligente", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GenreCard(
                            title = "Éxitos 2025",
                            subtitle = "Top Global",
                            coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80",
                            brush = Brush.linearGradient(listOf(Color(0xFF4C1D95), Color(0xFF2E1065))),
                            modifier = Modifier.weight(1f),
                            onGenerateQueue = { viewModel.generateQueueByGenreAndTaste("Éxitos 2025") },
                            onClick = { searchQuery = "Éxitos 2025" }
                        )
                        GenreCard(
                            title = "Urbano & Trap",
                            subtitle = "Ritmos de fuego",
                            coverUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=500&q=80",
                            brush = Brush.linearGradient(listOf(Color(0xFF831843), Color(0xFF4C0519))),
                            modifier = Modifier.weight(1f),
                            onGenerateQueue = { viewModel.generateQueueByGenreAndTaste("Urbano & Trap") },
                            onClick = { searchQuery = "Urbano Trap" }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GenreCard(
                            title = "Electrónica",
                            subtitle = "Pura inmersión",
                            coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&q=80",
                            brush = Brush.linearGradient(listOf(Color(0xFF1E3A8A), Color(0xFF172554))),
                            modifier = Modifier.weight(1f),
                            onGenerateQueue = { viewModel.generateQueueByGenreAndTaste("Electrónica EDM") },
                            onClick = { searchQuery = "Electrónica" }
                        )
                        GenreCard(
                            title = "Pop Cósmico",
                            subtitle = "Melodías estelares",
                            coverUrl = "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=500&q=80",
                            brush = Brush.linearGradient(listOf(Color(0xFF9D174D), Color(0xFF500724))),
                            modifier = Modifier.weight(1f),
                            onGenerateQueue = { viewModel.generateQueueByGenreAndTaste("Pop") },
                            onClick = { searchQuery = "Pop" }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GenreCard(
                            title = "Rock & Alt",
                            subtitle = "Guitarras legendarias",
                            coverUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=500&q=80",
                            brush = Brush.linearGradient(listOf(Color(0xFF7C2D12), Color(0xFF451A03))),
                            modifier = Modifier.weight(1f),
                            onGenerateQueue = { viewModel.generateQueueByGenreAndTaste("Rock Alternativo") },
                            onClick = { searchQuery = "Rock" }
                        )
                        GenreCard(
                            title = "R&B & Chill",
                            subtitle = "Vibras suaves",
                            coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&q=80",
                            brush = Brush.linearGradient(listOf(Color(0xFF065F46), Color(0xFF022C22))),
                            modifier = Modifier.weight(1f),
                            onGenerateQueue = { viewModel.generateQueueByGenreAndTaste("R&B Chill") },
                            onClick = { searchQuery = "R&B Chill" }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(label, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun GenreCard(
    title: String,
    subtitle: String,
    coverUrl: String,
    brush: Brush,
    modifier: Modifier = Modifier,
    onGenerateQueue: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(115.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(brush)
            .clickable(onClick = onClick)
    ) {
        // Imagen de portada de fondo con opacidad
        AsyncImage(
            model = coverUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.45f }
        )

        // Overlay líquido oscuro
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.Bottom, modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(0.7f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(subtitle, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }

            // Botón rápido para generar cola del género + Me Gusta
            IconButton(
                onClick = onGenerateQueue,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Generar cola",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}