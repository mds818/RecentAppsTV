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
        val launch = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PKG)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return runCatching { context.startActivity(launch); true }.getOrDefault(false)
        }
        // fall back to its management activity, then to Play if not installed
        val explicit = Intent().apply {
            component = ComponentName(SHIZUKU_PKG, "$SHIZUKU_PKG.MainActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { context.startActivity(explicit); true }.getOrDefault(false)
    }
}
