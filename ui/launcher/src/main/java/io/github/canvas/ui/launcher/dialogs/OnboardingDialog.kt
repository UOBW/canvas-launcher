package io.github.canvas.ui.launcher.dialogs

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.canvas.data.icons.toAdaptiveIcon
import io.github.canvas.ui.AdaptiveIconComposable
import io.github.canvas.ui.ToggleSetting
import io.github.canvas.ui.ToggleSettingRequiringPermission
import io.github.canvas.ui.isDefaultLauncher
import io.github.canvas.ui.launcher.LauncherViewModel
import io.github.canvas.ui.launcher.R
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@Composable
fun OnboardingDialog(
    viewModel: LauncherViewModel,
    onClosed: () -> Unit,
) {
    val settings by viewModel.settings.collectAsState(initial = null)
    val context = LocalContext.current

    AlertDialog(
        icon = {
            AdaptiveIconComposable(
                remember {
                    context.getDrawable(io.github.canvas.ui.R.mipmap.ic_launcher)!!
                        .toAdaptiveIcon()
                },
                modifier = Modifier.size(48.dp)
            )
        },
        title = { Text(stringResource(R.string.onboarding_title)) },
        text = {
            Column {
                Text(stringResource(R.string.onboarding_description))

                if (settings == null) return@AlertDialog

                val windowInfo = LocalWindowInfo.current
                val isDefaultLauncher by remember(windowInfo) {
                    // recheck every time the window gains focus because the user might have just returned from the system settings
                    snapshotFlow { windowInfo.isWindowFocused }
                        .filter { it == true }
                        .map { context.isDefaultLauncher() }
                }.collectAsStateWithLifecycle(initialValue = false)

                ToggleSetting(
                    title = stringResource(R.string.onboarding_default_launcher),
                    description = stringResource(R.string.onboarding_default_launcher_description),
                    value = isDefaultLauncher,
                    onValueChanged = {
                        context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                    }
                )

                ToggleSettingRequiringPermission(
                    title = stringResource(io.github.canvas.ui.R.string.setting_contacts_search),
                    description = stringResource(io.github.canvas.ui.R.string.setting_contacts_search_description),
                    permission = Manifest.permission.READ_CONTACTS,
                    value = settings!!.contactSearchEnabled,
                    onValueChanged = { viewModel.setContactSearchEnabled(it) }
                )

                ToggleSettingRequiringPermission(
                    title = stringResource(io.github.canvas.ui.R.string.setting_calendar_search),
                    description = stringResource(io.github.canvas.ui.R.string.setting_calendar_search_description),
                    permission = Manifest.permission.READ_CALENDAR,
                    value = settings!!.calendarSearchEnabled,
                    onValueChanged = { viewModel.setCalendarSearchEnabled(it) }
                )
            }
        },
        confirmButton = { TextButton(onClick = onClosed) { Text(stringResource(R.string.onboarding_confirm)) } },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        onDismissRequest = { error("Dialog should not be dismissable") }
    )
}
