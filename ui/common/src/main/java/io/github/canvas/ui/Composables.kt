package io.github.canvas.ui

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode.Companion.DstIn
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.Color.Companion.Unspecified
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.CompositingStrategy.Companion.Offscreen
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.canvas.data.FormattedResIdLabelSearchResult
import io.github.canvas.data.ResIdLabelSearchResult
import io.github.canvas.data.SearchResult
import io.github.canvas.data.StringLabelSearchResult
import io.github.canvas.data.icons.AdaptiveIcon
import io.github.canvas.data.icons.toAdaptiveIcon
import kotlinx.coroutines.flow.first

/** Size of the badge icon compared to the main icon */
private const val BADGE_SIZE = 0.5f

/** How much of the badge icon should be outside its bounds */
private const val BADGE_OFFSET = 0.25f

@Composable
fun SearchResultIcon(
    searchResult: SearchResult,
    modifier: Modifier = Modifier,
    onGloballyPositioned: ((LayoutCoordinates) -> Unit)? = null,
): Unit = AdaptiveIconComposable(
    searchResult.icon, searchResult.badgeIcon, modifier, onGloballyPositioned
)

/** Same as the other overload, but displays a blank space if the icon is not yet loaded (null) */
@Composable
@JvmName("OptionalAdaptiveIconComposable")
fun AdaptiveIconComposable(
    icon: AdaptiveIcon?,
    badgeIcon: AdaptiveIcon? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    onGloballyPositioned: ((LayoutCoordinates) -> Unit)? = null,
): Unit = when (icon) {
    null -> Spacer(modifier)
    else -> AdaptiveIconComposable(icon, badgeIcon, modifier, onGloballyPositioned)
}

@Composable
fun AdaptiveIconComposable(
    icon: AdaptiveIcon,
    badgeIcon: AdaptiveIcon? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    onGloballyPositioned: ((LayoutCoordinates) -> Unit)? = null,
) {
    BoxWithConstraints(modifier) {
        Icon(
            painter = rememberAdaptiveIconPainter(icon),
            contentDescription = null,
            tint = Unspecified,
            modifier = Modifier
                .fillMaxSize()
                .applyIf(onGloballyPositioned != null) {
                    onGloballyPositioned(onGloballyPositioned!!)
                }
        )

        if (badgeIcon != null) {
            Icon(
                painter = rememberAdaptiveIconPainter(badgeIcon),
                contentDescription = null,
                tint = Unspecified,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxSize(BADGE_SIZE)
                    .offset(
                        x = this@BoxWithConstraints.maxWidth * BADGE_SIZE * BADGE_OFFSET,
                        y = this@BoxWithConstraints.maxHeight * BADGE_SIZE * BADGE_OFFSET
                    )
            )
        }
    }
}

@Composable
fun searchResultLabel(searchResult: SearchResult): String =
    when (searchResult) {
        is StringLabelSearchResult -> searchResult.label
        is ResIdLabelSearchResult -> stringResource(searchResult.label)
        is FormattedResIdLabelSearchResult -> stringResource(
            searchResult.label,
            *searchResult.formatArgs.toTypedArray()
        )

        else -> error("Search result does not implement either ResIdLabel or StringLabel or FormattedResIdLabel")
    }

/** Add fading edges to the top and bottom of a LazyColumn when scrolling */
fun Modifier.fadingEdges(
    listState: LazyListState,
    topFadeHeight: Dp,
    bottomFadeHeight: Dp,
): Modifier = graphicsLayer { compositingStrategy = Offscreen }
    .drawWithContent {
        drawContent()

        if (topFadeHeight > 0.dp && listState.canScrollBackward) {
            drawRect(
                brush = verticalGradient(
                    endY = topFadeHeight.toPx(),
                    colors = listOf(Transparent, White)
                ),
                blendMode = DstIn
            )
        }

        if (bottomFadeHeight > 0.dp && listState.canScrollForward) {
            drawRect(
                brush = verticalGradient(
                    startY = size.height - bottomFadeHeight.toPx(),
                    colors = listOf(White, Transparent)
                ),
                blendMode = DstIn
            )
        }
    }

@Composable
fun AsyncIcon(load: suspend () -> Drawable) {
    val icon: AdaptiveIcon? by produceState(initialValue = null, key1 = load) {
        value = load().toAdaptiveIcon()
    }

    if (icon != null) {
        AdaptiveIconComposable(icon!!)
    } else {
        CircularProgressIndicator(Modifier.fillMaxSize())
    }
}

@Composable
fun TextInputDialog(
    title: String,
    value: TextFieldValue,
    onValueChanged: (TextFieldValue) -> Unit,
    confirmButtonText: String,
    dismissButtonText: String = stringResource(R.string.generic_cancel),
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
    trailingIcon: @Composable (() -> Unit)? = null,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Unspecified,
    /** null - input is valid, "" - silent error (no highlighting but submit is disabled), else - show error message, submit disabled */
    errorMessage: String? = null,
) {
    AlertDialog(
        title = { Text(title) },
        text = {
            val focusRequester = remember { FocusRequester() }

            TextField(
                value = value,
                onValueChange = onValueChanged,
                isError = errorMessage?.isNotEmpty() ?: false,
                singleLine = true,
                trailingIcon = trailingIcon,
                keyboardOptions = KeyboardOptions(
                    capitalization = capitalization,
                    imeAction = ImeAction.Done,
                    showKeyboardOnFocus = true
                ),
                keyboardActions = KeyboardActions { onConfirmed() },
                modifier = Modifier.focusRequester(focusRequester),
                supportingText = if (errorMessage?.isNotEmpty() ?: false) ({
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }) else null
            )

            val windowInfo = LocalWindowInfo.current
            LaunchedEffect(windowInfo, focusRequester) {
                snapshotFlow { windowInfo.isWindowFocused }.first { it == true } // Wait for focus
                try {
                    focusRequester.requestFocus()
                } catch (e: IllegalStateException) {
                    log.e("Failed to request focus", e)
                }
            }
        },
        onDismissRequest = onDismissed,
        confirmButton = {
            TextButton(onClick = onConfirmed, enabled = errorMessage == null) {
                Text(confirmButtonText)
            }
        },
        dismissButton = { TextButton(onClick = onDismissed) { Text(dismissButtonText) } }
    )
}
