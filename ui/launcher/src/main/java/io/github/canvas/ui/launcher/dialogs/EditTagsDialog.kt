package io.github.canvas.ui.launcher.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.canvas.data.SearchResult
import io.github.canvas.data.Tag
import io.github.canvas.data.TagSetSaver
import io.github.canvas.data.TaggableSearchResult
import io.github.canvas.ui.TextInputDialog
import io.github.canvas.ui.launcher.R
import io.github.canvas.ui.launcher.name
import io.github.canvas.ui.searchResultLabel

@Composable
fun EditTagsDialog(
    searchResult: TaggableSearchResult,
    allSearchResults: List<SearchResult>,
    onClosed: () -> Unit,
    onEditTags: (tags: Set<Tag>) -> Unit,
) {
    var newlyAddedTags by rememberSaveable(stateSaver = TagSetSaver) {
        mutableStateOf(emptySet<Tag.Custom>())
    }
    var selectedTags by rememberSaveable(stateSaver = TagSetSaver) {
        mutableStateOf(searchResult.tags)
    }

    val allTags =
        (allSearchResults.flatMap { it.tags } + Tag.Builtin.entries + newlyAddedTags).distinct()

    AlertDialog(
        title = {
            Text(stringResource(R.string.edit_tags_dialog_title, searchResultLabel(searchResult)))
        },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((tag, name) in allTags.map { it to it.name }.sortedBy { (_, name) -> name }) {
                    val selected = tag in selectedTags
                    FilterChip(
                        label = { Text(name) },
                        selected = selected,
                        leadingIcon = {
                            AnimatedVisibility(selected) {
                                Icon(
                                    Icons.Default.Done,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        },
                        onClick = {
                            if (tag in selectedTags) selectedTags -= tag else selectedTags += tag
                        },
                    )
                }

                var showAddTagDialog by rememberSaveable { mutableStateOf(false) }
                Spacer(Modifier.fillMaxWidth()) // Force line break, show Add tag in its own row
                AssistChip(
                    onClick = { showAddTagDialog = true },
                    label = { Text(stringResource(R.string.edit_tags_dialog_add_tag)) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    }
                )

                if (showAddTagDialog) {
                    this
                    AddTagDialog(
                        onAddTag = {
                            val tag = Tag.Custom(it)
                            newlyAddedTags += tag
                            selectedTags += tag
                        },
                        onClosed = { showAddTagDialog = false }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onClosed()
                onEditTags(selectedTags.toSet())
            }) { Text(stringResource(R.string.edit_tags_dialog_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onClosed) { Text(stringResource(R.string.edit_tags_dialog_cancel)) }
        },
        onDismissRequest = onClosed,
        modifier = Modifier.fillMaxWidth() // Prevent the dialog from resizing during animations
    )
}

@Composable
private fun AddTagDialog(
    onAddTag: (name: String) -> Unit,
    onClosed: () -> Unit,
) {
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    TextInputDialog(
        title = stringResource(R.string.edit_tags_dialog_add_tag),
        value = text,
        onValueChanged = {
            // Force lowercase, no whitespace
            text = it.copy(text = it.text.lowercase().filterNot { it.isWhitespace() || it == '#' })
        },
        confirmButtonText = stringResource(R.string.add_tag_confirm),
        onConfirmed = {
            onClosed()
            onAddTag(text.text)
        },
        onDismissed = onClosed,
        capitalization = KeyboardCapitalization.None,
        errorMessage = when {
            text.text.isEmpty() -> ""

            Tag.Builtin.entries.any { it.name.equals(text.text, ignoreCase = true) } ->
                stringResource(R.string.add_tag_error_reserved)

            else -> null
        }
    )
}
