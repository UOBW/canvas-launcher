package io.github.canvas.ui.settings

import android.content.Intent.ACTION_SET_WALLPAPER
import android.provider.Settings.ACTION_HOME_SETTINGS
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Left
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Right
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.activity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.canvas.ui.settings.screens.AboutScreen
import io.github.canvas.ui.settings.screens.DeveloperOptionsScreen
import io.github.canvas.ui.settings.screens.HiddenAppsScreen
import io.github.canvas.ui.settings.screens.LicensesScreen
import io.github.canvas.ui.settings.screens.MainScreen
import io.github.canvas.ui.settings.screens.SearchScreen
import io.github.canvas.ui.settings.screens.UiScreen
import kotlinx.serialization.Serializable

interface Route

@Serializable
data object MainScreen : Route

@Serializable
data object SearchScreen : Route

@Serializable
data object UiScreen : Route

@Serializable
data object HiddenAppsScreen : Route

@Serializable
data object AdvancedScreen : Route

@Serializable
data object ChangeWallpaperExternal : Route

@Serializable
data object ChangeLauncherExternal : Route

@Serializable
data object AboutScreen : Route

@Serializable
data object LicensesScreen : Route

@StringRes
fun getTitle(route: String?): Int = when (route) {
    // This seems to be the best solution until support for polymorphic serialization is added to the navigation library
    MainScreen::class.qualifiedName -> R.string.main_screen_title
    SearchScreen::class.qualifiedName -> R.string.search_screen_title
    UiScreen::class.qualifiedName -> R.string.ui_screen_title
    HiddenAppsScreen::class.qualifiedName -> R.string.hidden_results_screen_title
    AdvancedScreen::class.qualifiedName -> R.string.developer_options_screen_title
    AboutScreen::class.qualifiedName -> R.string.about_screen_title
    LicensesScreen::class.qualifiedName -> R.string.license_screen_title
    else -> R.string.empty
}

fun shouldShowBackButton(route: String?): Boolean = route != MainScreen::class.qualifiedName

/** The main settings composable that controls the navigation */
@Composable
fun Settings(settingsViewModel: SettingsViewModel) {
    val navController = rememberNavController()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            val backStackEntry by navController.currentBackStackEntryFlow
                .collectAsStateWithLifecycle(initialValue = null)
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                navigationIcon = {
                    if (shouldShowBackButton(backStackEntry?.destination?.route)) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = stringResource(R.string.accessibility_button_back_contentDescription)
                            )
                        }
                    }
                },
                title = { Text(stringResource(getTitle(backStackEntry?.destination?.route))) }
            )
        },
        // Status bar insets are provided by the top bar and navigation bar insets are not needed here
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainScreen,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { slideIntoContainer(towards = Left) },
            exitTransition = { slideOutOfContainer(towards = Left) },
            popEnterTransition = { slideIntoContainer(towards = Right) },
            popExitTransition = { slideOutOfContainer(towards = Right) }
        ) {
            composable<MainScreen> { MainScreen(settingsViewModel, navController::navigate) }
            composable<SearchScreen> { SearchScreen(settingsViewModel, navController::navigate) }
            composable<UiScreen> { UiScreen(settingsViewModel, navController::navigate) }
            composable<HiddenAppsScreen> { HiddenAppsScreen(settingsViewModel) }
            composable<AdvancedScreen> { DeveloperOptionsScreen(settingsViewModel) }
            composable<AboutScreen> { AboutScreen(settingsViewModel, navController::navigate) }
            composable<LicensesScreen> { LicensesScreen() }
            activity<ChangeWallpaperExternal> { action = ACTION_SET_WALLPAPER }
            activity<ChangeLauncherExternal> { action = ACTION_HOME_SETTINGS }
        }
    }
}
