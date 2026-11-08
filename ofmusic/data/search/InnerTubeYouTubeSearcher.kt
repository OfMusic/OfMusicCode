package com.haziel.ofmusic.data.search

import android.util.Log
import com.haziel.ofmusic.domain.model.SearchResult
import com.haziel.ofmusic.domain.search.YouTubeSearcher
import com.metrolist.innertubex.InnerTube
import com.metrolist.innertubex.models.YouTubeClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class InnerTubeYouTubeSearcher : YouTubeSearcher {

    private val tag = "InnerTubeSearcher"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }

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

            try {
                val client = InnerTube(httpClient = httpClient)
                val response = client.search(
                    client = YouTubeClient.WEB,
                    query = trimmedQuery
                )
                val bodyText = response.bodyAsText()
                client.close()

                val root = json.parseToJsonElement(bodyText).jsonObject
                val results = parseSearchRoot(root, trimmedQuery)

                results
                    .filter { isOfficialAndValid(it, trimmedQuery) }
                    .sortedWith(
                        compareByDescending<SearchResult> { it.isOfficialArtist }
                            .thenByDescending { it.viewCount ?: 0L }
                    )
                    .take(maxResults)
                    .also {
                        Log.d(tag, "✓ Búsqueda filtrada: ${it.size} resultados oficiales para '$trimmedQuery'")
                    }
            } catch (e: Exception) {
                Log.e(tag, "✗ Error: ${e.message}", e)
                emptyList()
            }
        }

    private fun isOfficialAndValid(result: SearchResult, query: String): Boolean {
        val titleLower = result.title.lowercase()
        val channelLower = result.channelName.lowercase()

        // 1. REGLA ESTRICTA: Solo devolver si es de un artista oficial
        if (!result.isOfficialArtist) return false

        // 2. REGLA ESTRICTA: Descartar palabras clave no oficiales en título o canal
        if (unofficialKeywords.any { titleLower.contains(it) }) return false
        if (unofficialChannels.any { channelLower.contains(it) }) return false

        return true
    }

    private fun parseSearchRoot(root: JsonObject, query: String): List<SearchResult> {
        val contents = root["contents"]?.jsonObject ?: return emptyList()

        val sectionList = contents["twoColumnSearchResultsRenderer"]
            ?.jsonObject
            ?.get("primaryContents")
            ?.jsonObject
            ?.get("sectionListRenderer")
            ?.jsonObject
            ?: contents["sectionListRenderer"]?.jsonObject
            ?: return emptyList()

        val sections = sectionList["contents"]?.jsonArray ?: return emptyList()
        val results = mutableListOf<SearchResult>()

        for (section in sections) {
            val sectionObj = section.jsonObject

            sectionObj["itemSectionRenderer"]?.jsonObject?.let { itemSection ->
                val items = itemSection["contents"]?.jsonArray ?: return@let
                for (item in items) {
                    extractVideoRenderer(item, query)?.let { results.add(it) }
                }
            }

            sectionObj["shelfRenderer"]?.jsonObject?.let { shelf ->
                val shelfContent = shelf["content"]?.jsonObject
                val items = shelfContent?.get("verticalListRenderer")
                    ?.jsonObject?.get("items")?.jsonArray
                    ?: shelfContent?.get("horizontalListRenderer")
                        ?.jsonObject?.get("items")?.jsonArray
                items?.forEach { item ->
                    extractVideoRenderer(item, query)?.let { results.add(it) }
                }
            }

            extractVideoRenderer(section, query)?.let { results.add(it) }
        }

        return results
    }

    private fun extractVideoRenderer(node: JsonElement, query: String): SearchResult? {
        val nodeObj = node.jsonObject
        val video = nodeObj["videoRenderer"]?.jsonObject
            ?: nodeObj["musicVideoRenderer"]?.jsonObject
            ?: nodeObj.takeIf { it.containsKey("videoId") }
            ?: return null

        val videoId = video["videoId"]?.jsonPrimitive?.content ?: return null
        val title = extractText(video["title"]) ?: return null

        val channel = extractText(video["ownerText"])
            ?: extractText(video["longBylineText"])
            ?: extractText(video["shortBylineText"])
            ?: "Desconocido"

        val durationText = video["lengthText"]
            ?.jsonObject?.get("simpleText")?.jsonPrimitive?.content
            ?: "0:00"
        val durationSeconds = parseDuration(durationText)

        val thumbnail = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        val viewCount = video["viewCountText"]
            ?.let { extractText(it) }
            ?.let { parseViewCount(it) }

        val hasVerifiedBadge = hasVerifiedArtistBadge(video)
        val isOfficial = isOfficialChannel(channel, query, hasVerifiedBadge)

        return SearchResult(
            videoId = videoId,
            title = title,
            channelName = channel,
            durationSeconds = durationSeconds,
            thumbnailUrl = thumbnail,
            isOfficialArtist = isOfficial,
            viewCount = viewCount
        )
    }

    private fun extractText(node: JsonElement?): String? {
        if (node == null) return null
        val obj = node.jsonObject
        obj["simpleText"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }?.let { return it }
        obj["runs"]?.jsonArray
            ?.firstOrNull()?.jsonObject?.get("text")
            ?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }?.let { return it }
        return null
    }

    private fun parseDuration(text: String): Int {
        val parts = text.split(":").mapNotNull { it.trim().toIntOrNull() }
        return when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> 0
        }
    }

    private fun parseViewCount(text: String): Long? {
        val digits = text.filter { it.isDigit() }
        return digits.toLongOrNull()
    }

    private fun hasVerifiedArtistBadge(video: JsonObject): Boolean {
        val badgeArrays = listOfNotNull(
            video["ownerBadges"]?.jsonArray,
            video["badges"]?.jsonArray
        )

        for (array in badgeArrays) {
            for (item in array) {
                val badgeRenderer = item.jsonObject["metadataBadgeRenderer"]?.jsonObject ?: continue
                val style = badgeRenderer["style"]?.jsonPrimitive?.content ?: ""
                val iconType = badgeRenderer["icon"]?.jsonObject?.get("iconType")?.jsonPrimitive?.content ?: ""
                val tooltip = badgeRenderer["tooltip"]?.jsonPrimitive?.content ?: ""

                if (style.contains("VERIFIED_ARTIST", ignoreCase = true) ||
                    style.contains("OFFICIAL_ARTIST", ignoreCase = true) ||
                    style.contains("VERIFIED", ignoreCase = true) ||
                    iconType.contains("ARTIST", ignoreCase = true) ||
                    tooltip.contains("Official Artist", ignoreCase = true) ||
                    tooltip.contains("Artista oficial", ignoreCase = true) ||
                    tooltip.contains("Verificado", ignoreCase = true)
                ) {
                    return true
                }
            }
        }
        return false
    }

    private fun isOfficialChannel(channelName: String, query: String, hasVerifiedBadge: Boolean): Boolean {
        if (hasVerifiedBadge) return true

        val lower = channelName.lowercase().trim()
        val queryLower = query.lowercase().trim()

        // Canales de artista auto-generados por YouTube ("- Topic" / " - Topic")
        if (lower.endsWith("- topic") || lower.endsWith(" - topic")) return true

        // Canales VEVO oficiales (ej. "TaylorSwiftVEVO", "BadBunnyVEVO")
        if (lower.contains("vevo")) return true

        // Canales de discográficas y distribuidores oficiales reconocidos
        val officialLabels = listOf(
            "records", "recordings", "music group", "entertainment", "publishing",
            "warner music", "sony music", "universal music", "atlantic records",
            "columbia records", "epic records", "republic records", "interscope",
            "capitol records", "rca records", "def jam", "virgin music", "rimas entertainment"
        )
        if (officialLabels.any { lower.contains(it) }) return true

        // Coincidencia con nombre del artista oficial
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
