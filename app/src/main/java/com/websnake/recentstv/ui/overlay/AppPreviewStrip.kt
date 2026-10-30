package com.websnake.recentstv.ui.overlay

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.websnake.recentstv.R
import com.websnake.recentstv.ui.theme.*

data class PreviewApp(val label: String, val icon: Drawable?)

@Composable
fun AppPreviewStrip(title: String, apps: List<PreviewApp>, count: Int = apps.size) {
    if (count == 0) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$title · $count", style = Type.small, color = Tokens.textDim)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            apps.take(8).forEach { app ->
                val icon = app.icon
                val painter = if (icon != null) remember(icon) { drawablePainter(icon) } else painterResource(R.drawable.ic_launcher)
                Image(painter, contentDescription = app.label, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(7.dp)).background(Tokens.surface2).padding(2.dp))
            }
            if (count > apps.take(8).size) Text("+${count - apps.take(8).size}", style = Type.small, color = Tokens.textDim)
        }
    }
}
