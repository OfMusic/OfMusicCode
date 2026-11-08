package com.haziel.ofmusic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.haziel.ofmusic.data.preferences.AppThemeOption

@Composable
fun OfMusicTheme(
    selectedTheme: AppThemeOption = AppThemeOption.VIOLET_DEFAULT,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = selectedTheme.primaryColor,
        onPrimary = if (selectedTheme == AppThemeOption.LIQUID_WHITE) Color.Black else Color.White,
        secondary = selectedTheme.secondaryColor,
        tertiary = selectedTheme.primaryColor,
        background = selectedTheme.backgroundColor,
        surface = selectedTheme.surfaceColor,
        onBackground = Color.White,
        onSurface = Color.White,
        onSurfaceVariant = Color(0xFFA0A0A0)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}