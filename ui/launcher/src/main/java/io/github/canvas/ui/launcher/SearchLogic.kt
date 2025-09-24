package io.github.canvas.ui.launcher

import android.webkit.URLUtil
import androidx.core.net.toUri
import io.github.canvas.data.FavoritableSearchResult
import io.github.canvas.data.SearchResult
import io.github.canvas.data.activities.Activity
import io.github.canvas.data.calendar.CalendarSearchResult
import io.github.canvas.data.contacts.Contact
import io.github.canvas.data.math.MathExpressionParser
import io.github.canvas.data.math.MetronomeParser
import io.github.canvas.data.math.UnitConversionParser
import io.github.canvas.data.settings.Settings
import io.github.canvas.data.shortcuts.Shortcut
import io.github.canvas.data.sortedByLabel
import io.github.canvas.data.unlocalizedName
import io.github.canvas.ui.AppStoreSearch
import io.github.canvas.ui.MathSearchResult
import io.github.canvas.ui.MetronomeSearchResult
import io.github.canvas.ui.UnitConversionSearchResult
import io.github.canvas.ui.UrlSearchResult
import io.github.canvas.ui.WebSearch
import io.github.canvas.ui.WikipediaSearch

/** Same as the other overload, but deals with not-yet-loaded search results */
@JvmName("computeSearchResultsNullable")
fun computeSearchResults(
    searchTerm: String,
    initialResults: Settings.InitialResults,
    activities: List<Activity>?,
    static: List<Shortcut>?,
    dynamic: List<Shortcut>?,
    pinned: List<Shortcut>?,
    cached: List<Shortcut>?,
    contacts: List<Contact>?,
    calendar: List<CalendarSearchResult>?,
): List<SearchResult> = computeSearchResults(
    searchTerm, initialResults, activities ?: emptyList(),
    static ?: emptyList(), dynamic ?: emptyList(), pinned ?: emptyList(), cached ?: emptyList(),
    contacts ?: emptyList(), calendar ?: emptyList()
)

/** Given a search term and a list of possible search results, computes the search results to be shown to the user, in order */
fun computeSearchResults(
    searchTerm: String,
    initialResults: Settings.InitialResults,
    activities: List<Activity>,
    static: List<Shortcut>,
    dynamic: List<Shortcut>,
    pinned: List<Shortcut>,
    cached: List<Shortcut>,
    contacts: List<Contact>,
    calendar: List<CalendarSearchResult>,
): List<SearchResult> {
    if (searchTerm.isEmpty()) {
        log.d("Empty search term, all activities returned")
        return when (initialResults) {
            Settings.InitialResults.FAVORITES -> (activities + static + dynamic + pinned + cached + contacts)
                .filter { it.isFavorite }.sortedByLabel()

            Settings.InitialResults.ALL_APPS -> activities.sortedByLabel() // Don't show favorites first
            Settings.InitialResults.NOTHING -> emptyList()
        }
    }
    //Commands
    if (searchTerm[0] == '/') {
        return when (searchTerm) {
            "/all" -> activities + pinned + static + dynamic + cached + contacts
            "/shortcuts" -> pinned + static + dynamic + cached
            "/static" -> static
            "/dynamic" -> dynamic
            "/pinned" -> pinned
            "/cached" -> cached
            "/contacts" -> contacts
            "/calendar" -> calendar
            else -> emptyList()
        }
    }

    val results = mutableListOf<SearchResult>()
    if (searchTerm[0] == '#') {
        val term = searchTerm.drop(1)
        results += activities.tagSort(term)
        results += pinned.tagSort(term)
        results += static.tagSort(term)
        results += dynamic.tagSort(term)
        results += cached.tagSort(term)
        results += contacts.tagSort(term)
        results += calendar.tagSort(term)
    } else {
        results += activities.hammingDistanceSort(searchTerm)
        results += pinned.searchTokenSort(searchTerm)
        results += static.searchTokenSort(searchTerm)
        results += dynamic.searchTokenSort(searchTerm)
        results += cached.searchTokenSort(searchTerm)
        results += contacts.searchTokenSort(searchTerm)
        results += calendar.searchTokenSort(searchTerm)
    }
    log.d("Search results build: ${results.size} results")
    return results
}

private fun <T : SearchResult> List<T>.searchTokenSort(searchTerm: String): List<T> =
    abstractSortBy(searchTerm) { it.searchTokens }

private fun <T : SearchResult> List<T>.tagSort(searchTerm: String): List<T> =
    abstractSortBy(searchTerm) { r -> r.tags.map { it.unlocalizedName } }

/** Sorts the search results by [selector], using [SearchResult.timesOpened] as a tie-breaker */
private inline fun <T : SearchResult> List<T>.abstractSortBy(
    searchTerm: String, crossinline selector: (T) -> List<String>,
): List<T> = filter { result ->
    selector(result).any { it.startsWith(searchTerm, ignoreCase = true) }
}.sortedWith(
    compareBy<T> { result ->
        selector(result).indexOfFirst { it.startsWith(searchTerm, ignoreCase = true) }
    }.thenByDescending { result -> result.timesOpened }
)

private fun <T : FavoritableSearchResult> List<T>.hammingDistanceSort(searchTerm: String): List<T> =
    map { result ->
        result to result.searchTokens.minOf {
            it.hammingDistanceTo(searchTerm, maxDistance = if (result.isFavorite) 1 else 0)
        }
    }.filter { (_, distance) ->
        distance != Int.MAX_VALUE
    }.sortedWith(
        compareBy<Pair<T, Int>> { (_, distance) -> distance }
            .thenBy { (result, distance) ->
                result.searchTokens.indexOfFirst {
                    it.hammingDistanceTo(searchTerm, maxDistance = distance) == distance
                }
            }.thenByDescending { (result, _) -> result.timesOpened }
    ).map { (result, _) -> result }

private fun String.hammingDistanceTo(searchTerm: String, maxDistance: Int): Int {
    if (this.length < searchTerm.length) return Int.MAX_VALUE
    var distance = 0
    for (i in 0..searchTerm.lastIndex) {
        if (!this[i].equals(searchTerm[i], ignoreCase = true)) {
            if (distance == maxDistance) return Int.MAX_VALUE
            distance++
        }
    }
    return distance
}

fun computeSpecialSearchResults(searchTerm: String): List<SearchResult> =
    if (hideSpecialSearchResults(searchTerm)) emptyList() else buildList {
        if (URLUtil.isValidUrl(searchTerm)) add(UrlSearchResult(searchTerm.toUri()))
        if (!searchTerm.equals("e", ignoreCase = true) &&
            !searchTerm.equals("inf", ignoreCase = true)
        ) { //Don't show the math result for the common search terms "e" and "inf"
            MathExpressionParser.parse(searchTerm)
                ?.let { add(MathSearchResult(it.result, it.formatArgs, it.successful)) }
        }
        UnitConversionParser.parse(searchTerm)
            ?.let { add(UnitConversionSearchResult(it.result, it.formatArgs, it.successful)) }
        MetronomeParser.parse(searchTerm)
            ?.let { add(MetronomeSearchResult(it)) }
    }

fun computeExternalSearchSearchResults(searchTerm: String, settings: Settings): List<SearchResult> =
    if (hideSpecialSearchResults(searchTerm)) emptyList() else listOfNotNull(
        WebSearch(searchTerm),
        WikipediaSearch(searchTerm),
        if (settings.appStoreSearchEnabled) AppStoreSearch(searchTerm) else null,
    )

private fun hideSpecialSearchResults(searchTerm: String): Boolean =
    searchTerm.isEmpty() || searchTerm[0] == '/' || searchTerm[0] == '#'
