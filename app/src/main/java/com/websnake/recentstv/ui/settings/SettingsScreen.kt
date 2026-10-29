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
import com.websnake.recentstv.data.RecentsSettings
import com.websnake.recentstv.ui.theme.*

enum class SetRow(val section: String? = null) {
    POSITION("Appearance"), DIRECTION, SIZE, SHAPE, PADDING, ACCENT, BG_DIM, BG_TINT, HEADER, SETTINGS_BUTTON, HELP, APP_NAMES, APP_TEXT_POSITION, APP_DETAILS, MOTION, CLOSE_SPEED,
    EXCEPTIONS("Apps"),
    PRIVILEGED("Advanced · Shizuku / Porter"), MANAGER, KILL_ALL, FORCE_STOP,
    CLOSE_HINT, HIDDEN_KILL_EXCLUSION, PROTECTED_APPS, INDIVIDUAL_PROTECTION, CONFIRM_KILL_ALL, RESET("Defaults")
}

enum class SettingsSection(val title: String, val description: String, val rows: List<SetRow>) {
    APPEARANCE("Appearance", "Cards, colors, layout and app labels", listOf(SetRow.POSITION, SetRow.DIRECTION, SetRow.SIZE, SetRow.SHAPE, SetRow.PADDING, SetRow.ACCENT, SetRow.BG_DIM, SetRow.BG_TINT, SetRow.HEADER, SetRow.APP_NAMES, SetRow.APP_TEXT_POSITION, SetRow.APP_DETAILS)),
    CONTROLS("Controls", "Settings access, close hint and motion", listOf(SetRow.SETTINGS_BUTTON, SetRow.CLOSE_HINT, SetRow.HELP, SetRow.MOTION, SetRow.CLOSE_SPEED)),
    APPS("Apps", "App visibility and optional privileged closing", listOf(SetRow.EXCEPTIONS, SetRow.KILL_ALL, SetRow.CONFIRM_KILL_ALL, SetRow.HIDDEN_KILL_EXCLUSION, SetRow.PROTECTED_APPS, SetRow.INDIVIDUAL_PROTECTION)),
    ADVANCED("Advanced", "Porter / Shizuku access and force-stop", listOf(SetRow.PRIVILEGED, SetRow.MANAGER, SetRow.FORCE_STOP));
    companion object { fun forRow(row: SetRow): SettingsSection? = entries.firstOrNull { row in it.rows } }
}

object SettingsMenu {
    val presets = SettingsSection.entries.size
    val about = presets + 1
    val reset = about + 1
}

@Composable
fun SettingsHome(focus: Int, smoothMotion: Boolean = true) {
    val listState = rememberLazyListState()
    KeepFocusedItemVisible(listState, focus, smoothMotion)
    Box(Modifier.fillMaxSize().background(Tokens.bg)) {
        Column(Modifier.fillMaxSize().padding(start = 60.dp, end = 60.dp, top = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Recent Apps · Settings", style = Type.title, color = Tokens.text)
            Text("↑↓ Choose · OK Open · BACK Return", style = Type.small, color = Tokens.textDim,
                modifier = Modifier.padding(bottom = 6.dp))
            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(SettingsMenu.reset + 1) { index ->
                    Column {
                    if (index < SettingsSection.entries.size) {
                        val section = SettingsSection.entries[index]
                        SettingRow(section.title, section.description, focus == index, compact = true) {
                            Text("›", style = Type.title, color = Tokens.textDim)
                        }
                    } else if (index == SettingsMenu.presets || index == SettingsMenu.about) {
                        val presets = index == SettingsMenu.presets
                        SettingRow(if (presets) "Appearance presets" else "About",
                            if (presets) "Default, Clean, Clean+ and Classic · restore previous look" else "Updates, project links and optional access tools",
                            focus == index, compact = true) {
                            Text("›", style = Type.title, color = Tokens.textDim)
                        }
                    } else {
                        SettingRow("Reset to defaults", "Restore the default profile · confirmation required", focus == index, compact = true) {
                            Text("Reset…", style = Type.small, color = Tokens.textDim)
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(settings: RecentsSettings, section: SettingsSection, focus: Int, exceptionsSummary: String, protectedSummary: String, managerName: String, managerStatus: String, privilegedActive: Boolean? = null) {
    val listState = rememberLazyListState()
    KeepFocusedItemVisible(listState, focus, settings.smoothMotion, section)
    Box(Modifier.fillMaxSize().background(Tokens.bg)) {
        Column(Modifier.fillMaxSize().padding(start = 60.dp, end = 60.dp, top = 20.dp)) {
            Text("Settings · ${section.title}", style = Type.title, color = Tokens.text)
            Text("↑↓ Choose · ←→ Adjust · OK Toggle / Open", style = Type.small, color = Tokens.textDim,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(section.rows.size) { index ->
                    val row = section.rows[index]
                    Column {
                        val group = when (row) {
                            SetRow.POSITION -> "Layout"
                            SetRow.SIZE -> "Cards"
                            SetRow.ACCENT -> "Colors & background"
                            SetRow.HEADER -> "Information"
                            SetRow.SETTINGS_BUTTON -> "Access & guidance"
                            SetRow.MOTION -> "Motion"
                            SetRow.KILL_ALL -> "Kill All"
                            SetRow.PROTECTED_APPS -> "Protection"
                            SetRow.EXCEPTIONS -> "App visibility"
                            else -> null
                        }
                        group?.let { SettingsGroupHeading(it) }
                        if (row == SetRow.KILL_ALL || row == SetRow.PROTECTED_APPS) {
                            AccessInfoLabel(when (privilegedActive) {
                                true -> "Requires Porter / Shizuku · Active"
                                false -> "Requires Porter / Shizuku · Not active"
                                null -> "Requires Porter / Shizuku · Checking access…"
                            }, when (privilegedActive) { true -> Tokens.accent; false -> Color(0xFFE0B56A); null -> Tokens.textDim })
                            Spacer(Modifier.height(8.dp))
                        }
                        val title = when (row) {
                            SetRow.POSITION -> "Selection position"
                            SetRow.DIRECTION -> "Older apps appear"
                            SetRow.SIZE -> "Card size"
                            SetRow.SHAPE -> "Card shape"
                            SetRow.PADDING -> "Card padding"
                            SetRow.ACCENT -> "Accent color"
                            SetRow.BG_DIM -> "Background dim"
                            SetRow.BG_TINT -> "Background color"
                            SetRow.HEADER -> "Show header"
                            SetRow.SETTINGS_BUTTON -> "Show settings button"
                            SetRow.APP_TEXT_POSITION -> "App text position"
                            SetRow.APP_NAMES -> "Show app names"
                            SetRow.APP_DETAILS -> "App details"
                            SetRow.CLOSE_HINT -> "Close hint style"
                            SetRow.HELP -> "Show control hints"
                            SetRow.MOTION -> "Smooth motion"
                            SetRow.CLOSE_SPEED -> "Close animation speed"
                            SetRow.HIDDEN_KILL_EXCLUSION -> "Exclude hidden apps from Kill All"
                            SetRow.EXCEPTIONS -> "App exceptions"
                            SetRow.PROTECTED_APPS -> "Protected apps"
                            SetRow.INDIVIDUAL_PROTECTION -> "Protect individual closes"
                            SetRow.CONFIRM_KILL_ALL -> "Confirm Kill All"
                            SetRow.PRIVILEGED -> "Use Shizuku / Porter"
                            SetRow.MANAGER -> "Open $managerName"
                            SetRow.KILL_ALL -> "Kill All location"
                            SetRow.FORCE_STOP -> "Force-stop on close"
                            SetRow.RESET -> "Reset to defaults"
                        }
                        val desc = when (row) {
                            SetRow.POSITION -> "Keep the selected card centered, or let the row reach either edge."
                            SetRow.DIRECTION -> "Choose which way to browse from the most recent app."
                            SetRow.SIZE -> "The size of application cards."
                            SetRow.SHAPE -> "Use square, rounded, or circular cards."
                            SetRow.PADDING -> "Change the gray space around each icon without changing the card size."
                            SetRow.ACCENT -> "OK opens presets and more colors. Preview before applying."
                            SetRow.BG_DIM -> "Darken the app behind the overlay."
                            SetRow.BG_TINT -> "Tint applied with the background dim."
                            SetRow.HEADER -> "Show the title, app count, and mode. The settings button has its own switch."
                            SetRow.SETTINGS_BUTTON -> "When hidden, press UP twice to open Settings."
                            SetRow.APP_TEXT_POSITION -> "Place names and details under the selected app or at the bottom. Independent of control hints."
                            SetRow.APP_NAMES -> "Show the selected app name. Turn off for an icon-only view."
                            SetRow.APP_DETAILS -> "Privileged only shows details for actual tasks, with Porter or Shizuku."
                            SetRow.CLOSE_HINT -> "Style of the second-press hint beneath an app."
                            SetRow.HELP -> "Show remote-control hints along the bottom."
                            SetRow.MOTION -> "Animate focus and scrolling. Off gives immediate movement."
                            SetRow.CLOSE_SPEED -> "Normal for daily use; Slow makes the drop clearer in recordings."
                            SetRow.HIDDEN_KILL_EXCLUSION -> "Off allows Kill All to close hidden apps; protected apps are always skipped."
                            SetRow.EXCEPTIONS -> "$exceptionsSummary · Works in both modes"
                            SetRow.PROTECTED_APPS -> protectedSummary
                            SetRow.INDIVIDUAL_PROTECTION -> "Also protect chosen apps from DOWN and the context-menu close action."
                            SetRow.CONFIRM_KILL_ALL -> "Review before closing all apps. Off closes immediately; protected apps are still skipped."
                            SetRow.PRIVILEGED -> "When enabled, use actual tasks if authorized; otherwise use history."
                            SetRow.MANAGER -> "$managerStatus. OK opens the manager or requests access."
                            SetRow.KILL_ALL -> "Hidden automatically in usage-history mode."
                            SetRow.FORCE_STOP -> "Also stop the app when closing its task."
                            SetRow.RESET -> "Restore the preferred profile and clear app exceptions. Confirmation required."
                        }
                        val requiresAccess = row == SetRow.CLOSE_HINT || row == SetRow.CLOSE_SPEED ||
                            row == SetRow.FORCE_STOP || row == SetRow.PRIVILEGED
                        SettingRow(title, desc, focus == index,
                            accessLabel = if (requiresAccess) "Porter / Shizuku \u00b7 " + when (privilegedActive) { true -> "Active"; false -> "Not active"; null -> "Checking access\u2026" } else null,
                            accessColor = when (privilegedActive) { true -> Tokens.accent; false -> Color(0xFFE0B56A); null -> Tokens.textDim }) {
                            when (row) {
                                SetRow.POSITION -> Segmented(listOf("Centered", "Side"), settings.position.ordinal)
                                SetRow.DIRECTION -> Segmented(listOf("← Left", "Right →"), settings.direction.ordinal)
                                SetRow.SIZE -> Segmented(listOf("Small", "Medium", "Large"), settings.size.ordinal)
                                SetRow.SHAPE -> { ShapePreview(settings.shape); Spacer(Modifier.width(11.dp)); Segmented(listOf("Square", "Rounded", "Circle"), settings.shape.ordinal) }
                                SetRow.PADDING -> Segmented(listOf("None", "Compact", "Normal", "Wide"), settings.cardPadding.ordinal)
                                SetRow.ACCENT -> {
                                    Box(Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(settings.accentColor))
                                    Spacer(Modifier.width(16.dp))
                                    Text("›", style = Type.title, color = Tokens.textDim)
                                }
                                SetRow.BG_DIM -> DimBar(settings.bgDim)
                                SetRow.BG_TINT -> SwatchRow(BgTint.entries.map { it.color }, settings.bgTint.ordinal, ring = true)
                                SetRow.HEADER -> SettingsToggle(settings.showHeader)
                                SetRow.SETTINGS_BUTTON -> SettingsToggle(settings.showSettingsButton)
                                SetRow.APP_TEXT_POSITION -> Segmented(listOf("Under app", "Bottom"), settings.appTextPosition.ordinal)
                                SetRow.APP_NAMES -> SettingsToggle(settings.showAppNames)
                                SetRow.APP_DETAILS -> Segmented(listOf("Off", "Always", "Privileged only"), settings.appDetailsMode.ordinal)
                                SetRow.CLOSE_HINT -> Segmented(listOf("Elevated", "Minimal"), settings.closeHintStyle.ordinal)
                                SetRow.HELP -> SettingsToggle(settings.showHelp)
                                SetRow.MOTION -> SettingsToggle(settings.smoothMotion)
                                SetRow.CLOSE_SPEED -> Segmented(listOf("Normal", "Slow"), settings.closeAnimationSpeed.ordinal)
                                SetRow.PRIVILEGED -> SettingsToggle(settings.usePrivileged)
                                SetRow.KILL_ALL -> Segmented(listOf("App row", "Top controls", "Hidden"), settings.killAllLocation.ordinal)
                                SetRow.FORCE_STOP -> SettingsToggle(settings.forceStopOnKill)
                                SetRow.RESET -> Text("Reset…", style = Type.small, color = Tokens.textDim)
                                SetRow.HIDDEN_KILL_EXCLUSION -> SettingsToggle(settings.excludeHiddenFromKillAll)
                                SetRow.EXCEPTIONS -> Text("›", style = Type.title, color = Tokens.textDim)
                                SetRow.PROTECTED_APPS -> Text("›", style = Type.title, color = Tokens.textDim)
                                SetRow.INDIVIDUAL_PROTECTION -> SettingsToggle(settings.protectIndividualCloses)
                                SetRow.CONFIRM_KILL_ALL -> SettingsToggle(settings.confirmKillAll)
                                SetRow.MANAGER -> Text("↗", style = Type.row, color = Tokens.textDim)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResetDefaultsDialog(confirmSelected: Boolean) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
        Column(Modifier.width(540.dp).clip(RoundedCornerShape(20.dp)).background(Tokens.surface)
            .border(1.dp, Tokens.line, RoundedCornerShape(20.dp)).padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("Reset to your default profile?", style = Type.title, color = Tokens.text)
            Text("Centered · older apps left · Large · Circle · Green · 80% dim · Black background",
                style = Type.body, color = Tokens.textDim)
            Text("Also clears hidden and protected app lists and the saved appearance. Android permissions stay unchanged.",
                style = Type.small, color = Tokens.textDim)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Segmented(listOf("Cancel", "Reset"), if (confirmSelected) 1 else 0)
            }
            Text("←→ Choose · OK Confirm · BACK Cancel", style = Type.small, color = Tokens.textDim)
        }
    }
}
@Composable
private fun SettingRow(title: String, desc: String, focused: Boolean, compact: Boolean = false, accessLabel: String? = null, accessColor: Color = Tokens.textDim, control: @Composable () -> Unit) {
    val accent = LocalAccent.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(if (focused) Tokens.surface2 else Tokens.surface)
            .border(1.dp, if (focused) accent.base else Color.Transparent, RoundedCornerShape(11.dp))
            .padding(horizontal = 22.dp, vertical = if (compact) 10.dp else 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.row, color = Tokens.text)
            if (accessLabel != null) AccessInfoLabel(accessLabel, accessColor)
            Text(desc, style = Type.small, color = Tokens.textDim, modifier = Modifier.padding(top = 4.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) { control() }
    }
}

@Composable
internal fun SettingsToggle(on: Boolean) {
    val accent = LocalAccent.current
    Box(
        Modifier.size(width = 54.dp, height = 28.dp).clip(RoundedCornerShape(percent = 50))
            .background(if (on) accent.base else Color(0xFF2B2F36))
            .border(1.dp, if (on) accent.base else Tokens.line, RoundedCornerShape(percent = 50)),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier.padding(horizontal = 3.dp).size(21.dp).clip(RoundedCornerShape(percent = 50))
                .background(if (on) Color.White else Color(0xFF6B7280))
        )
    }
}

@Composable
private fun Segmented(labels: List<String>, selected: Int) {
    val accent = LocalAccent.current
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF16181C))
            .border(1.dp, Tokens.line, RoundedCornerShape(8.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        labels.forEachIndexed { i, lab ->
            val sel = i == selected
            Box(
                Modifier.clip(RoundedCornerShape(6.dp))
                    .background(if (sel) accent.base else Color.Transparent)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) { Text(lab, style = Type.small, color = if (sel) accent.onDark else Tokens.textDim) }
        }
    }
}

/** Row of colour swatches; the selected one gets a white ring. */
@Composable
private fun SwatchRow(colors: List<Color>, selected: Int, ring: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        colors.forEachIndexed { i, c ->
            val sel = i == selected
            Box(
                Modifier
                    .size(if (sel) 38.dp else 30.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(c)
                    .border(
                        if (sel) 2.5.dp else 1.dp,
                        if (sel) Color.White else Tokens.line,
                        RoundedCornerShape(percent = 50),
                    )
            )
        }
    }
}

/** 0–100% dim level shown as a filled bar with a percentage; ‹/› adjust. */
@Composable
private fun DimBar(pct: Int) {
    val accent = LocalAccent.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier
                .width(260.dp).height(14.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(Color(0xFF2B2F36)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(pct / 100f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(accent.base)
            )
        }
        Text("$pct%", style = Type.small, color = Tokens.text, modifier = Modifier.width(56.dp))
    }
}

@Composable
private fun ShapePreview(shape: CardShapeOpt) {
    Box(
        Modifier.size(38.dp).clip(shape.shape(38.dp)).background(Color(0xFF3A3F48))
            .border(1.dp, Tokens.line, shape.shape(38.dp))
    )
}




