@file:OptIn(ExperimentalTime::class)

package io.github.canvas.ui.resultrenderers

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import io.github.canvas.data.calendar.CalendarSearchResult
import io.github.canvas.ui.R
import io.github.canvas.ui.SimpleInteractionSource
import java.util.Formatter
import kotlin.time.ExperimentalTime

@Composable
fun CalendarRenderer(
    event: CalendarSearchResult,
    modifier: Modifier,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    beforeClick: (() -> Unit)?,
    interactionSource: SimpleInteractionSource?,
) {
    val resources = LocalResources.current
    val context = LocalContext.current

    val description = remember(resources) {
        val time = if (event.startTime == null) {
            resources.getString(R.string.event_description_time_recurring)
        } else {
            DateUtils.formatDateRange(
                context, Formatter(),
                event.startTime!!, event.endTime!!,
                DateUtils.FORMAT_SHOW_DATE or (if (!event.isAllDay) DateUtils.FORMAT_SHOW_TIME else 0) or DateUtils.FORMAT_NO_NOON or DateUtils.FORMAT_NO_MIDNIGHT or DateUtils.FORMAT_ABBREV_TIME or DateUtils.FORMAT_NUMERIC_DATE,
                if (event.isAllDay) "UTC" else null // all day events are stored in UTC
            ).toString()
        }

        if (event.location != null) {
            resources.getString(R.string.event_description, time, event.location!!)
        } else time
    }
    DefaultRenderer(
        event, modifier, onClick, onLongClick, beforeClick, interactionSource, description
    )
}
