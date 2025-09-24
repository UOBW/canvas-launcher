package io.github.canvas.ui.launcher.dialogs

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES.O
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.canvas.data.CanvasLauncherApplication
import io.github.canvas.data.settings.Settings
import io.github.canvas.data.settings.Settings.MonochromeIconColors.PRIMARY
import io.github.canvas.data.shortcuts.Shortcut
import io.github.canvas.data.shortcuts.ShortcutsRepository
import io.github.canvas.ui.CanvasLauncherTheme
import io.github.canvas.ui.SearchResultIcon
import io.github.canvas.ui.launcher.R
import io.github.canvas.ui.launcher.log
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PinShortcutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (VERSION.SDK_INT < O) error("Reached code thought to be unreachable on this API version")
        log.d("PinShortcutActivity started")

        // Just access the repository directly, it is not worth it to create a view model
        val shortcutsRepository = (application as CanvasLauncherApplication).shortcutsRepository
        val settingsRepository = (application as CanvasLauncherApplication).settingsRepository

        enableEdgeToEdge()
        setContent {
            val coroutineScope = rememberCoroutineScope()

            val theme by settingsRepository.settings.map { it.theme }
                .collectAsStateWithLifecycle(initialValue = Settings.defaultTheme)
            val monochromeIconsEnabled by settingsRepository.settings.map { it.monochromeIconsEnabled }
                .collectAsStateWithLifecycle(initialValue = false)
            val monochromeIconColors by settingsRepository.settings.map { it.monochromeIconColors }
                .collectAsStateWithLifecycle(initialValue = PRIMARY)

            val pinRequest: ShortcutsRepository.PinRequest? by produceState(initialValue = null) {
                shortcutsRepository.processShortcutPinRequest(intent, this@PinShortcutActivity)
                    ?.let { value = it }
                    ?: finish()
            }

            if (pinRequest == null) return@setContent // Loading should be so quick that it isn't noticeable

            val possibleDuplicate by produceState(initialValue = false) {
                if (pinRequest != null)
                    value = shortcutsRepository.similarPinnedShortcutExists(pinRequest!!.shortcut)
            }

            CanvasLauncherTheme(
                theme = theme,
                monochromeIconsEnabled = monochromeIconsEnabled,
                monochromeIconColors = monochromeIconColors,
            ) {
                PinShortcutConfirmationDialog(
                    shortcut = pinRequest!!.shortcut,
                    possibleDuplicate = possibleDuplicate,
                    onConfirmed = {
                        coroutineScope.launch {
                            shortcutsRepository.acceptPinRequest(pinRequest!!)
                            finish()
                        }
                    },
                    onCancelled = {
                        log.i("Unable to pin shortcut ${pinRequest!!.shortcut}: cancelled by user")
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun PinShortcutConfirmationDialog(
    shortcut: Shortcut,
    possibleDuplicate: Boolean,
    onConfirmed: () -> Unit,
    onCancelled: () -> Unit,
) {
    AlertDialog(
        icon = {
            SearchResultIcon(
                shortcut,
                modifier = Modifier.size(48.dp),
            )
        },
        title = {
            Text(shortcut.label)
        },
        text = if (possibleDuplicate) ({
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.pin_shortcut_possible_duplicate))
            }
        }) else null,
        textContentColor = MaterialTheme.colorScheme.error,
        confirmButton = {
            TextButton(onClick = onConfirmed) {
                Text(stringResource(id = R.string.pin_shortcut_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelled) {
                Text(stringResource(id = R.string.pin_shortcut_cancel))
            }
        },
        onDismissRequest = onCancelled
    )
}
