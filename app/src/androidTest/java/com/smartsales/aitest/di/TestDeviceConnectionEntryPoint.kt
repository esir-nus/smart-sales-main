package com.smartsales.aitest.di

// 文件：app/src/androidTest/java/com/smartsales/aitest/di/TestDeviceConnectionEntryPoint.kt
// 模块：:app
// 说明：为测试暴露 DeviceConnectionManager，方便注入连接状态
// 作者：创建于 2025-11-27

import com.smartsales.feature.connectivity.DeviceConnectionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TestDeviceConnectionEntryPoint {
    fun deviceConnectionManager(): DeviceConnectionManager
}
