package com.websnake.recentstv.data

import org.junit.Assert.*
import org.junit.Test

class ForegroundUsageCacheTest {
    @Test fun opensQueryFreshOverlappingEventsAndKeepNewestForegroundTime() {
        val cache = ForegroundUsageCache(10_000, 100, 1_000)
        assertEquals(mapOf("a" to 9_900L), cache.read(10_000, 0) { start, end ->
            assertEquals(0L, start); assertEquals(10_000L, end); mapOf("a" to 9_900L)
        })
        assertEquals(mapOf("a" to 9_900L, "b" to 10_050L), cache.read(10_100, 100) { start, end ->
            assertEquals(9_900L, start); assertEquals(10_100L, end)
            mapOf("a" to 9_800L, "b" to 10_050L)
        })
    }
    @Test fun fullReconciliationDropsHistoryNoLongerReturnedByAndroid() {
        val cache = ForegroundUsageCache(10_000, 100, 1_000)
        cache.read(10_000, 0) { _, _ -> mapOf("a" to 9_900L) }
        assertTrue(cache.read(11_000, 1_000) { start, _ -> assertEquals(1_000L, start); emptyMap() }.isEmpty())
    }
    @Test fun permissionInvalidationRequiresFullReadAndDiscardsInFlightResults() {
        val cache = ForegroundUsageCache(10_000, 100, 1_000)
        cache.read(10_000, 0) { _, _ -> mapOf("a" to 9_900L) }
        cache.invalidate()
        assertEquals(mapOf("b" to 10_050L), cache.read(10_100, 100) { start, _ ->
            assertEquals(100L, start); mapOf("b" to 10_050L)
        })
        assertTrue(cache.read(10_200, 200) { _, _ -> cache.invalidate(); mapOf("c" to 10_100L) }.isEmpty())
        cache.read(10_300, 300) { start, _ -> assertEquals(300L, start); emptyMap() }
    }
    @Test fun clockChangesAndExpiredEntriesCannotLeakStaleHistory() {
        val cache = ForegroundUsageCache(10_000, 100, 100_000)
        cache.read(10_000, 0) { _, _ -> mapOf("old" to 50L) }
        assertTrue(cache.read(10_100, 100) { _, _ -> emptyMap() }.isEmpty())
        cache.read(9_000, 200) { start, _ -> assertEquals(0L, start); emptyMap() }
        cache.read(30_000, 300) { start, _ -> assertEquals(20_000L, start); emptyMap() }
    }
    @Test fun failedQueriesDoNotLoseTheUnqueriedIntervalAndResultsAreCopies() {
        val cache = ForegroundUsageCache(10_000, 100, 1_000)
        val snapshot = cache.read(10_000, 0) { _, _ -> mapOf("a" to 9_900L) }
        (snapshot as MutableMap).clear()
        assertTrue(runCatching { cache.read(10_100, 100) { _, _ -> error("unavailable") } }.isFailure)
        assertEquals(mapOf("a" to 9_900L), cache.read(10_200, 200) { start, _ ->
            assertEquals(9_900L, start); emptyMap()
        })
    }
}
