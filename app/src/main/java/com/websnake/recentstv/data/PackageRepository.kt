package com.websnake.recentstv.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build

/** Installed-app metadata: labels, real launcher icons, categories. */
class PackageRepository(context: Context) {

    // Process-wide caches: resolved once, then instant on every later overlay open
    // (and across Activity recreations) — the main lever for a fast warm open.
    private companion object {
        val labelCache = java.util.concurrent.ConcurrentHashMap<String, String>()
        val iconCache = java.util.concurrent.ConcurrentHashMap<String, Drawable>()
        val categoryCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    }

    private val pm: PackageManager = context.packageManager

    /** True only if the app is installed AND enabled — a disabled/uninstalled app
     *  whose task still lingers in recents must not be surfaced. Not cached:
     *  enabled state changes at runtime. */
    fun isUsable(pkg: String): Boolean = runCatching {
        pm.getApplicationInfo(pkg, 0).enabled   // throws for disabled-by-user / uninstalled
    }.getOrDefault(false)

    fun label(pkg: String): String = labelCache.getOrPut(pkg) {
        runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() })
    }

    /** Real launcher icon, resolved through several fallbacks so every app gets
     *  a usable icon (e.g. phone apps on TV like MX Player that lack a leanback
     *  launcher). Cached process-wide; never returns null. */
    fun icon(pkg: String): Drawable? {
        iconCache[pkg]?.let { return it }
        val resolved = resolveIcon(pkg)
        if (resolved != null) iconCache[pkg] = resolved
        return resolved
    }

    private fun resolveIcon(pkg: String): Drawable? {
        // 1. application icon — most reliable, present for virtually every app
        runCatching { pm.getApplicationIcon(pkg) }.getOrNull()?.let { return it }
        // 2. activity icon from the launch component
        runCatching {
            val cn = (pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg))?.component
            cn?.let { pm.getActivityIcon(it) }
        }.getOrNull()?.let { return it }
        // 3. TV banner, 4. system default
        runCatching { pm.getApplicationBanner(pkg) }.getOrNull()?.let { return it }
        return runCatching { pm.defaultActivityIcon }.getOrNull()
    }

    fun category(pkg: String): String = categoryCache.getOrPut(pkg) { resolveCategory(pkg) }

    private fun resolveCategory(pkg: String): String = runCatching {
        val ai = pm.getApplicationInfo(pkg, 0)
        if (ai.flags and ApplicationInfo.FLAG_SYSTEM != 0 && Build.VERSION.SDK_INT >= 26 &&
            ai.category == ApplicationInfo.CATEGORY_UNDEFINED
        ) "System" else categoryName(ai)
    }.getOrDefault("")

    private fun categoryName(ai: ApplicationInfo): String {
        if (Build.VERSION.SDK_INT < 26) return ""
        return when (ai.category) {
            ApplicationInfo.CATEGORY_GAME -> "Games"
            ApplicationInfo.CATEGORY_AUDIO -> "Music"
            ApplicationInfo.CATEGORY_VIDEO -> "Video"
            ApplicationInfo.CATEGORY_IMAGE -> "Photos"
            ApplicationInfo.CATEGORY_SOCIAL -> "Social"
            ApplicationInfo.CATEGORY_NEWS -> "News"
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
            else -> if (ai.flags and ApplicationInfo.FLAG_SYSTEM != 0) "System" else "App"
        }
    }

    /** All launchable apps (leanback first, then standard), sorted by label. */
    fun installedApps(): List<InstalledApp> {
        val seen = LinkedHashMap<String, InstalledApp>()
        for (action in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(action)
            val acts = runCatching { pm.queryIntentActivities(intent, 0) }.getOrDefault(emptyList())
            for (ri in acts) {
                val pkg = ri.activityInfo.packageName
                if (pkg in seen) continue
                val ai = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull()
                val system = ai != null && (ai.flags and ApplicationInfo.FLAG_SYSTEM != 0)
                seen[pkg] = InstalledApp(
                    packageName = pkg,
                    label = label(pkg),
                    icon = icon(pkg),
                    tag = if (system) "System" else "App",
                )
            }
        }
        return seen.values.sortedBy { it.label.lowercase() }
    }
}
