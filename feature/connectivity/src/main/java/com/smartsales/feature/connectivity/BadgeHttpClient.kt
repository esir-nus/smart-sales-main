package com.smartsales.feature.connectivity

// 文件：feature/connectivity/src/main/java/com/smartsales/feature/connectivity/BadgeHttpClient.kt
// 模块：:feature:connectivity
// 说明：ESP32徽章HTTP通信客户端，支持JPG上传和WAV下载
// 规范：docs/specs/esp32-protocol.md
// 作者：创建于 2026-01-09

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
 * - POST /upload: Upload JPG frame (multipart/form-data)
 * - GET /list: List WAV files (returns JSON array)
 * - GET /download?file=: Download WAV file
 * - POST /delete: Delete WAV file (body: filename=...)
 * 
 * @see docs/specs/esp32-protocol.md for full protocol specification
 */
interface BadgeHttpClient {
    /**
     * Upload a JPG file to badge.
     * @param baseUrl Base URL with port, e.g., "http://192.168.1.100:8088"
     * @param file JPG file to upload (must have .jpg extension)
     * @return Success if HTTP 200, Error otherwise
     */
    suspend fun uploadJpg(baseUrl: String, file: File): Result<Unit>

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
     * @return Success if downloaded, Error otherwise
     */
    suspend fun downloadWav(baseUrl: String, filename: String, dest: File): Result<Unit>

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
     * @return true if reachable (HEAD /list returns 2xx)
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

    override suspend fun uploadJpg(baseUrl: String, file: File): Result<Unit> =
        withContext(dispatchers.io) {
            // Validate file extension
            if (!file.name.lowercase().endsWith(".jpg")) {
                return@withContext Result.Error(
                    BadgeHttpException.InvalidFormatException("Only .jpg files allowed, got: ${file.name}")
                )
            }
            
            // Validate file exists
            if (!file.exists()) {
                return@withContext Result.Error(
                    BadgeHttpException.NotFoundException("File not found: ${file.absolutePath}")
                )
            }
            
            // Validate file size (max 10MB per spec)
            if (file.length() > MAX_FILE_SIZE_BYTES) {
                return@withContext Result.Error(
                    BadgeHttpException.InvalidFormatException("File too large: ${file.length()} bytes (max: $MAX_FILE_SIZE_BYTES)")
                )
            }

            runCatching {
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file",
                        file.name,
                        file.asRequestBody("image/jpeg".toMediaType())
                    )
                    .build()

                val request = Request.Builder()
                    .url("$baseUrl/upload")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    when {
                        response.isSuccessful -> Result.Success(Unit)
                        response.code == 400 -> Result.Error(
                            BadgeHttpException.ClientException(400, response.body?.string() ?: "Bad request")
                        )
                        response.code == 403 -> Result.Error(
                            BadgeHttpException.InvalidFormatException("File type not allowed")
                        )
                        response.code in 400..499 -> Result.Error(
                            BadgeHttpException.ClientException(response.code, "Client error")
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
        dest: File
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
                                input.copyTo(output)
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
                val request = Request.Builder()
                    .url("$baseUrl/list")
                    .head()
                    .build()

                val reachableClient = client.newBuilder()
                    .connectTimeout(REACHABLE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(REACHABLE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build()

                reachableClient.newCall(request).execute().use { response ->
                    response.code in 200..399
                }
            }.getOrElse { false }
        }

    companion object {
        private const val CONNECT_TIMEOUT_SECONDS = 10L
        private const val READ_TIMEOUT_SECONDS = 60L  // For large WAV downloads
        private const val WRITE_TIMEOUT_SECONDS = 30L // For JPG uploads
        private const val REACHABLE_TIMEOUT_SECONDS = 3L
        private const val MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024 // 10MB per spec
    }
}
