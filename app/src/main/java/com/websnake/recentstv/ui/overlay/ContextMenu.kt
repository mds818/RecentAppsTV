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
import androidx.compose.foundation.layout.height
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
import com.websnake.recentstv.data.ActionPolicy
import com.websnake.recentstv.data.RecentTask
import com.websnake.recentstv.ui.theme.LocalAccent
import com.websnake.recentstv.ui.theme.Tokens
import com.websnake.recentstv.ui.theme.Type

/** Context actions use stable IDs: exceptions 0, close 1, protection 2, app info 3. */
@Composable
fun ContextMenu(task: RecentTask, selected: Int, canManageTasks: Boolean = true, closeProtected: Boolean = false, appProtected: Boolean = false, excludeHiddenFromKillAll: Boolean = true, allowDismiss: Boolean = false, allowRestore: Boolean = false, restoreCount: Int = 0) {
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
                .padding(if (allowRestore) 14.dp else if (allowDismiss) 16.dp else 22.dp),
        ) {
            // app header
            Row(verticalAlignment = Alignment.CenterVertically) {
                val painter = remember(task.packageName, task.icon) {
                    task.icon?.let { drawablePainter(it) }
                }
                if (painter != null) {
                    Box(Modifier.size(if (allowRestore) 40.dp else 44.dp).clip(RoundedCornerShape(11.dp)).background(Tokens.card), Alignment.Center) {
                        Image(painter, task.label, Modifier.size(if (allowRestore) 30.dp else 34.dp), contentScale = ContentScale.Fit)
                    }
                    Spacer(Modifier.width(16.dp))
                }
                Text(task.label, style = Type.row, color = Tokens.text, maxLines = 1)
            }
            Spacer(Modifier.size(if (allowRestore) 8.dp else if (allowDismiss) 12.dp else 20.dp))

            val actions = ActionPolicy.context(allowDismiss, canManageTasks && !closeProtected, canManageTasks, allowRestore)
            actions.forEachIndexed { index, action ->
                if (index > 0) Spacer(Modifier.size(if (allowRestore) 4.dp else 8.dp))
                if (action == 5 || (index > 0 && action in listOf(2, 0) && actions[index - 1] in listOf(4, 1))) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Tokens.line))
                    Spacer(Modifier.size(6.dp))
                }
                val title = when (action) {
                    5 -> "Restore dismissed apps"
                    4 -> "Dismiss from list"
                    1 -> "Close task"
                    2 -> if (appProtected) "Unprotect app" else "Protect app"
                    0 -> "Hide app"
                    else -> "App info"
                }
                val detail = when (action) {
                    5 -> "Show $restoreCount dismissed ${if (restoreCount == 1) "app" else "apps"} again"
                    4 -> "Hide until used again; does not close the app"
                    1 -> "Remove the task; use the force-stop preference"
                    2 -> if (appProtected) "Remove from the protected-app list" else "Prevent closing; dismissal follows Protection settings"
                    0 -> if (canManageTasks && excludeHiddenFromKillAll) "Add to App exceptions; also skipped by Close all" else "Hide until removed from App exceptions"
                    else -> "Open the system app settings"
                }
                MenuRow(title, detail, selected == action, accent.base, compact = allowDismiss || allowRestore, dense = allowRestore)
            }
            Text("↑↓ Choose · OK Select · BACK Cancel", style = Type.small, color = Tokens.textDim,
                modifier = Modifier.padding(top = if (allowRestore) 10.dp else 16.dp))
        }
    }
}

@Composable
private fun MenuRow(title: String, desc: String, focused: Boolean, accent: Color, enabled: Boolean = true, compact: Boolean = false, dense: Boolean = false) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (focused) Tokens.surface2 else Color.Transparent)
            .border(1.5.dp, if (focused) accent else Color.Transparent, RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = if (dense) 6.dp else if (compact) 9.dp else 13.dp),
    ) {
        Text(title, style = Type.row, color = if (!enabled) Tokens.textFaint else if (focused) accent else Tokens.text)
        if (!compact || focused) Text(desc, style = Type.small, color = Tokens.textDim, maxLines = if (dense) 2 else Int.MAX_VALUE, modifier = Modifier.padding(top = 2.dp))
    }
}

