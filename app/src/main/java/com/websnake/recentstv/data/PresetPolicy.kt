package com.websnake.recentstv.data

enum class PresetApplyMode { FULL, APPEARANCE_ONLY }

/** New registered preferences join full presets automatically; personal/safety choices stay put. */
object PresetPolicy {
    private val preserved = setOf("saved_profiles", "profile_undo", "appearance_before_presets",
        "exceptions", "protected_apps", "dismissed_apps", "use_privileged", "auto_check_updates", "show_update_notification",
        "force_stop_on_kill", "force_stop", "confirm_kill_all", "bulk_confirmation", "confirm_restore_dismissed",
        "protect_individual_closes", "allow_dismiss_protected", "protection_feedback", "exclude_hidden_from_kill_all")
    private val actions = setOf("edge_action", "edge_without_access", "edge_shortcut", "edge_menu_items", "restore_menu_configured",
        "down_action", "down_with_access", "down_without_access", "enable_dismiss", "dismiss_scope", "dismiss_fallback",
        "bulk_action", "bulk_with_access", "bulk_without_access", "app_list_mode", "empty_list_recovery",
        "remember_view", "animation_speed", "close_animation_speed")
    fun managed(key: String) = key !in preserved
    fun visual(key: String) = managed(key) && key !in actions
}

data class PresetChange(val group: String, val label: String, val before: String, val after: String)

/** Compare effective values, not absent preference keys. Runs only in the preset review. */
object PresetChanges {
    private val legacy = setOf("showHeader", "showViewSwitch", "viewSwitchMode", "showViewNotice", "restoreInTop",
        "confirmKillAll", "downAction", "bulkAction", "dismissFallback", "closeAnimationSpeed")
    private val labels = mapOf("position" to "Selection position", "direction" to "Older apps appear",
        "size" to "Card size", "shape" to "Card shape", "bgDim" to "Background dim", "bgTint" to "Background tint",
        "killAllLocation" to "Bulk button location", "viewButtons" to "Switch buttons", "edgeAction" to "Edge action with access",
        "edgeWithoutAccess" to "Edge action without access", "downWithAccess" to "DOWN with access", "downWithoutAccess" to "DOWN without access",
        "bulkWithAccess" to "Bulk action with access", "bulkWithoutAccess" to "Bulk action without access",
        "hideTopBulk" to "Reveal bulk button on UP", "hideTopRestore" to "Reveal restore button on UP", "hideTopSwitch" to "Reveal switch button on UP")
    fun between(before: RecentsSettings, after: RecentsSettings): List<PresetChange> =
        RecentsSettings::class.java.declaredFields.filterNot { it.isSynthetic || java.lang.reflect.Modifier.isStatic(it.modifiers) || it.name in legacy }
            .mapNotNull { field ->
                field.isAccessible = true
                val a = field.get(before); val b = field.get(after)
                if (a == b) null else {
                    val name = field.name
                    val group = when {
                        name == "usePrivileged" || name == "autoCheckUpdates" || name == "showUpdateNotification" -> "Access & updates"
                        name.startsWith("protect") || name.startsWith("allowDismissProtected") || name.startsWith("confirm") || name == "forceStopOnKill" || name == "excludeHiddenFromKillAll" -> "Protection & confirmations"
                        name.startsWith("edge") || name.startsWith("down") || name.startsWith("bulk") || name.contains("Dismiss") || name == "dismissScope" || name == "listMode" || name == "rememberView" || name == "emptyListRecovery" -> "Actions & behaviour"
                        else -> "Appearance & controls"
                    }
                    PresetChange(group, labels[name] ?: words(name.replace(Regex("([a-z])([A-Z])"), "$1 $2")), value(a, name), value(b, name))
                }
            }.sortedWith(compareBy({ it.group }, { it.label }))
    private fun words(text: String) = text.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }
    private fun value(value: Any?, name: String): String = when {
        value == null -> "Preset colour"
        value is Boolean -> if (value) "On" else "Off"
        name == "bgDim" -> "$value%"
        name == "customAccent" && value is Int -> "#%06X".format(value and 0xFFFFFF)
        value is Enum<*> -> words(value.name)
        value is Set<*> -> if (value.isEmpty()) "None" else value.joinToString { if (it is Enum<*>) words(it.name) else it.toString() }
        else -> value.toString()
    }
}
