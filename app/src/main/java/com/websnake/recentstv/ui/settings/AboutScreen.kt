package com.websnake.recentstv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.websnake.recentstv.ui.theme.*

@Composable
fun AboutScreen(version: String) {
    Column(Modifier.fillMaxSize().background(Tokens.bg).padding(horizontal = 60.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("About · RecentAppsTV (Fork)", style = Type.title, color = Tokens.text)
        Text(version, style = Type.row, color = LocalAccent.current.base)
        Text("A recent-app switcher for Android TV and Google TV.", style = Type.body, color = Tokens.text)
        Text("With Porter / Shizuku", style = Type.label, color = Tokens.text)
        Text("Authorized access provides actual tasks, task switching and closing controls.",
            style = Type.body, color = Tokens.textDim)
        Text("Without privileged access", style = Type.label, color = Tokens.text)
        Text("Usage Access provides recently used apps you can launch. History entries are not running tasks.",
            style = Type.body, color = Tokens.textDim)
        Text("Project · github.com/mds818/RecentAppsTV", style = Type.body, color = LocalAccent.current.base)
        Text("Based on RecentAppsTV · Qutaiba-Khader/RecentAppsTV", style = Type.body, color = Tokens.text)
        Text("github.com/Qutaiba-Khader/RecentAppsTV", style = Type.small, color = Tokens.textDim)
        Text("Usage-history fallback and TV appearance enhancements",
            style = Type.small, color = Tokens.textDim)
        Text("BACK Return", style = Type.small, color = LocalAccent.current.base,
            modifier = Modifier.padding(top = 8.dp))
    }
}
