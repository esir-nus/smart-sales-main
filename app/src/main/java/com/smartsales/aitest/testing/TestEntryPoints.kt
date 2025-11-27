package com.smartsales.aitest.testing

// 文件：app/src/main/java/com/smartsales/aitest/testing/TestEntryPoints.kt
// 模块：:app
// 说明：提供给 androidTest 使用的 Hilt EntryPoint，便于直接访问单例依赖
// 作者：创建于 2025-11-27

import com.smartsales.feature.chat.AiSessionRepository
import com.smartsales.feature.chat.history.ChatHistoryRepository
import com.smartsales.feature.connectivity.DeviceConnectionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DeviceConnectionEntryPoint {
    fun deviceConnectionManager(): DeviceConnectionManager
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ChatHistoryEntryPoint {
    fun aiSessionRepository(): AiSessionRepository
    fun chatHistoryRepository(): ChatHistoryRepository
}
