package com.smartsales.feature.media.audiofiles

// 文件：feature/media/src/main/java/com/smartsales/feature/media/audiofiles/AudioStorageRepository.kt
// 模块：:feature:media
// 说明：定义统一的本地音频存储抽象，汇聚设备同步与手机上传的录音
// 作者：创建于 2025-11-21

import android.net.Uri
import com.smartsales.feature.media.devicemanager.DeviceMediaFile
import kotlinx.coroutines.flow.Flow

/**
 * 统一管理本地音频文件的接口。
 * - 所有录音都会被复制到应用专属的音频目录。
 * - origin 用于区分来源（设备同步 / 手机上传）。
 */
interface AudioStorageRepository {
    val audios: Flow<List<StoredAudio>>

    /**
 * 从设备媒体服务器下载并写入本地存储。
 */
    suspend fun importFromDevice(baseUrl: String, file: DeviceMediaFile): StoredAudio

    /**
 * 从手机本地 Uri 复制到存储目录。
 */
    suspend fun importFromPhone(uri: Uri): StoredAudio

    suspend fun delete(audioId: String)
}

data class StoredAudio(
    val id: String,
    val displayName: String,
    val sizeBytes: Long,
    val durationMillis: Long?,
    val timestampMillis: Long,
    val origin: AudioOrigin,
    val localUri: Uri,
)

enum class AudioOrigin { DEVICE, PHONE }
