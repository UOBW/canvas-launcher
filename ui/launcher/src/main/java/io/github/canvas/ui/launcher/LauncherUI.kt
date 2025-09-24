package io.github.canvas.ui.launcher

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration.Short
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult.ActionPerformed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.canvas.data.IconChangeableSearchResult
import io.github.canvas.data.RenameableSearchResult
import io.github.canvas.data.SearchResult
import io.github.canvas.data.TaggableSearchResult
import io.github.canvas.data.Uid
import io.github.canvas.data.settings.Settings
import io.github.canvas.data.settings.Settings.ButtonBehavior
import io.github.canvas.data.settings.Settings.ButtonBehavior.CLEAR_SEARCH_TERM
import io.github.canvas.data.settings.Settings.ButtonBehavior.DO_NOTHING
import io.github.canvas.data.settings.Settings.ButtonBehavior.HIDE_KEYBOARD
import io.github.canvas.data.settings.Settings.ButtonBehavior.OPEN_FIRST_RESULT
import io.github.canvas.data.tryEmit
import io.github.canvas.data.widgets.WidgetId
import io.github.canvas.data.widgets.Widgets
import io.github.canvas.data.widgets.WidgetsRepository.RequestWidgetPermissionRequest
import io.github.canvas.data.widgets.orEmpty
import io.github.canvas.ui.MutableSimpleInteractionSource
import io.github.canvas.ui.applyIf
import io.github.canvas.ui.launcher.dialogs.ChangeIconDialog
import io.github.canvas.ui.launcher.dialogs.EditTagsDialog
import io.github.canvas.ui.launcher.dialogs.OnboardingDialog
import io.github.canvas.ui.launcher.dialogs.RenameDialog
import io.github.canvas.ui.launcher.dialogs.SearchResultDetailsSheet
import io.github.canvas.ui.launcher.widgets.AllAppsAddWidgetSheet
import io.github.canvas.ui.launcher.widgets.MoveWidgetDialog
import io.github.canvas.ui.launcher.widgets.ReorderWidgetsDialog
import io.github.canvas.ui.launcher.widgets.SingleAppAddWidgetSheet
import io.github.canvas.ui.launcher.widgets.WidgetDetailsSheet
import io.github.canvas.ui.launcher.widgets.WidgetScreen
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.Int.Companion.MAX_VALUE

const val SCREEN_WIDGETS: Int = 0
const val SCREEN_RESULTS: Int = 1

/** The one composable to rule them all - ties all the screens, dialogs and sheets together */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Launcher(
    viewModel: LauncherViewModel,
    textColor: Color,
    searchFocusRequester: FocusRequester,
    onSearchFocus: () -> Unit,
    onStartAddShortcutActivity: (activity: ComponentName) -> Unit,
    onRequestWidgetPermission: (RequestWidgetPermissionRequest) -> Unit,
) {
    val searchTerm by viewModel.searchTerm
    val specialSearchResults: List<SearchResult> by viewModel.specialSearchResults.collectAsState()
    val searchResults: List<SearchResult> by viewModel.searchResults.collectAsState()
    val externalSearchSearchResults by viewModel.externalSearchSearchResults.collectAsState()
    val widgets: Widgets? by viewModel.widgets.collectAsState()

    val listState = rememberLazyListState()
    val pagerState = rememberPagerState { 2 }
    val snackBarHostState = remember { SnackbarHostState() }
    val firstResultsInteractionSource = remember { MutableSimpleInteractionSource() }

    //Sheets & dialogs
    var detailsSheetResult: Uid? by rememberSaveable(stateSaver = Uid.Saver) { mutableStateOf(null) }
    var renameDialogResult: Uid? by rememberSaveable(stateSaver = Uid.Saver) { mutableStateOf(null) }
    var changeIconDialogResult: Uid?
            by rememberSaveable(stateSaver = Uid.Saver) { mutableStateOf(null) }
    var editTagsDialogResult: Uid?
            by rememberSaveable(stateSaver = Uid.Saver) { mutableStateOf(null) }
    var singleAppAddWidgetSheetPackageName: String? by rememberSaveable { mutableStateOf(null) }
    var showAllAppsAddWidgetSheet: Boolean by rememberSaveable { mutableStateOf(false) }
    var detailsSheetWidgetId: WidgetId?
            by rememberSaveable(stateSaver = WidgetId.Saver) { mutableStateOf(null) }
    var reorderWidgetsDialogPage: Int? by rememberSaveable { mutableStateOf(null) }
    var moveWidgetDialogWidgetId: WidgetId?
            by rememberSaveable(stateSaver = WidgetId.Saver) { mutableStateOf(null) }

    val imeButtonBehavior by viewModel.settings.map { it.imeButtonBehavior }
        .collectAsState(initial = Settings.defaultImeButtonBehavior)
    val spaceKeyBehavior by viewModel.settings.map { it.spaceKeyBehavior }
        .collectAsState(initial = Settings.defaultSpaceKeyBehavior)
    val widgetsEnabled by viewModel.settings.map { it.widgetsEnabled }
        .collectAsState(initial = true)
    val resizeWidgets by viewModel.settings.map { it.resizeWidgets }
        .collectAsState(initial = true)
    val onboardingShown by viewModel.settings.map { it.onboardingShown }
        .collectAsState(initial = true)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    fun doButtonBehavior(behavior: ButtonBehavior) {
        // @formatter:off the formatter doesn't support when guards yet
        when (behavior) {
            DO_NOTHING -> {}
            HIDE_KEYBOARD -> keyboardController?.hide()

            // The options below are useless when on the widgets screen or when without search term, so default to close keyboard
            else if pagerState.settledPage == SCREEN_WIDGETS ||
                    searchTerm.text.isEmpty() -> keyboardController?.hide()

            CLEAR_SEARCH_TERM -> viewModel.selectSearchTerm()
            OPEN_FIRST_RESULT -> firstResultsInteractionSource.tryEmit()
        }
        // @formatter:on
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = {
            SnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
            )
        }
    ) { contentPadding ->
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.titleLarge.merge(color = textColor),
            // For the icon tint
            LocalContentColor provides textColor
        ) {
            Column(modifier = Modifier.padding(contentPadding)) {
                SearchField(
                    searchTerm = searchTerm,
                    onSearchTermChange = { newValue ->
                        val oldValue = searchTerm // Alias
                        if (
                            spaceKeyBehavior != DO_NOTHING &&
                            newValue.text.count { it.isWhitespace() } == 1 &&
                            oldValue.text.count { it.isWhitespace() } == 0 &&
                            newValue.text.length <= oldValue.text.length + 1 // Try not to detect pasted text
                        ) { // A single space has just been inserted
                            viewModel.updateSearchTerm(oldValue) // Revert the change
                            doButtonBehavior(spaceKeyBehavior)
                        } else {
                            viewModel.updateSearchTerm(newValue)
                            if (newValue.text != oldValue.text) { // Scroll to the top if the search term changed
                                if (newValue.text.isNotBlank()) coroutineScope.launch {
                                    pagerState.animateScrollToPage(SCREEN_RESULTS)
                                }
                                listState.requestScrollToItem(0)
                            }
                        }
                    },
                    focusRequester = searchFocusRequester,
                    onFocus = onSearchFocus,
                    onImeAction = { doButtonBehavior(imeButtonBehavior) }
                )

                VerticalPager(
                    state = pagerState,
                    userScrollEnabled = widgetsEnabled,
                    flingBehavior = PagerDefaults.flingBehavior(
                        state = pagerState,
                        snapPositionalThreshold = 0.1f
                    ),
                    beyondViewportPageCount = 0, // Don't render widgets while on search screen
                    modifier = Modifier
                        .applyIf(resizeWidgets) { imePadding() }
                        .fillMaxSize()
                ) { screen ->
                    when (screen) {
                        SCREEN_WIDGETS -> WidgetScreen(
                            widgets = widgets.orEmpty(),
                            loadWidgetView = { widget ->
                                viewModel.loadWidgetView(widget, context)
                            },
                            onShowWidgetDetailsSheet = { detailsSheetWidgetId = it.id },
                            onShowAddWidgetSheet = { showAllAppsAddWidgetSheet = true },
                        )

                        SCREEN_RESULTS -> SearchResultsScreen(
                            specialSearchResults, searchResults, externalSearchSearchResults,
                            listState = listState,
                            beforeLaunch = {
                                viewModel.updateSearchTerm(
                                    searchTerm.copy(selection = TextRange(0, MAX_VALUE))
                                )
                            },
                            onShowDetailsSheet = { detailsSheetResult = it.uid },
                            firstResultInteractionSource = firstResultsInteractionSource,
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(pagerState) {
        // Select the search term when scrolling to the widgets page
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (page == SCREEN_WIDGETS) viewModel.selectSearchTerm()
        }
    }

    LaunchedEffect(widgetsEnabled, pagerState) {
        // Go to results page if user disabled widgets
        if (!widgetsEnabled) pagerState.scrollToPage(SCREEN_RESULTS)
    }

    BackHandler {
        viewModel.updateSearchTerm(TextFieldValue())
    }

    val activity = LocalActivity.current
    LaunchedEffect(activity) {
        // Home button pressed while already in app
        (activity as? ComponentActivity)?.addOnNewIntentListener {
            viewModel.selectSearchTerm()
//            coroutineScope.launch { pagerState.animateScrollToPage(SCREEN_WIDGETS) }
        } ?: log.w("Unable to add home button listener: activity is not a ComponentActivity")
    }

    // -----------------------------
    // Dialogs & modal bottom sheets
    // -----------------------------
    if (!onboardingShown) {
        OnboardingDialog(
            viewModel = viewModel,
            onClosed = { viewModel.setOnboardingShown(true) }
        )
    }

    detailsSheetResult?.let { resultUid ->
        val searchResult = getSearchResultByUid(
            resultUid, viewModel.allSearchResults.collectAsState().value,
            specialSearchResults, externalSearchSearchResults,
            onSearchResultNoLongerExists = { detailsSheetResult = null }
        ) ?: return@let // Search result is still loading

        SearchResultDetailsSheet(
            searchResult = searchResult,
            onClosed = { detailsSheetResult = null },
            onStartAddShortcutActivity = onStartAddShortcutActivity,
            onShowRenameDialog = { renameDialogResult = it.uid },
            onShowAddWidgetSheet = { singleAppAddWidgetSheetPackageName = it },
            onShowChangeIconDialog = { changeIconDialogResult = it.uid },
            onShowEditTagsDialog = { editTagsDialogResult = it.uid },
            onShowUndoHideSnackbar = { searchResult ->
                coroutineScope.launch {
                    val result = snackBarHostState.showSnackbar(
                        message = context.getString(R.string.snackbar_undo_hide_text),
                        actionLabel = context.getString(R.string.snackbar_undo),
                        withDismissAction = true,
                        duration = Short
                    )
                    if (result == ActionPerformed) {
                        searchResult.setHiddenAsync(false, context)
                    }
                }
            },
        )
    }

    renameDialogResult?.let { resultUid ->
        val searchResult = getSearchResultByUid(
            resultUid, viewModel.allSearchResults.collectAsState().value,
            specialSearchResults, externalSearchSearchResults,
            onSearchResultNoLongerExists = { renameDialogResult = null }
        ) ?: return@let // Search result is still loading

        RenameDialog(
            searchResult = searchResult as RenameableSearchResult,
            onClosed = { renameDialogResult = null },
            onRename = { newName, oldName ->
                if (newName == oldName) return@RenameDialog // Don't show the undo snackbar if nothing changed
                searchResult.renameAsync(newName, context)

                coroutineScope.launch {
                    val result = snackBarHostState.showSnackbar(
                        message = context.getString(R.string.snackbar_undo_rename_text),
                        actionLabel = context.getString(R.string.snackbar_undo),
                        withDismissAction = true,
                        duration = Short
                    )
                    if (result == ActionPerformed) {
                        searchResult.renameAsync(oldName, context)
                    }
                }
            }
        )
    }

    changeIconDialogResult?.let { resultUid ->
        val searchResult = getSearchResultByUid(
            resultUid, viewModel.allSearchResults.collectAsState().value,
            specialSearchResults, externalSearchSearchResults,
            onSearchResultNoLongerExists = { renameDialogResult = null }
        ) ?: return@let // Search result is still loading

        ChangeIconDialog(
            searchResult = searchResult as IconChangeableSearchResult,
            availableIconPacks = viewModel.availableIconPacks.collectAsState().value,
            loadIconPack = viewModel::loadIconPack,
            onClosed = { changeIconDialogResult = null },
            onIconChanged = { icon -> searchResult.setIconAsync(icon, context) }
        )
    }

    editTagsDialogResult?.let { resultUid ->
        val allResults by viewModel.allSearchResults.collectAsState()
        val searchResult = getSearchResultByUid(
            resultUid, allResults, specialSearchResults, externalSearchSearchResults,
            onSearchResultNoLongerExists = { editTagsDialogResult = null }
        ) ?: return@let // Search result is still loading

        EditTagsDialog(
            searchResult = searchResult as TaggableSearchResult,
            allSearchResults = allResults!!,
            onClosed = { editTagsDialogResult = null },
            onEditTags = { searchResult.setTagsAsync(it, context) }
        )
    }

    singleAppAddWidgetSheetPackageName?.let { packageName ->
        val availableWidgets by remember {
            viewModel.getAvailableWidgetsOfApp(packageName, context)
        }.collectAsState(initial = emptyList())

        if (availableWidgets == null) { // App is no longer installed
            @Suppress("AssignedValueIsNeverRead") // Idk why this is reported
            singleAppAddWidgetSheetPackageName = null
            return@let
        }

        SingleAppAddWidgetSheet(
            widgets = availableWidgets!!,
            onClosed = { singleAppAddWidgetSheetPackageName = null },
            onAddWidget = { widget ->
                viewModel.addWidget(widget, onRequestWidgetPermission, activity!!, context)
            }
        )
    }

    if (showAllAppsAddWidgetSheet) {
        val availableWidgets by remember {
            viewModel.getAvailableWidgetsByApp(context)
        }.collectAsState(initial = emptyList())
        AllAppsAddWidgetSheet(
            widgets = availableWidgets,
            onClosed = { showAllAppsAddWidgetSheet = false },
            onAddWidget = { widget ->
                viewModel.addWidget(widget, onRequestWidgetPermission, activity!!, context)
            }
        )
    }

    detailsSheetWidgetId?.let { widgetId ->
        val widget = getWidgetById(
            widgetId, widgets,
            onWidgetNoLongerExists = { detailsSheetWidgetId = null }
        ) ?: return@let // Widgets are still loading

        WidgetDetailsSheet(
            widget = widget,
            showReorderButton = widgets!!.page(widget.page).size > 1, // Don't show the reorder button if the widget is alone on the page
            loadWidgetIcon = { viewModel.loadWidgetIcon(widget, context) },
            onClosed = { detailsSheetWidgetId = null },
            onRemoveWidget = { viewModel.remove(widget) },
            onReconfigureWidget = { viewModel.reconfigureWidget(widget, activity!!) },
            onReorderWidgets = { reorderWidgetsDialogPage = it },
            onMoveWidget = { moveWidgetDialogWidgetId = widgetId },
            onAddNewWidget = { showAllAppsAddWidgetSheet = true },
        )
    }

    reorderWidgetsDialogPage?.let { page ->
        if (widgets == null) return@let // Widgets not loaded yet

        ReorderWidgetsDialog(
            widgets = widgets!!,
            onReorder = { reorderedWidgets, updatedWeights ->
                viewModel.reorderWidgets(reorderedWidgets, updatedWeights)
            },
            onClosed = { reorderWidgetsDialogPage = null },
            page = page
        )
    }

    moveWidgetDialogWidgetId?.let { widgetId ->
        val widget = getWidgetById(
            widgetId, widgets,
            onWidgetNoLongerExists = { detailsSheetWidgetId = null }
        ) ?: return@let // Widgets are still loading

        MoveWidgetDialog(
            widget = widget,
            widgets = widgets!!,
            onMove = { page -> viewModel.moveWidget(widget, page) },
            onClosed = { moveWidgetDialogWidgetId = null }
        )
    }
}
