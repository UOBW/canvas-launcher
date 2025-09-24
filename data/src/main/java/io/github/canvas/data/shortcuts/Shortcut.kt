package io.github.canvas.data.shortcuts

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Process
import androidx.annotation.RequiresApi
import io.github.canvas.data.HideableSearchResult
import io.github.canvas.data.RenameableSearchResult
import io.github.canvas.data.SearchResult
import io.github.canvas.data.StringLabelSearchResult
import io.github.canvas.data.Uid
import io.github.canvas.data.application
import io.github.canvas.data.icons.AdaptiveIcon
import io.github.canvas.data.launcherApps
import io.github.canvas.data.log

@RequiresApi(Build.VERSION_CODES.N_MR1)
data class Shortcut(
    /** The app that defined the shortcut */
    val packageName: String,
    /** The app specific id of the shortcut */
    val shortcutId: String,
    /** The type of the shortcut */
    val type: Type,
    /** A human-readable label for the shortcut */
    override val label: String,
    /** The icon of the shortcut */
    override val icon: AdaptiveIcon,
    /** The priority of the shortcut in the search results if this shortcut is static or dynamic, else 0 */
    val rank: Int,
    /** The icon of the app that defined the shortcut */
    override val badgeIcon: AdaptiveIcon,
    override val originalLabelOrNull: String?,
) : SearchResult, StringLabelSearchResult, RenameableSearchResult, HideableSearchResult {
    override val uid: Uid = shortcutUid(packageName, shortcutId)

    override val searchTokens: List<String> = label.split(' ')

    override fun open(context: Context, options: Bundle) {
        try {
            context.launcherApps.startShortcut(
                packageName, shortcutId,
                null, options, Process.myUserHandle()
            )
            log.d("Shortcut $this launched")
        } catch (exc: Exception) {
            log.e("Unable to launch shortcut $this, reloading cache", exc)
            context.application.shortcutsRepository.reloadAsync()
        }
    }

    fun unpinAsync(context: Context): Unit =
        context.application.shortcutsRepository.unpinShortcutAsync(this)

    override fun renameAsync(newName: String, context: Context): Unit =
        context.application.shortcutsRepository.renameAsync(this, newName)

    override fun setHiddenAsync(value: Boolean, context: Context): Unit =
        context.application.shortcutsRepository.setHiddenAsync(this, value)

    override fun toString(): String = "shortcut/$shortcutId ($label, $type)"

    enum class Type {
        /** The shortcut is static (defined in the manifest) */
        STATIC,

        /** The shortcut is dynamic (created at runtime) */
        DYNAMIC,

        /** The shortcut is pinned (added by the user) */
        PINNED,

        /** The shortcut is cached (whatever that means) */
        CACHED
    }
}

fun shortcutUid(packageName: String, shortcutId: String): Uid =
    Uid("shortcut/$packageName/$shortcutId")
