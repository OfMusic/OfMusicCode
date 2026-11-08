package com.haziel.ofmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haziel.ofmusic.ui.theme.OfMusicTheme
import com.haziel.ofmusic.ui.main.MainScreen
import com.haziel.ofmusic.ui.main.MusicViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MusicViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            OfMusicTheme(selectedTheme = state.selectedTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}