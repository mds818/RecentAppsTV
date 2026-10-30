package com.websnake.recentstv

import com.websnake.recentstv.data.*
import com.websnake.recentstv.data.ViewSwitchPolicy
import com.websnake.recentstv.data.SwitchOrigin
import com.websnake.recentstv.data.EdgeShortcut
import com.websnake.recentstv.data.RowSwitchPosition
import com.websnake.recentstv.data.AppListSource
import com.websnake.recentstv.data.ListSourcePolicy
import com.websnake.recentstv.data.ActionPolicy
import com.websnake.recentstv.data.AppListMode
import com.websnake.recentstv.data.DownAction
import com.websnake.recentstv.data.DismissPolicy
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
    private var protectionConsumedEvent by mutableIntStateOf(0)
    private var protectionEvent by mutableIntStateOf(0)
    private var protectionTaskId by mutableIntStateOf(Int.MIN_VALUE)
    private var protectionMessage by mutableStateOf<String?>(null)
    private var protectionHintJob: kotlinx.coroutines.Job? = null
    private var peekDir by mutableIntStateOf(-1)             // -1 none, 0 up, 1 down
    private var dismissing by mutableStateOf<Pair<Int, Int>?>(null)
    private var killing by mutableStateOf(false)
    private var killPrompt by mutableStateOf(false)
    private var confirmKill by mutableStateOf(false)
    private var promptBulkAction by mutableStateOf(DownAction.CLOSE_TASK)
    private var bulkAnimatingPackages by mutableStateOf<Set<String>>(emptySet())
    private var viewRevision by mutableIntStateOf(0)
    private var rowVisible by mutableStateOf(true)
    private var armedEdgeSide by mutableStateOf<RowSwitchPosition?>(null)
    private var edgeArmed by mutableStateOf(false)
    private var edgeArmJob: kotlinx.coroutines.Job? = null
    private var restoredPackages by mutableStateOf<Set<String>>(emptySet())
    private var restoreRevision by mutableIntStateOf(0)
    private var restoreArrivalJob: kotlinx.coroutines.Job? = null
    private var viewNoticeVisible by mutableStateOf(false)
    private var viewNotice by mutableStateOf<ViewNoticeEvent?>(null)
    private var noticeSequence = 0L
    private var restorePreview by mutableStateOf<List<com.websnake.recentstv.ui.overlay.PreviewApp>>(emptyList())
    private var restorePreviewLoading by mutableStateOf(false)
    private var restorePreviewCount by mutableIntStateOf(0)
    private var restorePreviewJob: kotlinx.coroutines.Job? = null
    private var restorePrompt by mutableStateOf(false)
    private var restoreConfirm by mutableStateOf(false)
    private var edgeMenuRight = true
    private var edgeMenuOpen by mutableStateOf(false)
    private var edgeMenuSelected by mutableIntStateOf(0)
    private var promptBulkFromEdge = false
    private var viewNoticeJob: kotlinx.coroutines.Job? = null
    private var restoreFocused by mutableStateOf(false)
    private var viewFocused by mutableStateOf(false)
    private var temporaryListMode: AppListMode? = null
    private var gearFocused by mutableStateOf(false)        // settings gear (top-right)
    private var topKillFocused by mutableStateOf(false)
    private var settingsHintVisible by mutableStateOf(false)
    private var settingsHintJob: kotlinx.coroutines.Job? = null
    private var loadedSource = AppListSource.NEEDS_ACCESS
    private var privileged by mutableStateOf(false)
    private var ctxTask by mutableStateOf<RecentTask?>(null) // long-press context menu target
    private var ctxSelected by mutableIntStateOf(0)
    private var longPressFired = false
    private var centerHeld = false
    private var ignoreCenterUntilUp = false
    private var longPressJob: kotlinx.coroutines.Job? = null
    private var pendingLocalDismiss = false
    private var busy = false                                  // guards async transitions

    private val openTrace = com.websnake.recentstv.data.LoadTrace.begin("open")
    private var reloadJob: kotlinx.coroutines.Job? = null
    private var actionJob: kotlinx.coroutines.Job? = null

    // onResume already loads the list. During a load, the final access check handles
    // binder changes; restarting here would duplicate blocking queries on cold/warm opens.
    private fun reloadForAccessChange() {
        if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) && scene != Scene.LOADING) reload()
    }
    private val permListener = Shizuku.OnRequestPermissionResultListener { _, _ -> runOnUiThread { reloadForAccessChange() } }
    private val binderReceived = Shizuku.OnBinderReceivedListener { runOnUiThread { reloadForAccessChange() } }
    private val binderDead = Shizuku.OnBinderDeadListener { runOnUiThread { privileged = false; reloadForAccessChange() } }

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
                if (scene == Scene.OVERLAY && openTrace != null) {
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        androidx.compose.runtime.withFrameNanos { }
                        openTrace.mark("visible")
                    }
                }
                val scrimColor = settings.bgTint.color
                val scrimAlpha = settings.bgDim / 100f
                when (scene) {
                    Scene.LOADING -> Scrim(scrimColor, scrimAlpha)
                    Scene.USAGE -> UsageAccessState(onGrant = ::openUsageAccess)
                    Scene.EMPTY -> Box(Modifier.fillMaxSize()) { Scrim(scrimColor, scrimAlpha); EmptyState() }
                    Scene.OVERLAY -> Box(Modifier.fillMaxSize()) {
                        Scrim(scrimColor, scrimAlpha)
                        if (tasks.isEmpty()) EmptyState(guidance = "↑ Controls     BACK Exit", tasksView = loadedSource == AppListSource.TASKS, compact = hasRowSwitch())
                        RecentsOverlay(
                            items = buildItems(),
                            restoredPackages = restoredPackages,
                            restoreRevision = restoreRevision,
                            focus = focus,
                            gearFocused = gearFocused,
                            viewFocused = viewFocused,
                            restoreFocused = restoreFocused,
                            showTopRestore = hasTopRestore() && (!settings.hideTopRestore || topControlsFocused()),
                            edgeHelp = edgeControlHint(),
                            showViewSwitch = hasViewSwitch() && (!settings.hideTopSwitch || topControlsFocused()),
                            showingTasks = loadedSource == AppListSource.TASKS,
                            showEdgeSwitchHint = !edgeMenuOpen && !busy && hasEdgeSwitch() && !topControlsFocused() && atSwitchEdge() && (effectiveShortcut() == EdgeShortcut.ONE_PRESS || edgeArmed),
                            edgePointsRight = edgeKey() == KeyEvent.KEYCODE_DPAD_RIGHT,
                            edgeBothDirections = settings.rowSwitchPosition == RowSwitchPosition.BOTH && itemCount() <= 1,
                            edgeConfirmation = effectiveShortcut() == EdgeShortcut.TWO_PRESSES,
                            rowVisible = rowVisible,
                            viewRevision = viewRevision,
                            viewNotice = viewNotice,
                            edgeActionLabel = edgeActionLabel(),
                            viewNoticeVisible = viewNoticeVisible,
                            topKillFocused = topKillFocused,
                            settingsHintVisible = settingsHintVisible,
                            showTopKillAll = hasTopKillAll() && (!settings.hideTopBulk || topControlsFocused()),
                            closeHintStyle = settings.closeHintStyle,
                            protectedApps = settings.protectedApps,
                            closingPackages = bulkAnimatingPackages,
                            bulkDismiss = resolvedBulkAction() == DownAction.DISMISS,
                            dismissLabel = taskAt(focus)?.let { resolvedDownAction(it) == DownAction.DISMISS } == true,
                            showTaskIndicator = settings.showTaskIndicator && privileged,
                            protectionPending = protectionEvent > protectionConsumedEvent, onProtectionHandled = { protectionConsumedEvent = it },
                            protectionEvent = protectionEvent, protectionTaskId = protectionTaskId, protectionMessage = protectionMessage,
                            protectionAnimated = settings.protectionFeedback.animate,
                            peekDir = peekDir,
                            dismissing = dismissing,
                            killing = killing,
                            size = settings.size,
                            shape = settings.shape,
                            cardPadding = settings.cardPadding,
                            taskCount = tasks.size,
                            position = settings.position,
                            direction = settings.direction,
                            showHeader = settings.headerMode.visible(privileged),
                            showSettingsButton = settings.showSettingsButton,
                            showHelp = settings.showHelp,
                            showAppNames = settings.showAppNames,
                            appTextPosition = settings.appTextPosition,
                            showAppDetails = settings.appDetailsMode.visible(privileged),
                            smoothMotion = settings.smoothMotion,
                            animationSpeed = settings.animationSpeed,
                            closeAnimationMs = settings.animationSpeed.duration(240),
                            onDismissComplete = ::onCardFlewOff,
                        )
                        if (restorePrompt) com.websnake.recentstv.ui.overlay.RestoreDismissedConfirmation(restoreConfirm, restorePreviewCount, restorePreview, restorePreviewLoading)
                        if (edgeMenuOpen) com.websnake.recentstv.ui.overlay.EdgeActionMenu(edgeMenuItems().map { it.label }, edgeMenuSelected, edgeMenuRight, settings.smoothMotion)
                        ctxTask?.let { com.websnake.recentstv.ui.overlay.ContextMenu(it, ctxSelected, taskForAction(it) != null, isIndividuallyProtected(it), it.packageName in settings.protectedApps, settings.excludeHiddenFromKillAll, canDismissTask(it), showContextRestore(), settings.dismissedApps.size) }
                        if (killPrompt) com.websnake.recentstv.ui.overlay.KillAllConfirmation(
                            bulkCandidates(promptBulkAction).distinctBy { it.packageName }.size, bulkTasks.filter { it.packageName in settings.protectedApps }.distinctBy { it.packageName }.size,
                            settings.forceStopOnKill, confirmKill, promptBulkAction == DownAction.DISMISS,
                            bulkCandidates(promptBulkAction).distinctBy { it.packageName }.take(8).map { com.websnake.recentstv.ui.overlay.PreviewApp(it.label, it.icon) },
                            bulkTasks.filter { it.packageName in settings.protectedApps }.distinctBy { it.packageName }.take(8).map { com.websnake.recentstv.ui.overlay.PreviewApp(it.label, it.icon) })
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.websnake.recentstv.updates.UpdateManager.onResume(this)
        temporaryListMode = null
        edgeMenuOpen = false; restorePrompt = false; restorePreviewJob?.cancel()
        clearEdgeArm()
        viewNoticeJob?.cancel(); viewNoticeVisible = false; viewNotice = null
        reload()
        permissionWatch?.cancel()
        permissionWatch = lifecycleScope.launch {
            while (true) {
                delay(1000)
                val ready = repo.ready()
                val allowed = repo.hasUsageAccess()
                if (scene != Scene.LOADING && !busy && (ready != privileged || ListSourcePolicy.resolve(temporaryListMode ?: settings.listMode, ready, allowed) != loadedSource)) reload()
            }
        }
    }

    private var permissionWatch: kotlinx.coroutines.Job? = null

    override fun onPause() {
        clearEdgeArm()
        viewNoticeJob?.cancel(); viewNoticeVisible = false; viewNotice = null
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

    private fun reload(origin: SwitchOrigin? = null, anchor: RowSwitchPosition? = null, restoring: Boolean = false) {
        val oldPackages = tasks.mapTo(HashSet()) { it.packageName }
        val previousPackage = if (origin != null || restoring) taskAt(focus)?.packageName else null
        val inPlace = (origin != null || restoring) && scene == Scene.OVERLAY
        val startedAt = android.os.SystemClock.elapsedRealtime()
        val loadTrace = com.websnake.recentstv.data.LoadTrace.begin("reload")
        if (!inPlace) { bulkTasks = emptyList(); scene = Scene.LOADING; rowVisible = true }
        else { busy = true; rowVisible = restoring || !settings.smoothMotion }
        clearEdgeArm()
        clearSettingsHint()
        killPrompt = false
        cancelCenterPress()
        actionJob?.cancel()
        reloadJob?.cancel()
        reloadJob = lifecycleScope.launch {
            settings = withContext(Dispatchers.IO) { store.settings.first() }
            loadTrace?.mark("settings")
            repo.privilegedEnabled = settings.usePrivileged
            val hadAccess = repo.ready()
            if (!inPlace) { privileged = hadAccess; restoreFocused = false; topKillFocused = false; viewFocused = false }
            killing = false; dismissing = null; peekDir = -1; ctxTask = null
            pendingLocalDismiss = false
            bulkAnimatingPackages = emptySet()
            val source = ListSourcePolicy.resolve(temporaryListMode ?: settings.listMode, hadAccess, repo.hasUsageAccess())
            if (source == AppListSource.NEEDS_ACCESS) {
                privileged = hadAccess; loadedSource = source; tasks.clear(); busy = false; rowVisible = true; scene = Scene.USAGE; return@launch
            }
            val history = source == AppListSource.HISTORY
            val loaded = withContext(Dispatchers.IO) {
                loadTrace?.mark("tasks-start")
                val actual = if (hadAccess) repo.getRecentTasks(emptySet()) else emptyList()
                loadTrace?.mark("tasks-end")
                val refreshDismissal = settings.enableDismiss && DismissPolicy.applies(settings.dismissScope, source) && settings.dismissedApps.isNotEmpty()
                val foreground = if (history || refreshDismissal)
                    runCatching { repo.getForegroundUsage() }.getOrDefault(emptyMap()) else emptyMap()
                loadTrace?.mark("history-end")
                val refreshed = if (refreshDismissal && DismissPolicy.remaining(settings.dismissedApps, foreground) != settings.dismissedApps) {
                    store.refreshDismissed(foreground)
                    store.settings.first()
                } else settings
                val entries = if (history) runCatching { repo.getRecentApps(refreshed.exceptions, foreground) }.getOrDefault(emptyList()).map { entry ->
                    entry.copy(matchedTaskId = actual.firstOrNull { it.packageName == entry.packageName }?.taskId)
                } else actual
                loadTrace?.mark("enrichment-end")
                Triple(actual, entries, refreshed)
            }
            val currentAccess = repo.ready()
            if (hadAccess != currentAccess || ListSourcePolicy.resolve(temporaryListMode ?: settings.listMode, currentAccess, repo.hasUsageAccess()) != source) { reload(); return@launch }
            if (inPlace && !restoring && settings.smoothMotion) {
                val remaining = settings.animationSpeed.duration(170).toLong() - (android.os.SystemClock.elapsedRealtime() - startedAt)
                if (remaining > 0) delay(remaining)
            }
            settings = loaded.third; privileged = hadAccess; loadedSource = source; bulkTasks = loaded.first
            val newEntries = loaded.second.filterNot {
                it.packageName in loaded.third.exceptions || (loaded.third.enableDismiss && DismissPolicy.applies(loaded.third.dismissScope, source) && it.packageName in loaded.third.dismissedApps)
            }
            restoreArrivalJob?.cancel()
            restoredPackages = if (restoring && settings.smoothMotion) newEntries.mapTo(HashSet()) { it.packageName } - oldPackages else emptySet()
            if (restoring) restoreRevision++
            tasks.clear(); tasks.addAll(loaded.second.filterNot {
                it.packageName in settings.exceptions || (settings.enableDismiss && DismissPolicy.applies(settings.dismissScope, source) && it.packageName in settings.dismissedApps)
            })
            killing = false; dismissing = null; peekDir = -1; gearFocused = false; restoreFocused = false; topKillFocused = false
            ctxTask = null; longPressFired = false; busy = false
            viewFocused = origin == SwitchOrigin.TOP && hasViewSwitch()
            if (tasks.isEmpty()) showEmptyView() else {
                scene = Scene.OVERLAY
                val retained = if (origin == SwitchOrigin.TOP || restoring) buildItems().indexOfFirst { it is OverlayItem.Card && it.task.packageName == previousPackage } else -1
                focus = if (retained >= 0) retained else defaultFocus()
            }
            if (origin == SwitchOrigin.ROW && hasRowSwitch()) { viewFocused = false; focus = buildItems().indexOfFirst { it is OverlayItem.ViewSwitch && it.side == (anchor ?: RowSwitchPosition.START) }.coerceAtLeast(0) }
            if (origin == SwitchOrigin.EDGE) { viewFocused = false; focus = ViewSwitchPolicy.edgeIndex(itemCount(), anchor ?: settings.rowSwitchPosition) }
            if (origin != null || (restoring && oldPackages.isEmpty())) viewRevision++
            rowVisible = true
            loadTrace?.mark("ready")
            if (restoredPackages.isNotEmpty()) restoreArrivalJob = lifecycleScope.launch { delay(settings.animationSpeed.duration(300).toLong() + 150L); restoredPackages = emptySet() }
            if (origin != null && settings.viewNoticeStyle != com.websnake.recentstv.data.ViewNoticeStyle.OFF) {
                viewNoticeJob?.cancel()
                viewNotice = ViewNoticeEvent(++noticeSequence, if (source == AppListSource.TASKS) "Tasks" else "Recent apps", settings.viewNoticeStyle)
                viewNoticeVisible = true
                viewNoticeJob = lifecycleScope.launch { delay(settings.viewNoticeStyle.durationMs); viewNoticeVisible = false }
            }
        }
    }

    private fun buildItems(): List<OverlayItem> {
        val list = ArrayList<OverlayItem>()
        if (hasBulkAction() && settings.killAllLocation.inRow) list.add(OverlayItem.KillAll)
        tasks.forEach { list.add(OverlayItem.Card(it)) }
        if (hasRowSwitch()) {
            if (settings.rowSwitchPosition != RowSwitchPosition.END) list.add(0, OverlayItem.ViewSwitch(RowSwitchPosition.START))
            if (settings.rowSwitchPosition != RowSwitchPosition.START) list.add(OverlayItem.ViewSwitch(RowSwitchPosition.END))
        }
        return list
    }
    private fun defaultFocus() = buildItems().indexOfFirst { it is OverlayItem.Card }.coerceAtLeast(0)
    private fun itemCount(): Int = buildItems().size
    private fun isKillAllAt(i: Int) = buildItems().getOrNull(i) == OverlayItem.KillAll
    private fun isRowSwitchAt(i: Int) = buildItems().getOrNull(i) is OverlayItem.ViewSwitch
    private fun taskAt(i: Int): RecentTask? = (buildItems().getOrNull(i) as? OverlayItem.Card)?.task

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

        if (restorePrompt) {
            if (!down || event.repeatCount > 0) return true
            when (code) {
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> restorePrompt = false
                KeyEvent.KEYCODE_DPAD_LEFT -> restoreConfirm = false
                KeyEvent.KEYCODE_DPAD_RIGHT -> restoreConfirm = true
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A -> {
                    restorePrompt = false
                    if (restoreConfirm) restoreDismissedFromMenu()
                }
            }
            return true
        }
        if (edgeMenuOpen) {
            if (!down || event.repeatCount > 0) return true
            val choices = edgeMenuItems()
            if (choices.isEmpty()) {
                if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_ESCAPE ||
                    code == if (edgeMenuRight) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT) edgeMenuOpen = false
                return true
            }
            edgeMenuSelected = edgeMenuSelected.coerceIn(0, choices.lastIndex)
            when (code) {
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> edgeMenuOpen = false
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> if ((code == KeyEvent.KEYCODE_DPAD_LEFT) == edgeMenuRight) edgeMenuOpen = false
                KeyEvent.KEYCODE_DPAD_UP -> edgeMenuSelected = (edgeMenuSelected - 1).coerceAtLeast(0)
                KeyEvent.KEYCODE_DPAD_DOWN -> edgeMenuSelected = (edgeMenuSelected + 1).coerceAtMost(choices.lastIndex)
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A -> {
                    val choice = choices[edgeMenuSelected]; edgeMenuOpen = false; cancelCenterPress()
                    when (choice) {
                        EdgeMenuItem.SWITCH_VIEW -> switchView(SwitchOrigin.EDGE)
                        EdgeMenuItem.CLOSE_ALL -> killAll(DownAction.CLOSE_TASK)
                        EdgeMenuItem.DISMISS_ALL -> killAll(DownAction.DISMISS)
                        EdgeMenuItem.RESTORE_DISMISSED -> requestRestore()
                        EdgeMenuItem.SETTINGS -> openSettings()
                    }
                }
            }
            return true
        }
        if (killPrompt) {
            if (isCenter && !down) { ignoreCenterUntilUp = false; return true }
            if (!down || (isCenter && (event.repeatCount > 0 || ignoreCenterUntilUp))) return true
            when (code) {
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> { killPrompt = false; cancelCenterPress(); restoreTopSettingsHint() }
                KeyEvent.KEYCODE_DPAD_LEFT -> confirmKill = false
                KeyEvent.KEYCODE_DPAD_RIGHT -> confirmKill = bulkCandidates(promptBulkAction).isNotEmpty()
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
                else if (gearFocused || topKillFocused || viewFocused || restoreFocused) { gearFocused = false; restoreFocused = false; topKillFocused = false; viewFocused = false }
                else finish()
                return true
            }
            KeyEvent.KEYCODE_MENU -> { cancelCenterPress(); openSettings(); return true }
        }
        // Reaching the edge while holding RIGHT must never activate the shortcut.
        if (down && isEdgeKey(code) && event.repeatCount > 0 && scene == Scene.OVERLAY &&
            !topControlsFocused() && hasEdgeSwitch() && atSwitchEdge()) return true
        if (down && !isEdgeKey(code)) clearEdgeArm()
        // Hidden settings access requires a deliberate second gesture, not a held UP repeat.
        if (down && code == KeyEvent.KEYCODE_DPAD_UP && event.repeatCount > 0) return true
        if (down && code != KeyEvent.KEYCODE_DPAD_UP && !topKillFocused && !viewFocused) clearSettingsHint()

        // Consume the entire press that opened the menu, including repeated downs and
        // its release. Choosing a menu action always requires a fresh OK press.
        if (isCenter && down && event.repeatCount > 0) {
            if (centerHeld && ctxTask == null && !topControlsFocused() && !isKillAllAt(focus))
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
        if (scene == Scene.OVERLAY && isCenter && !topControlsFocused() && !busy && !killing && dismissing == null) {
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
                KeyEvent.KEYCODE_DPAD_UP -> openSettings()
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

    private fun showContextRestore(): Boolean {
        val edgeRestore = hasEdgeSwitch() && (effectiveEdgeAction() == EdgeAction.RESTORE ||
            (effectiveEdgeAction() == EdgeAction.MENU && EdgeMenuItem.RESTORE_DISMISSED in edgeMenuItems()))
        return RestoreAccessPolicy.showInContext(settings.contextRestoreMode, settings.dismissedApps.isNotEmpty(), hasTopRestore(), edgeRestore)
    }
    private fun contextActions(task: RecentTask): List<Int> = ActionPolicy.context(
        canDismissTask(task), taskForAction(task) != null && !isIndividuallyProtected(task), taskForAction(task) != null, showContextRestore())

    private fun handleCtxKey(code: Int) {
        val task = ctxTask ?: return
        val actions = contextActions(task)
        val index = actions.indexOf(ctxSelected).coerceAtLeast(0)
        when (code) {
            KeyEvent.KEYCODE_DPAD_UP -> ctxSelected = actions[(index - 1).coerceAtLeast(0)]
            KeyEvent.KEYCODE_DPAD_DOWN -> ctxSelected = actions[(index + 1).coerceAtMost(actions.lastIndex)]
            KeyEvent.KEYCODE_DPAD_LEFT -> ctxTask = null
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A ->
                if (ctxSelected in actions) executeCtx(task, ctxSelected) else ctxSelected = actions.first()
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
        ctxSelected = contextActions(task).first()
        ctxTask = task
    }

    private fun showEmptyView() {
        focus = 0
        topKillFocused = false
        gearFocused = false
        viewFocused = !recoveryActive() && hasViewSwitch()
        restoreFocused = !recoveryActive() && !hasViewSwitch() && hasTopRestore()
        scene = if (hasAnyViewSwitch()) Scene.OVERLAY else Scene.EMPTY
    }

    private fun removeFromView(task: RecentTask) {
        val idx = tasks.indexOfFirst { it.taskId == task.taskId }
        if (idx >= 0) tasks.removeAt(idx)
        if (tasks.isEmpty()) showEmptyView() else focus = focus.coerceIn(0, itemCount() - 1)
    }

    private fun executeCtx(task: RecentTask, option: Int) {
        ctxTask = null
        when (option) {
            5 -> requestRestore()
            0 -> {                                    // add to exceptions
                lifecycleScope.launch {
                    store.toggleException(task.packageName)
                    settings = store.settings.first()
                    removeFromView(task)
                }
            }
            4 -> {
                if (!canDismissTask(task)) return
                if (!repo.hasUsageAccess()) { requestDismissAccess(); return }
                pendingLocalDismiss = true
                dismissing = task.taskId to 1
            }
            1 -> {                                    // close actual task, never a synthetic usage ID
                val actual = taskForAction(task) ?: return
                if (!repo.ready()) { reload(); return }
                if (isIndividuallyProtected(task)) { showProtectedNotice(); return }
                removeFromView(task)
                bulkTasks = bulkTasks.filterNot { it.packageName == task.packageName }
                val fs = settings.forceStopOnKill
                val protectedPackages = individualProtectionPackages()
                actionJob = lifecycleScope.launch { withContext(Dispatchers.IO) { repo.dismiss(actual, fs, protectedPackages) } }
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
        if (topControlsFocused()) {
            val controls = buildList {
                if (hasTopKillAll()) add(0)
                if (hasTopRestore()) add(3)
                if (hasViewSwitch()) add(1)
                if (settings.showSettingsButton) add(2)
            }
            val selected = if (topKillFocused) 0 else if (restoreFocused) 3 else if (viewFocused) 1 else 2
            fun select(id: Int?) {
                topKillFocused = id == 0; restoreFocused = id == 3; viewFocused = id == 1; gearFocused = id == 2
            }
            when (code) {
                KeyEvent.KEYCODE_DPAD_DOWN -> { select(null); clearSettingsHint() }
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    val index = controls.indexOf(selected)
                    if (index >= 0) select(controls[(index + if (code == KeyEvent.KEYCODE_DPAD_LEFT) -1 else 1).coerceIn(0, controls.lastIndex)])
                }
                KeyEvent.KEYCODE_DPAD_UP -> if (!settings.showSettingsButton) openSettings()
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A -> when (selected) {
                    0 -> killAll()
                    3 -> requestRestore()
                    1 -> switchView()
                    2 -> openSettings()
                }
            }
            return
        }
        when (code) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                peekDir = -1
                if (hasEdgeSwitch() && isEdgeKey(code) && atSwitchEdge()) edgeSwitchStep(code)
                else { clearEdgeArm(); moveFocus(if ((code == KeyEvent.KEYCODE_DPAD_RIGHT) == (settings.direction == com.websnake.recentstv.data.AppDirection.OLDER_RIGHT)) +1 else -1) }
            }
            KeyEvent.KEYCODE_DPAD_UP ->
                if (peekDir == 1) peekDir = -1          // cancel a pending peek → card back to normal
                else requestSettingsAccess()
            KeyEvent.KEYCODE_DPAD_DOWN -> if (!isKillAllAt(focus)) dismissStep(1)  // DOWN → dismiss/kill flow
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A -> activate()
        }
    }

    private fun topControlsFocused() = gearFocused || topKillFocused || viewFocused || restoreFocused
    private fun canDismiss() = settings.enableDismiss && DismissPolicy.applies(settings.dismissScope, loadedSource)
    private fun resolvedBulkAction(checkAccess: Boolean = false) = ActionPolicy.bulkForAccess(settings.bulkWithAccess, settings.bulkWithoutAccess, if (checkAccess) repo.ready() else privileged, canDismiss(), loadedSource)
    private fun hasBulkAction() = resolvedBulkAction() != DownAction.OFF && tasks.isNotEmpty() && (resolvedBulkAction() != DownAction.DISMISS || tasks.any { dismissAllowed(it) })
    private fun hasTopKillAll() = hasBulkAction() && settings.killAllLocation.inTop

    private fun hasViewSwitch() = settings.viewButtons.top && privileged
    private fun hasRowSwitch() = settings.viewButtons.row && privileged
    private fun hasTopRestore() = settings.restoreTopMode.visible(loadedSource) && settings.dismissedApps.isNotEmpty()
    private fun recoveryActive() = settings.emptyListRecovery != EmptyListRecovery.OFF && canDismiss() && tasks.isEmpty() && settings.dismissedApps.isNotEmpty()
    private fun effectiveShortcut() = EdgeActivationPolicy.resolve(settings.edgeShortcut, buildItems().any { it is OverlayItem.KillAll || it is OverlayItem.ViewSwitch }, recoveryActive())
    private fun effectiveEdgeAction() = if (recoveryActive()) {
        if (settings.emptyListRecovery == EmptyListRecovery.RESTORE) EdgeAction.RESTORE else EdgeAction.MENU
    } else EdgeActionPolicy.resolve(settings.edgeAction, settings.edgeWithoutAccess, privileged)
    private fun edgeControlHint(): String? {
        if (!settings.showEdgeHints || !hasEdgeSwitch()) return null
        val direction = if (settings.rowSwitchPosition == RowSwitchPosition.BOTH) "|← / →|" else if (edgeKey() == KeyEvent.KEYCODE_DPAD_RIGHT) "→|" else "|←"
        return "$direction ${edgeActionLabel()}"
    }
    private fun hasEdgeSwitch() = effectiveShortcut() != EdgeShortcut.OFF && when (effectiveEdgeAction()) {
        EdgeAction.SWITCH_VIEW -> privileged
        EdgeAction.CLOSE_OR_DISMISS -> privileged || canDismiss()
        EdgeAction.DISMISS_ALL -> canDismiss()
        EdgeAction.MENU -> true
        EdgeAction.RESTORE -> settings.dismissedApps.isNotEmpty()
        EdgeAction.OFF -> false
    }
    private fun edgeMenuItems() = EdgeMenuPolicy.available(if (recoveryActive() && settings.emptyListRecovery == EmptyListRecovery.MENU) EdgeMenuPolicy.recoverySelection(settings.edgeMenuItems) else settings.edgeMenuItems, privileged, canDismiss(), settings.dismissedApps.isNotEmpty(), killAllCandidates().isNotEmpty(), tasks.any { dismissAllowed(it) })
    private fun edgeActionLabel() = when (effectiveEdgeAction()) {
        EdgeAction.SWITCH_VIEW -> if (loadedSource == AppListSource.TASKS) "Recent apps" else "Tasks"
        EdgeAction.CLOSE_OR_DISMISS -> if (privileged) "Close all" else "Dismiss all"
        EdgeAction.DISMISS_ALL -> "Dismiss all"
        EdgeAction.MENU -> "Actions"
        EdgeAction.RESTORE -> "Restore apps"
        EdgeAction.OFF -> "Off"
    }
    private fun hasAnyViewSwitch() = hasViewSwitch() || hasRowSwitch() || hasEdgeSwitch() || hasTopRestore()
    private fun atSwitchEdge() = ViewSwitchPolicy.atEdge(focus, itemCount(), settings.rowSwitchPosition)
    private fun edgeKey() = if (ViewSwitchPolicy.edgeIsRight(armedEdgeSide ?: ViewSwitchPolicy.sideAt(focus, itemCount(), settings.rowSwitchPosition), settings.direction == com.websnake.recentstv.data.AppDirection.OLDER_RIGHT)) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
    private fun isEdgeKey(code: Int) = if (settings.rowSwitchPosition == RowSwitchPosition.BOTH && itemCount() <= 1) code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT else code == edgeKey()
    private fun clearEdgeArm() { armedEdgeSide = null; edgeArmJob?.cancel(); edgeArmJob = null; edgeArmed = false }
    private fun edgeSwitchStep(code: Int) {
        val side = if ((code == KeyEvent.KEYCODE_DPAD_RIGHT) == (settings.direction == com.websnake.recentstv.data.AppDirection.OLDER_LEFT)) RowSwitchPosition.START else RowSwitchPosition.END
        if (effectiveShortcut() == EdgeShortcut.TWO_PRESSES && (!edgeArmed || armedEdgeSide != side)) {
            armedEdgeSide = side
            edgeArmed = true
            edgeArmJob?.cancel()
            edgeArmJob = lifecycleScope.launch { delay(3000); edgeArmed = false; armedEdgeSide = null }
        } else { clearEdgeArm(); when (effectiveEdgeAction()) {
            EdgeAction.SWITCH_VIEW -> switchView(SwitchOrigin.EDGE, side)
            EdgeAction.CLOSE_OR_DISMISS -> killAll(if (privileged) DownAction.CLOSE_TASK else DownAction.DISMISS)
            EdgeAction.DISMISS_ALL -> killAll(DownAction.DISMISS)
            EdgeAction.MENU -> { edgeMenuRight = code == KeyEvent.KEYCODE_DPAD_RIGHT; cancelCenterPress(); edgeMenuSelected = if (recoveryActive()) edgeMenuItems().indexOf(EdgeMenuItem.RESTORE_DISMISSED).coerceAtLeast(0) else 0; edgeMenuOpen = true }
            EdgeAction.RESTORE -> requestRestore()
            EdgeAction.OFF -> Unit
        } }
    }

    private fun requestRestore() {
        if (busy || settings.dismissedApps.isEmpty()) return
        if (settings.confirmRestoreDismissed) {
            restoreConfirm = false
            val packages = settings.dismissedApps.keys.toList()
            restorePreviewCount = packages.size; restorePreview = emptyList(); restorePreviewLoading = true
            restorePrompt = true
            restorePreviewJob?.cancel()
            restorePreviewJob = lifecycleScope.launch {
                try {
                    restorePreview = withContext(Dispatchers.IO) { packages.take(8).map { pkg ->
                        runCatching {
                            val app = packageManager.getApplicationInfo(pkg, 0)
                            com.websnake.recentstv.ui.overlay.PreviewApp(app.loadLabel(packageManager).toString(), app.loadIcon(packageManager))
                        }.getOrElse { com.websnake.recentstv.ui.overlay.PreviewApp(pkg.substringAfterLast('.'), null) }
                    } }
                } finally { restorePreviewLoading = false }
            }
        } else restoreDismissedFromMenu()
    }
    private fun restoreDismissedFromMenu() {
        if (busy) return
        busy = true
        lifecycleScope.launch {
            try { store.restoreDismissed(); settings = store.settings.first() }
            catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.widget.Toast.makeText(this@RecentsActivity, "Could not restore dismissed apps", android.widget.Toast.LENGTH_SHORT).show()
            }
            finally { busy = false }
            reload(restoring = true)
        }
    }
    private fun switchView(origin: SwitchOrigin = SwitchOrigin.TOP, edgeSide: RowSwitchPosition? = null) {
        if (!hasAnyViewSwitch() || busy) return
        if (!repo.ready()) { reload(); return }
        val anchor = when (origin) {
            SwitchOrigin.ROW -> (buildItems().getOrNull(focus) as? OverlayItem.ViewSwitch)?.side
            SwitchOrigin.EDGE -> edgeSide ?: ViewSwitchPolicy.sideAt(focus, itemCount(), settings.rowSwitchPosition)
            else -> null
        }
        val target = if (loadedSource == AppListSource.HISTORY) AppListMode.ANDROID_TASKS else AppListMode.RECENTLY_USED
        if (target == AppListMode.RECENTLY_USED && !repo.hasUsageAccess()) {
            android.widget.Toast.makeText(this, "Usage Access is needed for Recent apps", android.widget.Toast.LENGTH_SHORT).show()
            openUsageAccess(); return
        }
        temporaryListMode = target
        cancelCenterPress()
        clearSettingsHint()
        if (settings.rememberView) {
            busy = true
            lifecycleScope.launch {
                try {
                    store.setListMode(target)
                    settings = store.settings.first()
                    temporaryListMode = null
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    android.widget.Toast.makeText(this@RecentsActivity, "Could not save preferred view", android.widget.Toast.LENGTH_SHORT).show()
                } finally { busy = false }
                reload(origin, anchor)
            }
        } else reload(origin, anchor)
    }

    private fun requestSettingsAccess() {
        restoreFocused = false
        if (settings.showSettingsButton) {
            clearSettingsHint()
            gearFocused = true; topKillFocused = false; viewFocused = false
        }
        else if (hasViewSwitch()) { viewFocused = true; topKillFocused = false; gearFocused = false; showSettingsHint(false) }
        else if (hasTopKillAll()) { topKillFocused = true; gearFocused = false; showSettingsHint(false) }
        else if (hasTopRestore()) { restoreFocused = true; topKillFocused = false; viewFocused = false; gearFocused = false; showSettingsHint(false) }
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
        if ((topKillFocused || viewFocused || restoreFocused) && !settings.showSettingsButton) showSettingsHint(false)
    }

    private fun moveFocus(d: Int) {
        val n = itemCount()
        focus = (focus + d).coerceIn(0, (n - 1).coerceAtLeast(0))
    }

    private fun taskForAction(task: RecentTask): RecentTask? =
        if (privileged) bulkTasks.firstOrNull { it.packageName == task.packageName } else null

    private fun requestDismissAccess() {
        android.widget.Toast.makeText(this, "Usage Access is needed to show dismissed apps again", android.widget.Toast.LENGTH_LONG).show()
        openUsageAccess()
    }

    private fun dismissAllowed(task: RecentTask) = settings.allowDismissProtected || task.packageName !in settings.protectedApps
    private fun canDismissTask(task: RecentTask) = canDismiss() && dismissAllowed(task)

    private fun dismissLocally(task: RecentTask) {
        if (!dismissAllowed(task)) { dismissing = null; peekDir = -1; showProtectedNotice(true); return }
        if (!canDismiss() || !repo.hasUsageAccess()) { dismissing = null; requestDismissAccess(); return }
        busy = true
        actionJob = lifecycleScope.launch {
            try {
                val foreground = withContext(Dispatchers.IO) { repo.getForegroundUsage() }
                if (!repo.hasUsageAccess()) { requestDismissAccess(); return@launch }
                store.dismissFromList(task.packageName, foreground[task.packageName] ?: 0L)
                settings = store.settings.first()
                removeFromView(task)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.widget.Toast.makeText(this@RecentsActivity, "Could not dismiss app", android.widget.Toast.LENGTH_SHORT).show()
                reload()
            } finally { busy = false; dismissing = null; peekDir = -1 }
        }
    }

    private fun resolvedDownAction(task: RecentTask): DownAction {
        val action = ActionPolicy.downForAccess(settings.downWithAccess, settings.downWithoutAccess, privileged && repo.ready(), taskForAction(task) != null, canDismiss())
        return ActionPolicy.protectedDown(action, isIndividuallyProtected(task), dismissAllowed(task), canDismiss())
    }

    private fun dismissStep(dir: Int) {
        val target = taskAt(focus) ?: return
        val action = resolvedDownAction(target)
        if (action == DownAction.OFF) {
            val intended = ActionPolicy.downForAccess(settings.downWithAccess, settings.downWithoutAccess, privileged && repo.ready(), taskForAction(target) != null, canDismiss())
            if (intended == DownAction.CLOSE_TASK && isIndividuallyProtected(target)) showProtectedNotice()
            else if (intended == DownAction.DISMISS && !dismissAllowed(target)) showProtectedNotice(true)
            return
        }
        val local = action == DownAction.DISMISS
        if (!local && !repo.ready()) { reload(); return }
        if (local && !repo.hasUsageAccess()) { requestDismissAccess(); return }
        if (isKillAllAt(focus)) return
        if (!local && taskAt(focus)?.let(::isIndividuallyProtected) == true) { peekDir = -1; showProtectedNotice(); return }
        if (dismissing != null) return
        if (peekDir == dir) {
            // stage 2 — fly off & remove
            val task = taskAt(focus) ?: return
            peekDir = -1
            pendingLocalDismiss = local
            dismissing = task.taskId to dir
        } else {
            peekDir = dir                              // stage 1 — peek
        }
    }

    private fun onCardFlewOff(taskId: Int) {
        val local = pendingLocalDismiss
        pendingLocalDismiss = false
        val idx = tasks.indexOfFirst { it.taskId == taskId }
        if (idx < 0) { dismissing = null; return }
        val task = tasks[idx]
        if (local) { dismissLocally(task); return }
        if (!repo.ready()) { reload(); return }
        val actual = taskForAction(task) ?: run { reload(); return }
        if (isIndividuallyProtected(task)) { dismissing = null; peekDir = -1; showProtectedNotice(); return }
        // Remove from the UI immediately so the row collapses with no leftover gap;
        // the privileged removeTask runs in the background.
        tasks.removeAt(idx)
        bulkTasks = bulkTasks.filterNot { it.packageName == task.packageName }
        dismissing = null
        peekDir = -1
        if (tasks.isEmpty()) showEmptyView()
        else focus = focus.coerceIn(0, itemCount() - 1)
        val fs = settings.forceStopOnKill
        val protectedPackages = individualProtectionPackages()
        actionJob = lifecycleScope.launch { withContext(Dispatchers.IO) { repo.dismiss(actual, fs, protectedPackages) } }
    }

    private fun activate() {
        if (isRowSwitchAt(focus)) switchView(SwitchOrigin.ROW)
        else if (isKillAllAt(focus)) killAll()
        else taskAt(focus)?.let { switchTo(it) }
    }

    private fun switchTo(task: RecentTask) {
        if (busy) return
        busy = true
        actionJob = lifecycleScope.launch {
            val moved = withContext(Dispatchers.IO) { repo.switchTo(taskForAction(task) ?: task) }
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

    private fun killAll(requested: DownAction? = null) {
        if (busy) return
        val action = requested ?: resolvedBulkAction()
        promptBulkFromEdge = requested != null
        if (!bulkActionAvailable(action) || (requested == null && resolvedBulkAction(checkAccess = true) != action)) { reload(); return }
        if (action == DownAction.OFF) return
        if (action == DownAction.DISMISS && !repo.hasUsageAccess()) { requestDismissAccess(); return }
        promptBulkAction = action
        clearSettingsHint()
        if (!ActionPolicy.confirm(settings.bulkConfirmation, action)) { cancelCenterPress(); executeKillAll(); return }
        cancelCenterPress()
        ignoreCenterUntilUp = topKillFocused
        confirmKill = false
        killPrompt = true
    }

    private fun bulkCandidates(action: DownAction = resolvedBulkAction()): List<RecentTask> = when (action) {
        DownAction.DISMISS -> tasks.filter { dismissAllowed(it) }
        DownAction.CLOSE_TASK -> killAllCandidates()
        else -> emptyList()
    }

    private fun killAllCandidates() = KillAllPolicy.candidates(bulkTasks, settings.exceptions, settings.protectedApps, settings.excludeHiddenFromKillAll)

    private fun individualProtectionPackages() = if (settings.protectIndividualCloses) settings.protectedApps else emptySet()
    private fun isIndividuallyProtected(task: RecentTask) = task.packageName in individualProtectionPackages()
    private var protectedNoticeAt = 0L
    private fun showProtectedNotice(dismissal: Boolean = false) {
        if (settings.protectionFeedback == com.websnake.recentstv.data.ProtectionFeedback.OFF) return
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - protectedNoticeAt < 1000) return
        protectedNoticeAt = now
        protectionTaskId = taskAt(focus)?.taskId ?: Int.MIN_VALUE
        protectionMessage = if (settings.protectionFeedback.hint) { if (dismissal) "Protected from dismissal" else "Protected from closing" } else null
        protectionEvent++
        peekDir = -1
        protectionHintJob?.cancel()
        protectionHintJob = lifecycleScope.launch { delay(1500); protectionMessage = null }
    }

    private fun bulkActionAvailable(action: DownAction) = when (action) {
        DownAction.CLOSE_TASK -> repo.ready()
        DownAction.DISMISS -> canDismiss()
        else -> false
    }
    private fun executeKillAll() {
        if (busy) return
        val action = promptBulkAction
        // A confirmation never changes meaning if access disappears or arrives.
        if (!bulkActionAvailable(action) || (!promptBulkFromEdge && resolvedBulkAction(checkAccess = true) != action)) { reload(); return }
        if (action == DownAction.DISMISS && !repo.hasUsageAccess()) { requestDismissAccess(); return }
        val snapshot = bulkCandidates(action)
        if (snapshot.isEmpty()) {
            restoreTopSettingsHint()
            android.widget.Toast.makeText(this, if (action == DownAction.DISMISS) "No apps available to dismiss" else "No Android tasks to close", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        busy = true
        bulkAnimatingPackages = snapshot.mapTo(HashSet()) { it.packageName }
        killing = true
        val protectedPackages = settings.protectedApps
        val fs = settings.forceStopOnKill
        val startedAt = android.os.SystemClock.elapsedRealtime()
        actionJob = lifecycleScope.launch {
            try {
                if (action == DownAction.DISMISS) {
                    val foreground = withContext(Dispatchers.IO) { repo.getForegroundUsage() }
                    if (!repo.hasUsageAccess()) { reload(); return@launch }
                    store.dismissPackages(snapshot.mapTo(HashSet()) { it.packageName }, foreground)
                    settings = store.settings.first()
                } else withContext(Dispatchers.IO) { repo.killAll(snapshot, fs, protectedPackages) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.widget.Toast.makeText(this@RecentsActivity, "Could not complete bulk action", android.widget.Toast.LENGTH_SHORT).show()
                reload(); return@launch
            }
            val animationTime = if (settings.smoothMotion) settings.animationSpeed.duration(240).toLong() + (snapshot.size - 1).coerceAtLeast(0) * settings.animationSpeed.duration(20).toLong() else 0L
            val remaining = animationTime - (android.os.SystemClock.elapsedRealtime() - startedAt)
            if (remaining > 0) delay(remaining)
            val closedPackages = snapshot.mapTo(HashSet()) { it.packageName }
            tasks.removeAll { it.packageName in closedPackages }
            if (action == DownAction.CLOSE_TASK) bulkTasks = bulkTasks.filterNot { it.packageName in closedPackages }
            killing = false
            bulkAnimatingPackages = emptySet()
            busy = false
            if (tasks.isEmpty()) showEmptyView()
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






