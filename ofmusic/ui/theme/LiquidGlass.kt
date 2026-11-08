package com.haziel.ofmusic.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.liquidGlass(
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    alpha: Float = 0.05f
) = this
    .clip(shape)
    .background(Color.White.copy(alpha = alpha))
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.3f),
                Color.White.copy(alpha = 0.05f)
            )
        ),
        shape = shape
    )