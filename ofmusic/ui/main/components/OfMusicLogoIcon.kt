package com.haziel.ofmusic.ui.main.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.haziel.ofmusic.ui.theme.OfMusicTheme

@Composable
fun OfMusicLogoIcon(
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
    primaryColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.primary
) {
    Canvas(
        modifier = modifier.size(size),
    ) {
        val canvasSize = this.size.minDimension
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f

        val outerRadius = canvasSize * 0.38f
        drawCircle(
            color = Color(0x40000000),
            radius = outerRadius * 1.03f,
            center = Offset(cx, cy + (canvasSize * 0.015f)),
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.35f),
                    Color(0xFF180F2A),
                    Color(0xFF10081D),
                ),
                center = Offset(cx, cy),
                radius = outerRadius,
            ),
            radius = outerRadius,
            center = Offset(cx, cy),
        )

        val innerRadius = canvasSize * 0.215f
        val innerCenter = Offset(cx + (canvasSize * 0.015f), cy - (canvasSize * 0.005f))
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(
                    primaryColor,
                    primaryColor.copy(alpha = 0.80f),
                    primaryColor.copy(alpha = 0.50f),
                ),
                start = Offset(innerCenter.x - innerRadius, innerCenter.y - innerRadius),
                end = Offset(innerCenter.x + innerRadius, innerCenter.y + innerRadius),
            ),
            radius = innerRadius,
            center = innerCenter,
        )

        val stemLeft = cx + (canvasSize * 0.005f)
        val stemRight = cx + (canvasSize * 0.062f)
        val flagRight = cx + (canvasSize * 0.150f)
        val flagTop = cy - (canvasSize * 0.250f)
        val flagBottom = cy - (canvasSize * 0.125f)
        val stemBottom = cy + (canvasSize * 0.115f)

        val noteHeadCenter = Offset(cx - (canvasSize * 0.065f), cy + (canvasSize * 0.115f))
        val noteHeadWidth = canvasSize * 0.235f
        val noteHeadHeight = canvasSize * 0.185f
        val rotationAngle = -26f

        val shadowOffset = Offset(canvasSize * 0.016f, canvasSize * 0.022f)
        val shadowColor = Color(0x80080016)

        rotate(degrees = rotationAngle, pivot = noteHeadCenter + shadowOffset) {
            drawOval(
                color = shadowColor,
                topLeft = Offset(
                    (noteHeadCenter.x + shadowOffset.x) - (noteHeadWidth / 2f),
                    (noteHeadCenter.y + shadowOffset.y) - (noteHeadHeight / 2f),
                ),
                size = Size(noteHeadWidth, noteHeadHeight),
            )
        }

        val shadowStemPath = Path().apply {
            moveTo(stemLeft + shadowOffset.x, flagTop + shadowOffset.y)
            lineTo((flagRight + shadowOffset.x) - (canvasSize * 0.02f), flagTop + shadowOffset.y)
            quadraticTo(
                flagRight + shadowOffset.x,
                flagTop + shadowOffset.y,
                flagRight + shadowOffset.x,
                flagTop + shadowOffset.y + (canvasSize * 0.02f),
            )
            lineTo(flagRight + shadowOffset.x, (flagBottom + shadowOffset.y) - (canvasSize * 0.01f))
            quadraticTo(
                flagRight + shadowOffset.x,
                flagBottom + shadowOffset.y,
                (flagRight + shadowOffset.x) - (canvasSize * 0.015f),
                flagBottom + shadowOffset.y,
            )
            lineTo(stemRight + shadowOffset.x, (flagBottom + shadowOffset.y) - (canvasSize * 0.025f))
            lineTo(stemRight + shadowOffset.x, stemBottom + shadowOffset.y)
            lineTo(stemLeft + shadowOffset.x, stemBottom + shadowOffset.y)
            close()
        }
        drawPath(shadowStemPath, color = shadowColor)

        val stemFlagPath = Path().apply {
            moveTo(stemLeft + (canvasSize * 0.02f), flagTop)
            quadraticTo(stemLeft, flagTop, stemLeft, flagTop + (canvasSize * 0.02f))
            lineTo(stemLeft, stemBottom)
            lineTo(stemRight, stemBottom)
            lineTo(stemRight, flagBottom - (canvasSize * 0.02f))
            lineTo(flagRight - (canvasSize * 0.015f), flagBottom)
            quadraticTo(
                flagRight,
                flagBottom,
                flagRight,
                flagBottom - (canvasSize * 0.015f),
            )
            lineTo(flagRight, flagTop + (canvasSize * 0.025f))
            quadraticTo(
                flagRight,
                flagTop,
                flagRight - (canvasSize * 0.025f),
                flagTop,
            )
            close()
        }

        drawPath(
            path = stemFlagPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryColor,
                    primaryColor.copy(alpha = 0.8f),
                    primaryColor.copy(alpha = 0.5f),
                ),
                startY = flagTop,
                endY = stemBottom,
            ),
        )

        val flagGlossPath = Path().apply {
            moveTo(stemLeft + (canvasSize * 0.02f), flagTop)
            lineTo(flagRight - (canvasSize * 0.025f), flagTop)
            quadraticTo(
                flagRight,
                flagTop,
                flagRight,
                flagTop + (canvasSize * 0.025f),
            )
            lineTo(flagRight, flagTop + (canvasSize * 0.065f))
            lineTo(stemLeft, flagTop + (canvasSize * 0.065f))
            lineTo(stemLeft, flagTop + (canvasSize * 0.02f))
            quadraticTo(stemLeft, flagTop, stemLeft + (canvasSize * 0.02f), flagTop)
            close()
        }

        drawPath(
            path = flagGlossPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x88FFFFFF),
                    Color(0x15FFFFFF),
                ),
            ),
        )

        rotate(degrees = rotationAngle, pivot = noteHeadCenter) {
            drawOval(
                brush = Brush.linearGradient(
                    colors = listOf(
                        primaryColor,
                        primaryColor.copy(alpha = 0.85f),
                        primaryColor.copy(alpha = 0.6f),
                    ),
                    start = Offset(
                        noteHeadCenter.x - (noteHeadWidth / 2f),
                        noteHeadCenter.y - (noteHeadHeight / 2f),
                    ),
                    end = Offset(
                        noteHeadCenter.x + (noteHeadWidth / 2f),
                        noteHeadCenter.y + (noteHeadHeight / 2f),
                    ),
                ),
                topLeft = Offset(
                    noteHeadCenter.x - (noteHeadWidth / 2f),
                    noteHeadCenter.y - (noteHeadHeight / 2f),
                ),
                size = Size(noteHeadWidth, noteHeadHeight),
            )

            drawOval(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0x99FFFFFF),
                        Color(0x00FFFFFF),
                    ),
                    start = Offset(
                        noteHeadCenter.x - (noteHeadWidth * 0.4f),
                        noteHeadCenter.y - (noteHeadHeight * 0.4f),
                    ),
                    end = Offset(noteHeadCenter.x, noteHeadCenter.y),
                ),
                topLeft = Offset(
                    noteHeadCenter.x - (noteHeadWidth * 0.42f),
                    noteHeadCenter.y - (noteHeadHeight * 0.42f),
                ),
                size = Size(noteHeadWidth * 0.8f, noteHeadHeight * 0.5f),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OfMusicLogoIconPreview() {
    OfMusicTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090611)),
            contentAlignment = Alignment.Center,
        ) {
            OfMusicLogoIcon(size = 320.dp)
        }
    }
}