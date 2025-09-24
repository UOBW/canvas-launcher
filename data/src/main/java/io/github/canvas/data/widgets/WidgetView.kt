package io.github.canvas.data.widgets

/*
 * This file is based on a snippet shared by https://stackoverflow.com/users/1439522/prom85 on StackOverflow (https://stackoverflow.com/a/79514075/16038079)
 * Original code licensed under CC BY-SA 4.0 (https://creativecommons.org/licenses/by-sa/4.0/), changes made
 */

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.graphics.PointF
import android.util.TypedValue
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.ViewConfiguration.getLongPressTimeout
import kotlin.math.abs
import kotlin.math.roundToInt

class WidgetView(context: Context) : AppWidgetHostView(context) {
    private var hasPerformedLongPress: Boolean = false
    private var pointDown = PointF()

    private val pendingCheckForLongPress = object : Runnable {
        private var originalWindowAttachCount: Int = 0

        override fun run() {
            if (parent != null
                //    hasWindowFocus()
                && originalWindowAttachCount == windowAttachCount
                && !hasPerformedLongPress
            ) {
                onLongPress?.invoke()
                hasPerformedLongPress = true
            }
        }

        fun rememberWindowAttachCount() {
            originalWindowAttachCount = windowAttachCount
        }
    }

    //    private val threshold: Int = 5.dpToPx
    private val threshold: Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        5f,
        context.resources.displayMetrics
    ).roundToInt()

    var onLongPress: (() -> Unit)? = null

//    init {
//        // Hardware Acceleration - deactivate on android O or higher
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) setLayerType(LAYER_TYPE_SOFTWARE, null)
//    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Consume any touch events for ourselves after longpress is triggered
        if (hasPerformedLongPress) {
            hasPerformedLongPress = false
            return true
        }

        // Watch for long press events at this level to make sure
        // users can open the widget details sheet
        when (ev.action) { //TODO replace with actionMasked
            ACTION_DOWN -> {
                pointDown.set(ev.x, ev.y)
                // Used to be postCheckForLongClick()
                removeCallbacks(pendingCheckForLongPress)
                hasPerformedLongPress = false
                if (onLongPress != null) {
                    pendingCheckForLongPress.rememberWindowAttachCount()
                    postDelayed(pendingCheckForLongPress, getLongPressTimeout().toLong())
                }
            }

            ACTION_UP, ACTION_CANCEL -> {
                hasPerformedLongPress = false
                removeCallbacks(pendingCheckForLongPress)
            }

            ACTION_MOVE -> {
                val diffX = abs(pointDown.x - ev.x)
                val diffY = abs(pointDown.y - ev.y)
                if (diffX >= threshold || diffY >= threshold) {
                    hasPerformedLongPress = false
                    removeCallbacks(pendingCheckForLongPress)
                }
            }
        }

        // Otherwise, continue letting touch events fall through to children
        return false
    }

    override fun cancelLongPress() {
        super.cancelLongPress()
        hasPerformedLongPress = false
        removeCallbacks(pendingCheckForLongPress)
    }

    //override fun getDescendantFocusability() = ViewGroup.FOCUS_BLOCK_DESCENDANTS
}
