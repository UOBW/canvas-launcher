package io.github.canvas.ui.settings.screens

import android.Manifest
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.canvas.data.settings.Settings
import io.github.canvas.ui.BasicSettingsItem
import io.github.canvas.ui.FixedChooseOptionSetting
import io.github.canvas.ui.StaticOption
import io.github.canvas.ui.ToggleSetting
import io.github.canvas.ui.ToggleSettingRequiringPermission
import io.github.canvas.ui.settings.HiddenAppsScreen
import io.github.canvas.ui.settings.R
import io.github.canvas.ui.settings.Route
import io.github.canvas.ui.settings.SettingsViewModel

enum class InitialResultsOption : StaticOption {
    FAVORITES {
        @Composable
        override fun title(): String = stringResource(R.string.initial_results_favorites)
    },
    ALL_APPS {
        @Composable
        override fun title(): String = stringResource(R.string.initial_results_all_apps)
    },
    NOTHING {
        @Composable
        override fun title(): String = stringResource(R.string.initial_results_nothing)
    }
}

fun Settings.InitialResults.toOption(): InitialResultsOption = when (this) {
    Settings.InitialResults.FAVORITES -> InitialResultsOption.FAVORITES
    Settings.InitialResults.ALL_APPS -> InitialResultsOption.ALL_APPS
    Settings.InitialResults.NOTHING -> InitialResultsOption.NOTHING
}

fun InitialResultsOption.toSetting(): Settings.InitialResults = when (this) {
    InitialResultsOption.FAVORITES -> Settings.InitialResults.FAVORITES
    InitialResultsOption.ALL_APPS -> Settings.InitialResults.ALL_APPS
    InitialResultsOption.NOTHING -> Settings.InitialResults.NOTHING
}

@Composable
fun SearchScreen(
    settingsViewModel: SettingsViewModel,
    navigateTo: (route: Route) -> Unit,
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle(initialValue = null)
    settings?.let { settings ->
        Column {
            BasicSettingsItem(
                title = stringResource(R.string.hidden_results_screen_title),
                onClick = { navigateTo(HiddenAppsScreen) }
            )

            HorizontalDivider()
            ToggleSetting(
                value = settings.sortResultsByUsage,
                onValueChanged = { settingsViewModel.setSortResultsByUsage(it) },
                title = stringResource(R.string.search_sort_by_usage),
                description = stringResource(R.string.search_sort_by_usage_description)
            )

            FixedChooseOptionSetting(
                value = settings.initialResults.toOption(),
                onValueChanged = { settingsViewModel.setInitialResults(it.toSetting()) },
                title = stringResource(R.string.setting_initial_results)
            )

            HorizontalDivider()

            ToggleSettingRequiringPermission(
                permission = Manifest.permission.READ_CONTACTS,
                value = settings.contactSearchEnabled,
                onValueChanged = { value -> settingsViewModel.setContactsSearchEnabled(value) },
                title = stringResource(io.github.canvas.ui.R.string.setting_contacts_search),
                description = stringResource(io.github.canvas.ui.R.string.setting_contacts_search_description)
            )
            ToggleSetting(
                value = settings.hideContactShortcuts,
                onValueChanged = { value -> settingsViewModel.setHideContactShortcuts(value) },
                title = stringResource(R.string.setting_hide_contact_shortcuts),
                enabled = settings.contactSearchEnabled
            )

            ToggleSettingRequiringPermission(
                permission = Manifest.permission.READ_CALENDAR,
                value = settings.calendarSearchEnabled,
                onValueChanged = { settingsViewModel.setCalendarSearchEnabled(it) },
                title = stringResource(io.github.canvas.ui.R.string.setting_calendar_search),
                description = stringResource(io.github.canvas.ui.R.string.setting_calendar_search_description)
            )

            ToggleSetting(
                value = settings.appStoreSearchEnabled,
                onValueChanged = { settingsViewModel.setAppStoreSearchEnabled(it) },
                title = stringResource(R.string.setting_app_store_search),
            )
        }
    }
}
