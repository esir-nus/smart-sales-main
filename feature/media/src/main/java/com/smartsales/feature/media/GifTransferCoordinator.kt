package com.smartsales.feature.media

// 文件：feature/media/src/main/java/com/smartsales/feature/media/GifTransferCoordinator.kt
// 模块：:feature:media
// 说明：协调GIF传输流程：BLE命令 + HTTP上传帧
// 作者：创建于 2026-01-09

import android.content.ContentResolver
import android.net.Uri
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.BleSession
import com.smartsales.feature.connectivity.gateway.BleGateway
import com.smartsales.feature.connectivity.gateway.GifCommand
import com.smartsales.feature.connectivity.gateway.GifCommandResult
import com.smartsales.feature.connectivity.BadgeHttpClient
import com.smartsales.feature.connectivity.WifiProvisioner
import com.smartsales.feature.media.processing.GifFrameExtractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates GIF transfer to ESP32 badge.
 * 
 * Flow:
 * 1. Query network to get badge IP
 * 2. Extract GIF frames
 * 3. BLE: jpg#send → jpg#receive
 * 4. HTTP: Upload each frame
 * 5. BLE: jpg#end → jpg#display
 */
interface GifTransferCoordinator {
    fun transfer(
        session: BleSession,
        gifUri: Uri
    ): Flow<GifTransferState>
}

sealed class GifTransferState {
    data object Preparing : GifTransferState()
    data class Extracting(val current: Int, val total: Int) : GifTransferState()
    data object Connecting : GifTransferState()
    data class Uploading(val current: Int, val total: Int) : GifTransferState()
    data object Finalizing : GifTransferState()
    data object Complete : GifTransferState()
    data class Error(val message: String) : GifTransferState()
}

@Singleton
class DefaultGifTransferCoordinator @Inject constructor(
    private val contentResolver: ContentResolver,
    private val bleGateway: BleGateway,
    private val wifiProvisioner: WifiProvisioner,
    private val httpClient: BadgeHttpClient,
    private val frameExtractor: GifFrameExtractor,
    private val dispatchers: DispatcherProvider
) : GifTransferCoordinator {

    override fun transfer(
        session: BleSession,
        gifUri: Uri
    ): Flow<GifTransferState> = flow {
        withContext(dispatchers.io) {
            emit(GifTransferState.Preparing)
            
            // Step 1: Query network to get badge IP
            val networkResult = wifiProvisioner.queryNetworkStatus(session)
            if (networkResult is Result.Error) {
                emit(GifTransferState.Error("无法查询设备网络状态: ${networkResult.throwable.message}"))
                return@withContext
            }
            val networkStatus = (networkResult as Result.Success).data
            val baseUrl = "http://${networkStatus.ipAddress}:8088"
            
            // Pre-flight check: is badge reachable?
            if (!httpClient.isReachable(baseUrl)) {
                emit(GifTransferState.Error("徽章HTTP服务不可达"))
                return@withContext
            }
            
            // Step 2: Extract GIF frames
            val cacheDir = File(contentResolver.openInputStream(gifUri)?.let {
                it.close()
                // Use app cache directory
                "/data/user/0/com.smartsales.aitest/cache/gif_frames"
            } ?: run {
                emit(GifTransferState.Error("无法读取GIF文件"))
                return@withContext
            })
            
            val extractResult = frameExtractor.extractFrames(
                source = gifUri,
                outputDir = cacheDir,
                onProgress = { current, total ->
                    // Emit extraction progress
                    emit(GifTransferState.Extracting(current, total))
                }
            )
            
            if (extractResult is Result.Error) {
                emit(GifTransferState.Error("帧提取失败: ${extractResult.throwable.message}"))
                return@withContext
            }
            
            val frames = (extractResult as Result.Success).data
            if (frames.isEmpty()) {
                emit(GifTransferState.Error("未提取到任何帧"))
                return@withContext
            }
            
            // Step 3: BLE command - jpg#send
            emit(GifTransferState.Connecting)
            val startResult = bleGateway.sendGifCommand(session, GifCommand.START)
            if (startResult !is GifCommandResult.Ready) {
                emit(GifTransferState.Error("徽章未准备好接收: $startResult"))
                return@withContext
            }
            
            // Step 4: HTTP upload frames
            for ((index, frame) in frames.withIndex()) {
                emit(GifTransferState.Uploading(index + 1, frames.size))
                
                val uploadResult = httpClient.uploadJpg(baseUrl, frame)
                if (uploadResult is Result.Error) {
                    emit(GifTransferState.Error("上传第${index + 1}帧失败: ${uploadResult.throwable.message}"))
                    return@withContext
                }
            }
            
            // Step 5: BLE command - jpg#end
            emit(GifTransferState.Finalizing)
            val endResult = bleGateway.sendGifCommand(session, GifCommand.END)
            if (endResult !is GifCommandResult.DisplayOk) {
                emit(GifTransferState.Error("徽章显示失败: $endResult"))
                return@withContext
            }
            
            // Clean up
            frames.forEach { it.delete() }
            cacheDir.deleteRecursively()
            
            emit(GifTransferState.Complete)
        }
    }
}
