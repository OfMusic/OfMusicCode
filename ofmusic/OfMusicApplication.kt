package com.haziel.ofmusic

import android.app.Application
import android.content.Context
import android.util.Log
import coil.Coil
import com.haziel.ofmusic.data.extractor.UrlCache
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.haziel.ofmusic.data.supabase.SupabaseClientProvider

class OfMusicApplication : Application() {

    private val tag = "OfMusicApp"

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Se completa cuando YoutubeDL y FFmpeg están inicializados.
     * Las clases que dependan de yt-dlp deben esperar a este Deferred.
     */
    private val ytdlpReady = CompletableDeferred<Boolean>()

    override fun onCreate() {
        super.onCreate()
        // ─── 1. Coil ───
        try {
            Coil.setImageLoader(OfMusicImageLoader.create(this))
        } catch (e: Exception) {
            Log.e(tag, "Error configurando Coil: ${e.message}", e)
        }

        // ─── 2. Supabase (NUEVO) ───
        try {
            SupabaseClientProvider.getInstance()
            Log.d(tag, "✓ Supabase inicializado")
        } catch (e: Exception) {
            Log.e(tag, "✗ Error inicializando Supabase: ${e.message}", e)
        }

        // ─── 3. yt-dlp en BACKGROUND ───
        appScope.launch {
            initYoutubeDl()
            // Warm-up de NewPipe para que la primera canción sea rápida
            try {
                Log.d(tag, "⏳ Warm-up NewPipe...")
                com.haziel.ofmusic.data.extractor.NewPipeStreamExtractor(this@OfMusicApplication).warmUp()
            } catch (e: Exception) {
                Log.w(tag, "Warm-up NewPipe falló: ${e.message}")
            }
        }
    }


    private suspend fun initYoutubeDl() {
        try {
            // ─── Inicializar YoutubeDL (bloquea pero en IO, no en main) ───
            Log.d(tag, "⏳ Inicializando YoutubeDL en background...")
            val start1 = System.currentTimeMillis()
            YoutubeDL.getInstance().init(this@OfMusicApplication)
            val elapsed1 = System.currentTimeMillis() - start1
            Log.d(tag, "✓ YoutubeDL inicializado (${elapsed1}ms)")

            // ─── Inicializar FFmpeg ───
            try {
                Log.d(tag, "⏳ Inicializando FFmpeg...")
                val start2 = System.currentTimeMillis()
                FFmpeg.getInstance().init(this@OfMusicApplication)
                val elapsed2 = System.currentTimeMillis() - start2
                Log.d(tag, "✓ FFmpeg inicializado (${elapsed2}ms)")
            } catch (e: Throwable) {
                Log.e(tag, "✗ FFmpeg falló: ${e.message}", e)
            }

            // Marcar como listo
            ytdlpReady.complete(true)

            // ─── Podar cache de URLs expiradas ───
            try {
                UrlCache(applicationContext).pruneExpired()
            } catch (e: Exception) {
                Log.w(tag, "Error podando UrlCache: ${e.message}")
            }

            // ─── Actualizar yt-dlp ───
            try {
                Log.d(tag, "⏳ Verificando actualizaciones de yt-dlp...")
                YoutubeDL.getInstance().updateYoutubeDL(
                    applicationContext,
                    YoutubeDL.UpdateChannel._STABLE
                )
                Log.d(tag, "✓ yt-dlp actualizado")
            } catch (e: Exception) {
                Log.w(tag, "No se pudo actualizar yt-dlp: ${e.message}")
            }

            // NOTA: quitamos el warm-up porque fallaba siempre con dQw4w9WgXcQ.
            // El primer yt-dlp real lo hará el usuario al buscar/reproducir.

        } catch (e: Throwable) {
            Log.e(tag, "✗ Error crítico inicializando YoutubeDL: ${e.message}", e)
            ytdlpReady.complete(false)
        }

        try {
            Log.d(tag, "⏳ Warm-up NewPipeExtractor...")
            val newPipe = com.haziel.ofmusic.data.extractor.NewPipeStreamExtractor(this@OfMusicApplication)
            newPipe.warmUp()
        } catch (e: Exception) {
            Log.w(tag, "Warm-up NewPipe falló: ${e.message}")
        }
    }

    /**
     * Suspende hasta que yt-dlp esté inicializado.
     * Las clases que usen YoutubeDL deben llamar a esto primero.
     */
    suspend fun awaitYtDlpReady(): Boolean {
        return ytdlpReady.await()
    }
}