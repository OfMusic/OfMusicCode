package com.haziel.ofmusic.data.update

import org.json.JSONObject

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val changelog: String,
    val mandatory: Boolean
) {
    companion object {
        fun fromJson(json: String): UpdateInfo? {
            return try {
                val o = JSONObject(json)
                val rawUrl = o.optString("apkUrl", "")
                val verName = o.optString("versionName", "")

                val formattedUrl = when {
                    rawUrl.startsWith("http://") || rawUrl.startsWith("https://") -> rawUrl
                    rawUrl.isNotBlank() -> "https://github.com/OfMusic/ofmusic-releases/releases/latest/download/$rawUrl"
                    verName.isNotBlank() -> "https://github.com/OfMusic/ofmusic-releases/releases/latest/download/OfMusic.v${verName}.apk"
                    else -> "https://github.com/OfMusic/ofmusic-releases/releases/latest/download/app-release.apk"
                }

                UpdateInfo(
                    versionCode = o.getInt("versionCode"),
                    versionName = verName,
                    apkUrl = formattedUrl,
                    changelog = o.optString("changelog", ""),
                    mandatory = o.optBoolean("mandatory", false)
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}