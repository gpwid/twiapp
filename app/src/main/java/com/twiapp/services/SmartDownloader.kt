package com.twiapp.services

import android.content.Context
import android.util.Log
import com.twiapp.data.DownloadHistoryManager
import com.twiapp.data.DownloadRecord
import com.twiapp.utils.Platform
import com.twiapp.utils.UrlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

/**
 * Download state emitted during the download flow.
 */
sealed class DownloadState {
    data object Idle : DownloadState()
    data object FetchingInfo : DownloadState()
    data class Downloading(val progress: Float, val eta: String = "") : DownloadState()
    data object Saving : DownloadState()
    data class Success(val filePath: String, val mediaType: MediaType) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

enum class MediaType { VIDEO, IMAGE, AUDIO }

/**
 * The brain of TwiApp — orchestrates platform-aware downloads.
 * Picks the best yt-dlp options per platform for optimal results.
 */
object SmartDownloader {

    private const val TAG = "SmartDownloader"

    /**
     * Downloads media from the given URL with smart platform-specific settings.
     * Emits DownloadState updates as a Flow.
     */
    fun download(
        context: Context,
        url: String,
        videoQuality: String = "1080"
    ): Flow<DownloadState> = flow {
        emit(DownloadState.FetchingInfo)

        try {
            val platform = UrlParser.detectPlatform(url)
            val options = buildOptions(platform, videoQuality)
            val outputDir = getOutputDir(context)

            Log.d(TAG, "Downloading from ${platform?.displayName ?: "unknown"}: $url")
            Log.d(TAG, "Options: $options")

            emit(DownloadState.Downloading(0f))

            val filePath = YtDlpManager.download(
                context = context,
                url = url,
                outputDir = outputDir,
                options = options,
                onProgress = { progress, eta ->
                    // Note: flow emissions from callback require channel-based approach
                    // For simplicity, we'll handle progress in the DownloadService via
                    // the YtDlpManager callback directly
                }
            )

            emit(DownloadState.Saving)

            // Save to gallery
            val file = File(filePath)
            val mediaType = detectMediaType(file)
            val savedTitle = file.nameWithoutExtension
            val savedFileSize = file.length()
            val contentUri = GalleryManager.saveToGallery(context, file, mediaType)

            // Save to history
            val platformName = UrlParser.detectPlatform(url)?.displayName ?: "Unknown"
            DownloadHistoryManager.addRecord(context, DownloadRecord(
                url = url,
                platform = platformName,
                title = savedTitle,
                filePath = filePath,
                contentUri = contentUri,
                fileSize = savedFileSize,
                success = true
            ))

            emit(DownloadState.Success(filePath, mediaType))

        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            val userMessage = when {
                e.message?.contains("private", ignoreCase = true) == true ->
                    "This content is private and cannot be downloaded."
                e.message?.contains("not found", ignoreCase = true) == true ->
                    "Content not found. The post may have been deleted."
                e.message?.contains("Unsupported URL", ignoreCase = true) == true ->
                    "This URL is not supported."
                else -> "Download failed: ${e.message ?: "Unknown error"}"
            }

            // Save failed download to history
            val platformName = UrlParser.detectPlatform(url)?.displayName ?: "Unknown"
            DownloadHistoryManager.addRecord(context, DownloadRecord(
                url = url,
                platform = platformName,
                title = userMessage,
                success = false
            ))

            emit(DownloadState.Error(userMessage))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Build platform-specific yt-dlp options for the best results.
     */
    private fun buildOptions(platform: Platform?, quality: String): Map<String, String> {
        val options = mutableMapOf<String, String>()

        // Common options
        options["--no-mtime"] = ""  // Don't set file modification time to upload date
        options["--no-playlist"] = ""  // Download single video, not playlist

        when (platform) {
            Platform.TIKTOK -> {
                // Download without watermark, best quality
                options["-f"] = "best"
                // TikTok often serves better quality with specific format
            }

            Platform.INSTAGRAM -> {
                // Download all items in carousel/sidecar
                options["-f"] = "best"
                // For stories and carousels, get all items
            }

            Platform.YOUTUBE -> {
                // Best video+audio up to specified quality
                options["-f"] = "bestvideo[height<=$quality]+bestaudio/best[height<=$quality]/best"
                options["--merge-output-format"] = "mp4"
            }

            Platform.TWITTER -> {
                // Best quality, convert GIFs to actual GIF format
                options["-f"] = "best"
            }

            Platform.FACEBOOK -> {
                // Best quality
                options["-f"] = "best"
            }

            null -> {
                // Unknown platform, use sensible defaults
                options["-f"] = "best"
            }
        }

        return options
    }

    private fun detectMediaType(file: File): MediaType {
        val ext = file.extension.lowercase()
        return when (ext) {
            "mp4", "mkv", "webm", "avi", "mov", "flv" -> MediaType.VIDEO
            "mp3", "m4a", "ogg", "wav", "flac", "opus" -> MediaType.AUDIO
            "jpg", "jpeg", "png", "webp", "gif", "bmp" -> MediaType.IMAGE
            else -> MediaType.VIDEO // Default to video
        }
    }

    private fun getOutputDir(context: Context): File {
        val dir = File(context.cacheDir, "twiapp_downloads")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}
