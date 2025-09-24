package io.github.canvas.data.calendar

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import androidx.compose.ui.graphics.Color
import io.github.canvas.data.CustomRendererSearchResult
import io.github.canvas.data.SearchResult
import io.github.canvas.data.StringLabelSearchResult
import io.github.canvas.data.Uid
import io.github.canvas.data.icons.AdaptiveIcon
import io.github.canvas.data.icons.AdaptiveIcon.Companion.SCALE_FIT
import io.github.canvas.data.icons.CalendarIconForeground

data class CalendarSearchResult(
    val eventId: Long,
    val color: Color,
    override val label: String,
    /** millis since epoch, null if recurring */
    val startTime: Long?,
    /** millis since epoch, null if recurring */
    val endTime: Long?,
    val isAllDay: Boolean,
    val location: String?,
) : SearchResult, StringLabelSearchResult, CustomRendererSearchResult {
    override val uid: Uid = Uid("calendar/$eventId")

    override val icon: AdaptiveIcon = AdaptiveIcon(
        foreground = CalendarIconForeground(color),
        foregroundScale = SCALE_FIT,
        background = null,
        monochrome = null
    )

    override val searchTokens: List<String> = label.split(' ')

    override fun open(context: Context, options: Bundle) {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent, options)
    }
}

val CalendarSearchResult.isRecurring: Boolean get() = startTime == null
