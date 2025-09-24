package io.github.canvas.ui.launcher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction.Companion.Search
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType.Companion.Text
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.canvas.data.SearchResult
import io.github.canvas.ui.SearchResultComposable
import io.github.canvas.ui.SimpleInteractionSource
import io.github.canvas.ui.fadingEdges

@Composable
fun SearchField(
    searchTerm: TextFieldValue,
    onSearchTermChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    onFocus: () -> Unit,
    onImeAction: KeyboardActionScope.() -> Unit,
) {
    BasicTextField(
        value = searchTerm,
        onValueChange = onSearchTermChange,
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) onFocus() },
        cursorBrush = SolidColor(LocalTextStyle.current.color),
        textStyle = LocalTextStyle.current,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            autoCorrectEnabled = false,
            keyboardType = Text,
            imeAction = Search,
            showKeyboardOnFocus = true
        ),
        keyboardActions = KeyboardActions(onImeAction)
    )
}

private object SearchResultContentType
private object SpacerTopContentType
private object SpacerBottomContentType

@Composable
private fun LazyItemScope.SearchResultListItem(
    searchResult: SearchResult,
    onShowDetailsSheet: (SearchResult) -> Unit,
    beforeLaunch: (() -> Unit)? = null,
    interactionSource: SimpleInteractionSource?,
) {
    SearchResultComposable(
        searchResult = searchResult,
        onLongClick = { onShowDetailsSheet(searchResult) },
        beforeClick = beforeLaunch,
        modifier = Modifier.animateItem(),
        interactionSource = interactionSource
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchResultsScreen(
    specialSearchResults: List<SearchResult>,
    searchResults: List<SearchResult>,
    externalSearchSearchResults: List<SearchResult>,
    onShowDetailsSheet: (SearchResult) -> Unit,
    /** Is optionally called before a search result is launched */
    beforeLaunch: (() -> Unit)? = null,
    listState: LazyListState,
    /** Should be passed to the first search result */
    firstResultInteractionSource: SimpleInteractionSource,
) {
    fun LazyListScope.items(searchResults: List<SearchResult>, isFirst: Boolean): Unit =
        itemsIndexed(
            items = searchResults,
            key = { _, r -> r.uid.string },
            contentType = { _, _ -> SearchResultContentType }
        ) { i, r ->
            SearchResultListItem(
                r, onShowDetailsSheet, beforeLaunch,
                interactionSource = if (isFirst && i == 0) firstResultInteractionSource else null
            )
        }

    LazyColumn(
        modifier = Modifier
            .fadingEdges(
                listState = listState,
                topFadeHeight = 10.dp,
                bottomFadeHeight = 0.dp
            )
            .imePadding(),
        state = listState
    ) {
        item(
            key = "hack",
            contentType = SpacerTopContentType
        ) { // A simple hack to scroll the list to the top when a result is inserted above the top result
            Spacer(Modifier.height(0.25.dp))
        }

        items(specialSearchResults, isFirst = true)
        items(searchResults, isFirst = specialSearchResults.isEmpty())
        items(
            externalSearchSearchResults,
            isFirst = specialSearchResults.isEmpty() && searchResults.isEmpty()
        )

        item(key = "spacer_bottom", contentType = SpacerBottomContentType) {
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
