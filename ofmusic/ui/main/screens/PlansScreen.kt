package com.haziel.ofmusic.ui.main.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
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
import com.haziel.ofmusic.domain.tier.UserTier
import com.haziel.ofmusic.ui.theme.liquidGlass
import com.haziel.ofmusic.ui.theme.liquidGlass

@Composable
fun PlansScreen(
    currentTier: UserTier,
    onBack: () -> Unit,
    onSelectPlan: (UserTier) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07070A)) // Fondo oscuro base
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            Text(
                text = "Mejora tu música",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Elige el plan perfecto para ti",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tarjeta Free
        PlanCard(
            title = "Free",
            price = "$0",
            period = "para siempre",
            features = listOf("25 descargas", "5 playlists", "Calidad 256 kbps"),
            isCurrent = currentTier == UserTier.FREE,
            isRecommended = false,
            accentColor = Color.White.copy(alpha = 0.5f),
            onSelect = { }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tarjeta Standard
        PlanCard(
            title = "Standard",
            price = "$49",
            period = "al mes",
            features = listOf("150 descargas", "20 playlists", "Video y letras", "Modo offline"),
            isCurrent = currentTier == UserTier.STANDARD,
            isRecommended = true,
            accentColor = Color(0xFF00E676), // Verde
            onSelect = { onSelectPlan(UserTier.STANDARD) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tarjeta Pro
        PlanCard(
            title = "Pro",
            price = "$99",
            period = "al mes",
            features = listOf("Descargas ilimitadas", "Playlists ilimitadas", "Calidad 320 kbps", "Todo lo de Standard"),
            isCurrent = currentTier == UserTier.PRO,
            isRecommended = false,
            accentColor = Color(0xFFFFD700), // Dorado
            onSelect = { onSelectPlan(UserTier.PRO) }
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    period: String,
    features: List<String>,
    isCurrent: Boolean,
    isRecommended: Boolean,
    accentColor: Color,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(RoundedCornerShape(24.dp), alpha = if (isRecommended) 0.12f else 0.05f)
            .border(
                width = if (isRecommended) 1.5.dp else 0.dp,
                brush = if (isRecommended) Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color.Transparent)) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            // Header (Título e Icono)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = accentColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )

                if (isRecommended) {
                    Box(
                        modifier = Modifier
                            .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Recomendado", color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (title == "Pro") {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = accentColor)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Precio
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = price, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = period, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de beneficios
            features.forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = feature, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botón
            Button(
                onClick = onSelect,
                enabled = !isCurrent, // Deshabilitado si ya tienes ese plan
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .liquidGlass(RoundedCornerShape(16.dp), alpha = if (isRecommended) 0.25f else 0.1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecommended) accentColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                    contentColor = Color.White,
                    disabledContainerColor = Color.White.copy(alpha = 0.05f),
                    disabledContentColor = Color.White.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = if (isCurrent) {
                        "Plan Actual"
                    } else if (title == "Free") {
                        "Básico"
                    } else {
                        "Próximamente"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}