package com.websnake.recentstv.data

import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs

/** Process-memory history, never a background collector. Each read queries fresh events.
 * Overlap covers recently delivered events; occasional full reads reconcile older changes.
 */
internal class ForegroundUsageCache(
    private val windowMs: Long,
    private val overlapMs: Long = 60_000,
    private val reconcileMs: Long = 60_000,
) {
    private val generation = AtomicLong()
    private var loadedGeneration = -1L
    private var lastWall: Long? = null
    private var lastElapsed = 0L
    private var fullElapsed = 0L
    private var latest = emptyMap<String, Long>()

    // Permission callbacks may call this on the UI thread; never wait for a query lock.
    fun invalidate() { generation.incrementAndGet() }

    @Synchronized
    fun read(wall: Long, elapsed: Long, query: (Long, Long) -> Map<String, Long>): Map<String, Long> {
        val epoch = generation.get()
        val previous = lastWall
        val floor = (wall - windowMs).coerceAtLeast(0)
        val full = previous == null || loadedGeneration != epoch || wall < previous ||
            elapsed < lastElapsed || elapsed - fullElapsed >= reconcileMs ||
            abs((wall - previous) - (elapsed - lastElapsed)) > 5_000
        val begin = if (full) floor else maxOf(floor, previous!! - overlapMs)
        // A failed query must not advance the cursor or alter the previous snapshot.
        val fresh = query(begin, wall)
        if (generation.get() != epoch) {
            lastWall = null
            latest = emptyMap()
            return emptyMap()
        }
        val merged = if (full) HashMap() else HashMap(latest)
        merged.entries.removeAll { it.value < floor || it.value > wall }
        fresh.forEach { (pkg, time) ->
            if (time in floor..wall) merged[pkg] = maxOf(merged[pkg] ?: 0L, time)
        }
        latest = merged
        lastWall = wall
        lastElapsed = elapsed
        loadedGeneration = epoch
        if (full) fullElapsed = elapsed
        return HashMap(merged)
    }
}
