package com.haziel.ofmusic.domain.search

import com.haziel.ofmusic.domain.model.SearchResult

interface YouTubeSearcher {
    suspend fun search(query: String, maxResults: Int = 30): List<SearchResult>
}
