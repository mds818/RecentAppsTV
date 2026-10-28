package com.websnake.recentstv.ui.overlay

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
import com.websnake.recentstv.data.RecentTask
import com.websnake.recentstv.ui.theme.LocalAccent
import com.websnake.recentstv.ui.theme.Tokens
import com.websnake.recentstv.ui.theme.Type

/** Context actions use stable IDs: exceptions 0, close 1, protection 2, app info 3. */
@Composable
fun ContextMenu(task: RecentTask, selected: Int, canManageTasks: Boolean = true, closeProtected: Boolean = false, appProtected: Boolean = false) {
    val accent = LocalAccent.current
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(400.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Tokens.surface)
                .border(1.dp, Tokens.line, RoundedCornerShape(20.dp))
                .padding(22.dp),
        ) {
            // app header
            Row(verticalAlignment = Alignment.CenterVertically) {
                val painter = remember(task.packageName) {
                    task.icon?.let {
                        val w = it.intrinsicWidth.takeIf { v -> v > 0 } ?: 96
                        val h = it.intrinsicHeight.takeIf { v -> v > 0 } ?: 96
                        BitmapPainter(it.toBitmap(w, h).asImageBitmap())
                    }
                }
                if (painter != null) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(11.dp)).background(Tokens.card), Alignment.Center) {
                        Image(painter, task.label, Modifier.size(34.dp), contentScale = ContentScale.Fit)
                    }
                    Spacer(Modifier.width(16.dp))
                }
                Text(task.label, style = Type.row, color = Tokens.text, maxLines = 1)
            }
            Spacer(Modifier.size(20.dp))

            MenuRow("Add to exceptions", if (canManageTasks) "Hide from recents and exclude from Kill All" else "Hide this app from recents", selected == 0, accent.base)
            if (canManageTasks) {
                Spacer(Modifier.size(8.dp))
                MenuRow("Close app", if (closeProtected) "Protected · change protection in Settings → Apps" else "Remove task; use the force-stop preference",
                    selected == 1, accent.base, enabled = !closeProtected)
                Spacer(Modifier.size(8.dp))
                MenuRow(if (appProtected) "Unprotect app" else "Protect app",
                    if (appProtected) "Remove from the protected-app list" else "Keep this app safe from Kill All",
                    selected == 2, accent.base)
            }
            Spacer(Modifier.size(8.dp))
            MenuRow("App info", "Open the system app settings", selected == 3, accent.base)
            Text("↑↓ Choose · OK Select · BACK Cancel", style = Type.small, color = Tokens.textDim,
                modifier = Modifier.padding(top = 16.dp))
        }
    }
}

@Composable
private fun MenuRow(title: String, desc: String, focused: Boolean, accent: Color, enabled: Boolean = true) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (focused) Tokens.surface2 else Color.Transparent)
            .border(1.5.dp, if (focused) accent else Color.Transparent, RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 13.dp),
    ) {
        Text(title, style = Type.row, color = if (!enabled) Tokens.textFaint else if (focused) accent else Tokens.text)
        Text(desc, style = Type.small, color = Tokens.textDim, modifier = Modifier.padding(top = 2.dp))
    }
}

