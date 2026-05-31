package com.websnake.recentstv

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.lifecycleScope
import com.websnake.recentstv.data.RecentTask
import com.websnake.recentstv.data.RecentTasksRepository
import com.websnake.recentstv.data.RecentsSettings
import com.websnake.recentstv.data.SettingsStore
import com.websnake.recentstv.shizuku.ShizukuManager
import com.websnake.recentstv.ui.overlay.OverlayItem
import com.websnake.recentstv.ui.overlay.RecentsOverlay
import com.websnake.recentstv.ui.state.EmptyState
import com.websnake.recentstv.ui.state.ShizukuGate
import com.websnake.recentstv.ui.theme.RecentsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

private enum class Scene { LOADING, OVERLAY, EMPTY, SHIZUKU }

class RecentsActivity : ComponentActivity() {

    private lateinit var repo: RecentTasksRepository
    private lateinit var store: SettingsStore

    // ── UI state (observed by the composition, mutated by the key handler) ──
    // Start transparent (LOADING) so the Shizuku gate never flashes before the
    // async readiness check resolves; reload() picks the real scene immediately.
    private var scene by mutableStateOf(Scene.LOADING)
    private val tasks = mutableStateListOf<RecentTask>()
    private var settings by mutableStateOf(RecentsSettings())
    private var focus by mutableIntStateOf(0)
    private var peekDir by mutableIntStateOf(-1)             // -1 none, 0 up, 1 down
    private var dismissing by mutableStateOf<Pair<Int, Int>?>(null)
    private var killing by mutableStateOf(false)
    private var gearFocused by mutableStateOf(false)        // settings gear (top-right)
    private var shzFocus by mutableIntStateOf(0)
    private var ctxTask by mutableStateOf<RecentTask?>(null) // long-press context menu target
    private var ctxSelected by mutableIntStateOf(0)
    private var longPressFired = false
    private var busy = false                                  // guards async transitions

    private var reloadJob: kotlinx.coroutines.Job? = null

    private val permListener = Shizuku.OnRequestPermissionResultListener { _, _ -> reload() }
    // Shizuku's binder attaches asynchronously after process start; re-resolve the
    // scene the moment it arrives — but only while still waiting, so it doesn't
    // double the recents read on a warm open (onResume already reloaded).
    private val binderReceived = Shizuku.OnBinderReceivedListener {
        if (scene == Scene.LOADING || scene == Scene.SHIZUKU) reload()
    }
    private val binderDead = Shizuku.OnBinderDeadListener { scene = Scene.SHIZUKU }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = RecentTasksRepository(applicationContext)
        store = SettingsStore(applicationContext)
        ShizukuManager.addPermissionListener(permListener)
        runCatching { Shizuku.addBinderReceivedListenerSticky(binderReceived) }
        runCatching { Shizuku.addBinderDeadListener(binderDead) }

        setContent {
            RecentsTheme(accent = settings.accent.color) {
                val scrimColor = settings.bgTint.color
                val scrimAlpha = settings.bgDim / 100f
                when (scene) {
                    Scene.LOADING -> {}                       // transparent; backdrop shows dimmed
                    Scene.SHIZUKU -> ShizukuGate(focus = shzFocus)
                    Scene.EMPTY -> Box(Modifier.fillMaxSize()) { Scrim(scrimColor, scrimAlpha); EmptyState() }
                    Scene.OVERLAY -> Box(Modifier.fillMaxSize()) {
                        Scrim(scrimColor, scrimAlpha)
                        RecentsOverlay(
                            items = buildItems(),
                            focus = focus,
                            gearFocused = gearFocused,
                            peekDir = peekDir,
                            dismissing = dismissing,
                            killing = killing,
                            size = settings.size,
                            shape = settings.shape,
                            taskCount = tasks.size,
                            onDismissComplete = ::onCardFlewOff,
                        )
                        ctxTask?.let { com.websnake.recentstv.ui.overlay.ContextMenu(it, ctxSelected) }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        // No close transition → the underlying app is revealed instantly, no blink.
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        ShizukuManager.removePermissionListener(permListener)
        runCatching { Shizuku.removeBinderReceivedListener(binderReceived) }
        runCatching { Shizuku.removeBinderDeadListener(binderDead) }
    }

    // ── load / scene routing ───────────────────────────────────────────────
    private fun reload() {
        lifecycleScope.launch {
            settings = withContext(Dispatchers.IO) { store.settings.first() }
            if (!ShizukuManager.state().ready) {
                scene = Scene.SHIZUKU
                shzFocus = 0
                return@launch
            }
            val loaded = withContext(Dispatchers.IO) { repo.getRecentTasks(settings.exceptions) }
            tasks.clear(); tasks.addAll(loaded)
            // Reset ALL transient guards — otherwise a warm-reopened overlay (after a
            // switch set busy=true and finish()ed without process death) stays dead.
            killing = false; dismissing = null; peekDir = -1; gearFocused = false
            ctxTask = null; longPressFired = false; busy = false
            if (tasks.isEmpty()) {
                scene = Scene.EMPTY
            } else {
                scene = Scene.OVERLAY
                focus = defaultFocus()
            }
        }
    }

    private fun buildItems(): List<OverlayItem> {
        val list = ArrayList<OverlayItem>()
        if (settings.showKillAll && tasks.isNotEmpty()) list.add(OverlayItem.KillAll)
        tasks.forEach { list.add(OverlayItem.Card(it)) }
        return list
    }

    private fun defaultFocus(): Int =
        if (settings.showKillAll && tasks.isNotEmpty()) 1 else 0

    private fun itemCount(): Int = buildItems().size
    private fun isKillAllAt(i: Int): Boolean =
        settings.showKillAll && tasks.isNotEmpty() && i == 0
    private fun taskAt(i: Int): RecentTask? {
        val offset = if (settings.showKillAll && tasks.isNotEmpty()) 1 else 0
        return tasks.getOrNull(i - offset)
    }

    // ── key handling ────────────────────────────────────────────────────────
    private fun openSettings() = startActivity(Intent(this, SettingsActivity::class.java))

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode
        val down = event.action == KeyEvent.ACTION_DOWN
        val isCenter = code == KeyEvent.KEYCODE_DPAD_CENTER ||
            code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_BUTTON_A

        if (down) when (code) {
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                if (ctxTask != null) { ctxTask = null } else finish(); return true
            }
            KeyEvent.KEYCODE_MENU -> { openSettings(); return true }
        }

        // Context menu, when open, captures everything.
        if (ctxTask != null) {
            if (down) handleCtxKey(code)
            return true
        }

        // Overlay OK: hold = context menu; tap (on key-up) = activate.
        if (scene == Scene.OVERLAY && isCenter && !gearFocused && !busy && !killing && dismissing == null) {
            if (down) {
                if (event.repeatCount == 1 && !isKillAllAt(focus) && taskAt(focus) != null) {
                    longPressFired = true
                    ctxSelected = 0; ctxTask = taskAt(focus)
                }
                return true
            }
            if (event.action == KeyEvent.ACTION_UP) {
                if (longPressFired) longPressFired = false else handleOverlayKey(KeyEvent.KEYCODE_DPAD_CENTER)
                return true
            }
        }

        if (!down) return true
        when (scene) {
            Scene.LOADING -> {}
            Scene.SHIZUKU -> handleShizukuKey(code)
            Scene.EMPTY -> when (code) {              // no apps, but Settings still reachable
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A,
                KeyEvent.KEYCODE_DPAD_UP -> openSettings()
            }
            Scene.OVERLAY -> handleOverlayKey(code)
        }
        return true
    }

    private fun handleCtxKey(code: Int) {
        val task = ctxTask ?: return
        when (code) {
            KeyEvent.KEYCODE_DPAD_UP -> ctxSelected = (ctxSelected - 1).coerceAtLeast(0)
            KeyEvent.KEYCODE_DPAD_DOWN -> ctxSelected = (ctxSelected + 1).coerceAtMost(2)
            KeyEvent.KEYCODE_DPAD_LEFT -> ctxTask = null
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A ->
                executeCtx(task, ctxSelected)
        }
    }

    private fun removeFromView(task: RecentTask) {
        val idx = tasks.indexOfFirst { it.taskId == task.taskId }
        if (idx >= 0) tasks.removeAt(idx)
        if (tasks.isEmpty()) scene = Scene.EMPTY else focus = focus.coerceIn(0, itemCount() - 1)
    }

    private fun executeCtx(task: RecentTask, option: Int) {
        ctxTask = null
        when (option) {
            0 -> {                                    // add to exceptions
                lifecycleScope.launch { store.toggleException(task.packageName) }
                removeFromView(task)
            }
            1 -> {                                    // kill app
                removeFromView(task)
                val fs = settings.forceStopOnKill
                lifecycleScope.launch { withContext(Dispatchers.IO) { repo.dismiss(task, fs) } }
            }
            2 -> {                                    // app info
                runCatching {
                    startActivity(
                        Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.parse("package:${task.packageName}"),
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                finish()
            }
        }
    }

    private fun handleOverlayKey(code: Int) {
        if (busy || killing) return
        if (gearFocused) {
            when (code) {
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT ->
                    gearFocused = false
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A ->
                    startActivity(Intent(this, SettingsActivity::class.java))
            }
            return
        }
        when (code) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> { peekDir = -1; moveFocus(-1) }   // toward Kill-All
            KeyEvent.KEYCODE_DPAD_LEFT -> { peekDir = -1; moveFocus(+1) }    // toward older
            KeyEvent.KEYCODE_DPAD_UP ->
                if (peekDir == 1) peekDir = -1          // cancel a pending peek → card back to normal
                else gearFocused = true                 // otherwise UP → settings gear
            KeyEvent.KEYCODE_DPAD_DOWN -> if (!isKillAllAt(focus)) dismissStep(1)  // DOWN → dismiss/kill flow
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A -> activate()
        }
    }

    private fun moveFocus(d: Int) {
        val n = itemCount()
        focus = (focus + d).coerceIn(0, (n - 1).coerceAtLeast(0))
    }

    private fun dismissStep(dir: Int) {
        if (isKillAllAt(focus)) return
        if (dismissing != null) return
        if (peekDir == dir) {
            // stage 2 — fly off & remove
            val task = taskAt(focus) ?: return
            peekDir = -1
            dismissing = task.taskId to dir
        } else {
            peekDir = dir                              // stage 1 — peek
        }
    }

    private fun onCardFlewOff(taskId: Int) {
        val idx = tasks.indexOfFirst { it.taskId == taskId }
        if (idx < 0) { dismissing = null; return }
        val task = tasks[idx]
        // Remove from the UI immediately so the row collapses with no leftover gap;
        // the privileged removeTask runs in the background.
        tasks.removeAt(idx)
        dismissing = null
        peekDir = -1
        if (tasks.isEmpty()) scene = Scene.EMPTY
        else focus = focus.coerceIn(0, itemCount() - 1)
        val fs = settings.forceStopOnKill
        lifecycleScope.launch { withContext(Dispatchers.IO) { repo.dismiss(task, fs) } }
    }

    private fun activate() {
        if (isKillAllAt(focus)) killAll()
        else taskAt(focus)?.let { switchTo(it) }
    }

    private fun switchTo(task: RecentTask) {
        if (busy) return
        busy = true
        lifecycleScope.launch {
            val moved = withContext(Dispatchers.IO) { repo.switchTo(task) }
            if (!moved) {
                // fallback: relaunch via the app's own launch intent
                val li = packageManager.getLeanbackLaunchIntentForPackage(task.packageName)
                    ?: packageManager.getLaunchIntentForPackage(task.packageName)
                li?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { li?.let { startActivity(it) } }
            }
            delay(120)
            finish()                                   // reveal the now-foregrounded app
        }
    }

    private fun killAll() {
        if (busy) return
        busy = true
        killing = true
        val snapshot = tasks.toList()
        val fs = settings.forceStopOnKill
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { repo.killAll(snapshot, fs) }
            delay(520)                                  // let the staggered fly-up play
            tasks.clear()
            killing = false
            busy = false
            scene = Scene.EMPTY
        }
    }

    private fun handleShizukuKey(code: Int) {
        when (code) {
            KeyEvent.KEYCODE_DPAD_LEFT -> shzFocus = (shzFocus + 1).coerceAtMost(1)
            KeyEvent.KEYCODE_DPAD_RIGHT -> shzFocus = (shzFocus - 1).coerceAtLeast(0)
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A -> {
                // Either button advances the flow so a mis-focus can't strand the user.
                val st = ShizukuManager.state()
                if (!st.binderAlive) {
                    ShizukuManager.openShizuku(this)         // Shizuku service not running
                } else if (!st.permissionGranted) {
                    ShizukuManager.requestPermission()       // running but not granted
                }
                reload()
            }
        }
    }
}

/** Solid scrim that mutes whatever app is behind the translucent overlay, so the
 *  carousel reads cleanly even over bright apps (the system dim alone isn't
 *  enough against light content). */
@androidx.compose.runtime.Composable
private fun Scrim(color: Color, alpha: Float) {
    Box(Modifier.fillMaxSize().background(color.copy(alpha = alpha)))
}
