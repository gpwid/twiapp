package com.twiapp.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks active downloads so the UI can display real-time progress.
 * This is a singleton shared between DownloadService and HistoryScreen.
 */
object DownloadTracker {

    data class ActiveDownload(
        val id: String,
        val url: String,
        val platform: String,
        val progress: Float = 0f,    // 0-100
        val eta: String = "",
        val status: Status = Status.FETCHING
    )

    enum class Status {
        FETCHING, DOWNLOADING, SAVING, DONE, ERROR
    }

    private val _activeDownloads = MutableStateFlow<Map<String, ActiveDownload>>(emptyMap())
    val activeDownloads: StateFlow<Map<String, ActiveDownload>> = _activeDownloads.asStateFlow()

    fun addDownload(download: ActiveDownload) {
        _activeDownloads.value = _activeDownloads.value + (download.id to download)
    }

    fun updateDownload(id: String, progress: Float, eta: String = "", status: Status = Status.DOWNLOADING) {
        val current = _activeDownloads.value[id] ?: return
        _activeDownloads.value = _activeDownloads.value + (id to current.copy(
            progress = progress,
            eta = eta,
            status = status
        ))
    }

    fun markDone(id: String) {
        _activeDownloads.value = _activeDownloads.value - id
    }

    fun markError(id: String) {
        _activeDownloads.value = _activeDownloads.value - id
    }
}
