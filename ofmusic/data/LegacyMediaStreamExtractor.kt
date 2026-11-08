package com.haziel.ofmusic.data

import android.content.Context
import com.haziel.ofmusic.data.extractor.MediaStreamExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracción de URLs de transmisión de audio y video.
 */
interface LegacyMediaStreamExtractor {
    suspend fun getAudioUrl(videoId: String): String
    suspend fun getVideoUrl(videoId: String): String
}
@Deprecated("Reemplazada por data.extractor.YtDlpStreamExtractor")
class LegacyDefaultMediaStreamExtractor(private val context: Context) : MediaStreamExtractor {

    override suspend fun getAudioUrl(videoId: String): String = withContext(Dispatchers.IO) {
        val cleanId = extractVideoId(videoId)
        "https://www.youtube.com/watch?v=$cleanId"
    }

    override suspend fun getVideoUrl(videoId: String): String = withContext(Dispatchers.IO) {
        val cleanId = extractVideoId(videoId)
        "https://www.youtube.com/watch?v=$cleanId"
    }

    private fun extractVideoId(input: String): String {
        val trimmed = input.trim()
        if (trimmed.length == 11 && !trimmed.contains("/") && !trimmed.contains(":") && !trimmed.contains("?")) {
            return trimmed
        }
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
