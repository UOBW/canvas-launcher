package io.github.canvas.ui

import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.core.net.toUri
import io.github.canvas.data.CustomRendererSearchResult
import io.github.canvas.data.FormattedResIdLabelSearchResult
import io.github.canvas.data.HideableSearchResult
import io.github.canvas.data.ResIdLabelSearchResult
import io.github.canvas.data.SearchResult
import io.github.canvas.data.Uid
import io.github.canvas.data.application
import io.github.canvas.data.icons.AdaptiveIcon
import io.github.canvas.data.icons.ColorBackground
import io.github.canvas.data.icons.ComposePainter
import io.github.canvas.data.icons.LetterPainter
import io.github.canvas.data.icons.ResIdForeground
import io.github.canvas.data.icons.VectorForeground
import io.github.canvas.data.math.Metronome

/** A "search result" that is shown by the launcher based not on search tokens, but on some logic or unconditionally */
interface SpecialSearchResult : SearchResult {
    override val searchTokens: List<String> get() = emptyList()
}

data class UrlSearchResult(
    private val url: Uri,
) : SpecialSearchResult, ResIdLabelSearchResult {
    override val uid: Uid get() = Uid("special/open_url")
    override val label: Int get() = R.string.result_openurl
    override val icon: AdaptiveIcon by ::Icon

    override fun open(context: Context, options: Bundle) {
        context.startActivity(Intent(Intent.ACTION_VIEW, url), options)
    }

    companion object {
        private val Icon = AdaptiveIcon(
            foreground = VectorForeground(Icons.AutoMirrored.Default.OpenInNew),
            foregroundScale = AdaptiveIcon.Companion.SCALE_FIT,
            background = ColorBackground(White),
            monochrome = null
        )
    }
}

private val MathIcon = AdaptiveIcon(
    foreground = ComposePainter(LetterPainter("=", Color.Companion.Black)),
    background = ColorBackground(White),
    monochrome = null
)

data class MathSearchResult(
    override val label: Int,
    override val formatArgs: List<Any>,
    val isSuccessful: Boolean,
) : SpecialSearchResult, FormattedResIdLabelSearchResult {
    override val uid: Uid get() = Uid("special/math")
    override val icon: AdaptiveIcon by ::MathIcon
    override val isError: Boolean get() = !isSuccessful

    override fun open(context: Context, options: Bundle) {
        //Copy to clipboard on click
        context.copyToClipboard(context.getString(label, *formatArgs.toTypedArray()))
    }
}

data class UnitConversionSearchResult(
    override val label: Int,
    override val formatArgs: List<Any>,
    val isSuccessful: Boolean,
) : SpecialSearchResult, FormattedResIdLabelSearchResult {
    override val uid: Uid get() = Uid("special/units")
    override val icon: AdaptiveIcon by ::MathIcon
    override val isError: Boolean get() = !isSuccessful

    override fun open(context: Context, options: Bundle) {
        //Copy to clipboard on click
        context.copyToClipboard(context.getString(label, *formatArgs.toTypedArray()))
    }
}

data class MetronomeSearchResult(
    val metronome: Metronome,
) : SpecialSearchResult, CustomRendererSearchResult, ResIdLabelSearchResult {
    override val uid: Uid get() = Uid("special/metronome")

    override val icon: AdaptiveIcon by ::Icon
    override val label: Int get() = R.string.result_metronome

    override fun open(context: Context, options: Bundle) {} // Handled by renderer

    companion object {
        private val Icon = AdaptiveIcon(
            foreground = VectorForeground(Icons.Default.Timer),
            foregroundScale = 0.5f,
            background = ColorBackground(White),
            monochrome = null
        )
    }
}

data class WebSearch(
    val searchTerm: String,
) : SpecialSearchResult, FormattedResIdLabelSearchResult {
    override val uid: Uid get() = Uid("search/web")
    override val label: Int get() = R.string.web_search
    override val formatArgs: List<String> = listOf(searchTerm)
    override val icon: AdaptiveIcon by ::Icon

    override fun open(context: Context, options: Bundle) {
        context.startActivity(
            Intent(Intent.ACTION_WEB_SEARCH).putExtra(
                SearchManager.QUERY,
                searchTerm
            ), options
        )
    }

    companion object {
        private val Icon = AdaptiveIcon(
            foreground = VectorForeground(Icons.Default.TravelExplore),
            foregroundScale = 0.5f,
            background = ColorBackground(White),
            monochrome = null
        )
    }
}

data class WikipediaSearch(
    val searchTerm: String,
) : SpecialSearchResult, FormattedResIdLabelSearchResult {
    override val uid: Uid get() = Uid("search/wikipedia")

    override val label: Int @StringRes get() = R.string.wikipedia_search
    override val formatArgs: List<Any> get() = listOf(searchTerm)
    override val icon: AdaptiveIcon by ::Icon

    override fun open(context: Context, options: Bundle) {
        try {
            //Try starting the official Wikipedia app first
            val app = Intent(Intent.ACTION_SEND).apply {
                component = ComponentName("org.wikipedia", "org.wikipedia.search.SearchActivity")
                putExtra(Intent.EXTRA_TEXT, searchTerm)
                type = "text/plain"
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(app, options)
        } catch (_: Exception) {
            // The app is not installed, the version of the app is too old or a future version changed the search activity, open the Wikipedia website instead
            log.d("Unable to open the Wikipedia app, opening the website instead")
            val uri = BaseUrl.buildUpon().appendQueryParameter("search", searchTerm).build()
            context.startActivity(Intent(Intent.ACTION_VIEW, uri), options)
        }
    }

    companion object {
        private val BaseUrl: Uri = "https://en.wikipedia.org/wiki/Special:Search".toUri()

        private val Icon = AdaptiveIcon(
            foreground = ResIdForeground(R.drawable.wikipedia_icon),
            background = ColorBackground(White),
            monochrome = null
        )
    }
}

data class AppStoreSearch(
    val searchTerm: String,
) : SpecialSearchResult, ResIdLabelSearchResult, HideableSearchResult {
    override val uid: Uid get() = Uid("search/app_store")

    override val label: Int @StringRes get() = R.string.app_store_search
    override val icon: AdaptiveIcon by ::Icon

    override fun open(context: Context, options: Bundle) {
        val url = BaseUrl.buildUpon().appendQueryParameter("q", searchTerm).build()
        context.startActivity(Intent(Intent.ACTION_VIEW, url), options)
    }

    override fun setHiddenAsync(value: Boolean, context: Context): Unit =
        context.application.settingsRepository.setAppStoreSearchEnabledAsync(!value)

    companion object {
        private val BaseUrl: Uri = "market://search".toUri()

        private val Icon = AdaptiveIcon(
            foreground = VectorForeground(Icons.Default.GetApp),
            foregroundScale = 0.5f,
            background = ColorBackground(White),
            monochrome = null
        )
    }
}
