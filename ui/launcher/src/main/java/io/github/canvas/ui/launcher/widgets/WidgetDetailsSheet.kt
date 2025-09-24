package io.github.canvas.ui.launcher.widgets

import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Window
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.canvas.data.widgets.Widget
import io.github.canvas.ui.AsyncIcon
import io.github.canvas.ui.launcher.DetailsSheet
import io.github.canvas.ui.launcher.DetailsSheetButton
import io.github.canvas.ui.launcher.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetDetailsSheet(
    widget: Widget,
    showReorderButton: Boolean,
    onReorderWidgets: (page: Int) -> Unit,
    onRemoveWidget: () -> Unit,
    onReconfigureWidget: () -> Unit,
    onMoveWidget: () -> Unit,
    onAddNewWidget: () -> Unit,
    loadWidgetIcon: suspend () -> Drawable,
    onClosed: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()

    fun closeNoAnimation() = onClosed()
    suspend fun closeAnimated() {
        sheetState.hide()
        onClosed()
    }

    DetailsSheet(
        onDismissRequest = onClosed,
        icon = { AsyncIcon(loadWidgetIcon) },
        title = { Text(widget.name) },
        sheetState = sheetState,
    ) {
        if (widget.reconfigurationActivity != null) {
            DetailsSheetButton(
                icon = Icons.Default.Settings,
                text = stringResource(R.string.widget_details_reconfigure),
                onClick = {
                    onReconfigureWidget()
                    closeNoAnimation()
                }
            )
        }

        if (showReorderButton) {
            DetailsSheetButton(
                icon = Icons.Default.Reorder,
                text = stringResource(R.string.widget_details_reorder),
                onClick = {
                    coroutineScope.launch {
                        onReorderWidgets(widget.page)
                        closeAnimated()
                    }
                }
            )
        }

        DetailsSheetButton(
            icon = Icons.Default.Window,
            text = stringResource(R.string.widget_details_move),
            onClick = {
                coroutineScope.launch {
                    onMoveWidget()
                    closeAnimated()
                }
            }
        )

        DetailsSheetButton(
            icon = Icons.Default.Clear,
            text = stringResource(R.string.widget_details_remove),
            onClick = {
                onRemoveWidget()
                coroutineScope.launch { closeAnimated() }
            }
        )

        HorizontalDivider()

        DetailsSheetButton(
            icon = Icons.Default.Add,
            text = stringResource(R.string.widget_details_add_widget),
            onClick = {
                coroutineScope.launch {
                    closeAnimated()
                    onAddNewWidget()
                }
            }
        )

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
