package com.websnake.recentstv.ui.state

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.websnake.recentstv.ui.theme.Tokens
import com.websnake.recentstv.ui.theme.Type

@Composable
fun UsageAccessState() {
    Box(Modifier.fillMaxSize().background(Tokens.surface), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 700.dp).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("Allow Usage Access", style = Type.title, color = Tokens.text)
            Text("Without Shizuku, RecentAppsTV shows apps used in the last 7 days. These are usage history, not running tasks. Closing apps requires Shizuku.",
                style = Type.body, color = Tokens.textDim, textAlign = TextAlign.Center)
            Text("Press OK to open Usage Access settings and allow RecentAppsTV. Then press Back to return.",
                style = Type.body, color = Tokens.text, textAlign = TextAlign.Center)
            Text("MENU opens app settings / BACK closes", style = Type.small, color = Tokens.textDim)
        }
    }
}
