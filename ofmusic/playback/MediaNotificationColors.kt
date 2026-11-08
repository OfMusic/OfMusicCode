package com.haziel.ofmusic.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object MediaNotificationColors {

    private const val TAG = "MediaNotificationColors"

    data class Colors(
        val primary: Int,
        val onPrimary: Int,
        val background: Int,
        val accent: Int
    ) {
        companion object {
            val DEFAULT = Colors(
                primary = 0xFF00E676.toInt(),
                onPrimary = 0xFFFFFFFF.toInt(),
                background = 0xFF1A1210.toInt(),
                accent = 0xFFFFFFFF.toInt()
            )
        }
    }

    private val cache = ConcurrentHashMap<String, Colors>()

    fun get(artworkUrl: String?): Colors {
        if (artworkUrl.isNullOrBlank()) return Colors.DEFAULT
        return cache[artworkUrl] ?: Colors.DEFAULT
    }

    fun put(artworkUrl: String, colors: Colors) {
        cache[artworkUrl] = colors
        if (cache.size > 40) cache.clear()
    }

    suspend fun precompute(context: Context, artworkUrl: String?): Colors =
        withContext(Dispatchers.IO) {
            if (artworkUrl.isNullOrBlank()) return@withContext Colors.DEFAULT

            cache[artworkUrl]?.let { return@withContext it }

            try {
                val request = ImageRequest.Builder(context)
                    .data(artworkUrl)
                    .allowHardware(false)
                    .size(200, 200)
                    .build()

                val result = context.imageLoader.execute(request)
                val bitmap = (result as? SuccessResult)?.drawable?.let {
                    (it as? BitmapDrawable)?.bitmap
                } ?: return@withContext Colors.DEFAULT

                val colors = buildFromBitmap(bitmap)
                put(artworkUrl, colors)
                Log.d(TAG, "✓ Colores precalculados")
                colors
            } catch (e: Exception) {
                Log.w(TAG, "Error extrayendo colores: ${e.message}")
                Colors.DEFAULT
            }
        }

    private fun buildFromBitmap(bitmap: Bitmap): Colors {
        val palette = Palette.from(bitmap)
            .maximumColorCount(16)
            .clearFilters()
            .generate()

        val vibrant = palette.getVibrantColor(
            palette.getDominantColor(Colors.DEFAULT.primary)
        )
        val darkVibrant = palette.getDarkVibrantColor(
            palette.getDarkMutedColor(Colors.DEFAULT.background)
        )

        val primaryLuminance = ColorUtils.calculateLuminance(vibrant)
        val onPrimary = if (primaryLuminance > 0.5) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()

        return Colors(
            primary = vibrant,
            onPrimary = onPrimary,
            background = darkVibrant,
            accent = onPrimary
        )
    }
}