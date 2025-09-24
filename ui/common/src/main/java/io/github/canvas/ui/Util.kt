package io.github.canvas.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.core.content.getSystemService
import io.github.canvas.data.Logger
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.AT_MOST_ONCE
import kotlin.contracts.contract

internal val log: Logger = Logger("io.github.canvas.ui.common")

@OptIn(ExperimentalContracts::class)
inline fun Modifier.applyIf(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    contract {
        callsInPlace(modifier, AT_MOST_ONCE)
    }
    return if (condition) this.modifier() else this
}

fun DrawScope.draw(
    painter: Painter,
    size: Size,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
): Unit = with(painter) { draw(size, alpha, colorFilter) }

fun Context.copyToClipboard(text: String) {
    getSystemService<ClipboardManager>()!!.setPrimaryClip(ClipData.newPlainText(text, text))
    if (SDK_INT < VERSION_CODES.TIRAMISU) {
        Toast.makeText(this, R.string.toast_copied_to_clipboard, LENGTH_SHORT).show()
    }
}

fun Context.isDefaultLauncher(): Boolean = packageManager.resolveActivity(
    Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }, 0
)?.activityInfo?.packageName == packageName
