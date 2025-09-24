package io.github.canvas.ui.launcher.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import io.github.canvas.data.RenameableSearchResult
import io.github.canvas.data.originalLabel
import io.github.canvas.ui.TextInputDialog
import io.github.canvas.ui.launcher.R
import kotlin.Int.Companion.MAX_VALUE

@Composable
fun RenameDialog(
    searchResult: RenameableSearchResult,
    onClosed: () -> Unit,
    onRename: (newName: String, oldName: String) -> Unit,
) {
    var value by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(text = searchResult.label, selection = TextRange(0, MAX_VALUE))
        )
    }

    TextInputDialog(
        title = stringResource(R.string.rename_dialog_title, searchResult.label),
        value = value,
        onValueChanged = { value = it },
        confirmButtonText = stringResource(R.string.rename_dialog_confirm),
        onDismissed = onClosed,
        onConfirmed = {
            onClosed()
            onRename(value.text, searchResult.label)
        },
        trailingIcon = if (value.text != searchResult.originalLabel) ({
            Icon(
                Icons.Default.Refresh,
                contentDescription = stringResource(R.string.rename_dialog_accessibility_reset),
                modifier = Modifier.clickable(
                    onClickLabel = stringResource(R.string.rename_dialog_accessibility_reset),
                    role = Role.Button
                ) {
                    value = TextFieldValue(
                        text = searchResult.originalLabel,
                        selection = TextRange(0, MAX_VALUE)
                    )
                }
            )
        }) else null,
        capitalization = KeyboardCapitalization.Words,
        errorMessage = if (value.text.isEmpty()) "" else null
    )
}
