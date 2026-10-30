package com.websnake.recentstv.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.websnake.recentstv.ui.theme.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "recents_settings")

enum class FocusPosition { CENTER, SIDE }
enum class AppDirection { OLDER_LEFT, OLDER_RIGHT }
enum class CardPadding(val iconFraction: Float) { NONE(1f), COMPACT(0.84f), NORMAL(0.74f), WIDE(0.66f) }
enum class AppDetailsMode { OFF, ALWAYS, PRIVILEGED_ONLY;
    fun visible(privileged: Boolean) = this == ALWAYS || (this == PRIVILEGED_ONLY && privileged)
}
enum class KillAllLocation { APP_ROW, TOP_CONTROLS, HIDDEN, BOTH;
    val inRow get() = this == APP_ROW || this == BOTH
    val inTop get() = this == TOP_CONTROLS || this == BOTH
}
enum class AppTextPosition { UNDER_APP, BOTTOM }

enum class ProtectionFeedback { OFF, HINT_ONLY, ANIMATION_ONLY, ANIMATION_AND_HINT;
    val animate get() = this == ANIMATION_ONLY || this == ANIMATION_AND_HINT
    val hint get() = this == HINT_ONLY || this == ANIMATION_AND_HINT
}

enum class CloseHintStyle { ELEVATED, MINIMAL }
enum class CloseAnimationSpeed(val durationMs: Int) { NORMAL(240), SLOW(350) } // Legacy migration.
enum class AnimationSpeed(private val percent: Int) { FAST(70), NORMAL(100), SLOW(160);
    fun duration(normalMs: Int): Int = (normalMs * percent / 100).coerceAtLeast(0)
    companion object { fun legacy(close: CloseAnimationSpeed) = if (close == CloseAnimationSpeed.SLOW) SLOW else NORMAL }
}

/** Defaults form one profile; clearing preferences restores exactly these values. */
data class RecentsSettings(
    val position: FocusPosition = FocusPosition.CENTER,
    val direction: AppDirection = AppDirection.OLDER_LEFT,
    val killAllLocation: KillAllLocation = KillAllLocation.APP_ROW,
    val size: CardSizeOpt = CardSizeOpt.LARGE,
    val shape: CardShapeOpt = CardShapeOpt.CIRCLE,
    val cardPadding: CardPadding = CardPadding.NORMAL,
    val exceptions: Set<String> = emptySet(),
    val protectedApps: Set<String> = emptySet(),
    val protectIndividualCloses: Boolean = true,
    val allowDismissProtected: Boolean = true,
    val protectionFeedback: ProtectionFeedback = ProtectionFeedback.ANIMATION_AND_HINT,
    val excludeHiddenFromKillAll: Boolean = true,
    val confirmKillAll: Boolean = true,
    val bulkConfirmation: BulkConfirmation = BulkConfirmation.ALWAYS,
    val accent: AccentColor = AccentColor.GREEN,
    val customAccent: Int? = null,
    val bgDim: Int = 80,
    val bgTint: BgTint = BgTint.BLACK,
    val forceStopOnKill: Boolean = true,
    val showHeader: Boolean = true,
    val headerMode: HeaderMode = HeaderMode.legacy(showHeader),
    val showSettingsButton: Boolean = true,
    val showViewSwitch: Boolean = false, // Legacy boolean migration only.
    val viewSwitchMode: ViewSwitchMode = ViewSwitchMode.BUTTON,
    val rememberView: Boolean = true,
    val viewButtons: ViewButtons = ViewSwitchPolicy.legacyButtons(viewSwitchMode),
    val edgeAction: EdgeAction = EdgeAction.SWITCH_VIEW,
    val edgeWithoutAccess: EdgeAction = EdgeActionPolicy.legacyFallback(edgeAction),
    val confirmRestoreDismissed: Boolean = true,
    val contextRestoreMode: ContextRestoreMode = ContextRestoreMode.ADAPTIVE,
    val restoreInTop: Boolean = false, // Legacy migration.
    val restoreTopMode: RestoreTopMode = if (restoreInTop) RestoreTopMode.ALL_VIEWS else RestoreTopMode.RECENT_ONLY,
    val hideTopRestore: Boolean = false,
    val showEdgeHints: Boolean = true,
    val emptyListRecovery: EmptyListRecovery = EmptyListRecovery.MENU,
    val edgeMenuItems: Set<EdgeMenuItem> = EdgeMenuPolicy.defaults,
    val edgeShortcut: EdgeShortcut = ViewSwitchPolicy.legacyShortcut(viewSwitchMode),
    val rowSwitchPosition: RowSwitchPosition = RowSwitchPosition.START,
    val showViewNotice: Boolean = false, // Legacy boolean migration.
    val viewNoticeStyle: ViewNoticeStyle = ViewNoticeStyle.legacy(showViewNotice),
    val showHelp: Boolean = true,
    val showAppNames: Boolean = true,
    val appTextPosition: AppTextPosition = AppTextPosition.UNDER_APP,
    val appDetailsMode: AppDetailsMode = AppDetailsMode.PRIVILEGED_ONLY,
    val closeHintStyle: CloseHintStyle = CloseHintStyle.MINIMAL,
    val smoothMotion: Boolean = true,
    val closeAnimationSpeed: CloseAnimationSpeed = CloseAnimationSpeed.NORMAL,
    val animationSpeed: AnimationSpeed = AnimationSpeed.legacy(closeAnimationSpeed),
    val usePrivileged: Boolean = true,
    val listMode: AppListMode = AppListMode.ANDROID_TASKS,
    val downAction: DownAction = DownAction.AUTOMATIC,
    val bulkAction: BulkAction = BulkAction.CLOSE_TASKS, // Legacy migration only.
    val bulkWithAccess: BulkWithAccess = ActionPolicy.legacyBulkWithAccess(bulkAction, killAllLocation == KillAllLocation.HIDDEN),
    val bulkWithoutAccess: BulkWithoutAccess = ActionPolicy.legacyBulkWithoutAccess(bulkAction, killAllLocation == KillAllLocation.HIDDEN),
    val enableDismiss: Boolean = false,
    val dismissScope: DismissScope = DismissScope.RECENT_ONLY,
    val hideTopBulk: Boolean = false,
    val hideTopSwitch: Boolean = false,
    val dismissFallback: Boolean = false, // Legacy migration only.
    val downWithAccess: DownWithAccess = ActionPolicy.legacyWithAccess(downAction),
    val downWithoutAccess: DownWithoutAccess = ActionPolicy.legacyWithoutAccess(downAction, dismissFallback),
    val showTaskIndicator: Boolean = false,
    val dismissedApps: Map<String, Long> = emptyMap(),
    val autoCheckUpdates: Boolean = true,
    val showUpdateNotification: Boolean = true,
) {
    val accentColor get() = customAccent?.let { androidx.compose.ui.graphics.Color(it) } ?: accent.color
}

class SettingsStore(context: Context) {
    private val dataStore = context.dataStore
    private val defaults = RecentsSettings()
    private object Keys {
        // Register every primitive preference at declaration time. Future keys automatically
        // participate in profiles without per-version migrations.
        val profileTypes = linkedMapOf<String, String>()
        val profileEnums = java.util.concurrent.ConcurrentHashMap<String, Set<String>>()
        private fun stringPreferencesKey(name: String) = androidx.datastore.preferences.core.stringPreferencesKey(name).also { profileTypes[name] = "string" }
        private fun booleanPreferencesKey(name: String) = androidx.datastore.preferences.core.booleanPreferencesKey(name).also { profileTypes[name] = "boolean" }
        private fun stringSetPreferencesKey(name: String) = androidx.datastore.preferences.core.stringSetPreferencesKey(name).also { if (name == "edge_menu_items") profileTypes[name] = "strings" }
        private fun intPreferencesKey(name: String) = androidx.datastore.preferences.core.intPreferencesKey(name).also { profileTypes[name] = "int" }
        val savedProfiles = stringPreferencesKey("saved_profiles")
        val profileUndo = stringPreferencesKey("profile_undo")
        val protectionFeedback = stringPreferencesKey("protection_feedback")
        val legacyLayout = stringPreferencesKey("carousel_layout")
        val position = stringPreferencesKey("focus_position")
        val direction = stringPreferencesKey("app_direction")
        val showKillAll = booleanPreferencesKey("show_kill_all")
        val killAllLocation = stringPreferencesKey("kill_all_location")
        val appDetailsMode = stringPreferencesKey("app_details_mode")
        val closeHintStyle = stringPreferencesKey("close_hint_style")
        val animationSpeed = stringPreferencesKey("animation_speed")
        val closeAnimationSpeed = stringPreferencesKey("close_animation_speed")
        val size = stringPreferencesKey("size")
        val shape = stringPreferencesKey("shape")
        val cardPadding = stringPreferencesKey("card_padding")
        val exceptions = stringSetPreferencesKey("exceptions")
        val protectedApps = stringSetPreferencesKey("protected_apps")
        val protectIndividualCloses = booleanPreferencesKey("protect_individual_closes")
        val allowDismissProtected = booleanPreferencesKey("allow_dismiss_protected")
        val excludeHiddenFromKillAll = booleanPreferencesKey("exclude_hidden_from_kill_all")
        val confirmKillAll = booleanPreferencesKey("confirm_kill_all")
        val bulkConfirmation = stringPreferencesKey("bulk_confirmation")
        val accent = stringPreferencesKey("accent")
        val customAccent = intPreferencesKey("custom_accent_argb")
        val bgDim = intPreferencesKey("bg_dim_pct")
        val bgTint = stringPreferencesKey("bg_tint")
        val forceStop = booleanPreferencesKey("force_stop_on_kill")
        val showHeader = booleanPreferencesKey("show_header")
        val headerMode = stringPreferencesKey("header_mode")
        val showSettingsButton = booleanPreferencesKey("show_settings_button")
        val showViewSwitch = booleanPreferencesKey("show_view_switch")
        val viewSwitchMode = stringPreferencesKey("view_switch_mode")
        val rememberView = booleanPreferencesKey("remember_view")
        val viewButtons = stringPreferencesKey("view_buttons")
        val edgeShortcut = stringPreferencesKey("edge_shortcut")
        val restoreTopMode = stringPreferencesKey("restore_top_mode")
        val restoreInTop = booleanPreferencesKey("restore_in_top")
        val hideTopRestore = booleanPreferencesKey("hide_top_restore")
        val showEdgeHints = booleanPreferencesKey("show_edge_hints")
        val emptyListRecovery = stringPreferencesKey("empty_list_recovery")
        val edgeWithoutAccess = stringPreferencesKey("edge_without_access")
        val contextRestoreMode = stringPreferencesKey("context_restore_mode")
        val confirmRestoreDismissed = booleanPreferencesKey("confirm_restore_dismissed")
        val edgeAction = stringPreferencesKey("edge_action")
        val restoreMenuConfigured = booleanPreferencesKey("restore_menu_configured")
        val edgeMenuItems = stringSetPreferencesKey("edge_menu_items")
        val rowSwitchPosition = stringPreferencesKey("row_switch_position")
        val showViewNotice = booleanPreferencesKey("show_view_notice")
        val viewNoticeStyle = stringPreferencesKey("view_notice_style")
        val showHelp = booleanPreferencesKey("show_help")
        val showAppNames = booleanPreferencesKey("show_app_names")
        val appTextPosition = stringPreferencesKey("app_text_position")
        val showAppDetails = booleanPreferencesKey("show_app_details")
        val smooth = booleanPreferencesKey("smooth_motion")
        val usePrivileged = booleanPreferencesKey("use_privileged")
        val listMode = stringPreferencesKey("app_list_mode")
        val downAction = stringPreferencesKey("down_action")
        val downWithAccess = stringPreferencesKey("down_with_access")
        val downWithoutAccess = stringPreferencesKey("down_without_access")
        val bulkAction = stringPreferencesKey("bulk_action")
        val bulkWithAccess = stringPreferencesKey("bulk_with_access")
        val bulkWithoutAccess = stringPreferencesKey("bulk_without_access")
        val enableDismiss = booleanPreferencesKey("enable_dismiss")
        val dismissFallback = booleanPreferencesKey("dismiss_fallback")
        val dismissScope = stringPreferencesKey("dismiss_scope")
        val hideTopBulk = booleanPreferencesKey("hide_top_bulk")
        val hideTopSwitch = booleanPreferencesKey("hide_top_switch")
        val showTaskIndicator = booleanPreferencesKey("show_task_indicator")
        val dismissedApps = stringSetPreferencesKey("dismissed_apps")
        val autoCheckUpdates = booleanPreferencesKey("auto_check_updates")
        val showUpdateNotification = booleanPreferencesKey("show_update_notification")
        val appearanceRestore = stringPreferencesKey("appearance_before_presets")
    }
    // Save raw appearance keys, including absent values and legacy fallbacks, so
    // Restore includes action choices changed by the Default preset.
    private val appearanceStrings = listOf(Keys.legacyLayout, Keys.position, Keys.direction,
        Keys.emptyListRecovery, Keys.restoreTopMode, Keys.bulkWithAccess, Keys.killAllLocation, Keys.viewSwitchMode, Keys.headerMode, Keys.edgeShortcut, Keys.viewButtons, Keys.rowSwitchPosition, Keys.viewNoticeStyle, Keys.appTextPosition, Keys.appDetailsMode, Keys.closeHintStyle, Keys.animationSpeed, Keys.closeAnimationSpeed, Keys.size,
        Keys.shape, Keys.cardPadding, Keys.accent, Keys.bgTint, Keys.edgeAction, Keys.edgeWithoutAccess, Keys.downWithAccess, Keys.downWithoutAccess, Keys.dismissScope)
    private val appearanceBooleans = listOf(Keys.showKillAll, Keys.showHeader,
        Keys.rememberView, Keys.restoreInTop, Keys.hideTopRestore, Keys.showEdgeHints, Keys.enableDismiss, Keys.showTaskIndicator, Keys.hideTopBulk, Keys.hideTopSwitch, Keys.showSettingsButton, Keys.showViewSwitch, Keys.showViewNotice, Keys.showHelp, Keys.showAppNames, Keys.showAppDetails, Keys.smooth)
    private val appearanceInts = listOf(Keys.customAccent, Keys.bgDim)
    private fun snapshot(p: Preferences): String = org.json.JSONObject().apply {
        appearanceStrings.forEach { key -> p[key]?.let { put(key.name, it) } }
        appearanceBooleans.forEach { key -> p[key]?.let { put(key.name, it) } }
        appearanceInts.forEach { key -> p[key]?.let { put(key.name, it) } }
    }.toString()
    private fun readSnapshot(raw: String?): org.json.JSONObject? = runCatching {
        require(raw != null)
        org.json.JSONObject(raw).also { json ->
            val known = (appearanceStrings + appearanceBooleans + appearanceInts).map { it.name }.toSet()
            require(json.keys().asSequence().all { it in known })
            appearanceStrings.forEach { require(!json.has(it.name) || json.get(it.name) is String) }
            appearanceBooleans.forEach { require(!json.has(it.name) || json.get(it.name) is Boolean) }
            appearanceInts.forEach { require(!json.has(it.name) || json.get(it.name) is Int) }
        }
    }.getOrNull()
    private fun profileSchema() = Keys.profileTypes.filterKeys { it !in setOf("saved_profiles", "profile_undo", "appearance_before_presets") }
    private fun profileSnapshot(p: Preferences) = org.json.JSONObject().apply {
        val values = p.asMap().mapKeys { it.key.name }
        profileSchema().forEach { (name, _) -> put(name, when (val value = values[name]) { is Set<*> -> org.json.JSONArray(value.toList()); null -> org.json.JSONObject.NULL; else -> value }) }
    }
    private fun applyProfileValues(p: MutablePreferences, json: org.json.JSONObject) {
        profileSchema().forEach { (name, type) ->
            if (json.has(name)) {
                val value = json.opt(name)
                when (type) {
                    "strings" -> { val key = stringSetPreferencesKey(name); if (value == org.json.JSONObject.NULL) p.remove(key) else { val list = value as org.json.JSONArray; p[key] = (0 until list.length()).map { list.getString(it) }.toSet() } }
                    "boolean" -> { val key = booleanPreferencesKey(name); if (value == org.json.JSONObject.NULL) p.remove(key) else p[key] = value as Boolean }
                    "int" -> { val key = intPreferencesKey(name); if (value == org.json.JSONObject.NULL) p.remove(key) else p[key] = (value as Number).toInt() }
                    "string" -> { val key = stringPreferencesKey(name); if (value == org.json.JSONObject.NULL) p.remove(key) else p[key] = value as String }
                }
            }
        }
    }
    suspend fun exportProfile(name: String): String = ProfileCodec.document(name, profileSnapshot(dataStore.data.first())).toString(2)
    fun inspectProfile(raw: String): ProfileImport {
        decode(emptyPreferences()) // Discover enum options from the same readers used by normal settings.
        val report = ProfileCodec.inspect(raw, profileSchema())
        var skipped = report.skipped
        val explanations = mutableMapOf<String, String>()
        report.settings.keys().asSequence().toList().forEach { name ->
            val options = Keys.profileEnums[name]
            if (options != null && !report.settings.isNull(name)) {
                val value = report.settings.opt(name)
                if (value is org.json.JSONArray) {
                    val known = org.json.JSONArray()
                    val unknown = mutableListOf<String>()
                    for (i in 0 until value.length()) if (value.getString(i) in options) known.put(value.getString(i)) else { skipped++; unknown += value.getString(i) }
                    if (unknown.isNotEmpty()) explanations[name] = "Unknown choices skipped: ${unknown.joinToString()}"
                    if (value.length() > 0 && known.length() == 0) report.settings.remove(name) else report.settings.put(name, known)
                } else if (value !in options) { report.settings.remove(name); skipped++; explanations[name] = "Unknown choice; current setting kept" }
            }
        }
        return report.copy(recognized = report.settings.length(), skipped = skipped, entries = report.entries.map { entry -> explanations[entry.key]?.let { reason -> entry.copy(status = if (report.settings.has(entry.key)) "Partial" else "Skip", reason = reason) } ?: entry })
    }
    suspend fun previewProfile(raw: String): List<PresetChange> {
        val report = inspectProfile(raw)
        val before = dataStore.data.first()
        val candidate = before.toMutablePreferences()
        applyProfileValues(candidate, report.settings)
        return PresetChanges.between(decode(before), decode(candidate))
    }
    suspend fun reviewProfileSettings(raw: String): ProfileReviewSnapshot {
        val report = inspectProfile(raw)
        val before = dataStore.data.first()
        val candidate = before.toMutablePreferences()
        applyProfileValues(candidate, report.settings)
        val current = decode(before)
        val target = decode(candidate)
        val original = profileSnapshot(before)
        val accepted = report.settings.keys().asSequence().toSet()
        val effects = accepted.associateWith { key ->
            val without = candidate.toMutablePreferences()
            applyProfileValues(without, org.json.JSONObject().put(key, original.opt(key) ?: org.json.JSONObject.NULL))
            PresetChanges.between(decode(without), target)
        }
        val rows = ProfileSettingsReview.build(report.entries, accepted, current, target,
            before.asMap().mapKeys { it.key.name }, candidate.asMap().mapKeys { it.key.name }, effects)
        return ProfileReviewSnapshot(report, PresetChanges.between(current, target), rows)
    }
    suspend fun applyProfile(raw: String) {
        val report = inspectProfile(raw)
        require(report.recognized > 0) { "No compatible settings" }
        dataStore.edit { p -> p[Keys.profileUndo] = profileSnapshot(p).toString(); applyProfileValues(p, report.settings) }
    }
    suspend fun undoProfile(): Boolean {
        var restored = false
        dataStore.edit { p -> p[Keys.profileUndo]?.let { raw -> applyProfileValues(p, org.json.JSONObject(raw)); p.remove(Keys.profileUndo); restored = true } }
        return restored
    }
    val savedProfiles = dataStore.data.map { org.json.JSONArray(it[Keys.savedProfiles] ?: "[]") }
    suspend fun saveProfile(name: String, raw: String? = null, replaceId: String? = null) {
        val cleanName = name.trim().take(40); require(cleanName.isNotEmpty())
        val document = if (raw == null) ProfileCodec.document(cleanName, profileSnapshot(dataStore.data.first())) else org.json.JSONObject(raw).put("name", cleanName)
        inspectProfile(document.toString())
        dataStore.edit { p ->
            val old = org.json.JSONArray(p[Keys.savedProfiles] ?: "[]"); val next = org.json.JSONArray()
            for (i in 0 until old.length()) if (old.getJSONObject(i).optString("id") != replaceId) next.put(old.getJSONObject(i))
            require(next.length() < 30) { "Maximum 30 saved profiles" }
            next.put(org.json.JSONObject().put("id", replaceId ?: java.util.UUID.randomUUID().toString()).put("document", document))
            p[Keys.savedProfiles] = next.toString()
        }
    }
    suspend fun deleteProfile(id: String) = dataStore.edit { p ->
        val old = org.json.JSONArray(p[Keys.savedProfiles] ?: "[]"); val next = org.json.JSONArray()
        for (i in 0 until old.length()) if (old.getJSONObject(i).optString("id") != id) next.put(old.getJSONObject(i))
        p[Keys.savedProfiles] = next.toString()
    }
    val canRestoreAppearance = dataStore.data.map { readPresetUndo(it[Keys.appearanceRestore]) != null || readSnapshot(it[Keys.appearanceRestore]) != null }
    private inline fun <reified T : Enum<T>> enumSetPreference(p: Preferences, key: Preferences.Key<Set<String>>): Set<T>? {
        if (!Keys.profileEnums.containsKey(key.name)) Keys.profileEnums.putIfAbsent(key.name, enumValues<T>().mapTo(HashSet()) { it.name })
        return p[key]?.mapNotNull { runCatching { enumValueOf<T>(it) }.getOrNull() }?.toSet()
    }
    private inline fun <reified T : Enum<T>> enumPreference(p: Preferences, key: Preferences.Key<String>, fallback: T): T {
        if (!Keys.profileEnums.containsKey(key.name)) Keys.profileEnums.putIfAbsent(key.name, enumValues<T>().mapTo(HashSet()) { it.name })
        return enumValue(p[key], fallback)
    }
    private inline fun <reified T : Enum<T>> enumValue(value: String?, fallback: T): T =
        runCatching { enumValueOf<T>(value ?: fallback.name) }.getOrDefault(fallback)

    val settings = dataStore.data.map(::decode)
    private fun decode(p: Preferences): RecentsSettings {
        val legacy = p[Keys.legacyLayout]
        val legacyDetails = when (p[Keys.showAppDetails]) {
            true -> AppDetailsMode.ALWAYS
            false -> AppDetailsMode.OFF
            null -> defaults.appDetailsMode
        }
        return defaults.copy(
            position = enumPreference(p, Keys.position, if (legacy == "CLASSIC") FocusPosition.SIDE else defaults.position),
            direction = enumPreference(p, Keys.direction, if (legacy == "CENTERED") AppDirection.OLDER_RIGHT else defaults.direction),
            killAllLocation = enumPreference(p, Keys.killAllLocation, if (p[Keys.showKillAll] == false) KillAllLocation.HIDDEN else defaults.killAllLocation),
            size = enumPreference(p, Keys.size, defaults.size),
            shape = enumPreference(p, Keys.shape, defaults.shape),
            cardPadding = enumPreference(p, Keys.cardPadding, defaults.cardPadding),
            exceptions = p[Keys.exceptions] ?: defaults.exceptions,
            protectedApps = p[Keys.protectedApps] ?: defaults.protectedApps,
            protectIndividualCloses = p[Keys.protectIndividualCloses] ?: defaults.protectIndividualCloses,
            allowDismissProtected = p[Keys.allowDismissProtected] ?: defaults.allowDismissProtected,
            protectionFeedback = enumPreference(p, Keys.protectionFeedback, defaults.protectionFeedback),
            excludeHiddenFromKillAll = p[Keys.excludeHiddenFromKillAll] ?: defaults.excludeHiddenFromKillAll,
            confirmKillAll = enumPreference(p, Keys.bulkConfirmation, if (p[Keys.confirmKillAll] == false) BulkConfirmation.OFF else BulkConfirmation.ALWAYS) != BulkConfirmation.OFF,
            bulkConfirmation = enumPreference(p, Keys.bulkConfirmation, if (p[Keys.confirmKillAll] == false) BulkConfirmation.OFF else BulkConfirmation.ALWAYS),
            accent = enumPreference(p, Keys.accent, defaults.accent),
            customAccent = p[Keys.customAccent] ?: if (p[Keys.accent] == "ICE_CYAN") 0xFFB3FFFF.toInt() else null,
            bgDim = (p[Keys.bgDim] ?: defaults.bgDim).coerceIn(0, 100),
            bgTint = enumPreference(p, Keys.bgTint, defaults.bgTint),
            forceStopOnKill = p[Keys.forceStop] ?: defaults.forceStopOnKill,
            showHeader = p[Keys.showHeader] ?: defaults.showHeader,
            headerMode = enumPreference(p, Keys.headerMode, HeaderMode.legacy(p[Keys.showHeader] ?: defaults.showHeader)),
            showSettingsButton = p[Keys.showSettingsButton] ?: defaults.showSettingsButton,
            showViewSwitch = p[Keys.showViewSwitch] ?: defaults.showViewSwitch,
            viewSwitchMode = enumPreference(p, Keys.viewSwitchMode, when (p[Keys.showViewSwitch]) { false -> ViewSwitchMode.HIDDEN; true -> ViewSwitchMode.BUTTON; null -> defaults.viewSwitchMode }),
            rememberView = p[Keys.rememberView] ?: defaults.rememberView,
            viewButtons = enumPreference(p, Keys.viewButtons, ViewSwitchPolicy.legacyButtons(enumPreference(p, Keys.viewSwitchMode, when (p[Keys.showViewSwitch]) { false -> ViewSwitchMode.HIDDEN; else -> ViewSwitchMode.BUTTON }))),
            edgeAction = enumPreference(p, Keys.edgeAction, EdgeAction.SWITCH_VIEW),
            edgeWithoutAccess = enumPreference(p, Keys.edgeWithoutAccess, EdgeActionPolicy.legacyFallback(enumPreference(p, Keys.edgeAction, EdgeAction.SWITCH_VIEW))),
            confirmRestoreDismissed = p[Keys.confirmRestoreDismissed] ?: true,
            contextRestoreMode = enumPreference(p, Keys.contextRestoreMode, ContextRestoreMode.ADAPTIVE),
            restoreInTop = p[Keys.restoreInTop] ?: false,
            restoreTopMode = enumPreference(p, Keys.restoreTopMode, when (p[Keys.restoreInTop]) { true -> RestoreTopMode.ALL_VIEWS; false -> RestoreTopMode.OFF; null -> defaults.restoreTopMode }),
            hideTopRestore = p[Keys.hideTopRestore] ?: false,
            showEdgeHints = p[Keys.showEdgeHints] ?: true,
            emptyListRecovery = enumPreference(p, Keys.emptyListRecovery, defaults.emptyListRecovery),
            edgeMenuItems = EdgeMenuPolicy.selection(enumSetPreference<EdgeMenuItem>(p, Keys.edgeMenuItems), p[Keys.restoreMenuConfigured] ?: false),
            edgeShortcut = enumPreference(p, Keys.edgeShortcut, ViewSwitchPolicy.legacyShortcut(enumPreference(p, Keys.viewSwitchMode, ViewSwitchMode.BUTTON))),
            rowSwitchPosition = enumPreference(p, Keys.rowSwitchPosition, RowSwitchPosition.START),
            showViewNotice = p[Keys.showViewNotice] ?: false,
            viewNoticeStyle = enumPreference(p, Keys.viewNoticeStyle, ViewNoticeStyle.legacy(p[Keys.showViewNotice] ?: false)),
            showHelp = p[Keys.showHelp] ?: defaults.showHelp,
            showAppNames = p[Keys.showAppNames] ?: defaults.showAppNames,
            appTextPosition = enumPreference(p, Keys.appTextPosition, defaults.appTextPosition),
            appDetailsMode = enumPreference(p, Keys.appDetailsMode, legacyDetails),
            closeHintStyle = enumPreference(p, Keys.closeHintStyle, defaults.closeHintStyle),
            smoothMotion = p[Keys.smooth] ?: defaults.smoothMotion,
            closeAnimationSpeed = enumPreference(p, Keys.closeAnimationSpeed, defaults.closeAnimationSpeed),
            animationSpeed = enumPreference(p, Keys.animationSpeed, AnimationSpeed.legacy(enumPreference(p, Keys.closeAnimationSpeed, defaults.closeAnimationSpeed))),
            usePrivileged = p[Keys.usePrivileged] ?: defaults.usePrivileged,
            listMode = ListSourcePolicy.canonical(enumPreference(p, Keys.listMode, defaults.listMode)),
            downAction = ActionPolicy.migrateDown(p[Keys.downAction]),
            downWithAccess = enumPreference(p, Keys.downWithAccess, ActionPolicy.legacyWithAccess(ActionPolicy.migrateDown(p[Keys.downAction]))),
            downWithoutAccess = enumPreference(p, Keys.downWithoutAccess, ActionPolicy.legacyWithoutAccess(ActionPolicy.migrateDown(p[Keys.downAction]), p[Keys.dismissFallback] ?: ActionPolicy.legacyFallback(p[Keys.downAction]))),
            bulkAction = enumPreference(p, Keys.bulkAction, defaults.bulkAction),
            bulkWithAccess = if (p[Keys.killAllLocation] == "HIDDEN" || (p[Keys.killAllLocation] == null && p[Keys.showKillAll] == false)) BulkWithAccess.HIDDEN else
                enumPreference(p, Keys.bulkWithAccess, ActionPolicy.legacyBulkWithAccess(enumPreference(p, Keys.bulkAction, defaults.bulkAction))),
            bulkWithoutAccess = if (p[Keys.killAllLocation] == "HIDDEN" || (p[Keys.killAllLocation] == null && p[Keys.showKillAll] == false)) BulkWithoutAccess.HIDDEN else
                enumPreference(p, Keys.bulkWithoutAccess, ActionPolicy.legacyBulkWithoutAccess(enumPreference(p, Keys.bulkAction, defaults.bulkAction))),
            enableDismiss = p[Keys.enableDismiss] ?: false,
            dismissFallback = p[Keys.dismissFallback] ?: ActionPolicy.legacyFallback(p[Keys.downAction]),
            showTaskIndicator = p[Keys.showTaskIndicator] ?: false,
            dismissScope = enumPreference(p, Keys.dismissScope, DismissScope.RECENT_ONLY),
            hideTopBulk = p[Keys.hideTopBulk] ?: false,
            hideTopSwitch = p[Keys.hideTopSwitch] ?: false,
            dismissedApps = DismissPolicy.decode(p[Keys.dismissedApps] ?: emptySet()),
            autoCheckUpdates = p[Keys.autoCheckUpdates] ?: defaults.autoCheckUpdates,
            showUpdateNotification = p[Keys.showUpdateNotification] ?: defaults.showUpdateNotification,
        )
    }
    // Read/modify/write in one serialized transaction, so fast remote repeats cannot
    // reuse a stale UI snapshot and lose an adjustment or a toggle.
    private suspend fun <E : Enum<E>> step(key: Preferences.Key<String>, dir: Int, options: List<E>, current: (RecentsSettings) -> E) =
        dataStore.edit { p ->
            val index = current(decode(p)).ordinal
            p[key] = options[(index + dir).coerceIn(0, options.lastIndex)].name
        }
    private suspend fun toggle(key: Preferences.Key<Boolean>, current: (RecentsSettings) -> Boolean) =
        dataStore.edit { p -> p[key] = !current(decode(p)) }
    suspend fun adjustPosition(dir: Int) = step(Keys.position, dir, FocusPosition.entries) { it.position }
    suspend fun adjustDirection(dir: Int) = step(Keys.direction, dir, AppDirection.entries) { it.direction }
    suspend fun adjustSize(dir: Int) = step(Keys.size, dir, CardSizeOpt.entries) { it.size }
    suspend fun adjustShape(dir: Int) = step(Keys.shape, dir, CardShapeOpt.entries) { it.shape }
    suspend fun adjustCardPadding(dir: Int) = step(Keys.cardPadding, dir, CardPadding.entries) { it.cardPadding }
    suspend fun adjustAccent(dir: Int) = dataStore.edit { p ->
        p[Keys.accent] = AccentColor.entries[(decode(p).accent.ordinal + dir).coerceIn(0, AccentColor.entries.lastIndex)].name
        p.remove(Keys.customAccent)
    }
    suspend fun setCustomAccent(argb: Int) = dataStore.edit { it[Keys.customAccent] = argb }
    suspend fun adjustBgTint(dir: Int) = step(Keys.bgTint, dir, BgTint.entries) { it.bgTint }
    suspend fun adjustBgDim(dir: Int) = dataStore.edit { p -> p[Keys.bgDim] = (decode(p).bgDim + dir * 5).coerceIn(0, 100) }
    // Hidden location remains readable for old installs and appearance preset snapshots only.
    // Editing a bulk control materializes the displayed rules before clearing that legacy mask.
    private fun materializeBulk(p: MutablePreferences): RecentsSettings {
        val current = decode(p)
        p[Keys.bulkWithAccess] = current.bulkWithAccess.name
        p[Keys.bulkWithoutAccess] = current.bulkWithoutAccess.name
        if (current.killAllLocation == KillAllLocation.HIDDEN) p[Keys.killAllLocation] = KillAllLocation.APP_ROW.name
        p.remove(Keys.showKillAll)
        return current
    }
    suspend fun adjustKillAllLocation(dir: Int) = dataStore.edit {
        val current = materializeBulk(it)
        val options = listOf(KillAllLocation.APP_ROW, KillAllLocation.TOP_CONTROLS, KillAllLocation.BOTH)
        it[Keys.killAllLocation] = options[(options.indexOf(current.killAllLocation).coerceAtLeast(0) + dir).coerceIn(0, options.lastIndex)].name
    }
    suspend fun adjustBulkWithAccess(dir: Int) = dataStore.edit {
        val current = materializeBulk(it)
        val options = BulkWithAccess.values()
        it[Keys.bulkWithAccess] = options[(current.bulkWithAccess.ordinal + dir).coerceIn(0, options.lastIndex)].name
    }
    suspend fun adjustBulkWithoutAccess(dir: Int) = dataStore.edit {
        val current = materializeBulk(it)
        val options = BulkWithoutAccess.values()
        it[Keys.bulkWithoutAccess] = options[(current.bulkWithoutAccess.ordinal + dir).coerceIn(0, options.lastIndex)].name
    }
    suspend fun adjustAppTextPosition(dir: Int) = step(Keys.appTextPosition, dir, AppTextPosition.entries) { it.appTextPosition }
    suspend fun adjustAppDetailsMode(dir: Int) = step(Keys.appDetailsMode, dir, AppDetailsMode.entries) { it.appDetailsMode }
    suspend fun adjustAnimationSpeed(dir: Int) = step(Keys.animationSpeed, dir, AnimationSpeed.entries) { it.animationSpeed }
    suspend fun adjustCloseAnimationSpeed(dir: Int) = step(Keys.closeAnimationSpeed, dir, CloseAnimationSpeed.entries) { it.closeAnimationSpeed }
    suspend fun adjustCloseHintStyle(dir: Int) = step(Keys.closeHintStyle, dir, CloseHintStyle.entries) { it.closeHintStyle }
    suspend fun toggleForceStop() = toggle(Keys.forceStop) { it.forceStopOnKill }
    suspend fun toggleHiddenKillExclusion() = toggle(Keys.excludeHiddenFromKillAll) { it.excludeHiddenFromKillAll }
    suspend fun setHiddenKillExclusion(v: Boolean) = dataStore.edit { it[Keys.excludeHiddenFromKillAll] = v }
    suspend fun adjustProtectionFeedback(dir: Int) = step(Keys.protectionFeedback, dir, ProtectionFeedback.entries) { it.protectionFeedback }
    suspend fun setAllowDismissProtected(v: Boolean) = dataStore.edit { it[Keys.allowDismissProtected] = v }
    suspend fun toggleAllowDismissProtected() = toggle(Keys.allowDismissProtected) { it.allowDismissProtected }
    suspend fun toggleIndividualProtection() = toggle(Keys.protectIndividualCloses) { it.protectIndividualCloses }
    suspend fun toggleKillAllConfirmation() = dataStore.edit {
        val on = !decode(it).confirmKillAll
        it[Keys.confirmKillAll] = on
        it[Keys.bulkConfirmation] = if (on) BulkConfirmation.ALWAYS.name else BulkConfirmation.OFF.name
    }
    suspend fun setIndividualProtection(v: Boolean) = dataStore.edit { it[Keys.protectIndividualCloses] = v }
    suspend fun setKillAllConfirmation(v: Boolean) = dataStore.edit {
        it[Keys.confirmKillAll] = v
        it[Keys.bulkConfirmation] = if (v) BulkConfirmation.ALWAYS.name else BulkConfirmation.OFF.name
    }
    suspend fun adjustBulkConfirmation(dir: Int) = step(Keys.bulkConfirmation, dir, BulkConfirmation.entries) { it.bulkConfirmation }
    suspend fun adjustHeaderMode(dir: Int) = step(Keys.headerMode, dir, HeaderMode.entries) { it.headerMode }
    suspend fun adjustEdgeWithoutAccess(dir: Int) = dataStore.edit {
        val choices = EdgeActionPolicy.fallbackChoices
        it[Keys.edgeWithoutAccess] = choices[(choices.indexOf(decode(it).edgeWithoutAccess).coerceAtLeast(0) + dir).coerceIn(0, choices.lastIndex)].name
    }
    suspend fun adjustContextRestore(dir: Int) = step(Keys.contextRestoreMode, dir, ContextRestoreMode.entries) { it.contextRestoreMode }
    suspend fun setRestoreConfirmation(v: Boolean) = dataStore.edit { it[Keys.confirmRestoreDismissed] = v }
    suspend fun toggleRestoreConfirmation() = toggle(Keys.confirmRestoreDismissed) { it.confirmRestoreDismissed }
    suspend fun adjustDismissal(dir: Int) = dataStore.edit {
        val current = decode(it)
        val index = if (!current.enableDismiss) 0 else current.dismissScope.ordinal + 1
        val next = (index + dir).coerceIn(0, 2)
        it[Keys.enableDismiss] = next != 0
        if (next != 0) it[Keys.dismissScope] = DismissScope.entries[next - 1].name
    }
    suspend fun adjustEdgeAction(dir: Int) = dataStore.edit {
        val current = decode(it)
        if (it[Keys.edgeWithoutAccess] == null) it[Keys.edgeWithoutAccess] = current.edgeWithoutAccess.name
        val choices = EdgeActionPolicy.privilegedChoices
        it[Keys.edgeAction] = choices[(choices.indexOf(current.edgeAction).coerceAtLeast(0) + dir).coerceIn(0, choices.lastIndex)].name
    }
    suspend fun toggleEdgeMenuItem(item: EdgeMenuItem) = dataStore.edit {
        val selected = decode(it).edgeMenuItems
        if (item == EdgeMenuItem.RESTORE_DISMISSED) it[Keys.restoreMenuConfigured] = true
        it[Keys.edgeMenuItems] = (if (item in selected) selected - item else selected + item).map { value -> value.name }.toSet()
    }
    suspend fun setEdgeMenuItem(item: EdgeMenuItem, enabled: Boolean) = dataStore.edit {
        val selected = decode(it).edgeMenuItems
        if (item == EdgeMenuItem.RESTORE_DISMISSED) it[Keys.restoreMenuConfigured] = true
        it[Keys.edgeMenuItems] = (if (enabled) selected + item else selected - item).map { value -> value.name }.toSet()
    }
    suspend fun toggleShowHeader() = toggle(Keys.showHeader) { it.showHeader }
    suspend fun adjustViewButtons(dir: Int) = step(Keys.viewButtons, dir, ViewButtons.entries) { it.viewButtons }
    suspend fun adjustRecovery(dir: Int) = step(Keys.emptyListRecovery, dir, EmptyListRecovery.entries) { it.emptyListRecovery }
    suspend fun adjustRestoreTopMode(dir: Int) = step(Keys.restoreTopMode, dir, RestoreTopMode.entries) { it.restoreTopMode }
    suspend fun toggleRestoreInTop() = toggle(Keys.restoreInTop) { it.restoreInTop }
    suspend fun setHideTopRestore(v: Boolean) = dataStore.edit { it[Keys.hideTopRestore] = v }
    suspend fun toggleHideTopRestore() = toggle(Keys.hideTopRestore) { it.hideTopRestore }
    suspend fun setEdgeHints(v: Boolean) = dataStore.edit { it[Keys.showEdgeHints] = v }
    suspend fun toggleEdgeHints() = toggle(Keys.showEdgeHints) { it.showEdgeHints }
    suspend fun adjustEdgeShortcut(dir: Int) = step(Keys.edgeShortcut, dir, EdgeShortcut.entries) { it.edgeShortcut }
    suspend fun adjustRowSwitchPosition(dir: Int) = step(Keys.rowSwitchPosition, dir, RowSwitchPosition.entries) { it.rowSwitchPosition }
    suspend fun adjustViewNotice(dir: Int) = step(Keys.viewNoticeStyle, dir, ViewNoticeStyle.entries) { it.viewNoticeStyle }
    suspend fun toggleViewNotice() = toggle(Keys.showViewNotice) { it.showViewNotice }
    suspend fun setViewNotice(v: Boolean) = dataStore.edit { it[Keys.showViewNotice] = v }
    suspend fun adjustViewSwitchMode(dir: Int) = step(Keys.viewSwitchMode, dir, ViewSwitchMode.entries) { it.viewSwitchMode }
    suspend fun setRememberView(v: Boolean) = dataStore.edit { it[Keys.rememberView] = v }
    suspend fun toggleRememberView() = toggle(Keys.rememberView) { it.rememberView }
    suspend fun setListMode(mode: AppListMode) = dataStore.edit { it[Keys.listMode] = ListSourcePolicy.canonical(mode).name }
    suspend fun toggleViewSwitch() = toggle(Keys.showViewSwitch) { it.showViewSwitch }
    suspend fun setViewSwitch(v: Boolean) = dataStore.edit { it[Keys.showViewSwitch] = v }
    suspend fun toggleSettingsButton() = toggle(Keys.showSettingsButton) { it.showSettingsButton }
    suspend fun toggleAppNames() = toggle(Keys.showAppNames) { it.showAppNames }
    suspend fun toggleShowHelp() = toggle(Keys.showHelp) { it.showHelp }
    suspend fun toggleSmoothMotion() = toggle(Keys.smooth) { it.smoothMotion }
    suspend fun adjustListMode(dir: Int) = dataStore.edit { it[Keys.listMode] = ListSourcePolicy.adjust(decode(it).listMode, dir).name }
    suspend fun adjustBulkAction(dir: Int) = step(Keys.bulkAction, dir, BulkAction.entries) { it.bulkAction }
    suspend fun adjustDownWithAccess(dir: Int) = dataStore.edit {
        val options = DownWithAccess.values()
        it[Keys.downWithAccess] = options[(decode(it).downWithAccess.ordinal + dir).coerceIn(0, options.lastIndex)].name
    }
    suspend fun adjustDownWithoutAccess(dir: Int) = dataStore.edit {
        val options = DownWithoutAccess.values()
        it[Keys.downWithoutAccess] = options[(decode(it).downWithoutAccess.ordinal + dir).coerceIn(0, options.lastIndex)].name
    }
    suspend fun adjustDownAction(dir: Int) = dataStore.edit {
        val current = decode(it)
        val options = ActionPolicy.downChoices
        if (it[Keys.dismissFallback] == null) it[Keys.dismissFallback] = current.dismissFallback
        it[Keys.downAction] = options[(options.indexOf(current.downAction).coerceAtLeast(0) + dir).coerceIn(0, options.lastIndex)].name
    }
    suspend fun toggleDismissFallback() = toggle(Keys.dismissFallback) { it.dismissFallback }
    suspend fun setDismissFallback(v: Boolean) = dataStore.edit { it[Keys.dismissFallback] = v }
    suspend fun toggleDismiss() = toggle(Keys.enableDismiss) { it.enableDismiss }
    suspend fun setDismiss(v: Boolean) = dataStore.edit { it[Keys.enableDismiss] = v }
    suspend fun adjustDismissScope(dir: Int) = step(Keys.dismissScope, dir, DismissScope.entries) { it.dismissScope }
    suspend fun setHideTopBulk(v: Boolean) = dataStore.edit { it[Keys.hideTopBulk] = v }
    suspend fun setHideTopSwitch(v: Boolean) = dataStore.edit { it[Keys.hideTopSwitch] = v }
    suspend fun toggleHideTopBulk() = toggle(Keys.hideTopBulk) { it.hideTopBulk }
    suspend fun toggleHideTopSwitch() = toggle(Keys.hideTopSwitch) { it.hideTopSwitch }
    suspend fun toggleTaskIndicator() = toggle(Keys.showTaskIndicator) { it.showTaskIndicator }
    suspend fun setTaskIndicator(v: Boolean) = dataStore.edit { it[Keys.showTaskIndicator] = v }
    suspend fun dismissFromList(pkg: String, lastForeground: Long) = dataStore.edit {
        it[Keys.dismissedApps] = DismissPolicy.encode(decode(it).dismissedApps + (pkg to lastForeground))
    }
    suspend fun dismissPackages(packages: Set<String>, foreground: Map<String, Long>) = dataStore.edit {
        it[Keys.dismissedApps] = DismissPolicy.encode(decode(it).dismissedApps + packages.associateWith { pkg -> foreground[pkg] ?: 0L })
    }
    suspend fun restoreDismissed() = dataStore.edit { it.remove(Keys.dismissedApps) }
    suspend fun refreshDismissed(foreground: Map<String, Long>) = dataStore.edit {
        val before = decode(it).dismissedApps
        val after = DismissPolicy.remaining(before, foreground)
        if (before != after) it[Keys.dismissedApps] = DismissPolicy.encode(after)
    }
    suspend fun toggleUsePrivileged() = toggle(Keys.usePrivileged) { it.usePrivileged }
    suspend fun resetToDefaults() = dataStore.edit { p ->
        val profiles = p[Keys.savedProfiles]
        p.clear()
        if (profiles != null) p[Keys.savedProfiles] = profiles
        writePresetDefaults(p)
    }
    private fun writePresetDefaults(p: MutablePreferences) {
        val defaults = AppearancePreset.DEFAULT.appearance()
        p[Keys.rememberView] = defaults.rememberView
        p[Keys.viewButtons] = defaults.viewButtons.name
        p[Keys.edgeShortcut] = defaults.edgeShortcut.name
        p[Keys.edgeAction] = defaults.edgeAction.name
        p[Keys.edgeWithoutAccess] = defaults.edgeWithoutAccess.name
        p[Keys.enableDismiss] = defaults.enableDismiss
        p[Keys.dismissScope] = defaults.dismissScope.name
        p[Keys.downWithAccess] = defaults.downWithAccess.name
        p[Keys.downWithoutAccess] = defaults.downWithoutAccess.name
        p[Keys.showTaskIndicator] = defaults.showTaskIndicator
        p[Keys.restoreTopMode] = defaults.restoreTopMode.name
        p[Keys.hideTopRestore] = defaults.hideTopRestore
    }
    private fun presetKeys(): Set<String> = profileSchema().keys.filterTo(linkedSetOf()) { PresetPolicy.managed(it) }
    private fun presetUndo(p: Preferences) = org.json.JSONObject().put("presetVersion", 2).put("values",
        profileSnapshot(p).let { all -> org.json.JSONObject().apply { presetKeys().forEach { put(it, all.opt(it) ?: org.json.JSONObject.NULL) } } }).toString()
    private fun readPresetUndo(raw: String?): org.json.JSONObject? = runCatching {
        val doc = org.json.JSONObject(requireNotNull(raw)); require(doc.getInt("presetVersion") == 2)
        doc.getJSONObject("values").also { values ->
            require(values.keys().asSequence().all { it in presetKeys() && ProfileCodec.accepts(profileSchema().getValue(it), values.get(it)) })
        }
    }.getOrNull()
    private fun presetCandidate(p: Preferences, preset: AppearancePreset, mode: PresetApplyMode): MutablePreferences {
        val candidate = p.toMutablePreferences()
        if (mode == PresetApplyMode.FULL) {
            applyProfileValues(candidate, org.json.JSONObject().apply { presetKeys().forEach { put(it, org.json.JSONObject.NULL) } })
            writePresetDefaults(candidate)
        }
        writePresetLook(candidate, preset)
        if (mode == PresetApplyMode.APPEARANCE_ONLY) {
            val allowed = (appearanceStrings + appearanceBooleans + appearanceInts).map { it.name }.filter { PresetPolicy.visual(it) }.toSet()
            val original = profileSnapshot(p)
            applyProfileValues(candidate, org.json.JSONObject().apply {
                presetKeys().filter { it !in allowed }.forEach { put(it, original.opt(it) ?: org.json.JSONObject.NULL) }
            })
        }
        return candidate
    }
    suspend fun previewAppearanceModes(preset: AppearancePreset): Map<PresetApplyMode, List<PresetChange>> {
        val before = dataStore.data.first()
        val current = decode(before)
        return PresetApplyMode.entries.associateWith { mode -> PresetChanges.between(current, decode(presetCandidate(before, preset, mode))) }
    }
    suspend fun previewAppearancePreset(preset: AppearancePreset, mode: PresetApplyMode): List<PresetChange> {
        val before = dataStore.data.first()
        return PresetChanges.between(decode(before), decode(presetCandidate(before, preset, mode)))
    }
    suspend fun applyAppearancePreset(preset: AppearancePreset, mode: PresetApplyMode = PresetApplyMode.FULL) = dataStore.edit { p ->
        val undo = presetUndo(p)
        val candidate = presetCandidate(p, preset, mode)
        val values = profileSnapshot(candidate)
        applyProfileValues(p, org.json.JSONObject().apply { presetKeys().forEach { put(it, values.opt(it) ?: org.json.JSONObject.NULL) } })
        p[Keys.appearanceRestore] = undo
    }
    private fun writePresetLook(p: MutablePreferences, preset: AppearancePreset) {
        val look = preset.appearance()
        if (preset == AppearancePreset.DEFAULT) {
            p[Keys.edgeShortcut] = look.edgeShortcut.name
            p[Keys.edgeAction] = look.edgeAction.name
            p[Keys.edgeWithoutAccess] = look.edgeWithoutAccess.name
            p[Keys.enableDismiss] = look.enableDismiss
            p[Keys.dismissScope] = look.dismissScope.name
            p[Keys.downWithAccess] = look.downWithAccess.name
            p[Keys.downWithoutAccess] = look.downWithoutAccess.name
        }
        p[Keys.position] = look.position.name
        p[Keys.direction] = look.direction.name
        p[Keys.size] = look.size.name
        p[Keys.shape] = look.shape.name
        p[Keys.cardPadding] = look.cardPadding.name
        p[Keys.accent] = look.accent.name
        p.remove(Keys.customAccent)
        p[Keys.bgDim] = look.bgDim
        p[Keys.bgTint] = look.bgTint.name
        p[Keys.headerMode] = look.headerMode.name
        p[Keys.edgeShortcut] = look.edgeShortcut.name
        p[Keys.emptyListRecovery] = look.emptyListRecovery.name
        if (preset == AppearancePreset.CLEAN_PLUS) p[Keys.bulkWithAccess] = look.bulkWithAccess.name
        p[Keys.showTaskIndicator] = look.showTaskIndicator
        p[Keys.restoreInTop] = look.restoreInTop
        p[Keys.restoreTopMode] = look.restoreTopMode.name
        p[Keys.hideTopRestore] = look.hideTopRestore
        p[Keys.showEdgeHints] = look.showEdgeHints
        p[Keys.hideTopBulk] = look.hideTopBulk
        p[Keys.hideTopSwitch] = look.hideTopSwitch
        p[Keys.showSettingsButton] = look.showSettingsButton
        p[Keys.rememberView] = look.rememberView
        p[Keys.viewButtons] = look.viewButtons.name
        p[Keys.rowSwitchPosition] = look.rowSwitchPosition.name
        p[Keys.viewNoticeStyle] = look.viewNoticeStyle.name
        p[Keys.showHelp] = look.showHelp
        p[Keys.showAppNames] = look.showAppNames
        p[Keys.appTextPosition] = look.appTextPosition.name
        p[Keys.appDetailsMode] = look.appDetailsMode.name
        p[Keys.closeHintStyle] = look.closeHintStyle.name
        p[Keys.smooth] = look.smoothMotion
        // Animation speed is global and stays selected while comparing presets.
        p[Keys.killAllLocation] = look.killAllLocation.name
        // Service use, force-stop, confirmation and app protection are deliberately untouched.
    }
    suspend fun restoreAppearance(): Boolean {
        var restored = false
        dataStore.edit { p ->
            val complete = readPresetUndo(p[Keys.appearanceRestore])
            val saved = readSnapshot(p[Keys.appearanceRestore])
            if (complete != null) {
                applyProfileValues(p, complete)
                restored = true
            } else if (saved != null) {
                appearanceStrings.forEach { key ->
                    p.remove(key)
                    if (saved.has(key.name)) p[key] = saved.getString(key.name)
                }
                appearanceBooleans.forEach { key ->
                    p.remove(key)
                    if (saved.has(key.name)) p[key] = saved.getBoolean(key.name)
                }
                appearanceInts.forEach { key ->
                    p.remove(key)
                    if (saved.has(key.name)) p[key] = saved.getInt(key.name)
                }
                restored = true
            }
            p.remove(Keys.appearanceRestore)
        }
        return restored
    }
    suspend fun setPosition(v: FocusPosition) = dataStore.edit { it[Keys.position] = v.name }
    suspend fun setDirection(v: AppDirection) = dataStore.edit { it[Keys.direction] = v.name }
    suspend fun setSize(v: CardSizeOpt) = dataStore.edit { it[Keys.size] = v.name }
    suspend fun setShape(v: CardShapeOpt) = dataStore.edit { it[Keys.shape] = v.name }
    suspend fun setCardPadding(v: CardPadding) = dataStore.edit { it[Keys.cardPadding] = v.name }
    suspend fun setAccent(v: AccentColor) = dataStore.edit { it[Keys.accent] = v.name; it.remove(Keys.customAccent) }
    suspend fun setBgDim(v: Int) = dataStore.edit { it[Keys.bgDim] = v.coerceIn(0, 100) }
    suspend fun setBgTint(v: BgTint) = dataStore.edit { it[Keys.bgTint] = v.name }
    suspend fun setForceStop(v: Boolean) = dataStore.edit { it[Keys.forceStop] = v }
    suspend fun setShowHeader(v: Boolean) = dataStore.edit { it[Keys.showHeader] = v }
    suspend fun setShowSettingsButton(v: Boolean) = dataStore.edit { it[Keys.showSettingsButton] = v }
    suspend fun setAppNames(v: Boolean) = dataStore.edit { it[Keys.showAppNames] = v }
    suspend fun setShowHelp(v: Boolean) = dataStore.edit { it[Keys.showHelp] = v }
    suspend fun setSmoothMotion(v: Boolean) = dataStore.edit { it[Keys.smooth] = v }
    suspend fun setUsePrivileged(v: Boolean) = dataStore.edit { it[Keys.usePrivileged] = v }
    suspend fun toggleAutoCheckUpdates() = toggle(Keys.autoCheckUpdates) { it.autoCheckUpdates }
    suspend fun toggleUpdateNotification() = toggle(Keys.showUpdateNotification) { it.showUpdateNotification }
    suspend fun toggleException(pkg: String) = dataStore.edit { p ->
        val cur = (p[Keys.exceptions] ?: emptySet()).toMutableSet()
        if (!cur.remove(pkg)) cur.add(pkg)
        p[Keys.exceptions] = cur
    }
    suspend fun toggleProtectedApp(pkg: String) = dataStore.edit { p ->
        val current = (p[Keys.protectedApps] ?: emptySet()).toMutableSet()
        if (!current.remove(pkg)) current.add(pkg)
        p[Keys.protectedApps] = current
    }
}



