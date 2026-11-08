package com.haziel.ofmusic.data.lyrics

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class LyricsRepository {

    private val tag = "LyricsRepository"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val cache = mutableMapOf<String, List<LyricLine>>()

    suspend fun getLyrics(
        artist: String,
        title: String,
        durationMs: Long
    ): List<LyricLine> = withContext(Dispatchers.IO) {
        val key = "${artist.lowercase().trim()}|${title.lowercase().trim()}"
        cache[key]?.let { return@withContext it }

        try {
            val durationSec = (durationMs / 1000).toInt()
            val url = buildString {
                append("https://lrclib.net/api/get?")
                append("artist_name=").append(URLEncoder.encode(artist, "UTF-8"))
                append("&track_name=").append(URLEncoder.encode(title, "UTF-8"))
                if (durationSec > 0) append("&duration=").append(durationSec)
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "OfMusic/1.0 (https://github.com/haziel/ofmusic)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d(tag, "Sin letras para $key (HTTP ${response.code})")
                    cache[key] = emptyList()
                    return@withContext emptyList()
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) {
                    cache[key] = emptyList()
                    return@withContext emptyList()
                }

                val json = JSONObject(body)
                val synced = json.optString("syncedLyrics", "")
                val plain = json.optString("plainLyrics", "")

                val lines = when {
                    synced.isNotBlank() -> parseLrc(synced)
                    plain.isNotBlank() -> plain.lines()
                        .mapIndexed { i, text -> LyricLine(i * 4000L, text.trim()) }
                        .filter { it.text.isNotBlank() }
                    else -> emptyList()
                }

                cache[key] = lines
                Log.d(tag, "Letras obtenidas para $key: ${lines.size} líneas")
                lines
            }
        } catch (e: Exception) {
            Log.e(tag, "Error obteniendo letras: ${e.message}")
            emptyList()
        }
    }

    /** Parsea formato LRC: [mm:ss.xx] texto */
    private fun parseLrc(lrc: String): List<LyricLine> {
        val regex = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]\s*(.*)""")
        return lrc.lines()
            .mapNotNull { line ->
                val match = regex.find(line) ?: return@mapNotNull null
                val min = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
                val sec = match.groupValues[2].toLongOrNull() ?: return@mapNotNull null
                val fracStr = match.groupValues[3]
                val ms = when {
                    fracStr.isEmpty() -> 0L
                    fracStr.length == 2 -> fracStr.toLong() * 10
                    fracStr.length == 3 -> fracStr.toLong()
                    else -> 0L
                }
                val text = match.groupValues[4].trim()
                LyricLine(min * 60_000 + sec * 1000 + ms, text)
            }
            .filter { it.text.isNotBlank() }
            .sortedBy { it.timeMs }
    }
}