package io.github.canvas.data.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context

private const val HOST_ID = 0

internal class WidgetHost(context: Context) : AppWidgetHost(context, HOST_ID) {
    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?,
    ): AppWidgetHostView = WidgetView(context)
}
