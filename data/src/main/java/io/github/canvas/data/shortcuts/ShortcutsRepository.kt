package io.github.canvas.data.shortcuts

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.PinItemRequest
import android.content.pm.LauncherApps.ShortcutQuery.FLAG_GET_KEY_FIELDS_ONLY
import android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_CACHED
import android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
import android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
import android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutInfo.SURFACE_LAUNCHER
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES.N_MR1
import android.os.Build.VERSION_CODES.O
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Process
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi
import io.github.canvas.data.DateRepository
import io.github.canvas.data.Quintuple
import io.github.canvas.data.R
import io.github.canvas.data.background
import io.github.canvas.data.combine
import io.github.canvas.data.contacts.Contact
import io.github.canvas.data.contacts.ContactsRepository
import io.github.canvas.data.database.ShortcutCustomization
import io.github.canvas.data.database.ShortcutCustomizationDao
import io.github.canvas.data.firstNonNull
import io.github.canvas.data.icons.AdaptiveIcon
import io.github.canvas.data.icons.IconPack
import io.github.canvas.data.icons.IconPackRepository
import io.github.canvas.data.icons.MissingIcon
import io.github.canvas.data.icons.toAdaptiveIcon
import io.github.canvas.data.io
import io.github.canvas.data.listener
import io.github.canvas.data.log
import io.github.canvas.data.originalLabel
import io.github.canvas.data.repositoryCoroutineScope
import io.github.canvas.data.settings.SettingsRepository
import io.github.canvas.data.shortcuts.Shortcut.Type.CACHED
import io.github.canvas.data.shortcuts.Shortcut.Type.DYNAMIC
import io.github.canvas.data.shortcuts.Shortcut.Type.PINNED
import io.github.canvas.data.shortcuts.Shortcut.Type.STATIC
import io.github.canvas.data.sortByLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.os.Build.VERSION_CODES.R as ANDROID_R

class ShortcutsRepository internal constructor(
    private val launcherApps: LauncherApps,
    private val pm: PackageManager,
    private val customizationDao: ShortcutCustomizationDao,
    private val contactsRepository: ContactsRepository,
    private val settingsRepository: SettingsRepository,
    private val iconPackRepository: IconPackRepository,
    private val dateRepository: DateRepository,
) {
    private val coroutineScope = repositoryCoroutineScope()

    private val _staticShortcuts: MutableStateFlow<List<Shortcut>?> = MutableStateFlow(null)
    val staticShortcuts: StateFlow<List<Shortcut>?> = _staticShortcuts.asStateFlow()

    private val _dynamicShortcuts: MutableStateFlow<List<Shortcut>?> = MutableStateFlow(null)
    val dynamicShortcuts: StateFlow<List<Shortcut>?> = _dynamicShortcuts.asStateFlow()

    private val _pinnedShortcuts: MutableStateFlow<List<Shortcut>?> = MutableStateFlow(null)
    val pinnedShortcuts: StateFlow<List<Shortcut>?> = _pinnedShortcuts.asStateFlow()

    private val _cachedShortcuts: MutableStateFlow<List<Shortcut>?> = MutableStateFlow(null)
    val cachedShortcuts: StateFlow<List<Shortcut>?> = _cachedShortcuts.asStateFlow()

    private val _hiddenShortcuts: MutableStateFlow<List<Shortcut>> = MutableStateFlow(emptyList())
    val hiddenShortcuts: StateFlow<List<Shortcut>> = _hiddenShortcuts.asStateFlow()

    /** Reloads the list of shortcuts from the OS, blocking until the values have been emitted to the flows */
    private suspend fun reload(
        customizations: Map<String, ShortcutCustomization>,
        contacts: List<Contact>, hideContactShortcuts: Boolean,
        iconPack: IconPack?,
        dayOfMonth: Int,
    ) {
        if (VERSION.SDK_INT < N_MR1) {
            log.d("Unable to load app shortcuts: not supported below API level 25")
            return
        }

        if (io { !launcherApps.hasShortcutHostPermission() }) {
            log.w("Failed to load app shortcuts: no permission")
            _staticShortcuts.value = emptyList()
            _dynamicShortcuts.value = emptyList()
            _pinnedShortcuts.value = emptyList()
            _cachedShortcuts.value = emptyList()
            _hiddenShortcuts.value = emptyList()
            return
        }

        val static = mutableListOf<Shortcut>()
        val dynamic = mutableListOf<Shortcut>()
        val pinned = mutableListOf<Shortcut>()
        val cached = mutableListOf<Shortcut>()
        val hidden = mutableListOf<Shortcut>()

        io {
            launcherApps.getShortcuts(
                LauncherApps.ShortcutQuery().setQueryFlags(
                    FLAG_MATCH_MANIFEST or
                            FLAG_MATCH_DYNAMIC or
                            FLAG_MATCH_PINNED or
                            if (VERSION.SDK_INT >= ANDROID_R) FLAG_MATCH_CACHED else 0
                ), Process.myUserHandle()
            )
        }?.forEach { s ->
            if (VERSION.SDK_INT >= TIRAMISU && s.isExcludedFromSurfaces(SURFACE_LAUNCHER)) return@forEach
            if (!s.isEnabled) {
                log.i("Unpinning disabled shortcut $s")
                unpinShortcut(s.`package`, s.id)
                return@forEach
            }

            val customization = customizations[shortcutUid(s.`package`, s.id).string]
            val shortcut = s.toShortcut(
                customName = customization?.label,
                customBadgeIcon = s.activity?.let { iconPack?.loadIcon(it, dayOfMonth) }
            )

            if (
                hideContactShortcuts &&
                (shortcut.type == DYNAMIC || shortcut.type == CACHED) &&
                contacts.any { it.originalLabel == shortcut.label }
            ) return@forEach

            when (customization?.hidden) {
                true -> hidden += shortcut
                else -> when (shortcut.type) {
                    STATIC -> static += shortcut
                    DYNAMIC -> dynamic += shortcut
                    PINNED -> pinned += shortcut
                    CACHED -> cached += shortcut
                }
            }
        }

        _staticShortcuts.value = static.sortByLabel()
        _dynamicShortcuts.value = dynamic.sortByLabel()
        _pinnedShortcuts.value = pinned.sortByLabel()
        _cachedShortcuts.value = cached.sortByLabel()
        _hiddenShortcuts.value = hidden.sortByLabel()
        log.d("Shortcuts list rebuilt: ${static.size} static, ${dynamic.size} dynamic, ${pinned.size} pinned & ${cached.size} cached")
    }


    fun reloadAsync() {
        coroutineScope.launch {
            reload(
                customizations = customizationDao.all().first(),
                contacts = contactsRepository.all.value,
                hideContactShortcuts = settingsRepository.settings.first().hideContactShortcuts,
                iconPack = iconPackRepository.currentIconPack.value,
                dayOfMonth = dateRepository.dayOfMonth.value
            )
        }
    }

    init {
        if (VERSION.SDK_INT < N_MR1) {
            log.d("Unable to load app shortcuts: not supported below API level 25")
        } else {
            coroutineScope.launch {
                combine(
                    launcherApps.listener,
                    customizationDao.all(),
                    contactsRepository.all,
                    settingsRepository.settings.map { it.hideContactShortcuts }
                        .distinctUntilChanged(),
                    iconPackRepository.currentIconPack,
                    dateRepository.dayOfMonth
                ) { _, customizations, contacts, hideContactShortcuts, iconPack, dayOfMonth ->
                    Quintuple(customizations, contacts, hideContactShortcuts, iconPack, dayOfMonth)
                }.collectLatest { (customizations, contacts, hideContactShortcuts, iconPack, dayOfMonth) ->
                    reload(customizations, contacts, hideContactShortcuts, iconPack, dayOfMonth)
                }
            }
        }
        log.d("Shortcut repository initialized")
    }

    @RequiresApi(O)
    suspend fun processShortcutPinRequest(
        intent: Intent?, context: Context,
    ): PinRequest? = background {
        if (intent == null) {
            log.e("Unable to pin shortcut: intent is null")
            return@background null
        }

        val pinRequest = launcherApps.getPinItemRequest(intent)
        if (pinRequest == null) {
            log.e("Unable to pin shortcut: intent does not contain a pin request")
            return@background null
        }

        val shortcutInfo = pinRequest.shortcutInfo
        if (shortcutInfo == null) {
            log.e("Unable to pin shortcut: wrong pin request type ${pinRequest.requestType}, expected REQUEST_TYPE_SHORTCUT")
            return@background null
        }

        val shortcut = shortcutInfo.toShortcut(type = PINNED)
        if (pinnedShortcuts.value?.any { it.uid == shortcut.uid } == true) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, R.string.pin_shortcut_already_pinned, Toast.LENGTH_LONG)
                    .show()
            }
            log.w("Unable to pin shortcut $shortcut: already pinned")
            return@background null
        }

        return@background PinRequest(pinRequest, shortcut)
    }

    fun onShortcutCreationActivityResult(result: Intent?, context: Context) {
        coroutineScope.launch {
            if (VERSION.SDK_INT < O) error("Reached code thought to be unreachable on this API version")
            log.d("Shortcut creation activity finished")

            val pinRequest = processShortcutPinRequest(result, context)
                ?: return@launch // Logging is already done by processShortcutPinRequest()

            acceptPinRequest(pinRequest)
        }
    }

    @RequiresApi(O)
    suspend fun acceptPinRequest(pinRequest: PinRequest): Unit = io {
        if (pinRequest.pinItemRequest.accept()) {
            reloadAsync()
            log.d("New shortcut pinned: ${pinRequest.shortcut}")
        } else {
            log.i("Unable to pin shortcut ${pinRequest.shortcut}: request canceled")
        }
    }

    /** Tests whether a shortcut of the same app with the same label is already pinned */
    @RequiresApi(N_MR1)
    suspend fun similarPinnedShortcutExists(shortcut: Shortcut): Boolean = background {
        pinnedShortcuts.firstNonNull().any {
            it.type == PINNED && it.packageName == shortcut.packageName && it.label == shortcut.label
        }
    }

    /** Unpins the pinned shortcut */
    @RequiresApi(N_MR1)
    fun unpinShortcutAsync(shortcut: Shortcut) {
        coroutineScope.launch {
            if (shortcut.type == PINNED) {
                unpinShortcut(shortcut.packageName, shortcut.shortcutId)
            } else log.e("Failed to unpin shortcut $shortcut: not a pinned shortcut")
        }
    }

    @RequiresApi(N_MR1)
    private suspend fun unpinShortcut(packageName: String, shortcutId: String) {
        if (io { !launcherApps.hasShortcutHostPermission() }) {
            log.e("Failed to unpin shortcut: permission denied")
            return
        }
        // Get all shortcuts of the app and filter out the short
        val oldShortcuts = io {
            launcherApps.getShortcuts(
                LauncherApps.ShortcutQuery()
                    .setPackage(packageName)
                    .setQueryFlags(FLAG_MATCH_PINNED or FLAG_GET_KEY_FIELDS_ONLY),
                Process.myUserHandle()
            )
        }?.map { it.id }
        if (oldShortcuts == null) {
            log.e("Failed to unpin shortcut $packageName/$shortcutId: LauncherApps::getShortcuts() returned null despite having the shortcut host permission")
            return
        }
        val newShortcuts = oldShortcuts.filterNot { it == shortcutId }
        if (newShortcuts.size == oldShortcuts.size) {
            log.e("Failed to unpin shortcut $packageName/$shortcutId: shortcut does not exist")
            return
        }
        //Set the list of shortcuts of the app to all previously pinned shortcuts except the unpinned one
        io { launcherApps.pinShortcuts(packageName, newShortcuts, Process.myUserHandle()) }
        log.d("Successfully unpinned shortcut $packageName/$shortcutId")
        reloadAsync()
    }

    /** Creates a new Shortcut object with the data from the ShortcutInfo and the icon fetched using the LauncherApps API */
    @RequiresApi(N_MR1)
    private suspend fun ShortcutInfo.toShortcut(
        type: Shortcut.Type = when {
            isDeclaredInManifest -> STATIC
            isDynamic -> DYNAMIC
            isPinned -> PINNED
            (VERSION.SDK_INT >= ANDROID_R) && isCached -> CACHED
            else -> error("Unknown shortcut type ($this)")
        },
        customName: String? = null,
        customBadgeIcon: AdaptiveIcon? = null,
    ): Shortcut {
        val label = longLabel?.toString()
            ?: shortLabel?.toString()
            ?: "ERROR: No label defined".also { log.e("ShortcutInfo $this has no label") }
        return Shortcut(
            packageName = `package`,
            shortcutId = id,
            type = type,
            label = customName ?: label,
            icon = io { launcherApps.getShortcutBadgedIconDrawable(this, 0) }?.toAdaptiveIcon()
//                ?: launcherApps.getShortcutIconDrawable(this, 0)?.toAdaptiveIcon()
//                    ?.also { log.d("Shortcut $this has no badged icon") }
                ?: MissingIcon.also { log.e("Shortcut $this has no icon") },
            rank = rank,
            badgeIcon = customBadgeIcon
                ?: activity?.io { pm.getActivityIcon(this) }?.toAdaptiveIcon()
                ?: try {
                    io { pm.getApplicationIcon(`package`) }.toAdaptiveIcon()
                } catch (_: PackageManager.NameNotFoundException) {
                    MissingIcon.also { log.e("Failed to load badge icon for $this: package not found") }
                },
            originalLabelOrNull = if (customName == null) null else label
        )
    }

    @RequiresApi(O)
    class PinRequest internal constructor(
        internal val pinItemRequest: PinItemRequest,
        val shortcut: Shortcut,
    )

    object AddShortcutActivity : ActivityResultContract<ComponentName, Intent?>() {
        override fun createIntent(context: Context, input: ComponentName): Intent =
            Intent(Intent.ACTION_CREATE_SHORTCUT).setComponent(input)

        override fun parseResult(resultCode: Int, intent: Intent?): Intent? = intent
    }

    @RequiresApi(N_MR1)
    private suspend fun rename(shortcut: Shortcut, newName: String) {
        val customization = customizationDao.get(shortcut.uid.string).copy(
            label = if (shortcut.originalLabel == newName) null else newName
        )
        customizationDao.update(customization)
        log.d("Shortcut $shortcut renamed to $newName")
    }

    @RequiresApi(N_MR1)
    fun renameAsync(shortcut: Shortcut, newName: String) {
        coroutineScope.launch { rename(shortcut, newName) }
    }

    @RequiresApi(N_MR1)
    private suspend fun setHidden(shortcut: Shortcut, hidden: Boolean) {
        val customization = customizationDao.get(shortcut.uid.string).copy(hidden = hidden)
        customizationDao.update(customization)
        log.d("Shortcut $shortcut set hidden $hidden")
    }

    @RequiresApi(N_MR1)
    fun setHiddenAsync(shortcut: Shortcut, hidden: Boolean) {
        coroutineScope.launch { setHidden(shortcut, hidden) }
    }
}
