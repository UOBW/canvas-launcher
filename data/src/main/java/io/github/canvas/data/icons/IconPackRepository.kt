package io.github.canvas.data.icons

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Resources
import androidx.compose.ui.graphics.Color
import io.github.canvas.data.background
import io.github.canvas.data.io
import io.github.canvas.data.listener
import io.github.canvas.data.log
import io.github.canvas.data.repositoryCoroutineScope
import io.github.canvas.data.settings.SettingsRepository
import io.github.canvas.data.toComposeColor
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.xmlpull.v1.XmlPullParser.END_DOCUMENT
import org.xmlpull.v1.XmlPullParser.END_TAG
import org.xmlpull.v1.XmlPullParser.START_DOCUMENT
import org.xmlpull.v1.XmlPullParser.START_TAG
import org.xmlpull.v1.XmlPullParser.TEXT
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

class IconPackRepository internal constructor(
    private val pm: PackageManager,
    launcherApps: LauncherApps,
    settingsRepository: SettingsRepository,
) {
    // public methods are suspend and switch to this coroutineScope, private methods aren't
    private val coroutineScope = repositoryCoroutineScope()

    val availableIconPacks: StateFlow<List<AvailableIconPack>?> = launcherApps.listener.map {
        io {
            pm.queryIntentActivities(Intent("org.adw.launcher.THEMES"), 0).map { pack ->
                AvailableIconPack(
                    packageName = pack.activityInfo.packageName,
                    name = pack.loadLabel(pm).toString(),
                    icon = pack.loadIcon(pm).toAdaptiveIcon()
                )
            }
        }
    }.stateIn(coroutineScope, started = WhileSubscribed(), initialValue = null)

    val currentIconPack: StateFlow<IconPack?> = combine(
        launcherApps.listener,
        settingsRepository.settings.map { it.iconPack }.distinctUntilChanged()
    ) { _, iconPack ->
        if (iconPack != null) {
            loadIconPack(iconPack)
        } else null
    }.stateIn(coroutineScope, started = Eagerly, initialValue = null)

    @SuppressLint("DiscouragedApi")
    private suspend fun loadIconPack(packageName: String): IconPack {
        val resources = getResourcesForApp(packageName)
            ?: return EmptyIconPack.also { log.e("Failed to load icon pack $packageName: package not found") }

        // Use a hash map instead of a linked hash map to save memory
        val icons = hashMapOf<ComponentName, String>()
        val calendarIcons = hashMapOf<ComponentName, String>()
        val appfilterId = io { resources.getIdentifier("appfilter", "xml", packageName) }
        if (appfilterId == 0) {
            log.e("Failed to load icon pack $packageName: appfilter.xml not found")
            return EmptyIconPack
        }
        try {
            io {
                resources.getXml(appfilterId).use { parser ->
                    while (true) {
                        when (val tagType = parser.next()) {
                            START_TAG -> {
                                val name = parser.name
                                when (name) {
                                    "item" -> {
                                        val activity = parser.getAttributeValue(null, "component")
                                            ?.removePrefix("ComponentInfo{")?.removeSuffix("}")
                                            ?.let { ComponentName.unflattenFromString(it) }
                                            ?: run {
                                                log.w(
                                                    "Error while loading icon pack $packageName: couldn't parse activity name " +
                                                            parser.getAttributeValue(
                                                                null,
                                                                "component"
                                                            )
                                                )
                                                continue
                                            }
                                        icons[activity] = parser.getAttributeValue(null, "drawable")
                                            ?: run {
                                                log.w("Error while loading icon pack $packageName: drawable attribute of $activity not found")
                                                continue
                                            }
                                    }

                                    "calendar" -> {
                                        val activityName =
                                            parser.getAttributeValue(null, "component")
                                                .removePrefix("ComponentInfo{").removeSuffix("}")
                                        val activity =
                                            ComponentName.unflattenFromString(activityName)
                                        if (activity == null) {
                                            log.w("Error while loading icon pack $packageName: couldn't parse activity name $activityName")
                                            continue
                                        }
                                        calendarIcons[activity] =
                                            parser.getAttributeValue(null, "prefix")
                                    }

                                    "resources" -> {} // Top level resources tag, ignore

                                    else -> log.w("Encountered unknown tag in appfilter.xml: $name")
                                }
                            }

                            START_DOCUMENT, END_TAG, TEXT -> {}
                            END_DOCUMENT -> break
                            else -> error("Unknown event type $tagType")
                        }
                    }
                }
            }
        } catch (e: IOException) {
            log.e("Failed to load icon pack $packageName", e)
            return EmptyIconPack
        } catch (e: XmlPullParserException) {
            log.e("Failed to load icon pack $packageName", e)
            return EmptyIconPack
        }

        val backgroundColor = getDefaultBackgroundColor(packageName, resources)

        log.d("Icon pack $packageName loaded: ${icons.size} icons, ${calendarIcons.size} calendar icons")
        return IconPackImpl(
            packageName, icons, calendarIcons, resources, backgroundColor
        )
    }

    suspend fun loadIconPack(iconPack: AvailableIconPack): IconPack =
        background { loadIconPack(iconPack.packageName) }

    private suspend fun getResourcesForApp(packageName: String) = try {
        io { pm.getResourcesForApplication(packageName) }
    } catch (_: NameNotFoundException) {
        null
    }

    /** Some icon packs specify a background color, but other launchers seem to ignore it */
    @SuppressLint("DiscouragedApi")
    private suspend fun getDefaultBackgroundColor(
        packageName: String, resources: Resources,
    ): Color? = io {
        val backgroundColorId =
            resources.getIdentifier("icon_background_color", "color", packageName)
        return@io if (backgroundColorId != 0) {
            resources.getColor(backgroundColorId, null).toComposeColor()
        } else null
    }

    suspend fun loadCustomIcon(customIcon: CustomIcon): AdaptiveIcon? = background {
        val (packageName, icon) = customIcon
        val resources = getResourcesForApp(packageName)
            ?: return@background null.also { log.e("Failed to load icon $icon from icon pack $packageName: package not found") }
        return@background loadIconByName(
            packageName, icon,
            resources = resources,
            backgroundColor = getDefaultBackgroundColor(packageName, resources),
        ).also { if (it == null) log.e("Failed to load icon $icon from icon pack $packageName: icon not found") }
    }

    companion object {
        @SuppressLint("DiscouragedApi")
        private suspend fun loadIconByName(
            packageName: String, icon: String, resources: Resources, backgroundColor: Color?,
        ): AdaptiveIcon? = io {
            val id = resources.getIdentifier(icon, "drawable", packageName)
            return@io if (id == 0) null else resources.getDrawable(id, null)
        }?.toAdaptiveIcon(fallbackBackground = backgroundColor)
    }

    private class IconPackImpl(
        val packageName: String,
        val icons: Map<ComponentName, String>,
        val calendarIcons: Map<ComponentName, String>,
        val resources: Resources,
        val backgroundColor: Color?,
    ) : IconPack {
        @SuppressLint("DiscouragedApi")
        override suspend fun loadIcon(
            activity: ComponentName, dayOfMonth: Int,
        ): AdaptiveIcon? = background {
            val name = calendarIcons[activity]?.let { it + dayOfMonth }
                ?: icons[activity]
                ?: return@background null
            return@background loadIconByName(packageName, name, resources, backgroundColor)
                .also { if (it == null) log.e("Failed to load icon for ${activity.flattenToShortString()} from $packageName: drawable not found") }
        }

        override suspend fun loadIconByName(name: String): AdaptiveIcon? = background {
            loadIconByName(packageName, name, resources, backgroundColor)
                .also { if (it == null) log.e("Failed to load icon $name from $packageName: drawable not found") }
        }

        override suspend fun listIcons(): List<String> = background { icons.values.distinct() }
    }

    private object EmptyIconPack : IconPack {
        override suspend fun loadIcon(activity: ComponentName, dayOfMonth: Int) = null
        override suspend fun listIcons(): List<String> = emptyList()
        override suspend fun loadIconByName(name: String) = null
    }
}

data class AvailableIconPack(
    val packageName: String,
    val name: String,
    val icon: AdaptiveIcon,
)

interface IconPack {
    /** Load an icon from this icon pack, choosing dynamically based on [dayOfMonth] if available */
    suspend fun loadIcon(activity: ComponentName, dayOfMonth: Int): AdaptiveIcon?

    /** Returns a list of all the available icons in the icon pack, use [loadIconByName] to load them */
    suspend fun listIcons(): List<String>
    suspend fun loadIconByName(name: String): AdaptiveIcon?
}
