package com.websnake.recentstv

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
        modal(doc.optString("name"), choices = listOf(
            Choice("Back") {},
            Choice("Apply…") { review(doc.toString(), false) },
            Choice("View settings…") { details(store.inspectProfile(doc.toString())) },
            Choice("Export…") { export(doc.toString(2)) },
            Choice("Rename…") { nameDialog("Rename profile", doc.optString("name")) { name -> work { store.saveProfile(name, doc.toString(), id) } } },
            Choice("Delete…") { modal("Delete this saved profile?", label("Your current settings will be kept."), listOf(
                Choice("Cancel") {}, Choice("Delete") { work { store.deleteProfile(id) } })) }
        ))
    }
    private fun details(report: ProfileImport) {
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; isSoundEffectsEnabled = false }
        report.entries.forEach { entry ->
            val title = entry.key.replace('_', ' ').replaceFirstChar { it.uppercase() }
            content.addView(label(title, 16f).apply { setTextColor(if (entry.status == "Skip" || entry.status == "Partial") Color.rgb(235, 190, 92) else Color.WHITE) })
            content.addView(label("${entry.status} · ${entry.value}\n${entry.key}" + if (entry.reason.isEmpty()) "" else "\n${entry.reason}").apply { setPadding(dp(8), 0, dp(8), dp(12)) })
        }
        val scroll = ScrollView(this).apply {
            isSoundEffectsEnabled = false; addView(content)
            layoutParams = LinearLayout.LayoutParams(-1, minOf(dp(270), resources.displayMetrics.heightPixels / 2))
        }
        modal("All profile settings · ${report.entries.size}", scroll, listOf(Choice("Close") {}))
        // Scroll content with the remote; BACK also returns to the review.
        scroll.isFocusable = true; scroll.requestFocus()
    }
    private fun review(raw: String, imported: Boolean) {
        runCatching {
            val report = store.inspectProfile(raw)
            val text = "${report.recognized} compatible settings\n${report.missing} missing · kept unchanged\n${report.skipped} unknown or invalid · skipped\n\nReview every option below. Applying creates an undo checkpoint."
            val actions = mutableListOf(Choice("Cancel") {}, Choice("View all settings…", true) { details(report) })
            if (report.recognized > 0) {
                actions += Choice(if (imported) "Save and apply" else "Apply") {
                    work {
                        val accepted = ProfileCodec.document(report.name, report.settings).toString()
                        if (imported) store.saveProfile(report.name, accepted)
                        store.applyProfile(accepted)
                        message("Profile applied")
                    }
                }
            }
            modal(report.name, label(text), actions)
        }.onFailure { message(it.message ?: "Could not read profile") }
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
