package com.haziel.ofmusic.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haziel.ofmusic.domain.tier.TierLimits
import com.haziel.ofmusic.ui.theme.liquidGlass
import com.haziel.ofmusic.domain.tier.UserTier

@Composable
fun TierStatusCard(
    tier: UserTier,
    downloadsCount: Int,
    playlistsCount: Int,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val limits = TierLimits.forTier(tier)

    // Si es ilimitado, mostramos el infinito
    val downloadsText = if (limits.maxDownloads == Int.MAX_VALUE) "$downloadsCount / ∞"
    else "$downloadsCount / ${limits.maxDownloads}"
    val playlistsText = if (limits.maxPlaylists == Int.MAX_VALUE) "$playlistsCount / ∞"
    else "$playlistsCount / ${limits.maxPlaylists}"

    val isPro = tier == UserTier.PRO
    val isStandard = tier == UserTier.STANDARD
    val isFree = tier == UserTier.FREE

    val accentColor = when {
        isPro -> Color(0xFFFFD700) // Dorado
        isStandard -> Color(0xFF00E676) // Verde
        else -> Color.White.copy(alpha = 0.7f) // Gris
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.22f),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Tu Plan",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tier.displayName,
                        color = accentColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.width(8.dp))
                    if (isPro) Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = accentColor)
                    else if (isStandard) Icon(Icons.Default.Star, contentDescription = null, tint = accentColor)
                }
            }

            if (isFree) {
                Button(
                    onClick = onUpgradeClick,
                    modifier = Modifier
                        .height(36.dp)
                        .liquidGlass(RoundedCornerShape(50), alpha = 0.25f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700).copy(alpha = 0.25f),
                        contentColor = Color(0xFFFFD700)
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Mejorar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Contadores
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Descargas", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                Text(downloadsText, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Column {
                Text("Playlists", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                Text(playlistsText, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}