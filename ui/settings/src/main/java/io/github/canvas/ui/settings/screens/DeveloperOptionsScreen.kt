package io.github.canvas.ui.settings.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.canvas.ui.ToggleSetting
import io.github.canvas.ui.settings.R
import io.github.canvas.ui.settings.SettingsViewModel

@Composable
fun DeveloperOptionsScreen(settingsViewModel: SettingsViewModel) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle(initialValue = null)
    settings?.let { settings ->
        Column {
            ToggleSetting(
                value = settings.showDeveloperOptions,
                onValueChanged = { settingsViewModel.setShowDeveloperOptions(it) },
                title = stringResource(R.string.setting_show_developer_options)
            )

            ToggleSetting(
                value = !settings.onboardingShown,
                onValueChanged = { settingsViewModel.setOnboardingShown(!it) },
                title = stringResource(R.string.setting_show_onboarding_again)
            )
        }
    }
}
