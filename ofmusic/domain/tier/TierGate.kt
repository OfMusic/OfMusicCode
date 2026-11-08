package com.haziel.ofmusic.domain.tier

sealed class TierGateResult {
    data object Allowed : TierGateResult()
    data class Blocked(val reason: UpgradeReason) : TierGateResult()
}

/**
 * Puerta única de validación. Toda acción restringida por tier pasa por aquí.
 */
object TierGate {

    fun canDownload(tier: UserTier, currentDownloads: Int): TierGateResult {
        val limit = TierLimits.forTier(tier).maxDownloads
        return if (TierLimits.isUnlimited(limit) || currentDownloads < limit) {
            TierGateResult.Allowed
        } else {
            TierGateResult.Blocked(UpgradeReason.DownloadsLimitReached(currentDownloads, limit))
        }
    }

    fun canCreatePlaylist(tier: UserTier, currentPlaylists: Int): TierGateResult {
        val limit = TierLimits.forTier(tier).maxPlaylists
        return if (TierLimits.isUnlimited(limit) || currentPlaylists < limit) {
            TierGateResult.Allowed
        } else {
            TierGateResult.Blocked(UpgradeReason.PlaylistsLimitReached(currentPlaylists, limit))
        }
    }

    fun canAddSongToPlaylist(tier: UserTier, currentSongsInPlaylist: Int): TierGateResult {
        val limit = TierLimits.forTier(tier).maxSongsPerPlaylist
        return if (TierLimits.isUnlimited(limit) || currentSongsInPlaylist < limit) {
            TierGateResult.Allowed
        } else {
            TierGateResult.Blocked(
                UpgradeReason.SongsPerPlaylistLimitReached(currentSongsInPlaylist, limit)
            )
        }
    }

    fun canUseOfflineStreaming(tier: UserTier): TierGateResult =
        if (TierLimits.forTier(tier).offlineStreaming) TierGateResult.Allowed
        else TierGateResult.Blocked(UpgradeReason.OfflineStreamingLocked)

    fun canUseSyncedVideo(tier: UserTier): TierGateResult =
        if (TierLimits.forTier(tier).syncedVideo) TierGateResult.Allowed
        else TierGateResult.Blocked(UpgradeReason.SyncedVideoLocked)

    fun canUseSyncedLyrics(tier: UserTier): TierGateResult =
        if (TierLimits.forTier(tier).syncedLyrics) TierGateResult.Allowed
        else TierGateResult.Blocked(UpgradeReason.SyncedLyricsLocked)

    fun canUseQuality(tier: UserTier, requestedKbps: Int): TierGateResult =
        if (requestedKbps <= TierLimits.forTier(tier).audioQualityKbps) TierGateResult.Allowed
        else TierGateResult.Blocked(UpgradeReason.ProQualityLocked)
}