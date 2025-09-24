package io.github.canvas.ui.launcher.widgets

import android.widget.RemoteViews
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role.Companion.Button
import androidx.compose.ui.text.style.TextOverflow.Companion.Ellipsis
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.github.canvas.data.widgets.AvailableWidget
import io.github.canvas.data.widgets.WidgetsRepository.App
import io.github.canvas.ui.AdaptiveIconComposable
import io.github.canvas.ui.launcher.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleAppAddWidgetSheet(
    widgets: List<AvailableWidget>,
    onClosed: () -> Unit,
    onAddWidget: (widget: AvailableWidget) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onClosed,
        sheetState = sheetState,
    ) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 8.dp),
//            verticalArrangement = Arrangement.spacedBy(50.dp)
        ) {
            itemsIndexed(widgets) { index, widget ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            role = Button,
                            onClickLabel = stringResource(R.string.add_widget_sheet_accessibility_add_widget)
                        ) {
                            onAddWidget(widget)
                            coroutineScope.launch {
                                sheetState.hide()
                                onClosed()
                            }
                        },
                    horizontalAlignment = CenterHorizontally
                ) {
                    WidgetPreview(widget)
                    Text(widget.label, style = MaterialTheme.typography.titleLarge)
                    widget.description?.let { Text(it) }
                }

                if (index != widgets.lastIndex) {
                    HorizontalDivider(Modifier.padding(top = 10.dp, bottom = 20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllAppsAddWidgetSheet(
    widgets: List<Pair<App, List<AvailableWidget>>>,
    onClosed: () -> Unit,
    onAddWidget: (widget: AvailableWidget) -> Unit,
) {
    var expanded: App? by remember { mutableStateOf(null) }

    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onClosed,
        sheetState = sheetState,
    ) {
        LazyColumn(modifier = Modifier.padding(horizontal = 8.dp)) {
            itemsIndexed(widgets) { index, (app, appWidgets) ->
                ListItem(
                    leadingContent = {
                        AdaptiveIconComposable(
                            icon = app.icon,
                            modifier = Modifier.size(40.dp)
                        )
                    },
                    headlineContent = {
                        Text(app.name, style = MaterialTheme.typography.titleLarge)
                    },
                    supportingContent = {
                        Text(
                            text = appWidgets
                                .filter { it.label != app.name }
                                .takeIf { it.isNotEmpty() }
                                ?.joinToString { it.label }
                                ?: pluralStringResource(
                                    R.plurals.add_widget_sheet_generic_description,
                                    appWidgets.size, appWidgets.size
                                ),
                            maxLines = 1, overflow = Ellipsis
                        )
                    },
                    trailingContent = {
                        Crossfade(expanded) { expanded ->
                            Icon(
                                if (expanded === app) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.clickable(
                        role = Button,
                        onClickLabel = stringResource(if (expanded === app) R.string.accessibility_collapse else R.string.accessibility_expand)
                    ) {
                        expanded = if (expanded === app) null else app
                    },
                    colors = ListItemDefaults.colors(containerColor = Transparent)
                )

                AnimatedVisibility(visible = expanded === app) {
                    Column(verticalArrangement = spacedBy(16.dp)) {
                        for (widget in appWidgets) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        role = Button,
                                        onClickLabel = stringResource(R.string.add_widget_sheet_accessibility_add_widget)
                                    ) {
                                        onAddWidget(widget)
                                        coroutineScope.launch {
                                            sheetState.hide()
                                            onClosed()
                                        }
                                    },
                                horizontalAlignment = CenterHorizontally,
                            ) {
                                WidgetPreview(widget)
                                if (widget.label != app.name) { // Don't show the title if its the same as the app's name
                                    Text(widget.label, style = MaterialTheme.typography.titleMedium)
                                }
                                widget.description?.let {
                                    Text(it, style = MaterialTheme.typography.titleSmall)
                                }
//                                if (index != appWidgets.lastIndex) HorizontalDivider()
                            }
                        }
                    }
                }

                if (index != widgets.lastIndex) HorizontalDivider()
            }
        }
    }
}

@Composable
private fun WidgetPreview(
    widget: AvailableWidget,
) {
    val context = LocalContext.current

    when {
        widget.previewLayout != 0 -> AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp, max = 200.dp)
                .clip(RoundedCornerShape(widgetCornerSize)),
            factory = {
                RemoteViews(widget.provider.packageName, widget.previewLayout)
                    .apply(context, null) // Passing null seems to work
            }
        )

        widget.previewImage != null -> Image(
            painter = rememberDrawablePainter(widget.previewImage),
            modifier = Modifier
                .heightIn(min = 100.dp, max = 200.dp)
                .padding(16.dp)
                .clip(RoundedCornerShape(widgetCornerSize)),
            contentDescription = stringResource(R.string.add_widget_sheet_accessibility_widget_preview)
        )

        else -> error("Either previewLayout or previewImage must be not null/0")
    }
}
