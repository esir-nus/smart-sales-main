package com.smartsales.prism.data.connectivity.legacy

// 文件：feature/connectivity/src/main/java/com/smartsales/feature/connectivity/BadgeHttpClient.kt
// 模块：:feature:connectivity
// 说明：ESP32徽章HTTP通信客户端，支持WAV下载
// 规范：docs/specs/esp32-protocol.md
// 作者：创建于 2026-01-09

import android.util.Log
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HTTP client for ESP32 badge file operations.
 * 
 * Endpoints (port 8088):
 * - GET /list: List WAV files (returns JSON array)
 * - GET /download?file=: Download WAV file
 * - POST /delete: Delete WAV file (body: filename=...)
 * 
 * @see docs/specs/esp32-protocol.md for full protocol specification
 */
interface BadgeHttpClient {
    /**
     * List WAV files on badge.
     * @param baseUrl Base URL with port
     * @return List of filenames (e.g., ["rec1.wav", "rec2.wav"])
     */
    suspend fun listWavFiles(baseUrl: String): Result<List<String>>

    /**
     * Download a WAV file from badge.
     * @param baseUrl Base URL with port
     * @param filename Name of WAV file to download
     * @param dest Destination file to save to
     * @param onProgress Optional callback invoked with (bytesRead, totalBytes) during download
     * @return Success if downloaded, Error otherwise
     */
    suspend fun downloadWav(
        baseUrl: String,
        filename: String,
        dest: File,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null
    ): Result<Unit>

    /**
     * Delete a WAV file from badge.
     * @param baseUrl Base URL with port
     * @param filename Name of WAV file to delete
     * @return Success if deleted, Error otherwise
     */
    suspend fun deleteWav(baseUrl: String, filename: String): Result<Unit>
    
    /**
     * Check if badge HTTP server is reachable.
     * @param baseUrl Base URL with port
     * @return true if reachable (GET / health check returns 2xx)
     */
    suspend fun isReachable(baseUrl: String): Boolean
}

/**
 * Exception types for badge HTTP operations.
 */
sealed class BadgeHttpException(message: String) : Exception(message) {
    /** Network unreachable or timeout */
    class NetworkException(message: String) : BadgeHttpException(message)
    
    /** Server returned 4xx */
    class ClientException(val code: Int, message: String) : BadgeHttpException("$message (HTTP $code)")
    
    /** Server returned 5xx */
    class ServerException(val code: Int, message: String) : BadgeHttpException("$message (HTTP $code)")
    
    /** File not found (404) */
    class NotFoundException(message: String) : BadgeHttpException(message)
    
    /** Invalid file format or extension */
    class InvalidFormatException(message: String) : BadgeHttpException(message)
}

@Singleton
class DefaultBadgeHttpClient @Inject constructor(
    private val dispatchers: DispatcherProvider
) : BadgeHttpClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()


    override suspend fun listWavFiles(baseUrl: String): Result<List<String>> =
        withContext(dispatchers.io) {
            runCatching {
                val request = Request.Builder()
                    .url("$baseUrl/list")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.Error(
                            BadgeHttpException.ServerException(response.code, "List failed")
                        )
                    }

                    val body = response.body?.string() ?: "[]"
                    // Simple JSON array parsing without Android framework dependency
                    // Expected format: ["file1.wav", "file2.wav", ...]
                    val files = parseJsonStringArray(body)
                    Result.Success(files)
                }
            }.getOrElse { e ->
                Result.Error(BadgeHttpException.NetworkException(e.message ?: "Network error"))
            }
        }

    /**
     * Parse a simple JSON string array without Android framework dependency.
     * Expected format: ["string1", "string2", ...]
     */
    private fun parseJsonStringArray(json: String): List<String> {
        val trimmed = json.trim()
        if (trimmed == "[]") return emptyList()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return emptyList()
        
        return trimmed
            .removeSurrounding("[", "]")
            .split(",")
            .mapNotNull { element ->
                val trimmedElement = element.trim()
                if (trimmedElement.startsWith("\"") && trimmedElement.endsWith("\"")) {
                    trimmedElement.removeSurrounding("\"")
                } else {
                    null
                }
            }
    }

    override suspend fun downloadWav(
        baseUrl: String,
        filename: String,
        dest: File,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)?
    ): Result<Unit> = retryOnServerError { downloadWavOnce(baseUrl, filename, dest, onProgress) }

    private suspend fun downloadWavOnce(
        baseUrl: String,
        filename: String,
        dest: File,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)?
    ): Result<Unit> = withContext(dispatchers.io) {
        // Validate filename
        if (!filename.lowercase().endsWith(".wav")) {
            return@withContext Result.Error(
                BadgeHttpException.InvalidFormatException("Only .wav files can be downloaded")
            )
        }

        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/download?file=$filename")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> {
                        response.body?.byteStream()?.use { input ->
                            FileOutputStream(dest).use { output ->
                                val totalBytes = response.body?.contentLength() ?: -1L
                                if (onProgress != null && totalBytes > 0) {
                                    val buffer = ByteArray(8192)
                                    var bytesRead = 0L
                                    var read: Int
                                    while (input.read(buffer).also { read = it } != -1) {
                                        output.write(buffer, 0, read)
                                        bytesRead += read
                                        onProgress(bytesRead, totalBytes)
                                    }
                                } else {
                                    input.copyTo(output)
                                }
                            }
                        }
                        Result.Success(Unit)
                    }
                    response.code == 404 -> Result.Error(
                        BadgeHttpException.NotFoundException("File not found: $filename")
                    )
                    response.code == 403 -> Result.Error(
                        BadgeHttpException.InvalidFormatException("Invalid filename: $filename")
                    )
                    response.code in 400..499 -> Result.Error(
                        BadgeHttpException.ClientException(response.code, "Download failed")
                    )
                    response.code in 500..599 -> Result.Error(
                        BadgeHttpException.ServerException(response.code, "Server error")
                    )
                    else -> Result.Error(
                        Exception("Unexpected response: ${response.code}")
                    )
                }
            }
        }.getOrElse { e ->
            Result.Error(BadgeHttpException.NetworkException(e.message ?: "Network error"))
        }
    }

    override suspend fun deleteWav(baseUrl: String, filename: String): Result<Unit> =
        retryOnServerError { deleteWavOnce(baseUrl, filename) }
    
    private suspend fun deleteWavOnce(baseUrl: String, filename: String): Result<Unit> =
        withContext(dispatchers.io) {
            // Validate filename
            if (!filename.lowercase().endsWith(".wav")) {
                return@withContext Result.Error(
                    BadgeHttpException.InvalidFormatException("Only .wav files can be deleted")
                )
            }

            runCatching {
                // Per spec: POST /delete with body: filename=<name>
                val requestBody = "filename=$filename"
                    .toRequestBody("application/x-www-form-urlencoded".toMediaType())

                val request = Request.Builder()
                    .url("$baseUrl/delete")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    when {
                        response.isSuccessful -> Result.Success(Unit)
                        response.code == 404 -> Result.Error(
                            BadgeHttpException.NotFoundException("File not found: $filename")
                        )
                        response.code == 403 -> Result.Error(
                            BadgeHttpException.InvalidFormatException("Invalid filename: $filename")
                        )
                        response.code in 400..499 -> Result.Error(
                            BadgeHttpException.ClientException(response.code, "Delete failed")
                        )
                        response.code in 500..599 -> Result.Error(
                            BadgeHttpException.ServerException(response.code, "Server error")
                        )
                        else -> Result.Error(
                            Exception("Unexpected response: ${response.code}")
                        )
                    }
                }
            }.getOrElse { e ->
                Result.Error(BadgeHttpException.NetworkException(e.message ?: "Network error"))
            }
        }

    override suspend fun isReachable(baseUrl: String): Boolean =
        withContext(dispatchers.io) {
            runCatching {
                Log.d("SmartSalesConn", "BadgeHttpClient.isReachable start url=$baseUrl/")
                // Use GET / (health check endpoint) instead of HEAD /list
                // ESP32 returns {"status":"ok","version":"1.1.0"} on GET /
                val request = Request.Builder()
                    .url("$baseUrl/")
                    .get()
                    .build()

                val reachableClient = client.newBuilder()
                    .connectTimeout(REACHABLE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(REACHABLE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build()

                reachableClient.newCall(request).execute().use { response ->
                    val reachable = response.code in 200..399
                    Log.d(
                        "SmartSalesConn",
                        "BadgeHttpClient.isReachable response url=$baseUrl/ code=${response.code} reachable=$reachable"
                    )
                    reachable
                }
            }.getOrElse { error ->
                Log.w(
                    "SmartSalesConn",
                    "BadgeHttpClient.isReachable failure url=$baseUrl/ error=${error::class.java.simpleName}:${error.message}"
                )
                false
            }
        }

    companion object {
        private const val CONNECT_TIMEOUT_SECONDS = 10L
        private const val READ_TIMEOUT_SECONDS = 60L  // For large WAV downloads
        private const val WRITE_TIMEOUT_SECONDS = 30L // For WAV delete operations
        private const val REACHABLE_TIMEOUT_SECONDS = 3L
        private const val MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024 // 10MB per spec
    }
    
    /**
     * Retry helper for HTTP operations per spec:
     * - 5xx errors: retry 3x with exponential backoff (1s, 2s, 4s)
     * - Network errors: retry 3x with exponential backoff
     * - 4xx errors: no retry (client error)
     */
    private suspend fun <T> retryOnServerError(
        maxAttempts: Int = 3,
        initialDelayMs: Long = 1000,
        block: suspend () -> Result<T>
    ): Result<T> {
        var attempt = 0
        var lastError: Result<T>? = null
        
        while (attempt < maxAttempts) {
            when (val result = block()) {
                is Result.Success -> return result
                is Result.Error -> {
                    val throwable = result.throwable
                    // Only retry on 5xx or network errors
                    when (throwable) {
                        is BadgeHttpException.ServerException,
                        is BadgeHttpException.NetworkException -> {
                            lastError = result
                            if (attempt < maxAttempts - 1) {
                                val delayMs = initialDelayMs * (1 shl attempt) // 1s, 2s, 4s
                                kotlinx.coroutines.delay(delayMs)
                            }
                            attempt++
                        }
                        else -> return result // 4xx = don't retry
                    }
                }
            }
        }
        return lastError!!
    }
}
