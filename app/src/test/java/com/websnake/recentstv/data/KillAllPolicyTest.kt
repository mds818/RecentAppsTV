package com.websnake.recentstv.data

import org.junit.Assert.*
import org.junit.Test

class KillAllPolicyTest {
    private fun task(pkg: String, usage: Boolean = false) = RecentTask(1, pkg, pkg, null, isUsageEntry = usage)
    private val tasks = listOf(task("visible"), task("hidden"), task("protected"), task("hidden.protected"), task("history", true))
    private val hidden = setOf("hidden", "hidden.protected")
    private val protected = setOf("protected", "hidden.protected")

    @Test fun defaultExcludesHiddenAndProtected() {
        assertTrue(RecentsSettings().excludeHiddenFromKillAll)
        assertEquals(listOf("visible"), KillAllPolicy.candidates(tasks, hidden, protected, true).map { it.packageName })
    }
    @Test fun optInIncludesHiddenButNeverProtectedOrHistory() {
        assertEquals(listOf("visible", "hidden"), KillAllPolicy.candidates(tasks, hidden, protected, false).map { it.packageName })
    }
    @Test fun historyOnlyCannotBeClosed() {
        assertTrue(KillAllPolicy.candidates(listOf(task("history", true)), emptySet(), emptySet(), false).isEmpty())
    }
    @Test fun allProtectedProducesNoCandidates() {
        assertTrue(KillAllPolicy.candidates(tasks, hidden, tasks.map { it.packageName }.toSet(), false).isEmpty())
    }
}
