package com.smartsales.aitest.audio

// 文件：app/src/main/java/com/smartsales/aitest/audio/DeviceMediaDownloader.kt
// 模块：:app
// 说明：抽象设备媒体下载能力，便于在本地存储中替换为 Fake
// 作者：创建于 2025-11-21

import com.smartsales.core.util.Result
import com.smartsales.feature.media.devicemanager.DeviceMediaFile
import java.io.File

interface DeviceMediaDownloader {
    suspend fun download(baseUrl: String, file: DeviceMediaFile): Result<File>
}
