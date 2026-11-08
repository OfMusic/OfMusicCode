package com.haziel.ofmusic.data.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FavoriteDto(
    val id: Long? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("song_id") val songId: Long,
    @SerialName("youtube_id") val youtubeId: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)