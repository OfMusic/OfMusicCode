package com.haziel.ofmusic.ui.main.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haziel.ofmusic.R
import com.haziel.ofmusic.data.preferences.AppThemeOption
import com.haziel.ofmusic.domain.tier.UserTier
import com.haziel.ofmusic.ui.main.MusicUiState
import com.haziel.ofmusic.ui.main.MusicViewModel
import com.haziel.ofmusic.ui.main.components.OfMusicLogoIcon
import com.haziel.ofmusic.ui.main.components.ProfileAvatar
import com.haziel.ofmusic.ui.theme.liquidGlass

@Composable
fun SettingsScreen(
    state: MusicUiState,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onQuality: () -> Unit,
    onRefreshRate: () -> Unit,
    onGoToLibrary: () -> Unit,
    onOpenAuth: (Boolean) -> Unit,
    onOpenPlans: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenAccount: () -> Unit
) {
    val tier = state.authProfile?.asUserTier() ?: UserTier.FREE

    val isVipUser = remember(state.userProfile.email, state.authProfile?.email) {
        state.userProfile.email.equals("nahucanyenyyazmin@gmail.com", ignoreCase = true) ||
                (state.authProfile?.email?.equals("nahucanyenyyazmin@gmail.com", ignoreCase = true) == true)
    }

    val availableThemes = remember(isVipUser) {
        AppThemeOption.entries.filter { !it.isVipOnly || isVipUser }
    }

    LaunchedEffect(Unit) {
        viewModel.calculateStorage()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    radius = 1200f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // ─── Top Bar ───
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .liquidGlass(CircleShape, alpha = 0.1f)
                        .size(40.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                Text("Configuración", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)

                Spacer(Modifier.weight(1f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .liquidGlass(RoundedCornerShape(50), alpha = 0.1f)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    OfMusicLogoIcon(size = 18.dp, primaryColor = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text("OfMusic", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }

            // ─── Perfil ───
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(RoundedCornerShape(24.dp), alpha = 0.08f)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        ProfileAvatar(
                            photoUri = if (state.isLoggedIn) state.userProfile.photoUri else null,
                            name = if (state.isLoggedIn) state.userProfile.name else "I",
                            size = 72.dp
                        )
                        if (state.isLoggedIn) {
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(Color(0xFF1A1122), CircleShape)
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (state.isLoggedIn) state.userProfile.name else "Invitado",
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White
                        )
                        Text(
                            text = if (state.isLoggedIn) state.userProfile.email else "Inicia sesión para respaldar tu música",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (state.isLoggedIn) {
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Plan ${tier.displayName}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                if (state.isLoggedIn) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .liquidGlass(RoundedCornerShape(12.dp), alpha = 0.1f)
                            .clickable { onEditProfile() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Editar Perfil / Foto", color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                    )
                                )
                                .clickable { onOpenAuth(false) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Iniciar Sesión", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .liquidGlass(RoundedCornerShape(12.dp), alpha = 0.08f)
                                .clickable { onOpenAuth(true) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Crear Cuenta", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ─── APARIENCIA, TEMA & FRECUENCIA ───
            SectionTitle("APARIENCIA, TEMA & FRECUENCIA")

            SettingsItem(
                iconRes = R.drawable.ico_calidad_descarga,
                title = "Frecuencia de pantalla",
                subtitle = if (state.refreshRate == 0) "Automático (120 Hz fluido)" else "${state.refreshRate} Hz",
                onClick = onRefreshRate
            )

            // Selector de Temas con Previsualización del Logo
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .liquidGlass(RoundedCornerShape(20.dp), alpha = 0.06f)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Tema y Paleta de Colores", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Personaliza el color primario de toda la aplicación", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(Modifier.height(16.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableThemes, key = { it.key }) { themeOption ->
                        val isSelected = state.selectedTheme == themeOption

                        Box(
                            modifier = Modifier
                                .width(130.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(themeOption.backgroundColor)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) themeOption.primaryColor else Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable { viewModel.setAppTheme(themeOption) }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (themeOption.isVipOnly) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF2563EB).copy(alpha = 0.3f), RoundedCornerShape(50))
                                            .border(1.dp, Color(0xFF60A5FA), RoundedCornerShape(50))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("VIP AZUL", color = Color(0xFF60A5FA), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                }

                                OfMusicLogoIcon(size = 48.dp, primaryColor = themeOption.primaryColor)

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text = themeOption.title,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )

                                Spacer(Modifier.height(4.dp))

                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(themeOption.primaryColor)
                                )
                            }
                        }
                    }
                }
            }

            SectionTitle("TU COLECCIÓN")

            SettingsItem(
                iconRes = R.drawable.ico_musica_descargada,
                title = "Música Descargada (Offline)",
                subtitle = "${state.downloadedSongMap.size} canciones guardadas",
                onClick = onGoToLibrary
            )

            SettingsItem(
                iconRes = R.drawable.ico_calidad_descarga,
                title = "Calidad de Descarga",
                subtitle = state.downloadQuality,
                trailing = {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF00E676), CircleShape))
                },
                onClick = onQuality
            )

            SettingsItem(
                iconRes = R.drawable.ico_musica_descargada,
                title = "Descargar al dar Me Gusta",
                subtitle = if (state.autoDownloadFavorites) "Activado: canciones favoritas se descargan automáticamente" else "Desactivado",
                trailing = {
                    Switch(
                        checked = state.autoDownloadFavorites,
                        onCheckedChange = { viewModel.setAutoDownloadFavorites(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                },
                onClick = { viewModel.setAutoDownloadFavorites(!state.autoDownloadFavorites) }
            )

            SectionTitle("AUDIO & PROCESAMIENTO")

            SettingsItem(
                iconRes = R.drawable.ico_audio_espacial,
                title = "Audio Espacial & Dolby Atmos",
                subtitle = "Inmersión 3D Habilitada",
                trailing = {
                    Box(
                        modifier = Modifier
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("DOLBY", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                },
                onClick = { viewModel.toggleSpatialAudio(!state.isSpatialAudioEnabled) }
            )

            SectionTitle("CUENTA & PRIVACIDAD")

            SettingsItem(
                iconRes = R.drawable.ico_mi_cuenta,
                title = "Mi Cuenta",
                subtitle = if (state.isLoggedIn) "Sesión activa de ${state.userProfile.name}" else "Iniciar Sesión / Registrarse",
                onClick = {
                    if (state.isLoggedIn) {
                        onOpenAccount()
                    } else {
                        onOpenAuth(false)
                    }
                }
            )

            SettingsItem(
                iconRes = R.drawable.ico_carpeta_interna,
                title = "Carpeta Interna Protegida",
                subtitle = "/storage/OfMusic_Library",
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    }
                },
                onClick = onOpenStorage
            )

            val totalStorageMb = (state.downloadedStorageBytes + state.cacheStorageBytes) / (1024f * 1024f)
            val storageSubtitle = if (totalStorageMb > 1024f) {
                String.format("%.1f GB usados", totalStorageMb / 1024f)
            } else if (totalStorageMb > 0f) {
                String.format("%.1f MB usados", totalStorageMb)
            } else {
                "${state.downloadedSongMap.size} descargas guardadas"
            }

            SettingsItem(
                iconRes = R.drawable.ico_almacenamiento_cache,
                title = "Almacenamiento y Caché",
                subtitle = storageSubtitle,
                onClick = onOpenStorage
            )

            Spacer(Modifier.height(32.dp))

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("OfMusic Studio v3.5.0", color = Color.White.copy(alpha = 0.3f), style = MaterialTheme.typography.labelSmall)
                Text("Sonido de Alta Fidelidad & Personalización de Temas", color = Color.White.copy(alpha = 0.2f), style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color.White.copy(alpha = 0.4f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsItem(
    iconRes: Int,
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .liquidGlass(RoundedCornerShape(16.dp), alpha = 0.05f)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }

        if (trailing != null) {
            trailing()
        } else {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.3f))
        }
    }
}