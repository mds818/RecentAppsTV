package com.websnake.recentstv.data

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable

/** A single recent task surfaced in the overlay.
 *  @Immutable: the icon Drawable reference is treated as constant for a given
 *  task instance, letting Compose skip unchanged cards (the list is rebuilt, not
 *  mutated in place) — essential to avoid recomposing all cards on focus change. */
@Immutable
data class RecentTask(
    val taskId: Int,
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val category: String = "",
)

/** A launchable installed app, for the Exceptions list. */
@Immutable
data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val tag: String,            // "System" / "App"
)
