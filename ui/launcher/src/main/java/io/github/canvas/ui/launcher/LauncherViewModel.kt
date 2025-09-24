package io.github.canvas.ui.launcher

import android.app.Activity
import android.appwidget.AppWidgetHostView
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.canvas.data.CanvasLauncherApplication
import io.github.canvas.data.SearchResult
import io.github.canvas.data.activities.ActivitiesRepository
import io.github.canvas.data.calendar.CalendarRepository
import io.github.canvas.data.combine
import io.github.canvas.data.contacts.ContactsRepository
import io.github.canvas.data.icons.AvailableIconPack
import io.github.canvas.data.icons.IconPack
import io.github.canvas.data.icons.IconPackRepository
import io.github.canvas.data.settings.Settings
import io.github.canvas.data.settings.SettingsRepository
import io.github.canvas.data.shortcuts.ShortcutsRepository
import io.github.canvas.data.widgets.AvailableWidget
import io.github.canvas.data.widgets.Widget
import io.github.canvas.data.widgets.WidgetId
import io.github.canvas.data.widgets.Widgets
import io.github.canvas.data.widgets.WidgetsRepository
import io.github.canvas.data.widgets.WidgetsRepository.RequestWidgetPermissionRequest
import io.github.canvas.data.widgets.WidgetsRepository.RequestWidgetPermissionResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.Int.Companion.MAX_VALUE

private const val SEARCH_TERM = "searchTerm"
private const val SELECTION_START = "selectionStart"
private const val SELECTION_END = "selectionEnd"

/** The viewmodel for the main launcher screen that provides the search results */
class LauncherViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val activitiesRepository: ActivitiesRepository,
    private val shortcutsRepository: ShortcutsRepository,
    private val contactsRepository: ContactsRepository,
    private val calendarRepository: CalendarRepository,
    private val settingsRepository: SettingsRepository,
    private val widgetsRepository: WidgetsRepository,
    private val iconPackRepository: IconPackRepository,
) : ViewModel() {
    val searchTerm: MutableState<TextFieldValue> = mutableStateOf(
        TextFieldValue(
            text = savedStateHandle.get<String>(SEARCH_TERM) ?: "",
            selection = if (savedStateHandle.contains(SELECTION_START) &&
                savedStateHandle.contains(SELECTION_END)
            ) {
                TextRange(
                    savedStateHandle.get<Int>(SELECTION_START)!!,
                    savedStateHandle.get<Int>(SELECTION_END)!!
                )
            } else TextRange.Zero
        )
    )

    fun updateSearchTerm(searchTerm: TextFieldValue) {
        this.searchTerm.value = searchTerm
        savedStateHandle[SEARCH_TERM] = searchTerm.text
        savedStateHandle[SELECTION_START] = searchTerm.selection.start
        savedStateHandle[SELECTION_END] = searchTerm.selection.end
    }

    private val searchTermFlow: Flow<String> = snapshotFlow { searchTerm.value.text }

    /** Special search results appearing before all other results, such as [io.github.canvas.ui.MathSearchResult] or [io.github.canvas.ui.UrlSearchResult] */
    val specialSearchResults: StateFlow<List<SearchResult>> =
        searchTermFlow.map(::computeSpecialSearchResults)
            .stateIn(viewModelScope, started = Eagerly, initialValue = emptyList())

    /** The search results matching the [searchTerm] */
    val searchResults: StateFlow<List<SearchResult>> = combine(
        searchTermFlow,
        settingsRepository.settings.map { it.initialResults }.distinctUntilChanged(),
        activitiesRepository.activities,
        shortcutsRepository.staticShortcuts,
        shortcutsRepository.dynamicShortcuts,
        shortcutsRepository.pinnedShortcuts,
        shortcutsRepository.cachedShortcuts,
        contactsRepository.contacts,
        calendarRepository.events,
        ::computeSearchResults
    ).stateIn(viewModelScope, started = Eagerly, initialValue = emptyList())

    /** Shortcuts to search for [searchTerm] using an external search engine, displayed after all other search results */
    val externalSearchSearchResults: StateFlow<List<SearchResult>> = combine(
        searchTermFlow, settingsRepository.settings,
        ::computeExternalSearchSearchResults
    ).stateIn(viewModelScope, started = Eagerly, initialValue = emptyList())

    /** All search results that could potentially be in [searchResults], except special search results */
    val allSearchResults: StateFlow<List<SearchResult>?> = combine(
        activitiesRepository.activities,
        shortcutsRepository.staticShortcuts,
        shortcutsRepository.dynamicShortcuts,
        shortcutsRepository.pinnedShortcuts,
        shortcutsRepository.cachedShortcuts,
        contactsRepository.contacts,
        calendarRepository.events,
    ) { activities, static, dynamic, pinned, cached, contacts, events ->
        when {
            // Don't send half-loaded values, that would be interpreted as some search results having been deleted
            activities == null || static == null || dynamic == null || pinned == null || cached == null || contacts == null || events == null -> null
            else -> activities + static + dynamic + pinned + cached + contacts + events
        }
    }.stateIn(viewModelScope, started = WhileSubscribed(), initialValue = null)

    val settings: Flow<Settings> by settingsRepository::settings

    fun setContactSearchEnabled(value: Boolean) {
        viewModelScope.launch { settingsRepository.setContactSearchEnabled(value) }
    }

    fun setCalendarSearchEnabled(value: Boolean) {
        viewModelScope.launch { settingsRepository.setCalendarSearchEnabled(value) }
    }

    fun setOnboardingShown(value: Boolean) {
        viewModelScope.launch { settingsRepository.setOnboardingShown(value) }
    }

    fun onShortcutCreationActivityResult(result: Intent?, context: Context): Unit =
        shortcutsRepository.onShortcutCreationActivityResult(result, context)

    val availableIconPacks: StateFlow<List<AvailableIconPack>?> by iconPackRepository::availableIconPacks
    suspend fun loadIconPack(iconPack: AvailableIconPack): IconPack =
        iconPackRepository.loadIconPack(iconPack)

    val widgets: StateFlow<Widgets?> by widgetsRepository::widgets

    fun startListening(): Unit = widgetsRepository.startListening()
    fun stopListening(): Unit = widgetsRepository.stopListening()

    fun getAvailableWidgetsOfApp(
        packageName: String, context: Context,
    ): Flow<List<AvailableWidget>?> =
        widgetsRepository.getAvailableWidgetsOfApp(packageName, context)

    fun getAvailableWidgetsByApp(context: Context):
            Flow<List<Pair<WidgetsRepository.App, List<AvailableWidget>>>> =
        widgetsRepository.getAvailableWidgetsByApp(context)

    fun addWidget(
        widget: AvailableWidget,
        requestWidgetPermission: (RequestWidgetPermissionRequest) -> Unit,
        configurationResultActivity: Activity,
        context: Context,
    ): Unit = widgetsRepository.addWidgetAsync(
        widget, requestWidgetPermission, configurationResultActivity, context
    )

    fun onRequestWidgetPermissionResult(
        result: RequestWidgetPermissionResult,
        configurationResultActivity: Activity, context: Context,
    ): Unit = widgetsRepository.onRequestWidgetPermissionResult(
        result, configurationResultActivity, context
    )

    fun onWidgetConfigurationResult(resultCode: Int, intent: Intent?): Unit =
        widgetsRepository.onWidgetConfigurationResult(resultCode, intent)

    suspend fun loadWidgetView(
        widget: Widget, context: Context,
    ): AppWidgetHostView? = widgetsRepository.loadWidgetView(widget, context)

    fun remove(widget: Widget): Unit = widgetsRepository.removeAsync(widget)

    fun reconfigureWidget(widget: Widget, configurationResultActivity: Activity): Unit =
        widgetsRepository.reconfigureWidgetAsync(widget, configurationResultActivity)

    suspend fun loadWidgetIcon(widget: Widget, context: Context): Drawable =
        widgetsRepository.loadIcon(widget, context)

    fun moveWidget(widget: Widget, page: Int): Unit =
        widgetsRepository.moveWidgetAsync(widget, page)

    fun reorderWidgets(
        reorderedWidgets: List<WidgetId>, updatedWeights: Map<WidgetId, Float>,
    ): Unit = widgetsRepository.reorderWidgetsAsync(reorderedWidgets, updatedWeights)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as CanvasLauncherApplication)
                LauncherViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    activitiesRepository = application.activitiesRepository,
                    shortcutsRepository = application.shortcutsRepository,
                    contactsRepository = application.contactsRepository,
                    calendarRepository = application.calendarRepository,
                    settingsRepository = application.settingsRepository,
                    widgetsRepository = application.widgetsRepository,
                    iconPackRepository = application.iconPackRepository,
                )
            }
        }
    }
}

fun LauncherViewModel.selectSearchTerm() {
    if (searchTerm.value.text.isNotEmpty()) {
        updateSearchTerm(searchTerm.value.copy(selection = TextRange(0, MAX_VALUE)))
    }
}
