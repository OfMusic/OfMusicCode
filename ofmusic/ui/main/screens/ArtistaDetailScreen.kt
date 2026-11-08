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
import androidx.compose.ui.graphics.Brush
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
import com.haziel.ofmusic.ui.main.components.SongItemRow
import com.haziel.ofmusic.ui.theme.liquidGlass

data class TourDateData(
    val month: String,
    val day: String,
    val city: String,
    val venue: String,
    val time: String
)

@Composable
fun ArtistaDetailScreen(
    artistName: String,
    state: MusicUiState,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onExpandPlayer: () -> Unit,
    onShowDetails: (Song) -> Unit = {}
) {
    var isFollowing by remember { mutableStateOf(true) }
    var discographyFilter by remember { mutableStateOf("Álbumes") }
    var showTop10 by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary

    // Obtener canciones del artista desde state.songs
    val artistSongs = remember(state.songs, artistName) {
        val found = state.songs.filter { it.artist.contains(artistName, ignoreCase = true) || artistName.contains(it.artist, ignoreCase = true) }
        if (found.isNotEmpty()) found else state.songs.take(8)
    }

    val heroSong = artistSongs.firstOrNull()
    val topSongs = if (showTop10) artistSongs.take(10) else artistSongs.take(5)

    val albumGroups = remember(artistSongs) {
        artistSongs.groupBy { it.album }
    }

    val tourDates = remember {
        listOf(
            TourDateData("MAR", "28", "Ciudad de México", "Auditorio Nacional", "20:30 hrs"),
            TourDateData("ABR", "15", "Buenos Aires", "Movistar Arena", "21:00 hrs"),
            TourDateData("MAY", "04", "Madrid", "WiZink Center", "21:30 hrs")
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            // ─── Header & Banner Principal (Foto 1) ───
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    // Imagen del artista de fondo
                    if (heroSong?.getEffectiveArtworkUrl()?.isNotBlank() == true) {
                        AsyncImage(
                            model = heroSong.getEffectiveArtworkUrl(),
                            contentDescription = artistName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Overlay oscuro degradado
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.4f),
                                        Color.Black.copy(alpha = 0.85f),
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )

                    // Top Bar sobre la imagen
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.liquidGlass(CircleShape, alpha = 0.18f).size(40.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        Row {
                            IconButton(onClick = {}, modifier = Modifier.liquidGlass(CircleShape, alpha = 0.18f).size(40.dp)) {
                                Icon(Icons.Default.Share, contentDescription = "Compartir", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = {}, modifier = Modifier.liquidGlass(CircleShape, alpha = 0.18f).size(40.dp)) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Nombre e información sobre la portada
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(primaryColor.copy(alpha = 0.25f), RoundedCornerShape(50))
                                .border(1.dp, primaryColor, RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("ARTISTA VERIFICADO", color = primaryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = artistName,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )

                        Spacer(Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(primaryColor, CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text("4.8M oyentes mensuales", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            Spacer(Modifier.width(12.dp))
                            Text("📊 Top 10 Global Electrónica", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        }
                    }
                }
            }

            // ─── Botones de Acción (Siguiendo, Radio, Shuffle, Play) ───
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .liquidGlass(RoundedCornerShape(50), alpha = if (isFollowing) 0.25f else 0.10f)
                            .border(1.dp, if (isFollowing) primaryColor else Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                            .clickable { isFollowing = !isFollowing }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (isFollowing) Icons.Default.Check else Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isFollowing) "Siguiendo" else "Seguir", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    IconButton(
                        onClick = { viewModel.generateLikedBasedRadio() },
                        modifier = Modifier.liquidGlass(CircleShape, alpha = 0.12f).size(42.dp)
                    ) {
                        Icon(Icons.Default.Radio, contentDescription = "Radio", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    Spacer(Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (artistSongs.isNotEmpty()) {
                                viewModel.playSongsFromList(artistSongs.shuffled())
                                onExpandPlayer()
                            }
                        },
                        modifier = Modifier.liquidGlass(CircleShape, alpha = 0.12f).size(42.dp)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Aleatorio", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    Spacer(Modifier.weight(1f))

                    IconButton(
                        onClick = {
                            if (artistSongs.isNotEmpty()) {
                                viewModel.playSongsFromList(artistSongs, 0)
                                onExpandPlayer()
                            }
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(primaryColor)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Reproducir artista", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }

            // ─── Último Lanzamiento ───
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Último Lanzamiento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("VER TODO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                    }

                    Spacer(Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(RoundedCornerShape(20.dp), alpha = 0.10f)
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                            .clickable {
                                heroSong?.let {
                                    viewModel.playSongSingle(it)
                                    onExpandPlayer()
                                }
                            }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                            ) {
                                if (heroSong?.getEffectiveArtworkUrl()?.isNotBlank() == true) {
                                    AsyncImage(
                                        model = heroSong.getEffectiveArtworkUrl(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .background(primaryColor.copy(alpha = 0.20f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("NUEVO LANZAMIENTO", color = primaryColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(Modifier.height(4.dp))

                                Text(heroSong?.title ?: "Nébula Violeta (EP)", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("EP • 5 canciones", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)

                                Spacer(Modifier.height(6.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .border(1.dp, primaryColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("LOSSLESS ALAC", color = primaryColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("DOLBY ATMOS", color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            IconButton(
                                onClick = {
                                    heroSong?.let {
                                        viewModel.playSongSingle(it)
                                        onExpandPlayer()
                                    }
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            // ─── Canciones Populares ───
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Canciones Populares", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Top reproducciones", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                }
            }

            items(topSongs, key = { it.id }) { song ->
                val isCurrent = state.currentSong?.id == song.id
                SongItemRow(
                    song = song,
                    isCurrent = isCurrent,
                    isPlaying = isCurrent && state.isPlaying,
                    isLiked = state.likedSongIds.contains(song.id),
                    isDownloaded = song.isDownloaded,
                    isDownloading = state.downloadingSongIds.contains(song.id),
                    onClick = {
                        val idx = topSongs.indexOfFirst { it.id == song.id }
                        viewModel.playSongsFromList(topSongs, idx.coerceAtLeast(0))
                    },
                    onShowDetails = { onShowDetails(song) },
                    onLikeToggle = { viewModel.toggleLike(song) },
                    onDownloadToggle = { viewModel.downloadSong(song) }
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .liquidGlass(RoundedCornerShape(50), alpha = 0.08f)
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                        .clickable { showTop10 = !showTop10 }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (showTop10) "Mostrar menos" else "Ver las 10 mejores", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // ─── Discografía (Foto 2) ───
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Discografía", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Ver todo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                    }

                    Spacer(Modifier.height(10.dp))

                    val discFilters = listOf("Álbumes", "EPs y Sencillos", "En Vivo & Compilaciones")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(discFilters) { label ->
                            val isSelected = discographyFilter == label
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .liquidGlass(RoundedCornerShape(50), alpha = if (isSelected) 0.25f else 0.08f)
                                    .border(1.dp, if (isSelected) primaryColor else Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                                    .clickable { discographyFilter = label }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(label, color = Color.White, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(albumGroups.entries.toList()) { (albumName, songs) ->
                            val cover = songs.firstOrNull()?.getEffectiveArtworkUrl() ?: ""
                            Column(
                                modifier = Modifier
                                    .width(135.dp)
                                    .clickable {
                                        viewModel.playSongsFromList(songs, 0)
                                        onExpandPlayer()
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(135.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                ) {
                                    if (cover.isNotBlank()) {
                                        AsyncImage(model = cover, contentDescription = albumName, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    } else {
                                        Icon(Icons.Default.Album, contentDescription = null, tint = primaryColor, modifier = Modifier.size(48.dp).align(Alignment.Center))
                                    }
                                }

                                Spacer(Modifier.height(6.dp))

                                Text(albumName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("2025 • ${songs.size} canciones", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // ─── Bio & Trayectoria (Foto 2 / 3) ───
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .liquidGlass(RoundedCornerShape(24.dp), alpha = 0.08f)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(primaryColor, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text("BIO & TRAYECTORIA", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Pionero indiscutible de la nueva ola espacial de la música electrónica de ultra alta definición. Fusionando sintetizadores analógicos vintage con paisajes sonoros espaciales y masterización en Dolby Atmos, $artistName ha acumulado más de 500 millones de reproducciones en el ecosistema OfMusic.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ORIGEN", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("Berlín / Ciudad de México", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("SELLO DISCOGRÁFICO", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("AuraWave Audio Group", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .liquidGlass(RoundedCornerShape(50), alpha = 0.10f)
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Language, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("astralpulse.live", color = Color.White, fontSize = 11.sp)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .liquidGlass(RoundedCornerShape(50), alpha = 0.10f)
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Podcasts, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Podcast Quincenal", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // ─── De Gira (World Tour) (Foto 3) ───
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("De Gira (World Tour 2025)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Todas las fechas", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                    }

                    Spacer(Modifier.height(12.dp))

                    tourDates.forEach { tour ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .liquidGlass(RoundedCornerShape(18.dp), alpha = 0.08f)
                                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(primaryColor.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(tour.month, color = primaryColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(tour.day, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                    }
                                }

                                Spacer(Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tour.city, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${tour.venue} • ${tour.time}", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                }

                                Box(
                                    modifier = Modifier
                                        .liquidGlass(RoundedCornerShape(50), alpha = 0.20f)
                                        .border(1.dp, primaryColor, RoundedCornerShape(50))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Boletos", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}