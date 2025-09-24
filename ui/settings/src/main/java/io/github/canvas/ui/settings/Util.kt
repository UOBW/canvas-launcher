package io.github.canvas.ui.settings

import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import io.github.canvas.data.Logger

val log: Logger = Logger("io.github.canvas.ui.settings")

@Composable
fun rawStringResource(@RawRes id: Int): String {
    val resources = LocalResources.current
    return remember(id, resources) {
        resources.openRawResource(id).bufferedReader().use { it.readText() }
    }
}
