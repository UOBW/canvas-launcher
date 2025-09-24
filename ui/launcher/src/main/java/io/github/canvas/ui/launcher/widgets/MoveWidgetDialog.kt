package io.github.canvas.ui.launcher.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow.Companion.Ellipsis
import io.github.canvas.data.widgets.Widget
import io.github.canvas.data.widgets.Widgets
import io.github.canvas.ui.launcher.R

/** A dialog to move a widget to a different page */
@Composable
fun MoveWidgetDialog(
    widget: Widget,
    widgets: Widgets,
    onMove: (newScreen: Int) -> Unit,
    onClosed: () -> Unit,
) {
    fun moveTo(page: Int) {
        onClosed()
        onMove(page)
    }

    AlertDialog(
        title = {
            Text(
                text = stringResource(R.string.move_widget_title, widget.name),
                maxLines = 1,
                overflow = Ellipsis
            )
        },
        text = {
            LazyColumn {
                item {
                    MoveToScreenButton(
                        page = widgets.indexRange.first - 1,
                        subtitle = stringResource(R.string.move_widget_add_page),
                        onClick = { moveTo(widgets.indexRange.first - 1) }
                    )
                }
                items(items = widgets.indexRange.toList()) { i ->
                    val page = widgets.page(i)
                    MoveToScreenButton(
                        page = i,
                        subtitle = when {
                            page.isEmpty() -> stringResource(R.string.move_widget_empty_page)
                            else -> page.joinToString { it.name }
                        },
                        onClick = { moveTo(i) },
                        isCurrentPage = widget.page == i
                    )
                }
                item {
                    MoveToScreenButton(
                        page = widgets.indexRange.last + 1,
                        subtitle = stringResource(R.string.move_widget_add_page),
                        onClick = { moveTo(widgets.indexRange.last + 1) }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onClosed) { Text(stringResource(R.string.move_widget_cancel)) } },
        onDismissRequest = onClosed,
    )
}

@Composable
private fun MoveToScreenButton(
    page: Int,
    subtitle: String,
    onClick: () -> Unit,
    isCurrentPage: Boolean = false,
) = ListItem(
    headlineContent = {
        Text(
            stringResource(
                if (isCurrentPage) R.string.move_widget_current_page_name else R.string.move_widget_page_name,
                page
            )
        )
    },
    supportingContent = { Text(subtitle) },
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    modifier = Modifier.clickable { onClick() }
)
