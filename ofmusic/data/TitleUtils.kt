package com.haziel.ofmusic.data

object TitleUtils {

    private val patterns = listOf(
        Regex("""\s*[\(\[]\s*(official\s*)?(music\s*)?(video|audio|lyrics?|visualizer|clip|hd|hq|4k|1080p)\s*[\)\]]""", RegexOption.IGNORE_CASE),
        Regex("""\s*[\(\[]\s*official\s*[\)\]]""", RegexOption.IGNORE_CASE),
        Regex("""\s*[\(\[]\s*audio\s*[\)\]]""", RegexOption.IGNORE_CASE),
        Regex("""\s*-\s*topic$""", RegexOption.IGNORE_CASE),
        Regex("""\s*[\(\[]\s*lyric\s*video\s*[\)\]]""", RegexOption.IGNORE_CASE),
    )

    /** Limpia títulos de YouTube: "(Official Video)", "[HD]", "- Topic", etc. */
    fun cleanYouTubeTitle(raw: String): String {
        var t = raw
        patterns.forEach { t = t.replace(it, "") }
        // Colapsar espacios múltiples
        t = t.replace(Regex("""\s{2,}"""), " ").trim()
        // Si quedó vacío, devolver original
        return t.ifBlank { raw }
    }

    /** Intenta extraer artista del título tipo "Artista - Canción" */
    fun splitArtistFromTitle(title: String): Pair<String, String>? {
        val dashIdx = title.indexOf(" - ")
        if (dashIdx <= 0) return null
        val artist = title.substring(0, dashIdx).trim()
        val song = title.substring(dashIdx + 3).trim()
        if (artist.length in 2..60 && song.length in 2..80) {
            return artist to song
        }
        return null
    }
}