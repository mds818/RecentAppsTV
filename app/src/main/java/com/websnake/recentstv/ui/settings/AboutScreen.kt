package com.websnake.recentstv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import com.websnake.recentstv.ui.theme.*
import com.websnake.recentstv.updates.*

enum class AboutAction(val title: String, val detail: String, val url: String? = null) {
    CHECK("Check for updates", "Checks the latest stable GitHub release"),
    INSTALL("Download update", "Available when a newer release is found"),
    AUTO_CHECK("Automatic update checks", "At most once per app process · no background timer"),
    NOTIFY("Update notification", "Show a brief in-app message when a newer version is found"),
    UPDATES("Updates", "Check, download and notification preferences"),
    RELEASES("Release notes", "What's new and APK downloads", "${UpdateManager.REPOSITORY}/releases"),
    PROJECT("RecentAppsTV · Fork", "Project, help and issue reports", UpdateManager.REPOSITORY),
    UPSTREAM("Original RecentAppsTV", "Created by Qutaiba-Khader", "https://github.com/Qutaiba-Khader/RecentAppsTV"),
    PORTER("Porter", "Optional privileged access · setup and downloads", "https://github.com/d4rken-org/porter"),
    SHIZUKU("Shizuku", "Optional privileged access · setup guide", "https://shizuku.rikka.app/guide/setup/");
}

val aboutActions = listOf(AboutAction.UPDATES, AboutAction.PROJECT, AboutAction.UPSTREAM, AboutAction.PORTER, AboutAction.SHIZUKU)
val updateActions = listOf(AboutAction.CHECK, AboutAction.INSTALL, AboutAction.AUTO_CHECK, AboutAction.NOTIFY, AboutAction.RELEASES)

@Composable
fun AboutScreen(version: String, focus: Int, update: UpdateState, installAllowed: Boolean, settings: com.websnake.recentstv.data.RecentsSettings, updatesOnly: Boolean = false) {
    val actions = if (updatesOnly) updateActions else aboutActions
    val listState = rememberLazyListState()
    LaunchedEffect(focus) { listState.animateScrollToItem(focus) }
    Column(Modifier.fillMaxSize().background(Tokens.bg).padding(horizontal = 60.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (updatesOnly) "About · Updates" else "About · RecentAppsTV (Fork)", style = Type.title, color = Tokens.text)
        Text(version, style = Type.body, color = LocalAccent.current.base)
        Text("Switch recent apps your way. Porter / Shizuku adds actual tasks and closing controls; Usage Access works without either.",
            style = Type.small, color = Tokens.textDim)
        Text(if (updatesOnly || update.release != null) update.message else "Customize your switcher · optional privileged access", style = Type.body, color = LocalAccent.current.base)
        Text("↑↓ Choose · OK Open · BACK Return", style = Type.small, color = Tokens.textDim)
        LazyColumn(state = listState, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(actions.size) { index ->
                val action = actions[index]
                val available = when (action) {
                    AboutAction.CHECK -> update.phase !in listOf(UpdatePhase.CHECKING, UpdatePhase.INSTALLING, UpdatePhase.CONFIRM)
                    AboutAction.INSTALL -> update.phase in listOf(UpdatePhase.AVAILABLE, UpdatePhase.READY, UpdatePhase.CONFIRM) ||
                        (update.phase == UpdatePhase.ERROR && update.release != null)
                    else -> true
                }
                val title = when (action) {
                    AboutAction.CHECK -> if (update.phase == UpdatePhase.DOWNLOADING) "Cancel download" else action.title
                    AboutAction.INSTALL -> when (update.phase) {
                        UpdatePhase.READY -> if (installAllowed) "Install update" else "Allow app installation"
                        UpdatePhase.CONFIRM -> "Continue installation"
                        UpdatePhase.ERROR -> if (update.release != null) "Retry download" else action.title
                        else -> action.title
                    }
                    else -> action.title
                }
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                    .background(if (focus == index) Tokens.surface2 else Tokens.surface)
                    .border(1.dp, if (focus == index) LocalAccent.current.base else Color.Transparent, RoundedCornerShape(11.dp))
                    .padding(horizontal = 22.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(title, style = Type.row, color = if (available) Tokens.text else Tokens.textDim)
                        Text(if (action == AboutAction.INSTALL && update.release != null) "Version ${update.release.version} · Android confirms installation"
                            else action.detail, style = Type.small, color = Tokens.textDim)
                    }
                    Text(when (action) { AboutAction.AUTO_CHECK -> if (settings.autoCheckUpdates) "On" else "Off"; AboutAction.NOTIFY -> if (settings.showUpdateNotification) "On" else "Off"; else -> if (action.url != null) "↗" else "›" }, style = Type.row, color = if (available) LocalAccent.current.base else Tokens.textFaint)
                }
            }
        }
        Text(if (updatesOnly) "Downloads and installation start only when you choose them. Android controls storage placement." else "Links open in your TV browser, if one is installed.",
            style = Type.small, color = Tokens.textDim)
    }
}
