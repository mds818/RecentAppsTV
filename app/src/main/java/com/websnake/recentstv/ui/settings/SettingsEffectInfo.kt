package com.websnake.recentstv.ui.settings

import androidx.compose.ui.graphics.Color
import com.websnake.recentstv.data.*
import com.websnake.recentstv.ui.theme.Tokens

/** Explanations describe the current effect, never disable or silently rewrite a saved choice. */
internal fun settingEffectInfo(row: SetRow, s: RecentsSettings, access: Boolean?, usageAccessGranted: Boolean, managerName: String = "Porter / Shizuku"): Pair<String, Color>? {
    val neutral = Tokens.textDim
    val amber = Color(0xFFE0B56A)
    val ready = access == true
    val history = ListSourcePolicy.resolve(s.listMode, ready, usageAccessGranted) == AppListSource.HISTORY
    val dismissAllowed = s.enableDismiss && DismissPolicy.applies(s.dismissScope, ListSourcePolicy.resolve(s.listMode, ready, usageAccessGranted))
    val down = ActionPolicy.downForAccess(s.downWithAccess, s.downWithoutAccess, ready, ready, dismissAllowed)
    val bulk = ActionPolicy.bulkForAccess(s.bulkWithAccess, s.bulkWithoutAccess, ready, dismissAllowed, ListSourcePolicy.resolve(s.listMode, ready, usageAccessGranted))
    fun porter() = when (access) {
        null -> "Checking $managerName access…" to neutral
        true -> "$managerName · Active" to Tokens.accent
        false -> "$managerName · Not active" to amber
    }
    val dismissSelected = if (ready) (s.bulkWithAccess == BulkWithAccess.DISMISS_ALL || (s.bulkWithAccess == BulkWithAccess.BY_VIEW && history)) else s.bulkWithoutAccess == BulkWithoutAccess.DISMISS_ALL
    fun bulkUnavailable(): Pair<String, Color> = when {
        dismissSelected && !s.enableDismiss -> "Choose a Dismissal mode to show Dismiss all" to amber
        else -> "Bulk button is hidden in the current mode" to neutral
    }
    return when (row) {
        SetRow.VIEW_SWITCH -> when {
            s.viewButtons == ViewButtons.HIDDEN -> null
            access == null -> porter()
            !ready -> "$managerName · Not active · switch controls hidden without access" to amber
            !usageAccessGranted -> "Recent apps needs Usage Access; Tasks is available" to amber
            else -> "$managerName · Active · view switching available" to Tokens.accent
        }
        SetRow.EDGE_SHORTCUT -> if (s.edgeShortcut == EdgeShortcut.OFF) null else when (EdgeActionPolicy.resolve(s.edgeAction, s.edgeWithoutAccess, ready)) {
            EdgeAction.RESTORE -> if (s.dismissedApps.isEmpty()) "No dismissed entries to restore" to neutral else "Restore available" to neutral
            EdgeAction.OFF -> "Current mode action is Off · choose an action on this page" to amber
            EdgeAction.SWITCH_VIEW -> if (!ready) "Switch view needs $managerName · choose a fallback action" to amber else "Switch view · $managerName active" to neutral
            EdgeAction.DISMISS_ALL -> if (!dismissAllowed || !usageAccessGranted) "Dismiss all unavailable · check Dismissal and Usage Access" to amber else "Dismiss all · available without privileged access" to neutral
            EdgeAction.CLOSE_OR_DISMISS -> if (ready) "Close all · $managerName active" to neutral else if (dismissAllowed && usageAccessGranted) "Dismiss all · fallback available" to neutral else "Fallback dismissal unavailable · check Dismissal" to amber
            EdgeAction.MENU -> "Menu · only available actions are shown" to neutral
        }
        SetRow.ROW_SWITCH_POSITION -> if (!s.viewButtons.row && s.edgeShortcut == EdgeShortcut.OFF && s.emptyListRecovery == EmptyListRecovery.OFF) "Used by the app-row tile or edge shortcut" to neutral else null
        SetRow.REMEMBER_VIEW, SetRow.VIEW_NOTICE -> if (s.viewButtons == ViewButtons.HIDDEN && s.edgeShortcut == EdgeShortcut.OFF) "Used when a view-switch control is enabled" to neutral else null
        SetRow.HEADER -> when (s.headerMode) {
            HeaderMode.WITH_ACCESS -> porter()
            HeaderMode.WITHOUT_ACCESS -> if (ready) "Header is shown without privileged access" to neutral else "Header visible without privileged access" to neutral
            else -> null
        }
        SetRow.EMPTY_RECOVERY -> if (s.emptyListRecovery == EmptyListRecovery.OFF) null else "Only active for an empty list with dismissed entries · one press" to neutral
        SetRow.RESTORE_TOP -> when {
            s.restoreTopMode == RestoreTopMode.RECENT_ONLY && !history -> "Only shown in Recent apps view" to neutral
            s.restoreTopMode != RestoreTopMode.OFF && s.dismissedApps.isEmpty() -> "Button appears when dismissed entries exist" to neutral
            else -> null
        }
        SetRow.EDGE_ACTION -> when {
            access == null -> porter()
            !ready -> "Requires $managerName · Not active · using the fallback action below" to neutral
            s.edgeAction == EdgeAction.DISMISS_ALL && (!dismissAllowed || !usageAccessGranted) -> "Dismiss all unavailable · check Dismissal and Usage Access" to amber
            else -> "$managerName · Active · privileged-mode action" to neutral
        }
        SetRow.EDGE_WITHOUT_ACCESS -> when {
            s.edgeWithoutAccess == EdgeAction.OFF -> "Fallback gesture is off · choose Dismiss all or Menu to use it" to neutral
            s.edgeWithoutAccess == EdgeAction.DISMISS_ALL && (!s.enableDismiss || !usageAccessGranted) -> "Fallback Dismiss all unavailable · enable Dismissal and allow Usage Access" to amber
            else -> if (ready) "Saved fallback action · used without privileged access" to neutral else "Fallback action available" to neutral
        }
        SetRow.EDGE_MENU_SWITCH, SetRow.EDGE_MENU_CLOSE -> {
            val item = if (row == SetRow.EDGE_MENU_SWITCH) EdgeMenuItem.SWITCH_VIEW else EdgeMenuItem.CLOSE_ALL
            if (item !in s.edgeMenuItems) "Off · not included in the menu" to neutral
            else if (!ready) "Requires $managerName · Not active · hidden from the menu" to amber
            else if (item == EdgeMenuItem.CLOSE_ALL) "$managerName · Active · shown when closable tasks exist" to neutral else "$managerName · Active · available in the menu" to neutral
        }
        SetRow.EDGE_MENU_DISMISS -> if (EdgeMenuItem.DISMISS_ALL !in s.edgeMenuItems) "Off · not included in the menu" to neutral else if (!dismissAllowed || !usageAccessGranted) "Hidden in this mode · check Dismissal and Usage Access" to amber else "Available in the menu" to neutral
        SetRow.EDGE_MENU_RESTORE -> if (EdgeMenuItem.RESTORE_DISMISSED !in s.edgeMenuItems) "Off · not included in the menu" to neutral else if (s.dismissedApps.isEmpty()) "Hidden until apps have been dismissed" to neutral else "${s.dismissedApps.size} dismissed apps · restore available" to neutral
        SetRow.LIST_MODE -> when {
            access == null -> porter()
            ListSourcePolicy.canonical(s.listMode) == AppListMode.RECENTLY_USED && usageAccessGranted -> "Usage history · task actions available with $managerName" to neutral
            ListSourcePolicy.canonical(s.listMode) == AppListMode.RECENTLY_USED && ready -> "Usage Access unavailable · showing Android tasks" to amber
            ready -> "$managerName · Active · Android tasks" to Tokens.accent
            usageAccessGranted -> "$managerName · Not active · usage-history fallback" to amber
            else -> "$managerName · Not active · grant Usage Access to show recent apps" to amber
        }
        SetRow.DOWN_ACTION -> when {
            s.downWithAccess == DownWithAccess.DISMISS && s.enableDismiss && !dismissAllowed -> "Dismiss applies to Recent apps only" to neutral
            access == null -> porter()
            !ready -> "$managerName · Not active · saved for when access is active" to amber
            !s.enableDismiss && s.downWithAccess == DownWithAccess.DISMISS -> "Choose a Dismissal mode to use this action" to amber
            !s.enableDismiss && s.downWithAccess == DownWithAccess.CLOSE_OR_DISMISS -> "$managerName · Active · close only; dismiss is disabled" to Tokens.accent
            !usageAccessGranted && s.downWithAccess == DownWithAccess.DISMISS -> "$managerName · Active · allow Usage Access to dismiss" to amber
            !usageAccessGranted && s.downWithAccess == DownWithAccess.CLOSE_OR_DISMISS -> "$managerName · Active · close only; allow Usage Access to dismiss" to amber
            else -> "$managerName · Active · ${when (s.downWithAccess) {
                DownWithAccess.CLOSE_ONLY -> "close matching tasks only"
                DownWithAccess.CLOSE_OR_DISMISS -> "close tasks; dismiss unmatched entries"
                DownWithAccess.DISMISS -> "dismiss only"
                DownWithAccess.OFF -> "DOWN off"
            }}" to Tokens.accent
        }
        SetRow.BULK_ACTION -> when {
            access == null -> porter()
            !ready -> "$managerName · Not active · saved for when access is active" to amber
            dismissSelected && s.enableDismiss && !dismissAllowed -> "Dismiss applies to Recent apps only · button hidden in Tasks" to neutral
            s.bulkWithAccess == BulkWithAccess.BY_VIEW -> "$managerName · Active · Tasks: Close all; Recent apps: Dismiss all${if (!s.enableDismiss) " (dismissal is Off)" else ""}" to if (!s.enableDismiss && history) amber else neutral
            s.bulkWithAccess == BulkWithAccess.HIDDEN -> "$managerName · Active · button hidden" to Tokens.accent
            dismissSelected && !s.enableDismiss -> "Choose a Dismissal mode to show Dismiss all" to amber
            dismissSelected && !usageAccessGranted -> "Allow Usage Access to use Dismiss all" to amber
            else -> "$managerName · Active · ${if (bulk == DownAction.DISMISS) "Dismiss all" else "Close all"}" to Tokens.accent
        }
        SetRow.BULK_WITHOUT_ACCESS -> when {
            s.bulkWithoutAccess == BulkWithoutAccess.HIDDEN -> "Bulk button hidden in fallback mode" to neutral
            !s.enableDismiss -> "Fallback Dismiss all unavailable · Dismissal is Off" to amber
            !usageAccessGranted -> "Fallback Dismiss all unavailable · allow Usage Access" to amber
            ready -> "Dismiss all ready for fallback mode" to neutral
            else -> "Dismiss all available · hides entries without closing apps" to neutral
        }
        SetRow.KILL_ALL -> if (bulk == DownAction.OFF) bulkUnavailable() else if (bulk == DownAction.DISMISS && !usageAccessGranted) "Allow Usage Access to use Dismiss all" to amber else null
        SetRow.CONFIRM_KILL_ALL -> when {
            bulk == DownAction.OFF -> bulkUnavailable()
            s.bulkConfirmation == BulkConfirmation.CLOSE_ONLY && bulk == DownAction.DISMISS -> "Dismiss all skips confirmation in Close only" to neutral
            else -> null
        }
        SetRow.HIDDEN_KILL_EXCLUSION -> if (!ready) porter() else if (bulk != DownAction.CLOSE_TASK) "Used only by Close all, not Dismiss all" to neutral else porter()
        SetRow.DOWN_WITHOUT_ACCESS -> when {
            s.downWithoutAccess == DownWithoutAccess.OFF -> "Fallback DOWN action is Off" to neutral
            !s.enableDismiss -> "Dismiss unavailable · Dismissal is Off" to amber
            !usageAccessGranted -> "Dismiss unavailable · allow Usage Access" to amber
            ready -> "Dismiss ready for fallback mode" to neutral
            else -> "Dismiss available · hides an entry without closing the app" to neutral
        }
        SetRow.DISMISS_SCOPE -> if (!s.enableDismiss) "Applies to individual dismissal and Dismiss all. Enable dismiss first." to neutral else null
        SetRow.DISMISS_ENABLE -> if (s.enableDismiss && !usageAccessGranted) "Allow Usage Access for individual dismissal and Dismiss all" to amber else "Applies to individual dismissal and Dismiss all" to neutral
        SetRow.TASK_INDICATOR -> if (!history) "Only shown in the usage-history list" to neutral else porter()
        SetRow.FORCE_STOP, SetRow.PRIVILEGED, SetRow.PROTECTED_APPS, SetRow.INDIVIDUAL_PROTECTION -> porter()
        SetRow.CLOSE_HINT -> if (down == DownAction.DISMISS && !usageAccessGranted) "Dismiss inactive · allow Usage Access" to amber else if (down == DownAction.OFF) {
            if ((ready && s.downWithAccess == DownWithAccess.OFF) || (!ready && s.downWithoutAccess == DownWithoutAccess.OFF)) "Not used while DOWN is off" to neutral
            else "No DOWN action available with the current requirements" to amber
        } else null
        SetRow.CLOSE_SPEED -> when {
            !s.smoothMotion -> "Not used while Smooth motion is off" to neutral
            else -> null
        }
        SetRow.APP_DETAILS -> when (s.appDetailsMode) {
            AppDetailsMode.PRIVILEGED_ONLY -> porter()
            AppDetailsMode.ALWAYS -> "App category · works in both modes" to neutral
            AppDetailsMode.OFF -> "App details are hidden" to neutral
        }
        SetRow.APP_TEXT_POSITION -> if (!s.showAppNames && !s.appDetailsMode.visible(ready) && !(s.showTaskIndicator && history && ready))
            "Enable names or details to see text placement" to neutral else null
        else -> null
    }
}
