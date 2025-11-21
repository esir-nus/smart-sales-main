package com.smartsales.feature.media.audiofiles

// 文件：feature/media/src/main/java/com/smartsales/feature/media/audiofiles/DeviceHttpEndpointProvider.kt
// 模块：:feature:media
// 说明：提供设备 HTTP BaseUrl 的统一接口，避免 AudioFiles 直接依赖连接实现
// 作者：创建于 2025-11-21

import kotlinx.coroutines.flow.Flow

/**
 * 暴露当前设备 HTTP 服务的 BaseUrl。
 * - 连接成功时输出形如 `http://192.168.1.10:8000`
 * - 未连接时输出 null
 */
interface DeviceHttpEndpointProvider {
    val deviceBaseUrl: Flow<String?>
}
