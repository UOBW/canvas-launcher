package io.github.canvas.ui.settings.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.canvas.ui.BasicSettingsItem
import io.github.canvas.ui.settings.AboutScreen
import io.github.canvas.ui.settings.AdvancedScreen
import io.github.canvas.ui.settings.ChangeLauncherExternal
import io.github.canvas.ui.settings.R
import io.github.canvas.ui.settings.Route
import io.github.canvas.ui.settings.SearchScreen
import io.github.canvas.ui.settings.SettingsViewModel
import io.github.canvas.ui.settings.UiScreen

@Composable
fun MainScreen(viewModel: SettingsViewModel, navigateTo: (route: Route) -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle(initialValue = null)
    Column {
        BasicSettingsItem(
            title = stringResource(R.string.search_screen_title),
            description = stringResource(R.string.search_screen_description),
            icon = { Icon(Icons.Default.Search, contentDescription = null) },
            onClick = { navigateTo(SearchScreen) }
        )
        BasicSettingsItem(
            title = stringResource(R.string.ui_screen_title),
            description = stringResource(R.string.ui_screen_description),
            icon = { Icon(Icons.Default.Smartphone, contentDescription = null) },
            onClick = { navigateTo(UiScreen) }
        )
        BasicSettingsItem(
            title = stringResource(R.string.setting_open_home_app_chooser),
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            onClick = { navigateTo(ChangeLauncherExternal) }
        )
        if (settings?.showDeveloperOptions ?: false) {
            BasicSettingsItem(
                title = stringResource(R.string.developer_options_screen_title),
                // Kinda looks like the Android developer options icon
                icon = { Icon(Icons.Default.DataObject, contentDescription = null) },
                onClick = { navigateTo(AdvancedScreen) }
            )
        }
        BasicSettingsItem(
            title = stringResource(R.string.about_screen_title),
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            onClick = { navigateTo(AboutScreen) }
        )
    }
}
