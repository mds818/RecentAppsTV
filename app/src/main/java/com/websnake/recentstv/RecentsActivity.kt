package com.websnake.recentstv

import com.websnake.recentstv.data.KillAllPolicy

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
import com.websnake.recentstv.data.KillAllLocation
import com.websnake.recentstv.data.RecentsSettings
import com.websnake.recentstv.data.SettingsStore
import com.websnake.recentstv.shizuku.ShizukuManager
import com.websnake.recentstv.ui.overlay.OverlayItem
import com.websnake.recentstv.ui.overlay.RecentsOverlay
import com.websnake.recentstv.ui.state.EmptyState
import com.websnake.recentstv.ui.state.UsageAccessState
import com.websnake.recentstv.ui.theme.RecentsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

private enum class Scene { LOADING, OVERLAY, EMPTY, USAGE }

class RecentsActivity : ComponentActivity() {

    private lateinit var repo: RecentTasksRepository
    private lateinit var store: SettingsStore

    // ── UI state (observed by the composition, mutated by the key handler) ──
    // Resolve task/usage mode before presenting interactive cards.
    private var scene by mutableStateOf(Scene.LOADING)
    private val tasks = mutableStateListOf<RecentTask>()
    private var settings by mutableStateOf(RecentsSettings())
    private var focus by mutableIntStateOf(0)
    private var peekDir by mutableIntStateOf(-1)             // -1 none, 0 up, 1 down
    private var dismissing by mutableStateOf<Pair<Int, Int>?>(null)
    private var killing by mutableStateOf(false)
    private var killPrompt by mutableStateOf(false)
    private var confirmKill by mutableStateOf(false)
    private var gearFocused by mutableStateOf(false)        // settings gear (top-right)
    private var topKillFocused by mutableStateOf(false)
    private var settingsHintVisible by mutableStateOf(false)
    private var settingsHintJob: kotlinx.coroutines.Job? = null
    private var privileged by mutableStateOf(false)
    private var ctxTask by mutableStateOf<RecentTask?>(null) // long-press context menu target
    private var ctxSelected by mutableIntStateOf(0)
    private var longPressFired = false
    private var centerHeld = false
    private var ignoreCenterUntilUp = false
    private var longPressJob: kotlinx.coroutines.Job? = null
    private var busy = false                                  // guards async transitions

    private var reloadJob: kotlinx.coroutines.Job? = null
    private var actionJob: kotlinx.coroutines.Job? = null

    private val permListener = Shizuku.OnRequestPermissionResultListener { _, _ -> runOnUiThread { reload() } }
    // Shizuku's binder attaches asynchronously after process start; re-resolve the
    // scene the moment it arrives — but only while still waiting, so it doesn't
    // double the recents read on a warm open (onResume already reloaded).
    private val binderReceived = Shizuku.OnBinderReceivedListener {
        runOnUiThread { reload() }
    }
    private val binderDead = Shizuku.OnBinderDeadListener { runOnUiThread { privileged = false; reload() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.websnake.recentstv.updates.UpdateManager.checkOnce(applicationContext)
        repo = RecentTasksRepository(applicationContext)
        store = SettingsStore(applicationContext)
        ShizukuManager.addPermissionListener(permListener)
        runCatching { Shizuku.addBinderReceivedListenerSticky(binderReceived) }
        runCatching { Shizuku.addBinderDeadListener(binderDead) }

        setContent {
            RecentsTheme(accent = settings.accentColor) {
                val scrimColor = settings.bgTint.color
                val scrimAlpha = settings.bgDim / 100f
                when (scene) {
                    Scene.LOADING -> Scrim(scrimColor, scrimAlpha)
                    Scene.USAGE -> UsageAccessState()
                    Scene.EMPTY -> Box(Modifier.fillMaxSize()) { Scrim(scrimColor, scrimAlpha); EmptyState() }
                    Scene.OVERLAY -> Box(Modifier.fillMaxSize()) {
                        Scrim(scrimColor, scrimAlpha)
                        RecentsOverlay(
                            items = buildItems(),
                            focus = focus,
                            gearFocused = gearFocused,
                            topKillFocused = topKillFocused,
                            settingsHintVisible = settingsHintVisible,
                            showTopKillAll = hasTopKillAll(),
                            closeHintStyle = settings.closeHintStyle,
                            protectedApps = settings.protectedApps,
                            peekDir = peekDir,
                            dismissing = dismissing,
                            killing = killing,
                            size = settings.size,
                            shape = settings.shape,
                            cardPadding = settings.cardPadding,
                            taskCount = tasks.size,
                            position = settings.position,
                            direction = settings.direction,
                            showHeader = settings.showHeader,
                            showSettingsButton = settings.showSettingsButton,
                            showHelp = settings.showHelp,
                            showAppNames = settings.showAppNames,
                            appTextPosition = settings.appTextPosition,
                            showAppDetails = settings.appDetailsMode.visible(privileged),
                            smoothMotion = settings.smoothMotion,
                            closeAnimationMs = settings.closeAnimationSpeed.durationMs,
                            onDismissComplete = ::onCardFlewOff,
                        )
                        ctxTask?.let { com.websnake.recentstv.ui.overlay.ContextMenu(it, ctxSelected, privileged, isIndividuallyProtected(it), it.packageName in settings.protectedApps, settings.excludeHiddenFromKillAll) }
                        if (killPrompt) com.websnake.recentstv.ui.overlay.KillAllConfirmation(
                            killAllCandidates().size, bulkTasks.count { it.packageName in settings.protectedApps },
                            settings.forceStopOnKill, confirmKill)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.websnake.recentstv.updates.UpdateManager.onResume(this)
        reload()
        permissionWatch?.cancel()
        permissionWatch = lifecycleScope.launch {
            while (true) {
                delay(1000)
                val ready = repo.ready()
                val allowed = repo.hasUsageAccess()
                if (ready != privileged || (!ready && (scene == Scene.USAGE) == allowed)) reload()
            }
        }
    }

    private var permissionWatch: kotlinx.coroutines.Job? = null

    override fun onPause() {
        com.websnake.recentstv.updates.UpdateManager.onPause(this)
        clearSettingsHint()
        killPrompt = false
        cancelCenterPress()
        permissionWatch?.cancel()
        super.onPause()
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
    private var bulkTasks: List<RecentTask> = emptyList()

    private fun reload() {
        bulkTasks = emptyList()
        clearSettingsHint()
        killPrompt = false
        cancelCenterPress()
        actionJob?.cancel()
        scene = Scene.LOADING
        reloadJob?.cancel()
        reloadJob = lifecycleScope.launch {
            settings = withContext(Dispatchers.IO) { store.settings.first() }
            repo.privilegedEnabled = settings.usePrivileged
            privileged = repo.ready()
            killing = false; dismissing = null; peekDir = -1; ctxTask = null; busy = false; topKillFocused = false
            if (!privileged && !repo.hasUsageAccess()) {
                tasks.clear()
                scene = Scene.USAGE
                return@launch
            }
            val useTasks = privileged
            val loaded = withContext(Dispatchers.IO) {
                if (useTasks) repo.getRecentTasks(emptySet())
                else runCatching { repo.getRecentApps(settings.exceptions) }.getOrDefault(emptyList())
            }
            if (useTasks != repo.ready()) { reload(); return@launch }
            if (!useTasks && !repo.hasUsageAccess()) { scene = Scene.USAGE; return@launch }
            bulkTasks = if (useTasks) loaded else emptyList()
            tasks.clear(); tasks.addAll(loaded.filterNot { it.packageName in settings.exceptions })
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
        if (privileged && (settings.killAllLocation == KillAllLocation.APP_ROW) && tasks.isNotEmpty()) list.add(OverlayItem.KillAll)
        tasks.forEach { list.add(OverlayItem.Card(it)) }
        return list
    }

    private fun defaultFocus(): Int =
        if (privileged && (settings.killAllLocation == KillAllLocation.APP_ROW) && tasks.isNotEmpty()) 1 else 0

    private fun itemCount(): Int = buildItems().size
    private fun isKillAllAt(i: Int): Boolean =
        privileged && (settings.killAllLocation == KillAllLocation.APP_ROW) && tasks.isNotEmpty() && i == 0
    private fun taskAt(i: Int): RecentTask? {
        val offset = if (privileged && (settings.killAllLocation == KillAllLocation.APP_ROW) && tasks.isNotEmpty()) 1 else 0
        return tasks.getOrNull(i - offset)
    }

    // ── key handling ────────────────────────────────────────────────────────
    private fun openSettings() {
        clearSettingsHint()
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    // This overrides the public android.app.Activity callback. AndroidX core's inherited
    // library restriction triggers lint here even though no restricted superclass API is called.
    @android.annotation.SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode
        val down = event.action == KeyEvent.ACTION_DOWN
        val isCenter = code == KeyEvent.KEYCODE_DPAD_CENTER ||
            code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_BUTTON_A

        if (killPrompt) {
            if (isCenter && !down) { ignoreCenterUntilUp = false; return true }
            if (!down || (isCenter && (event.repeatCount > 0 || ignoreCenterUntilUp))) return true
            when (code) {
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> { killPrompt = false; cancelCenterPress(); restoreTopSettingsHint() }
                KeyEvent.KEYCODE_DPAD_LEFT -> confirmKill = false
                KeyEvent.KEYCODE_DPAD_RIGHT -> confirmKill = killAllCandidates().isNotEmpty()
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A -> {
                    val proceed = confirmKill
                    killPrompt = false; cancelCenterPress()
                    if (proceed) executeKillAll()
                    else restoreTopSettingsHint()
                }
            }
            return true
        }

        if (down) when (code) {
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                clearSettingsHint()
                cancelCenterPress()
                if (ctxTask != null) ctxTask = null
                else if (gearFocused || topKillFocused) { gearFocused = false; topKillFocused = false }
                else finish()
                return true
            }
            KeyEvent.KEYCODE_MENU -> { cancelCenterPress(); openSettings(); return true }
        }
        // Hidden settings access requires a deliberate second gesture, not a held UP repeat.
        if (down && code == KeyEvent.KEYCODE_DPAD_UP && event.repeatCount > 0) return true
        if (down && code != KeyEvent.KEYCODE_DPAD_UP && !topKillFocused) clearSettingsHint()

        // Consume the entire press that opened the menu, including repeated downs and
        // its release. Choosing a menu action always requires a fresh OK press.
        if (isCenter && down && event.repeatCount > 0) {
            if (centerHeld && ctxTask == null && !gearFocused && !topKillFocused && !isKillAllAt(focus))
                taskAt(focus)?.let { openHeldMenu(it) }
            return true
        }
        if (isCenter && event.action == KeyEvent.ACTION_UP) {
            longPressJob?.cancel()
            if (ignoreCenterUntilUp) { cancelCenterPress(); return true }
            if (!centerHeld) return true
            centerHeld = false
        }

        // Context menu, when open, captures everything.
        if (isCenter && ignoreCenterUntilUp) return true
        if (ctxTask != null) {
            if (down) {
                if (isCenter) ignoreCenterUntilUp = true
                handleCtxKey(code)
            }
            return true
        }

        // Overlay OK: hold = context menu; tap (on key-up) = activate.
        if (scene == Scene.OVERLAY && isCenter && !gearFocused && !topKillFocused && !busy && !killing && dismissing == null) {
            if (down) {
                if (!centerHeld) {
                    centerHeld = true
                    longPressFired = false
                    val task = taskAt(focus)
                    if (!isKillAllAt(focus) && task != null) longPressJob = lifecycleScope.launch {
                        delay(android.view.ViewConfiguration.getLongPressTimeout().toLong())
                        openHeldMenu(task)
                    }
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
            Scene.USAGE -> when (code) {
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A -> openUsageAccess()
            }
            Scene.EMPTY -> when (code) {              // no apps, but Settings still reachable
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A -> finish()
                KeyEvent.KEYCODE_DPAD_UP -> openSettings()
            }
            Scene.OVERLAY -> handleOverlayKey(code)
        }
        return true
    }

    private fun handleCtxKey(code: Int) {
        val task = ctxTask ?: return
        val canClose = privileged && !isIndividuallyProtected(task)
        val actions = buildList {
            add(0)
            if (canClose) add(1)
            if (privileged) add(2)
            add(3)
        }
        val index = actions.indexOf(ctxSelected).coerceAtLeast(0)
        when (code) {
            KeyEvent.KEYCODE_DPAD_UP -> ctxSelected = actions[(index - 1).coerceAtLeast(0)]
            KeyEvent.KEYCODE_DPAD_DOWN -> ctxSelected = actions[(index + 1).coerceAtMost(actions.lastIndex)]
            KeyEvent.KEYCODE_DPAD_LEFT -> ctxTask = null
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A ->
                executeCtx(task, ctxSelected)
        }
    }

    private fun cancelCenterPress() {
        longPressJob?.cancel()
        longPressJob = null
        centerHeld = false
        ignoreCenterUntilUp = false
        longPressFired = false
    }

    private fun openHeldMenu(task: RecentTask) {
        if (!centerHeld || ctxTask != null || scene != Scene.OVERLAY || busy || killing || dismissing != null) return
        longPressJob?.cancel()
        ignoreCenterUntilUp = true
        longPressFired = true
        peekDir = -1
        ctxSelected = 0
        ctxTask = task
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
                lifecycleScope.launch {
                    store.toggleException(task.packageName)
                    settings = store.settings.first()
                    removeFromView(task)
                }
            }
            1 -> {                                    // kill app
                if (!privileged || !repo.ready()) return
                if (isIndividuallyProtected(task)) { showProtectedNotice(); return }
                removeFromView(task)
                bulkTasks = bulkTasks.filterNot { it.packageName == task.packageName }
                val fs = settings.forceStopOnKill
                val protectedPackages = individualProtectionPackages()
                actionJob = lifecycleScope.launch { withContext(Dispatchers.IO) { repo.dismiss(task, fs, protectedPackages) } }
            }
            2 -> {
                if (!privileged || !repo.ready() || busy) return
                busy = true
                actionJob = lifecycleScope.launch {
                    try {
                        store.toggleProtectedApp(task.packageName)
                        settings = store.settings.first()
                        android.widget.Toast.makeText(this@RecentsActivity,
                            if (task.packageName in settings.protectedApps) "App protected" else "App unprotected",
                            android.widget.Toast.LENGTH_SHORT).show()
                    } finally { busy = false }
                }
            }
            3 -> {                                    // app info
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
        if (topKillFocused) {
            when (code) {
                KeyEvent.KEYCODE_DPAD_DOWN -> { topKillFocused = false; clearSettingsHint() }
                KeyEvent.KEYCODE_DPAD_RIGHT -> if (settings.showSettingsButton) { topKillFocused = false; gearFocused = true }
                KeyEvent.KEYCODE_DPAD_UP -> if (!settings.showSettingsButton) openSettings() else requestSettingsAccess()
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A -> killAll()
            }
            return
        }
        if (gearFocused) {
            when (code) {
                KeyEvent.KEYCODE_DPAD_UP -> if (!settings.showSettingsButton) requestSettingsAccess()
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (!settings.showSettingsButton) gearFocused = false
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (!settings.showSettingsButton) gearFocused = false
                    else if (hasTopKillAll()) { gearFocused = false; topKillFocused = true }
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> gearFocused = false
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A ->
                    if (settings.showSettingsButton) openSettings()
            }
            return
        }
        when (code) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> { peekDir = -1; moveFocus(if (settings.direction == com.websnake.recentstv.data.AppDirection.OLDER_RIGHT) +1 else -1) }
            KeyEvent.KEYCODE_DPAD_LEFT -> { peekDir = -1; moveFocus(if (settings.direction == com.websnake.recentstv.data.AppDirection.OLDER_RIGHT) -1 else +1) }
            KeyEvent.KEYCODE_DPAD_UP ->
                if (peekDir == 1) peekDir = -1          // cancel a pending peek → card back to normal
                else requestSettingsAccess()
            KeyEvent.KEYCODE_DPAD_DOWN -> if (!isKillAllAt(focus)) dismissStep(1)  // DOWN → dismiss/kill flow
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A -> activate()
        }
    }

    private fun hasTopKillAll() = privileged && tasks.isNotEmpty() && settings.killAllLocation == KillAllLocation.TOP_CONTROLS

    private fun requestSettingsAccess() {
        if (settings.showSettingsButton) {
            clearSettingsHint()
            gearFocused = true; topKillFocused = false
        }
        else if (hasTopKillAll()) { topKillFocused = true; gearFocused = false; showSettingsHint(false) }
        else if (settingsHintVisible) openSettings()
        else showSettingsHint(true)
    }

    private fun showSettingsHint(autoDismiss: Boolean) {
        settingsHintJob?.cancel()
        settingsHintVisible = true
        if (autoDismiss) settingsHintJob = lifecycleScope.launch { delay(3000); settingsHintVisible = false }
    }

    private fun clearSettingsHint() {
        settingsHintJob?.cancel(); settingsHintJob = null
        settingsHintVisible = false
    }

    private fun restoreTopSettingsHint() {
        if (topKillFocused && !settings.showSettingsButton) showSettingsHint(false)
    }

    private fun moveFocus(d: Int) {
        val n = itemCount()
        focus = (focus + d).coerceIn(0, (n - 1).coerceAtLeast(0))
    }

    private fun dismissStep(dir: Int) {
        if (!privileged || !repo.ready()) return
        if (isKillAllAt(focus)) return
        if (taskAt(focus)?.let(::isIndividuallyProtected) == true) { peekDir = -1; showProtectedNotice(); return }
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
        if (!privileged || !repo.ready()) { reload(); return }
        val idx = tasks.indexOfFirst { it.taskId == taskId }
        if (idx < 0) { dismissing = null; return }
        val task = tasks[idx]
        if (isIndividuallyProtected(task)) { dismissing = null; peekDir = -1; showProtectedNotice(); return }
        // Remove from the UI immediately so the row collapses with no leftover gap;
        // the privileged removeTask runs in the background.
        tasks.removeAt(idx)
        bulkTasks = bulkTasks.filterNot { it.packageName == task.packageName }
        dismissing = null
        peekDir = -1
        if (tasks.isEmpty()) scene = Scene.EMPTY
        else focus = focus.coerceIn(0, itemCount() - 1)
        val fs = settings.forceStopOnKill
        val protectedPackages = individualProtectionPackages()
        actionJob = lifecycleScope.launch { withContext(Dispatchers.IO) { repo.dismiss(task, fs, protectedPackages) } }
    }

    private fun activate() {
        if (isKillAllAt(focus)) killAll()
        else taskAt(focus)?.let { switchTo(it) }
    }

    private fun switchTo(task: RecentTask) {
        if (busy) return
        busy = true
        actionJob = lifecycleScope.launch {
            val moved = withContext(Dispatchers.IO) { repo.switchTo(task) }
            var launched = moved
            if (!moved) {
                // fallback: relaunch via the app's own launch intent
                val li = packageManager.getLeanbackLaunchIntentForPackage(task.packageName)
                    ?: packageManager.getLaunchIntentForPackage(task.packageName)
                li?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launched = li != null && runCatching { startActivity(li); true }.getOrDefault(false)
            }
            if (!launched) {
                busy = false
                android.widget.Toast.makeText(this@RecentsActivity, "Unable to open this app", android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }
            delay(120)
            finish()                                   // reveal the now-foregrounded app
        }
    }

    private fun killAll() {
        if (!privileged || !repo.ready() || busy) return
        clearSettingsHint()
        if (!settings.confirmKillAll) { cancelCenterPress(); executeKillAll(); return }
        cancelCenterPress()
        ignoreCenterUntilUp = topKillFocused
        confirmKill = false
        killPrompt = true
    }

    private fun killAllCandidates() = KillAllPolicy.candidates(bulkTasks, settings.exceptions, settings.protectedApps, settings.excludeHiddenFromKillAll)

    private fun individualProtectionPackages() = if (settings.protectIndividualCloses) settings.protectedApps else emptySet()
    private fun isIndividuallyProtected(task: RecentTask) = task.packageName in individualProtectionPackages()
    private var protectedNoticeAt = 0L
    private fun showProtectedNotice() {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - protectedNoticeAt < 1800) return
        protectedNoticeAt = now
        android.widget.Toast.makeText(this, "Protected app · change protection in Settings → Apps", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun executeKillAll() {
        if (!privileged || !repo.ready()) return
        if (busy) return
        val snapshot = killAllCandidates()
        if (snapshot.isEmpty()) {
            restoreTopSettingsHint()
            android.widget.Toast.makeText(this, "All recent apps are protected", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        busy = true
        killing = true
        val protectedPackages = settings.protectedApps
        val fs = settings.forceStopOnKill
        val startedAt = android.os.SystemClock.elapsedRealtime()
        actionJob = lifecycleScope.launch {
            withContext(Dispatchers.IO) { repo.killAll(snapshot, fs, protectedPackages) }
            val animationTime = if (settings.smoothMotion) 240L + (snapshot.size - 1).coerceAtLeast(0) * 20L else 0L
            val remaining = animationTime - (android.os.SystemClock.elapsedRealtime() - startedAt)
            if (remaining > 0) delay(remaining)
            val closedPackages = snapshot.mapTo(HashSet()) { it.packageName }
            tasks.removeAll { it.packageName in closedPackages }
            bulkTasks = bulkTasks.filterNot { it.packageName in closedPackages }
            killing = false
            busy = false
            if (tasks.isEmpty()) scene = Scene.EMPTY
            else { focus = defaultFocus(); scene = Scene.OVERLAY }
        }
    }

    private fun openUsageAccess() {
        val intents = listOf(
            Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS,
                android.net.Uri.parse("package:$packageName")),
            Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS),
            Intent(android.provider.Settings.ACTION_SETTINGS),
        )
        for (intent in intents) {
            if (runCatching { startActivity(intent); true }.getOrDefault(false)) return
        }
        android.widget.Toast.makeText(this, "Open Settings > Apps > Special app access > Usage access",
            android.widget.Toast.LENGTH_LONG).show()
    }
}

/** Solid scrim that mutes whatever app is behind the translucent overlay, so the
 *  carousel reads cleanly even over bright apps (the system dim alone isn't
 *  enough against light content). */
@androidx.compose.runtime.Composable
private fun Scrim(color: Color, alpha: Float) {
    Box(Modifier.fillMaxSize().background(color.copy(alpha = alpha)))
}






