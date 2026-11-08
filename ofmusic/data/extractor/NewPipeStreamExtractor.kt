package com.haziel.ofmusic.data.extractor

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request as OkRequest
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.IOException
import java.util.concurrent.TimeUnit

class OkHttpDownloader : Downloader() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val body = request.dataToSend()?.toRequestBody(null)

        val builder = OkRequest.Builder()
            .method(httpMethod, body)
            .url(url)

        headers.forEach { (key, values) ->
            values.forEach { value -> builder.addHeader(key, value) }
        }

        val response = client.newCall(builder.build()).execute()
        val responseBody = response.body?.string() ?: ""

        if (responseBody.contains("g-recaptcha")) {
            throw ReCaptchaException("ReCaptcha requerido", url)
        }

        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            responseBody,
            response.request.url.toString()
        )
    }
}

class NewPipeStreamExtractor(context: Context) {

    private val tag = "NewPipeExtractor"
    private val cache: UrlCache = UrlCache(context)

    // Cache en memoria de StreamInfo para evitar re-parseo
    private val streamInfoCache = object : LinkedHashMap<String, StreamInfo>(5, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, StreamInfo>?) = size > 5
    }

    init {
        try {
            NewPipe.init(OkHttpDownloader())
            Log.d(tag, "✓ NewPipeExtractor inicializado")
        } catch (e: Exception) {
            Log.e(tag, "✗ Error inicializando NewPipeExtractor: ${e.message}", e)
        }
    }

    /**
     * Warm-up: llamar al arrancar la app para que la primera canción real sea rápida.
     */
    suspend fun warmUp() = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "⏳ Warm-up NewPipe...")
            StreamInfo.getInfo(ServiceList.YouTube, "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
            Log.d(tag, "✓ Warm-up completado")
        } catch (e: Exception) {
            Log.w(tag, "Warm-up falló: ${e.message}")
        }
    }

    suspend fun getAudioUrl(videoId: String): String = withContext(Dispatchers.IO) {
        // 1. Cache persistente primero
        cache.get("np_audio_$videoId")?.let {
            Log.d(tag, "▶ NewPipe audio desde cache: $videoId")
            return@withContext it
        }

        val info = getStreamInfo(videoId)

        val bestAudio = info.audioStreams.maxByOrNull { it.bitrate }
            ?: throw MediaExtractionException("NewPipeExtractor: sin audio streams")

        val url = bestAudio.content
        cache.put("np_audio_$videoId", url)
        Log.d(tag, "✓ NewPipe audio OK: ${url.take(50)}...")
        url
    }

    suspend fun getVideoUrl(videoId: String): String = withContext(Dispatchers.IO) {
        cache.get("np_video_$videoId")?.let {
            Log.d(tag, "▶ NewPipe video desde cache: $videoId")
            return@withContext it
        }

        val info = getStreamInfo(videoId)

        val bestVideo = info.videoOnlyStreams.maxByOrNull { it.resolution }
            ?: throw MediaExtractionException("NewPipeExtractor: sin video-only streams")

        val url = bestVideo.content
        cache.put("np_video_$videoId", url)
        Log.d(tag, "✓ NewPipe video OK: ${url.take(50)}...")
        url
    }

    /**
     * Extrae audio + video en una sola llamada a StreamInfo (evita duplicar requests).
     */
    suspend fun getBothUrls(videoId: String): Pair<String, String> = withContext(Dispatchers.IO) {
        val audioCached = cache.get("np_audio_$videoId")
        val videoCached = cache.get("np_video_$videoId")

        if (audioCached != null && videoCached != null) {
            return@withContext audioCached to videoCached
        }

        val info = getStreamInfo(videoId)
        val audio = info.audioStreams.maxByOrNull { it.bitrate }?.content
            ?: throw MediaExtractionException("Sin audio")
        val video = info.videoOnlyStreams.maxByOrNull { it.resolution }?.content ?: ""

        cache.put("np_audio_$videoId", audio)
        if (video.isNotBlank()) cache.put("np_video_$videoId", video)

        audio to video
    }

    private suspend fun getStreamInfo(videoId: String): StreamInfo {
        streamInfoCache[videoId]?.let {
            Log.d(tag, "▶ StreamInfo desde memoria: $videoId")
            return it
        }

        val url = "https://www.youtube.com/watch?v=$videoId"
        Log.d(tag, "⏳ StreamInfo.getInfo: $videoId")

        val start = System.currentTimeMillis()
        try {
            val info = StreamInfo.getInfo(ServiceList.YouTube, url)
            val elapsed = System.currentTimeMillis() - start
            Log.d(tag, "✓ StreamInfo OK en ${elapsed}ms")
            streamInfoCache[videoId] = info
            return info
        } catch (e: ExtractionException) {
            Log.e(tag, "✗ StreamInfo falló: ${e.message}", e)
            throw MediaExtractionException("NewPipeExtractor: ${e.message}")
        }
    }
}