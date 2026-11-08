package com.haziel.ofmusic.data.download

import android.content.Context
import android.util.Log
import com.haziel.ofmusic.data.extractor.MediaExtractionException
import com.haziel.ofmusic.data.extractor.YtDlpStreamExtractor
import com.haziel.ofmusic.domain.model.Song
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

class AudioDownloadManager(private val context: Context) {

    private val tag = "AudioDownloadManager"
    private val extractor = YtDlpStreamExtractor()
    private val httpClient = OkHttpClient.Builder().build()

    // ── Cache thread-safe de URLs de stream (evita re-extracción) ──
    private val urlCache = ConcurrentHashMap<String, String>()

    // ── Directorio de música (con fallback seguro) ──
    fun getMusicDir(): File {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val dir = File(baseDir, "music")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun isSongDownloaded(song: Song): Boolean {
        return getDownloadedFilePath(song) != null
    }

    fun getDownloadedFilePath(song: Song): String? {
        // 1. Ruta guardada en el modelo
        song.localPath?.takeIf { it.isNotBlank() }?.let { path ->
            val file = File(path)
            if (file.exists() && file.length() > 1024) return file.absolutePath
        }

        // 2. Convención de nombre, probando varias extensiones
        val baseName = "${sanitizeFileName(song.title)}_${song.id}"
        for (ext in listOf("m4a", "mp3", "webm", "opus", "aac")) {
            val candidate = File(getMusicDir(), "$baseName.$ext")
            if (candidate.exists() && candidate.length() > 1024) {
                return candidate.absolutePath
            }
        }
        return null
    }

    suspend fun downloadAudio(
        song: Song,
        onProgress: ((Int) -> Unit)? = null
    ): String? = withContext(Dispatchers.IO) {

        // Ya descargada → devolver directamente
        getDownloadedFilePath(song)?.let {
            Log.d(tag, "Ya descargada en $it")
            onProgress?.invoke(100)
            return@withContext it
        }

        val youtubeId = song.youtubeId
        if (youtubeId.isNullOrBlank()) {
            Log.e(tag, "No hay youtubeId para '${song.title}'")
            return@withContext null
        }

        // ── Descarga principal vía yt-dlp (extrae + convierte a m4a con FFmpeg) ──
        downloadWithYtDlp(youtubeId, song, onProgress)?.let {
            return@withContext it
        }

        // ── Fallback: descargar stream directo vía OkHttp ──
        Log.w(tag, "yt-dlp falló, intentando fallback con OkHttp")
        return@withContext downloadWithOkHttp(song, onProgress)
    }

    /**
     * Descarga principal. yt-dlp extrae el audio y FFmpeg lo convierte a m4a.
     * Requiere FFmpeg inicializado en la Application.
     */
    private fun downloadWithYtDlp(
        youtubeId: String,
        song: Song,
        onProgress: ((Int) -> Unit)?
    ): String? {
        val baseName = "${sanitizeFileName(song.title)}_${song.id}"
        val outputTemplate = File(getMusicDir(), "$baseName.%(ext)s").absolutePath

        return try {
            Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(tag, "▶ Iniciando yt-dlp")
            Log.d(tag, "  videoId: $youtubeId")
            Log.d(tag, "  output: $outputTemplate")

            val request = YoutubeDLRequest("https://www.youtube.com/watch?v=$youtubeId").apply {
                addOption("-f", "bestaudio/best")
                addOption("-o", outputTemplate)
                addOption("--no-playlist")
                addOption("--extract-audio")
                addOption("--audio-format", "m4a")
                addOption("--audio-quality", "0")
                addOption("--no-warnings")
                addOption("--socket-timeout", "15")
            }

            val response = YoutubeDL.getInstance().execute(request) { progress, _, line ->
                val pct = progress.toInt().coerceIn(0, 99)
                onProgress?.invoke(pct)
                Log.v(tag, "  Progreso: $pct%")
            }

            // Log detallado para diagnóstico
            Log.d(tag, "  exitCode: ${response.exitCode}")
            Log.d(tag, "  out: ${response.out.take(300)}")
            if (response.err.isNotBlank()) {
                Log.w(tag, "  err: ${response.err.take(300)}")
            }

            // Diagnóstico explícito si falla
            if (response.exitCode != 0) {
                val err = response.err.lowercase()
                when {
                    err.contains("ffmpeg") -> Log.e(tag, "✗✗✗ FFmpeg NO inicializado. Revisa OfMusicApplication.")
                    err.contains("signature") || err.contains("403") ->
                        Log.e(tag, "✗✗✗ YouTube bloqueó la descarga (signature/403)")
                    err.contains("network") || err.contains("timeout") ->
                        Log.e(tag, "✗✗✗ Error de red")
                    else -> Log.e(tag, "✗✗✗ Error yt-dlp: ${response.err.take(200)}")
                }
            }

            // Verificar si el archivo se creó
            val result = findDownloadedFile(song)
            if (result != null) {
                Log.d(tag, "✓ yt-dlp completado: $result")
                onProgress?.invoke(100)
                result
            } else {
                Log.e(tag, "✗ yt-dlp terminó pero no encontró archivo")
                Log.e(tag, "  Patrón buscado: $baseName.[m4a|mp3|webm|opus|aac]")
                Log.e(tag, "  Archivos en music/:")
                getMusicDir().listFiles()?.forEach { f ->
                    Log.e(tag, "    - ${f.name} (${f.length()} bytes)")
                }
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "✗ Excepción en yt-dlp: ${e.message}", e)
            null
        }
    }

    /**
     * Fallback: descarga directa del stream de audio vía OkHttp.
     * No convierte el formato; guarda con la extensión correcta según el mime.
     */
    private suspend fun downloadWithOkHttp(
        song: Song,
        onProgress: ((Int) -> Unit)?
    ): String? {
        val streamUrl = try {
            when {
                // 1. URL ya extraída en el Song
                !song.audioUrl.isNullOrBlank() && song.audioUrl.contains("googlevideo.com") ->
                    song.audioUrl

                // 2. Extraer URL (con cache)
                !song.youtubeId.isNullOrBlank() -> {
                    val cached = urlCache[song.youtubeId]
                    if (cached != null) {
                        Log.d(tag, "✓ Cache hit de URL para ${song.youtubeId}")
                        cached
                    } else {
                        val extracted = extractor.getAudioUrl(
                            "https://www.youtube.com/watch?v=${song.youtubeId}"
                        )
                        urlCache[song.youtubeId] = extracted
                        extracted
                    }
                }

                else -> null
            }
        } catch (e: MediaExtractionException) {
            Log.e(tag, "Error extrayendo URL: ${e.message}")
            null
        } ?: return null

        if (!streamUrl.startsWith("http")) return null

        // Detectar extensión según el mime type del stream
        val extension = when {
            streamUrl.contains("mime=audio%2Fmp4") || streamUrl.contains("mime=audio/mp4") -> "m4a"
            streamUrl.contains("mime=audio%2Fwebm") || streamUrl.contains("mime=audio/webm") -> "webm"
            else -> "m4a"
        }

        val targetFile = File(
            getMusicDir(),
            "${sanitizeFileName(song.title)}_${song.id}.$extension"
        )

        return try {
            Log.d(tag, "▶ Iniciando descarga OkHttp")
            Log.d(tag, "  url: ${streamUrl.take(100)}...")
            Log.d(tag, "  target: ${targetFile.absolutePath}")

            val okHttpRequest = Request.Builder()
                .url(streamUrl)
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()

            httpClient.newCall(okHttpRequest).execute().use { response ->
                val body = response.body ?: run {
                    Log.e(tag, "Response body null")
                    return null
                }
                if (!response.isSuccessful) {
                    Log.e(tag, "HTTP ${response.code} al descargar")
                    return null
                }

                val contentLength = body.contentLength()
                body.byteStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (contentLength > 0) {
                                val progress = ((totalRead * 100) / contentLength).toInt()
                                onProgress?.invoke(progress.coerceIn(0, 99))
                            }
                        }
                    }
                }
            }

            if (targetFile.exists() && targetFile.length() > 1024) {
                Log.d(tag, "✓ Descarga OkHttp completada: ${targetFile.absolutePath}")
                onProgress?.invoke(100)
                targetFile.absolutePath
            } else {
                Log.e(tag, "Archivo descargado inválido o vacío")
                targetFile.delete()
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "✗ Error en descarga OkHttp: ${e.localizedMessage}", e)
            targetFile.delete()
            null
        }
    }

    private fun findDownloadedFile(song: Song): String? {
        val baseName = "${sanitizeFileName(song.title)}_${song.id}"
        for (ext in listOf("m4a", "mp3", "webm", "opus", "aac")) {
            val f = File(getMusicDir(), "$baseName.$ext")
            if (f.exists() && f.length() > 1024) return f.absolutePath
        }
        return null
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace("[^a-zA-Z0-9._-]".toRegex(), "_").take(60)
    }
    /**
     * Elimina el archivo descargado de una canción (si existe).
     * Retorna true si se eliminó algo, false si no había nada.
     */
    fun deleteDownload(song: Song): Boolean {
        val path = getDownloadedFilePath(song) ?: return false
        val file = File(path)
        return try {
            val deleted = file.delete()
            if (deleted) {
                Log.d(tag, "✓ Archivo eliminado: $path")
            } else {
                Log.w(tag, "No se pudo eliminar: $path")
            }
            deleted
        } catch (e: Exception) {
            Log.e(tag, "Error eliminando archivo: ${e.message}", e)
            false
        }
    }
}