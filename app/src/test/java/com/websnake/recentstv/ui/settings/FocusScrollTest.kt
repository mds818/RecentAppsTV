package com.websnake.recentstv.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class FocusScrollTest {
    @Test fun visibleRowsDoNotMoveTheList() {
        assertEquals(0f, focusScrollDelta(100, 90, 8, 492), 0f)
        assertEquals(0f, focusScrollDelta(8, 484, 8, 492), 0f)
    }
    @Test fun revealOnlyTheHiddenPartAtEitherEdge() {
        assertEquals(-28f, focusScrollDelta(-20, 90, 8, 492), 0f)
        assertEquals(48f, focusScrollDelta(450, 90, 8, 492), 0f)
    }
    @Test fun headerAndCardAreRevealedTogether() {
        assertEquals(28f, focusScrollDelta(400, 120, 8, 492), 0f)
    }
    @Test fun oversizedRowsKeepTheirHeadingAtTheTop() {
        assertEquals(92f, focusScrollDelta(100, 700, 8, 492), 0f)
        assertEquals(0f, focusScrollDelta(8, 700, 8, 492), 0f)
    }
    @Test fun ordinaryViewportLeavesOneRowOfContext() {
        assertEquals(90, focusComfortInset(90, 500))
    }
    @Test fun shortViewportsUseSmallerComfortZones() {
        assertEquals(60, focusComfortInset(90, 240))
        assertEquals(20, focusComfortInset(200, 240))
        assertEquals(0, focusComfortInset(700, 500))
    }
    @Test fun scrollBeginsWhileTheRowIsStillFullyVisibleNearTheBottom() {
        val zone = focusComfortInset(90, 500)
        assertEquals(0f, focusScrollDelta(370, 90, 0, 500), 0f)
        assertEquals(50f, focusScrollDelta(370, 90, 0, 500 - zone), 0f)
    }
    @Test fun scrollBeginsWhileTheRowIsStillFullyVisibleNearTheTop() {
        val zone = focusComfortInset(90, 500)
        assertEquals(0f, focusScrollDelta(30, 90, 0, 500), 0f)
        assertEquals(-60f, focusScrollDelta(30, 90, zone, 500), 0f)
    }
    @Test fun middleRowsStayStillWithEitherComfortZone() {
        val zone = focusComfortInset(90, 500)
        assertEquals(0f, focusScrollDelta(200, 90, zone, 500), 0f)
        assertEquals(0f, focusScrollDelta(200, 90, 0, 500 - zone), 0f)
    }
}
