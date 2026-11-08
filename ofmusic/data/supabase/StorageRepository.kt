package com.haziel.ofmusic.data.supabase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class StorageRepository(private val context: Context) {

    private val tag = "StorageRepository"
    private val client = SupabaseClientProvider.getInstance()
    private val storage = client.storage

    /**
     * Sube una imagen (local content:// o file://) a Supabase Storage y retorna la URL pública HTTP.
     * Si ya es una URL http/https, la retorna tal cual.
     * Si falla el Storage en Supabase, guarda una copia local persistente y/o base64.
     */
    suspend fun uploadImageIfNeeded(uriString: String, folder: String = "covers"): String = withContext(Dispatchers.IO) {
        val trimmed = uriString.trim()
        if (trimmed.isBlank()) return@withContext ""
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("data:image/")) {
            return@withContext trimmed
        }

        try {
            val uri = Uri.parse(trimmed)
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext saveLocalCopy(trimmed, folder)

            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) return@withContext saveLocalCopy(trimmed, folder)

            // Redimensionar imagen para optimizar ancho de banda (máx 1200px)
            val maxDim = 1200
            val scale = (maxDim.toFloat() / Math.max(originalBitmap.width, originalBitmap.height)).coerceAtMost(1.0f)
            val scaledWidth = (originalBitmap.width * scale).toInt().coerceAtLeast(1)
            val scaledHeight = (originalBitmap.height * scale).toInt().coerceAtLeast(1)

            val resizedBitmap = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
            } else originalBitmap

            val stream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            val bytes = stream.toByteArray()

            val fileName = "${folder}/${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"

            return@withContext runCatching {
                val bucket = storage.from("public-media")
                bucket.upload(fileName, bytes) {
                    upsert = true
                }
                val publicUrl = bucket.publicUrl(fileName)
                Log.d(tag, "✓ Imagen subida a Supabase Storage: $publicUrl")
                publicUrl
            }.getOrElse { e ->
                Log.w(tag, "⚠ Falló subida a Supabase Storage (${e.message}), guardando copia persistente/base64")
                saveLocalCopy(trimmed, folder, bytes)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error procesando imagen: ${e.message}", e)
            saveLocalCopy(trimmed, folder)
        }
    }

    private fun saveLocalCopy(uriString: String, folder: String, bytes: ByteArray? = null): String {
        return try {
            val fileDir = File(context.filesDir, folder).apply { if (!exists()) mkdirs() }
            val destFile = File(fileDir, "img_${System.currentTimeMillis()}.jpg")

            if (bytes != null) {
                FileOutputStream(destFile).use { it.write(bytes) }
            } else {
                val uri = Uri.parse(uriString)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output -> input.copyTo(output) }
                }
            }

            val localUriStr = Uri.fromFile(destFile).toString()
            Log.d(tag, "✓ Copia persistente local creada: $localUriStr")
            localUriStr
        } catch (e: Exception) {
            Log.e(tag, "Error creando copia local: ${e.message}")
            if (bytes != null && bytes.size < 500_000) {
                "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else {
                uriString
            }
        }
    }
}