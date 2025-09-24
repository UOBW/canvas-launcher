package io.github.canvas.data.widgets

import android.content.ComponentName
import androidx.compose.runtime.saveable.SaverScope
import java.util.SortedMap

data class Widget(
    val id: WidgetId,
    /** The page the widget is displayed on. 0 is the default page, -1, -2, … are to the left and 1, 2, … are to the right. */
    val page: Int,
    /** The vertical position of the widget */
    val position: Int,
    /** The vertical size of the widget, works similar to the compose weight() modifier */
    val verticalWeight: Float,
    /** The activity to reconfigure the widget, or null if not supported */
    val reconfigurationActivity: ComponentName?,
    /** True if swiping on the widget should scroll the widget, not the VerticalPager */
    val isScrollable: Boolean,
    /** A human-readable name for the widget */
    val name: String,
) {
    override fun toString(): String = "widget/$id ($name)"
}

/** Uniquely identifies a widget. See [WidgetHost.allocateAppWidgetId] */
@JvmInline
value class WidgetId(val value: Int) {
    override fun toString(): String = "$value"

    object Saver : androidx.compose.runtime.saveable.Saver<WidgetId?, Any> {
        override fun SaverScope.save(value: WidgetId?): Any = value?.value ?: false
        override fun restore(value: Any): WidgetId? = if (value is Int) WidgetId(value) else null
    }
}

/**
 * A mapping of page id to widgets on that page.
 * * Is guaranteed to be sorted by page
 * * get() never returns null, it returns an empty list instead
 * * Use indexRange to get the range of pages that should be displayed
 */
data class Widgets(
    private val widgets: SortedMap<Int, out List<Widget>>,
) {
    val indexRange: IntRange = when {
        widgets.isEmpty() -> 0..0 // Prevent NoSuchElementException
        else -> widgets.keys.first().coerceAtMost(0)..
                widgets.keys.last().coerceAtLeast(0)
    }

    fun pageOrNull(i: Int): List<Widget>? = if (i in indexRange) {
        widgets[i] ?: emptyList() // Empty screens don't have an entry in widgets
    } else null

    fun page(i: Int): List<Widget> = pageOrNull(i) ?: throw NoSuchElementException()

    fun getById(id: WidgetId): Widget? {
        for ((_, page) in widgets) page.find { it.id == id }?.let { return it }
        return null
    }

    fun isEmpty(): Boolean = widgets.isEmpty()

    val initialPage: List<Widget> get() = page(0)

    companion object {
        val EMPTY: Widgets = Widgets(sortedMapOf())
    }
}

fun Widgets?.orEmpty(): Widgets = this ?: Widgets.EMPTY
