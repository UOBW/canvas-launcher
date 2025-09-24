package io.github.canvas.ui.launcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import io.github.canvas.data.Logger
import io.github.canvas.data.SearchResult
import io.github.canvas.data.Tag
import io.github.canvas.data.Uid
import io.github.canvas.data.get
import io.github.canvas.data.widgets.Widget
import io.github.canvas.data.widgets.WidgetId
import io.github.canvas.data.widgets.Widgets
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

val log: Logger = Logger("io.github.canvas.ui.launcher")

val Dp.int: Int get() = value.toInt()

/**
 * Returns the [SearchResult] identified by [uid] from the list of all available search results
 * * If the list is still loading, returns `null`
 * * If no search result in the list matches [uid], calls [onSearchResultNoLongerExists] and returns `null`
 */
@Composable
fun getSearchResultByUid(
    uid: Uid,
    vararg allSearchResults: List<SearchResult>?,
    onSearchResultNoLongerExists: () -> Unit,
): SearchResult? {
    if (allSearchResults.any { it == null }) return null // Not all search results have loaded yet

    val searchResult = remember(uid, allSearchResults) {
        allSearchResults.firstNotNullOfOrNull { it!![uid] }
    }

    if (searchResult == null) { // For some reason the search result no longer exists
        onSearchResultNoLongerExists()
        return null
    }

    return searchResult
}

/**
 * Returns the [Widget] identified by [id] from the list of all widgets
 * * If the widgets are still loading, returns `null`
 * * If no widget matches [id], calls [onWidgetNoLongerExists] and returns `null`
 */
@OptIn(ExperimentalContracts::class)
@Composable
fun getWidgetById(
    id: WidgetId,
    widgets: Widgets?,
    onWidgetNoLongerExists: () -> Unit,
): Widget? {
    contract { returnsNotNull() implies (widgets != null) }

    if (widgets == null) return null // Not all widgets have loaded yet

    val widget = remember(id, widgets) { widgets.getById(id) }
    if (widget == null) { // For some reason the widget no longer exists
        onWidgetNoLongerExists()
        return null
    }

    return widget
}


@get:Composable
val Tag.name: String
    get() = when (this) {
        is Tag.Custom -> this.name
        is Tag.Builtin -> stringResource(
            when (this) {
                Tag.Builtin.GAME -> R.string.category_game
                Tag.Builtin.AUDIO -> R.string.category_audio
                Tag.Builtin.VIDEO -> R.string.category_video
                Tag.Builtin.IMAGE -> R.string.category_image
                Tag.Builtin.SOCIAL -> R.string.category_social
                Tag.Builtin.NEWS -> R.string.category_news
                Tag.Builtin.MAPS -> R.string.category_maps
                Tag.Builtin.PRODUCTIVITY -> R.string.category_productivity
                Tag.Builtin.ACCESSIBILITY -> R.string.category_accessibility
            }
        )
    }
