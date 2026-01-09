package com.smartsales.feature.media

// 文件：feature/media/src/main/java/com/smartsales/feature/media/WavDownloadCoordinator.kt
// 模块：:feature:media
// 说明：协调WAV下载流程：BLE命令 + HTTP下载
// 作者：创建于 2026-01-09

import android.content.Context
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.BleSession
import com.smartsales.feature.connectivity.gateway.BleGateway
import com.smartsales.feature.connectivity.gateway.WavCommand
import com.smartsales.feature.connectivity.gateway.WavCommandResult
import com.smartsales.feature.connectivity.BadgeHttpClient
import com.smartsales.feature.connectivity.WifiProvisioner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates WAV file download from ESP32 badge.
 * 
 * Flow:
 * 1. Query network to get badge IP
 * 2. BLE: wav#get → wav#send
 * 3. HTTP: GET /list → file list
 * 4. (User selects files in UI)
 * 5. HTTP: GET /download for each file
 * 6. BLE: wav#end → wav#ok
 */
interface WavDownloadCoordinator {
    /**
     * Phase 1: List available WAV files on badge.
     * Returns file list for UI selection.
     */
    fun listFiles(session: BleSession): Flow<WavListState>
    
    /**
     * Phase 2: Download selected files.
     * @param files List of filenames to download
     * @param destDir Destination directory for saved files
     */
    fun downloadFiles(
        session: BleSession,
        files: List<String>,
        destDir: File
    ): Flow<WavDownloadState>
}

sealed class WavListState {
    data object Scanning : WavListState()
    data class Found(val files: List<String>) : WavListState()
    data object Empty : WavListState()
    data class Error(val message: String) : WavListState()
}

sealed class WavDownloadState {
    data class Downloading(val filename: String, val current: Int, val total: Int) : WavDownloadState()
    data object Complete : WavDownloadState()
    data class Error(val message: String) : WavDownloadState()
}

@Singleton
class DefaultWavDownloadCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bleGateway: BleGateway,
    private val wifiProvisioner: WifiProvisioner,
    private val httpClient: BadgeHttpClient,
    private val dispatchers: DispatcherProvider
) : WavDownloadCoordinator {

    override fun listFiles(session: BleSession): Flow<WavListState> = callbackFlow {
        try {
            trySend(WavListState.Scanning)
            
            // Step 1: Query network to get badge IP
            val networkResult = withContext(dispatchers.io) {
                wifiProvisioner.queryNetworkStatus(session)
            }
            if (networkResult is Result.Error) {
                trySend(WavListState.Error("无法查询设备网络状态: ${networkResult.throwable.message}"))
                close()
                return@callbackFlow
            }
            val networkStatus = (networkResult as Result.Success).data
            val baseUrl = "http://${networkStatus.ipAddress}:8088"
            
            // Step 2: BLE command - wav#get
            val getResult = withContext(dispatchers.io) {
                bleGateway.sendWavCommand(session, WavCommand.GET)
            }
            if (getResult !is WavCommandResult.Ready) {
                trySend(WavListState.Error("徽章未准备好发送: $getResult"))
                close()
                return@callbackFlow
            }
            
            // Step 3: HTTP - list files
            val listResult = withContext(dispatchers.io) {
                httpClient.listWavFiles(baseUrl)
            }
            if (listResult is Result.Error) {
                trySend(WavListState.Error("获取文件列表失败: ${listResult.throwable.message}"))
                close()
                return@callbackFlow
            }
            
            val files = (listResult as Result.Success).data
            if (files.isEmpty()) {
                trySend(WavListState.Empty)
            } else {
                trySend(WavListState.Found(files))
            }
            close()
            
        } catch (e: Exception) {
            trySend(WavListState.Error("扫描失败: ${e.message}"))
            close()
        }
        
        awaitClose { }
    }

    override fun downloadFiles(
        session: BleSession,
        files: List<String>,
        destDir: File
    ): Flow<WavDownloadState> = callbackFlow {
        try {
            // Get badge IP
            val networkResult = withContext(dispatchers.io) {
                wifiProvisioner.queryNetworkStatus(session)
            }
            if (networkResult is Result.Error) {
                trySend(WavDownloadState.Error("无法查询设备网络状态"))
                close()
                return@callbackFlow
            }
            val networkStatus = (networkResult as Result.Success).data
            val baseUrl = "http://${networkStatus.ipAddress}:8088"
            
            // Ensure destination directory exists
            if (!destDir.exists()) destDir.mkdirs()
            
            // Download each file
            for ((index, filename) in files.withIndex()) {
                trySend(WavDownloadState.Downloading(filename, index + 1, files.size))
                
                val destFile = File(destDir, filename)
                val downloadResult = withContext(dispatchers.io) {
                    httpClient.downloadWav(baseUrl, filename, destFile)
                }
                if (downloadResult is Result.Error) {
                    trySend(WavDownloadState.Error("下载 $filename 失败: ${downloadResult.throwable.message}"))
                    close()
                    return@callbackFlow
                }
            }
            
            // BLE command - wav#end
            val endResult = withContext(dispatchers.io) {
                bleGateway.sendWavCommand(session, WavCommand.END)
            }
            if (endResult !is WavCommandResult.Done) {
                trySend(WavDownloadState.Error("完成命令失败: $endResult"))
                close()
                return@callbackFlow
            }
            
            trySend(WavDownloadState.Complete)
            close()
            
        } catch (e: Exception) {
            trySend(WavDownloadState.Error("下载失败: ${e.message}"))
            close()
        }
        
        awaitClose { }
    }
}
