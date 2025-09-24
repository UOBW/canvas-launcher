package io.github.canvas.ui.launcher

import android.app.ComponentCaller
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.app.WallpaperManager.FLAG_SYSTEM
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.canvas.data.hasFlagSet
import io.github.canvas.data.settings.Settings
import io.github.canvas.data.settings.Settings.MonochromeIconColors.PRIMARY
import io.github.canvas.data.shortcuts.ShortcutsRepository
import io.github.canvas.data.widgets.WidgetsRepository
import io.github.canvas.ui.CanvasLauncherTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class LauncherActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels<LauncherViewModel> { LauncherViewModel.Factory }

    private val searchFocusRequester: FocusRequester = FocusRequester()
    private var keyboardController: SoftwareKeyboardController? = null

    private var textColor: MutableStateFlow<Color> = MutableStateFlow(White)
    private fun updateTextColor(wallpaperColors: WallpaperColors?) {
        textColor.value = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            wallpaperColors != null &&
            wallpaperColors.colorHints hasFlagSet FLAG_SYSTEM
        ) Color.Black else White
    }

    private val addShortcut =
        registerForActivityResult(ShortcutsRepository.AddShortcutActivity) { result ->
            viewModel.onShortcutCreationActivityResult(result, this)
        }

    private val requestWidgetPermission =
        registerForActivityResult(WidgetsRepository.RequestWidgetPermission) { result ->
            viewModel.onRequestWidgetPermissionResult(
                result = result,
                configurationResultActivity = this@LauncherActivity,
                context = this@LauncherActivity,
            )
        }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        caller: ComponentCaller,
    ) {
        // Dispatch to registerForActivityResult()
        super.onActivityResult(requestCode, resultCode, data, caller)

        // Widget configuration requests have to be launched via AppWidgetHost.startAppWidgetConfigureActivityForResult(), so registerForActivityResult() can't be used
        if (requestCode == WidgetsRepository.CONFIGURE_WIDGET_REQUEST_CODE) {
            viewModel.onWidgetConfigurationResult(resultCode, data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.dark(Transparent.toArgb())
        )

        // Adapt the text color to the wallpaper
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val wallpaperManager = WallpaperManager.getInstance(this)
            updateTextColor(wallpaperManager.getWallpaperColors(FLAG_SYSTEM))
            WallpaperManager.getInstance(this).addOnColorsChangedListener({ colors, location ->
                if (location hasFlagSet FLAG_SYSTEM) updateTextColor(colors)
            }, Handler(Looper.getMainLooper()))
        }

        setContent {
            keyboardController = LocalSoftwareKeyboardController.current
            val theme by viewModel.settings.map { it.theme }
                .collectAsStateWithLifecycle(initialValue = Settings.defaultTheme)
            val monochromeIconsEnabled by viewModel.settings.map { it.monochromeIconsEnabled }
                .collectAsStateWithLifecycle(initialValue = false)
            val monochromeIconColors by viewModel.settings.map { it.monochromeIconColors }
                .collectAsStateWithLifecycle(initialValue = PRIMARY)

            CanvasLauncherTheme(
                theme = theme,
                monochromeIconsEnabled = monochromeIconsEnabled,
                monochromeIconColors = monochromeIconColors,
            ) {
                Launcher(
                    viewModel = viewModel,
                    textColor = textColor.collectAsState().value,
                    searchFocusRequester = searchFocusRequester,
                    onSearchFocus = {
                        keyboardController?.show()
                            ?: log.e("Failed to show software keyboard: not supported")
                    },
                    onStartAddShortcutActivity = { addShortcutActivity ->
                        addShortcut.launch(addShortcutActivity)
                    },
                    onRequestWidgetPermission = { request ->
                        requestWidgetPermission.launch(request)
                    },
                )
            }
        }

        log.d("Canvas Launcher activity started")
    }

    override fun onStart() {
        super.onStart()
        viewModel.startListening()
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopListening()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
            log.d("Focus requested")
        }
    }
}
