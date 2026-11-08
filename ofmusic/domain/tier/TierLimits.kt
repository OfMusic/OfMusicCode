package com.haziel.ofmusic.domain.tier

/**
 * Límites concretos de un tier.
 * -1 o UNLIMITED significa sin restricción.
 */
data class TierLimit(
    val maxDownloads: Int,
    val maxPlaylists: Int,
    val maxSongsPerPlaylist: Int,
    val audioQualityKbps: Int,
    val offlineStreaming: Boolean,
    val syncedVideo: Boolean,
    val cloudSync: Boolean,
    val syncedLyrics: Boolean
)

object TierLimits {

    const val UNLIMITED = Int.MAX_VALUE

    val FREE = TierLimit(
        maxDownloads = 25,
        maxPlaylists = 5,
        maxSongsPerPlaylist = 50,
        audioQualityKbps = 256,
        offlineStreaming = false,
        syncedVideo = false,
        cloudSync = true,
        syncedLyrics = false
    )

    val STANDARD = TierLimit(
        maxDownloads = 150,
        maxPlaylists = 20,
        maxSongsPerPlaylist = 100,
        audioQualityKbps = 256,
        offlineStreaming = true,
        syncedVideo = true,
        cloudSync = true,
        syncedLyrics = true
    )

    val PRO = TierLimit(
        maxDownloads = UNLIMITED,
        maxPlaylists = UNLIMITED,
        maxSongsPerPlaylist = UNLIMITED,
        audioQualityKbps = 320,
        offlineStreaming = true,
        syncedVideo = true,
        cloudSync = true,
        syncedLyrics = true
    )

    fun forTier(tier: UserTier): TierLimit = when (tier) {
        UserTier.FREE -> FREE
        UserTier.STANDARD -> STANDARD
        UserTier.PRO -> PRO
    }

    fun isUnlimited(value: Int): Boolean = value == UNLIMITED
}