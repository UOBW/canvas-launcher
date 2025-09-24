package io.github.canvas.data.settings

/** User preferences */
data class Settings(
    val contactSearchEnabled: Boolean,
    /** If true, hides dynamic shortcuts with the same name as a contact */
    val hideContactShortcuts: Boolean,
    val calendarSearchEnabled: Boolean,
    val appStoreSearchEnabled: Boolean,
    val initialResults: InitialResults,
    val sortResultsByUsage: Boolean,

    val theme: Theme,
    val monochromeIconsEnabled: Boolean,
    val monochromeIconColors: MonochromeIconColors,
    val iconPack: String?,

    val widgetsEnabled: Boolean,
    /** Resize widgets when the on-screen keyboard is expanded */
    val resizeWidgets: Boolean,

    val imeButtonBehavior: ButtonBehavior,
    val spaceKeyBehavior: ButtonBehavior,

    val onboardingShown: Boolean,
    val showDeveloperOptions: Boolean,
) {
    enum class ButtonBehavior {
        OPEN_FIRST_RESULT, HIDE_KEYBOARD, CLEAR_SEARCH_TERM, DO_NOTHING
    }

    enum class MonochromeIconColors {
        PRIMARY, SECONDARY, TERTIARY, BLACK, WHITE
    }

    enum class Theme {
        FOLLOW_SYSTEM, LIGHT, DARK
    }

    enum class InitialResults {
        FAVORITES, ALL_APPS, NOTHING
    }

    companion object {
        val defaultImeButtonBehavior: ButtonBehavior = ButtonBehavior.OPEN_FIRST_RESULT
        val defaultSpaceKeyBehavior: ButtonBehavior = ButtonBehavior.DO_NOTHING
        val defaultMonochromeIconColors: MonochromeIconColors = MonochromeIconColors.PRIMARY
        val defaultTheme: Theme = Theme.FOLLOW_SYSTEM
        val defaultInitialResults: InitialResults = InitialResults.ALL_APPS
    }
}
