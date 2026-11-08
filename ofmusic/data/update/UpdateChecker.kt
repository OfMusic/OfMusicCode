package com.haziel.ofmusic.data.update

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class UpdateChecker(
    private val context: Context,
    private val versionJsonUrl: String = DEFAULT_URL
) {
    private val tag = "UpdateChecker"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Devuelve la info de la última versión si hay una más nueva que la instalada.
     * Null si está actualizada o si falla la consulta.
     */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(versionJsonUrl)
                .header("Cache-Control", "no-cache")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(tag, "HTTP ${response.code} al consultar version.json")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val info = UpdateInfo.fromJson(body) ?: return@withContext null

            val currentVersionCode = getCurrentVersionCode()
            Log.d(tag, "Versión instalada: $currentVersionCode, versión remota: ${info.versionCode}")

            if (info.versionCode > currentVersionCode) {
                Log.d(tag, "✓ Actualización disponible: ${info.versionName}")
                info
            } else {
                Log.d(tag, "App actualizada")
                null
            }
        } catch (e: Exception) {
            Log.w(tag, "Error consultando actualización: ${e.message}")
            null
        }
    }

    private fun getCurrentVersionCode(): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) {
            0
        }
    }

    companion object {
        // ⚠️ CAMBIA ESTA URL POR LA TUYA
        const val DEFAULT_URL = "https://github.com/OfMusic/ofmusic-releases/releases/latest/download/version.json"
    }
}