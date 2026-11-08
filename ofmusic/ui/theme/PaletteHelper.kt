package com.haziel.ofmusic.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GlassColors(
    val dominant: Color,
    val vibrant: Color,
    val background: Color,
    val surfaceTint: Color
)

object PaletteHelper {

    suspend fun extractFromUrl(context: Context, imageUrl: String): GlassColors? = withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .build()

            val result = loader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as BitmapDrawable).bitmap
                return@withContext extractFromBitmap(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    private fun extractFromBitmap(bitmap: Bitmap): GlassColors {
        val cropX = (bitmap.width * 0.10f).toInt().coerceAtLeast(0)
        val cropY = (bitmap.height * 0.10f).toInt().coerceAtLeast(0)
        val cropW = (bitmap.width - 2 * cropX).coerceAtLeast(1)
        val cropH = (bitmap.height - 2 * cropY).coerceAtLeast(1)

        val innerBitmap = try {
            Bitmap.createBitmap(bitmap, cropX, cropY, cropW, cropH)
        } catch (_: Exception) {
            bitmap
        }

        val palette = Palette.Builder(innerBitmap)
            .resizeBitmapArea(112 * 112)
            .addFilter { _, hsl ->
                hsl[2] in 0.00f..0.96f
            }
            .generate()

        val dominantSwatch = palette.dominantSwatch
            ?: palette.darkMutedSwatch
            ?: palette.mutedSwatch
            ?: palette.vibrantSwatch

        val vibrantSwatch = palette.vibrantSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.lightVibrantSwatch
            ?: dominantSwatch

        val rawDominant = dominantSwatch?.rgb ?: 0xFF120D17.toInt()
        val rawVibrant = vibrantSwatch?.rgb ?: rawDominant

        val bgArgb = ensureDeepGlassColor(rawDominant)

        return GlassColors(
            dominant = Color(rawDominant),
            vibrant = Color(rawVibrant),
            background = Color(bgArgb),
            surfaceTint = Color(rawVibrant)
        )
    }

    private fun ensureDeepGlassColor(colorArgb: Int): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(colorArgb, hsl)

        // Si la portada es mayormente negra o gris oscura/acromática
        val isAchromatic = hsl[1] < 0.18f || hsl[2] < 0.15f
        if (isAchromatic) {
            // Preservar tono negro/gris oscuro profundo sin forzar saturación
            hsl[2] = hsl[2].coerceIn(0.02f, 0.08f)
            hsl[1] = hsl[1].coerceAtMost(0.15f)
        } else {
            // Para colores con tono visible, asegurar un fondo oscuro tintado
            hsl[2] = hsl[2].coerceIn(0.06f, 0.14f)
            if (hsl[1] < 0.20f) hsl[1] = 0.25f
        }
        return ColorUtils.HSLToColor(hsl)
    }
}
