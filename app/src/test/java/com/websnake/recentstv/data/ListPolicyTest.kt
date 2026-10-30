package com.websnake.recentstv.data

import org.junit.Assert.*
import org.junit.Test

class ListPolicyTest {
    @Test fun dismissalSurvivesUnchangedAndMissingUsage() {
        val hidden = mapOf("one" to 100L, "two" to 200L)
        assertEquals(hidden, DismissPolicy.remaining(hidden, mapOf("one" to 100L)))
        assertEquals(hidden, DismissPolicy.remaining(hidden, emptyMap()))
    }
    @Test fun onlyNewForegroundUsageRestoresTheMatchingApp() {
        val hidden = mapOf("one" to 100L, "two" to 200L)
        assertEquals(mapOf("two" to 200L), DismissPolicy.remaining(hidden, mapOf("one" to 101L, "two" to 199L)))
    }
    @Test fun anAppWithoutOldEventsReturnsOnFirstNewUse() {
        assertTrue(DismissPolicy.remaining(mapOf("one" to 0L), mapOf("one" to 1L)).isEmpty())
    }
    @Test fun persistedRecordsRoundTripAndIgnoreCorruption() {
        val records = mapOf("app.one" to 0L, "app.two" to 9876543210L)
        assertEquals(records, DismissPolicy.decode(DismissPolicy.encode(records)))
        assertTrue(DismissPolicy.decode(setOf("broken", "-1|app", "1|", "x|app")).isEmpty())
    }
    @Test fun listSourcesAreExplicitAndAutomaticRetainsOriginalBehavior() {
        assertTrue(DismissPolicy.usesHistory(AppListMode.AUTOMATIC, false))
        assertFalse(DismissPolicy.usesHistory(AppListMode.AUTOMATIC, true))
        assertTrue(DismissPolicy.usesHistory(AppListMode.RECENTLY_USED, true))
        assertTrue(DismissPolicy.usesHistory(AppListMode.RECENTLY_USED, false))
        assertTrue(DismissPolicy.usesHistory(AppListMode.ANDROID_TASKS, false))
        assertFalse(DismissPolicy.usesHistory(AppListMode.ANDROID_TASKS, true))
    }
    @Test fun anyAvailableSourceAvoidsThePermissionScreen() {
        for (mode in AppListMode.values()) for (privileged in listOf(false, true)) for (usage in listOf(false, true)) {
            val actual = ListSourcePolicy.resolve(mode, privileged, usage)
            assertEquals(!privileged && !usage, actual == AppListSource.NEEDS_ACCESS)
            if (!privileged && usage) assertEquals(AppListSource.HISTORY, actual)
            if (privileged && !usage) assertEquals(AppListSource.TASKS, actual)
            if (privileged && usage) assertEquals(if (mode == AppListMode.RECENTLY_USED) AppListSource.HISTORY else AppListSource.TASKS, actual)
        }
    }
    @Test fun twoSourceChoicesPreserveLegacyAutomaticAsTaskPreference() {
        assertEquals(listOf(AppListMode.ANDROID_TASKS, AppListMode.RECENTLY_USED), ListSourcePolicy.choices)
        assertEquals(AppListMode.ANDROID_TASKS, ListSourcePolicy.canonical(AppListMode.AUTOMATIC))
        for (mode in ListSourcePolicy.choices) assertEquals(mode, ListSourcePolicy.canonical(mode))
        assertEquals(AppListSource.TASKS, ListSourcePolicy.resolve(AppListMode.ANDROID_TASKS, true, true))
        assertEquals(AppListSource.HISTORY, ListSourcePolicy.resolve(AppListMode.ANDROID_TASKS, false, true))
        assertEquals(AppListSource.TASKS, ListSourcePolicy.resolve(AppListMode.ANDROID_TASKS, true, true))
    }
    @Test fun sourceAdjustmentUsesVisibleOrderInsteadOfLegacyEnumOrdinals() {
        for (initial in AppListMode.values()) {
            var mode = initial
            repeat(5) { mode = ListSourcePolicy.adjust(mode, -1) }
            assertEquals(AppListMode.ANDROID_TASKS, mode)
            mode = ListSourcePolicy.adjust(mode, 1)
            assertEquals(AppListMode.RECENTLY_USED, mode)
            assertEquals(AppListMode.RECENTLY_USED, ListSourcePolicy.adjust(mode, 1))
            assertEquals(AppListMode.ANDROID_TASKS, ListSourcePolicy.adjust(mode, -1))
        }
    }
    @Test fun viewSwitchModesAndPhysicalRightEdgeAreIndependentOfBrowseDirection() {
        assertTrue(ViewSwitchMode.BUTTON.button); assertFalse(ViewSwitchMode.BUTTON.edge)
        assertTrue(ViewSwitchMode.RIGHT_EDGE.edge); assertFalse(ViewSwitchMode.RIGHT_EDGE.button)
        assertTrue(ViewSwitchMode.BOTH.button && ViewSwitchMode.BOTH.edge)
        assertFalse(ViewSwitchMode.HIDDEN.button || ViewSwitchMode.HIDDEN.edge)
        assertTrue(ViewSwitchPolicy.atRightEdge(0, 5, false))
        assertFalse(ViewSwitchPolicy.atRightEdge(4, 5, false))
        assertTrue(ViewSwitchPolicy.atRightEdge(4, 5, true))
        assertFalse(ViewSwitchPolicy.atRightEdge(0, 5, true))
        assertTrue(ViewSwitchPolicy.atRightEdge(0, 0, true))
        assertTrue(RecentsSettings().rememberView)
        assertEquals(ViewSwitchMode.BUTTON, RecentsSettings().viewSwitchMode)
    }
    @Test fun rowEdgesFollowLogicalStartEndAndLegacyControlsMigrateIndependently() {
        assertTrue(ViewSwitchPolicy.edgeIsRight(RowSwitchPosition.START, false))
        assertFalse(ViewSwitchPolicy.edgeIsRight(RowSwitchPosition.START, true))
        assertFalse(ViewSwitchPolicy.edgeIsRight(RowSwitchPosition.END, false))
        assertTrue(ViewSwitchPolicy.edgeIsRight(RowSwitchPosition.END, true))
        assertTrue(ViewSwitchPolicy.atEdge(0, 8, RowSwitchPosition.START))
        assertTrue(ViewSwitchPolicy.atEdge(7, 8, RowSwitchPosition.END))
        assertFalse(ViewSwitchPolicy.atEdge(1, 8, RowSwitchPosition.START))
        for (old in ViewSwitchMode.values()) {
            assertEquals(old.button, ViewSwitchPolicy.legacyButtons(old).top)
            assertFalse(ViewSwitchPolicy.legacyButtons(old).row)
            assertEquals(old.edge, ViewSwitchPolicy.legacyShortcut(old) == EdgeShortcut.ONE_PRESS)
        }
        assertTrue(ViewButtons.BOTH.top && ViewButtons.BOTH.row)
        assertFalse(ViewButtons.HIDDEN.top || ViewButtons.HIDDEN.row)
    }
    @Test fun bothEdgesRetainTheirOwnAnchorAndBulkCanAppearInBothPlaces() {
        assertTrue(ViewSwitchPolicy.atEdge(0, 8, RowSwitchPosition.BOTH))
        assertTrue(ViewSwitchPolicy.atEdge(7, 8, RowSwitchPosition.BOTH))
        assertFalse(ViewSwitchPolicy.atEdge(3, 8, RowSwitchPosition.BOTH))
        assertEquals(RowSwitchPosition.START, ViewSwitchPolicy.sideAt(0, 8, RowSwitchPosition.BOTH))
        assertEquals(RowSwitchPosition.END, ViewSwitchPolicy.sideAt(7, 8, RowSwitchPosition.BOTH))
        assertTrue(KillAllLocation.BOTH.inRow && KillAllLocation.BOTH.inTop)
        assertFalse(KillAllLocation.HIDDEN.inRow || KillAllLocation.HIDDEN.inTop)
    }
    @Test fun noticeStylePreservesLegacyChoiceAndProminentLastsLonger() {
        assertEquals(ViewNoticeStyle.OFF, ViewNoticeStyle.legacy(false))
        assertEquals(ViewNoticeStyle.SUBTLE, ViewNoticeStyle.legacy(true))
        assertTrue(ViewNoticeStyle.PROMINENT.durationMs > ViewNoticeStyle.SUBTLE.durationMs)
    }
}
