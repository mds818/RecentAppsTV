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
    REMOTE_SETUP("Remote button setup", "Optional: open Recent Apps from a remote button", "${UpdateManager.REPOSITORY}#open-it-from-your-remote"),
    PORTER("Porter", "Optional privileged access · setup and downloads", "https://github.com/d4rken-org/porter"),
    SHIZUKU("Shizuku", "Optional privileged access · setup guide", "https://shizuku.rikka.app/guide/setup/");
}

val aboutActions = listOf(AboutAction.UPDATES, AboutAction.PROJECT, AboutAction.UPSTREAM, AboutAction.REMOTE_SETUP, AboutAction.PORTER, AboutAction.SHIZUKU)
val updateActions = listOf(AboutAction.CHECK, AboutAction.INSTALL, AboutAction.AUTO_CHECK, AboutAction.NOTIFY, AboutAction.RELEASES)

@Composable
fun AboutScreen(version: String, focus: Int, update: UpdateState, installAllowed: Boolean, settings: com.websnake.recentstv.data.RecentsSettings, updatesOnly: Boolean = false, bottomNotice: String? = null) {
    val actions = if (updatesOnly) updateActions else aboutActions
    val listState = rememberLazyListState()
    KeepFocusedItemVisible(listState, focus, settings.smoothMotion, updatesOnly)
    Column(Modifier.fillMaxSize().background(Tokens.bg).padding(start = 60.dp, end = 60.dp, top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(if (updatesOnly) "About · Updates" else "About · RecentAppsTV (Fork)", style = Type.title, color = Tokens.text)
        Text(version, style = Type.small, color = Tokens.textDim)
        Text(if (updatesOnly) update.message else "Porter / Shizuku adds task switching and closing. Usage Access works without either.",
            style = Type.small,
            color = if (!updatesOnly) Tokens.textDim else if (update.phase == UpdatePhase.ERROR) Color(0xFFFFB4AB) else LocalAccent.current.base)
        Box(Modifier.fillMaxWidth().height(1.dp).background(Tokens.line))
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
                Column {
                val group = when (action) {
                    AboutAction.PROJECT -> "Project and credits"
                    AboutAction.REMOTE_SETUP -> "Setup & tools"
                    AboutAction.AUTO_CHECK -> "Update preferences"
                    else -> null
                }
                group?.let { SettingsGroupHeading(it) }
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                    .background(if (focus == index) Tokens.surface2 else Tokens.surface)
                    .border(1.dp, if (focus == index) LocalAccent.current.base else Color.Transparent, RoundedCornerShape(11.dp))
                    .padding(horizontal = 22.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(title, style = Type.row, color = if (available) Tokens.text else Tokens.textDim)
                        Text(if (action == AboutAction.INSTALL && update.release != null) "Version ${update.release.version} · Android confirms installation"
                            else action.detail, style = Type.small, color = Tokens.textDim)
                    }
                    when (action) {
                        AboutAction.AUTO_CHECK -> SettingsToggle(settings.autoCheckUpdates)
                        AboutAction.NOTIFY -> SettingsToggle(settings.showUpdateNotification)
                        else -> Text(if (action.url != null) "↗" else "›", style = Type.row,
                            color = if (available) LocalAccent.current.base else Tokens.textFaint)
                    }
                }
                }
            }
        }
        if (!bottomNotice.isNullOrBlank()) {
            Text(bottomNotice, style = Type.small, color = Tokens.textDim,
                modifier = Modifier.fillMaxWidth().background(Tokens.bg).padding(vertical = 10.dp))
        }
    }
}
