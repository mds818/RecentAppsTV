package com.websnake.recentstv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.websnake.recentstv.data.RecentsSettings
import com.websnake.recentstv.ui.theme.AccentColor
import com.websnake.recentstv.ui.theme.BgTint
import com.websnake.recentstv.ui.theme.CardShapeOpt
import com.websnake.recentstv.ui.theme.LocalAccent
import com.websnake.recentstv.ui.theme.Tokens
import com.websnake.recentstv.ui.theme.Type

/** Settings rows, in order. */
enum class SetRow { KILL_ALL, FORCE_STOP, SIZE, SHAPE, ACCENT, BG_DIM, BG_TINT, EXCEPTIONS }

/** Surface 2 — leanback-style settings list. [focus] = row ordinal. */
@Composable
fun SettingsScreen(settings: RecentsSettings, focus: Int, exceptionsSummary: String) {
    val listState = rememberLazyListState()
    LaunchedEffect(focus) { listState.animateScrollToItem(focus) }

    Box(Modifier.fillMaxSize().background(Tokens.bg)) {
        Column(Modifier.fillMaxSize().padding(start = 60.dp, end = 60.dp, top = 40.dp)) {
            Text("Recent Apps", style = Type.hero, color = Tokens.text)
            Text("Settings", style = Type.body, color = Tokens.textDim, modifier = Modifier.padding(top = 4.dp, bottom = 22.dp))

            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(11.dp)) {
                item {
                    SettingRow("Show Kill All", "A clear-all tile at the start of the row.", focus == SetRow.KILL_ALL.ordinal) {
                        Toggle(on = settings.showKillAll)
                    }
                }
                item {
                    SettingRow("Force stop on kill", "Also force-stop the app so it can't relaunch.", focus == SetRow.FORCE_STOP.ordinal) {
                        Toggle(on = settings.forceStopOnKill)
                    }
                }
                item {
                    SettingRow("Size of task", "Scales every card in the overlay.", focus == SetRow.SIZE.ordinal) {
                        Segmented(listOf("Small", "Medium", "Large"), settings.size.ordinal)
                    }
                }
                item {
                    SettingRow("Shape of task", "Corner style of every card.", focus == SetRow.SHAPE.ordinal) {
                        ShapePreview(settings.shape); Spacer(Modifier.width(11.dp))
                        Segmented(listOf("Square", "Rounded", "Circle"), settings.shape.ordinal)
                    }
                }
                item {
                    SettingRow("Accent color", "Focus rings and the Kill All tile.", focus == SetRow.ACCENT.ordinal) {
                        SwatchRow(AccentColor.entries.map { it.color }, settings.accent.ordinal)
                    }
                }
                item {
                    SettingRow("Background dim", "How much the running app is hidden.", focus == SetRow.BG_DIM.ordinal) {
                        DimBar(settings.bgDim)
                    }
                }
                item {
                    SettingRow("Background color", "Tint of the overlay backdrop.", focus == SetRow.BG_TINT.ordinal) {
                        SwatchRow(BgTint.entries.map { it.color }, settings.bgTint.ordinal, ring = true)
                    }
                }
                item {
                    SettingRow("Exceptions", exceptionsSummary, focus == SetRow.EXCEPTIONS.ordinal) {
                        Text("‹", style = Type.title, color = Tokens.textDim)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, desc: String, focused: Boolean, control: @Composable () -> Unit) {
    val accent = LocalAccent.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(if (focused) Tokens.surface2 else Tokens.surface)
            .border(1.dp, if (focused) accent.base else Color.Transparent, RoundedCornerShape(11.dp))
            .padding(horizontal = 22.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.row, color = Tokens.text)
            Text(desc, style = Type.small, color = Tokens.textDim, modifier = Modifier.padding(top = 4.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) { control() }
    }
}

@Composable
private fun Toggle(on: Boolean) {
    val accent = LocalAccent.current
    Box(
        Modifier.size(width = 54.dp, height = 28.dp).clip(RoundedCornerShape(percent = 50))
            .background(if (on) accent.base else Color(0xFF2B2F36))
            .border(1.dp, if (on) accent.base else Tokens.line, RoundedCornerShape(percent = 50)),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier.padding(horizontal = 3.dp).size(21.dp).clip(RoundedCornerShape(percent = 50))
                .background(if (on) Color.White else Color(0xFF6B7280))
        )
    }
}

@Composable
private fun Segmented(labels: List<String>, selected: Int) {
    val accent = LocalAccent.current
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF16181C))
            .border(1.dp, Tokens.line, RoundedCornerShape(8.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        labels.forEachIndexed { i, lab ->
            val sel = i == selected
            Box(
                Modifier.clip(RoundedCornerShape(6.dp))
                    .background(if (sel) accent.base else Color.Transparent)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) { Text(lab, style = Type.small, color = if (sel) accent.onDark else Tokens.textDim) }
        }
    }
}

/** Row of colour swatches; the selected one gets a white ring. */
@Composable
private fun SwatchRow(colors: List<Color>, selected: Int, ring: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        colors.forEachIndexed { i, c ->
            val sel = i == selected
            Box(
                Modifier
                    .size(if (sel) 38.dp else 30.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(c)
                    .border(
                        if (sel) 2.5.dp else 1.dp,
                        if (sel) Color.White else Tokens.line,
                        RoundedCornerShape(percent = 50),
                    )
            )
        }
    }
}

/** 0–100% dim level shown as a filled bar with a percentage; ‹/› adjust. */
@Composable
private fun DimBar(pct: Int) {
    val accent = LocalAccent.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier
                .width(260.dp).height(14.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(Color(0xFF2B2F36)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(pct / 100f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(accent.base)
            )
        }
        Text("$pct%", style = Type.small, color = Tokens.text, modifier = Modifier.width(56.dp))
    }
}

@Composable
private fun ShapePreview(shape: CardShapeOpt) {
    Box(
        Modifier.size(38.dp).clip(shape.shape(38.dp)).background(Color(0xFF3A3F48))
            .border(1.dp, Tokens.line, shape.shape(38.dp))
    )
}
