package com.haziel.ofmusic.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.haziel.ofmusic.domain.tier.UpgradeReason
import com.haziel.ofmusic.ui.theme.liquidGlass

@Composable
fun UpgradeDialog(
    reason: UpgradeReason,
    onDismiss: () -> Unit,
    onSeePlans: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(RoundedCornerShape(32.dp), alpha = 0.1f)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Resplandor de fondo
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), Color.Transparent)))
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = reason.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = reason.message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { onDismiss(); onSeePlans() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .liquidGlass(RoundedCornerShape(50), alpha = 0.25f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Ver Planes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Ahora no",
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}