package com.twiapp.utils

import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.getSystemService

/**
 * Checks the clipboard for supported social media URLs.
 */
object ClipboardWatcher {

    /**
     * Get a supported URL from the clipboard, if any.
     * Returns null if clipboard is empty or doesn't contain a supported URL.
     */
    fun getSupportedUrl(context: Context): String? {
        val clipboard = context.getSystemService<ClipboardManager>() ?: return null
        val clip = clipboard.primaryClip ?: return null

        if (clip.itemCount == 0) return null

        val text = clip.getItemAt(0).text?.toString() ?: return null
        val url = UrlParser.extractUrl(text) ?: return null

        return if (UrlParser.isSupported(url)) url else null
    }
}
