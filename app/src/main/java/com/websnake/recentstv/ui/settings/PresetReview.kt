package com.websnake.recentstv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.websnake.recentstv.data.*
import com.websnake.recentstv.ui.theme.*

@Composable
internal fun ReviewFrame(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .8f)), contentAlignment = Alignment.Center) {
        Column(Modifier.width(720.dp).heightIn(max = 500.dp).clip(RoundedCornerShape(18.dp)).background(Tokens.surface)
            .border(1.dp, Tokens.line, RoundedCornerShape(18.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = Type.title, color = Tokens.text)
            Text(subtitle, style = Type.small, color = Tokens.textDim)
            content()
        }
    }
}

@Composable
private fun Divider() { Box(Modifier.fillMaxWidth().height(1.dp).background(Tokens.line)) }

@Composable
private fun ReviewLink(label: String, detail: String, selected: Boolean, enabled: Boolean = true) {
    val accent = LocalAccent.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
        .background(if (selected) Tokens.surface2 else Color.Transparent)
        .border(1.dp, if (selected) accent.base else Color.Transparent, RoundedCornerShape(8.dp))
        .padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = Type.body.copy(fontWeight = FontWeight.SemiBold), color = if (enabled) Tokens.text else Tokens.textDim)
        Text(detail, style = Type.small, color = Tokens.textDim, modifier = Modifier.weight(1f))
        Text("›", style = Type.row, color = Tokens.textDim)
    }
}

@Composable
private fun ReviewActions(cancelSelected: Boolean, applySelected: Boolean, applyLabel: String, enabled: Boolean) {
    val accent = LocalAccent.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        listOf("Cancel", applyLabel).forEachIndexed { i, label ->
            val selected = if (i == 0) cancelSelected else applySelected
            val primary = i == 1 && enabled
            Text(label, style = Type.body.copy(fontWeight = FontWeight.SemiBold),
                color = if (primary) accent.onDark else if (enabled || i == 0) Tokens.text else Tokens.textDim,
                modifier = Modifier.padding(start = 10.dp).clip(RoundedCornerShape(22.dp))
                    .background(if (primary) accent.base else if (selected) Tokens.surface2 else Color.Transparent)
                    .border(if (selected) 2.dp else 1.dp, if (selected) Tokens.text else if (primary) accent.base else Tokens.line, RoundedCornerShape(22.dp))
                    .padding(horizontal = 20.dp, vertical = 11.dp))
        }
    }
}

@Composable
fun ChangeReview(title: String, subtitle: String, changes: List<PresetChange>, focus: Int, smooth: Boolean, switchModes: Boolean = false) {
    val list = rememberLazyListState()
    KeepFocusedItemVisible(list, focus, smooth, changes)
    val accent = LocalAccent.current
    ReviewFrame(title, subtitle) {
        if (changes.isEmpty()) Text("No changes needed", style = Type.body, color = Tokens.textDim)
        else LazyColumn(state = list, modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(changes.size) { i ->
                val change = changes[i]
                Column {
                    if (i == 0 || changes[i - 1].group != change.group)
                        Text(change.group, style = Type.small, color = Tokens.text, modifier = Modifier.padding(vertical = 6.dp))
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(if (i == focus) Tokens.surface2 else Color.Transparent)
                        .border(1.dp, if (i == focus) accent.base else Color.Transparent, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 9.dp)) {
                        Text(change.label, style = Type.body, color = Tokens.text)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(change.before, style = Type.small, color = Tokens.textDim, modifier = Modifier.weight(1f))
                            Text("→", style = Type.small, color = Tokens.textDim)
                            Text(change.after, style = Type.small, color = Tokens.text, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        Text(if (switchModes) "↑↓ Scroll · ←→ Mode · OK / BACK Return" else "↑↓ Scroll · OK / BACK Return", style = Type.small, color = Tokens.textDim)
    }
}

@Composable
fun PresetReview(preset: AppearancePreset, mode: PresetApplyMode, focus: Int, changes: List<PresetChange>?,
    details: Boolean, detailFocus: Int, smooth: Boolean, error: Boolean) {
    val accent = LocalAccent.current
    if (details) {
        val count = changes?.size ?: 0
        ChangeReview("${preset.title} · Changes", "${if (mode == PresetApplyMode.FULL) "Full preset" else "Appearance only"} · $count ${if (count == 1) "change" else "changes"}", changes.orEmpty(), detailFocus, smooth, switchModes = true)
        return
    }
    ReviewFrame("Apply ${preset.title}", preset.description) {
        Text("Apply mode", style = Type.body.copy(fontWeight = FontWeight.SemiBold), color = Tokens.text)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PresetApplyMode.entries.forEach { choice ->
                val chosen = mode == choice
                Column(Modifier.weight(1f).height(82.dp).clip(RoundedCornerShape(11.dp))
                    .background(if (chosen) accent.tint else Color.Transparent)
                    .border(if (focus == 0 && chosen) 2.dp else 1.dp, if (chosen) accent.base else Tokens.line, RoundedCornerShape(11.dp))
                    .padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (choice == PresetApplyMode.FULL) "Full preset" else "Appearance only", style = Type.body.copy(fontWeight = FontWeight.SemiBold), color = Tokens.text)
                        if (chosen) Text("✓", style = Type.body, color = accent.base)
                    }
                    Text(if (choice == PresetApplyMode.FULL) "Recommended · App defaults plus this preset's look and controls."
                        else "Change the look while keeping your action preferences.", style = Type.small.copy(fontSize = 12.sp), color = Tokens.textDim)
                }
            }
        }
        ReviewLink("View changes", changes?.size?.let { "$it ${if (it == 1) "setting" else "settings"}" }.orEmpty(), focus == 1, changes != null)
        Divider()
        Text(if (error) "Could not prepare the preview. Close and reopen to retry."
            else "ⓘ  App lists, saved profiles, protection, access and update preferences stay unchanged. Your current setup is saved for Undo.",
            style = Type.small, color = Tokens.textDim)
        ReviewActions(focus == 3, focus == 2, if (mode == PresetApplyMode.FULL) "Apply preset" else "Apply appearance", changes != null)
        Text("↑↓ Navigate · ←→ Switch · OK Select · BACK Cancel", style = Type.small, color = Tokens.textDim)
    }
}

@Composable
fun ProfileApplyReview(report: ProfileImport, imported: Boolean, changes: List<PresetChange>, focus: Int, canManage: Boolean = false) {
    val cancelIndex = if (canManage) 3 else 2
    ReviewFrame("Apply ${report.name}", "${report.recognized} compatible settings · ${changes.size} ${if (changes.size == 1) "change" else "changes"}") {
        ReviewLink("View changes", "Current → profile", focus == 0)
        ReviewLink("All profile settings", "${report.missing} missing · ${report.skipped} skipped", focus == 1)
        if (canManage) ReviewLink("Manage profile", "Export · Rename · Delete", focus == 2)
        Divider()
        Text("Missing settings stay unchanged. Unknown or invalid settings are skipped. App lists, dismissed history and Android permissions are not included.", style = Type.small, color = Tokens.textDim)
        Text(if (imported) "ⓘ  Save and apply creates a saved profile and an Undo checkpoint."
            else "ⓘ  Your current settings are saved for Undo last profile change.", style = Type.small, color = Tokens.textDim)
        ReviewActions(focus == cancelIndex, focus == cancelIndex + 1, if (imported) "Save and apply" else "Apply profile", report.recognized > 0)
        Text("↑↓ Navigate · ←→ Choose · OK Select · BACK Cancel", style = Type.small, color = Tokens.textDim)
    }
}

@Composable
fun ProfileManagement(name: String, focus: Int) {
    ReviewFrame(name, "Manage saved profile") {
        ReviewLink("Export", "Save a JSON copy", focus == 0)
        ReviewLink("Rename", "Change this profile's name", focus == 1)
        ReviewLink("Delete", "Remove the saved copy; keep current settings", focus == 2)
        Divider()
        ReviewLink("Back", "Return to the profile review", focus == 3)
        Text("↑↓ Choose · OK Select · BACK Return", style = Type.small, color = Tokens.textDim)
    }
}
