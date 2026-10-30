package com.websnake.recentstv.data

import org.json.JSONObject

data class ProfileEntry(val key: String, val status: String, val value: String, val reason: String)

data class ProfileImport(val name: String, val settings: JSONObject, val recognized: Int, val missing: Int, val skipped: Int, val entries: List<ProfileEntry> = emptyList())

/** Partial, typed settings documents. Missing keys are intentionally not migrations. */
object ProfileCodec {
    const val MAX_BYTES = 65536
    fun document(name: String, settings: JSONObject) = JSONObject()
        .put("format", "RecentAppsTV-profile").put("formatVersion", 1)
        .put("name", name.take(40)).put("settings", settings)

    fun accepts(type: String, value: Any?): Boolean = value == JSONObject.NULL || when (type) {
        "strings" -> value is org.json.JSONArray && value.length() <= 32 && (0 until value.length()).all { value.opt(it) is String && value.getString(it).length <= 80 }
        "boolean" -> value is Boolean
        "string" -> value is String && value.length <= 256
        "int" -> value is Number && value.toDouble().isFinite() && value.toDouble() == value.toInt().toDouble()
        else -> false
    }

    fun inspect(raw: String, schema: Map<String, String>): ProfileImport {
        require(raw.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Profile is too large" }
        val document = JSONObject(raw)
        require(document.optString("format") == "RecentAppsTV-profile") { "Not a RecentAppsTV profile" }
        val incoming = document.getJSONObject("settings")
        val accepted = JSONObject()
        var skipped = 0
        val entries = mutableListOf<ProfileEntry>()
        incoming.keys().forEach { key ->
            val type = schema[key]
            val value = incoming.opt(key)
            if (type != null && accepts(type, value)) {
                accepted.put(key, value)
                entries += ProfileEntry(key, "Apply", if (value == JSONObject.NULL) "App default" else value.toString(), "")
            } else {
                skipped++
                entries += ProfileEntry(key, "Skip", value.toString(), if (type == null) "Unknown setting" else "Invalid value type; expected $type")
            }
        }
        schema.keys.filter { !incoming.has(it) }.forEach { entries += ProfileEntry(it, "Missing", "Unchanged", "Not included in this profile") }
        return ProfileImport(document.optString("name", "Imported profile").trim().take(40).ifEmpty { "Imported profile" },
            accepted, accepted.length(), schema.keys.count { !incoming.has(it) }, skipped, entries.sortedBy { it.key })
    }
}
