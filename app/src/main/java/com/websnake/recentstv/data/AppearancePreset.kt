package com.websnake.recentstv.data

/** Starting looks, not saved user profiles. Only presentation fields are applied. */
enum class AppearancePreset(val title: String, val description: String) {
    DEFAULT("Default", "Reset appearance · app names and guidance · Kill All with apps"),
    CLEAN("Clean", "Centered · app names · Normal padding · Kill All with apps"),
    CLEAN_PLUS("Clean+", "Centered icons · no labels or header · hidden controls"),
    CLASSIC("Classic", "Side layout · app names · Kill All with apps");

    fun appearance(): RecentsSettings = when (this) {
        CLEAN_PLUS -> RecentsSettings(
            cardPadding = CardPadding.COMPACT,
            showHeader = false, showSettingsButton = false, showHelp = false,
            showAppNames = false, appDetailsMode = AppDetailsMode.OFF,
            killAllLocation = KillAllLocation.HIDDEN,
        )
        CLEAN -> RecentsSettings(
            showHeader = false, showSettingsButton = false, showHelp = false,
            appDetailsMode = AppDetailsMode.OFF,
        )
        DEFAULT -> RecentsSettings()
        CLASSIC -> RecentsSettings(position = FocusPosition.SIDE)
    }
}
