package com.smartsales.aitest.ui.screens.device.model

// 文件：app/src/main/java/com/smartsales/aitest/ui/screens/device/model/DeviceModels.kt
// 模块：:app
// 说明：底部导航设备管理页的 UI 专用模型（本地模拟，不接入真实业务）
// 作者：创建于 2025-12-02

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

data class DeviceInfo(
    val id: String,
    val name: String,
    val model: String,
    val batteryLevel: Int,
    val lastConnected: Long?
)

enum class FileType { IMAGE, VIDEO, GIF }

data class DeviceFile(
    val id: String,
    val fileName: String,
    val fileType: FileType,
    val durationSeconds: Int? = null,
    val isApplied: Boolean = false,
    val fileSizeBytes: Long,
    val thumbnailUrl: String? = null
)
