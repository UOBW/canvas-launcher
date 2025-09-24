package io.github.canvas.ui.launcher.widgets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow.Companion.Ellipsis
import androidx.compose.ui.unit.dp
import io.github.canvas.data.widgets.Widget
import io.github.canvas.data.widgets.WidgetId
import io.github.canvas.data.widgets.Widgets
import io.github.canvas.ui.launcher.NumberInputValue
import io.github.canvas.ui.launcher.OutlinedNumberInput
import io.github.canvas.ui.launcher.R
import io.github.canvas.ui.launcher.log
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReorderWidgetsDialog(
    widgets: Widgets,
    page: Int,
    onReorder: (reorderedWidgets: List<WidgetId>, updatedWeights: Map<WidgetId, Float>) -> Unit,
    onClosed: () -> Unit,
) {
    if (page !in widgets.indexRange) { // The dialog was probably just opened before a widget was removed
        log.e("Failed to display ReorderWidgetsDialog: invalid page $page")
        onClosed()
        return
    }
    // widgets and page should never change while in the dialog, but if they do, reset the whole dialog
    key(widgets, page) {
        val reorderedWidgets = rememberSaveable(saver = WidgetIdListSaver) {
            widgets.page(page).map { it.id }.toMutableStateList()
        }
        val weights = rememberSaveable(saver = NumberInputValueMapSaver) {
            widgets.page(page)
                .map { it.id to NumberInputValue(it.verticalWeight) }
                .toMutableStateMap<WidgetId, NumberInputValue>()
        }

        AlertDialog(
            title = { Text(stringResource(R.string.reorder_widgets_title)) },
            text = {
                val listState = rememberLazyListState()
                val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
                    reorderedWidgets.apply { add(to.index, removeAt(from.index)) }
                }

                LazyColumn(
                    state = listState,
                    verticalArrangement = spacedBy(16.dp)
                ) {
                    items(items = reorderedWidgets, key = { it.value }) { widgetId ->
                        ReorderableItem(state = reorderableState, key = widgetId.value) {
                            WidgetItem(
                                widget = widgets.getById(widgetId)!!,
                                weight = weights[widgetId]!!,
                                onWeightChanged = { weights[widgetId] = it }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClosed()
                        onReorder(
                            reorderedWidgets.toList(), // Create a copy
                            weights.mapValues { it.value.number!! }
                        )
                    },
                    enabled = weights.all { it.value.number != null }
                ) {
                    Text(stringResource(R.string.reorder_widgets_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onClosed) { Text(stringResource(R.string.reorder_widgets_cancel)) }
            },
            onDismissRequest = onClosed,
        )
    }
}

@Composable
private fun ReorderableCollectionItemScope.WidgetItem(
    widget: Widget,
    weight: NumberInputValue,
    onWeightChanged: (NumberInputValue) -> Unit,
) {
    val defaultStyle = LocalTextStyle.current
    ListItem(
        modifier = Modifier.draggableHandle(),
        headlineContent = {
            Text(
                text = widget.name,
                overflow = Ellipsis,
                maxLines = 1,
            )
        },
        trailingContent = {
            Row(
                verticalAlignment = CenterVertically,
                horizontalArrangement = spacedBy(16.dp)
            ) {
                CompositionLocalProvider(LocalTextStyle provides defaultStyle) { // Don't use the style provided by ListItem
                    OutlinedNumberInput(
                        value = weight,
                        onValueChange = onWeightChanged,
                        modifier = Modifier.width(72.dp),
                        label = { Text(stringResource(R.string.reorder_widgets_label_weight)) }
                    )
                }
                Icon(
                    Icons.Default.DragHandle,
                    modifier = Modifier.size(24.dp),
                    contentDescription = stringResource(R.string.reorder_widgets_accessibility_drag_handle)
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = AlertDialogDefaults.containerColor)
    )
}

private object WidgetIdListSaver :
    androidx.compose.runtime.saveable.Saver<SnapshotStateList<WidgetId>, List<Int>> {
    override fun SaverScope.save(value: SnapshotStateList<WidgetId>): List<Int>? =
        value.map { it.value }

    override fun restore(value: List<Int>): SnapshotStateList<WidgetId>? =
        value.map { WidgetId(it) }.toMutableStateList()
}

private object NumberInputValueMapSaver :
    androidx.compose.runtime.saveable.Saver<SnapshotStateMap<WidgetId, NumberInputValue>, Map<Int, String>> {
    override fun SaverScope.save(value: SnapshotStateMap<WidgetId, NumberInputValue>): Map<Int, String> =
        value.mapKeys { it.key.value }.mapValues { it.value.text }

    override fun restore(value: Map<Int, String>): SnapshotStateMap<WidgetId, NumberInputValue> =
        value.map { (k, v) -> WidgetId(k) to NumberInputValue(v) }.toMutableStateMap()
}
