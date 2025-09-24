package io.github.canvas.ui.launcher.dialogs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Left
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Right
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.canvas.data.IconChangeableSearchResult
import io.github.canvas.data.icons.AdaptiveIcon
import io.github.canvas.data.icons.AvailableIconPack
import io.github.canvas.data.icons.CustomIcon
import io.github.canvas.data.icons.IconPack
import io.github.canvas.ui.AdaptiveIconComposable
import io.github.canvas.ui.applyIf
import io.github.canvas.ui.launcher.R
import io.github.canvas.ui.searchResultLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeIconDialog(
    searchResult: IconChangeableSearchResult,
    availableIconPacks: List<AvailableIconPack>?,
    loadIconPack: suspend (iconPack: AvailableIconPack) -> IconPack,
    onClosed: () -> Unit,
    onIconChanged: (newIcon: CustomIcon?) -> Unit,
) {
    var selectedIcon: String? by rememberSaveable { mutableStateOf(null) }
    var selectedIconPack: String? by rememberSaveable { mutableStateOf(null) }
    var defaultIconSelected: Boolean by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val defaultIconIcon: AdaptiveIcon? by produceState(initialValue = null) {
        value = searchResult.loadDefaultIcon(context)
    }

    AlertDialog(
        title = {
            Text(
                stringResource(R.string.change_icon_dialog_title, searchResultLabel(searchResult))
            )
        },
        text = {
            AnimatedContent(
                targetState = selectedIconPack,
                transitionSpec = {
                    val direction = if (initialState == null) Left else Right
                    slideIntoContainer(direction) togetherWith slideOutOfContainer(direction)
                }
            ) { state ->
                if (state == null) {
                    ChooseIconPackScreen(
                        availableIconPacks, defaultIconSelected, defaultIconIcon,
                        onIconPackSelected = {
                            selectedIconPack = it.packageName
                            defaultIconSelected = false
                        },
                        onDefaultIconSelected = { defaultIconSelected = true }
                    )
                } else {
                    val loadedIconPack: IconPack? by produceState(initialValue = null) {
                        if (availableIconPacks != null) {
                            availableIconPacks.find { it.packageName == state }
                                ?.let { value = loadIconPack(it) }
                                ?: run { selectedIconPack = null } // Icon pack was uninstalled
                        }
                    }
                    ChooseIconScreen(
                        loadedIconPack, selectedIcon,
                        onSelectionChanged = { selectedIcon = it },
                        onBack = { selectedIconPack = null; selectedIcon = null }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = (selectedIcon != null && selectedIconPack != null) || defaultIconSelected,
                onClick = {
                    onIconChanged(
                        if (defaultIconSelected) null else
                            CustomIcon(selectedIconPack!!, selectedIcon!!)
                    )
                    onClosed()
                }
            ) { Text(stringResource(R.string.change_icon_dialog_confirm)) }
        },
        dismissButton = { TextButton(onClick = onClosed) { Text(stringResource(R.string.change_icon_dialog_cancel)) } },
        onDismissRequest = onClosed,
    )
}

@Composable
private fun ChooseIconPackScreen(
    availableIconPacks: List<AvailableIconPack>?,
    defaultIconSelected: Boolean,
    defaultIconIcon: AdaptiveIcon?,
    onIconPackSelected: (iconPack: AvailableIconPack) -> Unit,
    onDefaultIconSelected: () -> Unit,
) {
    LazyColumn {
        item {
            ListItem(
                leadingContent = {
                    AdaptiveIconComposable(defaultIconIcon, modifier = Modifier.size(40.dp))
                },
                headlineContent = { Text(stringResource(R.string.change_icon_dialog_default_icon)) },
                colors = ListItemDefaults.colors(containerColor = Transparent),
                modifier = Modifier
                    .applyIf(defaultIconSelected) {
                        background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.change_icon_dialog_accessibility_default_icon),
                        onClick = onDefaultIconSelected
                    )
            )
        }

        items(availableIconPacks.orEmpty()) { iconPack ->
            ListItem(
                leadingContent = {
                    AdaptiveIconComposable(iconPack.icon, modifier = Modifier.size(40.dp))
                },
                headlineContent = { Text(iconPack.name) },
                colors = ListItemDefaults.colors(containerColor = Transparent),
                modifier = Modifier.clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.change_icon_dialog_accessibility_select_icon_pack),
                    onClick = { onIconPackSelected(iconPack) }
                )
            )
        }
    }
}

@Composable
private fun ChooseIconScreen(
    iconPack: IconPack?,
    selectedIcon: String?,
    onSelectionChanged: (newIcon: String) -> Unit,
    onBack: () -> Unit,
) {
    val gridState = rememberLazyGridState()

    Column {
        var searchTerm: String by rememberSaveable { mutableStateOf("") }
        TextField(
            value = searchTerm,
            onValueChange = {
                searchTerm = it
                gridState.requestScrollToItem(0)
            },
            label = { Text(stringResource(R.string.change_icon_dialog_search)) },
            leadingIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = stringResource(R.string.change_icon_dialog_accessibility_back_to_choose_icon_pack)
                    )
                }
            },
            maxLines = 1,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions { defaultKeyboardAction(ImeAction.Done) }
        )

        Spacer(Modifier.height(8.dp))

        val allIcons by produceState<List<String>?>(initialValue = null, key1 = iconPack) {
            if (iconPack != null) value = iconPack.listIcons()
        }

        if (allIcons == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Center) {
                CircularProgressIndicator()
            }
        } else {
            val icons = remember(allIcons, searchTerm) {
                allIcons!!.filter { it.contains(searchTerm) }
            }
            LazyVerticalGrid(
                columns = GridCells.FixedSize(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                state = gridState
            ) {
                items(icons, key = { it }) { iconName ->
                    val icon: AdaptiveIcon? by produceState(initialValue = null) {
                        value = iconPack!!.loadIconByName(iconName)
                    }
                    if (icon != null) {
                        NavigationDrawerItemDefaults.colors()
                        AdaptiveIconComposable(
                            icon!!,
                            modifier = Modifier
                                .size(48.dp)
                                .applyIf(iconName == selectedIcon) {
                                    background(
                                        MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                                .clickable(
                                    indication = null, interactionSource = null,
                                    role = Role.Button,
                                    onClickLabel = stringResource(R.string.change_icon_dialog_accessibility_select_icon),
                                ) { onSelectionChanged(iconName) }
                                .padding(4.dp) // 40dp icon with 4dp margins
                        )
                    } else Spacer(Modifier.size(48.dp))
                }
            }
        }
    }
}
