package io.github.canvas.ui.launcher.dialogs

import android.annotation.SuppressLint
import android.content.ComponentName
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.canvas.data.FavoritableSearchResult
import io.github.canvas.data.HideableSearchResult
import io.github.canvas.data.IconChangeableSearchResult
import io.github.canvas.data.RenameableSearchResult
import io.github.canvas.data.SearchResult
import io.github.canvas.data.TaggableSearchResult
import io.github.canvas.data.activities.Activity
import io.github.canvas.data.shortcuts.Shortcut
import io.github.canvas.data.shortcuts.Shortcut.Type.PINNED
import io.github.canvas.ui.SearchResultIcon
import io.github.canvas.ui.applyIf
import io.github.canvas.ui.launcher.DetailsSheet
import io.github.canvas.ui.launcher.DetailsSheetButton
import io.github.canvas.ui.launcher.R
import io.github.canvas.ui.launcher.name
import io.github.canvas.ui.searchResultLabel
import kotlinx.coroutines.launch

// Add an option to the details sheet to show the IconDebugDialog
private const val ENABLE_ICON_DEBUG_DIALOG = false

/** Provides a ModalBottomSheet that displays information about a SearchResult and allows to edit some of that information */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultDetailsSheet(
    searchResult: SearchResult,
    onClosed: () -> Unit,
    onStartAddShortcutActivity: (activity: ComponentName) -> Unit,
    onShowRenameDialog: (RenameableSearchResult) -> Unit,
    onShowAddWidgetSheet: (packageName: String) -> Unit,
    onShowUndoHideSnackbar: (searchResult: HideableSearchResult) -> Unit,
    onShowChangeIconDialog: (searchResult: IconChangeableSearchResult) -> Unit,
    onShowEditTagsDialog: (searchResult: TaggableSearchResult) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    fun closeNoAnimation() = onClosed()
    suspend fun closeAnimated() {
        sheetState.hide()
        onClosed()
    }

    // Extract functionality accessible from multiple places
    fun rename() {
        coroutineScope.launch {
            onShowRenameDialog(searchResult as RenameableSearchResult)
            closeAnimated()
        }
    }

    fun changeIcon() {
        coroutineScope.launch {
            onShowChangeIconDialog(searchResult as IconChangeableSearchResult)
            closeAnimated()
        }
    }


    DetailsSheet(
        onDismissRequest = onClosed,
        icon = {
            SearchResultIcon(
                searchResult = searchResult,
                modifier = Modifier.applyIf(searchResult is IconChangeableSearchResult) {
                    clickable { changeIcon() }
                }
            )
        },
        title = {
            Text(
                text = searchResultLabel(searchResult),
                modifier = Modifier.applyIf(searchResult is RenameableSearchResult) {
                    clickable { rename() }
                })
        },
        subtitle = if (searchResult.tags.isNotEmpty()) ({
            Text(
                text = searchResult.tags.map { "#${it.name}" }.sorted().joinToString(),
                modifier = Modifier.applyIf(searchResult is TaggableSearchResult) {
                    clickable { onShowEditTagsDialog(searchResult as TaggableSearchResult) }
                }
            )
        }) else null,
        sheetState = sheetState,
        onOpen = {
            searchResult.open(context)
            closeNoAnimation()
        }
    ) {
        if (searchResult is Activity) {

            // Section ADD

            searchResult.addShortcutActivity?.let { addShortcutActivity ->
                DetailsSheetButton(
                    icon = Icons.Default.Add,
                    text = stringResource(R.string.details_sheet_add_shortcut),
                    onClick = {
                        onStartAddShortcutActivity(addShortcutActivity)
                        closeNoAnimation()
                    }
                )
            }

            if (searchResult.hasWidgets) {
                DetailsSheetButton(
                    icon = Icons.Default.Widgets,
                    text = stringResource(R.string.details_sheet_add_widget),
                    onClick = {
                        coroutineScope.launch {
                            closeAnimated()
                            onShowAddWidgetSheet(searchResult.componentName.packageName)
                        }
                    }
                )
            }

            if (searchResult.addShortcutActivity != null || searchResult.hasWidgets) HorizontalDivider()

            // Section VIEW IN

            DetailsSheetButton(
                icon = Icons.Default.Info,
                text = stringResource(R.string.details_sheet_show_app_info),
                onClick = {
                    coroutineScope.launch {
                        searchResult.openAppInfo(context)
                        closeNoAnimation()
                    }
                }
            )

            if (!searchResult.isSystemApp) { // System apps usually don't have a store page
                DetailsSheetButton(
                    icon = Icons.Default.Shop,
                    text = stringResource(R.string.details_sheet_open_store_page),
                    onClick = {
                        searchResult.openStorePage(context)
                        closeNoAnimation()
                    }
                )
            }

            if (searchResult.isAppManagerInstalled) {
                DetailsSheetButton(
                    icon = Icons.Default.Settings,
                    text = stringResource(R.string.details_sheet_open_app_manager),
                    onClick = {
                        searchResult.openInAppManger(context)
                        closeNoAnimation()
                    }
                )
            }

            HorizontalDivider()
        }

        // Section EDIT

        if (searchResult is FavoritableSearchResult) {
            DetailsSheetButton(
                icon = if (searchResult.isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                text = stringResource(if (searchResult.isFavorite) R.string.details_sheet_unstar else R.string.details_sheet_star),
                onClick = {
                    searchResult.setIsFavoriteAsync(!searchResult.isFavorite, context)
                    coroutineScope.launch { closeAnimated() }
                }
            )
        }

        if (searchResult is HideableSearchResult && searchResult.isHideable) {
            DetailsSheetButton(
                icon = Icons.Default.VisibilityOff,
                text = stringResource(R.string.details_sheet_hide_result),
                onClick = {
                    onShowUndoHideSnackbar(searchResult)
                    searchResult.setHiddenAsync(true, context)
                    coroutineScope.launch { closeAnimated() }
                }
            )
        }

        if (searchResult is RenameableSearchResult) {
            DetailsSheetButton(
                icon = Icons.Default.TextFields,
                text = stringResource(R.string.details_sheet_rename),
                onClick = ::rename
            )
        }

        if (searchResult is IconChangeableSearchResult) {
            DetailsSheetButton(
                icon = Icons.Default.Category,
                text = stringResource(R.string.details_sheet_change_icon),
                onClick = ::changeIcon
            )
        }

        if (searchResult is TaggableSearchResult) {
            DetailsSheetButton(
                icon = Icons.Default.Tag,
                text = stringResource(R.string.details_sheet_edit_tags),
                onClick = {
                    coroutineScope.launch {
                        onShowEditTagsDialog(searchResult)
                        closeAnimated()
                    }
                }
            )
        }

        when (searchResult) { // Uninstall / remove
            is Activity -> if (!searchResult.isSystemApp) {
                DetailsSheetButton(
                    icon = Icons.Default.Delete,
                    text = stringResource(R.string.details_sheet_uninstall_app),
                    onClick = {
                        searchResult.uninstall(context)
                        coroutineScope.launch { closeAnimated() }
                    }
                )
            }

            // If a Shortcut object exists, the API version must be high enough
            is Shortcut -> @SuppressLint("newapi") if (searchResult.type == PINNED) {
                DetailsSheetButton(
                    icon = Icons.Default.Clear,
                    text = stringResource(R.string.details_sheet_unpin_shortcut),
                    onClick = {
                        searchResult.unpinAsync(context)
                        coroutineScope.launch { closeAnimated() }
                    }
                )
            }
        }

        if (ENABLE_ICON_DEBUG_DIALOG) {
            HorizontalDivider()
            var showIconDebugDialog by remember { mutableStateOf(false) }
            DetailsSheetButton(
                icon = Icons.Default.Adb,
                text = stringResource(R.string.details_sheet_debug_icon),
                onClick = { showIconDebugDialog = true })
            if (showIconDebugDialog) {
                IconDebugDialog(
                    searchResult = searchResult,
                    onClosed = { showIconDebugDialog = false }
                )
            }
        }

        Text(searchResult.uid.string, style = MaterialTheme.typography.labelSmall)

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
