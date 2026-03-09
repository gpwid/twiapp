package com.twiapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.twiapp.services.DownloadService
import com.twiapp.utils.UrlParser

/**
 * Transparent activity that receives shared URLs from other apps.
 * Immediately starts the DownloadService and finishes — user stays in their app.
 */
class ShareReceiverActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleShareIntent(intent)
        finish() // Close immediately — user stays in their app
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
        finish()
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") {
            finish()
            return
        }

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: run {
            Toast.makeText(this, "No URL received", Toast.LENGTH_SHORT).show()
            return
        }

        // Extract URL from shared text (may contain extra text around the link)
        val url = UrlParser.extractUrl(sharedText) ?: run {
            Toast.makeText(this, "No valid URL found", Toast.LENGTH_SHORT).show()
            return
        }

        if (!UrlParser.isSupported(url)) {
            Toast.makeText(this, "Unsupported platform", Toast.LENGTH_SHORT).show()
            return
        }

        val platform = UrlParser.detectPlatform(url)
        Toast.makeText(
            this,
            "⬇️ Downloading ${platform?.displayName ?: "media"}...",
            Toast.LENGTH_SHORT
        ).show()

        // Start background download — user stays in their app
        DownloadService.start(this, url)
    }
}
