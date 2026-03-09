package com.twiapp.services

import android.content.Context
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo
import com.yausername.ffmpeg.FFmpeg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Wrapper around youtubedl-android (yt-dlp).
 * Handles initialization, updates, info extraction, and downloads.
 * Uses lazy initialization — yt-dlp is initialized on first use.
 */
object YtDlpManager {

    private const val TAG = "YtDlpManager"
    private var isInitialized = false
    private val initMutex = Mutex()

    /**
     * Initialize yt-dlp if not already initialized.
     * Thread-safe — can be called from multiple coroutines.
     */
    suspend fun init(context: Context) = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext

        initMutex.withLock {
            // Double-check after acquiring lock
            if (isInitialized) return@withLock

            try {
                YoutubeDL.getInstance().init(context.applicationContext)
                FFmpeg.getInstance().init(context.applicationContext)
                isInitialized = true
                Log.d(TAG, "yt-dlp and FFmpeg initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize yt-dlp or FFmpeg", e)
                throw e
            }

            // Try to update yt-dlp to the latest version (non-blocking)
            try {
                val status = YoutubeDL.getInstance().updateYoutubeDL(context.applicationContext)
                Log.d(TAG, "yt-dlp update status: $status")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update yt-dlp (using bundled version): ${e.message}")
            }
        }
    }

    /**
     * Ensure yt-dlp is initialized before use.
     * If not initialized, will attempt to initialize.
     */
    private suspend fun ensureInitialized(context: Context) {
        if (!isInitialized) {
            init(context)
        }
    }

    /**
     * Get media info (title, thumbnail, duration, formats) without downloading.
     */
    suspend fun getMediaInfo(context: Context, url: String): VideoInfo = withContext(Dispatchers.IO) {
        ensureInitialized(context)
        val request = YoutubeDLRequest(url)
        request.addOption("--no-download")
        request.addOption("-f", "best")
        YoutubeDL.getInstance().getInfo(request)
    }

    /**
     * Download media to the specified output directory.
     *
     * @param context Application context for lazy initialization
     * @param url The media URL to download
     * @param outputDir The directory to save the downloaded file
     * @param options Additional yt-dlp options (platform-specific)
     * @param onProgress Callback with progress percentage (0.0 to 100.0) and ETA string
     * @return The downloaded file path
     */
    suspend fun download(
        context: Context,
        url: String,
        outputDir: File,
        options: Map<String, String> = emptyMap(),
        onProgress: ((Float, String) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        ensureInitialized(context)

        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val request = YoutubeDLRequest(url)

        // Output template
        request.addOption("-o", "${outputDir.absolutePath}/%(title).50s.%(ext)s")

        // Apply platform-specific options
        options.forEach { (key, value) ->
            if (value.isEmpty()) {
                request.addOption(key)
            } else {
                request.addOption(key, value)
            }
        }

        // Download with progress callback
        val response = YoutubeDL.getInstance().execute(
            request
        ) { progress, etaInSeconds, line ->
            val eta = if (etaInSeconds > 0) formatEta(etaInSeconds) else ""
            onProgress?.invoke(progress, eta)
        }

        Log.d(TAG, "Download complete. Output: ${response.out}")

        // Find the downloaded file
        val files = outputDir.listFiles()
        val downloadedFile = files?.maxByOrNull { it.lastModified() }
        downloadedFile?.absolutePath ?: throw Exception("Downloaded file not found")
    }

    private fun formatEta(seconds: Long): String {
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        }
    }
}
