package com.smartsales.feature.chat

import android.content.Context
import androidx.room.Room
import com.smartsales.feature.chat.persistence.AiSessionDao
import com.smartsales.feature.chat.persistence.AiSessionDatabase
import com.smartsales.feature.chat.persistence.RoomAiSessionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// 文件路径: feature/chat/src/main/java/com/smartsales/feature/chat/ChatModule.kt
// 文件作用: 通过Hilt将默认的ChatController注册到全局图
// 最近修改: 2025-11-14
@Module
@InstallIn(SingletonComponent::class)
interface ChatModule {
    @Binds
    @Singleton
    fun bindChatController(impl: DefaultChatController): ChatController
}

@Module
@InstallIn(SingletonComponent::class)
object ChatProvidesModule {
    @Provides
    @Singleton
    fun provideChatDatabase(
        @ApplicationContext context: Context
    ): AiSessionDatabase = Room.databaseBuilder(
        context,
        AiSessionDatabase::class.java,
        "chat_sessions.db"
    ).fallbackToDestructiveMigration().build()

    @Provides
    @Singleton
    fun provideAiSessionDao(database: AiSessionDatabase): AiSessionDao = database.aiSessionDao()

    @Provides
    @Singleton
    fun provideAiSessionRepository(
        dao: AiSessionDao,
        dispatchers: com.smartsales.core.util.DispatcherProvider
    ): AiSessionRepository = RoomAiSessionRepository(dao, dispatchers)

    @Provides
    @Singleton
    fun provideChatShareHandler(
        @ApplicationContext context: Context
    ): ChatShareHandler = AndroidChatShareHandler(context)
}
