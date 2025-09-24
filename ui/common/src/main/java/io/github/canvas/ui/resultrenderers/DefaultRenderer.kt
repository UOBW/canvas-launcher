package io.github.canvas.ui.resultrenderers

import android.app.ActivityOptions
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import io.github.canvas.data.SearchResult
import io.github.canvas.ui.R
import io.github.canvas.ui.SearchResultIcon
import io.github.canvas.ui.SimpleInteractionSource
import io.github.canvas.ui.searchResultHeight
import io.github.canvas.ui.searchResultLabel
import io.github.canvas.ui.verticalSearchResultPadding

/** Displays a search result using its icon and label, use [modifier] to make it clickable */
@Composable
fun DefaultRenderer(
    searchResult: SearchResult,
    modifier: Modifier,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    beforeClick: (() -> Unit)?,
    interactionSource: SimpleInteractionSource?,
    description: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current

    var iconPosition by remember { mutableStateOf(Offset.Unspecified) }
    var iconSize by remember { mutableStateOf(IntSize.Zero) }

    fun launch() {
        beforeClick?.invoke()

        if (onClick != null) {
            onClick()
        } else {
            searchResult.open(
                context,
                ActivityOptions.makeScaleUpAnimation(
                    view,
                    iconPosition.x.toInt(), iconPosition.y.toInt(),
                    iconSize.width, iconSize.height
                ).toBundle()
            )
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = verticalSearchResultPadding)
            .combinedClickable(
                onClick = ::launch,
                onLongClick = onLongClick,
                role = Role.Button,
                onClickLabel = stringResource(R.string.accessibility_open_search_result),
                onLongClickLabel = stringResource(R.string.accessibility_view_search_result_details)
            )
    ) {
        SearchResultIcon(
            searchResult = searchResult,
            onGloballyPositioned = {
                iconPosition = it.positionInRoot()
                iconSize = it.size
            },
            modifier = Modifier
                .padding(horizontal = verticalSearchResultPadding * 2)
                .size(searchResultHeight)
        )
        Column {
            Text(
                text = searchResultLabel(searchResult),
                color = if (searchResult.isError) MaterialTheme.colorScheme.error else Color.Unspecified,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )

            if (description != null) {
                Text(
                    text = description,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    LaunchedEffect(interactionSource) {
        interactionSource?.collect { launch() }
    }
}
