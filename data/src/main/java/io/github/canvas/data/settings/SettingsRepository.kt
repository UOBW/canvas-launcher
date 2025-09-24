package io.github.canvas.data.settings

import androidx.datastore.core.DataStore
import io.github.canvas.data.log
import io.github.canvas.data.proto.SettingsProto
import io.github.canvas.data.proto.SettingsProto.ButtonBehaviorProto
import io.github.canvas.data.proto.SettingsProto.InitialResultsProto
import io.github.canvas.data.proto.SettingsProto.MonochromeIconColorsProto
import io.github.canvas.data.proto.SettingsProto.ThemeProto
import io.github.canvas.data.proto.copy
import io.github.canvas.data.repositoryCoroutineScope
import io.github.canvas.data.settings.Settings.ButtonBehavior
import io.github.canvas.data.settings.Settings.ButtonBehavior.CLEAR_SEARCH_TERM
import io.github.canvas.data.settings.Settings.ButtonBehavior.DO_NOTHING
import io.github.canvas.data.settings.Settings.ButtonBehavior.HIDE_KEYBOARD
import io.github.canvas.data.settings.Settings.ButtonBehavior.OPEN_FIRST_RESULT
import io.github.canvas.data.settings.Settings.MonochromeIconColors
import io.github.canvas.data.settings.Settings.MonochromeIconColors.BLACK
import io.github.canvas.data.settings.Settings.MonochromeIconColors.PRIMARY
import io.github.canvas.data.settings.Settings.MonochromeIconColors.SECONDARY
import io.github.canvas.data.settings.Settings.MonochromeIconColors.TERTIARY
import io.github.canvas.data.settings.Settings.MonochromeIconColors.WHITE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


class SettingsRepository internal constructor(
    private val dataStore: DataStore<SettingsProto>,
) {
    private val coroutineScope: CoroutineScope = repositoryCoroutineScope()

    /** The current global user settings */
    val settings: Flow<Settings> = dataStore.data.map {
        Settings(
            contactSearchEnabled = it.contactSearch,
            hideContactShortcuts = !it.showContactShortcuts,
            appStoreSearchEnabled = it.appStoreSearch,
            initialResults = it.initialResults.toInitialResults(),
            theme = it.theme.toTheme(),
            monochromeIconsEnabled = it.monochromeIcons,
            monochromeIconColors = it.monochromeIconColors.toMonochromeIconColors(),
            iconPack = if (it.hasIconPack()) it.iconPack else null,
            widgetsEnabled = !it.widgetsDisabled,
            resizeWidgets = !it.widgetsBehindKeyboard,
            imeButtonBehavior = it.imeButtonBehavior.toButtonBehavior(defaultValue = Settings.defaultImeButtonBehavior),
            spaceKeyBehavior = it.spaceKeyBehavior.toButtonBehavior(defaultValue = Settings.defaultSpaceKeyBehavior),
            onboardingShown = it.onboardingShown,
            calendarSearchEnabled = it.calendarSearch,
            sortResultsByUsage = it.sortResultsByUsage,
            showDeveloperOptions = it.showDeveloperOptions,
        ).also { settings ->
            log.v("Settings accessed or changed: $settings")
        }
    }

    suspend fun setContactSearchEnabled(value: Boolean) {
        dataStore.updateData { it.copy { contactSearch = value } }
    }

    suspend fun setMonochromeIconsEnable(enabled: Boolean) {
        dataStore.updateData { it.copy { monochromeIcons = enabled } }
    }

    suspend fun setMonochromeIconColors(colors: MonochromeIconColors) {
        dataStore.updateData {
            it.copy { monochromeIconColors = colors.toMonochromeIconColorsProto() }
        }
    }

    suspend fun setAppStoreSearchEnabled(enabled: Boolean) {
        dataStore.updateData { it.copy { appStoreSearch = enabled } }
    }

    fun setAppStoreSearchEnabledAsync(enabled: Boolean) {
        coroutineScope.launch { setAppStoreSearchEnabled(enabled) }
    }

    suspend fun setWidgetsEnabled(enabled: Boolean) {
        dataStore.updateData { it.copy { widgetsDisabled = !enabled } }
    }

    suspend fun setResizeWidgets(enabled: Boolean) {
        dataStore.updateData { it.copy { widgetsBehindKeyboard = !enabled } }
    }

    suspend fun setImeButtonBehavior(behavior: ButtonBehavior) {
        dataStore.updateData { it.copy { imeButtonBehavior = behavior.toButtonBehaviorProto() } }
    }

    suspend fun setSpaceKeyBehavior(behavior: ButtonBehavior) {
        dataStore.updateData { it.copy { spaceKeyBehavior = behavior.toButtonBehaviorProto() } }
    }

    suspend fun setHideContactShortcuts(enabled: Boolean) {
        dataStore.updateData { it.copy { showContactShortcuts = !enabled } }
    }

    suspend fun setInitialResults(value: Settings.InitialResults) {
        dataStore.updateData { it.copy { initialResults = value.toInitialResultsProto() } }
    }

    suspend fun setIconPack(iconPack: String?) {
        dataStore.updateData {
            it.copy {
                if (iconPack != null) this.iconPack = iconPack else clearIconPack()
            }
        }
    }

    suspend fun setTheme(value: Settings.Theme) {
        dataStore.updateData { it.copy { theme = value.toThemeProto() } }
    }

    suspend fun setOnboardingShown(value: Boolean) {
        dataStore.updateData { it.copy { onboardingShown = value } }
    }

    suspend fun setCalendarSearchEnabled(enabled: Boolean) {
        dataStore.updateData { it.copy { calendarSearch = enabled } }
    }

    suspend fun setSortResultsByUsage(enabled: Boolean) {
        dataStore.updateData { it.copy { sortResultsByUsage = enabled } }
    }

    suspend fun setShowDeveloperOptions(enabled: Boolean) {
        dataStore.updateData { it.copy { showDeveloperOptions = enabled } }
    }

    companion object {
        private fun ButtonBehavior.toButtonBehaviorProto(): ButtonBehaviorProto = when (this) {
            OPEN_FIRST_RESULT -> ButtonBehaviorProto.OPEN_FIRST_RESULT
            HIDE_KEYBOARD -> ButtonBehaviorProto.HIDE_KEYBOARD
            DO_NOTHING -> ButtonBehaviorProto.DO_NOTHING
            CLEAR_SEARCH_TERM -> ButtonBehaviorProto.CLEAR_SEARCH_TERM
        }

        private fun ButtonBehaviorProto.toButtonBehavior(defaultValue: ButtonBehavior): ButtonBehavior =
            when (this) {
                ButtonBehaviorProto.BEHAVIOR_UNSPECIFIED, ButtonBehaviorProto.UNRECOGNIZED -> defaultValue
                ButtonBehaviorProto.OPEN_FIRST_RESULT -> OPEN_FIRST_RESULT
                ButtonBehaviorProto.HIDE_KEYBOARD -> HIDE_KEYBOARD
                ButtonBehaviorProto.DO_NOTHING -> DO_NOTHING
                ButtonBehaviorProto.CLEAR_SEARCH_TERM -> CLEAR_SEARCH_TERM
            }

        private fun MonochromeIconColorsProto.toMonochromeIconColors(): MonochromeIconColors =
            when (this) {
                MonochromeIconColorsProto.COLORS_UNSPECIFIED, MonochromeIconColorsProto.UNRECOGNIZED -> Settings.defaultMonochromeIconColors
                MonochromeIconColorsProto.PRIMARY -> PRIMARY
                MonochromeIconColorsProto.SECONDARY -> SECONDARY
                MonochromeIconColorsProto.TERTIARY -> TERTIARY
                MonochromeIconColorsProto.BLACK -> BLACK
                MonochromeIconColorsProto.WHITE -> WHITE
            }

        private fun MonochromeIconColors.toMonochromeIconColorsProto(): MonochromeIconColorsProto =
            when (this) {
                PRIMARY -> MonochromeIconColorsProto.PRIMARY
                SECONDARY -> MonochromeIconColorsProto.SECONDARY
                TERTIARY -> MonochromeIconColorsProto.TERTIARY
                BLACK -> MonochromeIconColorsProto.BLACK
                WHITE -> MonochromeIconColorsProto.WHITE
            }

        private fun ThemeProto.toTheme(): Settings.Theme = when (this) {
            ThemeProto.UNRECOGNIZED, ThemeProto.THEME_UNSPECIFIED -> Settings.defaultTheme
            ThemeProto.FOLLOW_SYSTEM -> Settings.Theme.FOLLOW_SYSTEM
            ThemeProto.LIGHT -> Settings.Theme.LIGHT
            ThemeProto.DARK -> Settings.Theme.DARK
        }

        private fun Settings.Theme.toThemeProto(): ThemeProto = when (this) {
            Settings.Theme.FOLLOW_SYSTEM -> ThemeProto.FOLLOW_SYSTEM
            Settings.Theme.LIGHT -> ThemeProto.LIGHT
            Settings.Theme.DARK -> ThemeProto.DARK
        }

        private fun InitialResultsProto.toInitialResults(): Settings.InitialResults = when (this) {
            InitialResultsProto.RESULTS_UNSPECIFIED, InitialResultsProto.UNRECOGNIZED -> Settings.defaultInitialResults
            InitialResultsProto.FAVORITES -> Settings.InitialResults.FAVORITES
            InitialResultsProto.ALL_APPS -> Settings.InitialResults.ALL_APPS
            InitialResultsProto.NOTHING -> Settings.InitialResults.NOTHING
        }

        private fun Settings.InitialResults.toInitialResultsProto(): InitialResultsProto =
            when (this) {
                Settings.InitialResults.FAVORITES -> InitialResultsProto.FAVORITES
                Settings.InitialResults.ALL_APPS -> InitialResultsProto.ALL_APPS
                Settings.InitialResults.NOTHING -> InitialResultsProto.NOTHING
            }
    }
}
