package com.haziel.ofmusic.ui.main.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun VinylArtwork(
    imageUrl: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 50.dp
) {
    // Animación de rotación infinita
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                rotation.animateTo(
                    targetValue = rotation.value + 360f,
                    animationSpec = tween(durationMillis = 12000, easing = LinearEasing)
                )
            }
        } else {
            // Se detiene suavemente en la posición actual
            rotation.stop()
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .rotate(rotation.value) // 👈 Aplica la rotación
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1E1E1E), Color(0xFF0A0A0A)) // Efecto de vinilo negro
                ),
                shape = CircleShape
            )
            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Surcos del vinilo (círculos sutiles)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(size * 0.08f)
                .border(1.dp, Color.White.copy(alpha = 0.04f), CircleShape)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(size * 0.15f)
                .border(1.dp, Color.White.copy(alpha = 0.04f), CircleShape)
        )

        // Portada (Centro del vinilo)
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize(0.6f) // La imagen ocupa el 60% del vinilo
                    .clip(CircleShape)
                    .border(1.dp, Color.Black.copy(alpha = 0.5f), CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.6f)
                    .clip(CircleShape)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.2f)
                )
            }
        }

        // Agujero central
        Box(
            modifier = Modifier
                .size(size * 0.08f)
                .background(Color.Black, CircleShape)
                .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
        )
    }
}