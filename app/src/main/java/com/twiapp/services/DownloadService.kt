package com.twiapp.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.twiapp.utils.NotificationHelper
import com.twiapp.utils.UrlParser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.io.File

/**
 * Foreground service that handles downloads in the background.
 * Pushes live progress to DownloadTracker so the UI can display it.
 */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var notificationId = NotificationHelper.NOTIFICATION_ID_BASE

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(EXTRA_URL) ?: run {
            stopSelf()
            return START_NOT_STICKY
        }

        val currentNotificationId = notificationId++
        val platform = UrlParser.detectPlatform(url)
        val trackingId = "${System.currentTimeMillis()}"

        // Register this download in the tracker
        DownloadTracker.addDownload(
            DownloadTracker.ActiveDownload(
                id = trackingId,
                url = url,
                platform = platform?.displayName ?: "Media",
                status = DownloadTracker.Status.FETCHING
            )
        )

        // Start as foreground service with initial notification
        val initialNotification = NotificationHelper.buildFetchingNotification(
            this,
            platform?.displayName ?: "media"
        )
        startForeground(currentNotificationId, initialNotification)

        scope.launch {
            val notificationManager = NotificationManagerCompat.from(this@DownloadService)

            SmartDownloader.download(this@DownloadService, url).collectLatest { state ->
                when (state) {
                    is DownloadState.FetchingInfo -> {
                        DownloadTracker.updateDownload(trackingId, 0f, status = DownloadTracker.Status.FETCHING)
                        val notification = NotificationHelper.buildFetchingNotification(
                            this@DownloadService,
                            platform?.displayName ?: "media"
                        )
                        notificationManager.notify(currentNotificationId, notification)
                    }

                    is DownloadState.Downloading -> {
                        DownloadTracker.updateDownload(trackingId, state.progress, state.eta, DownloadTracker.Status.DOWNLOADING)
                        val notification = NotificationHelper.buildProgressNotification(
                            this@DownloadService,
                            "${platform?.displayName ?: "Media"} Download",
                            state.progress.toInt(),
                            state.eta
                        )
                        notificationManager.notify(currentNotificationId, notification)
                    }

                    is DownloadState.Saving -> {
                        DownloadTracker.updateDownload(trackingId, 100f, status = DownloadTracker.Status.SAVING)
                        val notification = NotificationHelper.buildProgressNotification(
                            this@DownloadService,
                            "Saving to gallery...",
                            100
                        )
                        notificationManager.notify(currentNotificationId, notification)
                    }

                    is DownloadState.Success -> {
                        DownloadTracker.markDone(trackingId)
                        val mimeType = when (state.mediaType) {
                            MediaType.VIDEO -> "video/*"
                            MediaType.IMAGE -> "image/*"
                            MediaType.AUDIO -> "audio/*"
                        }
                        val notification = NotificationHelper.buildSuccessNotification(
                            this@DownloadService,
                            "Saved to gallery ✓",
                            state.filePath,
                            mimeType,
                            currentNotificationId
                        )
                        notificationManager.notify(currentNotificationId, notification)
                        stopForeground(STOP_FOREGROUND_DETACH)
                        stopSelf()
                    }

                    is DownloadState.Error -> {
                        DownloadTracker.markError(trackingId)
                        val notification = NotificationHelper.buildErrorNotification(
                            this@DownloadService,
                            state.message
                        )
                        notificationManager.notify(currentNotificationId, notification)
                        stopForeground(STOP_FOREGROUND_DETACH)
                        stopSelf()
                    }

                    is DownloadState.Idle -> { /* nothing */ }
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_URL = "extra_url"

        fun start(context: Context, url: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                putExtra(EXTRA_URL, url)
            }
            context.startForegroundService(intent)
        }
    }
}
