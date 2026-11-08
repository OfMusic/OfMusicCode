package com.haziel.ofmusic.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Speed
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
fun RefreshRateSelectionSheet(
    currentHz: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    // Estado local: 0 = Auto, 60, 90, 120
    var selectedHz by remember { mutableStateOf(currentHz) }

    val rateOptions = listOf(
        Triple(0, "Automático", "Equilibrio perfecto entre fluidez y batería"),
        Triple(60, "60 Hz", "Estándar • Mayor duración de batería"),
        Triple(90, "90 Hz", "Fluido • Desplazamiento más suave"),
        Triple(120, "120 Hz", "Máxima Fluidez • Experiencia visual premium")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF120D17),
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
                    Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(12.dp))
                Text("Frecuencia de pantalla", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(Modifier.height(12.dp))
            Text("Selecciona la tasa de refresco. Una tasa más alta hace que las animaciones se vean más suaves, pero consume más batería.", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))

            // Opciones
            rateOptions.forEach { (hz, title, description) ->
                val isSelected = selectedHz == hz
                val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                val bgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.03f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(bgColor)
                        .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                        .clickable { selectedHz = hz }
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
                        Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text(description, color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Botones
            Button(
                onClick = { onSave(selectedHz) },
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
                Text("Aplicar Frecuencia", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text("Cancelar", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}