package com.websnake.recentstv.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.websnake.recentstv.ui.theme.LocalAccent
import com.websnake.recentstv.ui.theme.Type

/** Part of its first setting's list item, so remote focus never stops on a heading. */
@Composable
internal fun SettingsGroupHeading(title: String) {
    Text(title, style = Type.small, color = LocalAccent.current.base,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 6.dp))
}
