package com.haziel.ofmusic.domain.model

import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class Playlist(
    val id: Long = System.currentTimeMillis() + (0..9999).random().toLong(),
    val title: String,
    val description: String = "",
    val artistName: String = "Mi Playlist",
    val coverUrl: String = "",
    val isPublic: Boolean = true,
    val downloadOffline: Boolean = false,
    val songs: List<Song> = emptyList(),
    val isUserCreated: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): String = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("description", description)
        put("artistName", artistName)
        put("coverUrl", coverUrl)
        put("isPublic", isPublic)
        put("downloadOffline", downloadOffline)
        put("isUserCreated", isUserCreated)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        put("songs", JSONArray().apply {
            songs.forEach { song -> put(song.toJson()) }
        })
    }.toString()

    companion object {
        fun fromJson(json: String): Playlist {
            val o = JSONObject(json)
            val songsArray = o.optJSONArray("songs") ?: JSONArray()

            val songs = (0 until songsArray.length()).mapNotNull { i ->
                runCatching {
                    Song.fromJson(songsArray.getString(i))
                }.getOrNull()
            }

            return Playlist(
                id = o.optLong("id", System.currentTimeMillis()),
                title = o.optString("title", "Sin nombre"),
                description = o.optString("description", ""),
                artistName = o.optString("artistName", "Mi Playlist"),
                coverUrl = o.optString("coverUrl", ""),
                isPublic = o.optBoolean("isPublic", true),
                downloadOffline = o.optBoolean("downloadOffline", false),
                songs = songs,
                isUserCreated = o.optBoolean("isUserCreated", true),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
            )
        }
    }
}