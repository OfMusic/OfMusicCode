package com.haziel.ofmusic.domain.model
import androidx.compose.runtime.Immutable

@Immutable
data class SearchResult(
    val videoId: String,
    val title: String,
    val channelName: String,
    val durationSeconds: Int,
    val thumbnailUrl: String,
    val isOfficialArtist: Boolean,
    val viewCount: Long?
)
