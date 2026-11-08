package com.haziel.ofmusic.ui.main.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.haziel.ofmusic.domain.model.UserProfile
import com.haziel.ofmusic.ui.theme.liquidGlass
import com.haziel.ofmusic.ui.video.MutedVideoPlayer

// ─────────────────────────────────────────────────────────────
//  Helper: contenedor glass reutilizable para diálogos
// ─────────────────────────────────────────────────────────────

@Composable
private fun GlassDialogContainer(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xF21A1A1A))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    }
}

@Composable
private fun GlassDialogTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun GlassDialogActions(
    confirmLabel: String,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .liquidGlass(RoundedCornerShape(50), alpha = 0.08f)
                .clickable(onClick = onCancel),
            contentAlignment = Alignment.Center
        ) {
            Text("Cancelar", color = Color.White, fontWeight = FontWeight.SemiBold)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .liquidGlass(RoundedCornerShape(50), alpha = if (confirmEnabled) 0.25f else 0.08f)
                .background(
                    if (confirmEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    else Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(50)
                )
                .clickable(enabled = confirmEnabled, onClick = onConfirm),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = confirmLabel,
                color = if (confirmEnabled) Color.White else Color.White.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GlassInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.04f)),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
        )
    )
}

// ─────────────────────────────────────────────────────────────
//  Create Playlist
// ─────────────────────────────────────────────────────────────

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    GlassDialogContainer(onDismiss = onDismiss) {
        GlassDialogTitle("Nueva Playlist")

        GlassInputField(
            value = name,
            onValueChange = { name = it },
            label = "Nombre de la playlist"
        )

        GlassDialogActions(
            confirmLabel = "Crear",
            confirmEnabled = name.isNotBlank(),
            onConfirm = { if (name.isNotBlank()) onCreate(name.trim()) },
            onCancel = onDismiss
        )
    }
}

// ─────────────────────────────────────────────────────────────
//  Edit Profile
// ─────────────────────────────────────────────────────────────

@Composable
fun EditProfileDialog(
    currentProfile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (name: String, email: String, birthDate: String, country: String, photoUri: String?) -> Unit,
) {
    var name by remember { mutableStateOf(currentProfile.name) }
    var email by remember { mutableStateOf(currentProfile.email) }
    var birthDate by remember { mutableStateOf(currentProfile.birthDate) }
    var country by remember { mutableStateOf(currentProfile.country) }
    var photoUri by remember { mutableStateOf<String?>(currentProfile.photoUri) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) photoUri = uri.toString()
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xF21A1A1A))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Editar Perfil",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(Modifier.height(16.dp))

                // Avatar con botón de cambio
                Box(contentAlignment = Alignment.BottomEnd) {
                    ProfileAvatar(
                        photoUri = photoUri,
                        name = name.ifBlank { "U" },
                        size = 84.dp
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Cambiar foto",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                GlassInputField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nombre de usuario"
                )

                Spacer(Modifier.height(10.dp))

                GlassInputField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Correo electrónico"
                )

                Spacer(Modifier.height(10.dp))

                GlassInputField(
                    value = birthDate,
                    onValueChange = { birthDate = it },
                    label = "Fecha de Nacimiento"
                )

                Spacer(Modifier.height(10.dp))

                GlassInputField(
                    value = country,
                    onValueChange = { country = it },
                    label = "País / Región"
                )

                GlassDialogActions(
                    confirmLabel = "Guardar",
                    confirmEnabled = name.isNotBlank() && email.isNotBlank(),
                    onConfirm = { onSave(name.trim(), email.trim(), birthDate.trim(), country.trim(), photoUri) },
                    onCancel = onDismiss
                )
            }
        }
    }
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    GlassDialogContainer(onDismiss = onDismiss) {
        GlassDialogTitle("Cambiar Contraseña")

        GlassInputField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = "Nueva Contraseña"
        )

        Spacer(Modifier.height(12.dp))

        GlassInputField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Confirmar Contraseña"
        )

        GlassDialogActions(
            confirmLabel = "Actualizar",
            confirmEnabled = newPassword.length >= 6 && newPassword == confirmPassword,
            onConfirm = { onConfirm(newPassword) },
            onCancel = onDismiss
        )
    }
}

@Composable
fun ResetStorageConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xF21A1A1A))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.CleaningServices,
                    contentDescription = null,
                    tint = Color(0xFFFFA500),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Reiniciar almacenamiento",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "¿Seguro que quieres borrar todas tus descargas, listas e historial para empezar de 0 con tu misma cuenta?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .liquidGlass(RoundedCornerShape(50), alpha = 0.08f)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancelar", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .liquidGlass(RoundedCornerShape(50), alpha = 0.22f)
                            .background(Color(0xFFFFA500).copy(alpha = 0.25f), RoundedCornerShape(50))
                            .clickable(onClick = onConfirm),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Reiniciar a 0", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteAccountConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xF21A1A1A))
                .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Eliminar cuenta permanentemente",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Esta acción eliminará tu perfil, listas, favoritos y cuenta de usuario permanentemente. No se puede deshacer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .liquidGlass(RoundedCornerShape(50), alpha = 0.08f)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancelar", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .liquidGlass(RoundedCornerShape(50), alpha = 0.22f)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.25f), RoundedCornerShape(50))
                            .clickable(onClick = onConfirm),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Quality Selection
// ─────────────────────────────────────────────────────────────

@Composable
fun QualitySelectionDialog(
    currentQuality: String,
    onDismiss: () -> Unit,
    onSelectQuality: (String) -> Unit
) {
    val qualities = listOf(
        "320 kbps (HQ Alta Calidad)",
        "256 kbps (Calidad Buena)",
        "192 kbps (Calidad Estándar)",
        "128 kbps (Ahorro de Datos)"
    )

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xF21A1A1A))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Calidad de descarga",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Elige la tasa de bits para las descargas offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )

                Spacer(Modifier.height(16.dp))

                qualities.forEach { quality ->
                    val isSelected = currentQuality.startsWith(quality.substringBefore(" "))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else Color.White.copy(alpha = 0.03f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectQuality(quality) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectQuality(quality) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = Color.White.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = quality,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cerrar", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  YouTube Video (fullscreen dialog, sin cambios mayores)
// ─────────────────────────────────────────────────────────────

@Composable
fun YouTubeVideoDialog(
    youtubeId: String,
    title: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    MutedVideoPlayer(
                        videoUrlOrId = youtubeId,
                        isPlayingAudio = true,
                        audioPositionMs = 0L,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Playlist Name (crear / renombrar)
// ─────────────────────────────────────────────────────────────

@Composable
fun PlaylistNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(CircleShape, alpha = 0.1f)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                GlassInputField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nombre"
                )

                GlassDialogActions(
                    confirmLabel = confirmLabel,
                    confirmEnabled = name.isNotBlank(),
                    onConfirm = { if (name.isNotBlank()) onConfirm(name.trim()) },
                    onCancel = onDismiss
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Confirmar eliminar playlist
// ─────────────────────────────────────────────────────────────

@Composable
fun DeletePlaylistConfirmDialog(
    playlistTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xF21A1A1A))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Eliminar playlist",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "¿Seguro que quieres eliminar \"$playlistTitle\"? Esta acción no se puede deshacer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(50)
                            )
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancelar", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.error)
                            .clickable(onClick = onConfirm),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Agregar canciones a playlist
// ─────────────────────────────────────────────────────────────

@Composable
fun AddSongsToPlaylistDialog(
    playlist: com.haziel.ofmusic.domain.model.Playlist,
    allSongs: List<com.haziel.ofmusic.domain.model.Song>,
    likedSongIds: Set<Long> = emptySet(),
    downloadedSongMap: Map<Long, String> = emptyMap(),
    onDismiss: () -> Unit,
    onConfirm: ((List<com.haziel.ofmusic.domain.model.Song>) -> Unit)? = null,
    onAddSong: ((com.haziel.ofmusic.domain.model.Song) -> Unit)? = null
) {
    var query by remember { mutableStateOf("") }
    var selectedFilterChip by remember { mutableStateOf("Todas") }
    var newlyAddedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    val existingIds = remember(playlist.songs) { playlist.songs.map { it.id }.toSet() }

    val candidateSongs = remember(allSongs, likedSongIds, downloadedSongMap) {
        val filteredList = allSongs.filter { song ->
            song.id in likedSongIds ||
                    downloadedSongMap.containsKey(song.id) ||
                    song.isLiked ||
                    song.isDownloaded
        }
        if (filteredList.isNotEmpty()) filteredList else allSongs
    }

    val displayList = candidateSongs.filter { song ->
        val matchesQuery = query.isBlank() ||
                song.title.contains(query, ignoreCase = true) ||
                song.artist.contains(query, ignoreCase = true)

        val isLikedSong = song.id in likedSongIds || song.isLiked
        val isDownloadedSong = downloadedSongMap.containsKey(song.id) || song.isDownloaded

        val matchesFilter = when (selectedFilterChip) {
            "Me Gusta" -> isLikedSong
            "Descargadas" -> isDownloadedSong
            else -> true
        }

        matchesQuery && matchesFilter
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 620.dp)
                .liquidGlass(RoundedCornerShape(28.dp), alpha = 0.12f)
                .background(Color(0xF2120D17), RoundedCornerShape(28.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .liquidGlass(CircleShape, alpha = 0.15f)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Agregar a playlist",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = playlist.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.liquidGlass(CircleShape, alpha = 0.1f).size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Buscador
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Buscar en descargadas o me gusta", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(Modifier.height(10.dp))

                // Chips de Filtro
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Todas", "Me Gusta", "Descargadas").forEach { chip ->
                        val isSelected = selectedFilterChip == chip
                        Box(
                            modifier = Modifier
                                .liquidGlass(RoundedCornerShape(50), alpha = if (isSelected) 0.25f else 0.05f)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable { selectedFilterChip = chip }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = chip,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Lista de Canciones
                if (displayList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (candidateSongs.isEmpty()) "No hay canciones descargadas ni en Me Gusta."
                            else "No se encontraron coincidencias.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayList, key = { it.id }) { song ->
                            val isAlreadyInPlaylist = song.id in existingIds || song.id in newlyAddedIds
                            val isLiked = song.id in likedSongIds || song.isLiked
                            val isDownloaded = downloadedSongMap.containsKey(song.id) || song.isDownloaded

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .border(
                                        width = 1.dp,
                                        color = if (isAlreadyInPlaylist) Color(0xFF00E676).copy(alpha = 0.3f)
                                        else Color.White.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Portada
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val artwork = song.getEffectiveArtworkUrl()
                                    if (artwork.isNotBlank()) {
                                        coil.compose.AsyncImage(
                                            model = artwork,
                                            contentDescription = null,
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                Spacer(Modifier.width(12.dp))

                                // Título & Artista
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isLiked) {
                                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFE91E63), modifier = Modifier.size(12.dp))
                                            Spacer(Modifier.width(4.dp))
                                        }
                                        if (isDownloaded) {
                                            Icon(Icons.Default.DownloadDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                            Spacer(Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = song.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(Modifier.width(8.dp))

                                // Estado / Botón Añadir
                                if (isAlreadyInPlaylist) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(Color(0xFF00E676).copy(alpha = 0.15f), RoundedCornerShape(50))
                                            .border(1.dp, Color(0xFF00E676).copy(alpha = 0.4f), RoundedCornerShape(50))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Añadida",
                                            tint = Color(0xFF00E676),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "Añadida",
                                            color = Color(0xFF00E676),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .liquidGlass(RoundedCornerShape(50), alpha = 0.2f)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(50))
                                            .clickable {
                                                newlyAddedIds = newlyAddedIds + song.id
                                                onAddSong?.invoke(song)
                                                onConfirm?.invoke(listOf(song))
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Add, contentDescription = "Añadir", tint = Color.White, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = "Añadir",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Botón Cierre / Listo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .liquidGlass(RoundedCornerShape(50), alpha = 0.2f)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(50))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Listo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Frecuencia de pantalla
// ─────────────────────────────────────────────────────────────

@Composable
fun RefreshRateSelectionDialog(
    currentRefreshRate: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    val options = listOf(
        0 to ("Automatico" to "El sistema elige la mejor frecuencia"),
        60 to ("60 Hz" to "Ahorro de bateria"),
        90 to ("90 Hz" to "Balance entre fluidez y bateria"),
        120 to ("120 Hz" to "Maxima fluidez")
    )

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xF21A1A1A))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Frecuencia de pantalla",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Elige la tasa de refresco de la pantalla. Una tasa mayor consume mas bateria.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )

                Spacer(Modifier.height(16.dp))

                options.forEach { (value, labels) ->
                    val (title, subtitle) = labels
                    val isSelected = currentRefreshRate == value
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else Color.White.copy(alpha = 0.03f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelect(value) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelect(value) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = Color.White.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.55f)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cerrar", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}