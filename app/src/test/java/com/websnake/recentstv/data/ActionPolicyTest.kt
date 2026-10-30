package com.websnake.recentstv.data

import org.junit.Assert.*
import org.junit.Test

class ActionPolicyTest {
    @Test fun explicitCloseNeverSilentlyBecomesDismiss() {
        for (enabled in listOf(false, true)) {
            assertEquals(DownAction.OFF, ActionPolicy.down(DownAction.CLOSE_TASK, false, enabled))
            assertEquals(DownAction.OFF, ActionPolicy.bulk(BulkAction.CLOSE_TASKS, false, enabled))
            assertEquals(DownAction.CLOSE_TASK, ActionPolicy.bulk(BulkAction.CLOSE_TASKS, true, enabled))
        }
    }
    @Test fun automaticDownClosesOnlyWhenATaskMatches() {
        assertEquals(DownAction.CLOSE_TASK, ActionPolicy.down(DownAction.AUTOMATIC, true, true))
        assertEquals(DownAction.DISMISS, ActionPolicy.down(DownAction.AUTOMATIC, false, true, true))
        assertEquals(DownAction.OFF, ActionPolicy.down(DownAction.AUTOMATIC, false, false))
        assertEquals(DownAction.CLOSE_TASK, ActionPolicy.down(DownAction.AUTOMATIC, true, false))
    }
    @Test fun automaticBulkUsesAccessRatherThanPretendingHistoryIsATask() {
        assertEquals(DownAction.CLOSE_TASK, ActionPolicy.bulk(BulkAction.AUTOMATIC, true, true))
        assertEquals(DownAction.DISMISS, ActionPolicy.bulk(BulkAction.AUTOMATIC, false, true))
        assertEquals(DownAction.OFF, ActionPolicy.bulk(BulkAction.AUTOMATIC, false, false))
    }
    @Test fun explicitDismissRequiresOptInAndOffNeverActs() {
        assertEquals(DownAction.OFF, ActionPolicy.bulk(BulkAction.DISMISS, true, false))
        assertEquals(DownAction.DISMISS, ActionPolicy.bulk(BulkAction.DISMISS, true, true))
        assertEquals(DownAction.OFF, ActionPolicy.down(DownAction.DISMISS, true, false))
        assertEquals(DownAction.DISMISS, ActionPolicy.down(DownAction.DISMISS, true, true))
        for (task in listOf(false,true)) for (enabled in listOf(false,true))
            assertEquals(DownAction.OFF, ActionPolicy.down(DownAction.OFF, task, enabled))
    }
    @Test fun contextPrioritizesActionsAndOmitsUnavailableOperations() {
        assertEquals(listOf(4,1,2,0,3), ActionPolicy.context(true,true,true))
        assertEquals(listOf(1,2,0,3), ActionPolicy.context(false,true,true))
        assertEquals(listOf(4,2,0,3), ActionPolicy.context(true,false,true))
        assertEquals(listOf(4,0,3), ActionPolicy.context(true,false,false))
        assertEquals(listOf(0,3), ActionPolicy.context(false,false,false))
    }
    @Test fun fallbackIsIndependentOfExplicitDismiss() {
        assertEquals(DownAction.OFF, ActionPolicy.down(DownAction.AUTOMATIC, false, true, false))
        assertEquals(DownAction.DISMISS, ActionPolicy.down(DownAction.DISMISS, false, true, false))
        assertEquals(DownAction.OFF, ActionPolicy.down(DownAction.AUTOMATIC, false, false, true))
        assertEquals(DownAction.CLOSE_TASK, ActionPolicy.down(DownAction.AUTOMATIC, true, true, false))
    }
    @Test fun migrationRetainsOldCloseOnlyAndExplicitAutomaticBehavior() {
        assertEquals(DownAction.AUTOMATIC, ActionPolicy.migrateDown("CLOSE_TASK"))
        assertFalse(ActionPolicy.legacyFallback("CLOSE_TASK"))
        assertEquals(DownAction.AUTOMATIC, ActionPolicy.migrateDown("AUTOMATIC"))
        assertTrue(ActionPolicy.legacyFallback("AUTOMATIC"))
        assertEquals(DownAction.OFF, ActionPolicy.migrateDown("OFF"))
        assertEquals(DownAction.DISMISS, ActionPolicy.migrateDown("DISMISS"))
        assertFalse(ActionPolicy.legacyFallback(null))
        assertEquals(DownAction.AUTOMATIC, ActionPolicy.migrateDown(null))
        assertEquals(DownAction.AUTOMATIC, ActionPolicy.migrateDown("invalid"))
    }
    @Test fun bulkConfirmationDistinguishesClosingFromLocalDismissal() {
        assertTrue(ActionPolicy.confirm(BulkConfirmation.ALWAYS, DownAction.CLOSE_TASK))
        assertTrue(ActionPolicy.confirm(BulkConfirmation.ALWAYS, DownAction.DISMISS))
        assertTrue(ActionPolicy.confirm(BulkConfirmation.CLOSE_ONLY, DownAction.CLOSE_TASK))
        assertFalse(ActionPolicy.confirm(BulkConfirmation.CLOSE_ONLY, DownAction.DISMISS))
        assertFalse(ActionPolicy.confirm(BulkConfirmation.OFF, DownAction.CLOSE_TASK))
        assertFalse(ActionPolicy.confirm(BulkConfirmation.OFF, DownAction.DISMISS))
        assertFalse(ActionPolicy.confirm(BulkConfirmation.ALWAYS, DownAction.OFF))
    }

    @Test fun activeServiceNeverDismissesUnmatchedTasksAutomatically() {
        assertEquals(DownAction.OFF, ActionPolicy.down(DownAction.AUTOMATIC, false, true, true, true))
        assertEquals(DownAction.DISMISS, ActionPolicy.down(DownAction.AUTOMATIC, false, true, true, false))
        assertEquals(DownAction.CLOSE_TASK, ActionPolicy.down(DownAction.AUTOMATIC, true, true, true, true))
        assertEquals(DownAction.DISMISS, ActionPolicy.down(DownAction.DISMISS, false, true, true, true))
    }
    @Test fun separateDownChoicesCoverBothAccessStatesIndependently() {
        for (with in DownWithAccess.values()) for (without in DownWithoutAccess.values()) {
            for (enabled in listOf(false, true)) for (task in listOf(false, true)) {
                val off = ActionPolicy.downForAccess(with, without, false, task, enabled)
                assertEquals(if (enabled && without == DownWithoutAccess.DISMISS) DownAction.DISMISS else DownAction.OFF, off)
                val expected = when (with) {
                    DownWithAccess.CLOSE_ONLY -> if (task) DownAction.CLOSE_TASK else DownAction.OFF
                    DownWithAccess.CLOSE_OR_DISMISS -> if (task) DownAction.CLOSE_TASK else if (enabled) DownAction.DISMISS else DownAction.OFF
                    DownWithAccess.DISMISS -> if (enabled) DownAction.DISMISS else DownAction.OFF
                    DownWithAccess.OFF -> DownAction.OFF
                }
                assertEquals(expected, ActionPolicy.downForAccess(with, without, true, task, enabled))
            }
        }
    }
    @Test fun migrationPreservesBuild25BehaviorInBothModes() {
        assertEquals(DownWithAccess.CLOSE_ONLY, ActionPolicy.legacyWithAccess(DownAction.AUTOMATIC))
        assertEquals(DownWithoutAccess.DISMISS, ActionPolicy.legacyWithoutAccess(DownAction.AUTOMATIC, true))
        assertEquals(DownWithoutAccess.OFF, ActionPolicy.legacyWithoutAccess(DownAction.AUTOMATIC, false))
        assertEquals(DownWithAccess.DISMISS, ActionPolicy.legacyWithAccess(DownAction.DISMISS))
        assertEquals(DownWithoutAccess.DISMISS, ActionPolicy.legacyWithoutAccess(DownAction.DISMISS, false))
        assertEquals(DownWithAccess.OFF, ActionPolicy.legacyWithAccess(DownAction.OFF))
        assertEquals(DownWithoutAccess.OFF, ActionPolicy.legacyWithoutAccess(DownAction.OFF, true))
    }
    @Test fun bulkRulesAreIndependentAndDismissRequiresOptIn() {
        for (with in BulkWithAccess.values()) for (without in BulkWithoutAccess.values()) for (enabled in listOf(false, true)) {
            assertEquals(if (without == BulkWithoutAccess.DISMISS_ALL && enabled) DownAction.DISMISS else DownAction.OFF,
                ActionPolicy.bulkForAccess(with, without, false, enabled))
            val active = when (with) {
                BulkWithAccess.CLOSE_ALL, BulkWithAccess.BY_VIEW -> DownAction.CLOSE_TASK
                BulkWithAccess.DISMISS_ALL -> if (enabled) DownAction.DISMISS else DownAction.OFF
                BulkWithAccess.HIDDEN -> DownAction.OFF
            }
            assertEquals(active, ActionPolicy.bulkForAccess(with, without, true, enabled))
        }
    }
    @Test fun oldBulkChoicesAndHiddenLocationsMigrateWithoutEnablingNewActions() {
        for (old in BulkAction.values()) {
            assertEquals(BulkWithAccess.HIDDEN, ActionPolicy.legacyBulkWithAccess(old, true))
            assertEquals(BulkWithoutAccess.HIDDEN, ActionPolicy.legacyBulkWithoutAccess(old, true))
            for (enabled in listOf(false, true)) for (ready in listOf(false, true)) {
                assertEquals(ActionPolicy.bulk(old, ready, enabled), ActionPolicy.bulkForAccess(
                    ActionPolicy.legacyBulkWithAccess(old), ActionPolicy.legacyBulkWithoutAccess(old), ready, enabled))
            }
        }
    }
}
