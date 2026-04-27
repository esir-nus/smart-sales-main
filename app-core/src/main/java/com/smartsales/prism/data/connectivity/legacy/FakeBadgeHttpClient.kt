package com.smartsales.prism.data.connectivity.legacy

import com.smartsales.core.util.Result
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// 文件：feature/connectivity/src/main/java/com/smartsales/feature/connectivity/FakeBadgeHttpClient.kt
// 模块：:feature:connectivity
// 说明：用于 UI 测试的假 HTTP 客户端，模拟 ESP32 徽章服务器响应
// 作者：创建于 2026-01-10

/**
 * Fake implementation of [BadgeHttpClient] for testing.
 * 
 * Provides configurable responses for all HTTP operations against the ESP32 badge.
 * Use [setUploadResult], [setListResult], etc. to configure expected outcomes.
 */
@Singleton
class FakeBadgeHttpClient @Inject constructor() : BadgeHttpClient {

    // Configurable responses
    private var listResult: Result<List<String>> = Result.Success(emptyList())
    private val listResults = ArrayDeque<Result<List<String>>>()
    private var downloadResult: Result<Unit> = Result.Success(Unit)
    private var deleteResult: Result<Unit> = Result.Success(Unit)
    private var isReachableResult: Boolean = true
    private val reachableResults = ArrayDeque<Boolean>()

    // Call tracking for verification
    private val listCalls = mutableListOf<String>()
    private val downloadCalls = mutableListOf<DownloadCall>()
    private val deleteCalls = mutableListOf<DeleteCall>()
    private val reachableCalls = mutableListOf<String>()

    // Configuration methods
    fun setListResult(result: Result<List<String>>) { listResult = result }
    fun setListResults(results: List<Result<List<String>>>) {
        listResults.clear()
        listResults.addAll(results)
    }
    fun setDownloadResult(result: Result<Unit>) { downloadResult = result }
    fun setDeleteResult(result: Result<Unit>) { deleteResult = result }
    fun setReachable(reachable: Boolean) { isReachableResult = reachable }
    fun setReachableResults(results: List<Boolean>) {
        reachableResults.clear()
        reachableResults.addAll(results)
    }

    // Reset all state
    fun reset() {
        listResult = Result.Success(emptyList())
        listResults.clear()
        downloadResult = Result.Success(Unit)
        deleteResult = Result.Success(Unit)
        isReachableResult = true
        reachableResults.clear()
        listCalls.clear()
        downloadCalls.clear()
        deleteCalls.clear()
        reachableCalls.clear()
    }

    // Verification helpers
    fun getListCalls(): List<String> = listCalls.toList()
    fun getDownloadCalls(): List<DownloadCall> = downloadCalls.toList()
    fun getDeleteCalls(): List<DeleteCall> = deleteCalls.toList()
    fun getReachableCalls(): List<String> = reachableCalls.toList()


    override suspend fun listWavFiles(baseUrl: String): Result<List<String>> {
        listCalls.add(baseUrl)
        return listResults.removeFirstOrNull() ?: listResult
    }

    override suspend fun downloadWav(
        baseUrl: String,
        filename: String,
        dest: File,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)?
    ): Result<Unit> {
        downloadCalls.add(DownloadCall(baseUrl, filename, dest.absolutePath))
        return downloadResult
    }

    override suspend fun deleteWav(baseUrl: String, filename: String): Result<Unit> {
        deleteCalls.add(DeleteCall(baseUrl, filename))
        return deleteResult
    }

    override suspend fun isReachable(baseUrl: String): Boolean {
        reachableCalls.add(baseUrl)
        return reachableResults.removeFirstOrNull() ?: isReachableResult
    }

    // Data classes for call tracking
    data class DownloadCall(val baseUrl: String, val filename: String, val destPath: String)
    data class DeleteCall(val baseUrl: String, val filename: String)
}
