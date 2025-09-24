package io.github.canvas.data.widgets

import android.content.ComponentName
import android.graphics.drawable.Drawable

/** A widget that is not yet added to the launcher, but can be added */
data class AvailableWidget(
    val provider: ComponentName,
    val label: String,
    val description: String?,
    /** One of either previewImage or previewLayout is guaranteed to be not null/0 */
    val previewImage: Drawable?,
    val previewLayout: Int,
)
