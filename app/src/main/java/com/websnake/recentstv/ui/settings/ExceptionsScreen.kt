package com.websnake.recentstv.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.tv.material3.Text
import com.websnake.recentstv.data.InstalledApp
import com.websnake.recentstv.ui.theme.LocalAccent
import com.websnake.recentstv.ui.theme.Tokens
import com.websnake.recentstv.ui.theme.Type

/** Surface 2b — scrollable installed-app list with per-app exclude toggles. */
@Composable
fun ExceptionsScreen(
    apps: List<InstalledApp>,
    excluded: Set<String>,
    focus: Int,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(focus) {
        if (focus in apps.indices) listState.animateScrollToItem(focus)
    }

    Box(Modifier.fillMaxSize().background(Tokens.bg)) {
        Column(Modifier.fillMaxSize().padding(start = 60.dp, end = 60.dp, top = 48.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("‹", style = Type.hero, color = Tokens.textDim)
                Spacer(Modifier.width(14.dp))
                Text("Exceptions", style = Type.hero, color = Tokens.text)
            }
            Text(
                "Hidden apps are filtered out of the overlay.",
                style = Type.body, color = Tokens.textDim,
                modifier = Modifier.padding(top = 4.dp, bottom = 22.dp),
            )
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(apps, key = { _, a -> a.packageName }) { i, app ->
                    ExcRow(app = app, excluded = app.packageName in excluded, focused = i == focus)
                }
            }
        }
        // bottom fade affordance
        Box(
            Modifier
                .fillMaxWidth()
                .size(width = 0.dp, height = 45.dp)
                .align(Alignment.BottomCenter)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Transparent, Tokens.bg)
                    )
                )
        )
    }
}

@Composable
private fun ExcRow(app: InstalledApp, excluded: Boolean, focused: Boolean) {
    val accent = LocalAccent.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) Tokens.surface2 else Tokens.surface)
            .border(1.dp, if (focused) accent.base else Color.Transparent, RoundedCornerShape(10.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val painter = remember(app.packageName) {
            app.icon?.let {
                val w = it.intrinsicWidth.takeIf { v -> v > 0 } ?: 96
                val h = it.intrinsicHeight.takeIf { v -> v > 0 } ?: 96
                BitmapPainter(it.toBitmap(w, h).asImageBitmap())
            }
        }
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(11.dp)).background(Tokens.card),
            contentAlignment = Alignment.Center,
        ) {
            if (painter != null) {
                Image(painter, app.label, Modifier.size(30.dp), contentScale = ContentScale.Fit)
            } else {
                Text(app.label.take(1).uppercase(), style = Type.row, color = Tokens.text)
            }
        }
        Spacer(Modifier.width(17.dp))
        Column(Modifier.weight(1f)) {
            Text(app.label, style = Type.row, color = Tokens.text)
            Text(app.tag, style = Type.small, color = Tokens.textDim)
        }
        Text(
            if (excluded) "Hidden from recents" else "Shown",
            style = Type.small,
            color = if (excluded) accent.base else Tokens.textDim,
            modifier = Modifier.padding(end = 14.dp),
        )
        Toggle(on = excluded)
    }
}

@Composable
private fun Toggle(on: Boolean) {
    val accent = LocalAccent.current
    Box(
        Modifier
            .size(width = 54.dp, height = 28.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (on) accent.base else Color(0xFF2B2F36))
            .border(1.dp, if (on) accent.base else Tokens.line, RoundedCornerShape(percent = 50)),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(horizontal = 3.dp)
                .size(21.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(if (on) Color.White else Color(0xFF6B7280))
        )
    }
}
