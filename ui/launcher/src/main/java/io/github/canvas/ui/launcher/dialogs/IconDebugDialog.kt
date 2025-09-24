package io.github.canvas.ui.launcher.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.Color.Companion.Unspecified
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.canvas.data.SearchResult
import io.github.canvas.data.icons.ColorBackground
import io.github.canvas.data.icons.ComposePainter
import io.github.canvas.data.icons.EmptyPainter
import io.github.canvas.ui.launcher.R
import io.github.canvas.ui.rememberAdaptiveIconPainter

private val iconSizeInt = 40.dp

@Composable
fun IconDebugDialog(
    searchResult: SearchResult,
    onClosed: () -> Unit,
) {
    @Composable
    fun Icon(label: String, icon: Painter) {
        Row(verticalAlignment = CenterVertically) {
            Text(label)
            Spacer(Modifier.weight(1f))
            androidx.compose.material3.Icon(
                icon,
                modifier = Modifier.size(iconSizeInt),
                tint = Unspecified,
                contentDescription = null
            )
        }
    }

    val icon = searchResult.icon

    AlertDialog(
        title = { Text(stringResource(R.string.icon_debug_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    "${stringResource(R.string.icon_debug_icon)} (${icon.javaClass.simpleName})",
                    rememberAdaptiveIconPainter(icon, monochrome = false)
                )
                icon.background?.let {
                    Icon(
                        "${stringResource(R.string.icon_debug_background)} (${it.javaClass.simpleName})",
                        rememberAdaptiveIconPainter(
                            icon.copy(foreground = ComposePainter(EmptyPainter)), monochrome = false
                        )
                    )
                }
                Icon(
                    "${stringResource(R.string.icon_debug_foreground)} (${icon.foreground.javaClass.simpleName})",
                    rememberAdaptiveIconPainter(
                        icon.copy(background = ColorBackground(Transparent)), monochrome = false
                    )
                )
                icon.monochrome?.let {
                    Icon(
                        "${stringResource(R.string.icon_debug_monochrome)} (${it.javaClass.simpleName})",
                        rememberAdaptiveIconPainter(
                            icon.copy(
                                background = ColorBackground(Transparent),
                                foreground = it
                            ), monochrome = false
                        )
                    )
                }
            }
        },
        onDismissRequest = onClosed,
        confirmButton = {
            TextButton(onClick = onClosed) {
                Text(stringResource(R.string.icon_debug_close))
            }
        }
    )
}
