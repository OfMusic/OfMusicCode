package com.haziel.ofmusic.ui.main.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haziel.ofmusic.ui.theme.liquidGlass
import coil.compose.AsyncImage
import com.haziel.ofmusic.ui.main.MusicViewModel
import com.haziel.ofmusic.ui.theme.liquidGlass

@Composable
fun NuevaPlaylistScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onCreateSuccess: () -> Unit,
    onAddSongsNow: (Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }
    var downloadOffline by remember { mutableStateOf(true) }
    var coverUrl by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coverUrl = uri.toString()
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // ─── Top Bar ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Cancelar",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Nueva Playlist",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                TextButton(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.createPlaylist(
                                title = title,
                                description = description,
                                coverUrl = coverUrl ?: "",
                                isPublic = isPublic,
                                downloadOffline = downloadOffline
                            )
                            onCreateSuccess()
                        }
                    },
                    enabled = title.isNotBlank()
                ) {
                    Text(
                        text = "Crear",
                        fontWeight = FontWeight.Bold,
                        color = if (title.isNotBlank()) primaryColor else Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // ─── Portada Selector ───
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(primaryColor.copy(alpha = 0.15f))
                        .liquidGlass(RoundedCornerShape(24.dp), alpha = 0.08f)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (!coverUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = coverUrl,
                            contentDescription = "Portada de Playlist",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = "Subir portada",
                                tint = primaryColor,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Elegir Portada",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ─── Campo Título ───
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nombre de la playlist", color = Color.White.copy(alpha = 0.6f)) },
                    placeholder = { Text("Ej: Sesiones Nocturnas Hi-Fi", color = Color.White.copy(alpha = 0.3f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = primaryColor
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ─── Campo Descripción ───
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (opcional)", color = Color.White.copy(alpha = 0.6f)) },
                    placeholder = { Text("Lossless ALAC, mezclas espaciales...", color = Color.White.copy(alpha = 0.3f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = primaryColor
                    ),
                    shape = RoundedCornerShape(16.dp),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ─── Toggle Descarga Offline ───
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(RoundedCornerShape(16.dp), alpha = 0.05f)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Descargar para escuchar Offline", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("Guarda automáticamente en Lossless", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                    }
                    Switch(
                        checked = downloadOffline,
                        onCheckedChange = { downloadOffline = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = primaryColor)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ─── Toggle Playlist Pública ───
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(RoundedCornerShape(16.dp), alpha = 0.05f)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Playlist Pública", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("Visible en tu perfil de OfMusic", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                    }
                    Switch(
                        checked = isPublic,
                        onCheckedChange = { isPublic = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = primaryColor)
                    )
                }

                Spacer(Modifier.height(28.dp))

                // ─── Botón Guardar & Agregar Canciones ───
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.createPlaylist(
                                title = title,
                                description = description,
                                coverUrl = coverUrl ?: "",
                                isPublic = isPublic,
                                downloadOffline = downloadOffline,
                                onCreated = { playlistId ->
                                    onAddSongsNow(playlistId)
                                }
                            )
                        }
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .liquidGlass(RoundedCornerShape(50), alpha = 0.25f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor.copy(alpha = 0.25f),
                        contentColor = Color.White,
                        disabledContainerColor = Color.White.copy(alpha = 0.05f),
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Agregar canciones a la lista", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}