package io.github.canvas.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.canvas.data.settings.Settings
import io.github.canvas.data.settings.Settings.MonochromeIconColors.PRIMARY
import io.github.canvas.ui.CanvasLauncherTheme
import kotlinx.coroutines.flow.map

class SettingsActivity : ComponentActivity() {
    private val viewModel: SettingsViewModel by viewModels { SettingsViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val theme by viewModel.settings.map { it?.theme }
                .collectAsStateWithLifecycle(initialValue = Settings.defaultTheme)
            val monochromeIconsEnabled by viewModel.settings.map { it?.monochromeIconsEnabled }
                .collectAsStateWithLifecycle(initialValue = null)
            val monochromeIconColors by viewModel.settings.map { it?.monochromeIconColors }
                .collectAsStateWithLifecycle(initialValue = null)

            CanvasLauncherTheme(
                theme = theme ?: Settings.defaultTheme,
                monochromeIconsEnabled = monochromeIconsEnabled == true,
                monochromeIconColors = monochromeIconColors ?: PRIMARY,
            ) {
                Settings(viewModel)
            }
        }
        log.d("Canvas Settings launched")
    }
}
