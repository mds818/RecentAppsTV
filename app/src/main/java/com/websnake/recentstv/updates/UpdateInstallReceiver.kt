package com.websnake.recentstv.updates

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Explicit, non-exported session callback. Confirmation is opened from the visible About page. */
class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { UpdateManager.installResult(context, intent) }
}
