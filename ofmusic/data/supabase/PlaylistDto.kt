package com.haziel.ofmusic.data.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDto(
    val id: Long,
    @SerialName("user_id") val userId: String,
    val title: String,
    @SerialName("description") val description: String = "",
    @SerialName("artist_name") val artistName: String = "Mi Playlist",
    @SerialName("cover_url") val coverUrl: String = "",
    @SerialName("is_public") val isPublic: Boolean = true,
    @SerialName("download_offline") val downloadOffline: Boolean = false,
    @SerialName("songs_json") val songsJson: String = "[]",
    @SerialName("is_user_created") val isUserCreated: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)