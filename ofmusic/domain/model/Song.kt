package com.haziel.ofmusic.domain.model

import java.io.File
import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer

@Serializable
@Immutable
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String = "Sencillo",
    val artworkUrl: String = "",
    val durationMs: Long = 0L,
    val youtubeId: String? = null,
    val audioUrl: String? = null,
    val videoUrl: String? = null,
    val isLiked: Boolean = false,
    val isDownloaded: Boolean = false,
    val localPath: String? = null
) {
    fun getEffectiveAudioSource(): String? {
        // 1. Archivo local descargado (prioridad máxima)
        if (!localPath.isNullOrBlank()) {
            val file = File(localPath)
            if (file.exists() && file.length() > 1024) {
                return localPath
            }
        }
        // 2. URL del stream extraída (googlevideo.com)
        if (!audioUrl.isNullOrBlank() && audioUrl.contains("googlevideo.com")) {
            return audioUrl
        }
        // 3. No hay fuente válida → null. El llamador debe extraerla antes.
        return null
    }

    fun getEffectiveVideoSource(): String? {
        if (!videoUrl.isNullOrBlank() && videoUrl.contains("googlevideo.com")) {
            return videoUrl
        }
        return null
    }

    /**
     * Devuelve la URL de la portada más confiable.
     * Valida que la URL guardada siga el patrón limpio esperado.
     * Si no, la regenera siempre desde el youtubeId.
     */
    fun getEffectiveArtworkUrl(): String {
        // Solo confiar en URLs limpias con patrón conocido
        val cleanPattern = Regex("""^https://i\.ytimg\.com/vi/[A-Za-z0-9_-]{11}/(hq|mq|sd|maxres)default\.jpg$""")

        if (cleanPattern.matches(artworkUrl)) {
            return artworkUrl
        }

        // Regenerar desde el videoId (siempre funciona)
        if (!youtubeId.isNullOrBlank() && youtubeId.length == 11) {
            return "https://i.ytimg.com/vi/$youtubeId/hqdefault.jpg"
        }

        return ""
    }

    companion object {
        fun fromJson(json: String): Song {
            val o = org.json.JSONObject(json)
            return Song(
                id = o.getLong("id"),
                title = o.getString("title"),
                artist = o.getString("artist"),
                album = o.optString("album", "Sencillo"),
                artworkUrl = o.optString("artworkUrl", ""),
                durationMs = o.optLong("durationMs", 0L),
                youtubeId = o.optString("youtubeId", null).takeIf { it != "null" },
                audioUrl = o.optString("audioUrl", null).takeIf { it != "null" },
                videoUrl = o.optString("videoUrl", null).takeIf { it != "null" },
                isLiked = o.optBoolean("isLiked", false),
                isDownloaded = o.optBoolean("isDownloaded", false),
                localPath = o.optString("localPath", null).takeIf { it != "null" }
            )
        }


    }
    fun toJson(): String = org.json.JSONObject().apply {
        put("id", id)
        put("title", title)
        put("artist", artist)
        put("album", album)
        put("artworkUrl", artworkUrl)
        put("durationMs", durationMs)
        put("youtubeId", youtubeId)
        put("audioUrl", audioUrl)
        put("videoUrl", videoUrl)
        put("isLiked", isLiked)
        put("isDownloaded", isDownloaded)
        put("localPath", localPath)
    }.toString()
}


