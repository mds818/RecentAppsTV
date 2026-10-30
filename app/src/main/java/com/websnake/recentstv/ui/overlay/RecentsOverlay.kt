package com.websnake.recentstv.ui.overlay

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.core.graphics.drawable.toBitmap
import androidx.tv.material3.Text
import com.websnake.recentstv.R
import com.websnake.recentstv.data.RecentTask
import com.websnake.recentstv.data.FocusPosition
import com.websnake.recentstv.data.AppDirection
import com.websnake.recentstv.data.CardPadding
import com.websnake.recentstv.data.CloseHintStyle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.websnake.recentstv.ui.theme.CardShapeOpt
import com.websnake.recentstv.ui.theme.CardSizeOpt
import com.websnake.recentstv.ui.theme.LocalAccent
import com.websnake.recentstv.ui.theme.Tokens
import com.websnake.recentstv.ui.theme.Type
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** One item in the focus ring: the Kill-All tile or a task card. */
sealed interface OverlayItem {
    data object KillAll : OverlayItem
    data class ViewSwitch(val side: com.websnake.recentstv.data.RowSwitchPosition) : OverlayItem
    data class Card(val task: RecentTask) : OverlayItem
}

private val ease = CubicBezierEasing(.22f, .61f, .36f, 1f)
private val LocalActionSpeed = staticCompositionLocalOf { com.websnake.recentstv.data.AnimationSpeed.NORMAL }
private val LocalMotionEnabled = staticCompositionLocalOf { true }
private val LocalIconFraction = staticCompositionLocalOf { CardPadding.NORMAL.iconFraction }

@Composable
fun RecentsOverlay(
    items: List<OverlayItem>,
    restoredPackages: Set<String>,
    restoreRevision: Int,
    focus: Int,
    gearFocused: Boolean,
    topKillFocused: Boolean,
    viewFocused: Boolean,
    restoreFocused: Boolean,
    showTopRestore: Boolean,
    edgeHelp: String?,
    showViewSwitch: Boolean,
    showingTasks: Boolean,
    showEdgeSwitchHint: Boolean,
    edgePointsRight: Boolean,
    edgeConfirmation: Boolean,
    edgeBothDirections: Boolean,
    rowVisible: Boolean,
    viewNotice: com.websnake.recentstv.data.ViewNoticeEvent?,
    edgeActionLabel: String,
    viewNoticeVisible: Boolean,
    viewRevision: Int,
    settingsHintVisible: Boolean,
    showTopKillAll: Boolean,
    closeHintStyle: CloseHintStyle,
    protectedApps: Set<String>,
    closingPackages: Set<String>,
    bulkDismiss: Boolean,
    dismissLabel: Boolean,
    showTaskIndicator: Boolean,
    protectionPending: Boolean,
    onProtectionHandled: (Int) -> Unit,
    protectionEvent: Int,
    protectionTaskId: Int,
    protectionMessage: String?,
    protectionAnimated: Boolean,
    peekDir: Int,                 // -1 none, 0 up, 1 down (only for focused card)
    dismissing: Pair<Int, Int>?,  // taskId, dir(0 up/1 down) currently flying off
    killing: Boolean,             // kill-all in progress → all cards fly up, staggered
    size: CardSizeOpt,
    shape: CardShapeOpt,
    cardPadding: CardPadding,
    taskCount: Int,
    position: FocusPosition,
    direction: AppDirection,
    showHeader: Boolean,
    showSettingsButton: Boolean,
    showHelp: Boolean,
    showAppNames: Boolean,
    appTextPosition: com.websnake.recentstv.data.AppTextPosition,
    showAppDetails: Boolean,
    smoothMotion: Boolean,
    animationSpeed: com.websnake.recentstv.data.AnimationSpeed,
    closeAnimationMs: Int,
    onDismissComplete: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val accent = LocalAccent.current
    val bulkLabel = if (bulkDismiss) "Dismiss all" else "Close all"
    val bulkIcon = if (bulkDismiss) R.drawable.ic_dismiss else R.drawable.ic_trash
    val topFocused = gearFocused || topKillFocused || viewFocused || restoreFocused
    val viewLabel = if (showingTasks) "Recent apps" else "Tasks"
    val focusedTask = if (!topFocused && dismissing == null)
        (items.getOrNull(focus) as? OverlayItem.Card)?.task else null
    val rowReveal = animateFloatAsState(if (rowVisible) 1f else 0f,
        tween(if (smoothMotion) animationSpeed.duration(if (rowVisible) 260 else 170) else 0, easing = FastOutSlowInEasing), label = "view-row")
    val switchSelected = !topFocused && items.getOrNull(focus) is OverlayItem.ViewSwitch
    val killSelected = !topFocused && items.getOrNull(focus) == OverlayItem.KillAll

    CompositionLocalProvider(LocalMotionEnabled provides smoothMotion, LocalActionSpeed provides animationSpeed, LocalIconFraction provides cardPadding.iconFraction) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val centered = position == FocusPosition.CENTER
        val olderRight = direction == AppDirection.OLDER_RIGHT
        val visual = if (olderRight) items else items.asReversed()
        val selected = (if (olderRight) focus else items.size - 1 - focus)
            .coerceIn(0, (items.size - 1).coerceAtLeast(0))
        // Every tile now has the same layout footprint; focus scaling does not shift the row.
        val gapPx = with(density) { 20.dp.roundToPx() }
        val paddingPx = with(density) { 30.dp.roundToPx() }
        val widths = visual.map { item ->
            with(density) {
                size.dp.roundToPx()
            }
        }
        val centerPx = paddingPx + widths.take(selected).sum() + gapPx * selected +
            (widths.getOrNull(selected) ?: 0) / 2f
        val viewportPx = constraints.maxWidth.toFloat()
        val stripPx = (paddingPx * 2 + widths.sum() + gapPx * (visual.size - 1).coerceAtLeast(0)).toFloat()
        val targetTx = if (centered) viewportPx / 2f - centerPx
            else if (stripPx <= viewportPx) (viewportPx - stripPx) / 2f
            else (viewportPx / 2f - centerPx).coerceIn(viewportPx - stripPx, 0f)
        val txAnim = key(viewRevision) { animateFloatAsState(targetTx, tween(if (smoothMotion) 280 else 0, easing = ease), label = "tx") }
        val peekAmount = animateFloatAsState(if (peekDir >= 0 && focusedTask != null) 1f else 0f,
            tween(if (smoothMotion) 200 else 0, easing = ease), label = "peek-space")
        // ── header ── count on the left, title + gear grouped at the right.
        Box(
            Modifier.fillMaxWidth().padding(start = 60.dp, end = 44.dp, top = 40.dp).height(40.dp),
        ) {
            if (showHeader) {
            Text(
                text = if (taskCount > 0) "$taskCount ${if (taskCount == 1) "app" else "apps"}" +
                    (if (centered && items.any { it is OverlayItem.Card && it.task.isUsageEntry }) " · Usage history" else "") else "",
                style = Type.small, color = Tokens.textDim,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                if (showHeader) {
                    Text(if (showingTasks) "Tasks" else "Recent apps", style = Type.title, color = Tokens.text)
                }
                if (showTopKillAll) {
                    TopAction(topKillFocused, bulkIcon, bulkLabel)
                }
                if (showTopRestore) { TopAction(restoreFocused, R.drawable.ic_restore, "Restore apps") }
                if (showViewSwitch) { TopAction(viewFocused, R.drawable.ic_switch_view, viewLabel) }
                if (showSettingsButton) {
                    SettingsGear(focused = gearFocused)
                }
            }
        }
        if ((showTopRestore && restoreFocused) || (showTopKillAll && topKillFocused) || (showSettingsButton && gearFocused) || (showViewSwitch && viewFocused)) {
            val controlsToRight = if (topKillFocused) (if (showTopRestore) 1 else 0) + (if (showSettingsButton) 1 else 0) + (if (showViewSwitch) 1 else 0)
                else if (restoreFocused) (if (showSettingsButton) 1 else 0) + (if (showViewSwitch) 1 else 0)
                else if (viewFocused && showSettingsButton) 1 else 0
            Text(if (gearFocused) "Settings" else if (restoreFocused) "Restore apps" else if (viewFocused) viewLabel else bulkLabel, style = Type.small, color = Tokens.text,
                textAlign = TextAlign.Center, maxLines = 1,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 88.dp, end = (20 + controlsToRight * 60).dp).width(88.dp))
        }
        AnimatedVisibility(settingsHintVisible,
            enter = fadeIn(tween(if (smoothMotion) 140 else 0)),
            exit = fadeOut(tween(if (smoothMotion) 140 else 0)),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 124.dp, end = 44.dp)) {
            Text("↑ Press UP again for settings", style = Type.small, color = accent.base,
                modifier = Modifier.background(Tokens.surface, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp))
        }

        // Restored cards rise at their final screen X; compensate for the parent scroll
        // while existing cards make room. Otherwise left-side insertions drift sideways.
        if (protectionMessage != null) {
            Text("ⓘ $protectionMessage", style = Type.small, color = accent.base,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 108.dp)
                    .background(Tokens.surface, RoundedCornerShape(20.dp)).padding(horizontal = 18.dp, vertical = 10.dp))
        }
        // Stable keyed slots animate their X positions after a card is removed.
        // Focus scrolling remains one translated layer; no extra input delay is added.
        Box(Modifier.align(Alignment.Center).fillMaxWidth().height(size.dp * 3.6f)) {
            Box(
                Modifier.align(Alignment.CenterStart)
                    .wrapContentWidth(align = Alignment.Start, unbounded = true)
                    .requiredWidth(with(density) { stripPx.toDp() })
                    .height(size.dp * 1.8f)
                    .graphicsLayer { translationX = txAnim.value },
            ) {
                visual.forEachIndexed { i, item ->
                    key(viewRevision, if (item is OverlayItem.Card) "t${item.task.taskId}" else if (item == OverlayItem.KillAll) "killall" else "view-switch-${(item as OverlayItem.ViewSwitch).side}") {
                        val nudge = remember { Animatable(0f) }
                        LaunchedEffect(protectionEvent) {
                            if (protectionPending && item is OverlayItem.Card && item.task.taskId == protectionTaskId) {
                                // Consume before animating: a recreated row must not replay this event.
                                onProtectionHandled(protectionEvent)
                                if (smoothMotion && protectionAnimated) {
                                    for (x in listOf(-5f, 5f, -3f, 3f, 0f)) nudge.animateTo(x, tween(45))
                                } else nudge.snapTo(0f)
                            } else nudge.snapTo(0f)
                        }
                        val arriving = item is OverlayItem.Card && item.task.packageName in restoredPackages
                        val arrival = remember(restoreRevision) { Animatable(if (arriving && smoothMotion) 0f else 1f) }
                        LaunchedEffect(restoreRevision) {
                            if (arriving && smoothMotion) arrival.animateTo(1f, tween(animationSpeed.duration(300), easing = ease))
                        }
                        val slotX = animateFloatAsState(
                            (paddingPx + i * (with(density) { size.dp.roundToPx() } + gapPx)).toFloat(),
                            tween(if (smoothMotion) animationSpeed.duration(200) else 0, easing = ease), label = "slot")
                        Box(Modifier.align(Alignment.CenterStart).offset { IntOffset((slotX.value + if (arriving) targetTx - txAnim.value else 0f).roundToInt(), 0) }.graphicsLayer { if (item is OverlayItem.Card) {
                            compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
                            clip = false
                            translationX = with(density) { nudge.value.dp.toPx() }
                            alpha = rowReveal.value * arrival.value
                            scaleX = (0.94f + 0.06f * rowReveal.value) * (0.86f + 0.14f * arrival.value); scaleY = scaleX
                            translationY = with(density) { 14.dp.toPx() } * (1f - rowReveal.value) + with(density) { 24.dp.toPx() } * (1f - arrival.value)
                        } }) {
                            val logicalIndex = if (olderRight) i else items.size - 1 - i
                            val isFocused = !topFocused && logicalIndex == focus
                            when (item) {
                                is OverlayItem.ViewSwitch -> KillAllTile(focused = isFocused, activated = false, size = size, shape = shape, label = viewLabel, icon = R.drawable.ic_switch_view)
                                is OverlayItem.KillAll -> KillAllTile(focused = isFocused, activated = killing, size = size, shape = shape, label = bulkLabel, icon = bulkIcon)
                                is OverlayItem.Card -> {
                                    val dir = dismissing?.takeIf { it.first == item.task.taskId }?.second
                                    AppCard(
                                        task = item.task, focused = isFocused,
                                        peekDir = if (isFocused) peekDir else -1,
                                        closeAnimationMs = closeAnimationMs, flyDir = dir, killing = killing && item.task.packageName in closingPackages,
                                        staggerIndex = items.take(logicalIndex).count { it is OverlayItem.Card && it.task.packageName in closingPackages },
                                        size = size, shape = shape,
                                        onFlyComplete = { onDismissComplete(item.task.taskId) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        run {
            val hintWidth = if (edgeConfirmation) 220.dp else 136.dp
            val hintPx = with(density) { hintWidth.toPx() }
            val insetPx = with(density) { 16.dp.toPx() }
            val cardCenter = centerPx + targetTx
            val gap = with(density) { (size.dp * 0.72f + 12.dp).toPx() }
            val besideLeft = if (edgePointsRight) cardCenter + gap else cardCenter - gap - hintPx
            val fitsBeside = besideLeft >= insetPx && besideLeft + hintPx <= viewportPx - insetPx
            val hintLeft = (if (fitsBeside) besideLeft else cardCenter - hintPx / 2f)
                .coerceIn(insetPx, (viewportPx - hintPx - insetPx).coerceAtLeast(insetPx))
            val hintTop = if (fitsBeside) maxHeight / 2 - 18.dp else maxHeight / 2 - size.dp * 0.9f - 40.dp
            val currentHint = Triple(IntOffset(hintLeft.roundToInt(), with(density) { hintTop.roundToPx() }), hintWidth,
                (if (edgeBothDirections && !edgeConfirmation) "← / →" else if (edgePointsRight) "→" else "←") + (if (edgeConfirmation) " Press again: $edgeActionLabel" else " $edgeActionLabel"))
            var lastVisibleHint by remember { mutableStateOf(currentHint) }
            SideEffect { if (showEdgeSwitchHint) lastVisibleHint = currentHint }
            val displayedHint = if (showEdgeSwitchHint) currentHint else lastVisibleHint
            AnimatedVisibility(showEdgeSwitchHint,
                enter = fadeIn(tween(if (smoothMotion) 160 else 0)), exit = fadeOut(tween(if (smoothMotion) 160 else 0)),
                modifier = Modifier.align(Alignment.TopStart).offset { displayedHint.first }) {
                Text(displayedHint.third,
                    style = Type.small.copy(shadow = androidx.compose.ui.graphics.Shadow(accent.base.copy(alpha = if (edgeConfirmation) 0.4f else 0.22f), androidx.compose.ui.geometry.Offset.Zero, 8f)),
                    color = accent.base, textAlign = TextAlign.Center,
                    modifier = Modifier.width(displayedHint.second).padding(horizontal = 8.dp, vertical = 7.dp))
            }
        }
        // Names and details move together; footer guidance reserves its own space.
        if (focusedTask != null || killSelected || switchSelected) {
            val bottomText = appTextPosition == com.websnake.recentstv.data.AppTextPosition.BOTTOM
            val labelWidth = minOf(320.dp, maxWidth - 32.dp)
            val halfLabelPx = with(density) { labelWidth.toPx() / 2f }
            val labelCenterPx = if (position == FocusPosition.SIDE) viewportPx / 2f
                else (centerPx + txAnim.value).coerceIn(halfLabelPx, viewportPx - halfLabelPx)
            val textTop = minOf(maxHeight / 2 + size.dp * 0.78f + 56.dp * peekAmount.value,
                maxHeight - if (showHelp) 98.dp else 62.dp).coerceAtLeast(16.dp)
            val textModifier = if (bottomText) {
                Modifier.align(Alignment.BottomCenter).padding(bottom = if (showHelp) 82.dp else 46.dp).fillMaxWidth(0.8f)
            } else {
                Modifier.align(Alignment.TopCenter)
                    .offset { IntOffset((labelCenterPx - viewportPx / 2f).roundToInt(), with(density) { textTop.roundToPx() }) }
                    .width(labelWidth)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = textModifier,
            ) {
                if (focusedTask == null || showAppNames) Text(focusedTask?.label ?: if (switchSelected) viewLabel else bulkLabel, style = Type.title, color = Tokens.text, maxLines = 1, textAlign = TextAlign.Center)
                if (showAppDetails && focusedTask != null && focusedTask.category.isNotBlank()) {
                    Text(focusedTask.category,
                        style = Type.small, color = Tokens.textDim, maxLines = 1, textAlign = TextAlign.Center)
                }
                if (showTaskIndicator && focusedTask?.isUsageEntry == true && focusedTask.matchedTaskId != null) {
                    Text("Task available", style = Type.small, color = Tokens.textDim, maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Tokens.surface2).padding(horizontal = 10.dp, vertical = 3.dp))
                }
            }
        }
        if (peekAmount.value > 0.001f && focusedTask != null) {
            val bubbleHalfPx = with(density) { 88.dp.toPx() }
            val marginPx = with(density) { 16.dp.toPx() }
            val bubbleCenterPx = (centerPx + txAnim.value)
                .coerceIn(bubbleHalfPx + marginPx, viewportPx - bubbleHalfPx - marginPx)
            val bubbleTop = maxHeight / 2 + size.dp * 0.575f + 23.dp * peekAmount.value + 9.dp
            Text(if (dismissLabel) "↓ Press again to dismiss" else "↓ Press again to close", style = Type.small, color = accent.base, textAlign = TextAlign.Center, maxLines = 1,
                modifier = Modifier.align(Alignment.TopCenter)
                    .offset { IntOffset((bubbleCenterPx - viewportPx / 2f).roundToInt(), with(density) { bubbleTop.roundToPx() }) }
                    .width(176.dp)
                    .graphicsLayer {
                        alpha = peekAmount.value
                        scaleX = 0.94f + 0.06f * peekAmount.value
                        scaleY = scaleX
                    }
                    .then(when (closeHintStyle) {
                        CloseHintStyle.MINIMAL -> Modifier
                        CloseHintStyle.ELEVATED -> Modifier.clip(RoundedCornerShape(24.dp))
                            .background(Tokens.surface2.copy(alpha = 0.96f))
                    })
                    .padding(horizontal = 10.dp, vertical = 7.dp))
        }
        if (showHelp) {
            val baseHint = if (items.none { it is OverlayItem.Card }) "↑ Controls · BACK Exit"
                else if (peekDir >= 0) if (dismissLabel) "↓ Dismiss · ↑ Cancel · ←→ Browse" else "↓ Close · ↑ Cancel · ←→ Browse"
                else "←→ Browse · OK Open · Hold OK More · ↑ Settings"
            Text(baseHint + if (edgeHelp != null && peekDir < 0) " · $edgeHelp" else "", style = Type.small, color = Tokens.textDim,
                textAlign = TextAlign.Center, maxLines = 1,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp))
        }
    }
    viewNotice?.let { event -> androidx.compose.runtime.key(event.id) { ViewNoticePopup(event, viewNoticeVisible, smoothMotion) } }
    }
}

@Composable
private fun SettingsGear(focused: Boolean) {
    TopAction(focused, R.drawable.ic_settings, "Settings")
}

@Composable
private fun TopAction(focused: Boolean, icon: Int, label: String) {
    val motion = LocalMotionEnabled.current
    val accent = LocalAccent.current
    val scale = animateFloatAsState(if (focused) 1.12f else 1f, tween(if (motion) 160 else 0), label = "gear")
    Box(
        Modifier
            .size(40.dp)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clip(RoundedCornerShape(12.dp))
            .background(if (focused) accent.base else Tokens.surface2)
            .border(1.5.dp, if (focused) accent.base else Tokens.line, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = label,
            colorFilter = ColorFilter.tint(if (focused) accent.onDark else Tokens.textDim),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun AppCard(
    task: RecentTask,
    focused: Boolean,
    peekDir: Int,
    flyDir: Int?,
    closeAnimationMs: Int,
    killing: Boolean,
    staggerIndex: Int,
    size: CardSizeOpt,
    shape: CardShapeOpt,
    onFlyComplete: () -> Unit,
) {
    val density = LocalDensity.current
    val accent = LocalAccent.current
    val motion = LocalMotionEnabled.current
    val scale = animateFloatAsState(if (focused) 1.15f else 0.9f, tween(if (motion) 220 else 0, easing = ease), label = "scale")
    val baseAlpha = animateFloatAsState(if (focused) 1f else 0.82f, tween(if (motion) 220 else 0), label = "alpha")
    val peekPx = with(density) { 23.dp.toPx() }

    val actionSpeed = LocalActionSpeed.current
    val fly = remember { Animatable(0f) }
    val flyAlpha = remember { Animatable(1f) }
    LaunchedEffect(flyDir) {
        if (flyDir != null) {
            val target = with(density) { 360.dp.toPx() } * if (flyDir == 0) -1 else 1
            kotlinx.coroutines.coroutineScope {
                launch { fly.animateTo(target, tween(if (motion) closeAnimationMs else 0,
                    easing = androidx.compose.animation.core.FastOutLinearInEasing)) }
                launch {
                    if (motion) delay((closeAnimationMs * 0.4f).toLong())
                    flyAlpha.animateTo(0f, tween(if (motion) closeAnimationMs - (closeAnimationMs * 0.4f).toInt() else 0))
                }
            }
            onFlyComplete()
        }
    }
    LaunchedEffect(killing) {
        if (killing) {
            if (motion) delay(staggerIndex * actionSpeed.duration(20).toLong())
            val target = with(density) { -360.dp.toPx() }
            kotlinx.coroutines.coroutineScope {
                launch { fly.animateTo(target, tween(if (motion) actionSpeed.duration(240) else 0)) }
                launch { flyAlpha.animateTo(0f, tween(if (motion) actionSpeed.duration(240) else 0)) }
            }
        }
    }

    val peekOffset = animateFloatAsState(when {
        flyDir != null -> if (flyDir == 0) -peekPx else peekPx
        peekDir == 0 -> -peekPx
        peekDir == 1 -> peekPx
        else -> 0f
    },
        tween(if (motion) 200 else 0, easing = ease), label = "card-peek")

    // Fixed footprint = exactly one card; glow/chevron overflow via requiredSize.
    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        if (focused) {
            Box(
                Modifier
                    .requiredSize(size.dp * 1.7f)
                    .graphicsLayer {
                        scaleX = scale.value; scaleY = scale.value
                        translationY = peekOffset.value + fly.value
                        alpha = flyAlpha.value
                    }
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(accent.glow.copy(alpha = 0.25f), Color.Transparent),
                                radius = this.size.minDimension / 2f,
                            )
                        )
                    }
            )
        }
        Box(
            Modifier
                .size(size.dp)
                .graphicsLayer {
                    scaleX = scale.value; scaleY = scale.value
                    translationY = peekOffset.value + fly.value
                    alpha = baseAlpha.value * flyAlpha.value
                }
                .clip(shape.shape(size.dp))
                .background(if (LocalIconFraction.current >= 1f) Color.Transparent else Tokens.card)
                .then(if (focused) Modifier.border(2.5.dp, accent.base, shape.shape(size.dp)) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            val painter = remember(task.packageName) { task.icon?.let { drawablePainter(it) } }
            if (painter != null) {
                Image(
                    painter = painter, contentDescription = task.label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(size.dp * LocalIconFraction.current).clip(shape.shape(size.dp)),
                )
            } else {
                Text(task.label.take(1).uppercase(), style = Type.hero, color = Tokens.text)
            }
        }

    }
}

@Composable
private fun KillAllTile(
    focused: Boolean,
    activated: Boolean,
    size: CardSizeOpt,
    shape: CardShapeOpt,
    label: String,
    icon: Int,
) {
    val accent = LocalAccent.current
    val motion = LocalMotionEnabled.current
    val scale = animateFloatAsState(if (focused) 1.08f else 0.9f, tween(if (motion) 180 else 0, easing = ease), label = "kscale")
    val w = size.dp
    val h = w
    val glyphColor = if (activated) accent.onDark else accent.base

    Box(Modifier.size(w, h), contentAlignment = Alignment.Center) {
        if (focused) {
            Box(
                Modifier.requiredSize(w * 1.3f, h * 1.3f)
                    .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(accent.glow.copy(alpha = 0.25f), Color.Transparent),
                                radius = this.size.minDimension / 2f,
                            )
                        )
                    }
            )
        }
        // Unfocused: NO background, NO border — just the green glyph + label.
        // Focused: same dark surface and outline as app cards. Fill only on activation.
        Box(
            Modifier
                .size(w, h)
                .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                .clip(shape.shape(size.dp))
                .then(if (focused) Modifier.background(if (activated) accent.base else Tokens.card)
                    .border(2.5.dp, accent.base, shape.shape(size.dp)) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Image(
                    painter = painterResource(icon),
                    contentDescription = label,
                    colorFilter = ColorFilter.tint(glyphColor),
                    modifier = Modifier.size(size.dp * if (focused) 0.38f else 0.34f),
                )
                if (!focused) Text(label, style = Type.label, color = glyphColor)
            }
        }
    }
}

/** Rasterise any Drawable into a Compose painter, downscaled to ~display size so
 *  we don't upload a 432px adaptive-icon texture to draw a ~120px tile. */
internal fun drawablePainter(d: android.graphics.drawable.Drawable): Painter {
    val cap = 160
    val iw = d.intrinsicWidth.takeIf { it > 0 } ?: cap
    val ih = d.intrinsicHeight.takeIf { it > 0 } ?: cap
    val s = minOf(1f, cap.toFloat() / maxOf(iw, ih))
    val w = (iw * s).toInt().coerceAtLeast(1)
    val h = (ih * s).toInt().coerceAtLeast(1)
    return BitmapPainter(d.toBitmap(w, h).asImageBitmap())
}




@Composable
private fun ViewNoticePopup(event: com.websnake.recentstv.data.ViewNoticeEvent, visible: Boolean, smooth: Boolean) {
    val state = remember { androidx.compose.animation.core.MutableTransitionState(false) }
    state.targetState = visible
    val prominent = event.style == com.websnake.recentstv.data.ViewNoticeStyle.PROMINENT
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visibleState = state, enter = fadeIn(tween(if (smooth) 140 else 0)), exit = fadeOut(tween(if (smooth) 140 else 0)),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = if (prominent) 64.dp else 88.dp)) {
            Text(event.text, style = if (prominent) Type.title else Type.small, color = Tokens.text,
                modifier = Modifier.background(Tokens.surface2, RoundedCornerShape(24.dp)).padding(horizontal = 24.dp, vertical = 12.dp))
        }
    }
}

@Composable
fun EdgeActionMenu(labels: List<String>, selected: Int, right: Boolean = true, smooth: Boolean = true) {
    val accent = LocalAccent.current
    val reveal = remember { Animatable(if (smooth) 0f else 1f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(if (smooth) 150 else 0)) }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.22f * reveal.value))) {
        Column(Modifier.align(if (right) Alignment.CenterEnd else Alignment.CenterStart).padding(28.dp).width(216.dp)
            .graphicsLayer { alpha = reveal.value; scaleX = 0.97f + reveal.value * 0.03f; scaleY = scaleX }
            .background(Tokens.surface2, RoundedCornerShape(18.dp))
            .border(1.dp, Tokens.line, RoundedCornerShape(18.dp)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Actions", style = Type.small, color = Tokens.textDim, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            if (labels.isEmpty()) Text("No actions available", style = Type.label, color = Tokens.textDim, modifier = Modifier.padding(12.dp))
            labels.forEachIndexed { index, label ->
                if (label == "Settings" && index > 0) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
                        .height(1.dp).background(Tokens.line))
                }
                val focused = index == selected
                val outline by androidx.compose.animation.animateColorAsState(if (focused) accent.base.copy(alpha = 0.8f) else Color.Transparent,
                    tween(if (smooth) 120 else 0), label = "edgeFocus")
                val icon = when (label) {
                    "Switch view" -> R.drawable.ic_switch_view
                    "Close all" -> R.drawable.ic_trash
                    "Dismiss all" -> R.drawable.ic_dismiss
                    "Restore dismissed apps" -> R.drawable.ic_restore
                    else -> R.drawable.ic_settings
                }
                val fill by androidx.compose.animation.animateColorAsState(if (focused) accent.base.copy(alpha = 0.13f) else Color.Transparent,
                    tween(if (smooth) 120 else 0), label = "edgeFill")
                Row(Modifier.fillMaxWidth().background(fill, RoundedCornerShape(11.dp))
                    .border(1.dp, outline, RoundedCornerShape(11.dp)).padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(painterResource(icon), contentDescription = null, colorFilter = ColorFilter.tint(if (focused) accent.base else Tokens.textDim), modifier = Modifier.size(20.dp))
                    Text(label, style = Type.label, color = Tokens.text)
                }
            }
            Text(if (right) "← Back to apps" else "→ Back to apps", style = Type.small, color = Tokens.textDim,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        }
    }
}

@Composable
fun RestoreDismissedConfirmation(confirm: Boolean, count: Int, apps: List<PreviewApp>, loading: Boolean) {
    val accent = LocalAccent.current
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)), contentAlignment = Alignment.Center) {
        Column(Modifier.width(450.dp).background(Tokens.surface2, RoundedCornerShape(20.dp)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Restore $count ${if (count == 1) "dismissed app" else "dismissed apps"}?", style = Type.title, color = Tokens.text)
            Text("Show dismissed entries again. Hidden apps and protection settings stay unchanged.", style = Type.small, color = Tokens.textDim)
            if (loading) Text("Loading app preview…", style = Type.small, color = Tokens.textDim)
            else AppPreviewStrip("Will restore", apps, count)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf("Cancel", "Restore").forEachIndexed { index, label ->
                    Text(label, style = Type.label, color = if (confirm == (index == 1)) accent.base else Tokens.textDim,
                        modifier = Modifier.background(Tokens.surface, RoundedCornerShape(10.dp)).padding(14.dp))
                }
            }
            Text("←→ Choose · OK Confirm · BACK Cancel", style = Type.small, color = Tokens.textDim)
        }
    }
}
