package io.github.canvas.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_DATE_CHANGED
import android.content.Intent.ACTION_TIMEZONE_CHANGED
import android.content.Intent.ACTION_TIME_CHANGED
import android.content.IntentFilter
import android.icu.util.Calendar
import android.icu.util.Calendar.DAY_OF_MONTH
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.RECEIVER_EXPORTED
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import java.util.Date

/** A simple repository to get and listen for changes in the current day of the month */
class DateRepository internal constructor(
    /** Required to listen to date changes via a broadcast receiver */
    private val context: Context,
) {
    private val coroutineScope = repositoryCoroutineScope()

    val dayOfMonth: StateFlow<Int> = callbackFlow<Int> {
        val calendar = Calendar.getInstance()

        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                calendar.time = Date() // now
                trySend(calendar.get(DAY_OF_MONTH))
                log.d("Day of month changed")
            }
        }

        val filter = IntentFilter().apply {
            addAction(ACTION_DATE_CHANGED)
            addAction(ACTION_TIME_CHANGED)
            addAction(ACTION_TIMEZONE_CHANGED)
        }

        ContextCompat.registerReceiver(context, broadcastReceiver, filter, RECEIVER_EXPORTED)

        // Initial value
        calendar.time = Date() // now
        trySend(calendar.get(DAY_OF_MONTH))

        awaitClose { context.unregisterReceiver(broadcastReceiver) }
    }.stateIn(coroutineScope, started = WhileSubscribed(), initialValue = 31)
}
