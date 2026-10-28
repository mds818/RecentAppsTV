package com.websnake.recentstv.data

/** Starting looks, not saved user profiles. Only presentation fields are applied. */
enum class AppearancePreset(val title: String, val description: String) {
    DEFAULT("Default", "Reset appearance · app names and guidance · Kill All with apps"),
    CLEAN("Clean", "Centered · app names · Normal padding · Kill All with apps"),
    CLEAN_PLUS("Clean+", "Centered icons · no labels or header · hidden controls"),
    CLASSIC("Classic", "Medium rounded cards · bottom labels · original-style side layout");

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
        CLASSIC -> RecentsSettings(
            position = FocusPosition.SIDE,
            size = com.websnake.recentstv.ui.theme.CardSizeOpt.MEDIUM,
            shape = com.websnake.recentstv.ui.theme.CardShapeOpt.ROUNDED,
            cardPadding = CardPadding.WIDE,
            bgDim = 60, bgTint = com.websnake.recentstv.ui.theme.BgTint.CHARCOAL,
            showSettingsButton = true, showHelp = false,
            appTextPosition = AppTextPosition.BOTTOM,
            closeHintStyle = CloseHintStyle.ELEVATED,
        )
    }
}
