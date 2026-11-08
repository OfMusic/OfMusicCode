package com.haziel.ofmusic.data.extractor

interface MediaStreamExtractor {
    suspend fun getAudioUrl(videoUrl: String): String
    suspend fun getVideoUrl(videoUrl: String): String
}
