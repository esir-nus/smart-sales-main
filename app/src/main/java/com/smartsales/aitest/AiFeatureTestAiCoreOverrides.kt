package com.smartsales.aitest

import com.smartsales.data.aicore.AiCoreConfig
import com.smartsales.data.aicore.BuildConfig as AiCoreBuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// 文件：app/src/main/java/com/smartsales/aitest/AiFeatureTestAiCoreOverrides.kt
// 模块：:app
// 说明：测试App默认启用真实 DashScope + 流式，转写/导出仍回退到 Fake
// 作者：创建于 2025-11-16
@Module
@InstallIn(SingletonComponent::class)
object AiFeatureTestAiCoreOverrides {
    @Provides
    @Singleton
    fun provideAiCoreConfig(): AiCoreConfig = AiCoreConfig(
        // 如果未提供 DashScope/Tingwu 凭证，自动切换到 Fake，避免仪器化环境阻塞
        preferFakeAiChat = AiCoreBuildConfig.DASHSCOPE_API_KEY.isBlank(),
        dashscopeMaxRetries = 2,
        dashscopeRequestTimeoutMillis = 15_000,
        dashscopeEnableStreaming = true,
        preferFakeTingwu = AiCoreBuildConfig.TINGWU_API_KEY.isBlank(),
        preferFakeExport = true,
        enableTingwuHttpLogging = true,
        tingwuVerboseLogging = true
    )
}
