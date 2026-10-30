package com.websnake.recentstv

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
import com.websnake.recentstv.data.PresetApplyMode
import com.websnake.recentstv.data.PresetChange
import com.websnake.recentstv.ui.settings.PresetReview
import androidx.compose.runtime.LaunchedEffect
import com.websnake.recentstv.data.AppearancePreset
import com.websnake.recentstv.ui.settings.SettingsMenu
import com.websnake.recentstv.ui.settings.AppearancePresetsScreen
import com.websnake.recentstv.ui.settings.ApplyPresetDialog
import com.websnake.recentstv.ui.settings.AboutScreen
import com.websnake.recentstv.ui.settings.aboutActions
import com.websnake.recentstv.ui.settings.updateActions
import com.websnake.recentstv.ui.settings.AboutAction
import com.websnake.recentstv.updates.UpdateManager
import com.websnake.recentstv.updates.UpdatePhase
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.websnake.recentstv.ui.settings.ExceptionsScreen
import com.websnake.recentstv.ui.settings.SetRow
import com.websnake.recentstv.ui.settings.SettingsSubmenu
import com.websnake.recentstv.ui.settings.SettingsScreen
import com.websnake.recentstv.ui.settings.SettingsHome
import com.websnake.recentstv.ui.settings.SettingsSection
import com.websnake.recentstv.ui.settings.AccentChart
import com.websnake.recentstv.ui.settings.AccentColorChart
import androidx.compose.ui.graphics.toArgb
import com.websnake.recentstv.ui.theme.AccentColor
import com.websnake.recentstv.ui.theme.BgTint
import com.websnake.recentstv.ui.theme.CardShapeOpt
import com.websnake.recentstv.ui.theme.CardSizeOpt
import com.websnake.recentstv.ui.theme.RecentsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SetScreen { SETTINGS, EXCEPTIONS, PROTECTED, PRESETS, ABOUT, UPDATES, SUBMENU }

class SettingsActivity : ComponentActivity() {

    private lateinit var store: SettingsStore
    private lateinit var pkgRepo: PackageRepository

    private var screen by mutableStateOf(SetScreen.SETTINGS)
    private var settings by mutableStateOf(RecentsSettings())
    private var setFocus by mutableIntStateOf(0)
    private var submenu by mutableStateOf<SettingsSubmenu?>(null)
    private val submenuBackStack = mutableListOf<Pair<SettingsSubmenu?, Int>>()
    private var section by mutableStateOf<SettingsSection?>(null)
    private var rootFocus by mutableIntStateOf(0)
    private var presetFocus by mutableIntStateOf(0)
    private var aboutFocus by mutableIntStateOf(0)
    private var updateFocus by mutableIntStateOf(0)
    private var installAllowed by mutableStateOf(false)
    private var presetPrompt by mutableStateOf(false)
    private var presetMode by mutableStateOf(PresetApplyMode.FULL)
    private var presetReviewFocus by mutableIntStateOf(0)
    private var presetDetails by mutableStateOf(false)
    private var presetDetailFocus by mutableIntStateOf(0)
    private var presetPreviews by mutableStateOf<Map<PresetApplyMode, List<PresetChange>>>(emptyMap())
    private val presetChanges get() = presetPreviews[presetMode]
    private var presetPreviewError by mutableStateOf(false)
    private var confirmPreset by mutableStateOf(false)
    private var canRestoreAppearance by mutableStateOf(false)
    private val apps = mutableStateListOf<InstalledApp>()
    private var excFocus by mutableIntStateOf(0)
    private var resetPrompt by mutableStateOf(false)
    private var confirmReset by mutableStateOf(false)
    private var colorPrompt by mutableStateOf(false)
    private var chartFocus by mutableIntStateOf(0)
    private var appsLoading by mutableStateOf(false)
    private var appsLoadError by mutableStateOf(false)
    private var appsLoadJob: kotlinx.coroutines.Job? = null
    private var appsLoadGeneration = 0
    private var managerStatus by mutableStateOf("Checking access")
    private var usageAccessGranted by mutableStateOf(true)
    private var managerReady by mutableStateOf<Boolean?>(null)
    private var managerLabel by mutableStateOf("Porter / Shizuku")
    private var statusJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UpdateManager.checkOnce(applicationContext)
        store = SettingsStore(applicationContext)
        pkgRepo = PackageRepository(applicationContext)
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val buildNumber = androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(packageInfo)
        val buildType = if (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) "Debug" else "Release"
        val version = "Version ${packageInfo.versionName} · Build $buildNumber · $buildType"
        lifecycleScope.launch {
            store.canRestoreAppearance.collect { canRestoreAppearance = it }
        }

        lifecycleScope.launch {
            store.settings.collect { updated ->
                val focusedPackage = apps.getOrNull(excFocus)?.packageName
                val focusedSetting = section?.let { currentRows(it).getOrNull(setFocus) }
                settings = updated
                if (focusedSetting != null) {
                    val rows = currentRows(section!!)
                    val retained = rows.indexOf(focusedSetting)
                    setFocus = if (retained >= 0) retained else setFocus.coerceIn(0, rows.lastIndex.coerceAtLeast(0))
                }
                if (apps.isNotEmpty()) {
                    val sorted = sortExceptions(apps, if (screen == SetScreen.PROTECTED) updated.protectedApps else updated.exceptions)
                    apps.clear(); apps.addAll(sorted)
                    excFocus = sorted.indexOfFirst { it.packageName == focusedPackage }.coerceAtLeast(0)
                }
            }
        }

        setContent {
            val menuStates = rememberSaveableStateHolder()
            RecentsTheme(accent = if (colorPrompt) AccentChart.colors[chartFocus] else settings.accentColor) {
                menuStates.SaveableStateProvider("${screen.name}:${section?.name.orEmpty()}:${submenu?.name.orEmpty()}") {
                when (screen) {
                    SetScreen.PRESETS -> AppearancePresetsScreen(presetFocus, canRestoreAppearance, settings.smoothMotion)
                    SetScreen.ABOUT -> AboutScreen(version, aboutFocus, UpdateManager.state.collectAsStateWithLifecycle().value, installAllowed, settings)
                    SetScreen.UPDATES -> AboutScreen(version, updateFocus, UpdateManager.state.collectAsStateWithLifecycle().value, installAllowed, settings, updatesOnly = true)
                    SetScreen.SUBMENU -> SettingsScreen(settings, section ?: SettingsSection.CONTROLS, setFocus, excSummary(), protectedSummary(), managerLabel, managerStatus, if (settings.usePrivileged) managerReady else false, usageAccessGranted, rowsOverride = submenu!!.rowsFor(settings), titleOverride = submenu!!.title)
                    SetScreen.SETTINGS -> if (section == null) SettingsHome(rootFocus, settings.smoothMotion) else SettingsScreen(
                        settings = settings,
                        section = section!!,
                        focus = setFocus,
                        exceptionsSummary = excSummary(),
                        protectedSummary = protectedSummary(),
                        managerName = managerLabel,
                        managerStatus = managerStatus,
                        privilegedActive = if (settings.usePrivileged) managerReady else false,
                        usageAccessGranted = usageAccessGranted,
                    )
                    SetScreen.EXCEPTIONS, SetScreen.PROTECTED -> ExceptionsScreen(
                        apps = apps,
                        excluded = if (screen == SetScreen.PROTECTED) settings.protectedApps else settings.exceptions,
                        protection = screen == SetScreen.PROTECTED,
                        protectIndividual = settings.protectIndividualCloses,
                        excludeHiddenFromKillAll = settings.excludeHiddenFromKillAll,
                        loading = appsLoading,
                        loadError = appsLoadError,
                        smoothMotion = settings.smoothMotion,
                        focus = excFocus,
                    )
                }
                }
                if (resetPrompt) com.websnake.recentstv.ui.settings.ResetDefaultsDialog(confirmReset)
                if (colorPrompt) AccentColorChart(chartFocus)
                if (presetPrompt) {
                    val preset = AppearancePreset.entries.getOrNull(presetFocus)
                    if (preset == null) ApplyPresetDialog(null, confirmPreset)
                    else {
                        LaunchedEffect(preset, settings) {
                            presetPreviewError = false
                            try { presetPreviews = store.previewAppearanceModes(preset) }
                            catch (e: kotlinx.coroutines.CancellationException) { throw e }
                            catch (_: Exception) { presetPreviewError = true }
                        }
                        PresetReview(preset, presetMode, presetReviewFocus, presetChanges, presetDetails, presetDetailFocus, settings.smoothMotion, presetPreviewError)
                    }
                }
            }
        }
    }

    private fun aboutAction() {
        val action = if (screen == SetScreen.UPDATES) updateActions[updateFocus] else aboutActions[aboutFocus]
        val update = UpdateManager.state.value
        when (action) {
            AboutAction.UPDATES -> { updateFocus = 0; screen = SetScreen.UPDATES }
            AboutAction.AUTO_CHECK -> persist { store.toggleAutoCheckUpdates() }
            AboutAction.NOTIFY -> persist { store.toggleUpdateNotification() }
            AboutAction.CHECK -> if (update.phase == UpdatePhase.DOWNLOADING) UpdateManager.cancelDownload() else UpdateManager.check(this)
            AboutAction.INSTALL -> when (update.phase) {
                UpdatePhase.AVAILABLE, UpdatePhase.ERROR -> UpdateManager.download(this)
                UpdatePhase.READY -> if (UpdateManager.canInstall(this)) UpdateManager.install(this) else UpdateManager.requestInstallPermission(this)
                UpdatePhase.CONFIRM -> UpdateManager.continueInstall(this)
                else -> {}
            }
            else -> try {
                startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(action.url)))
            } catch (_: Exception) {
                android.widget.Toast.makeText(this, "No browser is available. Open ${action.url} on another device.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun excSummary(): String {
        val n = settings.exceptions.size
        return if (n == 0) "No apps hidden" else "$n ${if (n == 1) "app" else "apps"} hidden"
    }
    private fun protectedSummary(): String {
        val n = settings.protectedApps.size
        return "$n ${if (n == 1) "app" else "apps"} protected"
    }

    // ── key handling ────────────────────────────────────────────────────────
    // This overrides the public android.app.Activity callback. AndroidX core's inherited
    // library restriction triggers lint here even though no restricted superclass API is called.
    @android.annotation.SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return true
        val isOk = event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
            event.keyCode == KeyEvent.KEYCODE_ENTER || event.keyCode == KeyEvent.KEYCODE_BUTTON_A
        // Holding OK must not repeat toggles or confirm an action opened by that same press.
        if (isOk && event.repeatCount > 0) return true
        if (presetPrompt) {
            val reviewed = AppearancePreset.entries.getOrNull(presetFocus)
            if (reviewed != null) {
                if (presetDetails) {
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP -> presetDetailFocus = (presetDetailFocus - 1).coerceAtLeast(0)
                        KeyEvent.KEYCODE_DPAD_DOWN -> presetDetailFocus = (presetDetailFocus + 1).coerceAtMost((presetChanges?.lastIndex ?: 0).coerceAtLeast(0))
                        KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> presetDetails = false
                        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            presetMode = if (event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) PresetApplyMode.FULL else PresetApplyMode.APPEARANCE_ONLY
                            presetDetailFocus = presetDetailFocus.coerceAtMost((presetChanges?.lastIndex ?: 0).coerceAtLeast(0))
                        }
                        else -> if (isOk) presetDetails = false
                    }
                } else when (event.keyCode) {
                    KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> presetPrompt = false
                    KeyEvent.KEYCODE_DPAD_UP -> presetReviewFocus = (presetReviewFocus - 1).coerceAtLeast(0)
                    KeyEvent.KEYCODE_DPAD_DOWN -> presetReviewFocus = (presetReviewFocus + 1).coerceAtMost(3)
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> if (presetReviewFocus >= 2) {
                        presetReviewFocus = if (event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) 3 else 2
                    } else if (presetReviewFocus <= 1) {
                        val next = if (event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) PresetApplyMode.FULL else PresetApplyMode.APPEARANCE_ONLY
                        if (next != presetMode) presetMode = next
                    }
                    else -> if (isOk) when (presetReviewFocus) {
                        0 -> presetReviewFocus = 2
                        1 -> if (presetChanges != null) { presetDetailFocus = 0; presetDetails = true }
                        2 -> if (presetChanges != null) {
                            val mode = presetMode
                            presetPrompt = false
                            persist {
                                store.applyAppearancePreset(reviewed, mode)
                                android.widget.Toast.makeText(this, "${reviewed.title} applied · Undo available in presets", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                        3 -> presetPrompt = false
                    }
                }
                return true
            }
            when (event.keyCode) {
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> presetPrompt = false
                KeyEvent.KEYCODE_DPAD_LEFT -> confirmPreset = false
                KeyEvent.KEYCODE_DPAD_RIGHT -> confirmPreset = true
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A -> {
                    presetPrompt = false
                    if (confirmPreset) {
                        val preset = AppearancePreset.entries.getOrNull(presetFocus)
                        persist {
                            val message = if (preset == null) {
                                if (store.restoreAppearance()) "Previous appearance restored" else "No saved appearance to restore"
                            } else {
                                store.applyAppearancePreset(preset)
                                "${preset.title} appearance applied"
                            }
                            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            return true
        }
        if (colorPrompt) {
            val col = chartFocus % AccentChart.columns
            when (event.keyCode) {
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> colorPrompt = false
                KeyEvent.KEYCODE_DPAD_LEFT -> if (col > 0) chartFocus--
                KeyEvent.KEYCODE_DPAD_RIGHT -> if (col < AccentChart.columns - 1) chartFocus++
                KeyEvent.KEYCODE_DPAD_UP -> chartFocus = (chartFocus - AccentChart.columns).coerceAtLeast(col)
                KeyEvent.KEYCODE_DPAD_DOWN -> chartFocus = (chartFocus + AccentChart.columns).coerceAtMost(AccentChart.colors.lastIndex - (AccentChart.columns - 1 - col))
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A -> {
                    val preset = AccentColor.entries.getOrNull(chartFocus)
                    val argb = AccentChart.colors[chartFocus].toArgb()
                    colorPrompt = false
                    persist { if (preset != null) store.setAccent(preset) else store.setCustomAccent(argb) }
                }
            }
            return true
        }
        if (resetPrompt) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> resetPrompt = false
                KeyEvent.KEYCODE_DPAD_LEFT -> confirmReset = false
                KeyEvent.KEYCODE_DPAD_RIGHT -> confirmReset = true
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A -> {
                    resetPrompt = false
                    if (confirmReset) persist {
                        store.resetToDefaults()
                        setFocus = 0
                        rootFocus = 0; section = null
                        android.widget.Toast.makeText(this, "Settings reset to defaults", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
            return true
        }
        if (event.keyCode == KeyEvent.KEYCODE_BACK || event.keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (screen == SetScreen.SUBMENU) {
                val parent = submenuBackStack.removeAt(submenuBackStack.lastIndex)
                submenu = parent.first; setFocus = parent.second
                screen = if (submenu == null) SetScreen.SETTINGS else SetScreen.SUBMENU
            }
            else if (screen == SetScreen.UPDATES) { screen = SetScreen.ABOUT }
            else if (screen != SetScreen.SETTINGS) {
                appsLoadGeneration++; appsLoadJob?.cancel(); appsLoading = false
                screen = if (submenu != null) SetScreen.SUBMENU else SetScreen.SETTINGS
            }
            else if (section != null) { section = null; setFocus = 0 }
            else finish()
            return true
        }
        when (screen) {
            SetScreen.SETTINGS, SetScreen.SUBMENU -> settingsKey(event.keyCode)
            SetScreen.EXCEPTIONS, SetScreen.PROTECTED -> excKey(event.keyCode)
            SetScreen.PRESETS -> when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> presetFocus = (presetFocus - 1).coerceAtLeast(0)
                KeyEvent.KEYCODE_DPAD_DOWN -> presetFocus = (presetFocus + 1).coerceAtMost(AppearancePreset.entries.size + 1)
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A -> {
                    if (presetFocus == AppearancePreset.entries.size + 1) {
                        startActivity(android.content.Intent(this, ProfilesActivity::class.java))
                    } else if (presetFocus < AppearancePreset.entries.size || canRestoreAppearance) {
                        confirmPreset = false; presetMode = PresetApplyMode.FULL; presetReviewFocus = 0; presetDetails = false; presetPreviews = emptyMap(); presetPreviewError = false; presetPrompt = true
                    }
                }
            }
            SetScreen.UPDATES -> when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> updateFocus = (updateFocus - 1).coerceAtLeast(0)
                KeyEvent.KEYCODE_DPAD_DOWN -> updateFocus = (updateFocus + 1).coerceAtMost(updateActions.lastIndex)
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A -> aboutAction()
            }
            SetScreen.ABOUT -> when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> aboutFocus = (aboutFocus - 1).coerceAtLeast(0)
                KeyEvent.KEYCODE_DPAD_DOWN -> aboutFocus = (aboutFocus + 1).coerceAtMost(aboutActions.lastIndex)
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A -> aboutAction()
            }
        }
        return true
    }

    private fun openSubmenu(menu: SettingsSubmenu) {
        submenuBackStack.add(submenu to setFocus)
        submenu = menu; setFocus = 0; screen = SetScreen.SUBMENU
    }
    private fun currentRows(section: SettingsSection) = submenu?.rowsFor(settings) ?: section.rowsFor(settings)
    private fun settingsKey(code: Int) {
        val activeSection = section
        if (activeSection == null) {
            when (code) {
                KeyEvent.KEYCODE_DPAD_UP -> rootFocus = (rootFocus - 1).coerceAtLeast(0)
                KeyEvent.KEYCODE_DPAD_DOWN -> rootFocus = (rootFocus + 1).coerceAtMost(SettingsMenu.reset)
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A -> {
                    when (rootFocus) {
                        SettingsMenu.reset -> { confirmReset = false; resetPrompt = true }
                        SettingsMenu.presets -> { presetFocus = 0; screen = SetScreen.PRESETS }
                        SettingsMenu.about -> { aboutFocus = 0; screen = SetScreen.ABOUT }
                        else -> { section = SettingsMenu.sectionAt(rootFocus); setFocus = 0 }
                    }
                }
            }
            return
        }
        when (code) {
            KeyEvent.KEYCODE_DPAD_UP -> setFocus = (setFocus - 1).coerceAtLeast(0)
            KeyEvent.KEYCODE_DPAD_DOWN -> setFocus = (setFocus + 1).coerceAtMost(currentRows(activeSection).lastIndex)
            KeyEvent.KEYCODE_DPAD_LEFT -> adjust(-1)
            KeyEvent.KEYCODE_DPAD_RIGHT -> adjust(+1)
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A ->
                when (currentRows(activeSection).getOrNull(setFocus)) {
                    SetRow.DOWN_OPTIONS -> openSubmenu(SettingsSubmenu.DOWN)
                    SetRow.RESTORE_TOP -> Unit
                    SetRow.HIDE_TOP_RESTORE -> persist { store.toggleHideTopRestore() }
                    SetRow.EDGE_HELP -> persist { store.toggleEdgeHints() }
                    SetRow.EDGE_OPTIONS -> openSubmenu(SettingsSubmenu.SHORTCUT)
                    SetRow.VIEW_OPTIONS -> openSubmenu(SettingsSubmenu.VIEW)
                    SetRow.DISMISS_OPTIONS -> openSubmenu(SettingsSubmenu.DISMISS)
                    SetRow.PROTECTION_OPTIONS -> openSubmenu(SettingsSubmenu.PROTECTION)
                    SetRow.BULK_OPTIONS -> openSubmenu(SettingsSubmenu.BULK)
                    SetRow.EDGE_MENU_OPTIONS -> openSubmenu(SettingsSubmenu.EDGE)
                    SetRow.HIDE_TOP_BULK -> persist { store.toggleHideTopBulk() }
                    SetRow.HIDE_TOP_SWITCH -> persist { store.toggleHideTopSwitch() }
                    SetRow.DOWN_WITHOUT_ACCESS -> Unit
                    SetRow.DISMISS_ENABLE -> Unit
                    SetRow.TASK_INDICATOR -> persist { store.toggleTaskIndicator() }
                    SetRow.RESTORE_DISMISSED -> persist {
                        store.restoreDismissed()
                        android.widget.Toast.makeText(this, "Dismissed apps restored", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    SetRow.ACCENT -> { chartFocus = AccentChart.nearest(settings.accentColor); colorPrompt = true }
                    SetRow.HIDDEN_KILL_EXCLUSION -> persist { store.toggleHiddenKillExclusion() }
                    SetRow.PROTECTION_FEEDBACK -> persist { store.adjustProtectionFeedback(1) }
                    SetRow.DISMISS_PROTECTED -> persist { store.toggleAllowDismissProtected() }
                    SetRow.INDIVIDUAL_PROTECTION -> persist { store.toggleIndividualProtection() }
                    SetRow.FORCE_STOP -> persist { store.toggleForceStop() }
                    SetRow.HEADER -> Unit
                    SetRow.SETTINGS_BUTTON -> persist { store.toggleSettingsButton() }
                    SetRow.RESTORE_CONFIRM -> persist { store.toggleRestoreConfirmation() }
                    SetRow.EDGE_MENU_RESTORE -> persist { store.toggleEdgeMenuItem(com.websnake.recentstv.data.EdgeMenuItem.RESTORE_DISMISSED) }
                    SetRow.EDGE_MENU_SWITCH -> persist { store.toggleEdgeMenuItem(com.websnake.recentstv.data.EdgeMenuItem.SWITCH_VIEW) }
                    SetRow.EDGE_MENU_CLOSE -> persist { store.toggleEdgeMenuItem(com.websnake.recentstv.data.EdgeMenuItem.CLOSE_ALL) }
                    SetRow.EDGE_MENU_DISMISS -> persist { store.toggleEdgeMenuItem(com.websnake.recentstv.data.EdgeMenuItem.DISMISS_ALL) }
                    SetRow.EDGE_MENU_SETTINGS -> persist { store.toggleEdgeMenuItem(com.websnake.recentstv.data.EdgeMenuItem.SETTINGS) }
                    SetRow.VIEW_SWITCH -> Unit
                    SetRow.VIEW_NOTICE -> Unit
                    SetRow.REMEMBER_VIEW -> persist { store.toggleRememberView() }
                    SetRow.APP_NAMES -> persist { store.toggleAppNames() }
                    SetRow.HELP -> persist { store.toggleShowHelp() }
                    SetRow.MOTION -> persist { store.toggleSmoothMotion() }
                    SetRow.PRIVILEGED -> persist { store.toggleUsePrivileged() }
                    SetRow.RESET -> { confirmReset = false; resetPrompt = true }
                    SetRow.EXCEPTIONS -> openExceptions()
                    SetRow.PROTECTED_APPS -> openExceptions(protection = true)
                    SetRow.MANAGER -> {
                        val manager = com.websnake.recentstv.shizuku.ShizukuManager
                        val state = manager.state()
                        if (settings.usePrivileged && state.binderAlive && !state.permissionGranted) manager.requestPermission()
                        else if (!manager.openShizuku(this))
                            android.widget.Toast.makeText(this, "Porter or Shizuku is not installed", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    else -> {} // Left/Right adjusts values. OK never advances to the next value.
                }
        }
    }

    private fun adjust(dir: Int) {
        when (section?.let { currentRows(it) }?.getOrNull(setFocus)) {
            SetRow.BULK_ACTION -> persist { store.adjustBulkWithAccess(dir) }
            SetRow.BULK_WITHOUT_ACCESS -> persist { store.adjustBulkWithoutAccess(dir) }
            SetRow.LIST_MODE -> persist { store.adjustListMode(dir) }
            SetRow.DOWN_ACTION -> persist { store.adjustDownWithAccess(dir) }
            SetRow.DOWN_WITHOUT_ACCESS -> persist { store.adjustDownWithoutAccess(dir) }
            SetRow.DISMISS_ENABLE -> persist { store.adjustDismissal(dir) }
            SetRow.TASK_INDICATOR -> persist { store.setTaskIndicator(dir > 0) }
            SetRow.POSITION -> persist { store.adjustPosition(dir) }
            SetRow.DIRECTION -> persist { store.adjustDirection(dir) }
            SetRow.SIZE -> persist { store.adjustSize(dir) }
            SetRow.SHAPE -> persist { store.adjustShape(dir) }
            SetRow.PADDING -> persist { store.adjustCardPadding(dir) }
            SetRow.ACCENT -> persist { store.adjustAccent(dir) }
            SetRow.BG_DIM -> persist { store.adjustBgDim(dir) }
            SetRow.BG_TINT -> persist { store.adjustBgTint(dir) }
            SetRow.KILL_ALL -> persist { store.adjustKillAllLocation(dir) }
            SetRow.HIDDEN_KILL_EXCLUSION -> persist { store.setHiddenKillExclusion(dir > 0) }
            SetRow.PROTECTION_FEEDBACK -> persist { store.adjustProtectionFeedback(dir) }
            SetRow.DISMISS_PROTECTED -> persist { store.setAllowDismissProtected(dir > 0) }
            SetRow.INDIVIDUAL_PROTECTION -> persist { store.setIndividualProtection(dir > 0) }
            SetRow.CONFIRM_KILL_ALL -> persist { store.adjustBulkConfirmation(dir) }
            SetRow.FORCE_STOP -> persist { store.setForceStop(dir > 0) }
            SetRow.HIDE_TOP_BULK -> persist { store.setHideTopBulk(dir > 0) }
            SetRow.HIDE_TOP_SWITCH -> persist { store.setHideTopSwitch(dir > 0) }
            SetRow.DISMISS_SCOPE -> persist { store.adjustDismissScope(dir) }
            SetRow.HEADER -> persist { store.adjustHeaderMode(dir) }
            SetRow.SETTINGS_BUTTON -> persist { store.setShowSettingsButton(dir > 0) }
            SetRow.VIEW_SWITCH -> persist { store.adjustViewButtons(dir) }
            SetRow.EDGE_SHORTCUT -> persist { store.adjustEdgeShortcut(dir) }
            SetRow.CONTEXT_RESTORE -> persist { store.adjustContextRestore(dir) }
            SetRow.RESTORE_TOP -> persist { store.adjustRestoreTopMode(dir) }
            SetRow.HIDE_TOP_RESTORE -> persist { store.setHideTopRestore(dir > 0) }
            SetRow.EDGE_HELP -> persist { store.setEdgeHints(dir > 0) }
            SetRow.EMPTY_RECOVERY -> persist { store.adjustRecovery(dir) }
            SetRow.EDGE_WITHOUT_ACCESS -> persist { store.adjustEdgeWithoutAccess(dir) }
            SetRow.RESTORE_CONFIRM -> persist { store.setRestoreConfirmation(dir > 0) }
            SetRow.EDGE_MENU_RESTORE -> persist { store.setEdgeMenuItem(com.websnake.recentstv.data.EdgeMenuItem.RESTORE_DISMISSED, dir > 0) }
            SetRow.EDGE_ACTION -> persist { store.adjustEdgeAction(dir) }
            SetRow.EDGE_MENU_SETTINGS -> persist { store.setEdgeMenuItem(com.websnake.recentstv.data.EdgeMenuItem.SETTINGS, dir > 0) }
            SetRow.EDGE_MENU_DISMISS -> persist { store.setEdgeMenuItem(com.websnake.recentstv.data.EdgeMenuItem.DISMISS_ALL, dir > 0) }
            SetRow.EDGE_MENU_CLOSE -> persist { store.setEdgeMenuItem(com.websnake.recentstv.data.EdgeMenuItem.CLOSE_ALL, dir > 0) }
            SetRow.EDGE_MENU_SWITCH -> persist { store.setEdgeMenuItem(com.websnake.recentstv.data.EdgeMenuItem.SWITCH_VIEW, dir > 0) }
            SetRow.ROW_SWITCH_POSITION -> persist { store.adjustRowSwitchPosition(dir) }
            SetRow.VIEW_NOTICE -> persist { store.adjustViewNotice(dir) }
            SetRow.REMEMBER_VIEW -> persist { store.setRememberView(dir > 0) }
            SetRow.APP_NAMES -> persist { store.setAppNames(dir > 0) }
            SetRow.APP_TEXT_POSITION -> persist { store.adjustAppTextPosition(dir) }
            SetRow.APP_DETAILS -> persist { store.adjustAppDetailsMode(dir) }
            SetRow.CLOSE_HINT -> persist { store.adjustCloseHintStyle(dir) }
            SetRow.CLOSE_SPEED -> persist { store.adjustAnimationSpeed(dir) }
            SetRow.HELP -> persist { store.setShowHelp(dir > 0) }
            SetRow.MOTION -> persist { store.setSmoothMotion(dir > 0) }
            SetRow.PRIVILEGED -> persist { store.setUsePrivileged(dir > 0) }
            else -> {}
        }
    }
    override fun onResume() {
        super.onResume()
        usageAccessGranted = com.websnake.recentstv.data.RecentTasksRepository(applicationContext).hasUsageAccess()
        UpdateManager.onResume(this)
        installAllowed = UpdateManager.canInstall(this)
        statusJob?.cancel()
        statusJob = lifecycleScope.launch {
            while (true) {
                val state = com.websnake.recentstv.shizuku.ShizukuManager.state()
                managerReady = state.ready
                managerLabel = com.websnake.recentstv.shizuku.ShizukuManager.managerName(this@SettingsActivity)
                managerStatus = when {
                    !settings.usePrivileged -> "Disabled for this app · usage history"
                    state.ready -> "Connected and authorized · actual tasks"
                    state.binderAlive -> "Running · access not granted"
                    else -> "Service unavailable · usage history"
                }
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    override fun onPause() {
        UpdateManager.onPause(this)
        statusJob?.cancel()
        super.onPause()
    }
    private fun openExceptions(protection: Boolean = false) {
        appsLoadJob?.cancel()
        val generation = ++appsLoadGeneration
        excFocus = 0
        screen = if (protection) SetScreen.PROTECTED else SetScreen.EXCEPTIONS
        appsLoading = true; appsLoadError = false
        apps.clear()
        appsLoadJob = lifecycleScope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) { pkgRepo.installedApps() }
                if (generation == appsLoadGeneration) {
                    apps.clear(); apps.addAll(sortExceptions(loaded, if (protection) settings.protectedApps else settings.exceptions))
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.w("Settings", "App list could not be loaded", error)
                if (generation == appsLoadGeneration) appsLoadError = true
            } finally {
                if (generation == appsLoadGeneration) appsLoading = false
            }
        }
    }

    private fun excKey(code: Int) {
        if (appsLoading) return
        if (appsLoadError) {
            if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_BUTTON_A)
                openExceptions(screen == SetScreen.PROTECTED)
            return
        }
        when (code) {
            KeyEvent.KEYCODE_DPAD_UP -> excFocus = (excFocus - 1).coerceAtLeast(0)
            KeyEvent.KEYCODE_DPAD_DOWN -> excFocus = (excFocus + 1).coerceAtMost((apps.size - 1).coerceAtLeast(0))
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A -> {
                apps.getOrNull(excFocus)?.let { app -> persist { if (screen == SetScreen.PROTECTED) store.toggleProtectedApp(app.packageName) else store.toggleException(app.packageName) } }
            }
        }
    }

    private fun sortExceptions(list: List<InstalledApp>, hidden: Set<String>) =
        list.sortedWith(compareByDescending<InstalledApp> { it.packageName in hidden }
            .thenBy { it.label.lowercase() }.thenBy { it.packageName })

    private fun persist(block: suspend () -> Unit) {
        lifecycleScope.launch { block() }
    }
}



