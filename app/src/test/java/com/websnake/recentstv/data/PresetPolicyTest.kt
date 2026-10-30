package com.websnake.recentstv.data

import org.junit.Assert.*
import org.junit.Test

class PresetPolicyTest {
    @Test fun personalSafetyAndAccessPreferencesStayOutsidePresets() {
        listOf("saved_profiles", "profile_undo", "exceptions", "protected_apps", "dismissed_apps", "use_privileged",
            "auto_check_updates", "show_update_notification", "force_stop_on_kill", "bulk_confirmation",
            "confirm_restore_dismissed", "protect_individual_closes", "allow_dismiss_protected").forEach { assertFalse(it, PresetPolicy.managed(it)) }
        assertTrue(PresetPolicy.managed("future_preference"))
    }
    @Test fun appearanceModeDoesNotChangeActionPolicies() {
        listOf("edge_action", "edge_without_access", "edge_menu_items", "edge_shortcut", "down_with_access", "down_without_access",
            "bulk_with_access", "enable_dismiss", "dismiss_scope", "remember_view", "empty_list_recovery").forEach { assertFalse(it, PresetPolicy.visual(it)) }
        assertTrue(PresetPolicy.visual("show_app_names"))
        assertTrue(PresetPolicy.visual("kill_all_location"))
    }
    @Test fun previewUsesEffectiveValuesAndOmitsUnchangedSettings() {
        val a = RecentsSettings()
        assertTrue(PresetChanges.between(a, a).isEmpty())
        val changes = PresetChanges.between(a, a.copy(edgeAction = EdgeAction.MENU, bgDim = 60))
        assertEquals(2, changes.size)
        assertTrue(changes.any { it.group == "Actions & behaviour" && it.after == "Menu" })
        assertTrue(changes.any { it.before == "80%" && it.after == "60%" })
    }
    @Test fun profileChangesIncludeAccessAndSafetyWithTheirOwnGroups() {
        val a = RecentsSettings()
        val changes = PresetChanges.between(a, a.copy(usePrivileged = false, autoCheckUpdates = false, protectIndividualCloses = false))
        assertEquals(3, changes.size)
        assertEquals(2, changes.count { it.group == "Access & updates" })
        assertEquals(1, changes.count { it.group == "Protection & confirmations" })
    }
    @Test fun effectiveLegacyOptionsAreNotListedTwice() {
        val a = RecentsSettings()
        val changes = PresetChanges.between(a, a.copy(showHeader = false, headerMode = HeaderMode.OFF))
        assertEquals(1, changes.size)
        assertEquals("Header mode", changes.single().label)
    }
}
