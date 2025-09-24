package io.github.canvas.ui.settings.screens

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.canvas.data.icons.toAdaptiveIcon
import io.github.canvas.ui.AdaptiveIconComposable
import io.github.canvas.ui.BasicSettingsItem
import io.github.canvas.ui.settings.LicensesScreen
import io.github.canvas.ui.settings.R
import io.github.canvas.ui.settings.Route
import io.github.canvas.ui.settings.SettingsViewModel
import io.github.canvas.ui.settings.rawStringResource

@Composable
fun AboutScreen(viewModel: SettingsViewModel, navigateTo: (route: Route) -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle(initialValue = null)

    val context = LocalContext.current
    val resources = LocalResources.current
    val uriHandler = LocalUriHandler.current

    val (versionName, versionCode) = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).let {
            @Suppress("DEPRECATION")
            it.versionName to (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode else it.versionCode)
        }
    }

    var showLicenseDialog by remember { mutableStateOf(false) }
    if (showLicenseDialog) LicenseDialog(onClosed = { showLicenseDialog = false })

    Column {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            AdaptiveIconComposable(
                remember {
                    context.getDrawable(io.github.canvas.ui.R.mipmap.ic_launcher)!!
                        .toAdaptiveIcon()
                },
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(io.github.canvas.ui.R.string.app_name),
                style = MaterialTheme.typography.displaySmall
            )
        }

        var developerOptionsCounter by remember { mutableIntStateOf(7) }
        var prevToast by remember { mutableStateOf<Toast?>(null) }
        BasicSettingsItem(
            title = stringResource(R.string.about_version_title),
            description = stringResource(
                R.string.about_version_format,
                "$versionName", versionCode
            ),
            onClick = {
                if (settings == null) return@BasicSettingsItem
                developerOptionsCounter--
                if (developerOptionsCounter == 0) viewModel.setShowDeveloperOptions(true)

                val toastText = when {
                    settings!!.showDeveloperOptions || developerOptionsCounter < 0 /* still processing */ ->
                        resources.getString(R.string.about_developer_options_already_enabled)

                    developerOptionsCounter == 0 ->
                        resources.getString(R.string.about_developer_options_enabled)

                    developerOptionsCounter <= 4 -> resources.getQuantityString(
                        R.plurals.about_developer_options_n_steps_away,
                        developerOptionsCounter, developerOptionsCounter
                    )

                    else -> null
                }
                if (toastText != null) {
                    prevToast?.cancel()
                    val toast = Toast.makeText(context, toastText, Toast.LENGTH_SHORT)
                    toast.show()
                    prevToast = toast
                }
            }
        )

        BasicSettingsItem(
            title = stringResource(R.string.about_license_title),
            description = stringResource(R.string.gnu_gpl_v3_name),
            onClick = { showLicenseDialog = true }
        )

        BasicSettingsItem(
            title = stringResource(R.string.about_source_code_title),
            description = stringResource(R.string.about_source_code_description),
            onClick = { uriHandler.openUri(resources.getString(R.string.github_repo_url)) }
        )

        HorizontalDivider()

        BasicSettingsItem(
            title = stringResource(R.string.about_developer_title),
            description = stringResource(R.string.about_developer_name),
            onClick = { uriHandler.openUri(resources.getString(R.string.about_developer_url)) }
        )

        BasicSettingsItem(
            title = stringResource(R.string.about_alpha_tester_title),
            description = stringResource(R.string.about_alpha_tester_name),
            onClick = { uriHandler.openUri(resources.getString(R.string.about_alpha_tester_url)) }
        )

        HorizontalDivider()

        BasicSettingsItem(
            title = stringResource(R.string.license_screen_title),
            description = stringResource(R.string.license_screen_description),
            onClick = { navigateTo(LicensesScreen) }
        )
    }
}

/** Displays the GNU GPLv3 license used by Canvas Launcher */
@Composable
fun LicenseDialog(onClosed: () -> Unit) {
    AlertDialog(
        title = { Text(stringResource(R.string.gnu_gpl_v3_name)) },
        text = {
            Text(
                rawStringResource(R.raw.gnu_gpl_v3), fontFamily = FontFamily.Monospace,
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        },
        onDismissRequest = onClosed,
        confirmButton = {
            TextButton(onClick = onClosed) { Text(stringResource(R.string.license_dialog_close)) }
        }
    )
}
