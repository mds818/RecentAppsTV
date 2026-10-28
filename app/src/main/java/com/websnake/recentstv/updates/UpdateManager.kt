package com.websnake.recentstv.updates

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.pm.PackageInfoCompat
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import com.websnake.recentstv.data.SettingsStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

enum class UpdatePhase { IDLE, CHECKING, CURRENT, AVAILABLE, DOWNLOADING, READY, INSTALLING, CONFIRM, ERROR }
data class ReleaseUpdate(val version: String, val name: String, val url: String, val size: Long,
    val digest: String?, val checksumUrl: String?)
data class UpdateState(val phase: UpdatePhase = UpdatePhase.IDLE, val message: String = "Not checked yet",
    val release: ReleaseUpdate? = null, val progress: Int = 0)

/** One check per app process, started by an activity. No timer, worker, service or polling. */
object UpdateManager {
    const val REPOSITORY = "https://github.com/mds818/RecentAppsTV"
    private const val API = "https://api.github.com/repos/mds818/RecentAppsTV/releases/latest"
    private const val MAX_APK = 128L * 1024 * 1024
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutable = MutableStateFlow(UpdateState())
    val state = mutable.asStateFlow()
    private var started = false
    private var job: Job? = null
    private var confirmation: Intent? = null
    private var foreground = java.lang.ref.WeakReference<ComponentActivity>(null)
    private var notifiedVersion: String? = null
    private var verifiedHash: String? = null
    fun onResume(activity: ComponentActivity) {
        foreground = java.lang.ref.WeakReference(activity)
        // Wait until the Activity has finished its onResume lifecycle transition.
        scope.launch { yield(); notifyIfAvailable(activity.applicationContext) }
    }
    fun onPause(activity: ComponentActivity) { if (foreground.get() === activity) foreground.clear() }
    private fun notifyIfAvailable(context: Context) {
        val release = mutable.value.release ?: return
        if (mutable.value.phase != UpdatePhase.AVAILABLE || notifiedVersion == release.version) return
        scope.launch {
            if (!SettingsStore(context).settings.first().showUpdateNotification) return@launch
            val activity = foreground.get() ?: return@launch
            if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return@launch
            if (notifiedVersion == release.version) return@launch
            notifiedVersion = release.version
            android.widget.Toast.makeText(activity, "RecentAppsTV ${release.version} is available · Settings → About → Updates", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    private fun prefs(context: Context) = context.getSharedPreferences("update_install", Context.MODE_PRIVATE)
    private fun apk(context: Context) = File(context.filesDir, "updates/update.apk")

    @Synchronized fun checkOnce(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        val p = prefs(app)
        val target = p.getLong("target", 0)
        if (target > 0 && installedCode(app) >= target) {
            p.edit().clear().apply()
            apk(app).delete()
            mutable.value = UpdateState(UpdatePhase.CURRENT, "Update installed successfully")
            return
        }
        // A process may die during installation. Recover a result or explain an unfinished session.
        if (p.contains("result") || target > 0) {
            val result = p.getString("result", null) ?: "Installation did not complete. Check for updates to try again."
            val session = p.getInt("session", -1)
            if (session >= 0) scope.launch(Dispatchers.IO) { runCatching { app.packageManager.packageInstaller.abandonSession(session) } }
            p.edit().clear().apply()
            apk(app).delete()
            mutable.value = UpdateState(UpdatePhase.ERROR, result)
            return
        }
        scope.launch {
            if (SettingsStore(app).settings.first().autoCheckUpdates) check(app)
            else if (mutable.value.phase == UpdatePhase.IDLE) mutable.value = UpdateState(message = "Automatic checks are off · check manually when you want")
        }
    }

    fun check(context: Context) {
        if (job?.isActive == true || mutable.value.phase in listOf(UpdatePhase.INSTALLING, UpdatePhase.CONFIRM)) return
        val app = context.applicationContext
        job = scope.launch {
            mutable.value = UpdateState(UpdatePhase.CHECKING, "Checking GitHub…")
            try {
                val release = withContext(Dispatchers.IO) {
                    val json = JSONObject(readText(API, 1024 * 1024))
                    require(!json.optBoolean("draft") && !json.optBoolean("prerelease")) { "No stable release is available." }
                    val version = json.getString("tag_name")
                    val installed = app.packageManager.getPackageInfo(app.packageName, 0).versionName.orEmpty()
                    require(UpdatePolicy.version(version) != null) { "The release uses an unsupported version format. View release notes instead." }
                    require(UpdatePolicy.version(installed) != null) { "This build uses a custom version. View release notes to check manually." }
                    if (!UpdatePolicy.newer(version, installed)) return@withContext null
                    val name = "RecentAppsTV-${version.removePrefix("v")}.apk"
                    val assets = json.getJSONArray("assets")
                    val entries = (0 until assets.length()).map { assets.getJSONObject(it) }
                    val asset = entries.singleOrNull { it.optString("name") == name }
                        ?: error("The latest release has no compatible APK yet.")
                    val size = asset.getLong("size")
                    require(size in 1..MAX_APK) { "Invalid APK download size." }
                    val digest = asset.optString("digest").removePrefix("sha256:")
                        .takeIf { it.matches(Regex("[a-fA-F0-9]{64}")) }?.lowercase()
                    val checksum = entries.singleOrNull { it.optString("name") == "SHA256SUMS.txt" }
                        ?.getString("browser_download_url")
                    require(digest != null || checksum != null) { "The release is missing its download checksum." }
                    ReleaseUpdate(version.removePrefix("v"), name, asset.getString("browser_download_url"), size, digest, checksum)
                }
                mutable.value = if (release == null) UpdateState(UpdatePhase.CURRENT, "You have the latest version")
                    else UpdateState(UpdatePhase.AVAILABLE, "Version ${release.version} is available", release)
                notifyIfAvailable(app)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.value = UpdateState(UpdatePhase.ERROR, friendly(e, "Could not check for updates")) }
        }
    }

    fun download(context: Context) {
        if (job?.isActive == true) return
        val release = mutable.value.release ?: return
        val app = context.applicationContext
        job = scope.launch {
            val file = apk(app)
            val partial = File(file.parentFile, "update.partial")
            mutable.value = UpdateState(UpdatePhase.DOWNLOADING, "Downloading ${release.version}…", release)
            try {
                withContext(Dispatchers.IO) {
                    file.parentFile!!.mkdirs(); file.delete(); partial.delete()
                    val expected = release.digest ?: UpdatePolicy.checksum(readText(release.checksumUrl!!, 64 * 1024), release.name)
                        ?: error("No unique checksum for this APK.")
                    val connection = connect(release.url)
                    try {
                        val sha = MessageDigest.getInstance("SHA-256")
                        connection.inputStream.use { input -> partial.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024); var total = 0L
                            while (true) {
                                ensureActive()
                                val n = input.read(buffer); if (n < 0) break
                                total += n
                                require(total <= release.size) { "Download is larger than expected." }
                                sha.update(buffer, 0, n); output.write(buffer, 0, n)
                                val percent = (total * 100 / release.size).toInt()
                                if (mutable.value.progress != percent) mutable.value = UpdateState(UpdatePhase.DOWNLOADING, "Downloading ${release.version}… $percent%", release, percent)
                            }
                            require(total == release.size) { "Download was incomplete. Please retry." }
                        } }
                        require(hex(sha.digest()) == expected) { "Download checksum does not match. Please retry." }
                    } finally { connection.disconnect() }
                    verifyArchive(app, partial, release.version)
                    verifiedHash = expected
                    require(partial.renameTo(file)) { "Could not save the downloaded APK." }
                }
                mutable.value = UpdateState(UpdatePhase.READY, "Download verified · ready to install", release, 100)
            } catch (e: CancellationException) {
                mutable.value = UpdateState(UpdatePhase.AVAILABLE, "Download cancelled", release)
            } catch (e: Exception) {
                file.delete()
                mutable.value = UpdateState(UpdatePhase.ERROR, friendly(e, "Could not download update"), release)
            } finally { partial.delete() }
        }
    }

    fun cancelDownload() { if (mutable.value.phase == UpdatePhase.DOWNLOADING) job?.cancel() }
    fun canInstall(context: Context) = Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()

    fun requestInstallPermission(activity: Activity) {
        try {
            activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}")))
        } catch (_: Exception) {
            mutable.value = mutable.value.copy(message = "This TV could not open installation permission settings. Enable it in Android Settings → Apps → Special app access.")
        }
    }

    fun install(context: Context) {
        if (job?.isActive == true || mutable.value.phase != UpdatePhase.READY) return
        val app = context.applicationContext
        val release = mutable.value.release ?: return
        if (!canInstall(app)) return
        job = scope.launch {
            mutable.value = UpdateState(UpdatePhase.INSTALLING, "Preparing Android installer…", release)
            try {
                withContext(Dispatchers.IO) {
                    val file = apk(app)
                    require(verifiedHash != null && file.inputStream().use { input ->
                        val sha = MessageDigest.getInstance("SHA-256"); val buffer = ByteArray(64 * 1024)
                        while (true) { val n = input.read(buffer); if (n < 0) break; sha.update(buffer, 0, n) }
                        hex(sha.digest())
                    } == verifiedHash) { "The verified download changed. Download the update again." }
                    val target = verifyArchive(app, file, release.version)
                    val installer = app.packageManager.packageInstaller
                    val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                        setAppPackageName(app.packageName)
                        // Do not force internal storage: also allow updates on adopted USB storage.
                        setInstallLocation(PackageInfo.INSTALL_LOCATION_AUTO)
                        setSize(file.length())
                        if (Build.VERSION.SDK_INT >= 31) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
                    }
                    val id = installer.createSession(params)
                    var committed = false
                    try {
                        installer.openSession(id).use { session ->
                            session.openWrite("base.apk", 0, file.length()).use { output ->
                                file.inputStream().use { it.copyTo(output) }; session.fsync(output)
                            }
                            val intent = Intent(app, UpdateInstallReceiver::class.java).setAction("${app.packageName}.UPDATE_RESULT")
                            val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
                            val callback = PendingIntent.getBroadcast(app, id, intent, flags)
                            prefs(app).edit().putLong("target", target).putInt("session", id).remove("result").commit()
                            session.commit(callback.intentSender)
                            committed = true
                        }
                    } finally {
                        if (!committed) { runCatching { installer.abandonSession(id) }; prefs(app).edit().clear().apply() }
                    }
                }
            } catch (e: Exception) {
                mutable.value = UpdateState(UpdatePhase.ERROR, friendly(e, "Could not start installation"), release)
            }
        }
    }

    fun continueInstall(activity: Activity) {
        val intent = confirmation ?: return
        try { activity.startActivity(intent); mutable.value = mutable.value.copy(phase = UpdatePhase.INSTALLING, message = "Complete the update in Android's installer") }
        catch (_: Exception) { mutable.value = mutable.value.copy(message = "Could not open Android's installer. Try again.") }
    }

    internal fun installResult(context: Context, intent: Intent) {
        val p = prefs(context)
        if (intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1) != p.getInt("session", -2)) return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            @Suppress("DEPRECATION") val next = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
            confirmation = next
            mutable.value = mutable.value.copy(phase = if (next != null) UpdatePhase.CONFIRM else UpdatePhase.ERROR,
                message = if (next != null) "Android needs your confirmation. Select Continue installation." else "Android did not provide an installation confirmation.")
            return
        }
        confirmation = null
        val message = when (status) {
            PackageInstaller.STATUS_SUCCESS -> "Update installed successfully"
            PackageInstaller.STATUS_FAILURE_ABORTED -> "Installation cancelled. Your current app is unchanged."
            PackageInstaller.STATUS_FAILURE_STORAGE -> "Installation failed: check free space and that adopted USB storage is connected."
            PackageInstaller.STATUS_FAILURE_CONFLICT -> "Installation conflict: the package or signing key is incompatible."
            PackageInstaller.STATUS_FAILURE_BLOCKED -> "Installation blocked by Android. Check installation permissions or device restrictions."
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "This update is not compatible with the device."
            else -> "Installation failed. Your current app is unchanged; check storage and try again."
        }
        p.edit().putString("result", message).apply()
        mutable.value = mutable.value.copy(phase = if (status == PackageInstaller.STATUS_SUCCESS) UpdatePhase.CURRENT else UpdatePhase.ERROR, message = message)
        apk(context).delete()
    }

    @Suppress("DEPRECATION")
    internal fun verifyArchive(context: Context, file: File, expectedVersion: String): Long {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val archive = pm.getPackageArchiveInfo(file.absolutePath, flags) ?: error("The downloaded file is not a valid APK.")
        require(archive.packageName == context.packageName) { "This APK belongs to another application." }
        val installed = pm.getPackageInfo(context.packageName, flags)
        fun signers(info: PackageInfo): Set<String> = (if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures)
            ?.map { hex(MessageDigest.getInstance("SHA-256").digest(it.toByteArray())) }?.toSet().orEmpty()
        val signatures = signers(archive)
        require(signatures.isNotEmpty() && signatures == signers(installed)) { "Signing key differs from the installed app. An in-place update is not possible." }
        val code = PackageInfoCompat.getLongVersionCode(archive)
        require(code > PackageInfoCompat.getLongVersionCode(installed)) { "This APK is not a newer build than the installed app." }
        require(archive.versionName == expectedVersion) { "APK version does not match the release." }
        return code
    }

    private fun installedCode(context: Context) = PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0))
    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }
    private fun friendly(e: Exception, prefix: String) = when (e) {
        is java.net.UnknownHostException, is java.net.SocketTimeoutException, is javax.net.ssl.SSLException -> "$prefix. Check your internet connection and try again."
        is IOException -> "$prefix. Check your connection and available storage, then retry."
        else -> e.message?.take(200) ?: "$prefix. Please retry."
    }

    private fun connect(address: String): HttpURLConnection {
        var url = URL(address)
        repeat(6) {
            require(url.protocol == "https" && url.host in setOf("api.github.com", "github.com", "objects.githubusercontent.com", "release-assets.githubusercontent.com")) { "Unexpected update download address." }
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000; readTimeout = 15000; instanceFollowRedirects = false
                setRequestProperty("User-Agent", "RecentAppsTV-Updater")
                setRequestProperty("Accept", "application/vnd.github+json, application/octet-stream")
            }
            val code = connection.responseCode
            if (code in 300..399) {
                val next = connection.getHeaderField("Location"); connection.disconnect()
                require(next != null) { "Invalid download redirect." }; url = URL(url, next)
            } else {
                if (code != 200) {
                    connection.disconnect()
                    error(when (code) { 403, 429 -> "GitHub is limiting requests. Please try again later."; 404 -> "No published release is available yet."; else -> "GitHub could not provide the update (HTTP $code)." })
                }
                return connection
            }
        }
        error("Too many update download redirects.")
    }

    private fun readText(address: String, limit: Int): String {
        val connection = connect(address)
        try { return connection.inputStream.use { input ->
            val bytes = input.readBytesLimited(limit)
            bytes.toString(Charsets.UTF_8)
        } } finally { connection.disconnect() }
    }
    private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
        while (true) { val n = read(buffer); if (n < 0) break; require(output.size() + n <= limit) { "Update metadata is too large." }; output.write(buffer, 0, n) }
        return output.toByteArray()
    }
}
