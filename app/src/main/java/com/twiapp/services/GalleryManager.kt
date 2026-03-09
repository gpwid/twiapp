package com.twiapp.services

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileInputStream

/**
 * Saves downloaded media files to the device gallery via MediaStore API.
 */
object GalleryManager {

    private const val TAG = "GalleryManager"

    /**
     * Save a file to the device gallery.
     * Uses MediaStore for Android 10+ (scoped storage).
     * The original temp file is deleted after saving.
     */
    fun saveToGallery(context: Context, file: File, mediaType: MediaType): String {
        val contentResolver = context.contentResolver

        val (collection, mimeType, relativePath) = when (mediaType) {
            MediaType.VIDEO -> Triple(
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                getMimeType(file, "video/mp4"),
                Environment.DIRECTORY_MOVIES + "/TwiApp"
            )
            MediaType.IMAGE -> Triple(
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                getMimeType(file, "image/jpeg"),
                Environment.DIRECTORY_PICTURES + "/TwiApp"
            )
            MediaType.AUDIO -> Triple(
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                getMimeType(file, "audio/mpeg"),
                Environment.DIRECTORY_MUSIC + "/TwiApp"
            )
        }

        val now = System.currentTimeMillis() / 1000 // seconds since epoch

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.DATE_ADDED, now)
            put(MediaStore.MediaColumns.DATE_MODIFIED, now)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
                put(MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis())
            }
        }

        val uri = contentResolver.insert(collection, contentValues)
            ?: throw Exception("Failed to create MediaStore entry")

        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(file).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw Exception("Failed to open output stream")

            // Mark as no longer pending
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(uri, contentValues, null, null)
            }

            Log.d(TAG, "Saved to gallery: ${file.name}")

            // Delete the temp file
            if (file.exists()) {
                file.delete()
            }

            return uri.toString()

        } catch (e: Exception) {
            // Clean up the MediaStore entry on failure
            contentResolver.delete(uri, null, null)
            throw e
        }
    }

    private fun getMimeType(file: File, default: String): String {
        return when (file.extension.lowercase()) {
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "mov" -> "video/quicktime"
            "avi" -> "video/x-msvideo"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "ogg" -> "audio/ogg"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            else -> default
        }
    }
}
