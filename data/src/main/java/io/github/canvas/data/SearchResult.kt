package io.github.canvas.data

import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.compose.runtime.saveable.SaverScope
import io.github.canvas.data.icons.AdaptiveIcon
import io.github.canvas.data.icons.CustomIcon
import java.lang.String.CASE_INSENSITIVE_ORDER

/**
 * Any result for a search in Canvas Launcher
 *
 * Subclasses have to implement one of [StringLabelSearchResult], [ResIdLabelSearchResult], or [FormattedResIdLabelSearchResult].
 *
 * By default, the search result is rendered using its icon and text, but subclasses can implement
 * [CustomRendererSearchResult] to indicate that they provide their own renderer in the UI layer.
 *
 * Additionally, subclasses are free to implement any of the _-ableSearchResult_ interfaces to
 * indicate that they support additional features.
 */
interface SearchResult {
    /** Opens this search result (optional, may do nothing) */
    fun open(context: Context, options: Bundle = Bundle.EMPTY)

    /** See [Uid] */
    val uid: Uid

    val icon: AdaptiveIcon
    val badgeIcon: AdaptiveIcon? get() = null

    /** Whether this search result is used to display an error */
    val isError: Boolean get() = false

    /** A list of terms under which this SearchResult can be found, ordered by relevance */
    val searchTokens: List<String>

    /** A list of tags that can be searched for by appending # to the search term */
    val tags: Set<Tag> get() = emptySet()

    val isFavorite: Boolean get() = false

    /** The number of times this result has been opened from Canvas Launcher (used for sorting results), or -1 if not implemented */
    val timesOpened: Int get() = -1
}

/**
 * Uniquely identifies a SearchResult
 *
 * Format: `type/id`:
 * * type: constant string such as `activity` or `contact`
 * * id: type specific, for example `packageName/activityName`
 *
 * There should never be two SearchResults with the same uid, but the uid also shouldn't change between reloads.
 */
@JvmInline
value class Uid(val string: String) {
    override fun toString(): String = string

    object Saver : androidx.compose.runtime.saveable.Saver<Uid?, Any> {
        override fun SaverScope.save(value: Uid?): Any = value?.string ?: false
        override fun restore(value: Any): Uid? = if (value is String) Uid(value) else null
    }
}

/** Marker interface to indicate that this search result has its own renderer in the UI layer */
interface CustomRendererSearchResult : SearchResult

interface StringLabelSearchResult : SearchResult {
    val label: String
}

interface ResIdLabelSearchResult : SearchResult {
    @get:StringRes
    val label: Int
}

interface FormattedResIdLabelSearchResult : SearchResult {
    @get:StringRes
    val label: Int
    val formatArgs: List<Any>
}

interface RenameableSearchResult : SearchResult, StringLabelSearchResult {
    /** Rename this search result */
    fun renameAsync(newName: String, context: Context)

    /** The label before it was renamed, or null if this search result hasn't been renamed. Should not be the same as `label`. */
    val originalLabelOrNull: String?
}

val RenameableSearchResult.originalLabel: String get() = originalLabelOrNull ?: label

/** A search result that can have a custom icon */
interface IconChangeableSearchResult : SearchResult {
    fun setIconAsync(icon: CustomIcon?, context: Context)
    suspend fun loadDefaultIcon(context: Context): AdaptiveIcon
}

interface HideableSearchResult : SearchResult {
    fun setHiddenAsync(value: Boolean, context: Context)
    val isHideable: Boolean get() = true
}

/** A search result that can be starred, meaning that it has higher priority and uses approximate string matching */
interface FavoritableSearchResult : SearchResult {
    fun setIsFavoriteAsync(starred: Boolean, context: Context)
}

fun <T : StringLabelSearchResult> MutableList<T>.sortByLabel(): MutableList<T> =
    apply { sortBy(CASE_INSENSITIVE_ORDER) { it.label } }

fun <T : StringLabelSearchResult> List<T>.sortedByLabel(): List<T> =
    sortedBy(CASE_INSENSITIVE_ORDER) { it.label }

fun <T> MutableList<T>.sortByLabelAndStar(): MutableList<T> where T : StringLabelSearchResult, T : FavoritableSearchResult =
    apply { sortWith(compareByDescending<T> { it.isFavorite }.thenBy(CASE_INSENSITIVE_ORDER) { it.label }) }

operator fun List<SearchResult>.get(uid: Uid): SearchResult? = find { it.uid == uid }
