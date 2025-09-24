package io.github.canvas.ui.launcher.widgets

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_SIZES
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.canvas.data.widgets.Widget
import io.github.canvas.data.widgets.WidgetView
import io.github.canvas.data.widgets.Widgets
import io.github.canvas.ui.launcher.CustomIndexHorizontalPager
import io.github.canvas.ui.launcher.R
import io.github.canvas.ui.launcher.int

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WidgetScreen(
    widgets: Widgets,
    loadWidgetView: suspend (Widget) -> AppWidgetHostView?,
    onShowWidgetDetailsSheet: (Widget) -> Unit,
    onShowAddWidgetSheet: () -> Unit,
) {
    if (widgets.isEmpty()) {
        Box(
            Modifier
                .fillMaxSize()
                .combinedClickable(
                    onLongClick = onShowAddWidgetSheet,
                    onClick = {}
                )
        ) {
            Text(
                stringResource(R.string.empty_widgets_screen),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Center)
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
    } else {
        CustomIndexHorizontalPager(
            indexRange = widgets.indexRange,
            modifier = Modifier.navigationBarsPadding()
        ) { pageIndex ->
            val page: List<Widget> = widgets.page(pageIndex)
            if (page.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    for (widget in page) {
                        Widget(
                            widget,
                            loadWidgetView = { loadWidgetView(widget) },
                            onShowWidgetDetailsSheet = { onShowWidgetDetailsSheet(widget) }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

// The docs say to use android.R.dimen.system_app_widget_background_radius, but that seems too rounded and even the stock launcher, at least on my phone, ignores it
val widgetCornerSize: CornerSize = CornerSize(16.dp)

@Composable
private fun ColumnScope.Widget(
    widget: Widget,
    loadWidgetView: suspend () -> AppWidgetHostView?,
    onShowWidgetDetailsSheet: () -> Unit,
) {
    key(widget.id) { //Don't reuse AndroidViews for different widgets
        val view: AppWidgetHostView? by produceState(initialValue = null) {
            value = loadWidgetView()
        }

        if (view == null) { // Widget is temporarily unavailable, fill up the space it would take up to avoid layout shifts
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .weight(widget.verticalWeight)
            )
        } else {
            val context = LocalContext.current
            val clipShape = remember {
                PaddedRoundedCornerShape(
                    cornerSize = widgetCornerSize,
                    padding = AppWidgetHostView.getDefaultPaddingForWidget(
                        context, view!!.appWidgetInfo.provider, null
                    ).toComposeRect()
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(widget.verticalWeight)
                    .clip(clipShape)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { view!! },
                    update = { widgetView ->
                        widgetView.updateAppWidgetOptions(Bundle().apply {
                            putInt(OPTION_APPWIDGET_MIN_WIDTH, this@BoxWithConstraints.maxWidth.int)
                            putInt(OPTION_APPWIDGET_MAX_WIDTH, this@BoxWithConstraints.maxWidth.int)
                            putInt(
                                OPTION_APPWIDGET_MIN_HEIGHT,
                                this@BoxWithConstraints.maxHeight.int
                            )
                            putInt(
                                OPTION_APPWIDGET_MAX_HEIGHT,
                                this@BoxWithConstraints.maxHeight.int
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                putParcelableArrayList(
                                    OPTION_APPWIDGET_SIZES,
                                    arrayListOf(
                                        SizeF(
                                            /* width = */ this@BoxWithConstraints.maxWidth.value,
                                            /* height = */ this@BoxWithConstraints.maxHeight.value
                                        )
                                    )
                                )
                            }
                        })

                        (view as? WidgetView)?.onLongPress = onShowWidgetDetailsSheet
                    }
                )
            }
        }
    }
}

private fun Rect.addPadding(padding: Rect): Rect = Rect(
    left = this.left + padding.left,
    top = this.top + padding.top,
    right = this.right - padding.right,
    bottom = this.bottom - padding.bottom
)

data class PaddedRoundedCornerShape(
    val cornerSize: CornerSize,
    val padding: Rect,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val actualSize = size.toRect().addPadding(padding)
        return Outline.Rounded(
            RoundRect(
                actualSize,
                CornerRadius(cornerSize.toPx(actualSize.size, density))
            )
        )
    }
}
