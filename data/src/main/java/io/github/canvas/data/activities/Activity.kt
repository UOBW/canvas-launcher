package io.github.canvas.data.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.Intent.createChooser
import android.net.Uri
import android.os.Bundle
import android.os.Process
import androidx.core.net.toUri
import io.github.canvas.data.FavoritableSearchResult
import io.github.canvas.data.HideableSearchResult
import io.github.canvas.data.IconChangeableSearchResult
import io.github.canvas.data.RenameableSearchResult
import io.github.canvas.data.SearchResult
import io.github.canvas.data.StringLabelSearchResult
import io.github.canvas.data.Tag
import io.github.canvas.data.TaggableSearchResult
import io.github.canvas.data.Uid
import io.github.canvas.data.application
import io.github.canvas.data.icons.AdaptiveIcon
import io.github.canvas.data.icons.CustomIcon
import io.github.canvas.data.launcherApps
import io.github.canvas.data.log
import io.github.canvas.data.unlocalizedName

private val CANVAS_SETTINGS =
    ComponentName("io.github.canvas", "io.github.canvas.ui.settings.SettingsActivity")

private val OPEN_STORE_URI_BASE = "market://details".toUri()
private val OPEN_IN_APP_MANAGER_URI_BASE = "app-manager://details".toUri()

data class Activity(
    val componentName: ComponentName,
    override val label: String,
    override val icon: AdaptiveIcon,
    val isSystemApp: Boolean,
    /** See https://developer.android.com/develop/ui/views/launch/shortcuts/creating-shortcuts#custom-pinned */
    val addShortcutActivity: ComponentName?,
    val hasWidgets: Boolean,
    override val originalLabelOrNull: String?,
    val isAppManagerInstalled: Boolean,
    override val isFavorite: Boolean,
    override val tags: Set<Tag>,
    override val timesOpened: Int,
) : SearchResult, StringLabelSearchResult, RenameableSearchResult, HideableSearchResult,
    FavoritableSearchResult,
    IconChangeableSearchResult, TaggableSearchResult {
    override val uid: Uid = activityUid(componentName)

    override val searchTokens: List<String> = label.split(' ') + tags.map { it.unlocalizedName }

    override fun open(context: Context, options: Bundle) {
        context.launcherApps.startMainActivity(
            componentName,
            Process.myUserHandle(),
            null,
            options
        )
        context.application.activitiesRepository.increaseTimesOpenedAsync(this)
        log.d("Activity $this launched")
    }

    fun openAppInfo(context: Context) {
        context.launcherApps.startAppDetailsActivity(
            componentName, Process.myUserHandle(), null, Bundle.EMPTY
        )
        log.d("Opened app details for activity $this")
    }

    fun openStorePage(context: Context) {
        val uri = OPEN_STORE_URI_BASE.buildUpon()
            .appendQueryParameter("id", componentName.packageName).build()
        context.startActivity(createChooser(Intent(ACTION_VIEW, uri), null))
        log.d("Opened store page for activity $this")
    }

    fun openInAppManger(context: Context) {
        context.startActivity(
            Intent(ACTION_VIEW).apply {
                `package` = "io.github.muntashirakon.AppManager"
                data = OPEN_IN_APP_MANAGER_URI_BASE.buildUpon()
                    .appendQueryParameter("id", this@Activity.componentName.packageName).build()
            }
        )
        log.d("Opened App Manager for activity $this")
    }

    fun uninstall(context: Context) {
        val intent = Intent(Intent.ACTION_DELETE)
        intent.data = Uri.fromParts("package", componentName.packageName, null)
        context.startActivity(intent)
        log.d("Uninstall of activity $this requested")
    }

    override fun renameAsync(newName: String, context: Context): Unit =
        context.application.activitiesRepository.renameAsync(this, newName)

    // Prevent the user from locking themselves out of the settings
    override val isHideable: Boolean get() = componentName != CANVAS_SETTINGS

    override fun setHiddenAsync(value: Boolean, context: Context): Unit =
        context.application.activitiesRepository.setHiddenAsync(this, value)

    override fun setIsFavoriteAsync(starred: Boolean, context: Context) {
        context.application.activitiesRepository.setStarredAsync(this, starred)
    }

    override fun setIconAsync(icon: CustomIcon?, context: Context) {
        context.application.activitiesRepository.setIconAsync(this, icon)
    }

    override suspend fun loadDefaultIcon(context: Context): AdaptiveIcon =
        context.application.activitiesRepository.loadDefaultIcon(this)

    override fun setTagsAsync(tags: Set<Tag>, context: Context) {
        context.application.activitiesRepository.setTagsAsync(this, tags)
    }

    override fun toString(): String =
        "activity/${componentName.packageName}/${componentName.className} ($label)"
}

internal fun activityUid(componentName: ComponentName): Uid =
    Uid("activity/${componentName.packageName}/${componentName.className}")
