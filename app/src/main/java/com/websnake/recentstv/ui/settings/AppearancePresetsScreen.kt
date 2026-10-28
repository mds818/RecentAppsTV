package com.websnake.recentstv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.websnake.recentstv.data.AppearancePreset
import com.websnake.recentstv.ui.theme.*

@Composable
fun AppearancePresetsScreen(focus: Int, canRestore: Boolean) {
    val listState = rememberLazyListState()
    LaunchedEffect(focus) { listState.animateScrollToItem(focus) }
    Column(Modifier.fillMaxSize().background(Tokens.bg).padding(horizontal = 60.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Appearance presets", style = Type.title, color = Tokens.text)
        Text("Try a look, or restore your saved appearance.", style = Type.body, color = Tokens.textDim)
        Text("↑↓ Choose · OK Review · BACK Return", style = Type.small, color = Tokens.textDim,
            modifier = Modifier.padding(bottom = 10.dp))
        LazyColumn(state = listState, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(AppearancePreset.entries.size + 1) { index ->
            val preset = AppearancePreset.entries.getOrNull(index)
            val available = preset != null || canRestore
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                .background(if (focus == index) Tokens.surface2 else Tokens.surface)
                .border(1.dp, if (focus == index) LocalAccent.current.base else Color.Transparent, RoundedCornerShape(11.dp))
                .padding(horizontal = 22.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(preset?.title ?: "Restore previous appearance", style = Type.row, color = if (available) Tokens.text else Tokens.textDim)
                Text(preset?.description ?: if (canRestore) "Return to the look saved before your first preset" else "Available after applying a preset",
                    style = Type.small, color = Tokens.textDim)
            }
        }
        }
        Text("Your restore point stays saved while trying presets, even after restarting the app.",
            style = Type.small, color = Tokens.textDim)
    }
}

@Composable
fun ApplyPresetDialog(preset: AppearancePreset?, confirmSelected: Boolean) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
        Column(Modifier.width(540.dp).clip(RoundedCornerShape(20.dp)).background(Tokens.surface)
            .border(1.dp, Tokens.line, RoundedCornerShape(20.dp)).padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(if (preset == null) "Restore previous appearance?" else "Apply ${preset.title}?", style = Type.title, color = Tokens.text)
            Text(preset?.description ?: "Return to the appearance you had before trying these presets.", style = Type.body, color = Tokens.textDim)
            Text(if (preset == null) "This uses your saved restore point. Applying another preset afterward will save a new one."
                else if (preset == AppearancePreset.CLASSIC) "Medium rounded cards · Green · 60% Charcoal dim. Your previous appearance stays saved."
                else "Large circular cards · Green · 80% dim. Your previous appearance stays saved until you restore it.",
                style = Type.small, color = Tokens.textDim)
            if (preset == AppearancePreset.CLEAN_PLUS) Text("Settings stays available: press UP twice. Names and Kill All are hidden.",
                style = Type.small, color = Tokens.textDim)
            Text("Exceptions, protected apps, confirmation, force-stop and Porter / Shizuku choices are kept.",
                style = Type.small, color = Tokens.textDim)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                listOf("Cancel", if (preset == null) "Restore" else "Apply").forEachIndexed { index, label ->
                    val selected = confirmSelected == (index == 1)
                    Text(label, style = Type.label, color = if (selected) LocalAccent.current.onDark else Tokens.textDim,
                        modifier = Modifier.padding(start = 10.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (selected) LocalAccent.current.base else Tokens.surface2)
                            .padding(horizontal = 20.dp, vertical = 10.dp))
                }
            }
            Text("←→ Choose · OK Confirm · BACK Cancel", style = Type.small, color = Tokens.textDim)
        }
    }
}
