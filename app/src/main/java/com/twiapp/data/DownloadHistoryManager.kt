package com.twiapp.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages download history persistence using a JSON file.
 */
object DownloadHistoryManager {

    private const val TAG = "DownloadHistory"
    private const val FILE_NAME = "download_history.json"
    private const val MAX_RECORDS = 200
    private val mutex = Mutex()

    private fun getFile(context: Context): File =
        File(context.filesDir, FILE_NAME)

    suspend fun addRecord(context: Context, record: DownloadRecord) = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val records = loadRecordsInternal(context).toMutableList()
                records.add(0, record) // newest first
                if (records.size > MAX_RECORDS) {
                    records.subList(MAX_RECORDS, records.size).clear()
                }
                getFile(context).writeText(DownloadRecord.listToJson(records))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save record", e)
            }
        }
    }

    suspend fun getAll(context: Context): List<DownloadRecord> = withContext(Dispatchers.IO) {
        mutex.withLock { loadRecordsInternal(context) }
    }

    suspend fun deleteRecord(context: Context, recordId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val records = loadRecordsInternal(context).filter { it.id != recordId }
                getFile(context).writeText(DownloadRecord.listToJson(records))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete record", e)
            }
        }
    }

    suspend fun clearAll(context: Context) = withContext(Dispatchers.IO) {
        mutex.withLock {
            try { getFile(context).delete() } catch (_: Exception) {}
        }
    }

    private fun loadRecordsInternal(context: Context): List<DownloadRecord> {
        return try {
            val file = getFile(context)
            if (file.exists()) {
                DownloadRecord.listFromJson(file.readText())
            } else emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load history", e)
            emptyList()
        }
    }
}
