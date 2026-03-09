package com.twiapp.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents a single download record in the history.
 */
data class DownloadRecord(
    val id: String = System.currentTimeMillis().toString(),
    val url: String,
    val platform: String,
    val title: String = "",
    val filePath: String = "",
    val contentUri: String = "",
    val fileSize: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val success: Boolean = true
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("url", url)
        put("platform", platform)
        put("title", title)
        put("filePath", filePath)
        put("contentUri", contentUri)
        put("fileSize", fileSize)
        put("timestamp", timestamp)
        put("success", success)
    }

    companion object {
        fun fromJson(json: JSONObject): DownloadRecord = DownloadRecord(
            id = json.optString("id", ""),
            url = json.optString("url", ""),
            platform = json.optString("platform", ""),
            title = json.optString("title", ""),
            filePath = json.optString("filePath", ""),
            contentUri = json.optString("contentUri", ""),
            fileSize = json.optLong("fileSize", 0L),
            timestamp = json.optLong("timestamp", 0),
            success = json.optBoolean("success", true)
        )

        fun listFromJson(jsonStr: String): List<DownloadRecord> {
            if (jsonStr.isBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonStr)
                (0 until array.length()).map { fromJson(array.getJSONObject(it)) }
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun listToJson(records: List<DownloadRecord>): String {
            val array = JSONArray()
            records.forEach { array.put(it.toJson()) }
            return array.toString()
        }
    }
}
