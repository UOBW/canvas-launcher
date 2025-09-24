package io.github.canvas.ui.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType.Companion.Number
import androidx.compose.ui.unit.dp

@Composable
fun OutlinedNumberInput(
    value: NumberInputValue,
    onValueChange: (NumberInputValue) -> Unit,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Unspecified,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    label: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value.text,
        onValueChange = { newValue ->
            //TODO localize
            onValueChange(NumberInputValue(newValue))
        },
        singleLine = true,
        isError = value.number == null,
        keyboardOptions = KeyboardOptions(
            keyboardType = Number,
            imeAction = imeAction
        ),
        keyboardActions = keyboardActions,
        modifier = modifier,
        label = label
    )
}

data class NumberInputValue(
    val text: String,
    val number: Float?,
) {
    //TODO localize
    constructor(number: Float) : this(text = number.toString().removeSuffix(".0"), number = number)

    //TODO localize
    constructor(text: String) : this(text = text, number = text.toFloatOrNull())

}

/** A button that shows an icon next to some text */
@Composable
fun DetailsSheetButton(icon: ImageVector, text: String, onClick: () -> Unit) {
    TextButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text)
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DetailsSheet(
    onDismissRequest: () -> Unit,
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    subtitle: (@Composable () -> Unit)? = null,
    /** callback for the open button */
    onOpen: (() -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(verticalAlignment = CenterVertically) {
                Box(
                    Modifier
                        .padding(end = 8.dp)
                        .size(48.dp)
                ) { icon() }

                Column(modifier = Modifier.weight(1f)) { //Push the edit button to the right edge
                    CompositionLocalProvider(
                        LocalTextStyle provides MaterialTheme.typography.titleLarge
                    ) { title() }

                    if (subtitle != null) {
                        CompositionLocalProvider(
                            LocalTextStyle provides MaterialTheme.typography.titleSmall
                        ) { subtitle() }
                    }
                }

                Spacer(Modifier.width(8.dp))

                if (onOpen != null) {
                    FilledTonalButton(
                        onClick = onOpen,
                        modifier = Modifier.size(ButtonDefaults.MinHeight),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Default.Launch,
                            contentDescription = stringResource(R.string.details_sheet_accessibility_open_result)
                        )
                    }
                }
            }

            content()
        }
    }
}

@Composable
fun CustomIndexHorizontalPager(
    indexRange: IntRange,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
) {
    val indexOffset = indexRange.first // The offset of the custom index compared to the pager index

    val state = rememberPagerState(
        initialPage = initialPage - indexOffset,
        pageCount = { indexRange.count() }
    )

    HorizontalPager(
        state = state,
        pageContent = { index -> pageContent(index + indexOffset) },
        key = { it + indexOffset },
        modifier = modifier
    )
}
