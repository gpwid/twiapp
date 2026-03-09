package com.twiapp.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.twiapp.data.DownloadHistoryManager
import com.twiapp.data.DownloadRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val _records = MutableStateFlow<List<DownloadRecord>>(emptyList())
    val records: StateFlow<List<DownloadRecord>> = _records

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            _records.value = DownloadHistoryManager.getAll(getApplication())
            _isLoading.value = false
        }
    }

    fun deleteRecord(recordId: String) {
        viewModelScope.launch {
            DownloadHistoryManager.deleteRecord(getApplication(), recordId)
            loadHistory()
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            DownloadHistoryManager.clearAll(getApplication())
            _records.value = emptyList()
        }
    }
}
