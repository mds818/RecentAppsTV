package com.websnake.recentstv.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/** Snapshot of Shizuku readiness. */
data class ShizukuState(val binderAlive: Boolean, val permissionGranted: Boolean) {
    val ready get() = binderAlive && permissionGranted
}

/**
 * Thin wrapper around the Shizuku binder lifecycle + permission flow.
 * Mirrors the pattern proven in the force-stop app.
 */
object ShizukuManager {

    private const val REQUEST_CODE = 4711
    const val SHIZUKU_PKG = "moe.shizuku.privileged.api"
    const val PORTER_PKG = "eu.darken.porter"

    private fun managerPackage(context: Context): String {
        if (eu.darken.porter.sdk.Porter.connection.value?.backend == eu.darken.porter.sdk.PorterBackend.PORTER)
            return PORTER_PKG
        if (runCatching { Shizuku.pingBinder() }.getOrDefault(false)) return SHIZUKU_PKG
        // No connected service: offer an installed manager so it can be started.
        return if (runCatching { context.packageManager.getApplicationInfo(PORTER_PKG, 0).enabled }.getOrDefault(false)) PORTER_PKG
        else SHIZUKU_PKG
    }

    fun managerName(context: Context): String =
        if (managerPackage(context) == PORTER_PKG) "Porter" else "Shizuku"

    fun state(): ShizukuState {
        val alive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        val granted = alive && runCatching {
            !Shizuku.isPreV11() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        return ShizukuState(alive, granted)
    }

    fun requestPermission() {
        runCatching {
            if (Shizuku.pingBinder() && !Shizuku.isPreV11()) {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED &&
                    !Shizuku.shouldShowRequestPermissionRationale()
                ) {
                    Shizuku.requestPermission(REQUEST_CODE)
                } else {
                    Shizuku.requestPermission(REQUEST_CODE)
                }
            }
        }
    }

    fun addPermissionListener(listener: Shizuku.OnRequestPermissionResultListener) {
        runCatching { Shizuku.addRequestPermissionResultListener(listener) }
    }

    fun removePermissionListener(listener: Shizuku.OnRequestPermissionResultListener) {
        runCatching { Shizuku.removeRequestPermissionResultListener(listener) }
    }

    /** Open the Shizuku app so the user can start the service / grant. */
    fun openShizuku(context: Context): Boolean {
        val pkg = managerPackage(context)
        val launch = context.packageManager.getLeanbackLaunchIntentForPackage(pkg)
            ?: context.packageManager.getLaunchIntentForPackage(pkg)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return runCatching { context.startActivity(launch); true }.getOrDefault(false)
        }
        // fall back to its management activity, then to Play if not installed
        val explicit = Intent().apply {
            component = ComponentName(pkg, "$pkg.MainActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { context.startActivity(explicit); true }.getOrDefault(false)
    }
}
