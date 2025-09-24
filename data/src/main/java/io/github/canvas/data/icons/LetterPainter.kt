package io.github.canvas.data.icons

import android.graphics.Paint
import android.graphics.Paint.Align.CENTER
import android.graphics.Rect
import android.graphics.Typeface
import androidx.annotation.ColorInt
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Size.Companion.Unspecified
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb

/** Reusable instances to avoid new allocations */
private val paint by lazy {
    Paint().apply {
        setTypeface(Typeface.create("sans-serif", Typeface.NORMAL))
        textAlign = CENTER
        isAntiAlias = true
    }
}
private val rect = Rect()

/** A drawable that generates a contact icon based on the first letter of the contact name and the contact lookup key */
class LetterPainter(
    val letter: String,
    @ColorInt val color: Int,
) : Painter() {
    companion object {
        private const val SCALE: Float = 4f / 9f
    }

    constructor(letter: String, color: androidx.compose.ui.graphics.Color)
            : this(letter, color.toArgb())

    override fun DrawScope.onDraw() {
        // Draw letter tile.
        // Scale text by canvas bounds and user selected scaling factor
        paint.textSize = SCALE * size.minDimension
        paint.color = color
        paint.getTextBounds(letter, 0, letter.length, rect)

        // Draw the letter in the canvas
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                letter,
                center.x, center.y - rect.exactCenterY(),
                paint
            )
        }
    }

    override val intrinsicSize: Size get() = Unspecified
}
