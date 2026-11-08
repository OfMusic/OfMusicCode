package com.haziel.ofmusic.ui.main.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Título de la app: "Of" en blanco + "Music" en el color del tema.
 * Se usa en MainScreen y en SplashScreen.
 */
@Composable
fun OfMusicTitleText(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 22.sp
) {
    val primary = MaterialTheme.colorScheme.primary
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Black)) {
                append("Of")
            }
            withStyle(SpanStyle(color = primary, fontWeight = FontWeight.Black)) {
                append("Music")
            }
        },
        fontSize = fontSize,
        letterSpacing = 0.5.sp,
        modifier = modifier
    )
}

/**
 * Versión alternativa con icono, por si la usas en algún lugar.
 */
@Composable
fun OfMusicLogoHeader(
    modifier: Modifier = Modifier
) {
    OfMusicTitleText(modifier = modifier, fontSize = 24.sp)
}