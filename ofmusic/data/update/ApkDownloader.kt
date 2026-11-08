package com.haziel.ofmusic.data.update

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class ApkDownloader(private val context: Context) {

    private val tag = "ApkDownloader"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun downloadWithFallback(
        apkUrl: String,
        versionName: String = "",
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val candidates = mutableListOf<String>()

        if (apkUrl.isNotBlank()) candidates.add(apkUrl)

        val baseUrl = if (apkUrl.contains("/download/")) {
            apkUrl.substringBeforeLast("/") + "/"
        } else {
            "https://github.com/OfMusic/ofmusic-releases/releases/latest/download/"
        }

        if (versionName.isNotBlank()) {
            val cleanVer = versionName.replace("-Beta", "").replace("-beta", "").trim()
            candidates.add(baseUrl + "OfMusic.v" + versionName + ".apk")
            candidates.add(baseUrl + "OfMusic_v" + versionName + ".apk")
            candidates.add(baseUrl + "OfMusic v" + versionName + ".apk")
        }

        candidates.add(baseUrl + "app-release.apk")
        candidates.add(baseUrl + "app-debug.apk")

        val uniqueCandidates = candidates.distinct()

        for (candidateUrl in uniqueCandidates) {
            Log.d(tag, "🔍 Probando descargar APK desde: $candidateUrl")
            val result = downloadSingleUrl(candidateUrl, onProgress)
            if (result != null && result.exists() && result.length() > 1024) {
                Log.d(tag, "✓ Descarga exitosa usando candidate: $candidateUrl")
                return@withContext result
            }
        }

        Log.e(tag, "✗ No se pudo descargar el APK desde ninguna URL")
        null
    }

    private fun downloadSingleUrl(
        url: String,
        onProgress: (Int) -> Unit
    ): File? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "OfMusic-Updater")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(tag, "HTTP ${response.code} para $url")
                    return null
                }

                val body = response.body ?: return null
                val contentLength = body.contentLength()

                val apkFile = File(context.cacheDir, "ofmusic_update.apk")
                if (apkFile.exists()) apkFile.delete()

                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (contentLength > 0) {
                                val pct = ((totalRead * 100) / contentLength).toInt()
                                onProgress(pct.coerceIn(0, 100))
                            }
                        }
                    }
                }

                if (apkFile.exists() && apkFile.length() > 1024) {
                    apkFile
                } else null
            }
        } catch (e: Exception) {
            Log.w(tag, "Fallo al consultar candidate $url: ${e.message}")
            null
        }
    }
}