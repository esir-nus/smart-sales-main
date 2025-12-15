package com.smartsales.aitest

// 文件：app/src/main/java/com/smartsales/aitest/FeatureChatHomeModule.kt
// 模块：:app
// 说明：为 Home 提供默认的 AiSessionRepository，生产环境仅返回空历史，实际历史由上层持久化替换
// 作者：创建于 2025-12-12
import com.smartsales.feature.chat.home.AiSessionRepository
import com.smartsales.feature.chat.home.ChatMessageUi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FeatureChatHomeModule {

    @Provides
    @Singleton
    fun provideHomeAiSessionRepository(): AiSessionRepository {
        return object : AiSessionRepository {
            override suspend fun loadOlderMessages(currentTopMessageId: String?): List<ChatMessageUi> =
                emptyList()
        }
    }
}
