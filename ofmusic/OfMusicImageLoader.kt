package com.haziel.ofmusic

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object OfMusicImageLoader {
    fun create(context: Context): ImageLoader {
        val okHttp = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(context)
            .okHttpClient(okHttp)
            .crossfade(true)
            .crossfade(250)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100 MB
                    .build()
            }
            .build()
    }
}