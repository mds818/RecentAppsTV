package com.websnake.recentstv.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.websnake.recentstv.ui.theme.AccentColor
import com.websnake.recentstv.ui.theme.BgTint
import com.websnake.recentstv.ui.theme.CardShapeOpt
import com.websnake.recentstv.ui.theme.CardSizeOpt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "recents_settings")

/** Immutable snapshot of all persisted settings. */
data class RecentsSettings(
    val showKillAll: Boolean = true,
    val size: CardSizeOpt = CardSizeOpt.MEDIUM,
    val shape: CardShapeOpt = CardShapeOpt.ROUNDED,
    val exceptions: Set<String> = emptySet(),
    val accent: AccentColor = AccentColor.GREEN,
    val bgDim: Int = 62,                 // backdrop dim, 0..100 %
    val bgTint: BgTint = BgTint.CHARCOAL,
    val forceStopOnKill: Boolean = true,
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val showKillAll = booleanPreferencesKey("show_kill_all")
        val size = stringPreferencesKey("size")
        val shape = stringPreferencesKey("shape")
        val exceptions = stringSetPreferencesKey("exceptions")
        val accent = stringPreferencesKey("accent")
        val bgDim = intPreferencesKey("bg_dim_pct")
        val bgTint = stringPreferencesKey("bg_tint")
        val forceStop = booleanPreferencesKey("force_stop_on_kill")
    }

    val settings: Flow<RecentsSettings> = context.dataStore.data.map { p ->
        RecentsSettings(
            showKillAll = p[Keys.showKillAll] ?: true,
            size = runCatching { CardSizeOpt.valueOf(p[Keys.size] ?: "MEDIUM") }.getOrDefault(CardSizeOpt.MEDIUM),
            shape = runCatching { CardShapeOpt.valueOf(p[Keys.shape] ?: "ROUNDED") }.getOrDefault(CardShapeOpt.ROUNDED),
            exceptions = p[Keys.exceptions] ?: emptySet(),
            accent = runCatching { AccentColor.valueOf(p[Keys.accent] ?: "GREEN") }.getOrDefault(AccentColor.GREEN),
            bgDim = (p[Keys.bgDim] ?: 62).coerceIn(0, 100),
            bgTint = runCatching { BgTint.valueOf(p[Keys.bgTint] ?: "CHARCOAL") }.getOrDefault(BgTint.CHARCOAL),
            forceStopOnKill = p[Keys.forceStop] ?: true,
        )
    }

    suspend fun setShowKillAll(v: Boolean) = context.dataStore.edit { it[Keys.showKillAll] = v }
    suspend fun setSize(v: CardSizeOpt) = context.dataStore.edit { it[Keys.size] = v.name }
    suspend fun setShape(v: CardShapeOpt) = context.dataStore.edit { it[Keys.shape] = v.name }
    suspend fun setAccent(v: AccentColor) = context.dataStore.edit { it[Keys.accent] = v.name }
    suspend fun setBgDim(v: Int) = context.dataStore.edit { it[Keys.bgDim] = v.coerceIn(0, 100) }
    suspend fun setBgTint(v: BgTint) = context.dataStore.edit { it[Keys.bgTint] = v.name }
    suspend fun setForceStop(v: Boolean) = context.dataStore.edit { it[Keys.forceStop] = v }
    suspend fun toggleException(pkg: String) = context.dataStore.edit { p ->
        val cur = (p[Keys.exceptions] ?: emptySet()).toMutableSet()
        if (!cur.remove(pkg)) cur.add(pkg)
        p[Keys.exceptions] = cur
    }
}
