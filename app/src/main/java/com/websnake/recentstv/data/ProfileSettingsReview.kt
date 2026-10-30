package com.websnake.recentstv.data

enum class ProfileSettingsFilter(val title: String) { CHANGES("Changes"), ALL("All"), COMPATIBILITY("Compatibility") }
enum class ProfileSettingState { CHANGED, UNCHANGED, MISSING, UNSUPPORTED }

data class ProfileSettingRow(val key: String, val group: String, val label: String, val current: String, val target: String,
    val state: ProfileSettingState, val partial: Boolean, val legacy: Boolean, val raw: String, val reason: String) {
    val warning get() = partial || state == ProfileSettingState.MISSING || state == ProfileSettingState.UNSUPPORTED
    val status get() = when (state) {
        ProfileSettingState.CHANGED -> if (partial) "Changes · partly supported" else "Changes"
        ProfileSettingState.UNCHANGED -> if (partial) "Unchanged · partly supported" else "Unchanged"
        ProfileSettingState.MISSING -> "Missing · kept"
        ProfileSettingState.UNSUPPORTED -> "Unsupported · skipped"
    }
    fun technicalLines(): List<Pair<String, String>> = listOf("Setting key" to key, "Status" to status, "Current value" to current, "Effective result" to target,
        "Profile value" to raw, "Explanation" to reason.ifBlank { if (legacy) "Legacy format; effective values follow the current settings readers." else "Compatible setting." })
        .flatMap { (label, value) -> value.ifEmpty { "(empty)" }.chunked(320).mapIndexed { i, part -> (if (i == 0) label else "$label · continued") to part } }
}

data class ProfileReviewSnapshot(val report: ProfileImport, val changes: List<PresetChange>, val rows: List<ProfileSettingRow>)

/** Presentation only: unknown/missing settings remain governed by ProfileCodec, not a migration table. */
object ProfileSettingsReview {
    private val aliases = mapOf("focus_position" to "position", "app_direction" to "direction", "bg_dim_pct" to "bgDim",
        "custom_accent_argb" to "customAccent", "app_list_mode" to "listMode", "show_app_details" to "appDetailsMode",
        "show_kill_all" to "killAllLocation", "carousel_layout" to "position", "show_header" to "headerMode",
        "show_view_switch" to "viewButtons", "view_switch_mode" to "viewButtons", "restore_in_top" to "restoreTopMode",
        "show_view_notice" to "viewNoticeStyle", "confirm_kill_all" to "bulkConfirmation", "down_action" to "downWithAccess",
        "bulk_action" to "bulkWithAccess", "dismiss_fallback" to "downWithoutAccess", "close_animation_speed" to "animationSpeed")
    private val legacyKeys = setOf("show_app_details", "show_kill_all", "carousel_layout", "show_header", "show_view_switch",
        "view_switch_mode", "restore_in_top", "show_view_notice", "confirm_kill_all", "down_action", "bulk_action", "dismiss_fallback", "close_animation_speed", "restore_menu_configured")
    private val names = mapOf("focus_position" to "Selection position", "app_direction" to "Older apps appear", "size" to "Card size", "shape" to "Card shape",
        "bg_dim_pct" to "Background dim", "bg_tint" to "Background tint", "custom_accent_argb" to "Custom accent colour",
        "show_help" to "Show control hints", "use_privileged" to "Use Porter / Shizuku", "app_list_mode" to "List source",
        "kill_all_location" to "Bulk button location", "view_buttons" to "Switch buttons", "edge_without_access" to "Edge action without access",
        "edge_action" to "Edge action with access", "down_with_access" to "DOWN with access", "down_without_access" to "DOWN without access",
        "bulk_with_access" to "Bulk action with access", "bulk_without_access" to "Bulk action without access",
        "hide_top_restore" to "Reveal restore button on UP", "hide_top_bulk" to "Reveal bulk button on UP", "hide_top_switch" to "Reveal switch button on UP")
    private val appearance = setOf("focus_position", "app_direction", "size", "shape", "card_padding", "accent", "custom_accent_argb", "bg_dim_pct", "bg_tint", "header_mode", "show_app_names", "app_text_position", "app_details_mode")
    private val groups = listOf("Appearance", "Controls", "Apps", "Advanced", "Updates", "Compatibility")
    fun select(rows: List<ProfileSettingRow>, filter: ProfileSettingsFilter) = rows.filter { when (filter) {
        ProfileSettingsFilter.ALL -> true
        ProfileSettingsFilter.CHANGES -> it.state == ProfileSettingState.CHANGED
        ProfileSettingsFilter.COMPATIBILITY -> it.warning || it.legacy
    } }
    fun values(settings: RecentsSettings): Map<String, Any?> = RecentsSettings::class.java.declaredFields
        .filterNot { it.isSynthetic || java.lang.reflect.Modifier.isStatic(it.modifiers) }.associate { it.isAccessible = true; it.name to it.get(settings) }
    fun build(entries: List<ProfileEntry>, accepted: Set<String>, before: RecentsSettings, after: RecentsSettings,
        beforeRaw: Map<String, Any?>, afterRaw: Map<String, Any?>, effects: Map<String, List<PresetChange>>): List<ProfileSettingRow> {
        val current = values(before); val target = values(after)
        val net = PresetChanges.between(before, after).associateBy { it.label }
        return entries.map { entry ->
            val key = entry.key
            val property = aliases[key] ?: key.split('_').mapIndexed { i, s -> if (i == 0) s else s.replaceFirstChar { it.uppercase() } }.joinToString("")
            val impacts = effects[key].orEmpty().mapNotNull { net[it.label] }
            val legacy = key in legacyKeys
            val state = when {
                entry.status == "Missing" -> ProfileSettingState.MISSING
                key !in accepted -> ProfileSettingState.UNSUPPORTED
                impacts.isNotEmpty() -> ProfileSettingState.CHANGED
                else -> ProfileSettingState.UNCHANGED
            }
            var a = if (current.containsKey(property)) display(current[property], property) else display(beforeRaw[key], key)
            var b = if (target.containsKey(property)) display(target[property], property) else display(afterRaw[key], key)
            if (state == ProfileSettingState.CHANGED && a == b) { a = impacts.joinToString(" · ") { it.before }; b = impacts.joinToString(" · ") { it.after } }
            if (legacy && state == ProfileSettingState.UNCHANGED) b = "No effective change"
            if (state == ProfileSettingState.MISSING) b = "Kept unchanged"
            if (state == ProfileSettingState.UNSUPPORTED) b = "Skipped"
            val group = when {
                key !in accepted && entry.status != "Missing" -> "Compatibility"
                legacy -> "Compatibility"
                key in appearance -> "Appearance"
                key == "use_privileged" || key == "force_stop_on_kill" -> "Advanced"
                key == "auto_check_updates" || key == "show_update_notification" -> "Updates"
                key.startsWith("edge_") || key.startsWith("down_") || key in setOf("show_settings_button", "view_buttons", "hide_top_switch", "remember_view", "row_switch_position", "view_notice_style", "show_help", "show_edge_hints", "smooth_motion", "animation_speed", "close_hint_style", "empty_list_recovery") -> "Controls"
                else -> "Apps"
            }
            val reason = entry.reason.ifBlank { if (legacy && state == ProfileSettingState.UNCHANGED) "Legacy option; a newer setting may take precedence. Current effective values are shown." else "" }
            ProfileSettingRow(key, group, (names[key] ?: words(key).let { if (it.length > 80) it.take(80) + "…" else it }) + if (legacy) " (legacy)" else "", a, b, state,
                entry.status == "Partial", legacy, entry.value, reason)
        }.sortedWith(compareBy({ groups.indexOf(it.group) }, { it.label }))
    }
    private fun words(value: String) = value.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }
    private fun display(value: Any?, property: String): String = when {
        value == null -> if (property == "customAccent") "Preset colour" else "App default"
        value is Boolean -> if (value) "On" else "Off"
        property == "bgDim" || property == "bg_dim_pct" -> "$value%"
        property == "customAccent" && value is Int -> "#%06X".format(value and 0xFFFFFF)
        value is Enum<*> -> words(value.name)
        value is Set<*> -> if (value.isEmpty()) "None" else value.joinToString { if (it is Enum<*>) words(it.name) else it.toString() }
        else -> value.toString()
    }
}
