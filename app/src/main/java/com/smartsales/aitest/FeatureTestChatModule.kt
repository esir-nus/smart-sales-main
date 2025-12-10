package com.smartsales.aitest

import com.smartsales.feature.chat.home.ChatMessageUi
import com.smartsales.feature.chat.home.AiSessionRepository as HomeAiSessionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FeatureTestChatModule {

    /**
     * Home AiSessionRepository binding for tests (message loading).
     * Minimal fake: never loads additional messages.
     */
    @Provides
    @Singleton
    fun provideHomeAiSessionRepository(): HomeAiSessionRepository {
        return object : HomeAiSessionRepository {
            override suspend fun loadOlderMessages(
                currentTopMessageId: String?
            ): List<ChatMessageUi> = emptyList()
        }
    }
}
