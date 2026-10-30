package com.websnake.recentstv.ui.state

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun UsageAccessState(
    title: String = "Allow Usage Access",
    description: String = "Allow Usage Access to show recently used apps without Porter or Shizuku. No debugging or background service is needed.",
    onGrant: () -> Unit,
    guidance: String = "OK  Open Usage Access     UP  Settings     BACK  Exit",
) {
    Box(Modifier.fillMaxSize().background(Tokens.surface), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 700.dp).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(title, style = Type.title, color = Tokens.text)
            Text(description,
                style = Type.body, color = Tokens.textDim, textAlign = TextAlign.Center)
            Box(Modifier.border(1.dp, Tokens.accent, RoundedCornerShape(16.dp))
                .clickable(onClick = onGrant).padding(horizontal = 28.dp, vertical = 14.dp)) {
                Text("Grant Usage Access", style = Type.body, color = Tokens.accent)
            }
            Text(guidance, style = Type.small, color = Tokens.textDim)
        }
    }
}
