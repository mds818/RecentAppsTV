package com.websnake.recentstv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.websnake.recentstv.ui.theme.*

object AccentChart {
    const val columns = 12
    val colors = AccentColor.entries.map { it.color } + List(columns - AccentColor.entries.size) { Color.hsv(0f, 0f, 1f - it * .09f) } +
        (0 until 5).flatMap { row -> (0 until columns).map { col ->
            Color.hsv(col * 30f, .3f + row * .15f, 1f - row * .05f)
        } }
    fun nearest(color: Color) = colors.indices.minBy { i ->
        val c = colors[i]
        (c.red-color.red)*(c.red-color.red) + (c.green-color.green)*(c.green-color.green) + (c.blue-color.blue)*(c.blue-color.blue)
    }
}

@Composable
fun AccentColorChart(focus: Int) {
    val selected = AccentChart.colors[focus]
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.8f)), contentAlignment=Alignment.Center) {
        Column(Modifier.width(630.dp).clip(RoundedCornerShape(20.dp)).background(Tokens.surface).padding(26.dp),
            verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text("Choose accent color", style=Type.title, color=Tokens.text)
            Text("↑↓ ←→ Browse · OK Apply · BACK Cancel", style=Type.small, color=Tokens.textDim)
            Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
                AccentChart.colors.chunked(AccentChart.columns).forEachIndexed { row, colors ->
                    if (row == 0 || row == 1) Text(if (row == 0) "Presets & neutrals" else "More colors",
                        style = Type.small, color = Tokens.textDim)
                    Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                        colors.forEachIndexed { col, color ->
                            val active = row*AccentChart.columns+col == focus
                            Box(Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).background(color)
                                .border(if(active) 3.dp else 1.dp, if(active) Color.White else Tokens.line, RoundedCornerShape(6.dp)),
                                contentAlignment=Alignment.Center) {
                                if(active) Text("✓", style=Type.row, color=if(color.luminance() > .4f) Color.Black else Color.White)
                            }
                        }
                    }
                }
            }
            Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(selected))
                Text("#%06X".format(selected.toArgb() and 0xFFFFFF), style=Type.row, color=Tokens.text)
                Text("Live preview", style=Type.small, color=Tokens.textDim)
            }
        }
    }
}
