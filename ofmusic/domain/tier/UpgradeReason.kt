package com.haziel.ofmusic.domain.tier

/**
 * Razón por la que se bloquea una acción. Se usa para mostrar el diálogo de upgrade.
 */
sealed class UpgradeReason {
    abstract val title: String
    abstract val message: String
    abstract val requiredTier: UserTier

    data class DownloadsLimitReached(val current: Int, val max: Int) : UpgradeReason() {
        override val title = "Límite de descargas alcanzado"
        override val message =
            "Has usado $current de $max descargas de tu plan Free. Actualiza para descargar más."
        override val requiredTier = UserTier.STANDARD
    }

    data class PlaylistsLimitReached(val current: Int, val max: Int) : UpgradeReason() {
        override val title = "Límite de playlists alcanzado"
        override val message =
            "Has creado $current de $max playlists permitidas en tu plan Free."
        override val requiredTier = UserTier.STANDARD
    }

    data class SongsPerPlaylistLimitReached(val current: Int, val max: Int) : UpgradeReason() {
        override val title = "Playlist llena"
        override val message =
            "Esta playlist alcanzó el máximo de $max canciones en tu plan Free."
        override val requiredTier = UserTier.STANDARD
    }

    data object OfflineStreamingLocked : UpgradeReason() {
        override val title = "Reproducción offline"
        override val message =
            "Reproducir sin conexión está disponible en Standard y Pro."
        override val requiredTier = UserTier.STANDARD
    }

    data object SyncedVideoLocked : UpgradeReason() {
        override val title = "Video sincronizado"
        override val message =
            "El video sincronizado consume mucho ancho de banda. Disponible en Standard y Pro."
        override val requiredTier = UserTier.STANDARD
    }

    data object SyncedLyricsLocked : UpgradeReason() {
        override val title = "Letras sincronizadas"
        override val message =
            "Las letras sincronizadas están disponibles en Standard y Pro."
        override val requiredTier = UserTier.STANDARD
    }

    data object ProQualityLocked : UpgradeReason() {
        override val title = "Calidad 320 kbps"
        override val message =
            "La calidad máxima de audio es exclusiva del plan Pro."
        override val requiredTier = UserTier.PRO
    }
}