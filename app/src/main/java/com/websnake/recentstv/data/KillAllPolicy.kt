package com.websnake.recentstv.data

/** Visibility and protection are independent; usage entries are never closable. */
object KillAllPolicy {
    fun candidates(tasks: List<RecentTask>, hidden: Set<String>, protected: Set<String>, excludeHidden: Boolean): List<RecentTask> =
        tasks.filterNot { it.isUsageEntry || it.packageName in protected || (excludeHidden && it.packageName in hidden) }
}
