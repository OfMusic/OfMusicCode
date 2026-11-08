package com.haziel.ofmusic.ui.main

/**
 * Eventos one-shot que emite el ViewModel y la UI consume una sola vez.
 * A diferencia del uiState, no se repiten al rotar la pantalla ni
 * al recomponer.
 */
sealed class MusicUiEvent {
    data class ShowToast(val message: String) : MusicUiEvent()
    data class ShowUpgrade(val reason: com.haziel.ofmusic.domain.tier.UpgradeReason) : MusicUiEvent()
    data object DismissUpgrade : MusicUiEvent()
}