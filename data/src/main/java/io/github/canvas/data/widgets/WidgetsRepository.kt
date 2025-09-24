package io.github.canvas.data.widgets

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.ACTION_APPWIDGET_BIND
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_PROVIDER
import android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
import android.appwidget.AppWidgetProviderInfo
import android.appwidget.AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN
import android.appwidget.AppWidgetProviderInfo.WIDGET_CATEGORY_SEARCHBOX
import android.appwidget.AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL
import android.appwidget.AppWidgetProviderInfo.WIDGET_FEATURE_HIDE_FROM_PICKER
import android.appwidget.AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Color.RED
import android.graphics.drawable.Drawable
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES.O
import android.os.Build.VERSION_CODES.P
import android.os.Build.VERSION_CODES.S
import android.os.Bundle
import android.os.Process
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import android.widget.ListView
import android.widget.StackView
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.children
import io.github.canvas.data.BuildConfig
import io.github.canvas.data.background
import io.github.canvas.data.database.WidgetDao
import io.github.canvas.data.database.WidgetDb
import io.github.canvas.data.firstNonNull
import io.github.canvas.data.hasFlagSet
import io.github.canvas.data.icons.AdaptiveIcon
import io.github.canvas.data.icons.toAdaptiveIcon
import io.github.canvas.data.io
import io.github.canvas.data.isPackageInstalled
import io.github.canvas.data.listener
import io.github.canvas.data.log
import io.github.canvas.data.settings.SettingsRepository
import io.github.canvas.data.sortedBy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.Int.Companion.MAX_VALUE

class WidgetsRepository internal constructor(
    private val dao: WidgetDao,
    private val widgetManager: AppWidgetManager,
    private val host: WidgetHost,
    private val pm: PackageManager,
    private val launcherApps: LauncherApps,
    private val settingsRepository: SettingsRepository,
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _widgets = MutableStateFlow<Widgets?>(null)

    /** Stores the widgets separately for each page. Only page 0 is guaranteed to exist, and it may have no widgets. */
    val widgets: StateFlow<Widgets?> = _widgets.asStateFlow()

    private suspend fun reload(
        widgetsEnabled: Boolean,
        widgetsDb: List<WidgetDb>,
    ) {
        if (!widgetsEnabled) {
            _widgets.value = Widgets.EMPTY
            log.d("Widget list reloaded: widgets disabled")
            return
        }

        if (widgetsDb.isEmpty()) {
            io { host.deleteHost() } // In case there was a data wipe
            log.d("Widget list reloaded: no widgets")
            _widgets.value = Widgets.EMPTY
            return
        }

        if (BuildConfig.DEBUG && VERSION.SDK_INT >= O) {
            when (io { host.appWidgetIds }.size) {
                in 0..widgetsDb.size -> log.e("AppWidgetHost has less widgets than the database") // Recoverable
                in widgetsDb.size..MAX_VALUE -> error("AppWidgetHost has more widgets than the database") // Unrecoverable
            }
        }

        val widgets = mutableMapOf<Int, MutableList<Widget>>()
        for (db in widgetsDb) {
            val id = WidgetId(db.id)
            val info = io { widgetManager.getAppWidgetInfo(id) }
            if (info == null) {
                log.e("Failed to load widget $id: getAppWidgetInfo() returned null, removing widget")
                removeAsync(id)
                continue
            }
            widgets.getOrPut(key = db.page, defaultValue = { mutableListOf() }).add(
                Widget(
                    id = id,
                    page = db.page,
                    position = db.position,
                    verticalWeight = db.verticalWeight,
                    reconfigurationActivity = if (
                        VERSION.SDK_INT >= P &&
                        info.widgetFeatures hasFlagSet WIDGET_FEATURE_RECONFIGURABLE
                    ) info.configure else null,
                    isScrollable = db.scrollable,
                    name = info.loadLabel()
                )
            )
        }
        widgets.forEach { (_, page) -> page.sortBy { it.position } }
        log.d("Widget list reloaded: ${widgetsDb.size} widgets")
        _widgets.value = Widgets(widgets.toSortedMap())
    }

    private fun reloadAsync() {
        coroutineScope.launch {
            reload(settingsRepository.settings.first().widgetsEnabled, dao.all().first())
        }
    }

    init {
        coroutineScope.launch {
            combine(
                settingsRepository.settings.map { it.widgetsEnabled }.distinctUntilChanged(),
                dao.all(),
            ) { widgetsEnabled, widgetsDb ->
                Pair(widgetsEnabled, widgetsDb)
            }.collectLatest { (widgetsEnabled, widgetsDb) ->
                reload(widgetsEnabled, widgetsDb)
            }
        }
    }

    fun startListening() {
        coroutineScope.launch(context = Dispatchers.Main) {
            host.startListening()
            log.d("Started listening for widget updates")
        }
    }

    fun stopListening() {
        coroutineScope.launch(context = Dispatchers.Main) {
            host.stopListening()
            log.d("Stopped listening for widget updates")
        }
    }

    suspend fun hasWidgets(packageName: String): Boolean = io {
        return@io if (VERSION.SDK_INT >= O) {
            widgetManager.getInstalledProvidersForPackage(packageName, Process.myUserHandle()).any()
        } else {
            widgetManager.installedProviders.any { it.provider.packageName == packageName }
        }
    }

    /** Returns all available widgets of the given/all app. Emits null if the app is the app is no longer installed. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAvailableWidgetsOfApp(
        packageName: String?, context: Context,
    ): Flow<List<AvailableWidget>?> = launcherApps.listener.mapLatest {
        if (packageName != null && !pm.isPackageInstalled(packageName)) return@mapLatest null // App is no longer installed

        val allWidgets = io {
            if (VERSION.SDK_INT >= O && packageName != null) {
                // Optimization
                widgetManager.getInstalledProvidersForPackage(packageName, Process.myUserHandle())
            } else widgetManager.installedProviders
        }

        return@mapLatest allWidgets.mapNotNull { widget ->
            if (packageName != null && widget.provider.packageName != packageName) return@mapNotNull null

            if (
                VERSION.SDK_INT >= P &&
                widget.widgetFeatures hasFlagSet WIDGET_FEATURE_HIDE_FROM_PICKER
            ) return@mapNotNull null // Widget is hidden from picker

            if (
                !widget.widgetCategory.hasFlagSet(WIDGET_CATEGORY_HOME_SCREEN)
                && !widget.widgetCategory.hasFlagSet(WIDGET_CATEGORY_SEARCHBOX)
            ) return@mapNotNull null // Widget should not be shown on the home screen

            val label = widget.loadLabel()
            return@mapNotNull AvailableWidget(
                provider = widget.provider,
                label = label,
                description = if (VERSION.SDK_INT >= S) {
                    io { widget.loadDescription(context) }?.toString()
                        ?.takeIf { it != label } //Prevent duplicate description
                } else null,
                previewImage = if (widget.previewLayout == 0) io {
                    widget.loadPreviewImage(context, 0)
                        ?: widget.loadIcon(context, 0)
                } else null,
                previewLayout = widget.previewLayout
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAvailableWidgetsByApp(context: Context): Flow<List<Pair<App, List<AvailableWidget>>>> =
        getAvailableWidgetsOfApp(packageName = null, context).mapLatest {
            it!!.groupBy { it.provider.packageName }
                .map { (packageName, widgets) ->
                    val app = io { pm.getApplicationInfo(packageName, 0) }
                    App(
                        name = io { app.loadLabel(pm).toString() },
                        icon = io { app.loadIcon(pm) }.toAdaptiveIcon()
                    ) to widgets
                }.sortedBy(String.CASE_INSENSITIVE_ORDER) { (app, _) -> app.name }
        }

    private suspend fun addWidget(
        widget: AvailableWidget,
        requestWidgetPermission: (RequestWidgetPermissionRequest) -> Unit,
        configurationResultActivity: Activity,
        context: Context,
    ) {
        val id = io { host.allocateWidgetId() }
        val success = io { widgetManager.bindAppWidgetIdIfAllowed(id, widget.provider) }
        if (success) {
            log.d("Successfully added widget ${widget.label}")
            finalizeAdd(id, configurationResultActivity, context)
        } else {
            log.d("Asking for permission to add widget ${widget.label}")
            requestWidgetPermission(
                RequestWidgetPermissionRequest(
                    widgetId = id,
                    provider = widget.provider
                )
            )
        }
    }

    fun addWidgetAsync(
        widget: AvailableWidget,
        requestWidgetPermission: (RequestWidgetPermissionRequest) -> Unit,
        configurationResultActivity: Activity,
        context: Context,
    ) {
        coroutineScope.launch {
            addWidget(widget, requestWidgetPermission, configurationResultActivity, context)
        }
    }

    fun onRequestWidgetPermissionResult(
        result: RequestWidgetPermissionResult,
        configurationResultActivity: Activity,
        context: Context,
    ) {
        coroutineScope.launch {
            val id = result.widgetId
            if (id.isInvalid()) {
                log.e("Failed to add widget: ACTION_APPWIDGET_BIND did not return a valid widget id")
                return@launch
            }
            if (!result.success) {
                log.e("Failed to add widget: ACTION_APPWIDGET_BIND was not successful")
                io { host.deleteAppWidgetId(result.widgetId) }
                return@launch
            }
            finalizeAdd(id, configurationResultActivity, context)
        }
    }

    @SuppressLint("InlinedApi")
    private suspend fun finalizeAdd(
        id: WidgetId,
        configurationResultActivity: Activity,
        context: Context,
    ) {
        val info = io { widgetManager.getAppWidgetInfo(id) }
        if (info == null) {
            log.e("Failed to add widget: getAppWidgetInfo() returned null")
            io { host.deleteAppWidgetId(id) }
            return
        }

        //Configuration is required if the widget has a configuration activity and, on Android 9+, isn't marked as both optional and reconfigurable
        val configurationRequired = info.configure != null && (VERSION.SDK_INT < P ||
                !info.widgetFeatures.hasFlagSet(WIDGET_FEATURE_CONFIGURATION_OPTIONAL) ||
                !info.widgetFeatures.hasFlagSet(WIDGET_FEATURE_RECONFIGURABLE))

        dao.insert(
            WidgetDb(
                id = id.value,
                page = 0,
                position = widgets.firstNonNull().initialPage.lastOrNull()?.position?.plus(1) ?: 0,
                verticalWeight = 1f,
                configurationPending = configurationRequired,
                scrollable = isScrollable(id, info, context)
            )
        )
        if (configurationRequired) {
            configureWidget(id, configurationResultActivity)
            log.d("Starting initial configuration for widget ${info.provider.flattenToShortString()}")
        } else {
            log.d("Added widget ${info.provider.flattenToShortString()}")
        }
    }

    /** @param configurationResultActivity the activity to which the intent result will be delivered */
    suspend fun configureWidget(id: WidgetId, configurationResultActivity: Activity): Unit = io {
        host.startAppWidgetConfigureActivityForResult(
            configurationResultActivity, id,
            intentFlags = 0, requestCode = CONFIGURE_WIDGET_REQUEST_CODE, options = Bundle.EMPTY
        )
    }

    private suspend fun isScrollable(
        id: WidgetId,
        info: AppWidgetProviderInfo,
        context: Context,
    ): Boolean {
        //The view needs to be created on the UI thread
        val widgetView = withContext(Dispatchers.Main) { host.createView(context, id, info) }
        if (widgetView == null) {
            log.e("Failed to determine if widget $id is scrollable: createView() returned null")
            return false
        }
        return isScrollable(widgetView)
    }

    private fun isScrollable(view: View): Boolean {
        return when (view) {
            is ListView, is GridView, is StackView -> true
            is ViewGroup -> view.children.any { isScrollable(it) }
            else -> false
        }
    }

    fun onWidgetConfigurationResult(resultCode: Int, intent: Intent?) {
        coroutineScope.launch {
            val id = WidgetId(
                intent?.getIntExtra(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID)
                    ?: INVALID_APPWIDGET_ID
            )
            when {
                id.isInvalid() -> log.e("Failed to configure widget: ACTION_APPWIDGET_CONFIGURE did not return a valid widget id")

                resultCode != RESULT_OK -> {
                    val db = dao.get(id)
                    if (db.configurationPending) {
                        log.e("Failed to add widget: ACTION_APPWIDGET_CONFIGURE was not successful")
                        dao.delete(id)
                        io { host.deleteAppWidgetId(id) }
                    } else {
                        log.e("Failed to reconfigure widget: ACTION_APPWIDGET_CONFIGURE was not successful")
                    }
                }

                else -> log.d("Successfully configured widget $id")
            }
        }
    }

    suspend fun loadWidgetView(widget: Widget, context: Context): AppWidgetHostView? = background {
        val info = io { widgetManager.getAppWidgetInfo(widget.id) }
        if (info == null) {
            log.e("Failed to load widget view: getAppWidgetInfo() returned null")
            reloadAsync()
            return@background null
        }
        return@background withContext(Dispatchers.Main) {
            host.createView(context, widget.id, info)!!
        }
    }

    fun reconfigureWidgetAsync(widget: Widget, configurationResultActivity: Activity) {
        coroutineScope.launch {
            if (widget.reconfigurationActivity != null) {
                configureWidget(widget.id, configurationResultActivity)
            } else log.e("reconfigureWidget() called with a non-reconfigurable widget")
        }
    }

    private suspend fun remove(widget: WidgetId) {
        io { host.deleteAppWidgetId(widget) }
        dao.delete(widget)
        log.d("Widget $widget removed")
    }

    private fun removeAsync(widget: WidgetId) {
        coroutineScope.launch { remove(widget) }
    }

    fun removeAsync(widget: Widget): Unit = removeAsync(widget.id)

    @Suppress("DEPRECATION") //Used as a fallback
    private suspend fun AppWidgetProviderInfo.loadLabel() =
        io { loadLabel(pm) }
            ?: label
            ?: provider.flattenToShortString()

    suspend fun loadIcon(widget: Widget, context: Context): Drawable =
        io { widgetManager.getAppWidgetInfo(widget.id)?.loadIcon(context, 0) ?: RED.toDrawable() }

    /**
     *  * Applies the vertical weights from [updatedWeights]
     *  * Reorders the widgets in the order of [reorderedWidgets]
     *
     *  Assumes that all widgets in [reorderedWidgets] are from the same page.
     */
    private suspend fun reorderWidgets(
        reorderedWidgets: List<WidgetId>, updatedWeights: Map<WidgetId, Float>,
    ) {
        dao.transaction {
            for ((index, widgetId) in reorderedWidgets.withIndex()) {
                dao.update(dao.get(widgetId).copy(position = index))
            }
            for ((widgetId, weight) in updatedWeights) {
                dao.update(dao.get(widgetId).copy(verticalWeight = weight))
            }
        }
    }

    fun reorderWidgetsAsync(
        reorderedWidgets: List<WidgetId>,
        updatedWeights: Map<WidgetId, Float>,
    ) {
        coroutineScope.launch { reorderWidgets(reorderedWidgets, updatedWeights) }
    }

    private suspend fun moveWidget(widget: Widget, page: Int) {
        dao.transaction {
            dao.update(
                dao.get(widget.id).copy(
                    page = page,
                    position = widgets.firstNonNull().pageOrNull(page)
                        ?.lastOrNull()?.position?.plus(1) ?: 0
                )
            )
        }
    }

    fun moveWidgetAsync(widget: Widget, page: Int) {
        coroutineScope.launch { moveWidget(widget, page) }
    }

    data class RequestWidgetPermissionRequest(
        internal val widgetId: WidgetId,
        internal val provider: ComponentName,
    )

    data class RequestWidgetPermissionResult(
        internal val success: Boolean,
        internal val widgetId: WidgetId,
    )

    object RequestWidgetPermission :
        ActivityResultContract<RequestWidgetPermissionRequest, RequestWidgetPermissionResult>() {
        override fun createIntent(context: Context, input: RequestWidgetPermissionRequest): Intent =
            Intent(ACTION_APPWIDGET_BIND)
                .putWidgetIdExtra(input.widgetId)
                .putExtra(EXTRA_APPWIDGET_PROVIDER, input.provider)

        override fun parseResult(resultCode: Int, intent: Intent?): RequestWidgetPermissionResult =
            RequestWidgetPermissionResult(
                success = resultCode == RESULT_OK,
                widgetId = intent?.getWidgetIdExtra() ?: INVALID_WIDGET_ID
            )
    }

    /** Used as a key in [getAvailableWidgetsOfApp] */
    data class App(
        val name: String,
        val icon: AdaptiveIcon,
    )

    companion object {
        const val CONFIGURE_WIDGET_REQUEST_CODE: Int = 1
    }
}

private val INVALID_WIDGET_ID = WidgetId(INVALID_APPWIDGET_ID)

private fun Int.toWidgetId() = WidgetId(this)
private fun WidgetId.isInvalid() = value == INVALID_APPWIDGET_ID

private fun Intent.putWidgetIdExtra(id: WidgetId) = putExtra(EXTRA_APPWIDGET_ID, id.value)
private fun Intent.getWidgetIdExtra() =
    getIntExtra(EXTRA_APPWIDGET_ID, /* defaultValue = */INVALID_APPWIDGET_ID)
        .takeIf { it != INVALID_APPWIDGET_ID }
        ?.toWidgetId()

// Wrappers that convert WidgetId to Int
// @formatter:off
/** @see AppWidgetManager.getAppWidgetInfo */
private fun AppWidgetManager.getAppWidgetInfo(id: WidgetId): AppWidgetProviderInfo? = getAppWidgetInfo(id.value)
/** @see AppWidgetManager.bindAppWidgetIdIfAllowed */
private fun AppWidgetManager.bindAppWidgetIdIfAllowed(id: WidgetId, provider: ComponentName) = bindAppWidgetIdIfAllowed(id.value, provider)

/** @see AppWidgetHost.allocateAppWidgetId */
private fun AppWidgetHost.allocateWidgetId() = allocateAppWidgetId().toWidgetId()
/** @see AppWidgetHost.createView */
private fun AppWidgetHost.createView(context: Context, id: WidgetId, widget: AppWidgetProviderInfo): AppWidgetHostView? = createView(context, id.value, widget)
/** @see AppWidgetHost.deleteAppWidgetId */
private fun AppWidgetHost.deleteAppWidgetId(id: WidgetId) = deleteAppWidgetId(id.value)
/** @see AppWidgetHost.startAppWidgetConfigureActivityForResult */
private fun AppWidgetHost.startAppWidgetConfigureActivityForResult(activity: Activity, appWidgetId: WidgetId, intentFlags: Int, requestCode: Int, options: Bundle) = startAppWidgetConfigureActivityForResult(activity, appWidgetId.value, intentFlags, requestCode, options)

private suspend fun WidgetDao.get(id: WidgetId) = get(id.value)
private suspend fun WidgetDao.delete(id: WidgetId) = delete(id.value)
// @formatter:on
