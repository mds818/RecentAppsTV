package com.websnake.recentstv.ui.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.tv.material3.Text
import com.websnake.recentstv.R
import com.websnake.recentstv.data.RecentTask
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
    data class Card(val task: RecentTask) : OverlayItem
}

private val ease = CubicBezierEasing(.22f, .61f, .36f, 1f)

@Composable
fun RecentsOverlay(
    items: List<OverlayItem>,
    focus: Int,
    gearFocused: Boolean,
    peekDir: Int,                 // -1 none, 0 up, 1 down (only for focused card)
    dismissing: Pair<Int, Int>?,  // taskId, dir(0 up/1 down) currently flying off
    killing: Boolean,             // kill-all in progress → all cards fly up, staggered
    size: CardSizeOpt,
    shape: CardShapeOpt,
    taskCount: Int,
    onDismissComplete: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val accent = LocalAccent.current
    val visual = remember(items) { items.asReversed() }
    val focusVisual = items.size - 1 - focus

    // Measured layout positions → correct, robust centering (no analytic guesswork).
    val centers = remember { mutableStateMapOf<Int, Float>() }
    var stripW by remember { mutableStateOf(0f) }
    var viewportW by remember { mutableStateOf(0f) }

    val targetTx = when {
        viewportW == 0f -> 0f
        stripW <= viewportW -> (viewportW - stripW) / 2f          // whole strip fits → centre it
        else -> {
            // fallback centre = screen centre → targetTx 0 (clean slide-in once
            // the real card centre is measured), never a nonsensical jump.
            val c = centers[focusVisual] ?: (viewportW / 2f)
            (viewportW / 2f - c).coerceIn(viewportW - stripW, 0f)  // clamp so ends stay in view
        }
    }
    // One draw-phase transform for the whole strip → smooth, no per-frame relayout.
    val txAnim = animateFloatAsState(targetTx, tween(340, easing = ease), label = "tx")

    val focusedTask = if (!gearFocused && dismissing == null)
        (items.getOrNull(focus) as? OverlayItem.Card)?.task else null

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { viewportW = it.size.width.toFloat() },
    ) {
        // ── header ── count on the left, title + gear grouped at the right.
        Box(
            Modifier.fillMaxWidth().padding(start = 60.dp, end = 44.dp, top = 40.dp),
        ) {
            Text(
                text = if (taskCount > 0) "$taskCount ${if (taskCount == 1) "app" else "apps"}" else "",
                style = Type.small, color = Tokens.textDim,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Text("Recent apps", style = Type.title, color = Tokens.text)
                SettingsGear(focused = gearFocused)
            }
        }

        // ── carousel ── a plain Row translated as a single layer. wrapContentWidth
        // (unbounded) lets the Row size to its TRUE content width (otherwise the
        // parent clamps it to the screen and Kill-All gets pushed off the edge).
        Box(Modifier.align(Alignment.Center).fillMaxWidth().height(size.dp * 3.6f)) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .wrapContentWidth(align = Alignment.Start, unbounded = true)
                    .onGloballyPositioned { stripW = it.size.width.toFloat() }
                    .graphicsLayer { translationX = txAnim.value }
                    .padding(horizontal = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                visual.forEachIndexed { i, item ->
                    // Stable key per item → Compose tracks card identity across
                    // removals, so a dismissed card's slot collapses (no leftover
                    // gap) and remembered animation state stays with its own card.
                    key(if (item is OverlayItem.Card) "t${item.task.taskId}" else "killall") {
                        Box(
                            Modifier.onGloballyPositioned {
                                centers[i] = it.positionInParent().x + it.size.width / 2f
                            },
                        ) {
                            val logicalIndex = items.size - 1 - i
                            val isFocused = !gearFocused && logicalIndex == focus
                            when (item) {
                                is OverlayItem.KillAll -> KillAllTile(focused = isFocused, size = size, shape = shape)
                                is OverlayItem.Card -> {
                                    val dir = dismissing?.takeIf { it.first == item.task.taskId }?.second
                                    AppCard(
                                        task = item.task,
                                        focused = isFocused,
                                        peekDir = if (isFocused) peekDir else -1,
                                        flyDir = dir,
                                        killing = killing,
                                        staggerIndex = logicalIndex,
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

        // ── single focused-app label, centered along the bottom of the screen ──
        if (focusedTask != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 46.dp),
            ) {
                Text(focusedTask.label, style = Type.title, color = Tokens.text, maxLines = 1, textAlign = TextAlign.Center)
                if (focusedTask.category.isNotBlank()) {
                    Text(focusedTask.category, style = Type.small, color = Tokens.textDim, maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun SettingsGear(focused: Boolean) {
    val accent = LocalAccent.current
    val scale = animateFloatAsState(if (focused) 1.12f else 1f, tween(160), label = "gear")
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
            painter = painterResource(R.drawable.ic_settings),
            contentDescription = "Settings",
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
    killing: Boolean,
    staggerIndex: Int,
    size: CardSizeOpt,
    shape: CardShapeOpt,
    onFlyComplete: () -> Unit,
) {
    val density = LocalDensity.current
    val accent = LocalAccent.current
    val scale = animateFloatAsState(if (focused) 1.15f else 0.9f, tween(260, easing = ease), label = "scale")
    val baseAlpha = animateFloatAsState(if (focused) 1f else 0.82f, tween(260), label = "alpha")
    val peekPx = with(density) { 23.dp.toPx() }

    val fly = remember { Animatable(0f) }
    val flyAlpha = remember { Animatable(1f) }
    LaunchedEffect(flyDir) {
        if (flyDir != null) {
            val target = with(density) { 360.dp.toPx() } * if (flyDir == 0) -1 else 1
            kotlinx.coroutines.coroutineScope {
                launch { fly.animateTo(target, tween(330)) }
                launch { flyAlpha.animateTo(0f, tween(330)) }
            }
            onFlyComplete()
        }
    }
    LaunchedEffect(killing) {
        if (killing) {
            delay(staggerIndex * 28L)
            val target = with(density) { -360.dp.toPx() }
            kotlinx.coroutines.coroutineScope {
                launch { fly.animateTo(target, tween(330)) }
                launch { flyAlpha.animateTo(0f, tween(330)) }
            }
        }
    }

    val peekOffset = when (peekDir) { 0 -> -peekPx; 1 -> peekPx; else -> 0f }

    // Fixed footprint = exactly one card; glow/chevron overflow via requiredSize.
    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        if (focused) {
            Box(
                Modifier
                    .requiredSize(size.dp * 1.7f)
                    .graphicsLayer {
                        scaleX = scale.value; scaleY = scale.value
                        translationY = peekOffset + fly.value
                        alpha = flyAlpha.value
                    }
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(accent.glow, Color.Transparent),
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
                    translationY = peekOffset + fly.value
                    alpha = baseAlpha.value * flyAlpha.value
                }
                .clip(shape.shape(size.dp))
                .background(Tokens.card)
                .then(if (focused) Modifier.border(2.5.dp, accent.base, shape.shape(size.dp)) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            val painter = remember(task.packageName) { task.icon?.let { drawablePainter(it) } }
            if (painter != null) {
                Image(
                    painter = painter, contentDescription = task.label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(size.dp * 0.66f).clip(shape.shape(size.dp)),
                )
            } else {
                Text(task.label.take(1).uppercase(), style = Type.hero, color = Tokens.text)
            }
        }

        // peek hint — explicit "press again to remove" cue, in the peek direction
        if (focused && peekDir >= 0 && flyDir == null) {
            val up = peekDir == 0
            Box(
                Modifier
                    .requiredWidth(size.dp * 4.5f)
                    .graphicsLayer {
                        translationY = with(density) {
                            (if (up) -(size.dp * 0.5f * 1.15f + 34.dp) else (size.dp * 0.5f * 1.15f + 34.dp)).toPx()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(accent.tint)
                        .border(1.dp, accent.base.copy(alpha = 0.5f), RoundedCornerShape(percent = 50))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(if (up) "▲" else "▼", style = Type.small, color = accent.base)
                    Text("Press again to remove", style = Type.small, color = accent.base, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun KillAllTile(
    focused: Boolean,
    size: CardSizeOpt,
    shape: CardShapeOpt,
) {
    val accent = LocalAccent.current
    val scale = animateFloatAsState(if (focused) 1.10f else 1f, tween(260, easing = ease), label = "kscale")
    val w = size.dp * 1.34f
    val h = size.dp * 1.18f
    val glyphColor = if (focused) accent.onDark else accent.base

    Box(Modifier.size(w, h), contentAlignment = Alignment.Center) {
        if (focused) {
            Box(
                Modifier.requiredSize(w * 1.5f, h * 1.5f)
                    .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(accent.glow, Color.Transparent),
                                radius = this.size.minDimension / 2f,
                            )
                        )
                    }
            )
        }
        // Unfocused: NO background, NO border — just the green glyph + label.
        // Focused: fills solid accent with the dark glyph.
        Box(
            Modifier
                .size(w, h)
                .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                .clip(shape.shape(size.dp))
                .then(if (focused) Modifier.background(accent.base) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_trash),
                    contentDescription = "Kill all",
                    colorFilter = ColorFilter.tint(glyphColor),
                    modifier = Modifier.size(size.dp * 0.42f),
                )
                Text("Kill all", style = Type.label, color = glyphColor)
            }
        }
    }
}

/** Rasterise any Drawable into a Compose painter, downscaled to ~display size so
 *  we don't upload a 432px adaptive-icon texture to draw a ~120px tile. */
private fun drawablePainter(d: android.graphics.drawable.Drawable): Painter {
    val cap = 160
    val iw = d.intrinsicWidth.takeIf { it > 0 } ?: cap
    val ih = d.intrinsicHeight.takeIf { it > 0 } ?: cap
    val s = minOf(1f, cap.toFloat() / maxOf(iw, ih))
    val w = (iw * s).toInt().coerceAtLeast(1)
    val h = (ih * s).toInt().coerceAtLeast(1)
    return BitmapPainter(d.toBitmap(w, h).asImageBitmap())
}
