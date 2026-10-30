package com.websnake.recentstv.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.websnake.recentstv.ui.theme.*

@Composable
fun KillAllConfirmation(count: Int, protectedCount: Int, forceStop: Boolean, confirmSelected: Boolean, dismissOnly: Boolean = false, apps: List<PreviewApp> = emptyList(), protectedApps: List<PreviewApp> = emptyList()) {
    val accent = LocalAccent.current
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .75f)), contentAlignment = Alignment.Center) {
        Column(Modifier.width(550.dp).clip(RoundedCornerShape(20.dp)).background(Tokens.surface)
            .border(1.dp, Tokens.line, RoundedCornerShape(20.dp)).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (dismissOnly) "Dismiss $count ${if (count == 1) "app" else "apps"}?" else if (count == 0) "No Android tasks to close" else "Close $count ${if (count == 1) "app" else "apps"}?",
                style = Type.title, color = Tokens.text)
            Text(if (dismissOnly) "Hide these apps from this list until they are used again. Nothing is closed or force-stopped."
                else if (count == 0) "No matching tasks can be closed. Check task availability and protection in Settings → Apps."
                else if (forceStop) "This removes their recent tasks and force-stops the apps. Background activity may stop."
                else "This removes their recent tasks. Background services may keep running.",
                style = Type.body, color = Tokens.textDim)
            AppPreviewStrip(if (dismissOnly) "Will dismiss" else "Will close", apps, count)
            if (!dismissOnly) AppPreviewStrip("Protected · will stay", protectedApps, protectedCount)
            Text(if (dismissOnly) "Restore the list in Settings → Apps → Dismissal." else "$protectedCount protected · protection can be changed in Settings → Apps",
                style = Type.small, color = Tokens.textDim)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                listOf("Cancel", if (dismissOnly) "Dismiss apps" else "Close apps").take(if (count == 0) 1 else 2).forEachIndexed { i, label ->
                    val selected = if (i == 0) !confirmSelected else confirmSelected
                    Box(Modifier.padding(start = 12.dp).clip(RoundedCornerShape(8.dp))
                        .background(if (selected) accent.base else Tokens.surface2)
                        .padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text(label, style = Type.row, color = if (selected) accent.onDark else Tokens.textDim)
                    }
                }
            }
            Text("←→ Choose · OK Confirm · BACK Cancel", style = Type.small, color = Tokens.textDim)
        }
    }
}
