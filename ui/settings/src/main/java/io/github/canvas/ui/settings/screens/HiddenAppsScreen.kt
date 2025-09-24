package io.github.canvas.ui.settings.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.canvas.data.HideableSearchResult
import io.github.canvas.ui.SearchResultComposable
import io.github.canvas.ui.fadingEdges
import io.github.canvas.ui.settings.R
import io.github.canvas.ui.settings.SettingsViewModel

@Composable
fun HiddenAppsScreen(settingsViewModel: SettingsViewModel) {
    val hiddenResults: List<HideableSearchResult> by settingsViewModel.hiddenResults.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fadingEdges(
                    listState = listState,
                    topFadeHeight = 10.dp,
                    bottomFadeHeight = 0.dp
                )
                .padding(horizontal = 16.dp),
            state = listState
        ) {
            items(
                items = hiddenResults,
                key = { it.uid.string }
            ) { result ->
                Row(
                    modifier = Modifier
                        .clickable(
                            onClick = { result.setHiddenAsync(false, context) },
                            role = Role.Button,
                            onClickLabel = stringResource(R.string.accessibility_unhide_result)
                        )
                        .animateItem(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f)) {
                        SearchResultComposable(result)
                    }
                    Icon(Icons.Default.Visibility, contentDescription = null)
                }
            }
            item(key = "spacer_bottom") {
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }

        AnimatedVisibility(
            visible = hiddenResults.isEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                stringResource(R.string.no_hidden_results),
                textAlign = TextAlign.Center
            )
        }
    }
}
