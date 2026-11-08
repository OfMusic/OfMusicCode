package com.haziel.ofmusic.data.extractor

import android.content.Context
import android.util.Log
import org.json.JSONObject

/**
 * Cache persistente de URLs extraídas de YouTube.
 * Las URLs de googlevideo.com expiran en ~6h. Guardamos con TTL de 5h
 * para asegurar que siempre sean válidas cuando se usan.
 */
class UrlCache(context: Context) {

    private val tag = "UrlCache"
    private val prefs = context.getSharedPreferences("ofmusic_url_cache", Context.MODE_PRIVATE)

    private val TTL_MS = 5 * 60 * 60 * 1000L  // 5 horas

    data class Entry(
        val url: String,
        val expiresAt: Long
    )

    fun get(key: String): String? {
        val raw = prefs.getString(key, null) ?: return null
        return try {
            val json = JSONObject(raw)
            val url = json.getString("url")
            val expiresAt = json.getLong("expiresAt")
            if (System.currentTimeMillis() > expiresAt) {
                prefs.edit().remove(key).apply()
                Log.d(tag, "URL expirada para: $key")
                null
            } else {
                Log.d(tag, "✓ URL en cache para: $key")
                url
            }
        } catch (e: Exception) {
            prefs.edit().remove(key).apply()
            null
        }
    }

    fun put(key: String, url: String) {
        try {
            val json = JSONObject().apply {
                put("url", url)
                put("expiresAt", System.currentTimeMillis() + TTL_MS)
            }
            prefs.edit().putString(key, json.toString()).apply()
            Log.d(tag, "URL guardada para: $key")
        } catch (e: Exception) {
            Log.e(tag, "Error guardando URL: ${e.message}")
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun pruneExpired() {
        val now = System.currentTimeMillis()
        val all = prefs.all
        val editor = prefs.edit()
        var removed = 0
        all.forEach { (key, value) ->
            try {
                val json = JSONObject(value as String)
                if (now > json.getLong("expiresAt")) {
                    editor.remove(key)
                    removed++
                }
            } catch (_: Exception) {
                editor.remove(key)
                removed++
            }
        }
        editor.apply()
        if (removed > 0) Log.d(tag, "Podadas $removed URLs expiradas")
    }
}