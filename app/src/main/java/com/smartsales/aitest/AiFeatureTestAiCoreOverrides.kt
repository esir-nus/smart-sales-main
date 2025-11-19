package com.smartsales.aitest

import com.smartsales.data.aicore.AiCoreConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// 文件：aiFeatureTestApp/src/main/java/com/smartsales/aitest/AiFeatureTestAiCoreOverrides.kt
// 模块：:aiFeatureTestApp
// 说明：测试App默认启用真实 DashScope + 流式，转写/导出仍回退到 Fake
// 作者：创建于 2025-11-16
@Module
@InstallIn(SingletonComponent::class)
object AiFeatureTestAiCoreOverrides {
    @Provides
    @Singleton
    fun provideAiCoreConfig(): AiCoreConfig = AiCoreConfig(
        preferFakeAiChat = false,
        dashscopeMaxRetries = 2,
        dashscopeRequestTimeoutMillis = 15_000,
        dashscopeEnableStreaming = true,
        preferFakeTingwu = false,
        preferFakeExport = true,
        enableTingwuHttpLogging = true,
        tingwuVerboseLogging = true
    )
}
