package com.twiapp.utils

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.twiapp.MainActivity
import com.twiapp.TwiApp

/**
 * Creates and manages download notifications.
 */
object NotificationHelper {

    const val NOTIFICATION_ID_BASE = 1000

    /**
     * Build a progress notification for an active download.
     */
    fun buildProgressNotification(
        context: Context,
        title: String,
        progress: Int,
        eta: String = ""
    ): Notification {
        val contentText = if (eta.isNotEmpty()) {
            "Downloading... $progress% • ETA: $eta"
        } else {
            "Downloading... $progress%"
        }

        return NotificationCompat.Builder(context, TwiApp.DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(contentText)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Build a "fetching info" notification (indeterminate progress).
     */
    fun buildFetchingNotification(context: Context, platform: String): Notification {
        return NotificationCompat.Builder(context, TwiApp.DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("TwiApp")
            .setContentText("Fetching $platform media info...")
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Build a success notification with Open and Share action buttons.
     */
    fun buildSuccessNotification(
        context: Context,
        title: String,
        filePath: String,
        mimeType: String,
        notificationId: Int
    ): Notification {
        val fileUri = Uri.parse(filePath)

        // "Open" action
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val openPending = PendingIntent.getActivity(
            context, notificationId * 2,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Share" action
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val sharePending = PendingIntent.getActivity(
            context, notificationId * 2 + 1,
            Intent.createChooser(shareIntent, "Share via"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tap to open app
        val tapIntent = PendingIntent.getActivity(
            context, notificationId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, TwiApp.COMPLETE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download Complete")
            .setContentText(title)
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_view, "Open", openPending)
            .addAction(android.R.drawable.ic_menu_share, "Share", sharePending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    /**
     * Build an error notification with retry capability.
     */
    fun buildErrorNotification(
        context: Context,
        errorMessage: String
    ): Notification {
        val tapIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, TwiApp.COMPLETE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Download Failed")
            .setContentText(errorMessage)
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }
}
