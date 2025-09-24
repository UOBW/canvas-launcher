package io.github.canvas.ui.settings.screens

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import io.github.canvas.ui.settings.R
import io.github.canvas.ui.settings.rawStringResource

private data class LicenseUse(
    @field:StringRes val projectName: Int,
    @field:StringRes val author: Int,
    @field:StringRes val url: Int,
    @field:StringRes val licenseName: Int,
    @field:RawRes val licenseText: Int,
)

private fun licenseUseList() = listOf(
    // Ordered alphabetically
    // @formatter:off
    LicenseUse(R.string.accompanist_name, R.string.aosp, R.string.accompanist_url, R.string.apache_2_0_name, R.raw.apache_2_0),
    LicenseUse(R.string.androidx_name, R.string.aosp, R.string.androidx_url, R.string.apache_2_0_name, R.raw.apache_2_0),
    LicenseUse(R.string.serialization_name, R.string.jetbrains, R.string.serialization_url, R.string.apache_2_0_name, R.raw.apache_2_0),
    LicenseUse(R.string.widgetview_name, R.string.widgetview_author, R.string.widgetview_url, R.string.cc_by_sa_4_0_name, R.raw.cc_by_sa_4_0),
    LicenseUse(R.string.contact_icon_name, R.string.aosp, R.string.contact_icon_url, R.string.apache_2_0_name, R.raw.apache_2_0),
    LicenseUse(R.string.metronome_tempi_name, R.string.metronome_tempi_author, R.string.metronome_tempi_url, R.string.cc_by_sa_4_0_name, R.raw.cc_by_sa_4_0),
    LicenseUse(R.string.protobuf_name, R.string.google, R.string.protobuf_url, R.string.protobuf_license_name, R.raw.protobuf_bsd),
    LicenseUse(R.string.reorderable_name, R.string.reorderable_author, R.string.reorderable_url, R.string.apache_2_0_name, R.raw.apache_2_0),
    // @formatter:on
)

@Composable
fun LicensesScreen() {
    val licenseUseList = remember { licenseUseList() }
    // Index to licenseUseList
    var detailsDialogIndex by rememberSaveable { mutableIntStateOf(-1) }

    LazyColumn {
        itemsIndexed(licenseUseList) { index, licenseUse ->
            LicenseUse(licenseUse, onShowDetailsDialog = { detailsDialogIndex = index })
        }
    }

    if (detailsDialogIndex != -1) {
        LicenceUseDetailsDialog(
            licenseUse = licenseUseList[detailsDialogIndex],
            onClosed = { detailsDialogIndex = -1 }
        )
    }
}

@Composable
private fun LicenseUse(
    licenseUse: LicenseUse,
    onShowDetailsDialog: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(licenseUse.projectName)) },
        supportingContent = {
            Text(
                stringResource(
                    R.string.license_use_description,
                    stringResource(licenseUse.author),
                    stringResource(licenseUse.licenseName)
                )
            )
        },
        modifier = Modifier.clickable(
            onClick = onShowDetailsDialog,
            role = Role.Button,
            onClickLabel = stringResource(R.string.license_use_accessibility_show_details),
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LicenceUseDetailsDialog(
    licenseUse: LicenseUse,
    onClosed: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClosed,
        title = {
            Text(text = stringResource(licenseUse.projectName))
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    stringResource(
                        R.string.license_use_details_author,
                        stringResource(licenseUse.author)
                    )
                )
                Text(
                    buildAnnotatedString {
                        val url = stringResource(licenseUse.url)
                        withLink(
                            LinkAnnotation.Url(
                                url,
                                TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline))
                            )
                        ) { append(url) }
                    }
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = rawStringResource(licenseUse.licenseText).trimIndent(),
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onClosed) { Text(stringResource(R.string.license_use_details_close)) }
        }
    )
}
