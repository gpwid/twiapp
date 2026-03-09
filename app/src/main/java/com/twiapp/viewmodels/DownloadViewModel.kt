package com.twiapp.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.twiapp.services.DownloadState
import com.twiapp.services.SmartDownloader
import com.twiapp.utils.Platform
import com.twiapp.utils.UrlParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DownloadUiState(
    val url: String = "",
    val platform: Platform? = null,
    val state: DownloadState = DownloadState.Idle,
    val progress: Float = 0f,
    val eta: String = "",
)

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    /**
     * Start downloading from the given URL (in-app manual download flow).
     */
    fun startDownload(url: String) {
        val platform = UrlParser.detectPlatform(url)
        _uiState.value = DownloadUiState(
            url = url,
            platform = platform,
            state = DownloadState.FetchingInfo
        )

        viewModelScope.launch {
            SmartDownloader.download(getApplication(), url).collect { state ->
                _uiState.value = _uiState.value.copy(
                    state = state,
                    progress = if (state is DownloadState.Downloading) state.progress else _uiState.value.progress,
                    eta = if (state is DownloadState.Downloading) state.eta else _uiState.value.eta,
                )
            }
        }
    }

    /**
     * Reset to idle state for a new download.
     */
    fun reset() {
        _uiState.value = DownloadUiState()
    }
}
