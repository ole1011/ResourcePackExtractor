package de.ole101.rpx.extraction

sealed class ExtractionEvent {
    data class Started(val totalBytes: Long) : ExtractionEvent()
    data class Progress(val bytesProcessed: Long, val fileName: String) : ExtractionEvent()
    data class Message(val text: String) : ExtractionEvent()
    data class Error(val exception: Exception) : ExtractionEvent()
    object Completed : ExtractionEvent()
}
