package com.websnake.recentstv.updates

/** Only stable numeric release tags are eligible for automatic updates. */
object UpdatePolicy {
    fun version(value: String): List<Int>? {
        val match = Regex("v?(\\d+)\\.(\\d+)\\.(\\d+)").matchEntire(value) ?: return null
        return match.groupValues.drop(1).map { it.toIntOrNull() ?: return null }
    }

    fun newer(remote: String, installed: String): Boolean {
        val a = version(remote) ?: return false
        val b = version(installed) ?: return false
        for (i in a.indices) if (a[i] != b[i]) return a[i] > b[i]
        return false
    }

    /** Build metadata enables same-version updates; older release versions never replace newer ones. */
    fun offer(remote: String, installed: String, remoteCode: Long?, installedCode: Long): Boolean {
        val a = version(remote) ?: return false
        val b = version(installed) ?: return false
        if (a != b && !newer(remote, installed)) return false
        return if (remoteCode == null) newer(remote, installed)
            else remoteCode in 1..2100000000L && remoteCode > installedCode
    }

    /** Explain a newer release whose APK cannot advance the installed build. */
    fun buildConflict(remote: String, installed: String, remoteCode: Long?, installedCode: Long): Boolean =
        newer(remote, installed) && remoteCode != null && remoteCode in 1..2100000000L && remoteCode <= installedCode

    fun checksum(text: String, filename: String): String? = text.lineSequence().mapNotNull { line ->
        val match = Regex("^([a-fA-F0-9]{64})\\s+\\*?(.+?)\\s*$").matchEntire(line) ?: return@mapNotNull null
        if (match.groupValues[2] == filename) match.groupValues[1].lowercase() else null
    }.singleOrNull()
}
