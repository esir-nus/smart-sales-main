package com.smartsales.aitest.di

// 文件：app/src/main/java/com/smartsales/aitest/di/DeviceConnectionEntryPoint.kt
// 模块：:app
// 说明：为测试暴露设备连接状态流，便于直接推送连接状态
// 作者：创建于 2025-11-27

import com.smartsales.feature.connectivity.DefaultDeviceConnectionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DeviceConnectionEntryPoint {
    /** 直接返回 DefaultDeviceConnectionManager，测试中可注入连接状态 */
    fun connectionManager(): DefaultDeviceConnectionManager
}
