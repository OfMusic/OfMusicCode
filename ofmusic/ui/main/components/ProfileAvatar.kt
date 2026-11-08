package com.haziel.ofmusic.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun ProfileAvatar(
    photoUri: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showGlassBorder: Boolean = true
) {
    // Contenedor exterior: fondo glass + borde sutil
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (showGlassBorder) Color.White.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.primaryContainer
            )
            .then(
                if (showGlassBorder) {
                    Modifier.border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.35f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        shape = CircleShape
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Contenido interno (foto o inicial)
        Box(
            modifier = Modifier
                .size(size - 4.dp)
                .clip(CircleShape)
                .background(
                    if (showGlassBorder) Color.Transparent
                    else MaterialTheme.colorScheme.primaryContainer
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!photoUri.isNullOrBlank()) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Foto de perfil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val initial = name.firstOrNull()?.uppercaseChar()?.toString()
                if (initial != null) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (showGlassBorder) Color.White
                        else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Perfil",
                        tint = if (showGlassBorder) Color.White
                        else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}