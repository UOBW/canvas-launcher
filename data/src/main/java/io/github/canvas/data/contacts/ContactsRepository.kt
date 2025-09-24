package io.github.canvas.data.contacts

import android.Manifest.permission.READ_CONTACTS
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Contacts.DISPLAY_NAME
import android.provider.ContactsContract.Contacts.LOOKUP_KEY
import android.provider.ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
import android.provider.ContactsContract.Contacts.PINNED
import android.provider.ContactsContract.Contacts.STARRED
import android.provider.ContactsContract.Contacts._ID
import androidx.core.database.getStringOrNull
import androidx.core.net.toUri
import io.github.canvas.data.ContentResolverBackedRepository
import io.github.canvas.data.DateRepository
import io.github.canvas.data.Quadruple
import io.github.canvas.data.componentName
import io.github.canvas.data.database.ContactCustomization
import io.github.canvas.data.database.ContactCustomizationDao
import io.github.canvas.data.icons.IconPack
import io.github.canvas.data.icons.IconPackRepository
import io.github.canvas.data.icons.toAdaptiveIcon
import io.github.canvas.data.io
import io.github.canvas.data.log
import io.github.canvas.data.originalLabel
import io.github.canvas.data.repositoryCoroutineScope
import io.github.canvas.data.settings.SettingsRepository
import io.github.canvas.data.useOnIo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ContactsRepository internal constructor(
    private val contentResolver: ContentResolver,
    private val context: Context,
    private val pm: PackageManager,
    private val customizationDao: ContactCustomizationDao,
    private val settingsRepository: SettingsRepository,
    private val iconPackRepository: IconPackRepository,
    private val dateRepository: DateRepository,
) : ContentResolverBackedRepository(
    contentResolver = contentResolver, uri = Contacts.CONTENT_URI
) {
    private val coroutineScope = repositoryCoroutineScope()

    private val _contacts: MutableStateFlow<List<Contact>?> = MutableStateFlow(null)
    val contacts: StateFlow<List<Contact>?> = _contacts.asStateFlow()

    private val _hidden: MutableStateFlow<List<Contact>> = MutableStateFlow(emptyList())
    val hidden: StateFlow<List<Contact>> = _hidden.asStateFlow()

    private val _all: MutableStateFlow<List<Contact>> = MutableStateFlow(emptyList())
    val all: StateFlow<List<Contact>> = _all.asStateFlow()

    /**
     * Reloads the list of contacts from the OS, blocking until the new value has been emitted to the flow
     * If the contactsSearchEnabled setting is not enabled, this method will clear the contacts from the flow and return
     */
    private suspend fun reload(
        customizations: Map<Long, ContactCustomization>,
        contactSearchEnabled: Boolean,
        iconPack: IconPack?,
        dayOfMonth: Int,
    ) {
        if (!contactSearchEnabled) {
            log.d("Not loading contacts: disabled in settings")
            _contacts.value = emptyList()
            return
        }

        if (io { context.checkSelfPermission(READ_CONTACTS) } == PERMISSION_DENIED) {
            log.e("Error while loading contacts: no permission")
            _contacts.value = emptyList() // No longer loading
            return
        }

        // Register the content observer if it hasn't been registered yet
        tryRegisterContentObserver()

        // Get the icon of the default contacts app
        val badgeIcon = io {
            pm.queryIntentActivities(
                Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CONTACTS), 0
            )
        }.firstOrNull()?.activityInfo?.let {
            iconPack?.loadIcon(it.componentName, dayOfMonth)
                ?: io { it.loadIcon(pm) }?.toAdaptiveIcon()
        }

        val cursor = io {
            @SuppressLint("Recycle")
            contentResolver.query(
                Contacts.CONTENT_URI, // Table to query
                arrayOf( // List of columns to return
                    _ID,
                    DISPLAY_NAME,
                    LOOKUP_KEY,
                    STARRED,
                    PINNED,
                    PHOTO_THUMBNAIL_URI
                ),
                null, // Filter
                null, // Arguments to the filter
                null // Sort
            )
        }

        when (cursor?.io { count }) {
            null -> log.e("Failed to load contacts: query() returned null")

            0 -> {
                _contacts.value = emptyList()
                io { cursor.close() }
                log.d("Contacts list rebuilt: no contacts found")
            }

            else -> {
                val contacts = mutableListOf<Contact>()
                val hidden = mutableListOf<Contact>()

                cursor.useOnIo {
                    val idIndex = cursor.getColumnIndexOrThrow(_ID)
                    val nameIndex = cursor.getColumnIndexOrThrow(DISPLAY_NAME)
                    val lookupKeyIndex = cursor.getColumnIndexOrThrow(LOOKUP_KEY)
                    val isStarredIndex = cursor.getColumnIndexOrThrow(STARRED)
                    val pinPositionIndex = cursor.getColumnIndexOrThrow(PINNED)
                    val iconUriIndex = cursor.getColumnIndexOrThrow(PHOTO_THUMBNAIL_URI)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIndex)
                        val lookupKey = cursor.getString(lookupKeyIndex)
                        val name = cursor.getStringOrNull(nameIndex)
                            ?: continue // Skip contacts without names, because they can't be searched for anyway
                        val iconUri: Uri? = cursor.getStringOrNull(iconUriIndex)?.toUri()
                        val icon = iconUri?.let { uri ->
                            contentResolver.openInputStream(uri).use { stream ->
                                Drawable.createFromStream(stream, iconUri.toString())
                            }?.toAdaptiveIcon()
                        } ?: generateContactIcon(
                            contactName = name,
                            lookupKey = lookupKey,
                            res = context.resources
                        )
                        val customization = customizations[id]

                        val contact = Contact(
                            id = id,
                            label = customization?.label ?: name,
                            lookupKey = lookupKey,
                            icon = icon,
                            badgeIcon = badgeIcon,
                            isStarred = cursor.getInt(isStarredIndex) > 0,
                            priority = cursor.getInt(pinPositionIndex),
                            originalLabelOrNull = if (customization?.label != null) name else null,
                        )
                        when (customization?.hidden) {
                            false, null -> contacts.add(contact)
                            true -> hidden.add(contact)
                        }
                    }
                }

                contacts.sort()
                hidden.sort()
                _contacts.value = contacts
                _hidden.value = hidden
                _all.value = contacts + hidden
                log.d("Contacts list rebuilt: ${contacts.size} contacts")
            }
        }
    }

    init {
        // Do the initial reload and listen for changes
        coroutineScope.launch {
            combine(
                contentObserverListener,
                customizationDao.all(),
                settingsRepository.settings.map { it.contactSearchEnabled }.distinctUntilChanged(),
                iconPackRepository.currentIconPack,
                dateRepository.dayOfMonth
            ) { _, customizations, contactSearchEnabled, iconPack, dayOfMonth ->
                Quadruple(customizations, contactSearchEnabled, iconPack, dayOfMonth)
            }.collectLatest { (customisations, contactSearchEnabled, iconPack, dayOfMonth) ->
                reload(customisations, contactSearchEnabled, iconPack, dayOfMonth)
            }
        }
        log.d("Contacts repository initialized")
    }

    private suspend fun rename(contact: Contact, newName: String) {
        val customization = customizationDao.get(contact.id).copy(
            label = if (contact.originalLabel == newName) null else newName
        )
        customizationDao.update(customization)
        log.d("Contact $contact renamed to $newName")
    }

    fun renameAsync(contact: Contact, newName: String) {
        coroutineScope.launch { rename(contact, newName) }
    }

    private suspend fun setHidden(contact: Contact, hidden: Boolean) {
        val customization = customizationDao.get(contact.id).copy(hidden = hidden)
        customizationDao.update(customization)
        log.d("Contact $contact set hidden $hidden")
    }

    fun setHiddenAsync(contact: Contact, hidden: Boolean) {
        coroutineScope.launch { setHidden(contact, hidden) }
    }
}
