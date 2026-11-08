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
import androidx.compose.runtime.Composable
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
import com.haziel.ofmusic.ui.theme.liquidGlass
import coil.compose.AsyncImage
import com.haziel.ofmusic.domain.model.Song
import com.haziel.ofmusic.domain.tier.UserTier
import com.haziel.ofmusic.ui.main.MusicUiState
import com.haziel.ofmusic.ui.main.MusicViewModel
import com.haziel.ofmusic.ui.theme.liquidGlass
import com.haziel.ofmusic.ui.main.components.SongItemRow

@Composable
fun HomeScreenContent(
    state: MusicUiState,
    viewModel: MusicViewModel,
    onExpandPlayer: () -> Unit,
    onShowDetails: (Song) -> Unit,
    userTier: UserTier,
    onUpgradeClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // ─── Banner Premium (Solo para Free) ───
        if (userTier == UserTier.FREE) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), Color.Transparent)))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .clickable { onUpgradeClick() }
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pásate a Standard", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            Text("Descargas ilimitadas, video y más.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            SectionSaludo(state)
        }

        // ─── Píldora Audio Status (Datos Reales) ───
        item {
            val isCurrentOffline = state.currentSong?.let { state.downloadedSongMap.containsKey(it.id) || it.isDownloaded } ?: false
            val audioPillTitle = when {
                isCurrentOffline -> "ALAC Lossless Offline"
                state.isPlaying -> "ALAC Lossless Activo"
                else -> "Audio Engine Listo"
            }
            val spatialText = if (state.isSpatialAudioEnabled) "Dolby Atmos" else "Stereo Lossless"
            val qualityShort = if (isCurrentOffline) "24-bit/96kHz" else state.downloadQuality.substringBefore(" ")
            val audioPillSubtitle = "$spatialText • $qualityShort"

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x221D1A27))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (state.isPlaying || isCurrentOffline) MaterialTheme.colorScheme.primary
                                    else Color.White.copy(alpha = 0.5f),
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = audioPillTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = audioPillSubtitle,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // ─── Mix Destacado (Ondas del Infinito) ───
        item {
            val primaryColor = MaterialTheme.colorScheme.primary
            val secondaryColor = MaterialTheme.colorScheme.secondary
            val heroSong = state.songs.firstOrNull()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .liquidGlass(RoundedCornerShape(24.dp), alpha = 0.12f)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.45f),
                                secondaryColor.copy(alpha = 0.25f),
                                Color(0xFF0C0914)
                            )
                        )
                    )
                    .border(1.dp, primaryColor.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("MIX DESTACADO", color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Ondas del Infinito",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Curada por Astral Pulse • Lossless Studio Master",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(Modifier.height(14.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                                Text("Astral Pulse", color = Color.White, fontSize = 11.sp)
                            }
                            Box(modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                                Text("Synthwave", color = Color.White, fontSize = 11.sp)
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { viewModel.generateLikedBasedRadio() },
                                modifier = Modifier.liquidGlass(RoundedCornerShape(50), alpha = 0.25f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor.copy(alpha = 0.25f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(Modifier.width(6.dp))
                                Text("Escuchar Mix", fontWeight = FontWeight.Bold)
                            }

                            Spacer(Modifier.width(12.dp))

                            val firstSongInMix = state.songs.firstOrNull()
                            val isMixLiked = firstSongInMix != null && state.likedSongIds.contains(firstSongInMix.id)
                            IconButton(
                                onClick = {
                                    firstSongInMix?.let { viewModel.toggleLike(it) }
                                },
                                modifier = Modifier
                                    .liquidGlass(CircleShape, alpha = 0.12f)
                                    .size(42.dp)
                            ) {
                                Icon(
                                    imageVector = if (isMixLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Me gusta",
                                    tint = if (isMixLiked) Color(0xFFE91E63) else Color.White
                                )
                            }
                        }
                    }

                    if (heroSong?.getEffectiveArtworkUrl()?.isNotBlank() == true) {
                        Spacer(Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .border(1.dp, primaryColor.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        ) {
                            AsyncImage(
                                model = heroSong.getEffectiveArtworkUrl(),
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // ─── Escuchar de nuevo ───
        if (state.songs.isNotEmpty()) {
            item {
                SectionHeader(title = "Escuchar de nuevo", onSeeAllClick = {
                    viewModel.playSongsFromList(state.songs.take(10))
                    onExpandPlayer()
                })
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.songs.take(5)) { song ->
                        AlbumCard(song = song, onClick = { onShowDetails(song) })
                    }
                }
            }
        }

        // ─── Pistas en rotación ───
        item {
            SectionHeader(title = "Pistas en rotación", onSeeAllClick = {
                viewModel.playSongsFromList(state.songs)
                onExpandPlayer()
            })
        }

        items(state.songs.take(10)) { song ->
            val isCurrent = state.currentSong?.id == song.id
            SongItemRow(
                song = song,
                isCurrent = isCurrent,
                isPlaying = isCurrent && state.isPlaying,
                onClick = { viewModel.playSongSingle(song) },
                onShowDetails = { onShowDetails(song) }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, onSeeAllClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text("Ver todo", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clickable { onSeeAllClick() })
    }
}

@Composable
private fun SectionSaludo(state: MusicUiState) {
    val firstName = if (state.isLoggedIn) {
        if (state.userProfile.name.contains(".")) {
            state.userProfile.name.split(".").firstOrNull() ?: ""
        } else {
            state.userProfile.name.split(" ").firstOrNull() ?: ""
        }
    } else {
        "Invitado"
    }

    val tier = state.authProfile?.asUserTier() ?: UserTier.FREE
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .liquidGlass(RoundedCornerShape(22.dp), alpha = 0.08f)
            .border(1.dp, primaryColor.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "¡Hola, $firstName!",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("👋", fontSize = 18.sp)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(primaryColor.copy(alpha = 0.2f), RoundedCornerShape(50))
                            .border(0.5.dp, primaryColor, RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Plan ${tier.displayName}",
                            color = primaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Lossless Studio Master",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .border(1.5.dp, primaryColor, CircleShape)
                    .padding(2.dp)
                    .clip(CircleShape)
            ) {
                com.haziel.ofmusic.ui.main.components.ProfileAvatar(
                    photoUri = if (state.isLoggedIn) state.userProfile.photoUri else null,
                    name = firstName,
                    size = 40.dp
                )
            }
        }
    }
}

@Composable
private fun AlbumCard(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(130.dp).clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (song.getEffectiveArtworkUrl().isNotBlank()) {
                AsyncImage(
                    model = song.getEffectiveArtworkUrl(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(song.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}