package de.ole101.rpx.client.state

import de.ole101.rpx.extraction.ExtractionEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExtractionState {
    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting.asStateFlow()

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted.asStateFlow()

    private val _extractedBytes = MutableStateFlow(0L)
    val extractedBytes: StateFlow<Long> = _extractedBytes.asStateFlow()

    private val _totalBytes = MutableStateFlow(0L)
    val totalBytes: StateFlow<Long> = _totalBytes.asStateFlow()

    private val _currentFile = MutableStateFlow<String?>(null)
    val currentFile: StateFlow<String?> = _currentFile.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun updateFromEvent(event: ExtractionEvent) {
        when (event) {
            is ExtractionEvent.Started -> {
                _isExtracting.value = true
                _isCompleted.value = false
                _totalBytes.value = event.totalBytes
                _extractedBytes.value = 0
                _message.value = "Starting..."
                _error.value = null
            }

            is ExtractionEvent.Progress -> {
                _extractedBytes.value = event.bytesProcessed
                _currentFile.value = event.fileName
                _message.value = "Extracting ${event.fileName}".take(60)
            }

            is ExtractionEvent.Message -> {
                _message.value = event.text
            }

            is ExtractionEvent.Error -> {
                _error.value = event.exception.message ?: "Unknown error"
                _isExtracting.value = false
            }

            is ExtractionEvent.Completed -> {
                _isExtracting.value = false
                _isCompleted.value = true
                _extractedBytes.value = _totalBytes.value
            }
        }
    }

    fun reset() {
        _isExtracting.value = false
        _isCompleted.value = false
        _extractedBytes.value = 0
        _totalBytes.value = 0
        _currentFile.value = null
        _message.value = null
        _error.value = null
    }
}
