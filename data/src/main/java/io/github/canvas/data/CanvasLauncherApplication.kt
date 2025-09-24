package io.github.canvas.data

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.pm.LauncherApps
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import androidx.core.content.getSystemService
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.room.Room
import io.github.canvas.data.activities.ActivitiesRepository
import io.github.canvas.data.calendar.CalendarRepository
import io.github.canvas.data.contacts.ContactsRepository
import io.github.canvas.data.database.AppDatabase
import io.github.canvas.data.icons.IconPackRepository
import io.github.canvas.data.settings.SettingsProtoSerializer
import io.github.canvas.data.settings.SettingsRepository
import io.github.canvas.data.shortcuts.ShortcutsRepository
import io.github.canvas.data.widgets.WidgetHost
import io.github.canvas.data.widgets.WidgetsRepository


class CanvasLauncherApplication : Application() {
    lateinit var activitiesRepository: ActivitiesRepository
        private set
    lateinit var shortcutsRepository: ShortcutsRepository
        private set
    lateinit var contactsRepository: ContactsRepository
        private set
    lateinit var calendarRepository: CalendarRepository
        private set
    lateinit var widgetsRepository: WidgetsRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var iconPackRepository: IconPackRepository
        private set
    lateinit var dateRepository: DateRepository
        private set

    override fun onCreate() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build()
            )
            StrictMode.setVmPolicy(
                VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }

        super.onCreate()
        val launcherApps = getSystemService<LauncherApps>()!!
        val pm = packageManager
        // onCreate() is only called once in the entire lifetime of the process, so the datastore is guaranteed to be only created once
        val datastore = DataStoreFactory.create(
            serializer = SettingsProtoSerializer,
            produceFile = { dataStoreFile("settings.pb") }
        )
        val db = Room.databaseBuilder(this, AppDatabase::class.java, "canvas-launcher.db").build()
        val widgetDao = db.getWidgetDao()
        val activityCustomizationDao = db.getActivityCustomizationDao()
        val shortcutCustomizationDao = db.getShortcutCustomizationDao()
        val contactCustomizationDao = db.getContactCustomizationDao()
        val widgetManager = AppWidgetManager.getInstance(this)
        val widgetHost = WidgetHost(this)
        val context = this
        val contentResolver = contentResolver

        dateRepository = DateRepository(this)
        settingsRepository = SettingsRepository(datastore)
        iconPackRepository = IconPackRepository(pm, launcherApps, settingsRepository)
        widgetsRepository = WidgetsRepository(
            widgetDao, widgetManager, widgetHost, pm, launcherApps,
            settingsRepository
        )
        activitiesRepository = ActivitiesRepository(
            launcherApps, pm, activityCustomizationDao,
            iconPackRepository, widgetsRepository, dateRepository, settingsRepository
        )
        contactsRepository = ContactsRepository(
            contentResolver, context, pm, contactCustomizationDao,
            settingsRepository, iconPackRepository, dateRepository
        )
        shortcutsRepository = ShortcutsRepository(
            launcherApps, pm, shortcutCustomizationDao,
            contactsRepository, settingsRepository, iconPackRepository, dateRepository
        )
        calendarRepository = CalendarRepository(
            contentResolver, context,
            settingsRepository
        )

        log.d("CanvasLauncherApplication initialized")
    }
}

/** Shorthand for applicationContext as CanvasLauncherApplication */
val Context.application: CanvasLauncherApplication
    get() = applicationContext as CanvasLauncherApplication
