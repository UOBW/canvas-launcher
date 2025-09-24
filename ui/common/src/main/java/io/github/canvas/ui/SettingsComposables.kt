package io.github.canvas.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.Role.Companion.RadioButton
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import io.github.canvas.data.icons.AdaptiveIcon
import kotlin.enums.EnumEntries
import kotlin.enums.enumEntries

/** A button that takes you to a different screen in the settings */
@Composable
fun BasicSettingsItem(
    title: String,
    description: String? = null,
    icon: (@Composable (enabled: Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    BasicSetting(
        onClick = onClick,
        title = title,
        description = description,
        icon = icon,
        enabled = enabled
    )
}

@Suppress("NAME_SHADOWING")
@Composable
fun ToggleSetting(
    value: Boolean,
    onValueChanged: (value: Boolean) -> Unit,
    title: String,
    description: String? = null,
    icon: (@Composable (enabled: Boolean) -> Unit)? = null,
    enabled: Boolean = true,
) {
    BasicSetting(
        title = title,
        description = description,
        icon = icon,
        onClick = { onValueChanged(!value) },
        trailingContent = { enabled ->
            Switch(
                checked = value,
                onCheckedChange = onValueChanged,
                enabled = enabled
            )
        },
        enabled = enabled
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ToggleSettingRequiringPermission(
    permission: String,
    value: Boolean,
    onValueChanged: (value: Boolean) -> Unit,
    title: String,
    description: String? = null,
    icon: (@Composable (enabled: Boolean) -> Unit)? = null,
    enabled: Boolean = true,
) {
    val permissionState = rememberPermissionState(
        permission = permission,
        onPermissionResult = { result -> if (result != value) onValueChanged(result) }
    )

    ToggleSetting(
        value = value && permissionState.status.isGranted,
        onValueChanged = { enabled ->
            if (enabled && !permissionState.status.isGranted) {
                permissionState.launchPermissionRequest()
            } else {
                onValueChanged(enabled)
            }
        },
        title, description, icon, enabled
    )
}

interface AbstractOption {
    @Composable
    fun title(): String

    // Convenience
    val adaptiveIcon: AdaptiveIcon? get() = null

    @get:Composable
    val icon: Painter? get() = adaptiveIcon?.let { rememberAdaptiveIconPainter(it) }
}

interface StaticOption : AbstractOption

interface DynamicOption : AbstractOption {
    /** A unique id to identify this option, null is a valid id */
    val id: String?
}

// Same as the other overload, but allows omitting the options parameter
@Composable
inline fun <reified T> FixedChooseOptionSetting(
    value: T,
    noinline onValueChanged: (T) -> Unit,
    title: String,
    description: String? = null,
    noinline icon: @Composable ((enabled: Boolean) -> Unit)? = null,
    hasIcons: Boolean = false,
    enabled: Boolean = true,
): Unit where T : Enum<T>, T : StaticOption = FixedChooseOptionSetting(
    value, onValueChanged, title, description, icon, enumEntries<T>(), hasIcons, enabled
)

@Composable
fun <T> FixedChooseOptionSetting(
    value: T,
    onValueChanged: (T) -> Unit,
    title: String,
    description: String? = null,
    icon: @Composable ((enabled: Boolean) -> Unit)? = null,
    options: EnumEntries<T>,
    hasIcons: Boolean = false,
    enabled: Boolean = true,
): Unit where T : Enum<T>, T : StaticOption = AbstractChooseOptionSetting(
    value, onValueChanged, title, description, icon, options,
    optionsEqual = { o1, o2 -> o1 == o2 },
    hasIcons, enabled
)

@Composable
fun <T : DynamicOption> DynamicChooseOptionSetting(
    value: T,
    onValueChanged: (T) -> Unit,
    title: String,
    description: String? = null,
    icon: @Composable ((enabled: Boolean) -> Unit)? = null,
    options: List<T>,
    hasIcons: Boolean = false,
    enabled: Boolean = true,
): Unit = AbstractChooseOptionSetting(
    value, onValueChanged, title, description, icon, options,
    optionsEqual = { o1, o2 -> o1.id == o2.id },
    hasIcons, enabled
)

@Composable
private inline fun <T : AbstractOption> AbstractChooseOptionSetting(
    value: T,
    noinline onValueChanged: (T) -> Unit,
    title: String,
    description: String?,
    noinline icon: @Composable ((enabled: Boolean) -> Unit)? = null,
    options: List<T>,
    crossinline optionsEqual: (T, T) -> Boolean,
    hasIcons: Boolean,
    enabled: Boolean,
) {
    var open by remember { mutableStateOf(false) }

    BasicSetting(
        title = title, icon = icon,
        description = value.title(),
        onClick = { open = true },
        enabled = enabled
    )

    if (open) {
        var selection by remember { mutableStateOf(value) }
        AlertDialog(
            title = {
                Column {
                    Text(title)
                    if (description != null) {
                        Text(description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            text = {
                val scrollState = rememberScrollState()
                Column(
                    Modifier
                        .selectableGroup()
                        .verticalScroll(scrollState)
                ) {
                    for (option in options) {
                        val radioButton = @Composable {
                            RadioButton(
                                selected = optionsEqual(option, selection),
                                onClick = null
                            )
                        }
                        ListItem(
                            leadingContent = if (!hasIcons) radioButton else ({
                                val icon: Painter? = option.icon
                                if (icon != null) {
                                    Icon(
                                        painter = icon,
                                        tint = Color.Unspecified,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                    )
                                } else {
                                    Spacer(Modifier.width(40.dp))
                                }
                            }),
                            headlineContent = { Text(option.title()) },
                            trailingContent = if (hasIcons) radioButton else null,
                            modifier = Modifier.selectable(
                                selected = optionsEqual(option, selection),
                                onClick = { selection = option },
                                role = RadioButton
                            ),
                            colors = ListItemDefaults.colors(containerColor = Transparent)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    open = false
                    onValueChanged(selection)
                }) { Text("Okay") }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text("Cancel") }
            },
            onDismissRequest = { open = false },
        )
    }

}

private fun Color.disabled(enabled: Boolean) = if (!enabled) copy(alpha = 0.38f) else this

@Composable
private fun BasicSetting(
    onClick: (() -> Unit)?,
    title: String,
    description: String? = null,
    icon: @Composable ((enabled: Boolean) -> Unit)? = null,
    trailingContent: @Composable ((enabled: Boolean) -> Unit)? = null,
    enabled: Boolean = true,
) {
    ListItem(
        headlineContent = { Text(title, color = LocalContentColor.current.disabled(enabled)) },
        supportingContent = if (description != null) ({
            Text(description, color = LocalContentColor.current.disabled(enabled))
        }) else null,
        leadingContent = if (icon != null) ({ icon(enabled) }) else null,
        trailingContent = if (trailingContent != null) ({ trailingContent(enabled) }) else null,
        modifier = Modifier.applyIf(onClick != null) {
            clickable(onClick = onClick!!, role = Role.Button, enabled = enabled)
        },
        colors = ListItemDefaults.colors(containerColor = Transparent)
    )
}
