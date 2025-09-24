package io.github.canvas.ui

import android.graphics.Matrix
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Size.Companion.Unspecified
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import com.google.accompanist.drawablepainter.DrawablePainter
import io.github.canvas.data.icons.AdaptiveIcon
import io.github.canvas.data.icons.AndroidDrawable
import io.github.canvas.data.icons.CalendarIconForeground
import io.github.canvas.data.icons.ColorBackground
import io.github.canvas.data.icons.ComposePainter
import io.github.canvas.data.icons.ResIdForeground
import io.github.canvas.data.icons.VectorForeground
import io.github.canvas.data.settings.Settings.MonochromeIconColors
import io.github.canvas.data.settings.Settings.MonochromeIconColors.BLACK
import io.github.canvas.data.settings.Settings.MonochromeIconColors.PRIMARY
import io.github.canvas.data.settings.Settings.MonochromeIconColors.SECONDARY
import io.github.canvas.data.settings.Settings.MonochromeIconColors.TERTIARY
import io.github.canvas.data.settings.Settings.MonochromeIconColors.WHITE

@Suppress("FunctionName")
private fun ComposePath() = Path()
private typealias AndroidPath = android.graphics.Path

@Composable
fun rememberAdaptiveIconPainter(
    icon: AdaptiveIcon,
    monochrome: Boolean,
): Painter =
    rememberAdaptiveIconPainter(icon, if (monochrome) LocalMonochromeIconColors.current else null)

@Composable
fun rememberAdaptiveIconPainter(
    icon: AdaptiveIcon,
    monochromeColors: MonochromeIconColors? = LocalMonochromeIconColors.current,
): Painter {
    val unscaledMask: AndroidPath? = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AdaptiveIconDrawable(null, null).iconMask
        } else null
    }

    val monochrome = monochromeColors != null
    val (monochromeBackground, monochromeForeground) = when (monochromeColors) {
        null -> null to null
        PRIMARY -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        SECONDARY -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
        TERTIARY -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        BLACK -> Color.White to Color.Black
        WHITE -> Color.Black to Color.White
    }

    // Has to be done outside remember to be able to call composable functions
    val foreground = icon.monochrome?.takeIf { monochrome } ?: icon.foreground
    val rememberedPainter = when (foreground) {
        is ResIdForeground -> painterResource(foreground.id)
        is VectorForeground -> rememberVectorPainter(foreground.vector)
        is CalendarIconForeground -> rememberVectorPainter(Icons.Default.CalendarMonth)
        else -> null
    }

    return remember(icon, monochromeColors) {
        val foreground: Painter = when (foreground) {
            is AndroidDrawable -> DrawablePainter(foreground.drawable.apply {
                colorFilter = null // Reset any previously applied tint
            })

            is ComposePainter -> foreground.painter
            is ResIdForeground, is VectorForeground, is CalendarIconForeground -> rememberedPainter!!
        }

        val background: Painter? = when {
            monochromeBackground != null -> ColorPainter(monochromeBackground)
            else -> when (val background = icon.background) {
                is AndroidDrawable -> DrawablePainter(background.drawable)
                is ColorBackground -> ColorPainter(background.color)
                null -> null
            }
        }

        val colorFilter = monochromeForeground?.let { ColorFilter.tint(it) }
            ?: (icon.foreground as? CalendarIconForeground)?.color?.let { ColorFilter.tint(it) }

        return@remember AdaptiveIconPainter(
            background, foreground,
            unscaledMask, icon.foregroundScale,
            colorFilter
        )
    }
}

private class AdaptiveIconPainter(
    val background: Painter?,
    val foreground: Painter,
    val unscaledMask: AndroidPath?,
    val foregroundScale: Float,
    val colorFilter: ColorFilter?,
) : Painter() {
    companion object {
        const val MASK_SIZE = 100f
        val defaultBackground = Color.White
    }

    // Reuse instances
    val path: AndroidPath = AndroidPath()
    val matrix: Matrix = Matrix()

    override fun DrawScope.onDraw() {
        val shape = if (unscaledMask != null) {
            matrix.setScale(size.width / MASK_SIZE, size.height / MASK_SIZE)
            unscaledMask.transform(matrix, path)
            path.asComposePath()
        } else {
            ComposePath().apply { addOval(size.toRect()) }
        }

        clipPath(shape) {
            drawRect(defaultBackground)
            scale(1.5f) {
                if (background != null) draw(background, size)
                scale(foregroundScale) {
                    draw(foreground, size, colorFilter = colorFilter)
                }
            }
        }
    }

    override val intrinsicSize: Size get() = Unspecified
}
