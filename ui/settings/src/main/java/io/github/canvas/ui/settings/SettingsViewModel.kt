package io.github.canvas.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.canvas.data.CanvasLauncherApplication
import io.github.canvas.data.HideableSearchResult
import io.github.canvas.data.activities.ActivitiesRepository
import io.github.canvas.data.contacts.ContactsRepository
import io.github.canvas.data.icons.AvailableIconPack
import io.github.canvas.data.icons.IconPackRepository
import io.github.canvas.data.settings.Settings
import io.github.canvas.data.settings.Settings.ButtonBehavior
import io.github.canvas.data.settings.SettingsRepository
import io.github.canvas.data.shortcuts.ShortcutsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** This is basically an adapter for a SettingsRepository, but with non-blocking calls, that may return before the new value has been set */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val activitiesRepository: ActivitiesRepository,
    private val shortcutsRepository: ShortcutsRepository,
    private val contactsRepository: ContactsRepository,
    private val iconPackRepository: IconPackRepository,
) : ViewModel() {
    /** The current global user settings or null if not yet loaded */
    @Suppress("UnusedFlow") // Idk why this is reported
    val settings: Flow<Settings?> by settingsRepository::settings

    fun setContactsSearchEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setContactSearchEnabled(enabled) }
    }

    fun setCalendarSearchEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setCalendarSearchEnabled(enabled) }
    }

    fun setMonochromeIconsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMonochromeIconsEnable(enabled) }
    }

    fun setMonochromeIconColors(colors: Settings.MonochromeIconColors) {
        viewModelScope.launch { settingsRepository.setMonochromeIconColors(colors) }
    }

    fun setAppStoreSearchEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAppStoreSearchEnabled(enabled) }
    }

    fun setWidgetsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setWidgetsEnabled(enabled) }
    }

    fun setResizeWidgets(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setResizeWidgets(enabled) }
    }

    fun setImeButtonBehavior(behavior: ButtonBehavior) {
        viewModelScope.launch { settingsRepository.setImeButtonBehavior(behavior) }
    }

    fun setSpaceKeyBehavior(behavior: ButtonBehavior) {
        viewModelScope.launch { settingsRepository.setSpaceKeyBehavior(behavior) }
    }

    fun setHideContactShortcuts(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHideContactShortcuts(enabled) }
    }

    val availableIconPacks: StateFlow<List<AvailableIconPack>?> by iconPackRepository::availableIconPacks

    fun setIconPack(iconPack: String?) {
        viewModelScope.launch { settingsRepository.setIconPack(iconPack) }
    }

    fun setTheme(theme: Settings.Theme) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    fun setInitialResults(value: Settings.InitialResults) {
        viewModelScope.launch { settingsRepository.setInitialResults(value) }
    }

    fun setOnboardingShown(value: Boolean) {
        viewModelScope.launch { settingsRepository.setOnboardingShown(value) }
    }

    fun setShowDeveloperOptions(value: Boolean) {
        viewModelScope.launch { settingsRepository.setShowDeveloperOptions(value) }
    }

    fun setSortResultsByUsage(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSortResultsByUsage(value)
            activitiesRepository.deleteUsageDataAsync()
        }
    }

    val hiddenResults: StateFlow<List<HideableSearchResult>> =
        combine(
            activitiesRepository.hiddenActivities,
            shortcutsRepository.hiddenShortcuts,
            contactsRepository.hidden,
        ) { activities, shortcuts, contacts ->
            activities + shortcuts + contacts
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(), // Only needed when on the hidden results screen
            initialValue = emptyList()
        )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    (this[AndroidViewModelFactory.APPLICATION_KEY] as CanvasLauncherApplication)
                SettingsViewModel(
                    settingsRepository = application.settingsRepository,
                    activitiesRepository = application.activitiesRepository,
                    shortcutsRepository = application.shortcutsRepository,
                    contactsRepository = application.contactsRepository,
                    iconPackRepository = application.iconPackRepository
                )
            }
        }
    }
}
