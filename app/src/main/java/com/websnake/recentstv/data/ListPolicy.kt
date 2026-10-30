package com.websnake.recentstv.data

enum class AppListMode { AUTOMATIC, RECENTLY_USED, ANDROID_TASKS }
enum class ViewSwitchMode { BUTTON, RIGHT_EDGE, BOTH, HIDDEN;
    val button get() = this == BUTTON || this == BOTH
    val edge get() = this == RIGHT_EDGE || this == BOTH
}
enum class ViewButtons { TOP, APP_ROW, BOTH, HIDDEN;
    val top get() = this == TOP || this == BOTH
    val row get() = this == APP_ROW || this == BOTH
}
enum class EdgeShortcut { OFF, ONE_PRESS, TWO_PRESSES, ADAPTIVE }
enum class EmptyListRecovery { OFF, MENU, RESTORE }
enum class RestoreTopMode { OFF, ALL_VIEWS, RECENT_ONLY;
    fun visible(source: AppListSource) = this == ALL_VIEWS || (this == RECENT_ONLY && source == AppListSource.HISTORY)
}
object EdgeActivationPolicy {
    fun resolve(shortcut: EdgeShortcut, hasRowAction: Boolean, recovery: Boolean) = when {
        recovery -> EdgeShortcut.ONE_PRESS
        shortcut == EdgeShortcut.ADAPTIVE -> if (hasRowAction) EdgeShortcut.ONE_PRESS else EdgeShortcut.TWO_PRESSES
        else -> shortcut
    }
}
enum class RowSwitchPosition { START, END, BOTH }
enum class ViewNoticeStyle(val durationMs: Long) { OFF(0), SUBTLE(1600), PROMINENT(2400);
    companion object { fun legacy(enabled: Boolean) = if (enabled) SUBTLE else OFF }
}
enum class SwitchOrigin { TOP, ROW, EDGE }
object ViewSwitchPolicy {
    fun legacyButtons(mode: ViewSwitchMode) = if (mode.button) ViewButtons.TOP else ViewButtons.HIDDEN
    fun legacyShortcut(mode: ViewSwitchMode) = if (mode.edge) EdgeShortcut.ONE_PRESS else EdgeShortcut.OFF
    fun edgeIndex(count: Int, position: RowSwitchPosition) = if (position == RowSwitchPosition.START) 0 else (count - 1).coerceAtLeast(0)
    fun atEdge(focus: Int, count: Int, position: RowSwitchPosition) = count == 0 || focus == edgeIndex(count, position) || (position == RowSwitchPosition.BOTH && focus == 0)
    fun sideAt(focus: Int, count: Int, position: RowSwitchPosition) = if (position != RowSwitchPosition.BOTH) position else if (focus == 0 || count <= 1) RowSwitchPosition.START else RowSwitchPosition.END
    fun edgeIsRight(position: RowSwitchPosition, olderRight: Boolean) = (position == RowSwitchPosition.START) != olderRight

    fun atRightEdge(focus: Int, count: Int, olderRight: Boolean) =
        count == 0 || (count > 0 && focus == if (olderRight) count - 1 else 0)
}

enum class AppListSource { TASKS, HISTORY, NEEDS_ACCESS }

object ListSourcePolicy {
    val choices = listOf(AppListMode.ANDROID_TASKS, AppListMode.RECENTLY_USED)
    fun canonical(mode: AppListMode) = if (mode == AppListMode.AUTOMATIC) AppListMode.ANDROID_TASKS else mode
    fun adjust(mode: AppListMode, direction: Int): AppListMode =
        choices[(choices.indexOf(canonical(mode)).coerceAtLeast(0) + direction).coerceIn(0, choices.lastIndex)]
    fun resolve(mode: AppListMode, privileged: Boolean, usageAccess: Boolean): AppListSource = when {
        canonical(mode) == AppListMode.RECENTLY_USED && usageAccess -> AppListSource.HISTORY
        privileged -> AppListSource.TASKS
        usageAccess -> AppListSource.HISTORY
        else -> AppListSource.NEEDS_ACCESS
    }
}

enum class DownAction { CLOSE_TASK, DISMISS, AUTOMATIC, OFF }
enum class DownWithAccess { CLOSE_ONLY, CLOSE_OR_DISMISS, DISMISS, OFF }
enum class DownWithoutAccess { DISMISS, OFF }

enum class BulkAction { CLOSE_TASKS, DISMISS, AUTOMATIC }
enum class BulkWithAccess { CLOSE_ALL, DISMISS_ALL, HIDDEN, BY_VIEW }
enum class BulkWithoutAccess { DISMISS_ALL, HIDDEN }

enum class BulkConfirmation { ALWAYS, CLOSE_ONLY, OFF }

object ActionPolicy {
    fun protectedDown(action: DownAction, protectedClose: Boolean, dismissAllowed: Boolean, canDismiss: Boolean): DownAction =
        if (action == DownAction.CLOSE_TASK && protectedClose) {
            if (dismissAllowed && canDismiss) DownAction.DISMISS else DownAction.OFF
        } else if (action == DownAction.DISMISS && !dismissAllowed) DownAction.OFF else action

    fun legacyBulkWithAccess(choice: BulkAction, hidden: Boolean = false) = when {
        hidden -> BulkWithAccess.HIDDEN
        choice == BulkAction.DISMISS -> BulkWithAccess.DISMISS_ALL
        else -> BulkWithAccess.CLOSE_ALL
    }
    fun legacyBulkWithoutAccess(choice: BulkAction, hidden: Boolean = false) =
        if (hidden || choice == BulkAction.CLOSE_TASKS) BulkWithoutAccess.HIDDEN else BulkWithoutAccess.DISMISS_ALL
    fun bulkForAccess(withAccess: BulkWithAccess, withoutAccess: BulkWithoutAccess,
        privileged: Boolean, dismissEnabled: Boolean, source: AppListSource = AppListSource.TASKS): DownAction =
        if (!privileged) {
            if (withoutAccess == BulkWithoutAccess.DISMISS_ALL && dismissEnabled) DownAction.DISMISS else DownAction.OFF
        } else when (withAccess) {
            BulkWithAccess.BY_VIEW -> if (source == AppListSource.TASKS) DownAction.CLOSE_TASK else if (source == AppListSource.HISTORY && dismissEnabled) DownAction.DISMISS else DownAction.OFF
            BulkWithAccess.CLOSE_ALL -> DownAction.CLOSE_TASK
            BulkWithAccess.DISMISS_ALL -> if (dismissEnabled) DownAction.DISMISS else DownAction.OFF
            BulkWithAccess.HIDDEN -> DownAction.OFF
        }

    fun legacyWithAccess(choice: DownAction) = when (choice) {
        DownAction.DISMISS -> DownWithAccess.DISMISS
        DownAction.OFF -> DownWithAccess.OFF
        else -> DownWithAccess.CLOSE_ONLY
    }
    fun legacyWithoutAccess(choice: DownAction, fallback: Boolean) =
        if (choice == DownAction.DISMISS || (choice == DownAction.AUTOMATIC && fallback)) DownWithoutAccess.DISMISS else DownWithoutAccess.OFF
    fun downForAccess(withAccess: DownWithAccess, withoutAccess: DownWithoutAccess,
        privileged: Boolean, hasTask: Boolean, dismissEnabled: Boolean): DownAction {
        if (!privileged) return if (withoutAccess == DownWithoutAccess.DISMISS && dismissEnabled) DownAction.DISMISS else DownAction.OFF
        return when (withAccess) {
            DownWithAccess.CLOSE_ONLY -> if (hasTask) DownAction.CLOSE_TASK else DownAction.OFF
            DownWithAccess.CLOSE_OR_DISMISS -> if (hasTask) DownAction.CLOSE_TASK else if (dismissEnabled) DownAction.DISMISS else DownAction.OFF
            DownWithAccess.DISMISS -> if (dismissEnabled) DownAction.DISMISS else DownAction.OFF
            DownWithAccess.OFF -> DownAction.OFF
        }
    }

    val downChoices = listOf(DownAction.AUTOMATIC, DownAction.DISMISS, DownAction.OFF)
    fun migrateDown(raw: String?): DownAction = when (raw) {
        null, "CLOSE_TASK" -> DownAction.AUTOMATIC
        else -> runCatching { enumValueOf<DownAction>(raw) }.getOrDefault(DownAction.AUTOMATIC)
    }
    fun legacyFallback(raw: String?) = raw == "AUTOMATIC"
    fun confirm(choice: BulkConfirmation, action: DownAction): Boolean =
        action in listOf(DownAction.CLOSE_TASK, DownAction.DISMISS) &&
            (choice == BulkConfirmation.ALWAYS || (choice == BulkConfirmation.CLOSE_ONLY && action == DownAction.CLOSE_TASK))
    fun down(choice: DownAction, hasTask: Boolean, dismissEnabled: Boolean, fallbackEnabled: Boolean = false, privilegedAvailable: Boolean = hasTask): DownAction = when (choice) {
        DownAction.CLOSE_TASK -> if (hasTask) DownAction.CLOSE_TASK else DownAction.OFF
        DownAction.DISMISS -> if (dismissEnabled) DownAction.DISMISS else DownAction.OFF
        DownAction.AUTOMATIC -> if (hasTask) DownAction.CLOSE_TASK else if (dismissEnabled && fallbackEnabled && !privilegedAvailable) DownAction.DISMISS else DownAction.OFF
        DownAction.OFF -> DownAction.OFF
    }
    fun bulk(choice: BulkAction, privileged: Boolean, dismissEnabled: Boolean): DownAction = when (choice) {
        BulkAction.CLOSE_TASKS -> if (privileged) DownAction.CLOSE_TASK else DownAction.OFF
        BulkAction.DISMISS -> if (dismissEnabled) DownAction.DISMISS else DownAction.OFF
        BulkAction.AUTOMATIC -> if (privileged) DownAction.CLOSE_TASK else if (dismissEnabled) DownAction.DISMISS else DownAction.OFF
    }
    /** Stable IDs: dismiss, close, protection, permanent hiding, Android app info. */
    fun context(dismissEnabled: Boolean, canClose: Boolean, canProtect: Boolean, restore: Boolean = false): List<Int> = buildList {
        if (dismissEnabled) add(4)
        if (canClose) add(1)
        if (canProtect) add(2)
        add(0)
        add(3)
        if (restore) add(5)
    }
}

/** No background collector: compare Android's existing foreground events when loading the list. */
enum class DismissScope { RECENT_ONLY, BOTH_VIEWS }
object DismissPolicy {
    fun applies(scope: DismissScope, source: AppListSource) = source == AppListSource.HISTORY || (scope == DismissScope.BOTH_VIEWS && source == AppListSource.TASKS)
    fun decode(values: Set<String>): Map<String, Long> = values.mapNotNull {
        val split = it.indexOf('|')
        if (split < 1) null else it.substring(0, split).toLongOrNull()?.takeIf { n -> n >= 0 }
            ?.let { n -> it.substring(split + 1).takeIf { pkg -> pkg.isNotBlank() }?.let { pkg -> pkg to n } }
    }.toMap()
    fun encode(values: Map<String, Long>): Set<String> = values.map { (pkg, time) -> "$time|$pkg" }.toSet()
    fun remaining(dismissed: Map<String, Long>, foreground: Map<String, Long>): Map<String, Long> =
        dismissed.filter { (pkg, observed) -> (foreground[pkg] ?: 0L) <= observed }
    fun usesHistory(mode: AppListMode, privileged: Boolean): Boolean =
        mode == AppListMode.RECENTLY_USED || !privileged
}


enum class HeaderMode { OFF, ALWAYS, WITH_ACCESS, WITHOUT_ACCESS;
    fun visible(privileged: Boolean) = this == ALWAYS || (this == WITH_ACCESS && privileged) || (this == WITHOUT_ACCESS && !privileged)
    companion object { fun legacy(enabled: Boolean) = if (enabled) ALWAYS else OFF }
}
enum class EdgeAction { SWITCH_VIEW, CLOSE_OR_DISMISS, DISMISS_ALL, MENU, OFF, RESTORE }
object EdgeActionPolicy {
    val privilegedChoices = listOf(EdgeAction.OFF, EdgeAction.SWITCH_VIEW, EdgeAction.CLOSE_OR_DISMISS, EdgeAction.DISMISS_ALL, EdgeAction.MENU)
    val fallbackChoices = listOf(EdgeAction.OFF, EdgeAction.DISMISS_ALL, EdgeAction.MENU)
    fun legacyFallback(action: EdgeAction) = when (action) {
        EdgeAction.CLOSE_OR_DISMISS, EdgeAction.DISMISS_ALL -> EdgeAction.DISMISS_ALL
        EdgeAction.MENU -> EdgeAction.MENU
        else -> EdgeAction.OFF
    }
    fun resolve(withAccess: EdgeAction, withoutAccess: EdgeAction, privileged: Boolean) = if (privileged) withAccess else withoutAccess
}
enum class EdgeMenuItem(val label: String) { SWITCH_VIEW("Switch view"), CLOSE_ALL("Close all"), DISMISS_ALL("Dismiss all"), SETTINGS("Settings"), RESTORE_DISMISSED("Restore dismissed apps") }
object EdgeMenuPolicy {
    val defaults = setOf(EdgeMenuItem.SWITCH_VIEW, EdgeMenuItem.CLOSE_ALL, EdgeMenuItem.DISMISS_ALL, EdgeMenuItem.RESTORE_DISMISSED)
    fun selection(stored: Set<EdgeMenuItem>?, restoreConfigured: Boolean): Set<EdgeMenuItem> =
        (stored ?: defaults).let { if (restoreConfigured) it else it + EdgeMenuItem.RESTORE_DISMISSED }
    fun recoverySelection(configured: Set<EdgeMenuItem>) = (configured intersect setOf(EdgeMenuItem.SWITCH_VIEW, EdgeMenuItem.SETTINGS)) + EdgeMenuItem.RESTORE_DISMISSED
    fun available(selected: Set<EdgeMenuItem>, privileged: Boolean, canDismiss: Boolean, hasDismissed: Boolean = false, hasClosableTasks: Boolean = true, hasVisibleApps: Boolean = true): List<EdgeMenuItem> =
        listOf(EdgeMenuItem.SWITCH_VIEW, EdgeMenuItem.CLOSE_ALL, EdgeMenuItem.DISMISS_ALL, EdgeMenuItem.RESTORE_DISMISSED, EdgeMenuItem.SETTINGS).filter { it in selected && when (it) {
            EdgeMenuItem.SWITCH_VIEW -> privileged
            EdgeMenuItem.CLOSE_ALL -> privileged && hasClosableTasks
            EdgeMenuItem.DISMISS_ALL -> canDismiss && hasVisibleApps
            EdgeMenuItem.RESTORE_DISMISSED -> hasDismissed
            EdgeMenuItem.SETTINGS -> true
        }}
}
data class ViewNoticeEvent(val id: Long, val text: String, val style: ViewNoticeStyle)

enum class ContextRestoreMode { OFF, ON, ADAPTIVE }
object RestoreAccessPolicy {
    fun showInContext(mode: ContextRestoreMode, hasDismissed: Boolean, topReachable: Boolean, edgeReachable: Boolean) =
        hasDismissed && when (mode) {
            ContextRestoreMode.OFF -> false
            ContextRestoreMode.ON -> true
            ContextRestoreMode.ADAPTIVE -> !topReachable && !edgeReachable
        }
}
