package com.websnake.recentstv.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ProtectedDismissTest {
    @Test fun protectedCloseCanFallBackOnlyToEnabledDismissal() {
        assertEquals(DownAction.DISMISS, ActionPolicy.protectedDown(DownAction.CLOSE_TASK, true, true, true))
        assertEquals(DownAction.OFF, ActionPolicy.protectedDown(DownAction.CLOSE_TASK, true, false, true))
        assertEquals(DownAction.OFF, ActionPolicy.protectedDown(DownAction.CLOSE_TASK, true, true, false))
    }
    @Test fun protectionDoesNotChangeUnprotectedClosingOrDisabledActions() {
        assertEquals(DownAction.CLOSE_TASK, ActionPolicy.protectedDown(DownAction.CLOSE_TASK, false, true, true))
        assertEquals(DownAction.OFF, ActionPolicy.protectedDown(DownAction.DISMISS, false, false, true))
        assertEquals(DownAction.DISMISS, ActionPolicy.protectedDown(DownAction.DISMISS, true, true, true))
        assertEquals(DownAction.OFF, ActionPolicy.protectedDown(DownAction.OFF, true, true, true))
    }
}
