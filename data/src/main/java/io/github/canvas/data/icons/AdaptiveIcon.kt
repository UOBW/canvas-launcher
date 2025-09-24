package io.github.canvas.data.icons

import android.graphics.PixelFormat.OPAQUE
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES.O
import android.os.Build.VERSION_CODES.TIRAMISU
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.canvas.data.icons.AdaptiveIcon.Companion.SCALE_CROP
import io.github.canvas.data.icons.AdaptiveIcon.Companion.SCALE_FIT
import io.github.canvas.data.log

data class AdaptiveIcon(
    val foreground: AdaptiveIconForeground,
    val background: AdaptiveIconBackground?,
    val monochrome: AdaptiveIconForeground?,
    val foregroundScale: Float = 1f,
) {
    companion object {
        const val SCALE_FIT: Float = 0.45f
        const val SCALE_CROP: Float = 2f / 3f
    }
}

sealed interface AdaptiveIconForeground
sealed interface AdaptiveIconBackground

data class AndroidDrawable(
    val drawable: Drawable,
) : AdaptiveIconBackground, AdaptiveIconForeground

data class ComposePainter(
    val painter: Painter,
) : AdaptiveIconForeground

data class ResIdForeground(
    @DrawableRes val id: Int,
) : AdaptiveIconForeground

data class VectorForeground(
    val vector: ImageVector,
) : AdaptiveIconForeground

/** Prevent a dependency of :data on the material icons library */
data class CalendarIconForeground(val color: Color) : AdaptiveIconForeground

data class ColorBackground(
    val color: Color,
) : AdaptiveIconBackground

fun Drawable.toAdaptiveIcon(
    fallbackBackground: Color? = null,
): AdaptiveIcon {
    if (VERSION.SDK_INT > O && this is AdaptiveIconDrawable) {
        return AdaptiveIcon(
            foreground = this.foreground?.let { AndroidDrawable(it) }
                ?: MissingForeground.also { log.e("Icon $this has no foreground") },
            background = this.background?.let { AndroidDrawable(it) },
            monochrome = if (VERSION.SDK_INT >= TIRAMISU) this.monochrome?.let { AndroidDrawable(it) } else null
        )
    } else {
        @Suppress("DEPRECATION")
        return AdaptiveIcon(
            foreground = AndroidDrawable(this),
            background = fallbackBackground?.let { ColorBackground(it) },
            monochrome = null,
            foregroundScale = if (this.opacity == OPAQUE) SCALE_CROP else SCALE_FIT
        )
    }
}

val EmptyPainter: Painter = ColorPainter(Transparent)

val MissingForeground: AdaptiveIconForeground = ComposePainter(EmptyPainter)

val MissingIcon: AdaptiveIcon = AdaptiveIcon(
    foreground = MissingForeground,
    background = null,
    monochrome = null
)
