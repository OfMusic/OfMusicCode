package com.haziel.ofmusic.data.extractor

import android.util.Log
import com.metrolist.innertubex.utils.YouTubeUrlParser.extractVideoId
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

class CascadingStreamExtractor(
    private val newPipeExtractor: NewPipeStreamExtractor,
    private val fallbackExtractor: MediaStreamExtractor
) : MediaStreamExtractor {

    private val tag = "CascadingExtractor"

    override suspend fun getAudioUrl(videoUrl: String): String {
        val videoId = extractVideoId(videoUrl)
        try {
            return newPipeExtractor.getAudioUrl(videoId.toString())
        } catch (e: Exception) {
            Log.w(tag, "NewPipeExtractor falló, usando yt-dlp: ${e.message}")
        }
        return fallbackExtractor.getAudioUrl(videoUrl)
    }

    override suspend fun getVideoUrl(videoUrl: String): String {
        val videoId = extractVideoId(videoUrl)
        try {
            return newPipeExtractor.getVideoUrl(videoId.toString())
        } catch (e: Exception) {
            Log.w(tag, "NewPipeExtractor falló, usando yt-dlp: ${e.message}")
        }
        return fallbackExtractor.getVideoUrl(videoUrl)
    }
}