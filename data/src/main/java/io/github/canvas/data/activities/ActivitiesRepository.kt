package io.github.canvas.data.activities

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import io.github.canvas.data.DateRepository
import io.github.canvas.data.Tag
import io.github.canvas.data.database.ActivityCustomization
import io.github.canvas.data.database.ActivityCustomizationDao
import io.github.canvas.data.hasFlagSet
import io.github.canvas.data.icons.AdaptiveIcon
import io.github.canvas.data.icons.CustomIcon
import io.github.canvas.data.icons.IconPack
import io.github.canvas.data.icons.IconPackRepository
import io.github.canvas.data.icons.toAdaptiveIcon
import io.github.canvas.data.io
import io.github.canvas.data.isPackageInstalled
import io.github.canvas.data.listener
import io.github.canvas.data.log
import io.github.canvas.data.originalLabel
import io.github.canvas.data.repositoryCoroutineScope
import io.github.canvas.data.settings.SettingsRepository
import io.github.canvas.data.sortByLabelAndStar
import io.github.canvas.data.widgets.WidgetsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val CANVAS_LAUNCHER =
    ComponentName("io.github.canvas", "io.github.canvas.ui.launcher.LauncherActivity")
private const val APP_MANAGER = "io.github.muntashirakon.AppManager"

@SuppressLint("QueryPermissionsNeeded")
class ActivitiesRepository internal constructor(
    private val launcherApps: LauncherApps,
    private val pm: PackageManager,
    private val customizationDao: ActivityCustomizationDao,
    private val iconPackRepository: IconPackRepository,
    private val widgetsRepository: WidgetsRepository,
    private val dateRepository: DateRepository,
    private val settingsRepository: SettingsRepository,
) {
    private val coroutineScope = repositoryCoroutineScope()

    private val _activities: MutableStateFlow<List<Activity>?> = MutableStateFlow(null)
    val activities: StateFlow<List<Activity>?> = _activities.asStateFlow()

    private val _hiddenActivities: MutableStateFlow<List<Activity>> = MutableStateFlow(emptyList())
    val hiddenActivities: StateFlow<List<Activity>> = _hiddenActivities.asStateFlow()

    private val systemApps: List<String> =
        pm.getInstalledApplications(PackageManager.MATCH_SYSTEM_ONLY).map { it.packageName }

    private suspend fun reload(
        customizations: Map<String, ActivityCustomization>,
        iconPack: IconPack?,
        dayOfMonth: Int,
    ) {
        val nonHiddenActivities = mutableListOf<Activity>()
        val hiddenActivities = mutableListOf<Activity>()

        val activityList = io { launcherApps.getActivityList(null, Process.myUserHandle()) }

        val shortcutConfigActivities = io {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                launcherApps.hasShortcutHostPermission()
            ) {
                launcherApps.getShortcutConfigActivityList(null, Process.myUserHandle())
                    .associateBy(
                        keySelector = { it.componentName.packageName },
                        valueTransform = { it.componentName }
                    )
            } else emptyMap<String, ComponentName>()
        }

        val isAppManagerInstalled = pm.isPackageInstalled(APP_MANAGER)

        for (a in activityList) {
            if (a.componentName == CANVAS_LAUNCHER) continue

            val customization = customizations[activityUid(a.componentName).string]

            val activity = Activity(
                componentName = a.componentName,
                label = customization?.label ?: a.label.toString(),
                originalLabelOrNull = if (customization?.label != null) a.label.toString() else null,
                icon = customization?.customIcon?.let { iconPackRepository.loadCustomIcon(it) }
                    ?: loadDefaultIcon(a, iconPack, dayOfMonth),
                isSystemApp = a.componentName.packageName in systemApps,
                addShortcutActivity = shortcutConfigActivities[a.componentName.packageName],
                hasWidgets = widgetsRepository.hasWidgets(a.componentName.packageName),
                isAppManagerInstalled = isAppManagerInstalled,
                isFavorite = customization?.starred == true,
                tags = customization?.tags ?: setOfNotNull(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        when (a.applicationInfo.category) {
                            ApplicationInfo.CATEGORY_GAME -> Tag.Builtin.GAME
                            ApplicationInfo.CATEGORY_AUDIO -> Tag.Builtin.AUDIO
                            ApplicationInfo.CATEGORY_VIDEO -> Tag.Builtin.VIDEO
                            ApplicationInfo.CATEGORY_IMAGE -> Tag.Builtin.IMAGE
                            ApplicationInfo.CATEGORY_SOCIAL -> Tag.Builtin.SOCIAL
                            ApplicationInfo.CATEGORY_NEWS -> Tag.Builtin.NEWS
                            ApplicationInfo.CATEGORY_MAPS -> Tag.Builtin.MAPS
                            ApplicationInfo.CATEGORY_PRODUCTIVITY -> Tag.Builtin.PRODUCTIVITY
                            ApplicationInfo.CATEGORY_ACCESSIBILITY -> Tag.Builtin.ACCESSIBILITY
                            ApplicationInfo.CATEGORY_UNDEFINED ->
                                @Suppress("DEPRECATION") // Fallback for apps that haven't updated yet
                                if (a.applicationInfo.flags hasFlagSet ApplicationInfo.FLAG_IS_GAME)
                                    Tag.Builtin.GAME else null

                            else -> error("Unknown application category ${a.applicationInfo.category}")
                        }
                    } else {
                        @Suppress("DEPRECATION") // Only option on this api level
                        if (a.applicationInfo.flags hasFlagSet ApplicationInfo.FLAG_IS_GAME)
                            Tag.Builtin.GAME else null
                    }
                ),
                timesOpened = customization?.timesOpened ?: 0
            )

            if (customization?.hidden == true) {
                hiddenActivities += activity
            } else {
                nonHiddenActivities += activity
            }
        }
        _activities.value = nonHiddenActivities.sortByLabelAndStar()
        _hiddenActivities.value = hiddenActivities.sortByLabelAndStar()
        log.d("Activity list rebuilt, ${nonHiddenActivities.size} activities, ${hiddenActivities.size} hidden activities")
    }

    init {
        coroutineScope.launch {
            combine(
                launcherApps.listener,
                customizationDao.all(),
                iconPackRepository.currentIconPack,
                dateRepository.dayOfMonth
            ) { _, customizations, iconPack, dayOfMonth ->
                Triple(customizations, iconPack, dayOfMonth)
            }.collectLatest { (customization, iconPack, dayOfMonth) ->
                reload(customization, iconPack, dayOfMonth)
            }
        }

        coroutineScope.launch {
            if (!settingsRepository.settings.first().sortResultsByUsage) {
                deleteUsageData() // In case the process got killed before getting a chance to delete usage data
            }
        }

        log.d("Activities repository initialized")
    }

    private suspend fun loadDefaultIcon(
        activity: LauncherActivityInfo,
        iconPack: IconPack?, dayOfMonth: Int,
    ): AdaptiveIcon = io {
        iconPack?.loadIcon(activity.componentName, dayOfMonth)
            ?: activity.getBadgedIcon(0).toAdaptiveIcon()
    }

    private suspend fun rename(activity: Activity, newName: String) {
        val customization = customizationDao.get(activity.uid.string).copy(
            label = if (activity.originalLabel == newName) null else newName
        )
        customizationDao.update(customization)
        log.d("Activity $activity renamed to $newName")
    }

    fun renameAsync(activity: Activity, newName: String) {
        coroutineScope.launch { rename(activity, newName) }
    }

    private suspend fun setHidden(activity: Activity, hidden: Boolean) {
        val customization = customizationDao.get(activity.uid.string).copy(hidden = hidden)
        customizationDao.update(customization)
        log.d("Activity $activity set hidden $hidden")
    }

    fun setHiddenAsync(activity: Activity, hidden: Boolean) {
        coroutineScope.launch { setHidden(activity, hidden) }
    }

    private suspend fun setStarred(activity: Activity, starred: Boolean) {
        val customization = customizationDao.get(activity.uid.string).copy(starred = starred)
        customizationDao.update(customization)
        log.d("Activity $activity set starred $starred")
    }

    fun setStarredAsync(activity: Activity, starred: Boolean) {
        coroutineScope.launch { setStarred(activity, starred) }
    }

    private suspend fun setIcon(activity: Activity, icon: CustomIcon?) {
        val customization = customizationDao.get(activity.uid.string)
            .copy(customIcon_iconPack = icon?.iconPack, customIcon_drawable = icon?.drawable)
        customizationDao.update(customization)
        log.d("Activity $activity changed icon to $icon")
    }

    fun setIconAsync(activity: Activity, icon: CustomIcon?) {
        coroutineScope.launch { setIcon(activity, icon) }
    }

    suspend fun loadDefaultIcon(activity: Activity): AdaptiveIcon = io {
        loadDefaultIcon(
            activity = launcherApps.getActivityList(
                activity.componentName.packageName, Process.myUserHandle()
            ).single { it.componentName == activity.componentName },
            iconPack = iconPackRepository.currentIconPack.value,
            dayOfMonth = dateRepository.dayOfMonth.value,
        )
    }

    private suspend fun setTags(activity: Activity, tags: Set<Tag>) {
        val customization = customizationDao.get(activity.uid.string).copy(tags = tags)
        customizationDao.update(customization)
        log.d("Activity $activity changed tags to $tags")
    }

    fun setTagsAsync(activity: Activity, tags: Set<Tag>) {
        coroutineScope.launch { setTags(activity, tags) }
    }

    private suspend fun increaseTimesOpened(activity: Activity) {
        if (!settingsRepository.settings.first().sortResultsByUsage) return
        val customization = customizationDao.get(activity.uid.string)
        customizationDao.update(customization.copy(timesOpened = customization.timesOpened + 1))
    }

    fun increaseTimesOpenedAsync(activity: Activity) {
        coroutineScope.launch { increaseTimesOpened(activity) }
    }

    private suspend fun deleteUsageData() = customizationDao.deleteUsageData()

    fun deleteUsageDataAsync() {
        coroutineScope.launch {
            deleteUsageData()
        }
    }
}
