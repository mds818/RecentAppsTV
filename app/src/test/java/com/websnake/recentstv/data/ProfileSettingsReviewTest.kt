package com.websnake.recentstv.data

import org.junit.Assert.*
import org.junit.Test

class ProfileSettingsReviewTest {
    @Test fun readableValuesSeparateChangesMissingAndUnsupported() {
        val before = RecentsSettings()
        val after = before.copy(bgDim = 60)
        val rows = ProfileSettingsReview.build(listOf(ProfileEntry("bg_dim_pct", "Apply", "60", ""),
            ProfileEntry("show_help", "Missing", "Unchanged", "Not included"), ProfileEntry("future_option", "Skip", "something", "Unknown setting")),
            setOf("bg_dim_pct"), before, after, emptyMap(), emptyMap(), mapOf("bg_dim_pct" to PresetChanges.between(before, after)))
        val dim = rows.single { it.key == "bg_dim_pct" }
        assertEquals("Background dim", dim.label); assertEquals("Appearance", dim.group)
        assertEquals("80%", dim.current); assertEquals("60%", dim.target); assertEquals(ProfileSettingState.CHANGED, dim.state)
        assertEquals("Show control hints", rows.single { it.key == "show_help" }.label)
        assertEquals("Missing · kept", rows.single { it.key == "show_help" }.status)
        assertEquals("Unsupported · skipped", rows.single { it.key == "future_option" }.status)
        assertEquals(1, ProfileSettingsReview.select(rows, ProfileSettingsFilter.CHANGES).size)
        assertEquals(2, ProfileSettingsReview.select(rows, ProfileSettingsFilter.COMPATIBILITY).size)
        assertEquals(3, ProfileSettingsReview.select(rows, ProfileSettingsFilter.ALL).size)
    }
    @Test fun overriddenLegacyOptionsAreNotClaimedAsChanges() {
        val before = RecentsSettings()
        val rows = ProfileSettingsReview.build(listOf(ProfileEntry("show_header", "Apply", "false", "")),
            setOf("show_header"), before, before, emptyMap(), mapOf("show_header" to false), emptyMap())
        val row = rows.single()
        assertEquals(ProfileSettingState.UNCHANGED, row.state)
        assertTrue(row.legacy); assertTrue(row.label.endsWith("(legacy)"))
        assertTrue(row.reason.contains("precedence"))
        assertTrue(ProfileSettingsReview.select(rows, ProfileSettingsFilter.CHANGES).isEmpty())
        assertEquals(1, ProfileSettingsReview.select(rows, ProfileSettingsFilter.COMPATIBILITY).size)
    }
    @Test fun partlySupportedValuesCanStillContainChanges() {
        val before = RecentsSettings(); val after = before.copy(edgeAction = EdgeAction.MENU)
        val rows = ProfileSettingsReview.build(listOf(ProfileEntry("edge_action", "Partial", "MENU,FUTURE", "Unknown choices skipped")),
            setOf("edge_action"), before, after, emptyMap(), emptyMap(), mapOf("edge_action" to PresetChanges.between(before, after)))
        assertEquals("Changes · partly supported", rows.single().status)
        assertEquals("Menu", rows.single().target)
        assertEquals(1, ProfileSettingsReview.select(rows, ProfileSettingsFilter.CHANGES).size)
        assertEquals(1, ProfileSettingsReview.select(rows, ProfileSettingsFilter.COMPATIBILITY).size)
    }
    @Test fun explicitDefaultWithNoEffectiveChangeIsUnchanged() {
        val before = RecentsSettings()
        val row = ProfileSettingsReview.build(listOf(ProfileEntry("show_help", "Apply", "App default", "")),
            setOf("show_help"), before, before, emptyMap(), emptyMap(), emptyMap()).single()
        assertEquals(ProfileSettingState.UNCHANGED, row.state)
        assertEquals("On", row.target)
        assertFalse(row.warning)
    }
    @Test fun longUnknownKeysDoNotExpandTheMainScreen() {
        val key = "future_".repeat(100)
        val settings = RecentsSettings()
        val row = ProfileSettingsReview.build(listOf(ProfileEntry(key, "Skip", "true", "Unknown setting")), emptySet(),
            settings, settings, emptyMap(), emptyMap(), emptyMap()).single()
        assertTrue(row.label.length <= 81)
        assertEquals(key, row.key)
        assertEquals(key, row.technicalLines().filter { it.first.startsWith("Setting key") }.joinToString("") { it.second })
    }
    @Test fun detailsKeepFullValuesButBoundIndividualScrollingBlocks() {
        val raw = "x".repeat(1700)
        val row = ProfileSettingRow("future_option", "Compatibility", "Future option", "App default", "Skipped",
            ProfileSettingState.UNSUPPORTED, false, false, raw, "Unknown setting")
        val values = row.technicalLines().filter { it.first.startsWith("Profile value") }
        assertEquals(raw, values.joinToString("") { it.second })
        assertTrue(values.all { it.second.length <= 320 })
        assertTrue(row.technicalLines().any { it.first == "Setting key" && it.second == "future_option" })
    }
}
