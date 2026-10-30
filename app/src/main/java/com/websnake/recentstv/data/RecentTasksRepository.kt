package com.websnake.recentstv.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.websnake.recentstv.shizuku.ShizukuManager
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.Method

/**
 * Reads and mutates the system recent-task list through Shizuku-elevated
 * privileged binder calls, behind a clean interface so the UI never touches
 * Shizuku directly (and so it can be stubbed for previews).
 *
 *  - read    : IActivityTaskManager.getRecentTasks(...)  → dumpsys parse fallback
 *  - switch  : IActivityTaskManager.moveTaskToFront(...)  → launch-intent fallback (caller)
 *  - dismiss : IActivityTaskManager.removeTask(taskId)    → forceStopPackage fallback
 *  - killAll : removeTask per task                        → forceStop per task fallback
 */
class RecentTasksRepository(private val context: Context) {

    private companion object {
        const val MAX_TASKS = 12
        val usageHistory = ForegroundUsageCache(java.util.concurrent.TimeUnit.DAYS.toMillis(7))
    }

    private val pkgRepo = PackageRepository(context)
    private val ownPackage = context.packageName

    private val skipPackages = setOf(
        ownPackage,
        "com.android.systemui",
        ShizukuManager.SHIZUKU_PKG,
        ShizukuManager.PORTER_PKG,
    )
    private val launcherHints = listOf(
        "com.google.android.tvlauncher",
        "com.google.android.apps.tv.launcherx",
        "com.google.android.leanbacklauncher",
        "com.android.launcher",
    )

    // packages that register as a HOME/launcher — never shown as a "recent app"
    private val homePackages: Set<String> by lazy {
        runCatching {
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
                .addCategory(android.content.Intent.CATEGORY_HOME)
            context.packageManager.queryIntentActivities(intent, 0)
                .mapTo(HashSet()) { it.activityInfo.packageName }
        }.getOrDefault(emptySet())
    }

    private fun isHome(pkg: String) =
        pkg in homePackages || launcherHints.any { pkg.startsWith(it) }

    /** True when Shizuku is alive AND granted. */
    @Volatile var privilegedEnabled: Boolean = true
    fun ready(): Boolean = privilegedEnabled && ShizukuManager.state().ready

    /** Usage history entries are not running tasks. Negative IDs are UI keys only. */
    fun hasUsageAccess(): Boolean = runCatching {
        context.getSystemService(android.app.AppOpsManager::class.java)
            .checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), ownPackage) == android.app.AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false).also { if (!it) usageHistory.invalidate() }

    fun getForegroundUsage(): Map<String, Long> {
        if (!hasUsageAccess()) return emptyMap()
        val end = System.currentTimeMillis()
        return usageHistory.read(end, android.os.SystemClock.elapsedRealtime()) { begin, until ->
            val usage = context.getSystemService(android.app.usage.UsageStatsManager::class.java)
            val lastForeground = HashMap<String, Long>()
            val events = usage.queryEvents(begin, until)
            if (events != null) {
                val event = android.app.usage.UsageEvents.Event()
                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    if (event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
                        val pkg = event.packageName ?: continue
                        lastForeground[pkg] = maxOf(lastForeground[pkg] ?: 0L, event.timeStamp)
                    }
                }
            }
            lastForeground
        }
    }
    fun getRecentApps(exceptions: Set<String>, lastForeground: Map<String, Long> = getForegroundUsage()): List<RecentTask> {
        if (!hasUsageAccess()) return emptyList()
        return lastForeground.entries.asSequence()
            .sortedWith(compareByDescending<Map.Entry<String, Long>> { it.value }.thenBy { it.key })
            .filter { (pkg, _) ->
                pkg !in skipPackages && pkg !in exceptions && !isHome(pkg) && pkgRepo.isUsable(pkg) &&
                    (context.packageManager.getLeanbackLaunchIntentForPackage(pkg)
                        ?: context.packageManager.getLaunchIntentForPackage(pkg)) != null
            }
            .take(MAX_TASKS)
            .mapIndexed { i, entry -> RecentTask(-i - 1, entry.key, pkgRepo.label(entry.key),
                pkgRepo.icon(entry.key), pkgRepo.category(entry.key), true) }
            .toList()
    }
    // ── reading ──────────────────────────────────────────────────────────
    fun getRecentTasks(exceptions: Set<String>): List<RecentTask> {
        if (!ready()) return emptyList()
        // The privileged getRecentTasks binder call returns only the CALLER's own
        // task under the Shizuku shell identity (shell lacks REAL_GET_TASKS), so it
        // is useless for enumeration here. The shell `am stack list` / `dumpsys`
        // run as shell and see every task — use them as the source of truth.
        val raw = runCatching { readViaShell() }.getOrDefault(emptyList())
        Log.i("RecentsRepo", "read ${raw.size} tasks via shell")
        return raw.asSequence()
            .filter { (taskId, pkg) ->
                taskId > 0 && pkg.isNotBlank() && pkg !in skipPackages && !isHome(pkg) &&
                    pkg !in exceptions && pkgRepo.isUsable(pkg)   // drop disabled/uninstalled
            }
            .distinctBy { it.second }            // collapse multi-task same-package
            .take(MAX_TASKS)                      // cap composition cost on TV
            .map { (taskId, pkg) ->
                RecentTask(
                    taskId = taskId,
                    packageName = pkg,
                    label = pkgRepo.label(pkg),
                    icon = pkgRepo.icon(pkg),
                    category = pkgRepo.category(pkg),
                )
            }.toList()
    }

    /** Primary read path: privileged IActivityTaskManager.getRecentTasks. */
    private fun readViaBinder(): List<Pair<Int, String>> {
        val atm = activityTaskManager() ?: return emptyList()
        // getRecentTasks(int maxNum, int flags, int userId) → ParceledListSlice
        val m = atm.javaClass.getMethod(
            "getRecentTasks", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
        )
        val slice = m.invoke(atm, 50, 0, 0) ?: return emptyList()
        val list = slice.javaClass.getMethod("getList").invoke(slice) as? List<*> ?: return emptyList()
        val out = ArrayList<Pair<Int, String>>()
        for (info in list) {
            if (info == null) continue
            val taskId = readIntField(info, "taskId", "persistentId")
            val pkg = readPackageFromTaskInfo(info)
            if (pkg != null) out.add(taskId to pkg)
        }
        return out
    }

    private fun readPackageFromTaskInfo(info: Any): String? {
        (readField(info, "baseActivity") as? ComponentName)?.let { return it.packageName }
        (readField(info, "topActivity") as? ComponentName)?.let { return it.packageName }
        (readField(info, "realActivity") as? ComponentName)?.let { return it.packageName }
        (readField(info, "baseIntent") as? Intent)?.component?.let { return it.packageName }
        return null
    }

    /**
     * Read the task list as the Shizuku shell. Prefer the small/fast `am stack
     * list` output; fall back to the heavier `dumpsys activity recents` (full MRU)
     * only if that yields nothing.
     */
    private fun readViaShell(): List<Pair<Int, String>> = readViaDumpsys()

    private fun readViaDumpsys(): List<Pair<Int, String>> {
        val out = ArrayList<Pair<Int, String>>()
        val text = execShell("dumpsys activity recents")
        // split into "* Recent #N:" blocks, MRU order
        val blocks = text.split(Regex("\\* Recent #\\d+:"))
        val idRe = Regex("Task\\{[0-9a-fA-F]+ #(\\d+)")
        val affRe = Regex("A=\\d+:([\\w.]+)")
        val realRe = Regex("realActivity=([\\w.]+)/")
        val cmpRe = Regex("baseIntent=.*?cmp=([\\w.]+)/")
        for (b in blocks.drop(1)) {
            val taskId = idRe.find(b)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("#(\\d+)").find(b)?.groupValues?.get(1)?.toIntOrNull() ?: -1
            val pkg = realRe.find(b)?.groupValues?.get(1)
                ?: cmpRe.find(b)?.groupValues?.get(1)
                ?: affRe.find(b)?.groupValues?.get(1)
            if (pkg != null) out.add(taskId to pkg)
        }
        if (out.isEmpty()) {
            // last resort: `am stack list`
            val sl = execShell("am stack list")
            Regex("taskId=(\\d+): ([\\w.]+)/").findAll(sl).forEach {
                out.add(it.groupValues[1].toInt() to it.groupValues[2])
            }
        }
        return out
    }

    // ── actions ──────────────────────────────────────────────────────────
    /** Bring an existing task to the front. Returns false → caller should launch via intent. */
    fun switchTo(task: RecentTask): Boolean {
        if (task.isUsageEntry || !ready()) return false
        val moveTrace = LoadTrace.begin("task-move")
        // reflection moveTaskToFront(IApplicationThread, String, int taskId, int flags, Bundle)
        // — a null caller/options works for a privileged binder. Signatures vary across
        // ROMs, so resolve dynamically: taskId goes in the first int slot, 0 in any other.
        val moved = runCatching {
            val atm = activityTaskManager() ?: return@runCatching false
            val m = atm.javaClass.methods.firstOrNull {
                it.name == "moveTaskToFront" && it.parameterTypes.any { p -> p == Int::class.javaPrimitiveType }
            } ?: return@runCatching false
            val args = TaskMoveArguments.create(m.parameterTypes, task.taskId, Shizuku.getUid())
            m.invoke(atm, *args)
            true
        }.getOrDefault(false)
        if (moved) { moveTrace?.mark("binder-success"); return true }
        moveTrace?.mark("binder-failed")
        // Activity uses the ordinary launch intent if direct task switching is unavailable.
        // Spawning shell monkey duplicates that fallback and delays foreground handoff.
        return false
    }

    /** Remove a recent entry. Apps can own SEVERAL tasks, so remove every task
     *  for the package (else the app re-appears from a sibling task). Optionally
     *  force-stop the app so it can't immediately recreate its task. */
    fun dismiss(task: RecentTask, forceStop: Boolean, protectedApps: Set<String> = emptySet()): Boolean {
        if (task.isUsageEntry || task.packageName in protectedApps || !ready()) return false
        // Force-stop FIRST so the app can't recreate its tasks while we remove them.
        if (forceStop) forceStop(task.packageName)
        val ids = allTaskIdsForPackage(task.packageName).ifEmpty { listOf(task.taskId) }
        var ok = false
        for (id in ids) if (removeTaskViaBinder(id)) ok = true
        return ok
    }

    fun killAll(tasks: List<RecentTask>, forceStop: Boolean, protectedApps: Set<String> = emptySet()) {
        if (!ready()) return
        val pkgs = tasks.filterNot { it.isUsageEntry || it.packageName in protectedApps }.mapTo(HashSet()) { it.packageName }
        if (forceStop) for (p in pkgs) forceStop(p)
        val all = runCatching { readViaShell() }.getOrDefault(emptyList())
        for ((id, pkg) in all) if (pkg in pkgs) removeTaskViaBinder(id)
    }

    private fun allTaskIdsForPackage(pkg: String): List<Int> =
        runCatching { readViaShell() }.getOrDefault(emptyList())
            .filter { it.second == pkg }.map { it.first }

    /** Privileged removeTask — the Shizuku shell identity holds REMOVE_TASKS. */
    private fun removeTaskViaBinder(taskId: Int): Boolean = runCatching {
        val atm = activityTaskManager() ?: return false
        val m = atm.javaClass.getMethod("removeTask", Int::class.javaPrimitiveType)
        val r = m.invoke(atm, taskId)
        Log.i("RecentsRepo", "removeTask($taskId)=$r")
        (r as? Boolean) ?: true
    }.getOrElse {
        Log.w("RecentsRepo", "removeTask($taskId) failed: ${it.message}")
        false
    }

    /** Force-stop the app. Tries the privileged binder first, then ALWAYS runs the
     *  shell `am force-stop` (the Shizuku shell holds FORCE_STOP_PACKAGES) so the
     *  process is reliably killed regardless of reflection quirks. */
    private fun forceStop(pkg: String) {
        runCatching {
            activityManager()?.let { iam ->
                iam.javaClass.getMethod("forceStopPackage", String::class.java, Int::class.javaPrimitiveType)
                    .invoke(iam, pkg, 0)
            }
        }
        val out = execShell("am force-stop $pkg")
        Log.i("RecentsRepo", "force-stop $pkg -> ${out.ifBlank { "ok" }.trim()}")
    }

    // ── privileged binder handles ────────────────────────────────────────
    private fun activityTaskManager(): Any? = runCatching {
        val stub = Class.forName("android.app.IActivityTaskManager\$Stub")
        val asInterface: Method = stub.getMethod("asInterface", IBinder::class.java)
        asInterface.invoke(null, ShizukuBinderWrapper(SystemServiceHelper.getSystemService("activity_task")))
    }.getOrNull()

    private fun activityManager(): Any? = runCatching {
        val stub = Class.forName("android.app.IActivityManager\$Stub")
        val asInterface: Method = stub.getMethod("asInterface", IBinder::class.java)
        asInterface.invoke(null, ShizukuBinderWrapper(SystemServiceHelper.getSystemService("activity")))
    }.getOrNull()

    // ── reflection helpers ───────────────────────────────────────────────
    private fun readField(obj: Any, name: String): Any? = runCatching {
        val f = findField(obj.javaClass, name) ?: return null
        f.isAccessible = true
        f.get(obj)
    }.getOrNull()

    private fun readIntField(obj: Any, vararg names: String): Int {
        for (n in names) (readField(obj, n) as? Int)?.let { return it }
        return -1
    }

    private fun findField(start: Class<*>, name: String): java.lang.reflect.Field? {
        var c: Class<*>? = start
        while (c != null) {
            try { return c.getDeclaredField(name) } catch (_: NoSuchFieldException) { c = c.superclass }
        }
        return null
    }

    private fun nullFor(p: Class<*>): Any? = when (p) {
        Int::class.javaPrimitiveType -> 0
        Boolean::class.javaPrimitiveType -> false
        Long::class.javaPrimitiveType -> 0L
        else -> null
    }

    private fun execShell(command: String): String = runCatching {
        val newProcess = Shizuku::class.java.getDeclaredMethod(
            "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java
        )
        newProcess.isAccessible = true
        // Redirect stderr so a chatty command can't fill the pipe and deadlock waitFor().
        val process = newProcess.invoke(
            null, arrayOf("sh", "-c", "$command 2>/dev/null"), null, null
        ) as Process
        // Bound a dead binder / shell command so reloads cannot strand an IO worker forever.
        val watchdog = java.util.Timer(true)
        watchdog.schedule(object : java.util.TimerTask() {
            override fun run() { runCatching { process.destroy() } }
        }, 5000L)
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        try {
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) sb.append(line).append('\n')
            process.waitFor()
            sb.toString()
        } finally {
            watchdog.cancel()
            reader.close()
            process.destroy()
        }
    }.getOrElse { Log.w("RecentsRepo", "shell failed: ${it.message}"); "" }
}
