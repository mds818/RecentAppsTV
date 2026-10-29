package com.websnake.recentstv.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.websnake.recentstv.ui.theme.Tokens
import com.websnake.recentstv.ui.theme.Type

/** Passive information, not a lock or disabled-control indicator. */
@Composable
internal fun AccessInfoLabel(text: String, color: Color = Tokens.textDim) {
    Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Canvas(Modifier.size(12.dp)) {
            drawCircle(color, radius = size.minDimension / 2 - 1.dp.toPx(), style = Stroke(1.dp.toPx()))
            drawCircle(color, radius = 0.7.dp.toPx(), center = Offset(center.x, size.height * 0.31f))
            drawLine(color, Offset(center.x, size.height * 0.47f), Offset(center.x, size.height * 0.72f),
                strokeWidth = 1.3.dp.toPx(), cap = StrokeCap.Round)
        }
        Text(text, style = Type.mono, color = Tokens.textDim)
    }
}
