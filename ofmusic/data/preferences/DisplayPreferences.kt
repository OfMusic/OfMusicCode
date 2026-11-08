package com.haziel.ofmusic.data.preferences

import android.content.Context
import androidx.compose.ui.graphics.Color

enum class AppThemeOption(
    val key: String,
    val title: String,
    val subtitle: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val isVipOnly: Boolean = false
) {
    VIOLET_DEFAULT(
        key = "violet_default",
        title = "OfMusic Violeta",
        subtitle = "Púrpura neón predeterminado",
        primaryColor = Color(0xFFA855F7),
        secondaryColor = Color(0xFF6D28D9),
        backgroundColor = Color(0xFF0D0A14),
        surfaceColor = Color(0xFF1E152A)
    ),
    LIQUID_WHITE(
        key = "liquid_white",
        title = "Liquid Glass Blanco",
        subtitle = "Cristal helado y translúcido",
        primaryColor = Color(0xFFFFFFFF),
        secondaryColor = Color(0xFFCBD5E1),
        backgroundColor = Color(0xFF0B0F17),
        surfaceColor = Color(0xFF1E293B)
    ),
    ROYAL_BLUE_VIP(
        key = "royal_blue_vip",
        title = "Azul Rey Exclusivo para mi Beba",
        subtitle = "Exclusivo para nahucanyenyyazmin@gmail.com",
        primaryColor = Color(0xFF2563EB),
        secondaryColor = Color(0xFF1D4ED8),
        backgroundColor = Color(0xFF060B18),
        surfaceColor = Color(0xFF0F172A),
        isVipOnly = true
    ),
    OBSIDIAN_GREEN(
        key = "obsidian_green",
        title = "Obsidiana Esmeralda",
        subtitle = "Verde místico y profundo",
        primaryColor = Color(0xFF10B981),
        secondaryColor = Color(0xFF047857),
        backgroundColor = Color(0xFF06120E),
        surfaceColor = Color(0xFF0F291E)
    ),
    CYBERPUNK_CYAN(
        key = "cyberpunk_cyan",
        title = "Cyberpunk Neón",
        subtitle = "Cian eléctrico futurista",
        primaryColor = Color(0xFF06B6D4),
        secondaryColor = Color(0xFF0891B2),
        backgroundColor = Color(0xFF07141A),
        surfaceColor = Color(0xFF112A36)
    );

    companion object {
        fun fromKey(key: String): AppThemeOption {
            return entries.find { it.key == key } ?: VIOLET_DEFAULT
        }
    }
}

class DisplayPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(
        "ofmusic_display_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        const val MODE_AUTO = 0
        const val MODE_60HZ = 60
        const val MODE_90HZ = 90
        const val MODE_120HZ = 120
    }

    fun getRefreshRate(): Int = prefs.getInt("refresh_rate", MODE_AUTO)

    fun setRefreshRate(hz: Int) {
        prefs.edit().putInt("refresh_rate", hz).apply()
    }

    fun getSelectedTheme(): AppThemeOption {
        val key = prefs.getString("app_theme_key", AppThemeOption.VIOLET_DEFAULT.key) ?: AppThemeOption.VIOLET_DEFAULT.key
        return AppThemeOption.fromKey(key)
    }

    fun setSelectedTheme(theme: AppThemeOption) {
        prefs.edit().putString("app_theme_key", theme.key).apply()
    }

    fun getAutoDownloadFavorites(): Boolean = prefs.getBoolean("auto_download_favorites", false)

    fun setAutoDownloadFavorites(enabled: Boolean) {
        prefs.edit().putBoolean("auto_download_favorites", enabled).apply()
    }
}