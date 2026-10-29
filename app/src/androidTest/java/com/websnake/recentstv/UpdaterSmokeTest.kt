package com.websnake.recentstv

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Bundle
import android.os.SystemClock
import com.websnake.recentstv.data.SettingsStore
import com.websnake.recentstv.ui.settings.SettingsMenu
import com.websnake.recentstv.updates.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.security.MessageDigest

/** Safe updater tests run only in the separately installed .preview package. */
class UpdaterSmokeTest : Instrumentation() {
    private var mode = "check"
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); mode = arguments?.getString("mode") ?: "check"; start() }
    private fun requireThat(ok: Boolean, message: String) { check(ok) { message } }
    private fun await(accept: () -> Boolean) {
        val until = SystemClock.uptimeMillis() + 30000
        while (!accept() && SystemClock.uptimeMillis() < until) SystemClock.sleep(100)
        requireThat(accept(), "Timed out: ${UpdateManager.state.value}")
    }
    @Suppress("UNCHECKED_CAST") private fun mutable(): MutableStateFlow<UpdateState> {
        val field = UpdateManager::class.java.getDeclaredField("mutable"); field.isAccessible = true
        return field.get(UpdateManager) as MutableStateFlow<UpdateState>
    }
    override fun onStart() {
        val result = Bundle()
        try {
            val context = targetContext
            requireThat(context.packageName.endsWith(".preview"), "Never run updater fixtures on the real package")
            val store = SettingsStore(context)
            when (mode) {
                "download" -> {
                    val json = org.json.JSONObject(File(context.filesDir, "fixtures/public-release.json").readText())
                    val release = ReleaseUpdate("1.0.2", json.getString("name"), json.getString("url"), json.getLong("size"), "0".repeat(64), null)
                    mutable().value = UpdateState(UpdatePhase.AVAILABLE, "Local integrity test", release)
                    UpdateManager.download(context)
                    await { UpdateManager.state.value.phase == UpdatePhase.ERROR }
                    requireThat(UpdateManager.state.value.message.contains("checksum"), "Expected integrity rejection: ${UpdateManager.state.value}")
                    requireThat(!File(context.filesDir, "updates/update.apk").exists(), "Corrupt update was retained")
                }
                "notice" -> {
                    runBlocking {
                        if (store.settings.first().autoCheckUpdates) store.toggleAutoCheckUpdates()
                        if (store.settings.first().showUpdateNotification) store.toggleUpdateNotification()
                    }
                    UpdateManager.checkOnce(context); SystemClock.sleep(1000)
                    val activity = startActivitySync(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    SystemClock.sleep(500)
                    mutable().value = UpdateState(UpdatePhase.AVAILABLE, "Test notice", ReleaseUpdate("9.0.0", "test.apk", "https://github.com/unused", 1, null, null))
                    val field = UpdateManager::class.java.getDeclaredField("notifiedVersion"); field.isAccessible = true
                    runOnMainSync { UpdateManager.onResume(activity as androidx.activity.ComponentActivity) }; SystemClock.sleep(500)
                    requireThat(field.get(UpdateManager) == null, "Disabled notification was shown")
                    runBlocking { store.toggleUpdateNotification() }
                    runOnMainSync { UpdateManager.onResume(activity as androidx.activity.ComponentActivity) }; SystemClock.sleep(1000)
                    requireThat(field.get(UpdateManager) == "9.0.0:null", "Enabled notification was not delivered")
                    activity.finish()
                }
                "disabled" -> {
                    runBlocking { if (store.settings.first().autoCheckUpdates) store.toggleAutoCheckUpdates() }
                    UpdateManager.checkOnce(context); SystemClock.sleep(1200)
                    requireThat(UpdateManager.state.value.phase == UpdatePhase.IDLE, "Disabled startup performed a check")
                    runBlocking { store.toggleAutoCheckUpdates() }
                    UpdateManager.checkOnce(context); SystemClock.sleep(500)
                    requireThat(UpdateManager.state.value.phase == UpdatePhase.IDLE, "Startup guard checked twice")
                    UpdateManager.check(context)
                    await { UpdateManager.state.value.phase !in listOf(UpdatePhase.IDLE, UpdatePhase.CHECKING) }
                    requireThat(UpdateManager.state.value.phase == UpdatePhase.CURRENT, "Manual check failed: ${UpdateManager.state.value}")
                }
                "archive" -> {
                    val good = File(context.filesDir, "fixtures/newer.apk")
                    requireThat(UpdateManager.verifyArchive(context, good, "1.0.2") == 19L, "Valid update rejected")
                    fun rejected(file: String, version: String, contains: String) {
                        val e = runCatching { UpdateManager.verifyArchive(context, File(context.filesDir, "fixtures/$file"), version) }.exceptionOrNull()
                        requireThat(e?.message?.contains(contains) == true, "Expected $contains for $file, got $e")
                    }
                    rejected("current.apk", "1.0.1", "not a newer build")
                    rejected("wrong-package.apk", "1.0.1", "another application")
                    rejected("wrong-key.apk", "1.0.2", "Signing key differs")
                    rejected("newer.apk", "9.9.9", "does not match")
                    File(context.filesDir, "fixtures/broken.apk").writeText("invalid APK")
                    rejected("broken.apk", "1.0.2", "not a valid APK")
                    // Receiver errors must map to useful UI text, and ignore unrelated sessions.
                    context.getSharedPreferences("update_install", 0).edit().putInt("session", 12345).commit()
                    for ((status, text) in listOf(PackageInstaller.STATUS_FAILURE_STORAGE to "USB", PackageInstaller.STATUS_FAILURE_ABORTED to "cancelled", PackageInstaller.STATUS_FAILURE_CONFLICT to "signing key")) {
                        UpdateManager.installResult(context, Intent().putExtra(PackageInstaller.EXTRA_SESSION_ID, 12345).putExtra(PackageInstaller.EXTRA_STATUS, status))
                        requireThat(UpdateManager.state.value.phase == UpdatePhase.ERROR && UpdateManager.state.value.message.contains(text), "Wrong installer result")
                    }
                    context.getSharedPreferences("update_install", 0).edit().clear().commit()
                }
                "stage" -> {
                    runBlocking { if (store.settings.first().autoCheckUpdates) store.toggleAutoCheckUpdates() }
                    UpdateManager.checkOnce(context); SystemClock.sleep(1000)
                    val source = File(context.filesDir, "fixtures/newer.apk")
                    UpdateManager.verifyArchive(context, source, "1.0.2")
                    val file = File(context.filesDir, "updates/update.apk"); file.parentFile!!.mkdirs(); source.copyTo(file, overwrite = true)
                    val digest = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
                    val field = UpdateManager::class.java.getDeclaredField("verifiedHash"); field.isAccessible = true; field.set(UpdateManager, digest)
                    mutable().value = UpdateState(UpdatePhase.READY, "Local test update verified · no public release changed", ReleaseUpdate("1.0.2", "fixture.apk", "https://github.com/unused", file.length(), digest, null))
                    val activity = startActivitySync(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    // Enter About → Updates using the same remote keys as a user.
                    val command = uiAutomation.executeShellCommand("am start -W -f 0x10008000 -n ${context.packageName}/com.websnake.recentstv.SettingsActivity")
                    android.os.ParcelFileDescriptor.AutoCloseInputStream(command).use { it.readBytes() }
                    SystemClock.sleep(1000)
                    repeat(SettingsMenu.about) { sendKeyDownUpSync(20); SystemClock.sleep(120) }
                    sendKeyDownUpSync(23); SystemClock.sleep(300); sendKeyDownUpSync(23); SystemClock.sleep(300); sendKeyDownUpSync(20)
                    SystemClock.sleep(1500)
                    context.getSharedPreferences("updater_test", 0).edit().putString("keep", "preserve-settings").commit()
                    runOnMainSync { UpdateManager.install(context) }
                    // Keep the instrumentation alive while the external script operates Android's UI.
                    await { UpdateManager.state.value.phase == UpdatePhase.CONFIRM || UpdateManager.state.value.phase == UpdatePhase.ERROR }
                    requireThat(UpdateManager.state.value.phase == UpdatePhase.CONFIRM, "Installer did not request confirmation: ${UpdateManager.state.value}")
                    File(context.filesDir, "test-confirm-ready").writeText("ready")
                    result.putString("stream", "PASS: PackageInstaller reached user confirmation; local fixture only\n")
                    SystemClock.sleep(25000)
                    activity.finish()
                }
                else -> {
                    UpdateManager.checkOnce(context)
                    await { UpdateManager.state.value.phase !in listOf(UpdatePhase.IDLE, UpdatePhase.CHECKING) }
                    requireThat(UpdateManager.state.value.phase == UpdatePhase.CURRENT, "Expected current version: ${UpdateManager.state.value}")
                    val before = UpdateManager.state.value
                    repeat(4) { UpdateManager.checkOnce(context) }
                    requireThat(UpdateManager.state.value === before, "Repeated startup started another check")
                }
            }
            result.putBoolean("passed", true)
            result.putString("stream", (result.getString("stream") ?: "") + "PASS: updater $mode\n")
            finish(Activity.RESULT_OK, result)
        } catch (e: Throwable) {
            result.putBoolean("passed", false); result.putString("stream", android.util.Log.getStackTraceString(e)); finish(Activity.RESULT_CANCELED, result)
        }
    }
}
