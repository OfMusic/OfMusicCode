package com.haziel.ofmusic.data.search

import android.util.Log
import com.haziel.ofmusic.domain.model.SearchResult
import com.haziel.ofmusic.domain.search.YouTubeSearcher
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class YtDlpYouTubeSearcher : YouTubeSearcher {

    private val tag = "YtDlpSearcher"

    private val unofficialKeywords = listOf(
        "cover", "reaction", "reacción", "tutorial", "como tocar", "letra", "lyrics",
        "karaoke", "instrumental", "remake", "fan made", "fanmade", "nightcore",
        "speed up", "slowed", "parodia", "bass boosted", "8d audio", "gameplay",
        "review", "opinion", "analysis", "vlog", "interview", "podcast", "chords",
        "acordes", "sub español", "sub espanol", "traducida"
    )

    private val unofficialChannels = listOf(
        "lyrics", "letras", "covers", "reaction", "karaoke", "sub español",
        "sub espanol", "vibes", "music video reaction", "tutoriales", "chords",
        "acordes", "fan", "fandom"
    )

    override suspend fun search(query: String, maxResults: Int): List<SearchResult> =
        withContext(Dispatchers.IO) {
            val trimmedQuery = query.trim()
            if (trimmedQuery.isBlank()) return@withContext emptyList()

            val searchQuery = "ytsearch${maxResults + 10}:$query"

            val request = YoutubeDLRequest(searchQuery)
            request.addOption("--dump-json")
            request.addOption("--flat-playlist")
            request.addOption("--no-warnings")
            request.addOption("--socket-timeout", "10")

            val response = try {
                YoutubeDL.getInstance().execute(request)
            } catch (e: Exception) {
                Log.e(tag, "Error ejecutando yt-dlp search: ${e.message}", e)
                return@withContext emptyList()
            }

            if (response.out.isBlank()) return@withContext emptyList()

            val rawResults = response.out
                .lineSequence()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    try {
                        val obj = JSONObject(line)
                        val videoId = obj.optString("id", "")
                        if (videoId.isBlank()) return@mapNotNull null

                        val title = obj.optString("title", "")
                        if (title.isBlank()) return@mapNotNull null

                        val channel = if (obj.has("channel") && !obj.isNull("channel")) {
                            obj.getString("channel")
                        } else if (obj.has("uploader") && !obj.isNull("uploader")) {
                            obj.getString("uploader")
                        } else {
                            "Desconocido"
                        }

                        val duration = obj.optInt("duration", 0)

                        val thumbnail = if (obj.has("thumbnail") && !obj.isNull("thumbnail") && obj.getString("thumbnail").isNotBlank()) {
                            obj.getString("thumbnail")
                        } else {
                            "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"
                        }

                        val views = if (obj.has("view_count") && !obj.isNull("view_count")) {
                            obj.getLong("view_count")
                        } else {
                            null
                        }

                        val isOfficial = isOfficialChannel(channel, trimmedQuery)

                        SearchResult(
                            videoId = videoId,
                            title = title,
                            channelName = channel,
                            durationSeconds = duration,
                            thumbnailUrl = thumbnail,
                            isOfficialArtist = isOfficial,
                            viewCount = views
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                .filter { isOfficialAndValid(it) }
                .toList()

            rawResults.sortedWith(
                compareByDescending<SearchResult> { it.isOfficialArtist }
                    .thenByDescending { it.viewCount ?: 0L }
            )
                .take(maxResults)
        }

    private fun isOfficialAndValid(result: SearchResult): Boolean {
        val titleLower = result.title.lowercase()
        val channelLower = result.channelName.lowercase()

        if (!result.isOfficialArtist) return false
        if (unofficialKeywords.any { titleLower.contains(it) }) return false
        if (unofficialChannels.any { channelLower.contains(it) }) return false

        return true
    }

    private fun isOfficialChannel(channelName: String, query: String): Boolean {
        val lower = channelName.lowercase().trim()
        val queryLower = query.lowercase().trim()

        if (lower.endsWith("- topic") || lower.endsWith(" - topic")) return true
        if (lower.contains("vevo")) return true

        val officialLabels = listOf(
            "records", "recordings", "music group", "entertainment", "publishing",
            "warner music", "sony music", "universal music", "atlantic records",
            "columbia records", "epic records", "republic records", "interscope",
            "capitol records", "rca records", "def jam", "virgin music", "rimas entertainment"
        )
        if (officialLabels.any { lower.contains(it) }) return true

        val queryWords = queryLower.split(" ").filter { it.length > 2 }
        val isArtistNameMatch = queryWords.isNotEmpty() && queryWords.all { word -> lower.contains(word) }

        if (isArtistNameMatch && (lower.contains("official") || lower.contains("oficial") || lower.contains("channel") || lower.contains("canal"))) {
            return true
        }

        if (isArtistNameMatch && lower.length <= queryLower.length + 10) {
            return true
        }

        return false
    }
}
