package com.haziel.ofmusic.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haziel.ofmusic.ui.theme.liquidGlass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualitySelectionSheet(
    currentQuality: String,
    wifiOnly: Boolean,
    autoDownload: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Boolean, Boolean) -> Unit
) {
    // Estados locales
    var selectedQuality by remember { mutableStateOf(currentQuality) }
    var isWifiOnly by remember { mutableStateOf(wifiOnly) }
    var isAutoDownload by remember { mutableStateOf(autoDownload) }

    val qualityOptions = listOf(
        Triple("320 kbps", "HQ Alta Calidad • Sonido cristalino", "Recomendado"),
        Triple("256 kbps", "Calidad Buena • Balance memoria/fidelidad", null),
        Triple("192 kbps", "Calidad Estándar • Descargas rápidas", null),
        Triple("128 kbps", "Ahorro de Datos • Espacio mínimo", null),
        Triple("Lossless Hi-Res ALAC", "24-bit / 96kHz • Máxima pureza acústica", "Audiófilo")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF120D17), // Fondo oscuro
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.HighQuality, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(12.dp))
                Text("Calidad de descarga", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier.liquidGlass(RoundedCornerShape(50), alpha = 0.1f).padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("OFMUSIC HQ", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Elige la tasa de bits y resolución para las descargas offline. Los archivos de mayor calidad ocuparán más almacenamiento.", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))

            // Opciones de calidad
            qualityOptions.forEach { (quality, description, tag) ->
                val isSelected = selectedQuality.contains(quality)
                val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                val bgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.03f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(bgColor)
                        .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                        .clickable { selectedQuality = quality }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(quality, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            if (tag != null) {
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(if (tag == "Recomendado") MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color(0xFFFFA500).copy(alpha = 0.3f), RoundedCornerShape(50))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(tag, color = if (tag == "Recomendado") MaterialTheme.colorScheme.primary else Color(0xFFFFA500), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(description, color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Switches
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Solo descargar con Wi-Fi", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    Text("Evita el consumo de datos móviles", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = isWifiOnly, onCheckedChange = { isWifiOnly = it }, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
            }

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Descarga automática de favoritos", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    Text("Mantén tus 'Me Gusta' siempre listos", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = isAutoDownload, onCheckedChange = { isAutoDownload = it }, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
            }

            Spacer(Modifier.height(24.dp))

            // Botón Guardar
            Button(
                onClick = { onSave(selectedQuality, isWifiOnly, isAutoDownload) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .liquidGlass(RoundedCornerShape(50), alpha = 0.25f),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    contentColor = Color.White
                )
            ) {
                Text("Guardar Calidad", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text("Cerrar", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}