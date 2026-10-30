package com.websnake.recentstv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.websnake.recentstv.data.RecentsSettings
import com.websnake.recentstv.data.AppListMode
import com.websnake.recentstv.data.DownAction
import com.websnake.recentstv.data.BulkAction
import com.websnake.recentstv.ui.theme.*

enum class SetRow(val section: String? = null) {
    PROTECTION_OPTIONS, CONTEXT_RESTORE, DOWN_OPTIONS, RESTORE_TOP, HIDE_TOP_RESTORE, EDGE_HELP, EMPTY_RECOVERY, EDGE_OPTIONS, EDGE_WITHOUT_ACCESS, EDGE_MENU_RESTORE, RESTORE_CONFIRM, VIEW_OPTIONS, DISMISS_OPTIONS, BULK_OPTIONS, EDGE_MENU_OPTIONS, DISMISS_SCOPE, HIDE_TOP_BULK, HIDE_TOP_SWITCH, POSITION("Appearance"), DIRECTION, SIZE, SHAPE, PADDING, ACCENT, BG_DIM, BG_TINT, HEADER, SETTINGS_BUTTON, VIEW_SWITCH, EDGE_SHORTCUT, EDGE_ACTION, EDGE_MENU_SWITCH, EDGE_MENU_CLOSE, EDGE_MENU_DISMISS, EDGE_MENU_SETTINGS, ROW_SWITCH_POSITION, REMEMBER_VIEW, VIEW_NOTICE, HELP, APP_NAMES, APP_TEXT_POSITION, APP_DETAILS, MOTION, CLOSE_SPEED,
    EXCEPTIONS("Apps"), LIST_MODE, TASK_INDICATOR, DISMISS_ENABLE, DOWN_WITHOUT_ACCESS, RESTORE_DISMISSED, DOWN_ACTION, BULK_ACTION, BULK_WITHOUT_ACCESS,
    PRIVILEGED("Advanced · Shizuku / Porter"), MANAGER, KILL_ALL, FORCE_STOP,
    CLOSE_HINT, HIDDEN_KILL_EXCLUSION, PROTECTED_APPS, INDIVIDUAL_PROTECTION, DISMISS_PROTECTED, PROTECTION_FEEDBACK, CONFIRM_KILL_ALL, RESET("Defaults")
}

enum class SettingsSection(val title: String, val description: String, val rows: List<SetRow>) {
    APPEARANCE("Appearance", "Cards, colors, layout and app labels", listOf(SetRow.POSITION, SetRow.DIRECTION, SetRow.SIZE, SetRow.SHAPE, SetRow.PADDING, SetRow.ACCENT, SetRow.BG_DIM, SetRow.BG_TINT, SetRow.HEADER, SetRow.APP_NAMES, SetRow.APP_TEXT_POSITION, SetRow.APP_DETAILS)),
    CONTROLS("Controls", "Settings access, close hint and motion", listOf(SetRow.SETTINGS_BUTTON, SetRow.VIEW_OPTIONS, SetRow.EDGE_OPTIONS, SetRow.DOWN_OPTIONS, SetRow.HELP, SetRow.EDGE_HELP, SetRow.MOTION, SetRow.CLOSE_SPEED)),
    APPS("Apps", "App visibility and optional privileged closing", listOf(SetRow.EXCEPTIONS, SetRow.LIST_MODE, SetRow.TASK_INDICATOR, SetRow.DISMISS_OPTIONS, SetRow.BULK_OPTIONS, SetRow.PROTECTION_OPTIONS)),
    ADVANCED("Advanced", "Porter / Shizuku access and force-stop", listOf(SetRow.PRIVILEGED, SetRow.MANAGER, SetRow.FORCE_STOP));
    fun rowsFor(settings: RecentsSettings) = visibleSettingsRows(rows, settings)
    companion object { fun forRow(row: SetRow): SettingsSection? = entries.firstOrNull { row in it.rows } }
}

enum class SettingsSubmenu(val title: String, val rows: List<SetRow>) {
    VIEW("View switching", listOf(SetRow.VIEW_SWITCH, SetRow.HIDE_TOP_SWITCH, SetRow.ROW_SWITCH_POSITION, SetRow.REMEMBER_VIEW, SetRow.VIEW_NOTICE)),
    DOWN("DOWN action", listOf(SetRow.DOWN_ACTION, SetRow.DOWN_WITHOUT_ACCESS, SetRow.CLOSE_HINT)),
    SHORTCUT("Edge shortcut", listOf(SetRow.EDGE_SHORTCUT, SetRow.EDGE_ACTION, SetRow.EDGE_WITHOUT_ACCESS, SetRow.EDGE_MENU_OPTIONS, SetRow.ROW_SWITCH_POSITION, SetRow.EMPTY_RECOVERY)),
    DISMISS("Dismissal", listOf(SetRow.DISMISS_ENABLE, SetRow.RESTORE_DISMISSED, SetRow.RESTORE_TOP, SetRow.HIDE_TOP_RESTORE, SetRow.RESTORE_CONFIRM, SetRow.CONTEXT_RESTORE)),
    BULK("Bulk actions", listOf(SetRow.BULK_ACTION, SetRow.BULK_WITHOUT_ACCESS, SetRow.KILL_ALL, SetRow.HIDE_TOP_BULK, SetRow.CONFIRM_KILL_ALL, SetRow.HIDDEN_KILL_EXCLUSION)),
    PROTECTION("Protection", listOf(SetRow.PROTECTED_APPS, SetRow.INDIVIDUAL_PROTECTION, SetRow.DISMISS_PROTECTED, SetRow.PROTECTION_FEEDBACK)),
    EDGE("Edge menu", listOf(SetRow.EDGE_MENU_SWITCH, SetRow.EDGE_MENU_CLOSE, SetRow.EDGE_MENU_DISMISS, SetRow.EDGE_MENU_RESTORE, SetRow.EDGE_MENU_SETTINGS));
    fun rowsFor(settings: RecentsSettings) = visibleSettingsRows(rows, settings)
}

internal fun visibleSettingsRows(rows: List<SetRow>, settings: RecentsSettings) = rows.filter { when (it) {
    SetRow.EDGE_MENU_OPTIONS -> settings.emptyListRecovery == com.websnake.recentstv.data.EmptyListRecovery.MENU || (settings.edgeShortcut != com.websnake.recentstv.data.EdgeShortcut.OFF && (settings.edgeAction == com.websnake.recentstv.data.EdgeAction.MENU || settings.edgeWithoutAccess == com.websnake.recentstv.data.EdgeAction.MENU))
    SetRow.EDGE_MENU_CLOSE, SetRow.EDGE_MENU_DISMISS, SetRow.EDGE_MENU_RESTORE -> settings.edgeAction == com.websnake.recentstv.data.EdgeAction.MENU || settings.edgeWithoutAccess == com.websnake.recentstv.data.EdgeAction.MENU || settings.emptyListRecovery != com.websnake.recentstv.data.EmptyListRecovery.MENU
    SetRow.EDGE_ACTION, SetRow.EDGE_WITHOUT_ACCESS -> settings.edgeShortcut != com.websnake.recentstv.data.EdgeShortcut.OFF
    SetRow.HIDE_TOP_RESTORE -> settings.restoreTopMode != com.websnake.recentstv.data.RestoreTopMode.OFF
    SetRow.EDGE_HELP -> settings.showHelp
    SetRow.HIDE_TOP_BULK -> settings.killAllLocation.inTop
    SetRow.HIDE_TOP_SWITCH -> settings.viewButtons.top
    SetRow.DISMISS_SCOPE -> settings.enableDismiss
    SetRow.CLOSE_SPEED -> settings.smoothMotion
    SetRow.ROW_SWITCH_POSITION -> settings.viewButtons.row || settings.edgeShortcut != com.websnake.recentstv.data.EdgeShortcut.OFF || settings.emptyListRecovery != com.websnake.recentstv.data.EmptyListRecovery.OFF
    SetRow.REMEMBER_VIEW, SetRow.VIEW_NOTICE -> settings.viewButtons != com.websnake.recentstv.data.ViewButtons.HIDDEN || settings.edgeShortcut != com.websnake.recentstv.data.EdgeShortcut.OFF
    else -> true
} }

object SettingsMenu {
    val presets = 0
    val about = SettingsSection.entries.size + 1
    val reset = about + 1
    fun sectionAt(index: Int): SettingsSection? = SettingsSection.entries.getOrNull(index - 1)
}

@Composable
fun SettingsHome(focus: Int, smoothMotion: Boolean = true) {
    val listState = rememberLazyListState()
    KeepFocusedItemVisible(listState, focus, smoothMotion)
    Box(Modifier.fillMaxSize().background(Tokens.bg)) {
        Column(Modifier.fillMaxSize().padding(start = 60.dp, end = 60.dp, top = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Recent Apps · Settings", style = Type.title, color = Tokens.text)
            Text("↑↓ Choose · OK Open · BACK Return", style = Type.small, color = Tokens.textDim,
                modifier = Modifier.padding(bottom = 6.dp))
            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(SettingsMenu.reset + 1) { index ->
                    Column {
                    if (index == 1 || index == 4 || index == SettingsMenu.reset) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(Tokens.line))
                    }
                    val section = SettingsMenu.sectionAt(index)
                    if (section != null) {
                        SettingRow(section.title, section.description, focus == index, compact = true) {
                            Text("›", style = Type.title, color = Tokens.textDim)
                        }
                    } else if (index == SettingsMenu.presets || index == SettingsMenu.about) {
                        val presets = index == SettingsMenu.presets
                        SettingRow(if (presets) "Appearance presets" else "About",
                            if (presets) "Start with a look · Default, Clean, Clean+ and Classic" else "Updates, project links and optional access tools",
                            focus == index, compact = true) {
                            Text("›", style = Type.title, color = Tokens.textDim)
                        }
                    } else {
                        SettingRow("Reset to defaults", "Restore the default profile · confirmation required", focus == index, compact = true) {
                            Text("Reset…", style = Type.small, color = Tokens.textDim)
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(settings: RecentsSettings, section: SettingsSection, focus: Int, exceptionsSummary: String, protectedSummary: String, managerName: String, managerStatus: String, privilegedActive: Boolean? = null, usageAccessGranted: Boolean = true, rowsOverride: List<SetRow>? = null, titleOverride: String? = null) {
    val listState = rememberLazyListState()
    val rows = rowsOverride ?: section.rowsFor(settings)
    KeepFocusedItemVisible(listState, focus, settings.smoothMotion, listOf(section, settings, privilegedActive, usageAccessGranted))
    Box(Modifier.fillMaxSize().background(Tokens.bg)) {
        Column(Modifier.fillMaxSize().padding(start = 60.dp, end = 60.dp, top = 20.dp)) {
            Text("Settings · ${titleOverride ?: section.title}", style = Type.title, color = Tokens.text)
            Text("↑↓ Choose · ←→ Adjust · OK Toggle / Open", style = Type.small, color = Tokens.textDim,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(rows.size) { index ->
                    val row = rows[index]
                    Column {
                        val group = when (row) {
                            SetRow.POSITION -> "Layout"
                            SetRow.SIZE -> "Cards"
                            SetRow.ACCENT -> "Colors & background"
                            SetRow.HEADER -> "Information"
                            SetRow.SETTINGS_BUTTON -> "Access & guidance"
                            SetRow.VIEW_SWITCH -> "View switching"
                            SetRow.DOWN_ACTION -> "DOWN action"
                            SetRow.MOTION -> "Motion"
                            SetRow.LIST_MODE -> "App list"
                            SetRow.DISMISS_ENABLE -> "Dismiss from list"
                            SetRow.BULK_ACTION -> "Bulk actions"
                            SetRow.PROTECTED_APPS -> "Protection"
                            SetRow.EXCEPTIONS -> "App visibility"
                            else -> null
                        }
                        group?.let { if (titleOverride == null || index > 0) SettingsGroupHeading(it) }
                        val title = when (row) {
                            SetRow.CONTEXT_RESTORE -> "Restore in app menu"
                            SetRow.DOWN_OPTIONS -> "DOWN action"
                            SetRow.RESTORE_TOP -> "Show Restore in top controls"
                            SetRow.HIDE_TOP_RESTORE -> "Show only after pressing UP"
                            SetRow.EDGE_HELP -> "Include edge action in hints"
                            SetRow.EMPTY_RECOVERY -> "Empty-list recovery"
                            SetRow.EDGE_OPTIONS -> "Edge shortcut"
                            SetRow.EDGE_WITHOUT_ACCESS -> "Action without privileged access"
                            SetRow.EDGE_MENU_RESTORE -> "Menu: Restore dismissed apps"
                            SetRow.RESTORE_CONFIRM -> "Confirm restore dismissed apps"
                            SetRow.VIEW_OPTIONS -> "View switching"
                            SetRow.DISMISS_OPTIONS -> "Dismissal"
                            SetRow.BULK_OPTIONS -> "Bulk actions"
                            SetRow.EDGE_MENU_OPTIONS -> "Menu contents"
                            SetRow.DISMISS_SCOPE -> "Dismiss in"
                            SetRow.HIDE_TOP_BULK, SetRow.HIDE_TOP_SWITCH -> "Show only after pressing UP"
                            SetRow.BULK_ACTION -> "With privileged access"
                            SetRow.BULK_WITHOUT_ACCESS -> "Without privileged access"
                            SetRow.LIST_MODE -> "List source"
                            SetRow.TASK_INDICATOR -> "Show task indicator"
                            SetRow.DISMISS_ENABLE -> "Dismissal"
                            SetRow.DOWN_WITHOUT_ACCESS -> "Without privileged access"
                            SetRow.RESTORE_DISMISSED -> "Restore dismissed apps"
                            SetRow.DOWN_ACTION -> "With privileged access"
                            SetRow.POSITION -> "Selection position"
                            SetRow.DIRECTION -> "Older apps appear"
                            SetRow.SIZE -> "Card size"
                            SetRow.SHAPE -> "Card shape"
                            SetRow.PADDING -> "Card padding"
                            SetRow.ACCENT -> "Accent color"
                            SetRow.BG_DIM -> "Background dim"
                            SetRow.BG_TINT -> "Background color"
                            SetRow.HEADER -> "Show header"
                            SetRow.SETTINGS_BUTTON -> "Show settings button"
                            SetRow.VIEW_SWITCH -> "Switch buttons"
                            SetRow.EDGE_SHORTCUT -> "Edge shortcut"
                            SetRow.EDGE_ACTION -> "Action with privileged access"
                            SetRow.EDGE_MENU_SWITCH -> "Menu: Switch view"
                            SetRow.EDGE_MENU_CLOSE -> "Menu: Close all"
                            SetRow.EDGE_MENU_DISMISS -> "Menu: Dismiss all"
                            SetRow.EDGE_MENU_SETTINGS -> "Menu: Settings"
                            SetRow.ROW_SWITCH_POSITION -> "Row switch position"
                            SetRow.VIEW_NOTICE -> "View-change message"
                            SetRow.REMEMBER_VIEW -> "Remember switched view"
                            SetRow.APP_TEXT_POSITION -> "App text position"
                            SetRow.APP_NAMES -> "Show app names"
                            SetRow.APP_DETAILS -> "App details"
                            SetRow.CLOSE_HINT -> "Close hint style"
                            SetRow.HELP -> "Show control hints"
                            SetRow.MOTION -> "Smooth motion"
                            SetRow.CLOSE_SPEED -> "Animation speed"
                            SetRow.HIDDEN_KILL_EXCLUSION -> "Exclude hidden apps from Close all"
                            SetRow.EXCEPTIONS -> "App exceptions"
                            SetRow.PROTECTED_APPS -> "Protected apps"
                            SetRow.PROTECTION_OPTIONS -> "Protection"
                            SetRow.PROTECTION_FEEDBACK -> "Protection feedback"
                            SetRow.DISMISS_PROTECTED -> "Allow dismissing protected apps"
                            SetRow.INDIVIDUAL_PROTECTION -> "Protect individual closes"
                            SetRow.CONFIRM_KILL_ALL -> "Confirm bulk action"
                            SetRow.PRIVILEGED -> "Use Shizuku / Porter"
                            SetRow.MANAGER -> "Open $managerName"
                            SetRow.KILL_ALL -> "Button location"
                            SetRow.FORCE_STOP -> "Force-stop on close"
                            SetRow.RESET -> "Reset to defaults"
                        }
                        val desc = when (row) {
                            SetRow.CONTEXT_RESTORE -> "Add Restore below a divider in the long-press menu. Adaptive shows it when no top or edge Restore is reachable in this view; Settings is not counted."
                            SetRow.DOWN_OPTIONS -> "Actions in each mode and the second-press close hint"
                            SetRow.RESTORE_TOP -> "Add a Restore button while dismissed entries exist. Edge menu visibility is configured separately."
                            SetRow.HIDE_TOP_RESTORE -> "Keep Restore hidden while browsing; press UP to reveal it."
                            SetRow.EDGE_HELP -> "Show the current edge direction, action and press count in the bottom guidance."
                            SetRow.EMPTY_RECOVERY -> "When the list is empty and dismissed entries exist, temporarily offer one-press recovery. The menu uses enabled Switch/Settings items plus Restore; closing and dismissal are omitted."
                            SetRow.EDGE_OPTIONS -> "Edge gesture, actions in each mode and menu contents"
                            SetRow.EDGE_WITHOUT_ACCESS -> "Used without Porter / Shizuku. Off disables the gesture; Menu offers available actions."
                            SetRow.EDGE_MENU_RESTORE -> "Show dismissed apps again. Available when the dismissed list is not empty."
                            SetRow.RESTORE_CONFIRM -> "Ask before restoring the dismissed list from the edge menu."
                            SetRow.VIEW_OPTIONS -> "Switch buttons, placement, remembered view and change messages"
                            SetRow.DISMISS_OPTIONS -> "${if (settings.enableDismiss) "Enabled" else "Off"} · affected views and restore dismissed apps"
                            SetRow.BULK_OPTIONS -> "Behavior in each access mode, button placement and confirmation"
                            SetRow.EDGE_MENU_OPTIONS -> if (settings.edgeAction != com.websnake.recentstv.data.EdgeAction.MENU && settings.edgeWithoutAccess != com.websnake.recentstv.data.EdgeAction.MENU) "Recovery always includes Restore. Choose optional Switch view and Settings items." else "Choose which actions appear in the edge menu. Recovery omits bulk actions."
                            SetRow.DISMISS_SCOPE -> "Recent apps only keeps actual task entries visible in Tasks. Both views also hides task entries without closing them."
                            SetRow.HIDE_TOP_BULK, SetRow.HIDE_TOP_SWITCH -> "Keep this top button hidden while browsing; press UP to reveal it."
                            SetRow.BULK_ACTION -> "Close all closes tasks. Dismiss all only hides entries. Follow view: Tasks closes; Recent apps dismisses."
                            SetRow.BULK_WITHOUT_ACCESS -> "Choose the bulk button automatically used without access. Dismiss all requires Dismissal to be enabled; Hidden removes the button in this mode."
                            SetRow.LIST_MODE -> if (com.websnake.recentstv.data.ListSourcePolicy.canonical(settings.listMode) == AppListMode.RECENTLY_USED)
                                "Prefer usage history in both modes. $managerName adds task actions. If Usage Access is unavailable, use Android tasks while access is active."
                            else "Prefer Android tasks with $managerName access; use recently used apps without it. Your saved preference returns automatically when access is available."
                            SetRow.TASK_INDICATOR -> "In usage history, mark the selected app when an Android task is available."
                            SetRow.DOWN_WITHOUT_ACCESS -> "Used automatically when $managerName access is inactive. Dismiss requires Dismissal to be enabled; Off leaves actions to long press."
                            SetRow.DISMISS_ENABLE -> "Controls individual dismissal and Dismiss all. Recent apps only keeps actual task entries visible in Tasks."
                            SetRow.RESTORE_DISMISSED -> "${settings.dismissedApps.size} ${if (settings.dismissedApps.size == 1) "app" else "apps"} dismissed. OK restores them; App exceptions are unchanged."
                            SetRow.DOWN_ACTION -> "Used automatically when $managerName access is active. Close or dismiss closes matching tasks and hides entries without a task; close protection is respected."
                            SetRow.POSITION -> "Keep the selected card centered, or let the row reach either edge."
                            SetRow.DIRECTION -> "Choose which way to browse from the most recent app."
                            SetRow.SIZE -> "The size of application cards."
                            SetRow.SHAPE -> "Use square, rounded, or circular cards."
                            SetRow.PADDING -> "Change the gray space around each icon without changing the card size."
                            SetRow.ACCENT -> "OK opens presets and more colors. Preview before applying."
                            SetRow.BG_DIM -> "Darken the app behind the overlay."
                            SetRow.BG_TINT -> "Tint applied with the background dim."
                            SetRow.HEADER -> "Show the title, count and current view. Privileged only uses Porter / Shizuku; Fallback only uses usage history. Settings visibility is independent."
                            SetRow.SETTINGS_BUTTON -> "When hidden, press UP twice to open Settings."
                            SetRow.VIEW_SWITCH -> "Choose a switch beside Settings, as a tile in the app row, or both."
                            SetRow.EDGE_ACTION -> "Used while Porter / Shizuku is active. Close all respects protection. The fallback action is configured separately."
                            SetRow.EDGE_MENU_SWITCH, SetRow.EDGE_MENU_CLOSE, SetRow.EDGE_MENU_DISMISS, SetRow.EDGE_MENU_SETTINGS -> "Include in the edge menu when available. Settings is the fallback if no selected action is available."
                            SetRow.EDGE_SHORTCUT -> "Act beyond the selected edge. Adaptive uses one press with a row action button, otherwise two. Held keys do not activate the action."
                            SetRow.ROW_SWITCH_POSITION -> "Start is beside the newest apps; End beside the oldest. Both adds a switch at each end. The edge shortcut follows the same choice and browse direction."
                            SetRow.VIEW_NOTICE -> "Show the new view name subtly, or larger with an icon. Prominent stays visible slightly longer."
                            SetRow.REMEMBER_VIEW -> "On saves an intentional switch as your preferred list. Off restores the preferred list next time. Automatic fallback never changes it."
                            SetRow.APP_TEXT_POSITION -> "Place names and details under the selected app or at the bottom. Independent of control hints."
                            SetRow.APP_NAMES -> "Show the selected app name. Turn off for an icon-only view."
                            SetRow.APP_DETAILS -> "Show app category. Privileged only shows it while privileged access is active, in either list."
                            SetRow.CLOSE_HINT -> "Style of the second-press hint beneath an app."
                            SetRow.HELP -> "Show remote-control hints along the bottom."
                            SetRow.MOTION -> "Animate focus, scrolling and view changes. Off gives immediate transitions."
                            SetRow.CLOSE_SPEED -> "Speed of close/dismiss, Restore, view changes and bulk animations. Focus feedback and confirmation timeouts stay unchanged."
                            SetRow.HIDDEN_KILL_EXCLUSION -> "Off allows Close all to close hidden apps; protected apps are always skipped."
                            SetRow.EXCEPTIONS -> "$exceptionsSummary · Works in both modes"
                            SetRow.PROTECTED_APPS -> "$protectedSummary · Choose how protection applies below."
                            SetRow.PROTECTION_OPTIONS -> "$protectedSummary · Closing, dismissal and feedback"
                            SetRow.PROTECTION_FEEDBACK -> "Feedback when protection blocks DOWN. Smooth motion off suppresses animation."
                            SetRow.DISMISS_PROTECTED -> "Allow hiding protected apps without closing them. DOWN dismisses instead of closing when dismissal is enabled. Off also skips protected apps in Dismiss all."
                            SetRow.INDIVIDUAL_PROTECTION -> "Prevent task closing via DOWN or long press. Dismissal follows the separate option below."
                            SetRow.CONFIRM_KILL_ALL -> "Always confirms both actions. Close only skips confirmation for Dismiss all. Off performs either action immediately."
                            SetRow.PRIVILEGED -> "When enabled, use actual tasks if authorized; otherwise use history."
                            SetRow.MANAGER -> "$managerStatus. OK opens the manager or requests access."
                            SetRow.KILL_ALL -> "Placement applies in both modes. The button label and icon follow the active rule above."
                            SetRow.FORCE_STOP -> "Also stop the app when closing its task."
                            SetRow.RESET -> "Restore the preferred profile and clear app exceptions. Confirmation required."
                        }
                        val info = settingEffectInfo(row, settings, privilegedActive, usageAccessGranted, managerName)
                        SettingRow(title, desc, focus == index,
                            dependent = row in setOf(SetRow.EDGE_HELP, SetRow.HIDE_TOP_RESTORE, SetRow.HIDE_TOP_SWITCH, SetRow.HIDE_TOP_BULK, SetRow.DISMISS_SCOPE, SetRow.EDGE_ACTION, SetRow.EDGE_WITHOUT_ACCESS, SetRow.EDGE_MENU_OPTIONS, SetRow.CLOSE_SPEED),
                            accessLabel = info?.first, accessColor = info?.second ?: Tokens.textDim) {
                            when (row) {
                                SetRow.DOWN_OPTIONS, SetRow.EDGE_OPTIONS, SetRow.VIEW_OPTIONS, SetRow.DISMISS_OPTIONS, SetRow.BULK_OPTIONS, SetRow.EDGE_MENU_OPTIONS -> Text("›", style = Type.title, color = Tokens.textDim)
                                SetRow.CONTEXT_RESTORE -> Segmented(listOf("Off", "On", "Adaptive"), settings.contextRestoreMode.ordinal)
                                SetRow.RESTORE_TOP -> Segmented(listOf("Off", "All views", "Recent only"), settings.restoreTopMode.ordinal)
                                SetRow.HIDE_TOP_RESTORE -> SettingsToggle(settings.hideTopRestore)
                                SetRow.EDGE_HELP -> SettingsToggle(settings.showEdgeHints)
                                SetRow.EMPTY_RECOVERY -> Segmented(listOf("Off", "Actions menu", "Restore"), settings.emptyListRecovery.ordinal)
                                SetRow.DISMISS_SCOPE -> Segmented(listOf("Recent apps only", "Both views"), settings.dismissScope.ordinal)
                                SetRow.HIDE_TOP_BULK -> SettingsToggle(settings.hideTopBulk)
                                SetRow.HIDE_TOP_SWITCH -> SettingsToggle(settings.hideTopSwitch)
                                SetRow.BULK_ACTION -> Segmented(listOf("Close all", "Dismiss all", "Hidden", "Follow view"), settings.bulkWithAccess.ordinal)
                                SetRow.BULK_WITHOUT_ACCESS -> Segmented(listOf("Dismiss all", "Hidden"), settings.bulkWithoutAccess.ordinal)
                                SetRow.LIST_MODE -> Segmented(listOf("Android tasks when available", "Recently used"), com.websnake.recentstv.data.ListSourcePolicy.choices.indexOf(com.websnake.recentstv.data.ListSourcePolicy.canonical(settings.listMode)).coerceAtLeast(0))
                                SetRow.TASK_INDICATOR -> SettingsToggle(settings.showTaskIndicator)
                                SetRow.DISMISS_ENABLE -> Segmented(listOf("Off", "Recent apps only", "Both views"), if (!settings.enableDismiss) 0 else settings.dismissScope.ordinal + 1)
                                SetRow.DOWN_WITHOUT_ACCESS -> Segmented(listOf("Dismiss", "Off"), settings.downWithoutAccess.ordinal)
                                SetRow.RESTORE_DISMISSED -> Text("Restore", style = Type.small, color = Tokens.textDim)
                                SetRow.DOWN_ACTION -> Segmented(listOf("Close only", "Close or dismiss", "Dismiss only", "Off"), settings.downWithAccess.ordinal)
                                SetRow.POSITION -> Segmented(listOf("Centered", "Side"), settings.position.ordinal)
                                SetRow.DIRECTION -> Segmented(listOf("← Left", "Right →"), settings.direction.ordinal)
                                SetRow.SIZE -> Segmented(listOf("Small", "Medium", "Large"), settings.size.ordinal)
                                SetRow.SHAPE -> { ShapePreview(settings.shape); Spacer(Modifier.width(11.dp)); Segmented(listOf("Square", "Rounded", "Circle"), settings.shape.ordinal) }
                                SetRow.PADDING -> Segmented(listOf("None", "Compact", "Normal", "Wide"), settings.cardPadding.ordinal)
                                SetRow.ACCENT -> {
                                    Box(Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(settings.accentColor))
                                    Spacer(Modifier.width(16.dp))
                                    Text("›", style = Type.title, color = Tokens.textDim)
                                }
                                SetRow.BG_DIM -> DimBar(settings.bgDim)
                                SetRow.BG_TINT -> SwatchRow(BgTint.entries.map { it.color }, settings.bgTint.ordinal, ring = true)
                                SetRow.HEADER -> Segmented(listOf("Off", "Always", "Privileged only", "Fallback only"), settings.headerMode.ordinal)
                                SetRow.SETTINGS_BUTTON -> SettingsToggle(settings.showSettingsButton)
                                SetRow.VIEW_SWITCH -> Segmented(listOf("Top controls", "App row", "Both", "Hidden"), settings.viewButtons.ordinal)
                                SetRow.EDGE_SHORTCUT -> Segmented(listOf("Off", "One press", "Two presses", "Adaptive"), settings.edgeShortcut.ordinal)
                                SetRow.ROW_SWITCH_POSITION -> Segmented(listOf("Start", "End", "Both"), settings.rowSwitchPosition.ordinal)
                                SetRow.VIEW_NOTICE -> Segmented(listOf("Off", "Subtle", "Prominent"), settings.viewNoticeStyle.ordinal)
                                SetRow.EDGE_ACTION -> Segmented(listOf("Off", "Switch", "Close all", "Dismiss all", "Menu"), com.websnake.recentstv.data.EdgeActionPolicy.privilegedChoices.indexOf(settings.edgeAction).coerceAtLeast(0))
                                SetRow.EDGE_WITHOUT_ACCESS -> Segmented(listOf("Off", "Dismiss all", "Menu"), com.websnake.recentstv.data.EdgeActionPolicy.fallbackChoices.indexOf(settings.edgeWithoutAccess).coerceAtLeast(0))
                                SetRow.EDGE_MENU_RESTORE -> SettingsToggle(com.websnake.recentstv.data.EdgeMenuItem.RESTORE_DISMISSED in settings.edgeMenuItems)
                                SetRow.RESTORE_CONFIRM -> SettingsToggle(settings.confirmRestoreDismissed)
                                SetRow.EDGE_MENU_SWITCH -> SettingsToggle(com.websnake.recentstv.data.EdgeMenuItem.SWITCH_VIEW in settings.edgeMenuItems)
                                SetRow.EDGE_MENU_CLOSE -> SettingsToggle(com.websnake.recentstv.data.EdgeMenuItem.CLOSE_ALL in settings.edgeMenuItems)
                                SetRow.EDGE_MENU_DISMISS -> SettingsToggle(com.websnake.recentstv.data.EdgeMenuItem.DISMISS_ALL in settings.edgeMenuItems)
                                SetRow.EDGE_MENU_SETTINGS -> SettingsToggle(com.websnake.recentstv.data.EdgeMenuItem.SETTINGS in settings.edgeMenuItems)
                                SetRow.REMEMBER_VIEW -> SettingsToggle(settings.rememberView)
                                SetRow.APP_TEXT_POSITION -> Segmented(listOf("Under app", "Bottom"), settings.appTextPosition.ordinal)
                                SetRow.APP_NAMES -> SettingsToggle(settings.showAppNames)
                                SetRow.APP_DETAILS -> Segmented(listOf("Off", "Always", "Privileged only"), settings.appDetailsMode.ordinal)
                                SetRow.CLOSE_HINT -> Segmented(listOf("Elevated", "Minimal"), settings.closeHintStyle.ordinal)
                                SetRow.HELP -> SettingsToggle(settings.showHelp)
                                SetRow.MOTION -> SettingsToggle(settings.smoothMotion)
                                SetRow.CLOSE_SPEED -> Segmented(listOf("Fast", "Normal", "Slow"), settings.animationSpeed.ordinal)
                                SetRow.PRIVILEGED -> SettingsToggle(settings.usePrivileged)
                                SetRow.KILL_ALL -> Segmented(listOf("App row", "Top controls", "Both"), listOf(com.websnake.recentstv.data.KillAllLocation.APP_ROW, com.websnake.recentstv.data.KillAllLocation.TOP_CONTROLS, com.websnake.recentstv.data.KillAllLocation.BOTH).indexOf(settings.killAllLocation).coerceAtLeast(0))
                                SetRow.FORCE_STOP -> SettingsToggle(settings.forceStopOnKill)
                                SetRow.RESET -> Text("Reset…", style = Type.small, color = Tokens.textDim)
                                SetRow.HIDDEN_KILL_EXCLUSION -> SettingsToggle(settings.excludeHiddenFromKillAll)
                                SetRow.EXCEPTIONS -> Text("›", style = Type.title, color = Tokens.textDim)
                                SetRow.PROTECTED_APPS -> Text("›", style = Type.title, color = Tokens.textDim)
                                SetRow.PROTECTION_OPTIONS -> Text("›", style = Type.title, color = Tokens.textDim)
                                SetRow.PROTECTION_FEEDBACK -> Segmented(listOf("Off", "Hint only", "Animation only", "Animation + hint"), settings.protectionFeedback.ordinal)
                                SetRow.DISMISS_PROTECTED -> SettingsToggle(settings.allowDismissProtected)
                                SetRow.INDIVIDUAL_PROTECTION -> SettingsToggle(settings.protectIndividualCloses)
                                SetRow.CONFIRM_KILL_ALL -> Segmented(listOf("Always", "Close only", "Off"), settings.bulkConfirmation.ordinal)
                                SetRow.MANAGER -> Text("↗", style = Type.row, color = Tokens.textDim)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResetDefaultsDialog(confirmSelected: Boolean) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
        Column(Modifier.width(540.dp).clip(RoundedCornerShape(20.dp)).background(Tokens.surface)
            .border(1.dp, Tokens.line, RoundedCornerShape(20.dp)).padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("Reset to your default profile?", style = Type.title, color = Tokens.text)
            Text("Centered · older apps left · Large · Circle · Green · 80% dim · Black background",
                style = Type.body, color = Tokens.textDim)
            Text("Also clears hidden and protected app lists and the saved appearance. Android permissions stay unchanged.",
                style = Type.small, color = Tokens.textDim)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Segmented(listOf("Cancel", "Reset"), if (confirmSelected) 1 else 0)
            }
            Text("←→ Choose · OK Confirm · BACK Cancel", style = Type.small, color = Tokens.textDim)
        }
    }
}
@Composable
private fun SettingRow(title: String, desc: String, focused: Boolean, compact: Boolean = false, dependent: Boolean = false, accessLabel: String? = null, accessColor: Color = Tokens.textDim, control: @Composable () -> Unit) {
    val accent = LocalAccent.current
    Row(
        Modifier
            .padding(start = if (dependent) 18.dp else 0.dp)
            .drawBehind {
                if (dependent) drawLine(Tokens.line, androidx.compose.ui.geometry.Offset(-9.dp.toPx(), 0f),
                    androidx.compose.ui.geometry.Offset(-9.dp.toPx(), size.height), 1.dp.toPx())
            }
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(if (focused) Tokens.surface2 else Tokens.surface)
            .border(1.dp, if (focused) accent.base else Color.Transparent, RoundedCornerShape(11.dp))
            .padding(horizontal = 22.dp, vertical = if (compact) 10.dp else 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.row, color = Tokens.text)
            if (accessLabel != null) AccessInfoLabel(accessLabel, accessColor)
            Text(desc, style = Type.small, color = Tokens.textDim, modifier = Modifier.padding(top = 4.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) { control() }
    }
}

@Composable
internal fun SettingsToggle(on: Boolean) {
    val accent = LocalAccent.current
    Box(
        Modifier.size(width = 54.dp, height = 28.dp).clip(RoundedCornerShape(percent = 50))
            .background(if (on) accent.base else Color(0xFF2B2F36))
            .border(1.dp, if (on) accent.base else Tokens.line, RoundedCornerShape(percent = 50)),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier.padding(horizontal = 3.dp).size(21.dp).clip(RoundedCornerShape(percent = 50))
                .background(if (on) Color.White else Color(0xFF6B7280))
        )
    }
}

@Composable
private fun Segmented(labels: List<String>, selected: Int) {
    val accent = LocalAccent.current
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF16181C))
            .border(1.dp, Tokens.line, RoundedCornerShape(8.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        labels.forEachIndexed { i, lab ->
            val sel = i == selected
            Box(
                Modifier.clip(RoundedCornerShape(6.dp))
                    .background(if (sel) accent.base else Color.Transparent)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) { Text(lab, style = Type.small, color = if (sel) accent.onDark else Tokens.textDim) }
        }
    }
}

/** Row of colour swatches; the selected one gets a white ring. */
@Composable
private fun SwatchRow(colors: List<Color>, selected: Int, ring: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        colors.forEachIndexed { i, c ->
            val sel = i == selected
            Box(
                Modifier
                    .size(if (sel) 38.dp else 30.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(c)
                    .border(
                        if (sel) 2.5.dp else 1.dp,
                        if (sel) Color.White else Tokens.line,
                        RoundedCornerShape(percent = 50),
                    )
            )
        }
    }
}

/** 0–100% dim level shown as a filled bar with a percentage; ‹/› adjust. */
@Composable
private fun DimBar(pct: Int) {
    val accent = LocalAccent.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier
                .width(260.dp).height(14.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(Color(0xFF2B2F36)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(pct / 100f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(accent.base)
            )
        }
        Text("$pct%", style = Type.small, color = Tokens.text, modifier = Modifier.width(56.dp))
    }
}

@Composable
private fun ShapePreview(shape: CardShapeOpt) {
    Box(
        Modifier.size(38.dp).clip(shape.shape(38.dp)).background(Color(0xFF3A3F48))
            .border(1.dp, Tokens.line, shape.shape(38.dp))
    )
}




