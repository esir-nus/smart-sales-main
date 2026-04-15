package com.smartsales.prism.data.audio

import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.domain.connectivity.WavDownloadResult

internal suspend fun ConnectivityBridge.downloadRecording(
    filename: String,
    onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null
): WavDownloadResult {
    val result = downloadRecording(filename)
    if (result is WavDownloadResult.Success) {
        onProgress?.invoke(result.sizeBytes, result.sizeBytes)
    }
    return result
}
