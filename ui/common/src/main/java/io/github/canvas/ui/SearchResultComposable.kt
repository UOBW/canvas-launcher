package io.github.canvas.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.canvas.data.CustomRendererSearchResult
import io.github.canvas.data.SearchResult
import io.github.canvas.data.calendar.CalendarSearchResult
import io.github.canvas.ui.resultrenderers.CalendarRenderer
import io.github.canvas.ui.resultrenderers.DefaultRenderer
import io.github.canvas.ui.resultrenderers.MetronomeRenderer

// Search result height is 40dp with 4dp padding above and below, leading to a total height of 48dp
val searchResultHeight: Dp = 40.dp
val verticalSearchResultPadding: Dp = 4.dp

@Composable
fun SearchResultComposable(
    searchResult: SearchResult,
    modifier: Modifier = Modifier,
    /** Optional, defaults to opening the result */
    onClick: (() -> Unit)? = null,
    /** Optional, defaults to do nothing */
    onLongClick: (() -> Unit)? = null,
    /** Optional, called before [onClick], but does not override the default action */
    beforeClick: (() -> Unit)? = null,
    interactionSource: SimpleInteractionSource? = null,
): Unit = when (searchResult) {
    is CustomRendererSearchResult -> when (searchResult) {
        is MetronomeSearchResult -> MetronomeRenderer(
            searchResult, modifier, onClick, onLongClick, beforeClick, interactionSource
        )

        is CalendarSearchResult -> CalendarRenderer(
            searchResult, modifier, onClick, onLongClick, beforeClick, interactionSource
        )

        else -> error("Unable to find renderer for search result $searchResult")
    }

    else -> DefaultRenderer(
        searchResult, modifier, onClick, onLongClick, beforeClick, interactionSource
    )
}
