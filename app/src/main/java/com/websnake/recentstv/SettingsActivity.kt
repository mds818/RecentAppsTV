package com.websnake.recentstv

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.lifecycleScope
import com.websnake.recentstv.data.InstalledApp
import com.websnake.recentstv.data.PackageRepository
import com.websnake.recentstv.data.RecentsSettings
import com.websnake.recentstv.data.SettingsStore
import com.websnake.recentstv.ui.settings.ExceptionsScreen
import com.websnake.recentstv.ui.settings.SetRow
import com.websnake.recentstv.ui.settings.SettingsScreen
import com.websnake.recentstv.ui.theme.AccentColor
import com.websnake.recentstv.ui.theme.BgTint
import com.websnake.recentstv.ui.theme.CardShapeOpt
import com.websnake.recentstv.ui.theme.CardSizeOpt
import com.websnake.recentstv.ui.theme.RecentsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SetScreen { SETTINGS, EXCEPTIONS }

class SettingsActivity : ComponentActivity() {

    private lateinit var store: SettingsStore
    private lateinit var pkgRepo: PackageRepository

    private var screen by mutableStateOf(SetScreen.SETTINGS)
    private var settings by mutableStateOf(RecentsSettings())
    private var setFocus by mutableIntStateOf(0)
    private val apps = mutableStateListOf<InstalledApp>()
    private var excFocus by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = SettingsStore(applicationContext)
        pkgRepo = PackageRepository(applicationContext)

        lifecycleScope.launch {
            store.settings.collect { settings = it }
        }

        setContent {
            RecentsTheme(accent = settings.accent.color) {
                when (screen) {
                    SetScreen.SETTINGS -> SettingsScreen(
                        settings = settings,
                        focus = setFocus,
                        exceptionsSummary = excSummary(),
                    )
                    SetScreen.EXCEPTIONS -> ExceptionsScreen(
                        apps = apps,
                        excluded = settings.exceptions,
                        focus = excFocus,
                    )
                }
            }
        }
    }

    private fun excSummary(): String {
        val n = settings.exceptions.size
        return if (n == 0) "No apps hidden" else "$n ${if (n == 1) "app" else "apps"} hidden"
    }

    // ── key handling ────────────────────────────────────────────────────────
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return true
        if (event.keyCode == KeyEvent.KEYCODE_BACK || event.keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (screen == SetScreen.EXCEPTIONS) { screen = SetScreen.SETTINGS } else finish()
            return true
        }
        when (screen) {
            SetScreen.SETTINGS -> settingsKey(event.keyCode)
            SetScreen.EXCEPTIONS -> excKey(event.keyCode)
        }
        return true
    }

    private fun settingsKey(code: Int) {
        val last = SetRow.entries.size - 1
        when (code) {
            KeyEvent.KEYCODE_DPAD_UP -> setFocus = (setFocus - 1).coerceAtLeast(0)
            KeyEvent.KEYCODE_DPAD_DOWN -> setFocus = (setFocus + 1).coerceAtMost(last)
            KeyEvent.KEYCODE_DPAD_LEFT -> adjust(-1)
            KeyEvent.KEYCODE_DPAD_RIGHT -> adjust(+1)
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A ->
                when (SetRow.entries[setFocus]) {
                    SetRow.KILL_ALL -> persist { store.setShowKillAll(!settings.showKillAll) }
                    SetRow.FORCE_STOP -> persist { store.setForceStop(!settings.forceStopOnKill) }
                    SetRow.SIZE -> stepSize(+1)
                    SetRow.SHAPE -> stepShape(+1)
                    SetRow.ACCENT -> stepAccent(+1)
                    SetRow.BG_DIM -> stepBgDim(+1)
                    SetRow.BG_TINT -> stepBgTint(+1)
                    SetRow.EXCEPTIONS -> openExceptions()
                }
        }
    }

    private fun adjust(dir: Int) {
        when (SetRow.entries[setFocus]) {
            SetRow.KILL_ALL -> persist { store.setShowKillAll(!settings.showKillAll) }
            SetRow.FORCE_STOP -> persist { store.setForceStop(!settings.forceStopOnKill) }
            SetRow.SIZE -> stepSize(dir)
            SetRow.SHAPE -> stepShape(dir)
            SetRow.ACCENT -> stepAccent(dir)
            SetRow.BG_DIM -> stepBgDim(dir)
            SetRow.BG_TINT -> stepBgTint(dir)
            SetRow.EXCEPTIONS -> {}
        }
    }

    private fun stepSize(dir: Int) {
        val v = CardSizeOpt.entries
        persist { store.setSize(v[(settings.size.ordinal + dir).coerceIn(0, v.size - 1)]) }
    }

    private fun stepShape(dir: Int) {
        val v = CardShapeOpt.entries
        persist { store.setShape(v[(settings.shape.ordinal + dir).coerceIn(0, v.size - 1)]) }
    }

    private fun stepAccent(dir: Int) {
        val v = AccentColor.entries
        persist { store.setAccent(v[(settings.accent.ordinal + dir).coerceIn(0, v.size - 1)]) }
    }

    private fun stepBgDim(dir: Int) {
        persist { store.setBgDim((settings.bgDim + dir * 5).coerceIn(0, 100)) }
    }

    private fun stepBgTint(dir: Int) {
        val v = BgTint.entries
        persist { store.setBgTint(v[(settings.bgTint.ordinal + dir).coerceIn(0, v.size - 1)]) }
    }

    private fun openExceptions() {
        excFocus = 0
        screen = SetScreen.EXCEPTIONS
        if (apps.isEmpty()) {
            lifecycleScope.launch {
                val loaded = withContext(Dispatchers.IO) { pkgRepo.installedApps() }
                apps.clear(); apps.addAll(loaded)
            }
        }
    }

    private fun excKey(code: Int) {
        when (code) {
            KeyEvent.KEYCODE_DPAD_UP -> excFocus = (excFocus - 1).coerceAtLeast(0)
            KeyEvent.KEYCODE_DPAD_DOWN -> excFocus = (excFocus + 1).coerceAtMost((apps.size - 1).coerceAtLeast(0))
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A -> {
                apps.getOrNull(excFocus)?.let { app -> persist { store.toggleException(app.packageName) } }
            }
        }
    }

    private fun persist(block: suspend () -> Unit) {
        lifecycleScope.launch { block() }
    }
}
