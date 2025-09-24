package io.github.canvas.ui.settings.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.canvas.data.icons.AdaptiveIcon
import io.github.canvas.data.icons.VectorForeground
import io.github.canvas.data.settings.Settings
import io.github.canvas.data.settings.Settings.ButtonBehavior
import io.github.canvas.data.sortedBy
import io.github.canvas.ui.BasicSettingsItem
import io.github.canvas.ui.DynamicChooseOptionSetting
import io.github.canvas.ui.DynamicOption
import io.github.canvas.ui.FixedChooseOptionSetting
import io.github.canvas.ui.StaticOption
import io.github.canvas.ui.ToggleSetting
import io.github.canvas.ui.rememberAdaptiveIconPainter
import io.github.canvas.ui.settings.ChangeWallpaperExternal
import io.github.canvas.ui.settings.R
import io.github.canvas.ui.settings.Route
import io.github.canvas.ui.settings.SettingsViewModel
import kotlinx.coroutines.flow.map

private enum class ButtonBehaviorOption : StaticOption {
    OPEN_FIRST_RESULT {
        @Composable
        override fun title() = stringResource(R.string.button_behavior_open_result)
    },
    HIDE_KEYBOARD {
        @Composable
        override fun title() = stringResource(R.string.button_behavior_hide_keyboard)
    },
    CLEAR_SEARCH_TERM {
        @Composable
        override fun title() = stringResource(R.string.button_behavior_clear_search_term)
    },
    DO_NOTHING {
        @Composable
        override fun title() = stringResource(R.string.button_behavior_do_nothing)
    },
}

private fun ButtonBehavior.toButtonBehaviorOption(): ButtonBehaviorOption = when (this) {
    ButtonBehavior.OPEN_FIRST_RESULT -> ButtonBehaviorOption.OPEN_FIRST_RESULT
    ButtonBehavior.HIDE_KEYBOARD -> ButtonBehaviorOption.HIDE_KEYBOARD
    ButtonBehavior.DO_NOTHING -> ButtonBehaviorOption.DO_NOTHING
    ButtonBehavior.CLEAR_SEARCH_TERM -> ButtonBehaviorOption.CLEAR_SEARCH_TERM
}

private fun ButtonBehaviorOption.toButtonBehavior(): ButtonBehavior = when (this) {
    ButtonBehaviorOption.OPEN_FIRST_RESULT -> ButtonBehavior.OPEN_FIRST_RESULT
    ButtonBehaviorOption.HIDE_KEYBOARD -> ButtonBehavior.HIDE_KEYBOARD
    ButtonBehaviorOption.DO_NOTHING -> ButtonBehavior.DO_NOTHING
    ButtonBehaviorOption.CLEAR_SEARCH_TERM -> ButtonBehavior.CLEAR_SEARCH_TERM
}

private data class IconPackOption(
    override val id: String?,
    val title: String?,
    override val adaptiveIcon: AdaptiveIcon?,
) : DynamicOption {
    @Composable
    override fun title(): String = title ?: stringResource(R.string.setting_icon_pack_system_icons)
}

private val SystemIconPack = IconPackOption(null, null, null)

private enum class MonochromeIconColorsOption : StaticOption {
    PRIMARY {
        @Composable
        override fun title(): String = stringResource(R.string.monochrome_icon_colors_primary)
    },
    SECONDARY {
        @Composable
        override fun title(): String = stringResource(R.string.monochrome_icon_colors_secondary)
    },
    TERTIARY {
        @Composable
        override fun title(): String = stringResource(R.string.monochrome_icon_colors_tertiary)
    },
    BLACK {
        @Composable
        override fun title(): String = stringResource(R.string.monochrome_icon_colors_black)
    },
    WHITE {
        @Composable
        override fun title(): String = stringResource(R.string.monochrome_icon_colors_white)
    };

    @get:Composable
    override val icon: Painter?
        get() = rememberAdaptiveIconPainter(
            icon = AdaptiveIcon(
                foreground = VectorForeground(Icons.Default.Info),
                foregroundScale = AdaptiveIcon.SCALE_FIT,
                monochrome = null,
                background = null
            ),
            monochromeColors = this.toMonochromeIconColors()
        )
}

private fun Settings.MonochromeIconColors.toMonochromeIconColorsOption() = when (this) {
    Settings.MonochromeIconColors.PRIMARY -> MonochromeIconColorsOption.PRIMARY
    Settings.MonochromeIconColors.SECONDARY -> MonochromeIconColorsOption.SECONDARY
    Settings.MonochromeIconColors.TERTIARY -> MonochromeIconColorsOption.TERTIARY
    Settings.MonochromeIconColors.BLACK -> MonochromeIconColorsOption.BLACK
    Settings.MonochromeIconColors.WHITE -> MonochromeIconColorsOption.WHITE
}

private fun MonochromeIconColorsOption.toMonochromeIconColors() = when (this) {
    MonochromeIconColorsOption.PRIMARY -> Settings.MonochromeIconColors.PRIMARY
    MonochromeIconColorsOption.SECONDARY -> Settings.MonochromeIconColors.SECONDARY
    MonochromeIconColorsOption.TERTIARY -> Settings.MonochromeIconColors.TERTIARY
    MonochromeIconColorsOption.BLACK -> Settings.MonochromeIconColors.BLACK
    MonochromeIconColorsOption.WHITE -> Settings.MonochromeIconColors.WHITE
}

private enum class ThemeOption : StaticOption {
    FOLLOW_SYSTEM {
        @Composable
        override fun title(): String = stringResource(R.string.theme_follow_system)
    },
    LIGHT {
        @Composable
        override fun title(): String = stringResource(R.string.theme_light)
    },
    DARK {
        @Composable
        override fun title(): String = stringResource(R.string.theme_dark)
    }
}

private fun Settings.Theme.toThemeOption() = when (this) {
    Settings.Theme.FOLLOW_SYSTEM -> ThemeOption.FOLLOW_SYSTEM
    Settings.Theme.LIGHT -> ThemeOption.LIGHT
    Settings.Theme.DARK -> ThemeOption.DARK
}

private fun ThemeOption.toTheme() = when (this) {
    ThemeOption.FOLLOW_SYSTEM -> Settings.Theme.FOLLOW_SYSTEM
    ThemeOption.LIGHT -> Settings.Theme.LIGHT
    ThemeOption.DARK -> Settings.Theme.DARK
}

@Composable
fun UiScreen(settingsViewModel: SettingsViewModel, navigateTo: (Route) -> Unit) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle(initialValue = null)
    settings?.let { settings ->
        Column {
            FixedChooseOptionSetting(
                value = settings.theme.toThemeOption(),
                onValueChanged = { settingsViewModel.setTheme(it.toTheme()) },
                title = stringResource(R.string.setting_theme)
            )

            BasicSettingsItem(
                title = stringResource(R.string.setting_open_wallpaper_picker),
                onClick = { navigateTo(ChangeWallpaperExternal) }
            )

            HorizontalDivider()

            ToggleSetting(
                value = settings.monochromeIconsEnabled,
                onValueChanged = { value -> settingsViewModel.setMonochromeIconsEnabled(value) },
                title = stringResource(R.string.setting_monochrome_icons)
            )

            FixedChooseOptionSetting(
                value = settings.monochromeIconColors.toMonochromeIconColorsOption(),
                onValueChanged = { settingsViewModel.setMonochromeIconColors(it.toMonochromeIconColors()) },
                title = stringResource(R.string.setting_monochrome_icon_colors),
                hasIcons = true,
                enabled = settings.monochromeIconsEnabled,
            )

            val availableIconPacks by remember {
                settingsViewModel.availableIconPacks.map { available ->
                    if (available != null) {
                        listOf(SystemIconPack) +
                                available.map { IconPackOption(it.packageName, it.name, it.icon) }
                                    .sortedBy(String.CASE_INSENSITIVE_ORDER) { it.title!! }
                    } else null
                }
            }.collectAsState(initial = null)
            DynamicChooseOptionSetting(
                value = availableIconPacks?.find { it.id == settings.iconPack } ?: SystemIconPack,
                onValueChanged = { settingsViewModel.setIconPack(it.id) },
                title = stringResource(R.string.setting_icon_pack),
                options = availableIconPacks ?: emptyList(),
                hasIcons = true,
                enabled = availableIconPacks != null, // Disable while loading
            )

            HorizontalDivider()

            ToggleSetting(
                value = settings.widgetsEnabled,
                onValueChanged = { settingsViewModel.setWidgetsEnabled(it) },
                title = stringResource(R.string.setting_widgets_enabled)
            )

            ToggleSetting(
                value = settings.resizeWidgets,
                onValueChanged = { value -> settingsViewModel.setResizeWidgets(value) },
                title = stringResource(R.string.setting_resize_widgets),
                description = stringResource(R.string.setting_resize_widgets_description),
                enabled = settings.widgetsEnabled
            )

            HorizontalDivider()

            FixedChooseOptionSetting(
                value = settings.imeButtonBehavior.toButtonBehaviorOption(),
                onValueChanged = { settingsViewModel.setImeButtonBehavior(it.toButtonBehavior()) },
                title = stringResource(R.string.setting_ime_action),
                description = stringResource(R.string.setting_ime_action_description),
            )
            FixedChooseOptionSetting(
                value = settings.spaceKeyBehavior.toButtonBehaviorOption(),
                onValueChanged = { settingsViewModel.setSpaceKeyBehavior(it.toButtonBehavior()) },
                title = stringResource(R.string.setting_space_key_action),
                description = stringResource(R.string.setting_space_key_action_description),
            )
        }
    }
}
