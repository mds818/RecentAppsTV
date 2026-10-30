package com.websnake.recentstv.ui.settings

import androidx.compose.ui.graphics.Color
import com.websnake.recentstv.data.*
import com.websnake.recentstv.ui.theme.Tokens
import org.junit.Assert.*
import org.junit.Test

class SettingsEffectInfoTest {
    @Test fun automaticChoicesExposeActualPorterStatusEvenWhenFallbackWorks() {
        val s = RecentsSettings(enableDismiss = true, dismissFallback = true, bulkAction = BulkAction.AUTOMATIC)
        for (row in listOf(SetRow.LIST_MODE, SetRow.DOWN_ACTION, SetRow.BULK_ACTION)) {
            val on = settingEffectInfo(row, s, true, true)!!
            val off = settingEffectInfo(row, s, false, true)!!
            assertEquals(Tokens.accent, on.second)
            assertEquals(Color(0xFFE0B56A), off.second)
            assertTrue(on.first.contains("Porter / Shizuku · Active"))
            assertTrue(off.first.contains("Porter / Shizuku · Not active"))
        }
    }
    @Test fun everyPrivilegedOnlyRowHasItsOwnStatusInsteadOfRelyingOnAHeading() {
        val s = RecentsSettings(appDetailsMode = AppDetailsMode.PRIVILEGED_ONLY)
        for (row in listOf(SetRow.APP_DETAILS, SetRow.HIDDEN_KILL_EXCLUSION, SetRow.PROTECTED_APPS, SetRow.INDIVIDUAL_PROTECTION)) {
            assertEquals(Tokens.accent, settingEffectInfo(row, s, true, true)!!.second)
            assertEquals(Color(0xFFE0B56A), settingEffectInfo(row, s, false, true)!!.second)
        }
        val always = settingEffectInfo(SetRow.APP_DETAILS, s.copy(appDetailsMode = AppDetailsMode.ALWAYS), false, true)!!
        assertEquals(Tokens.textDim, always.second)
    }
    @Test fun fallbackVisibilityAndInactiveDependenciesDoNotChangeSavedChoices() {
        val s = RecentsSettings(enableDismiss = false, dismissFallback = true, downAction = DownAction.OFF)
        assertTrue(SetRow.DOWN_WITHOUT_ACCESS in SettingsSubmenu.DOWN.rowsFor(s))
        assertTrue(SetRow.DOWN_WITHOUT_ACCESS in SettingsSubmenu.DOWN.rowsFor(s.copy(enableDismiss = true)))
        assertNotNull(settingEffectInfo(SetRow.CLOSE_HINT, s, false, true))
        assertNotNull(settingEffectInfo(SetRow.CLOSE_SPEED, s.copy(smoothMotion = false), true, true))
        assertTrue(s.dismissFallback)
        assertEquals(DownAction.OFF, s.downAction)
    }
    @Test fun providerNameIsSharedByAllPrivilegedStatusRows() {
        val settings = RecentsSettings(enableDismiss = true, dismissFallback = true,
            bulkAction = BulkAction.AUTOMATIC, appDetailsMode = AppDetailsMode.PRIVILEGED_ONLY)
        for (name in listOf("Porter", "Shizuku")) for (active in listOf(false, true)) {
            for (row in listOf(SetRow.LIST_MODE, SetRow.DOWN_ACTION, SetRow.BULK_ACTION,
                SetRow.APP_DETAILS, SetRow.PROTECTED_APPS, SetRow.INDIVIDUAL_PROTECTION)) {
                val text = settingEffectInfo(row, settings, active, true, name)!!.first
                assertTrue(text, text.startsWith("$name · " + if (active) "Active" else "Not active"))
                assertFalse(text, text.contains("Porter / Shizuku"))
            }
        }
    }
    @Test fun bulkRulesExplainMasterSwitchAndInactiveSavedMode() {
        val s = RecentsSettings(bulkWithAccess = BulkWithAccess.DISMISS_ALL,
            bulkWithoutAccess = BulkWithoutAccess.DISMISS_ALL, enableDismiss = false)
        assertTrue(settingEffectInfo(SetRow.BULK_ACTION, s, true, true)!!.first.contains("Dismissal"))
        assertTrue(settingEffectInfo(SetRow.BULK_WITHOUT_ACCESS, s, false, true)!!.first.contains("Dismissal is Off"))
        assertEquals(Color(0xFFE0B56A), settingEffectInfo(SetRow.BULK_WITHOUT_ACCESS, s, true, true)!!.second)
        assertTrue(settingEffectInfo(SetRow.KILL_ALL, s, true, true)!!.first.contains("Dismissal"))
        assertEquals(BulkWithAccess.DISMISS_ALL, s.bulkWithAccess)
        assertEquals(BulkWithoutAccess.DISMISS_ALL, s.bulkWithoutAccess)
    }
    @Test fun taskPreferenceFallsBackWithoutBlockingSavedActions() {
        val s = RecentsSettings(listMode = AppListMode.ANDROID_TASKS, enableDismiss = true,
            downWithoutAccess = DownWithoutAccess.DISMISS, bulkWithoutAccess = BulkWithoutAccess.DISMISS_ALL)
        assertTrue(settingEffectInfo(SetRow.LIST_MODE, s, false, true, "Porter")!!.first.contains("usage-history fallback"))
        assertTrue(settingEffectInfo(SetRow.LIST_MODE, s, true, true, "Porter")!!.first.contains("Android tasks"))
        for (row in listOf(SetRow.DOWN_WITHOUT_ACCESS, SetRow.BULK_WITHOUT_ACCESS)) {
            val info = settingEffectInfo(row, s, false, true)!!
            assertFalse(info.first.contains("no fallback"))
            assertTrue(info.first.contains("dismiss", ignoreCase = true))
        }
        val recent = s.copy(listMode = AppListMode.RECENTLY_USED)
        assertTrue(settingEffectInfo(SetRow.LIST_MODE, recent, true, false)!!.first.contains("showing Android tasks"))
        assertEquals(AppListMode.ANDROID_TASKS, s.listMode)
    }
    @Test fun activeDownDismissExplainsMissingUsageAccessWithoutBlockingTaskClosing() {
        val s = RecentsSettings(enableDismiss = true, dismissScope = DismissScope.BOTH_VIEWS, downWithAccess = DownWithAccess.DISMISS)
        assertTrue(settingEffectInfo(SetRow.DOWN_ACTION, s, true, false)!!.first.contains("allow Usage Access"))
        val mixed = s.copy(downWithAccess = DownWithAccess.CLOSE_OR_DISMISS)
        val info = settingEffectInfo(SetRow.DOWN_ACTION, mixed, true, false)!!
        assertTrue(info.first.contains("close only; allow Usage Access"))
        assertEquals(Color(0xFFE0B56A), info.second)
        assertEquals(DownAction.CLOSE_TASK, ActionPolicy.downForAccess(mixed.downWithAccess, mixed.downWithoutAccess, true, true, true))
        assertFalse(settingEffectInfo(SetRow.DOWN_ACTION, mixed, true, true)!!.first.contains("allow Usage Access"))
        assertFalse(settingEffectInfo(SetRow.DOWN_ACTION, s.copy(downWithAccess = DownWithAccess.CLOSE_ONLY), true, false)!!.first.contains("allow Usage Access"))
    }
    @Test fun optionalViewSwitchExplainsAvailabilityWithoutChangingPreferredSource() {
        val s = RecentsSettings(viewSwitchMode = ViewSwitchMode.BUTTON, listMode = AppListMode.RECENTLY_USED)
        assertEquals(Tokens.accent, settingEffectInfo(SetRow.VIEW_SWITCH, s, true, true)!!.second)
        assertTrue(settingEffectInfo(SetRow.VIEW_SWITCH, s, false, true)!!.first.contains("switch controls hidden"))
        assertTrue(settingEffectInfo(SetRow.VIEW_SWITCH, s, true, false)!!.first.contains("Usage Access"))
        assertNull(settingEffectInfo(SetRow.VIEW_SWITCH, s.copy(viewButtons = ViewButtons.HIDDEN), true, true))
        assertTrue(SetRow.VIEW_SWITCH in SettingsSubmenu.VIEW.rowsFor(s))
        assertEquals(AppListMode.RECENTLY_USED, s.listMode)
    }
    @Test fun fallbackWarningsDescribeUnavailableActionsRatherThanServiceAbsence() {
        val amber=Color(0xFFE0B56A)
        val s=RecentsSettings(enableDismiss=true, edgeShortcut=EdgeShortcut.TWO_PRESSES, edgeWithoutAccess=EdgeAction.DISMISS_ALL)
        assertEquals(Tokens.textDim, settingEffectInfo(SetRow.EDGE_SHORTCUT,s,false,true)!!.second)
        assertEquals(Tokens.textDim, settingEffectInfo(SetRow.DOWN_WITHOUT_ACCESS,s.copy(downWithoutAccess=DownWithoutAccess.DISMISS),false,true)!!.second)
        assertEquals(amber,settingEffectInfo(SetRow.BULK_WITHOUT_ACCESS,s.copy(enableDismiss=false,bulkWithoutAccess=BulkWithoutAccess.DISMISS_ALL),false,true)!!.second)
        assertEquals(amber,settingEffectInfo(SetRow.EDGE_MENU_SWITCH,s,false,true)!!.second)
        assertEquals(amber,settingEffectInfo(SetRow.EDGE_MENU_CLOSE,s,false,true)!!.second)
    }
}
