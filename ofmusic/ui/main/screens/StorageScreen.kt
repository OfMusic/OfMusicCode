package com.haziel.ofmusic.ui.main.screens

import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haziel.ofmusic.ui.main.MusicUiState
import com.haziel.ofmusic.ui.main.MusicViewModel
import com.haziel.ofmusic.ui.theme.liquidGlass

@Composable
fun StorageScreen(
    state: MusicUiState,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onOpenQuality: () -> Unit
) {
    // Calcular almacenamiento real del dispositivo
    val stat = remember { StatFs(Environment.getDataDirectory().path) }
    val totalBytes = stat.blockCountLong * stat.blockSizeLong
    val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
    val usedBytes = totalBytes - freeBytes

    // Variables para la vista (convertidas a GB)
    val totalGb = totalBytes / (1024f * 1024f * 1024f)
    val usedGb = usedBytes / (1024f * 1024f * 1024f)
    val freeGb = freeBytes / (1024f * 1024f * 1024f)

    // Música descargada (simulada desde el state)
    val musicGb = state.downloadedStorageBytes / (1024f * 1024f * 1024f)
    val cacheMb = state.cacheStorageBytes / (1024f * 1024f)

    // Cargar el cálculo al abrir la pantalla
    LaunchedEffect(Unit) {
        viewModel.calculateStorage()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .background(Brush.verticalGradient(listOf(Color(0xFF2E1065).copy(alpha = 0.3f), Color.Transparent)))
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
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.liquidGlass(CircleShape, alpha = 0.1f).size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Almacenamiento", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("y Caché", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.weight(1f))
                // Botón Liberar espacio
                Box(
                    modifier = Modifier
                        .liquidGlass(RoundedCornerShape(16.dp), alpha = 0.1f)
                        .clickable { viewModel.clearAppCache() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Liberar\nespacio", color = Color.White, style = MaterialTheme.typography.labelSmall, lineHeight = 12.sp)
                    }
                }
            }

            // ─── Uso de Almacenamiento (Gráfico) ───
            Column(
                modifier = Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(24.dp), alpha = 0.08f).padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("USO DE ALMACENAMIENTO", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, letterSpacing = 1.sp)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(String.format("%.1f GB", musicGb), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.width(6.dp))
                            Text("de música (${state.downloadedSongMap.size} pistas)", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
                        }
                    }
                    Icon(Icons.Default.PieChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }

                Spacer(Modifier.height(16.dp))

                // Barra de progreso apilada
                Row(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.1f))) {
                    Box(modifier = Modifier.weight(0.3f).fillMaxHeight().background(MaterialTheme.colorScheme.primary)) // Música
                    Box(modifier = Modifier.weight(0.2f).fillMaxHeight().background(Color(0xFF6D28D9))) // Apps
                    Box(modifier = Modifier.weight(0.3f).fillMaxHeight().background(Color.White.copy(alpha = 0.3f))) // Sistema
                }

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    LegendItem("OfMusic", MaterialTheme.colorScheme.primary)
                    LegendItem("Apps", Color(0xFF6D28D9))
                    LegendItem("Sistema", Color.White.copy(alpha = 0.3f))
                    LegendItem("${((freeBytes/totalBytes.toFloat())*100).toInt()}% libre", Color.Transparent, true)
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Smartphone, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Total móvil: ${String.format("%.0f", totalGb)} GB", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Box(modifier = Modifier.liquidGlass(RoundedCornerShape(50), alpha = 0.1f).clickable { viewModel.clearAppCache() }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cached, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Borrar caché (${String.format("%.1f", cacheMb)} MB)", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }

            SectionTitle("DISTRIBUCIÓN DEL CONTENIDO")

            StorageListItem(Icons.Default.QueueMusic, "Listas sin conexión", "${state.playlists.size} listas completas", "${String.format("%.1f", musicGb * 0.6f)} GB")
            StorageListItem(Icons.Default.Album, "Álbumes guardados", "12 álbumes en alta fidelidad", "${String.format("%.1f", musicGb * 0.3f)} GB")
            StorageListItem(Icons.Default.Podcasts, "Podcasts & Radio grabada", "3 episodios y sesiones en vivo", "${String.format("%.1f", musicGb * 0.1f)} GB")

            SectionTitle("AJUSTES DE DESCARGA")

            // Ubicación
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).liquidGlass(RoundedCornerShape(16.dp), alpha = 0.05f).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).background(Color(0xFFFFD700).copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ubicación de Descargas", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Carpeta Interna Protegida", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                    }
                    Box(modifier = Modifier.liquidGlass(RoundedCornerShape(50), alpha = 0.1f).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("AES-256", color = Color.White, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable { onOpenQuality() }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("/storage/emulated/0/OfMusic_Library", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1)
                    Icon(Icons.Default.Edit, contentDescription = "Mover", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            // Wi-Fi
            Row(modifier = Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(16.dp), alpha = 0.05f).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Descargar solo por Wi-Fi", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    Text("Evita cargos de datos móviles", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = state.wifiOnlyDownload,
                    onCheckedChange = { wifiOnly ->
                        viewModel.setDownloadPreferences(state.downloadQuality, wifiOnly, state.autoDownloadFavorites)
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Calidad
            Row(modifier = Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(16.dp), alpha = 0.05f).clickable { onOpenQuality() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HighQuality, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Calidad de descarga", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    Text(state.downloadQuality, color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
            }

            Spacer(Modifier.height(16.dp))

            // ─── Auto-descarga y Límite ───
            Column(modifier = Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(16.dp), alpha = 0.05f).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-descargar \"Favoritos\"", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                        Text("Sincroniza temas automáticamente", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = state.autoDownloadFavorites,
                        onCheckedChange = { autoFav ->
                            viewModel.setDownloadPreferences(state.downloadQuality, state.wifiOnlyDownload, autoFav)
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                    Spacer(Modifier.width(12.dp))
                    Text("Límite de almacenamiento inteligente", color = Color.White, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text("${state.storageLimitGb} GB", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Text("Gestiona y rota temas antiguos si se supera la cuota seleccionada.", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 36.dp, top = 4.dp))

                Slider(
                    value = state.storageLimitGb.toFloat(),
                    onValueChange = { viewModel.setStorageLimit(it.toInt()) },
                    valueRange = 5f..50f,
                    steps = 8, // 5, 10, 15, 20...
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = Color.White.copy(alpha = 0.1f))
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("5 GB", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                    Text("20 GB", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                    Text("35 GB", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                    Text("50 GB Máx", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ─── Eliminar todas las descargas ───
            Button(
                onClick = { viewModel.deleteAllDownloads() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .liquidGlass(RoundedCornerShape(50), alpha = 0.2f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4C0519).copy(alpha = 0.35f),
                    contentColor = Color(0xFFFF5252)
                ),
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFFF5252))
                Spacer(Modifier.width(8.dp))
                Text("Eliminar todas las descargas", fontWeight = FontWeight.Bold)
            }
            Text("Esta acción liberará ${String.format("%.1f", musicGb)} GB de almacenamiento local en el teléfono.", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp).fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)

            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp, start = 4.dp))
}

@Composable
private fun LegendItem(text: String, color: Color, isPercentage: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!isPercentage) {
            Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
    }
}

@Composable
private fun StorageListItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    sizeText: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .liquidGlass(RoundedCornerShape(16.dp), alpha = 0.05f)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(sizeText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
        }
    }
}