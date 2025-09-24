package io.github.canvas.data.calendar

import android.Manifest.permission.READ_CALENDAR
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars.VISIBLE
import androidx.core.database.getStringOrNull
import io.github.canvas.data.ContentResolverBackedRepository
import io.github.canvas.data.io
import io.github.canvas.data.log
import io.github.canvas.data.repositoryCoroutineScope
import io.github.canvas.data.settings.SettingsRepository
import io.github.canvas.data.toComposeColor
import io.github.canvas.data.useOnIo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class CalendarRepository internal constructor(
    private val contentResolver: ContentResolver,
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) : ContentResolverBackedRepository(contentResolver, uri = CalendarContract.CONTENT_URI) {
    private val coroutineScope = repositoryCoroutineScope()

    private val _events = MutableStateFlow<List<CalendarSearchResult>?>(value = null)
    val events: StateFlow<List<CalendarSearchResult>?> = _events.asStateFlow()

    private suspend fun reload(
        calendarSearchEnabled: Boolean,
    ) {
        if (!calendarSearchEnabled) {
            log.d("Not loading calendar events: disabled in settings")
            _events.value = emptyList()
            return
        }

        if (io { context.checkSelfPermission(READ_CALENDAR) } == PERMISSION_DENIED) {
            log.e("Error while loading calendar events: no permission")
            _events.value = emptyList() // No longer loading
            return
        }

        tryRegisterContentObserver()

        val calendarsCursor = io {
            @SuppressLint("Recycle")
            contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI, // Table to query
                arrayOf( // List of columns to return
                    CalendarContract.Calendars._ID,
                    CalendarContract.Calendars.CALENDAR_COLOR,
                    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
                ),
                "${VISIBLE}=1", // Filter
                null, // Arguments to the filter
                null // Sort
            )
        }

        if (calendarsCursor == null) {
            log.e("Failed to load calendar list: query() returned null")
            return
        }

        val results = mutableListOf<CalendarSearchResult>()
        val now = System.currentTimeMillis()

        calendarsCursor.useOnIo {
            val calendarIdIndex =
                calendarsCursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val calendarColorIndex =
                calendarsCursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)

            while (calendarsCursor.moveToNext()) {
                val calendarId = calendarsCursor.getLong(calendarIdIndex)
                val calendarColor = calendarsCursor.getInt(calendarColorIndex).toComposeColor()

                val eventsCursor = contentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    arrayOf(
                        // columns to query
                        CalendarContract.Events._ID,
                        CalendarContract.Events.TITLE,
                        CalendarContract.Events.EVENT_LOCATION,
                        CalendarContract.Events.DTSTART,
                        CalendarContract.Events.DTEND,
                        CalendarContract.Events.ALL_DAY,
                        CalendarContract.Events.RRULE,
                        CalendarContract.Events.RDATE,
                    ),
                    // Only get recurring or future events
                    "${CalendarContract.Events.CALENDAR_ID}=? AND (${CalendarContract.Events.DTEND}>=? OR ${CalendarContract.Events.RRULE} NOT NULL OR ${CalendarContract.Events.RDATE} NOT NULL)", // filter
                    arrayOf("$calendarId", "$now"), // filter args
                    null // sort
                )

                if (eventsCursor == null) {
                    log.e("Failed to load events from calendar $calendarId: query() return null")
                    continue
                }

                eventsCursor.use {
                    val eventIdIndex =
                        eventsCursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
                    val titleIndex =
                        eventsCursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                    val startIndex =
                        eventsCursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                    val endIndex = eventsCursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
                    val locationIndex =
                        eventsCursor.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION)
                    val rRuleIndex =
                        eventsCursor.getColumnIndexOrThrow(CalendarContract.Events.RRULE)
                    val rDateIndex =
                        eventsCursor.getColumnIndexOrThrow(CalendarContract.Events.RDATE)
                    val allDayIndex =
                        eventsCursor.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)

                    while (eventsCursor.moveToNext()) {
                        val isRecurring =
                            !eventsCursor.isNull(rRuleIndex) || !eventsCursor.isNull(rDateIndex)
                        results += CalendarSearchResult(
                            eventId = eventsCursor.getLong(eventIdIndex),
                            label = eventsCursor.getString(titleIndex),
                            color = calendarColor,
                            startTime = if (!isRecurring) eventsCursor.getLong(startIndex) else null,
                            endTime = if (!isRecurring) eventsCursor.getLong(endIndex) else null,
                            isAllDay = eventsCursor.getInt(allDayIndex) > 0,
                            location = eventsCursor.getStringOrNull(locationIndex)
                        )
                    }
                }
            }
        }

        _events.value = results.sortedWith( // sort by start time, recurring last
            compareBy<CalendarSearchResult> { it.isRecurring }
                .thenBy { it.startTime }
        )
        log.d("Calendar events reloaded: ${results.size} events")
    }

    init {
        coroutineScope.launch {
            combine(
                contentObserverListener,
                settingsRepository.settings.map { it.calendarSearchEnabled }.distinctUntilChanged()
            ) { _, calendarSearchEnabled ->
                calendarSearchEnabled
            }.collectLatest { calendarSearchEnabled ->
                reload(calendarSearchEnabled)
            }
        }
        log.d("Calendar repository initialized")
    }
}
