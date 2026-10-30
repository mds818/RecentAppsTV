package com.websnake.recentstv.data

/** Starting looks, not saved user profiles. Default also supplies the requested interaction defaults. */
enum class AppearancePreset(val title: String, val description: String) {
    DEFAULT("Default", "App names and guidance · adaptive edge switch · dismissal enabled"),
    CLEAN("Clean", "Centered · app names · Normal padding · Kill All with apps"),
    CLEAN_PLUS("Clean+", "Centered icons · hidden labels · top actions on UP"),
    CLASSIC("Classic", "Medium rounded cards · bottom labels · original-style side layout");

    fun appearance(): RecentsSettings = (when (this) {
        CLEAN_PLUS -> RecentsSettings(
            edgeShortcut = EdgeShortcut.ADAPTIVE, restoreInTop = true, hideTopRestore = true,
            bulkWithAccess = BulkWithAccess.CLOSE_ALL, hideTopBulk = true,
            viewSwitchMode = ViewSwitchMode.HIDDEN,
            cardPadding = CardPadding.COMPACT,
            showHeader = false, showSettingsButton = false, showHelp = false,
            showAppNames = false, appDetailsMode = AppDetailsMode.OFF,
            killAllLocation = KillAllLocation.TOP_CONTROLS,
        )
        CLEAN -> RecentsSettings(
            viewButtons = ViewButtons.HIDDEN, edgeShortcut = EdgeShortcut.ADAPTIVE, showTaskIndicator = true,
            restoreInTop = true, hideTopRestore = true,
            showHeader = true, showSettingsButton = false, showHelp = false,
            appDetailsMode = AppDetailsMode.OFF,
        )
        DEFAULT -> RecentsSettings(restoreInTop = true, hideTopRestore = false, viewButtons = ViewButtons.HIDDEN, edgeShortcut = EdgeShortcut.ADAPTIVE,
            edgeAction = EdgeAction.SWITCH_VIEW, enableDismiss = true, downWithoutAccess = DownWithoutAccess.DISMISS,
            showTaskIndicator = true, downWithAccess = DownWithAccess.CLOSE_OR_DISMISS)
        CLASSIC -> RecentsSettings(
            edgeShortcut = EdgeShortcut.ADAPTIVE, restoreInTop = true, hideTopRestore = false,
            position = FocusPosition.SIDE,
            size = com.websnake.recentstv.ui.theme.CardSizeOpt.MEDIUM,
            shape = com.websnake.recentstv.ui.theme.CardShapeOpt.ROUNDED,
            cardPadding = CardPadding.WIDE,
            bgDim = 60, bgTint = com.websnake.recentstv.ui.theme.BgTint.CHARCOAL,
            showSettingsButton = true, showHelp = false,
            appTextPosition = AppTextPosition.BOTTOM,
            closeHintStyle = CloseHintStyle.ELEVATED,
        )
    }).copy(rememberView = true, emptyListRecovery = EmptyListRecovery.MENU, restoreTopMode = RestoreTopMode.RECENT_ONLY,
        viewNoticeStyle = if (this == CLEAN_PLUS) ViewNoticeStyle.SUBTLE else ViewNoticeStyle.OFF)
}
