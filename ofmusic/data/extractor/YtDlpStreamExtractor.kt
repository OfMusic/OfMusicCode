package com.haziel.ofmusic.data.extractor

import android.content.Context
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class YtDlpStreamExtractor(
    context: Context? = null
) : MediaStreamExtractor {

    private val tag = "YtDlpExtractor"
    private val cache: UrlCache? = context?.let { UrlCache(it) }

    companion object {
        private val mutex = Mutex()
    }

    override suspend fun getAudioUrl(videoUrl: String): String =
        withContext(Dispatchers.IO) {
            val cleanId = extractVideoId(videoUrl)
            val cacheKey = "audio_$cleanId"

            // Cache hit
            cache?.get(cacheKey)?.let {
                Log.d(tag, "▶ Audio desde cache: $cleanId")
                return@withContext it
            }

            // yt-dlp es single-thread: serializar
            mutex.withLock {
                // Double-check después de adquirir el lock
                cache?.get(cacheKey)?.let { return@withLock it }

                val request = YoutubeDLRequest("https://www.youtube.com/watch?v=$cleanId").apply {
                    addOption("-f", "bestaudio/best")
                    addOption("-g")
                    addOption("--no-playlist")
                    addOption("--no-warnings")
                    addOption("--no-check-certificate")
                    addOption("--socket-timeout", "15")
                    addOption("--retries", "3")
                    addOption("--extractor-retries", "2")
                    addOption("--no-check-formats")
                    // Cliente android: YouTube responde más rápido que el web
                    addOption("--extractor-args", "youtube:player_client=android")
                }

                val response = try {
                    YoutubeDL.getInstance().execute(request)
                } catch (e: Exception) {
                    throw MediaExtractionException("Error yt-dlp audio: ${e.message}")
                }

                val url = response.out.trim().lines().firstOrNull { it.startsWith("http") }
                    ?: throw MediaExtractionException("URL vacía")

                if (!url.contains("googlevideo.com") && !url.contains("mime=audio")) {
                    throw MediaExtractionException("URL de audio inválida")
                }

                cache?.put(cacheKey, url)
                Log.d(tag, "✓ Audio extraído: $cleanId")
                url
            }
        }

    override suspend fun getVideoUrl(videoUrl: String): String =
        withContext(Dispatchers.IO) {
            val cleanId = extractVideoId(videoUrl)
            val cacheKey = "video_$cleanId"

            cache?.get(cacheKey)?.let {
                Log.d(tag, "▶ Video desde cache: $cleanId")
                return@withContext it
            }

            mutex.withLock {
                cache?.get(cacheKey)?.let { return@withLock it }

                val request = YoutubeDLRequest("https://www.youtube.com/watch?v=$cleanId").apply {
                    // Cadena de fallback robusta SIN restricción de extensión
                    // (muchos videos solo tienen webm/vp9 para video-only).
                    addOption(
                        "-f",
                        "bestvideo[height<=480]/bestvideo[height<=720]/bestvideo/best"
                    )
                    addOption("-g")
                    addOption("--no-playlist")
                    addOption("--no-warnings")
                    addOption("--socket-timeout", "15")
                    addOption("--retries", "2")
                    // ⚠️ NO usamos player_client=android para video:
                    // ese cliente no ofrece streams video-only.
                    // Cliente por defecto (web/tv) sí los ofrece.
                }

                val response = try {
                    YoutubeDL.getInstance().execute(request)
                } catch (e: Exception) {
                    throw MediaExtractionException("Error yt-dlp video: ${e.message}")
                }

                val url = response.out.trim().lines()
                    .firstOrNull { it.startsWith("http") && !it.contains("manifest") && !it.contains("m3u8") }
                    ?: throw MediaExtractionException("URL de video vacía o es manifest")

                if (!url.contains("googlevideo.com") || !url.contains("mime=video")) {
                    throw MediaExtractionException("URL de video inválida")
                }

                cache?.put(cacheKey, url)
                Log.d(tag, "✓ Video extraído: $cleanId")
                url
            }
        }

    private fun extractVideoId(input: String): String {
        val trimmed = input.trim()
        if (trimmed.length == 11 && !trimmed.contains("/")) return trimmed
        if (trimmed.contains("youtu.be/")) {
            val id = trimmed.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
            if (id.length == 11) return id
        }
        if (trimmed.contains("watch?v=")) {
            val id = trimmed.substringAfter("watch?v=").substringBefore("&").substringBefore("?")
            if (id.length == 11) return id
        }
        return trimmed
    }
}