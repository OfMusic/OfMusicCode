package com.haziel.ofmusic.ui.main

import com.haziel.ofmusic.domain.model.Playlist
import com.haziel.ofmusic.domain.model.Song
import com.haziel.ofmusic.domain.model.UserProfile
import com.haziel.ofmusic.domain.tier.UpgradeReason

data class MusicUiState(
    val songs: List<Song> = emptyList(),
    val volume: Float = 1f,
    val playlists: List<Playlist> = emptyList(),
    val likedSongIds: Set<Long> = emptySet(),
    val downloadedSongMap: Map<Long, String> = emptyMap(),
    val downloadingSongIds: Set<Long> = emptySet(),   // ← NUEVO
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = 0,
    val videoUrlMap: Map<Long, String> = emptyMap(),
    val searchQuery: String = "",
    val searchResults: List<Song> = emptyList(),
    val isSearching: Boolean = false,
    val isPreparingQueue: Boolean = false,
    val isLoading: Boolean = false,
    val userProfile: UserProfile = UserProfile(
        name = "user",
        email = "",
        isAdmin = false
    ),
    val downloadQuality: String = "320 kbps (HQ Alta Calidad)",
    val wifiOnlyDownload: Boolean = true,
    val autoDownloadFavorites: Boolean = false,
    val isDarkTheme: Boolean = true,
    val isSpatialAudioEnabled: Boolean = false,
    val currentLyrics: List<com.haziel.ofmusic.data.lyrics.LyricLine> = emptyList(),
    val isLoadingLyrics: Boolean = false,
    val showLyrics: Boolean = true,
    // ── Actualización in-app ──
    val updateInfo: com.haziel.ofmusic.data.update.UpdateInfo? = null,
    val isDownloadingUpdate: Boolean = false,
    val updateDownloadProgress: Int = 0,
    val updateErrorMessage: String? = null,
    val apkReadyToInstall: java.io.File? = null,
    // ── Frecuencia de pantalla & Temas ──
    val refreshRate: Int = 0,
    val selectedTheme: com.haziel.ofmusic.data.preferences.AppThemeOption = com.haziel.ofmusic.data.preferences.AppThemeOption.VIOLET_DEFAULT,
    // ── Autenticación (Fase 6.3) ──
    val authProfile: com.haziel.ofmusic.data.supabase.ProfileDto? = null,
    val isAuthLoading: Boolean = false,
    val authError: String? = null,
    val isLoggedIn: Boolean = false,
    val pendingUpgrade: UpgradeReason? = null,
    // 👇 NUEVOS CAMPOS PARA ALMACENAMIENTO
    val downloadedStorageBytes: Long = 0L, // Tamaño real de la música
    val cacheStorageBytes: Long = 0L,      // Tamaño del caché
    val storageLimitGb: Int = 20,          // Límite inteligente (20 GB por defecto)
    val playlistDownloadProgress: Pair<Long, Int>? = null // (playlistId to progressPercent)
)