package com.websnake.recentstv

import androidx.activity.ComponentDialog
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.websnake.recentstv.data.ProfileSettingRow
import com.websnake.recentstv.data.ProfileSettingsReview
import com.websnake.recentstv.data.ProfileSettingsFilter
import com.websnake.recentstv.ui.settings.AllProfileSettings
import com.websnake.recentstv.ui.settings.ProfileSettingDetails
import com.websnake.recentstv.data.PresetChange
import com.websnake.recentstv.ui.settings.ChangeReview
import com.websnake.recentstv.ui.settings.ProfileManagement
import com.websnake.recentstv.ui.settings.ProfileApplyReview
import com.websnake.recentstv.ui.theme.RecentsTheme
import android.app.Dialog
import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import android.view.KeyEvent
import com.websnake.recentstv.ui.settings.SilentFocusNavigation
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.websnake.recentstv.data.ProfileImport
import android.os.Bundle
import android.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.websnake.recentstv.data.SettingsStore
import com.websnake.recentstv.data.ProfileCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Native focusable controls also work with a remote and the system TV keyboard/file picker. */
class ProfilesActivity : ComponentActivity() {
    private lateinit var store: SettingsStore
    private var profiles = JSONArray()
    private var accent = Color.rgb(48, 220, 139)
    private var renderedState = ""
    private lateinit var rows: LinearLayout
    private var pendingExport: String? = null
    private val createFile = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val raw = pendingExport; pendingExport = null
        if (uri != null && raw != null) work {
            withContext(Dispatchers.IO) { requireNotNull(contentResolver.openOutputStream(uri)).use { it.write(raw.toByteArray(Charsets.UTF_8)) } }
            message("Profile exported")
        }
    }
    private val openFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) work {
            val raw = withContext(Dispatchers.IO) {
                requireNotNull(contentResolver.openInputStream(uri)).use {
                    val bytes = it.readBytesLimited(ProfileCodec.MAX_BYTES + 1)
                    require(bytes.size <= ProfileCodec.MAX_BYTES) { "Profile is too large" }
                    bytes.toString(Charsets.UTF_8)
                }
            }
            review(raw, true)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = SettingsStore(applicationContext)
        pendingExport = savedInstanceState?.getString("export")
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(10, 12, 13)); isFillViewport = true; isSoundEffectsEnabled = false }
        rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; isSoundEffectsEnabled = false; setPadding(dp(60), dp(20), dp(60), dp(20)) }
        scroll.addView(rows); setContentView(scroll)
        lifecycleScope.launch { combine(store.savedProfiles, store.settings) { profiles, settings -> profiles to settings }.collect { (saved, settings) -> profiles = saved; accent = settings.customAccent ?: settings.accent.color.toArgb(); val revision = saved.toString() + accent; if (revision != renderedState) { renderedState = revision; render() } } }
    }
    // Public Activity key callback; AndroidX inherits an overly broad library restriction here.
    @android.annotation.SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val content = findViewById<ViewGroup>(android.R.id.content)
        return if (content != null && SilentFocusNavigation.dispatch(content, event)) true else super.dispatchKeyEvent(event)
    }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); outState.putString("export", pendingExport) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun label(text: String, size: Float = 13f) = TextView(this).apply {
        this.text = text; textSize = size; typeface = androidx.core.content.res.ResourcesCompat.getFont(this@ProfilesActivity, R.font.manrope); setTextColor(Color.LTGRAY); setPadding(8, 10, 8, 14)
    }
    private fun makeButton(title: String, action: () -> Unit) = Button(this).apply {
            isSoundEffectsEnabled = false; isHapticFeedbackEnabled = false
            text = title; tag = title; isAllCaps = false; textSize = 20f
            typeface = androidx.core.content.res.ResourcesCompat.getFont(this@ProfilesActivity, R.font.manrope)
            gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START
            setPadding(24, 14, 24, 14)
            val states = android.graphics.drawable.StateListDrawable()
            fun shape(focused: Boolean) = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 16f; setColor(if (focused) Color.rgb(30, 34, 39) else Color.rgb(20, 23, 26))
                setStroke(2, if (focused) accent else Color.TRANSPARENT)
            }
            states.addState(intArrayOf(android.R.attr.state_focused), shape(true)); states.addState(intArrayOf(), shape(false)); background = states
            setTextColor(Color.WHITE); setOnClickListener { action() }
        }
    private fun button(title: String, action: () -> Unit) {
        rows.addView(makeButton(title, action), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 8 })
    }
    private fun render() {
        val previous = currentFocus?.tag as? String
        rows.removeAllViews()
        rows.addView(label("My profiles", 23f))
        rows.addView(label("Save preferences for later or transfer them as JSON. App lists, dismissed history and permissions are not included."))
        button("Save current settings…") { nameDialog("Save profile", "My profile") { name -> work { store.saveProfile(name); message("Profile saved") } } }
        button("Import profile…") { runCatching { openFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }.onFailure { message("No file picker available on this TV") } }
        button("Export current settings…") { work { export(store.exportProfile("My profile")) } }
        button("Undo last profile change") { work { message(if (store.undoProfile()) "Previous settings restored" else "Apply a profile first to create a backup") } }
        rows.addView(label("Saved profiles · ${profiles.length()}"))
        for (i in 0 until profiles.length()) {
            val entry = profiles.getJSONObject(i)
            val doc = entry.getJSONObject("document")
            val id = entry.getString("id")
            button(doc.optString("name", "Profile")) { actions(id, doc) }
            rows.getChildAt(rows.childCount - 1).tag = id
        }
        val target = (0 until rows.childCount).map { rows.getChildAt(it) }.firstOrNull { previous != null && it.tag == previous }
            ?: (0 until rows.childCount).map { rows.getChildAt(it) }.firstOrNull { it is Button }
        target?.requestFocus()
    }
    private data class Choice(val title: String, val keepOpen: Boolean = false, val action: () -> Unit)
    private fun modal(title: String, body: View? = null, choices: List<Choice>): Dialog {
        var content: ViewGroup? = null
        val dialog = object : Dialog(this) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                val root = content
                return if (root != null && SilentFocusNavigation.dispatch(root, event)) true else super.dispatchKeyEvent(event)
            }
        }
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; isSoundEffectsEnabled = false
            setPadding(dp(22), dp(18), dp(22), dp(18))
            background = GradientDrawable().apply { setColor(Color.rgb(21, 23, 27)); cornerRadius = dp(18).toFloat(); setStroke(dp(1), Color.rgb(48, 52, 59)) }
        }
        column.addView(label(title, 21f).apply { setTextColor(Color.WHITE) })
        if (body != null) column.addView(body)
        choices.forEach { choice ->
            column.addView(makeButton(choice.title) { if (!choice.keepOpen) dialog.dismiss(); choice.action() },
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(5) })
        }
        content = column
        dialog.setContentView(column)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        dialog.window?.setLayout(minOf(resources.displayMetrics.widthPixels - dp(80), dp(680)), -2)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog.window?.setDimAmount(0.65f)
        // Safety: the first action is Cancel / Back in destructive and apply dialogs.
        (column.getChildAt(if (body == null) 1 else 2) as? Button)?.requestFocus()
        return dialog
    }
    private fun actions(id: String, doc: JSONObject) {
        reviewProfile(doc.toString(), false, id, doc)
    }
    private fun review(raw: String, imported: Boolean) = reviewProfile(raw, imported, null, null)
    private fun reviewProfile(raw: String, imported: Boolean, savedId: String?, document: JSONObject?) = work {
        val preview = withContext(Dispatchers.Default) { store.reviewProfileSettings(raw) }
        val smooth = store.settings.first().smoothMotion
        showProfileReview(preview.report, imported, preview.changes, smooth, savedId, document, preview.rows)
    }
    private fun showProfileReview(report: ProfileImport, imported: Boolean, changes: List<PresetChange>, smooth: Boolean, savedId: String?, document: JSONObject?, settingRows: List<ProfileSettingRow>) {
        val canManage = savedId != null && document != null
        val lastRow = if (canManage) 2 else 1
        val cancelIndex = lastRow + 1
        val applyIndex = lastRow + 2
        var allSettings by mutableStateOf(false)
        var settingsFilter by mutableStateOf(ProfileSettingsFilter.ALL)
        var settingsFocus by mutableIntStateOf(-1)
        var technicalRow by mutableStateOf<ProfileSettingRow?>(null)
        var technicalFocus by mutableIntStateOf(0)
        var managing by mutableStateOf(false)
        var manageFocus by mutableIntStateOf(0)
        var focus by mutableIntStateOf(0)
        var detailFocus by mutableIntStateOf(0)
        var showingChanges by mutableStateOf(false)
        val dialog = object : ComponentDialog(this) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.action != KeyEvent.ACTION_DOWN) return true
                val ok = event.keyCode in listOf(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A)
                if (ok && event.repeatCount > 0) return true
                if (allSettings) {
                    val technical = technicalRow
                    if (technical != null) {
                        when (event.keyCode) {
                            KeyEvent.KEYCODE_DPAD_UP -> technicalFocus = (technicalFocus - 1).coerceAtLeast(0)
                            KeyEvent.KEYCODE_DPAD_DOWN -> technicalFocus = (technicalFocus + 1).coerceAtMost(technical.technicalLines().lastIndex)
                            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> technicalRow = null
                            else -> if (ok) technicalRow = null else return super.dispatchKeyEvent(event)
                        }
                        return true
                    }
                    val visible = ProfileSettingsReview.select(settingRows, settingsFilter)
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> allSettings = false
                        KeyEvent.KEYCODE_DPAD_UP -> settingsFocus = (settingsFocus - 1).coerceAtLeast(-1)
                        KeyEvent.KEYCODE_DPAD_DOWN -> settingsFocus = (settingsFocus + 1).coerceAtMost(visible.lastIndex.coerceAtLeast(-1))
                        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            val direction = if (event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) -1 else 1
                            val selectedKey = visible.getOrNull(settingsFocus)?.key
                            settingsFilter = ProfileSettingsFilter.entries[(settingsFilter.ordinal + direction).coerceIn(0, ProfileSettingsFilter.entries.lastIndex)]
                            val next = ProfileSettingsReview.select(settingRows, settingsFilter)
                            if (settingsFocus >= 0) settingsFocus = next.indexOfFirst { it.key == selectedKey }.coerceAtLeast(0).coerceAtMost(next.lastIndex.coerceAtLeast(-1))
                        }
                        else -> if (ok) {
                            if (settingsFocus < 0) { if (visible.isNotEmpty()) settingsFocus = 0 }
                            else visible.getOrNull(settingsFocus)?.let { technicalRow = it; technicalFocus = 0 }
                        } else return super.dispatchKeyEvent(event)
                    }
                    return true
                }
                if (managing) {
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> managing = false
                        KeyEvent.KEYCODE_DPAD_UP -> manageFocus = (manageFocus - 1).coerceAtLeast(0)
                        KeyEvent.KEYCODE_DPAD_DOWN -> manageFocus = (manageFocus + 1).coerceAtMost(3)
                        else -> if (ok) when (manageFocus) {
                            0 -> { dismiss(); export(document!!.toString(2)) }
                            1 -> {
                                dismiss()
                                nameDialog("Rename profile", report.name) { name -> work { store.saveProfile(name, document!!.toString(), savedId) } }
                            }
                            2 -> {
                                dismiss()
                                modal("Delete this saved profile?", label("Your current settings will be kept."), listOf(
                                    Choice("Cancel") {}, Choice("Delete") { work { store.deleteProfile(savedId!!) } }))
                            }
                            3 -> managing = false
                        } else return super.dispatchKeyEvent(event)
                    }
                    return true
                }
                if (showingChanges) {
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP -> detailFocus = (detailFocus - 1).coerceAtLeast(0)
                        KeyEvent.KEYCODE_DPAD_DOWN -> detailFocus = (detailFocus + 1).coerceAtMost(changes.lastIndex.coerceAtLeast(0))
                        KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> showingChanges = false
                        else -> if (ok) showingChanges = false else return super.dispatchKeyEvent(event)
                    }
                    return true
                }
                when (event.keyCode) {
                    KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> dismiss()
                    KeyEvent.KEYCODE_DPAD_UP -> focus = if (focus > lastRow) lastRow else (focus - 1).coerceAtLeast(0)
                    KeyEvent.KEYCODE_DPAD_DOWN -> focus = if (focus < lastRow) focus + 1 else if (report.recognized > 0) applyIndex else cancelIndex
                    KeyEvent.KEYCODE_DPAD_LEFT -> if (focus >= cancelIndex) focus = cancelIndex
                    KeyEvent.KEYCODE_DPAD_RIGHT -> if (focus >= cancelIndex && report.recognized > 0) focus = applyIndex
                    else -> if (ok) when {
                        focus == 0 -> { detailFocus = 0; showingChanges = true }
                        focus == 1 -> { allSettings = true; settingsFilter = ProfileSettingsFilter.ALL; settingsFocus = -1; technicalRow = null }
                        canManage && focus == 2 -> { manageFocus = 0; managing = true }
                        focus == cancelIndex -> dismiss()
                        focus == applyIndex && report.recognized > 0 -> {
                            dismiss()
                            work {
                                val accepted = ProfileCodec.document(report.name, report.settings).toString()
                                if (imported) store.saveProfile(report.name, accepted)
                                store.applyProfile(accepted)
                                message("Profile applied · Undo available in My profiles")
                            }
                        }
                    } else return super.dispatchKeyEvent(event)
                }
                return true
            }
        }
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val compose = ComposeView(this).apply {
            isSoundEffectsEnabled = false
            setContent {
                RecentsTheme(accent = androidx.compose.ui.graphics.Color(accent)) {
                    if (allSettings) {
                        val technical = technicalRow
                        if (technical != null) ProfileSettingDetails(technical, technicalFocus, smooth)
                        else AllProfileSettings(report.name, settingRows, settingsFilter, settingsFocus, smooth)
                    }
                    else if (managing) ProfileManagement(report.name, manageFocus)
                    else if (showingChanges) ChangeReview("${report.name} · Changes", "${changes.size} ${if (changes.size == 1) "change" else "changes"}", changes, detailFocus, smooth)
                    else ProfileApplyReview(report, imported, changes, focus, canManage)
                }
            }
        }
        dialog.setContentView(compose)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setOnDismissListener { compose.disposeComposition() }
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.window?.setDimAmount(0f) // The shared review frame draws the scrim once.
    }
    private fun export(raw: String) {
        pendingExport = raw
        runCatching { createFile.launch("RecentAppsTV-profile.json") }.onFailure { pendingExport = null; message("No file picker available on this TV") }
    }
    private fun nameDialog(title: String, current: String, done: (String) -> Unit) {
        val input = EditText(this).apply {
            setSingleLine(); setSelectAllOnFocus(true); setText(current); selectAll()
            isSoundEffectsEnabled = false; textSize = 18f; setTextColor(Color.WHITE)
            typeface = androidx.core.content.res.ResourcesCompat.getFont(this@ProfilesActivity, R.font.manrope)
            background = GradientDrawable().apply { setColor(Color.rgb(10, 12, 15)); cornerRadius = dp(8).toFloat(); setStroke(dp(1), accent) }
            setPadding(dp(12), dp(10), dp(12), dp(10))
            filters = arrayOf(android.text.InputFilter.LengthFilter(40))
        }
        lateinit var dialog: Dialog
        dialog = modal(title, input, listOf(Choice("Cancel") {}, Choice("Save", true) {
            val name = input.text.toString().trim()
            if (name.isEmpty()) input.error = "Enter a name" else { dialog.dismiss(); done(name) }
        }))
        input.requestFocus()
    }
    private fun work(block: suspend () -> Unit) { lifecycleScope.launch { try { block() } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        message(e.message ?: "Could not complete profile action")
    } } }
    private fun message(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(4096)
        while (output.size() < limit) { val count = read(buffer, 0, minOf(buffer.size, limit - output.size())); if (count < 0) break; output.write(buffer, 0, count) }
        return output.toByteArray()
    }
}
