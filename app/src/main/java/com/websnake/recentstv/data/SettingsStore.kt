package com.websnake.recentstv.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.websnake.recentstv.ui.theme.*
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "recents_settings")

enum class FocusPosition { CENTER, SIDE }
enum class AppDirection { OLDER_LEFT, OLDER_RIGHT }
enum class CardPadding(val iconFraction: Float) { NONE(1f), COMPACT(0.84f), NORMAL(0.74f), WIDE(0.66f) }
enum class AppDetailsMode { OFF, ALWAYS, PRIVILEGED_ONLY;
    fun visible(privileged: Boolean) = this == ALWAYS || (this == PRIVILEGED_ONLY && privileged)
}
enum class KillAllLocation { APP_ROW, TOP_CONTROLS, HIDDEN }
enum class AppTextPosition { UNDER_APP, BOTTOM }

enum class CloseHintStyle { ELEVATED, MINIMAL }
enum class CloseAnimationSpeed(val durationMs: Int) { NORMAL(240), SLOW(350) }

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
    val excludeHiddenFromKillAll: Boolean = true,
    val confirmKillAll: Boolean = true,
    val accent: AccentColor = AccentColor.GREEN,
    val customAccent: Int? = null,
    val bgDim: Int = 80,
    val bgTint: BgTint = BgTint.BLACK,
    val forceStopOnKill: Boolean = true,
    val showHeader: Boolean = true,
    val showSettingsButton: Boolean = true,
    val showHelp: Boolean = true,
    val showAppNames: Boolean = true,
    val appTextPosition: AppTextPosition = AppTextPosition.UNDER_APP,
    val appDetailsMode: AppDetailsMode = AppDetailsMode.PRIVILEGED_ONLY,
    val closeHintStyle: CloseHintStyle = CloseHintStyle.MINIMAL,
    val smoothMotion: Boolean = true,
    val closeAnimationSpeed: CloseAnimationSpeed = CloseAnimationSpeed.NORMAL,
    val usePrivileged: Boolean = true,
    val autoCheckUpdates: Boolean = true,
    val showUpdateNotification: Boolean = true,
) {
    val accentColor get() = customAccent?.let { androidx.compose.ui.graphics.Color(it) } ?: accent.color
}

class SettingsStore(context: Context) {
    private val dataStore = context.dataStore
    private val defaults = RecentsSettings()
    private object Keys {
        val legacyLayout = stringPreferencesKey("carousel_layout")
        val position = stringPreferencesKey("focus_position")
        val direction = stringPreferencesKey("app_direction")
        val showKillAll = booleanPreferencesKey("show_kill_all")
        val killAllLocation = stringPreferencesKey("kill_all_location")
        val appDetailsMode = stringPreferencesKey("app_details_mode")
        val closeHintStyle = stringPreferencesKey("close_hint_style")
        val closeAnimationSpeed = stringPreferencesKey("close_animation_speed")
        val size = stringPreferencesKey("size")
        val shape = stringPreferencesKey("shape")
        val cardPadding = stringPreferencesKey("card_padding")
        val exceptions = stringSetPreferencesKey("exceptions")
        val protectedApps = stringSetPreferencesKey("protected_apps")
        val protectIndividualCloses = booleanPreferencesKey("protect_individual_closes")
        val excludeHiddenFromKillAll = booleanPreferencesKey("exclude_hidden_from_kill_all")
        val confirmKillAll = booleanPreferencesKey("confirm_kill_all")
        val accent = stringPreferencesKey("accent")
        val customAccent = intPreferencesKey("custom_accent_argb")
        val bgDim = intPreferencesKey("bg_dim_pct")
        val bgTint = stringPreferencesKey("bg_tint")
        val forceStop = booleanPreferencesKey("force_stop_on_kill")
        val showHeader = booleanPreferencesKey("show_header")
        val showSettingsButton = booleanPreferencesKey("show_settings_button")
        val showHelp = booleanPreferencesKey("show_help")
        val showAppNames = booleanPreferencesKey("show_app_names")
        val appTextPosition = stringPreferencesKey("app_text_position")
        val showAppDetails = booleanPreferencesKey("show_app_details")
        val smooth = booleanPreferencesKey("smooth_motion")
        val usePrivileged = booleanPreferencesKey("use_privileged")
        val autoCheckUpdates = booleanPreferencesKey("auto_check_updates")
        val showUpdateNotification = booleanPreferencesKey("show_update_notification")
        val appearanceRestore = stringPreferencesKey("appearance_before_presets")
    }
    // Save raw appearance keys, including absent values and legacy fallbacks, so
    // Restore reproduces the pre-preset look without touching operational choices.
    private val appearanceStrings = listOf(Keys.legacyLayout, Keys.position, Keys.direction,
        Keys.killAllLocation, Keys.appTextPosition, Keys.appDetailsMode, Keys.closeHintStyle, Keys.closeAnimationSpeed, Keys.size,
        Keys.shape, Keys.cardPadding, Keys.accent, Keys.bgTint)
    private val appearanceBooleans = listOf(Keys.showKillAll, Keys.showHeader,
        Keys.showSettingsButton, Keys.showHelp, Keys.showAppNames, Keys.showAppDetails, Keys.smooth)
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
    val canRestoreAppearance = dataStore.data.map { readSnapshot(it[Keys.appearanceRestore]) != null }
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
            position = enumValue(p[Keys.position], if (legacy == "CLASSIC") FocusPosition.SIDE else defaults.position),
            direction = enumValue(p[Keys.direction], if (legacy == "CENTERED") AppDirection.OLDER_RIGHT else defaults.direction),
            killAllLocation = enumValue(p[Keys.killAllLocation], if (p[Keys.showKillAll] == false) KillAllLocation.HIDDEN else defaults.killAllLocation),
            size = enumValue(p[Keys.size], defaults.size),
            shape = enumValue(p[Keys.shape], defaults.shape),
            cardPadding = enumValue(p[Keys.cardPadding], defaults.cardPadding),
            exceptions = p[Keys.exceptions] ?: defaults.exceptions,
            protectedApps = p[Keys.protectedApps] ?: defaults.protectedApps,
            protectIndividualCloses = p[Keys.protectIndividualCloses] ?: defaults.protectIndividualCloses,
            excludeHiddenFromKillAll = p[Keys.excludeHiddenFromKillAll] ?: defaults.excludeHiddenFromKillAll,
            confirmKillAll = p[Keys.confirmKillAll] ?: defaults.confirmKillAll,
            accent = enumValue(p[Keys.accent], defaults.accent),
            customAccent = p[Keys.customAccent] ?: if (p[Keys.accent] == "ICE_CYAN") 0xFFB3FFFF.toInt() else null,
            bgDim = (p[Keys.bgDim] ?: defaults.bgDim).coerceIn(0, 100),
            bgTint = enumValue(p[Keys.bgTint], defaults.bgTint),
            forceStopOnKill = p[Keys.forceStop] ?: defaults.forceStopOnKill,
            showHeader = p[Keys.showHeader] ?: defaults.showHeader,
            showSettingsButton = p[Keys.showSettingsButton] ?: defaults.showSettingsButton,
            showHelp = p[Keys.showHelp] ?: defaults.showHelp,
            showAppNames = p[Keys.showAppNames] ?: defaults.showAppNames,
            appTextPosition = enumValue(p[Keys.appTextPosition], defaults.appTextPosition),
            appDetailsMode = enumValue(p[Keys.appDetailsMode], legacyDetails),
            closeHintStyle = enumValue(p[Keys.closeHintStyle], defaults.closeHintStyle),
            smoothMotion = p[Keys.smooth] ?: defaults.smoothMotion,
            closeAnimationSpeed = enumValue(p[Keys.closeAnimationSpeed], defaults.closeAnimationSpeed),
            usePrivileged = p[Keys.usePrivileged] ?: defaults.usePrivileged,
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
    suspend fun adjustKillAllLocation(dir: Int) = step(Keys.killAllLocation, dir, KillAllLocation.entries) { it.killAllLocation }
    suspend fun adjustAppTextPosition(dir: Int) = step(Keys.appTextPosition, dir, AppTextPosition.entries) { it.appTextPosition }
    suspend fun adjustAppDetailsMode(dir: Int) = step(Keys.appDetailsMode, dir, AppDetailsMode.entries) { it.appDetailsMode }
    suspend fun adjustCloseAnimationSpeed(dir: Int) = step(Keys.closeAnimationSpeed, dir, CloseAnimationSpeed.entries) { it.closeAnimationSpeed }
    suspend fun adjustCloseHintStyle(dir: Int) = step(Keys.closeHintStyle, dir, CloseHintStyle.entries) { it.closeHintStyle }
    suspend fun toggleForceStop() = toggle(Keys.forceStop) { it.forceStopOnKill }
    suspend fun toggleHiddenKillExclusion() = toggle(Keys.excludeHiddenFromKillAll) { it.excludeHiddenFromKillAll }
    suspend fun setHiddenKillExclusion(v: Boolean) = dataStore.edit { it[Keys.excludeHiddenFromKillAll] = v }
    suspend fun toggleIndividualProtection() = toggle(Keys.protectIndividualCloses) { it.protectIndividualCloses }
    suspend fun toggleKillAllConfirmation() = toggle(Keys.confirmKillAll) { it.confirmKillAll }
    suspend fun setIndividualProtection(v: Boolean) = dataStore.edit { it[Keys.protectIndividualCloses] = v }
    suspend fun setKillAllConfirmation(v: Boolean) = dataStore.edit { it[Keys.confirmKillAll] = v }
    suspend fun toggleShowHeader() = toggle(Keys.showHeader) { it.showHeader }
    suspend fun toggleSettingsButton() = toggle(Keys.showSettingsButton) { it.showSettingsButton }
    suspend fun toggleAppNames() = toggle(Keys.showAppNames) { it.showAppNames }
    suspend fun toggleShowHelp() = toggle(Keys.showHelp) { it.showHelp }
    suspend fun toggleSmoothMotion() = toggle(Keys.smooth) { it.smoothMotion }
    suspend fun toggleUsePrivileged() = toggle(Keys.usePrivileged) { it.usePrivileged }
    suspend fun resetToDefaults() = dataStore.edit { it.clear() }
    suspend fun applyAppearancePreset(preset: AppearancePreset) = dataStore.edit { p ->
        // Keep the original checkpoint while trying several presets.
        if (readSnapshot(p[Keys.appearanceRestore]) == null) p[Keys.appearanceRestore] = snapshot(p)
        val look = preset.appearance()
        p[Keys.position] = look.position.name
        p[Keys.direction] = look.direction.name
        p[Keys.size] = look.size.name
        p[Keys.shape] = look.shape.name
        p[Keys.cardPadding] = look.cardPadding.name
        p[Keys.accent] = look.accent.name
        p.remove(Keys.customAccent)
        p[Keys.bgDim] = look.bgDim
        p[Keys.bgTint] = look.bgTint.name
        p[Keys.showHeader] = look.showHeader
        p[Keys.showSettingsButton] = look.showSettingsButton
        p[Keys.showHelp] = look.showHelp
        p[Keys.showAppNames] = look.showAppNames
        p[Keys.appTextPosition] = look.appTextPosition.name
        p[Keys.appDetailsMode] = look.appDetailsMode.name
        p[Keys.closeHintStyle] = look.closeHintStyle.name
        p[Keys.smooth] = look.smoothMotion
        p[Keys.closeAnimationSpeed] = look.closeAnimationSpeed.name
        p[Keys.killAllLocation] = look.killAllLocation.name
        // Service use, force-stop, confirmation and app protection are deliberately untouched.
    }
    suspend fun restoreAppearance(): Boolean {
        var restored = false
        dataStore.edit { p ->
            val saved = readSnapshot(p[Keys.appearanceRestore])
            if (saved != null) {
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



