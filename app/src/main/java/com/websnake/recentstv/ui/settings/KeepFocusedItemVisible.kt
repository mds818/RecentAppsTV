package com.websnake.recentstv.ui.settings

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first

/** Zero for a fully visible item; otherwise move just enough to reveal it. */
internal fun focusScrollDelta(top: Int, size: Int, viewportTop: Int, viewportBottom: Int): Float = when {
    size > viewportBottom - viewportTop -> (top - viewportTop).toFloat()
    top < viewportTop -> (top - viewportTop).toFloat()
    top + size > viewportBottom -> (top + size - viewportBottom).toFloat()
    else -> 0f
}

/** About one row, capped for short viewports and large heading/card groups. */
internal fun focusComfortInset(itemSize: Int, viewportSize: Int): Int =
    minOf(itemSize, viewportSize / 4, ((viewportSize - itemSize) / 2).coerceAtLeast(0))

private class FocusScrollHistory {
    var focus: Int? = null
    var contentKey: Any? = null
}

/** React only to focus/content changes, never to each frame of a scroll animation. */
@Composable
internal fun KeepFocusedItemVisible(
    state: LazyListState,
    focus: Int,
    smoothMotion: Boolean,
    contentKey: Any? = null,
    bottomInset: Dp = 8.dp,
) {
    val density = LocalDensity.current
    val topGap = with(density) { 8.dp.roundToPx() }
    val bottomGap = with(density) { bottomInset.roundToPx() }
    val history = remember(state) { FocusScrollHistory() }
    LaunchedEffect(state, focus, smoothMotion, contentKey, topGap, bottomGap) {
        if (focus < 0) return@LaunchedEffect
        // Apply a comfort zone only in the navigation direction. Restoring a menu,
        // changing a value or reordering apps must not shift an already visible row.
        val direction = if (history.focus == null || history.contentKey != contentKey) 0
            else focus.compareTo(history.focus!!)
        history.focus = focus
        history.contentKey = contentKey
        // Wait for restored scroll state and the current item/header sizes to be measured.
        withFrameNanos { }
        var layout = snapshotFlow { state.layoutInfo }.first {
            it.totalItemsCount > focus && it.visibleItemsInfo.isNotEmpty()
        }
        if (layout.visibleItemsInfo.none { it.index == focus }) {
            // A held key can skip uncomposed rows. Bring the target near the relevant edge,
            // then refine using its actual size, including any section heading.
            val below = focus > layout.visibleItemsInfo.last().index
            val estimatedSize = layout.visibleItemsInfo.last().size
            val comfort = focusComfortInset(estimatedSize, layout.viewportEndOffset - layout.viewportStartOffset)
            val offset = if (below) -(layout.viewportEndOffset - layout.viewportStartOffset -
                estimatedSize - maxOf(bottomGap, comfort)).coerceAtLeast(0) else -maxOf(topGap, comfort)
            if (smoothMotion) state.animateScrollToItem(focus, offset) else state.scrollToItem(focus, offset)
        }
        repeat(2) {
            layout = snapshotFlow { state.layoutInfo }.first { info -> info.visibleItemsInfo.any { it.index == focus } }
            val item = layout.visibleItemsInfo.first { it.index == focus }
            val comfort = focusComfortInset(item.size, layout.viewportEndOffset - layout.viewportStartOffset)
            val top = layout.viewportStartOffset + if (state.canScrollBackward)
                maxOf(topGap, if (direction < 0) comfort else 0) else 0
            val bottom = layout.viewportEndOffset - if (state.canScrollForward)
                maxOf(bottomGap, if (direction > 0) comfort else 0) else 0
            val delta = focusScrollDelta(item.offset, item.size, top, bottom)
            if (delta == 0f) return@LaunchedEffect
            if (smoothMotion) state.animateScrollBy(delta, tween(160)) else state.scrollBy(delta)
            withFrameNanos { }
        }
    }
}
