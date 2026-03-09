package com.twiapp.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.twiapp.utils.ClipboardWatcher
import com.twiapp.utils.Platform
import com.twiapp.utils.UrlParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val url: String = "",
    val detectedPlatform: Platform? = null,
    val clipboardUrl: String? = null,
    val showClipboardBanner: Boolean = false,
    val isValidUrl: Boolean = false,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Check clipboard for supported URLs on app resume.
     */
    fun checkClipboard() {
        val url = ClipboardWatcher.getSupportedUrl(getApplication())
        _uiState.value = _uiState.value.copy(
            clipboardUrl = url,
            showClipboardBanner = url != null && url != _uiState.value.url
        )
    }

    /**
     * Update the URL text field.
     */
    fun updateUrl(newUrl: String) {
        val platform = UrlParser.detectPlatform(newUrl)
        _uiState.value = _uiState.value.copy(
            url = newUrl,
            detectedPlatform = platform,
            isValidUrl = platform != null,
            showClipboardBanner = _uiState.value.clipboardUrl != null
                && _uiState.value.clipboardUrl != newUrl
        )
    }

    /**
     * Use the URL from clipboard.
     */
    fun useClipboardUrl() {
        val url = _uiState.value.clipboardUrl ?: return
        updateUrl(url)
        _uiState.value = _uiState.value.copy(showClipboardBanner = false)
    }

    /**
     * Dismiss the clipboard banner.
     */
    fun dismissClipboardBanner() {
        _uiState.value = _uiState.value.copy(showClipboardBanner = false)
    }

    /**
     * Clear the URL field.
     */
    fun clearUrl() {
        _uiState.value = _uiState.value.copy(
            url = "",
            detectedPlatform = null,
            isValidUrl = false
        )
    }
}
