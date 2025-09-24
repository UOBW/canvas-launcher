package io.github.canvas.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.github.canvas.data.settings.Settings
import io.github.canvas.data.settings.Settings.MonochromeIconColors

/** Null if monochrome icons are disabled */
val LocalMonochromeIconColors: ProvidableCompositionLocal<MonochromeIconColors?> =
    compositionLocalOf { null }

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6650a4),
    secondary = Color(0xFF625b71),
    tertiary = Color(0xFF7D5260)
)

@Composable
fun CanvasLauncherTheme(
    theme: Settings.Theme,
    monochromeIconsEnabled: Boolean,
    monochromeIconColors: MonochromeIconColors,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (theme) {
        Settings.Theme.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        Settings.Theme.LIGHT -> false
        Settings.Theme.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(
        LocalMonochromeIconColors provides if (monochromeIconsEnabled) monochromeIconColors else null
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
