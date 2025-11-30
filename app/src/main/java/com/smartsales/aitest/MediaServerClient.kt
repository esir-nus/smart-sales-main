// 文件：app/src/main/java/com/smartsales/aitest/MediaServerClient.kt
// 模块：:app
// 说明：与硬件媒体HTTP服务交互的轻量客户端
// 作者：创建于 2025-11-21
package com.smartsales.aitest

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.smartsales.core.util.Result
import com.smartsales.feature.media.devicemanager.DeviceMediaFile
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.smartsales.aitest.audio.DeviceMediaDownloader
data class MediaServerFile(
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val modifiedAtMillis: Long,
    val mediaUrl: String,
    val downloadUrl: String,
    val durationMillis: Long? = null,
    val location: String? = null,
    val source: String? = null
) {
    val isVideo: Boolean = mimeType.startsWith("video")
    val isImage: Boolean = mimeType.startsWith("image")
}

class MediaServerClient @Inject constructor(
    @ApplicationContext private val context: Context,
) : DeviceMediaDownloader {
    private val resolver: ContentResolver = context.contentResolver

    suspend fun fetchFiles(baseUrl: String): Result<List<MediaServerFile>> = withContext(Dispatchers.IO) {
        val normalized = normalizeBaseUrl(baseUrl) ?: return@withContext Result.Error(IllegalArgumentException("无效地址"))
        runCatching {
            val connection = (URL("$normalized/files").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            connection.useAndRead { stream ->
                val payload = stream.bufferedReader().use { it.readText() }
                val root = JSONObject(payload)
                val filesArray = root.optJSONArray("files") ?: return@useAndRead emptyList()
                val result = mutableListOf<MediaServerFile>()
                for (i in 0 until filesArray.length()) {
                    val obj = filesArray.getJSONObject(i)
                    val name = obj.getString("name")
                    result.add(
                        MediaServerFile(
                            name = name,
                            sizeBytes = obj.optLong("sizeBytes"),
                            mimeType = obj.optString("mimeType", "application/octet-stream"),
                            modifiedAtMillis = obj.optLong("modifiedAtMillis"),
                            mediaUrl = absoluteUrl(normalized, obj.optString("mediaUrl", "")),
                            downloadUrl = absoluteUrl(normalized, obj.optString("downloadUrl", "")),
                            durationMillis = obj.optLongOrNull("durationMillis"),
                            location = obj.optStringOrNull("location"),
                            source = obj.optStringOrNull("source")
                        )
                    )
                }
                result
            }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it) }
        )
    }

    suspend fun deleteFile(baseUrl: String, fileName: String): Result<Unit> = withContext(Dispatchers.IO) {
        sendSimpleRequest(baseUrl, "/delete/${encode(fileName)}", "DELETE")
    }

    suspend fun applyFile(baseUrl: String, fileName: String): Result<Unit> = withContext(Dispatchers.IO) {
        sendSimpleRequest(baseUrl, "/apply/${encode(fileName)}", "POST")
    }

    suspend fun uploadFile(baseUrl: String, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        val normalized = normalizeBaseUrl(baseUrl) ?: return@withContext Result.Error(IllegalArgumentException("无效地址"))
        val displayName = resolveDisplayName(uri) ?: "upload-${System.currentTimeMillis()}"
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        val connection = (URL("$normalized/upload").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        val boundary = "----SmartSalesBoundary${System.currentTimeMillis()}"
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        val inputStream = resolver.openInputStream(uri) ?: return@withContext Result.Error(IllegalStateException("无法读取文件"))

        val result = runCatching {
            connection.outputStream.use { raw ->
                BufferedOutputStream(raw).use { out ->
                    val prefix = buildString {
                        append("--").append(boundary).append("\r\n")
                        append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(displayName).append("\"\r\n")
                        append("Content-Type: ").append(mimeType).append("\r\n\r\n")
                    }.toByteArray()
                    out.write(prefix)
                    inputStream.use { src ->
                        src.copyTo(out)
                    }
                    out.write("\r\n".toByteArray())
                    out.write("--${boundary}--\r\n".toByteArray())
                }
            }
            val code = connection.responseCode
            if (code in 200..299) Unit else throw IllegalStateException("上传失败 $code")
        }
        connection.disconnect()
        result.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(it) }
        )
    }

    override suspend fun download(baseUrl: String, file: DeviceMediaFile): Result<File> =
        downloadFile(baseUrl, MediaServerFile(
            name = file.name,
            sizeBytes = file.sizeBytes,
            mimeType = file.mimeType,
            modifiedAtMillis = file.modifiedAtMillis,
            mediaUrl = file.mediaUrl,
            downloadUrl = file.downloadUrl
        ))

    suspend fun downloadFile(baseUrl: String, file: MediaServerFile): Result<File> = withContext(Dispatchers.IO) {
        normalizeBaseUrl(baseUrl) ?: return@withContext Result.Error(IllegalArgumentException("无效地址"))
        val targetDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "device-media").apply {
            if (!exists()) mkdirs()
        }
        val destination = File(targetDir, file.name)
        val connection = (URL(file.downloadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        val result = runCatching {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("下载失败 ${connection.responseCode}")
            }
            connection.inputStream.use { input ->
                BufferedInputStream(input).use { src ->
                    FileOutputStream(destination).use { fos ->
                        BufferedOutputStream(fos).use { out ->
                            src.copyTo(out)
                        }
                    }
                }
            }
            destination
        }
        connection.disconnect()
        result.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it) }
        )
    }

    private suspend fun sendSimpleRequest(baseUrl: String, path: String, method: String): Result<Unit> {
        val normalized = normalizeBaseUrl(baseUrl) ?: return Result.Error(IllegalArgumentException("无效地址"))
        return runCatching {
            val connection = (URL("$normalized$path").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 10_000
                readTimeout = 10_000
                doInput = true
            }
            val code = connection.responseCode
            connection.disconnect()
            if (code in 200..299) Unit else throw IllegalStateException("$method 失败 $code")
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(it) }
        )
    }

    private fun resolveDisplayName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            var cursor: Cursor? = null
            try {
                cursor = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    name = cursor.getString(0)
                }
            } catch (e: Exception) {
                Log.w("MediaServerClient", "无法读取文件名", e)
            } finally {
                cursor?.close()
            }
        }
        if (name == null) {
            name = uri.lastPathSegment
        }
        return name
    }

    private fun HttpURLConnection.useAndRead(block: (InputStream) -> List<MediaServerFile>): List<MediaServerFile> {
        try {
            if (responseCode !in 200..299) {
                throw IllegalStateException("请求失败 $responseCode")
            }
            return inputStream.use(block)
        } finally {
            disconnect()
        }
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        optString(key, "").takeIf { it.isNotBlank() }

    private fun JSONObject.optLongOrNull(key: String): Long? {
        if (!has(key)) return null
        val value = optLong(key, Long.MIN_VALUE)
        return if (value == Long.MIN_VALUE) null else value
    }

    companion object {
        private fun normalizeBaseUrl(raw: String): String? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null
            val hasScheme = trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)
            val withScheme = if (hasScheme) trimmed else "http://$trimmed"
            return if (withScheme.endsWith("/")) withScheme.dropLast(1) else withScheme
        }

        private fun absoluteUrl(base: String, relative: String): String {
            if (relative.startsWith("http://") || relative.startsWith("https://")) return relative
            val cleanRelative = if (relative.startsWith("/")) relative else "/$relative"
            return base + cleanRelative
        }

        private fun encode(segment: String): String =
            URLEncoder.encode(segment, StandardCharsets.UTF_8.toString())
    }
}
