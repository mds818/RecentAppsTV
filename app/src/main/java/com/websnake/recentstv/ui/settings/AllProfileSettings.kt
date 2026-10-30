package com.websnake.recentstv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.websnake.recentstv.data.*
import com.websnake.recentstv.ui.theme.*

private val compatibilityColour = Color(0xFFEBBE5C)

@Composable
fun AllProfileSettings(name: String, rows: List<ProfileSettingRow>, filter: ProfileSettingsFilter, focus: Int, smooth: Boolean) {
    val visible = remember(rows, filter) { ProfileSettingsReview.select(rows, filter) }
    val counts = remember(rows) { ProfileSettingsFilter.entries.associateWith { ProfileSettingsReview.select(rows, it).size } }
    val list = rememberLazyListState()
    val accent = LocalAccent.current
    KeepFocusedItemVisible(list, focus.coerceAtLeast(0), smooth, filter)
    ReviewFrame("All profile settings", "$name · ${visible.size} ${if (visible.size == 1) "setting" else "settings"}") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileSettingsFilter.entries.forEach { option ->
                val selected = filter == option
                Text("${option.title} · ${counts[option]}", style = Type.small, color = if (selected) Tokens.text else Tokens.textDim,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                        .background(if (selected) accent.tint else Color.Transparent)
                        .border(if (selected && focus < 0) 2.dp else 1.dp, if (selected) accent.base else Tokens.line, RoundedCornerShape(9.dp))
                        .padding(horizontal = 12.dp, vertical = 11.dp))
            }
        }
        if (visible.isEmpty()) Text("No settings in this filter", style = Type.body, color = Tokens.textDim, modifier = Modifier.padding(vertical = 18.dp))
        else LazyColumn(state = list, modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(visible.size, key = { visible[it].key }) { i ->
                val row = visible[i]
                Column {
                    if (i == 0 || visible[i - 1].group != row.group)
                        Text(row.group, style = Type.small, color = Tokens.text, modifier = Modifier.padding(vertical = 7.dp))
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(if (focus == i) Tokens.surface2 else Color.Transparent)
                        .border(1.dp, if (focus == i) accent.base else Color.Transparent, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(row.label, style = Type.body, color = Tokens.text, modifier = Modifier.weight(1f))
                            Text(row.status, style = Type.small,
                                color = if (row.partial || row.state == ProfileSettingState.UNSUPPORTED) compatibilityColour
                                    else if (row.state == ProfileSettingState.CHANGED) accent.base else Tokens.textDim)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(row.current, style = Type.small, color = Tokens.textDim, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text("→", style = Type.small, color = Tokens.textDim)
                            Text(row.target, style = Type.small, color = Tokens.text, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        Text("↑↓ Choose · ←→ Filter · OK Details · BACK Return", style = Type.small, color = Tokens.textDim)
    }
}

@Composable
fun ProfileSettingDetails(row: ProfileSettingRow, focus: Int, smooth: Boolean) {
    val lines = remember(row) { row.technicalLines() }
    val list = rememberLazyListState()
    val accent = LocalAccent.current
    KeepFocusedItemVisible(list, focus, smooth, row.key)
    ReviewFrame(row.label, "Details · ${row.status}") {
        LazyColumn(state = list, modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(lines.size) { i ->
                val (label, text) = lines[i]
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(if (focus == i) Tokens.surface2 else Color.Transparent)
                    .border(1.dp, if (focus == i) accent.base else Color.Transparent, RoundedCornerShape(8.dp))
                    .padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(label, style = Type.small, color = Tokens.textDim)
                    Text(text, style = Type.body, color = Tokens.text)
                }
            }
        }
        Text("↑↓ Scroll · OK / BACK Return", style = Type.small, color = Tokens.textDim)
    }
}
