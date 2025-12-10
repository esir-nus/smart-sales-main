package com.smartsales.feature.chat.core

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/core/ChatFeatureModule.kt
// 模块：:feature:chat
// 说明：为聊天特性层提供 Hilt 绑定，接入真实 AiChatService 与 HomeOrchestrator
// 作者：创建于 2025-12-10

import com.smartsales.feature.chat.core.AiChatService as FeatureAiChatService
import com.smartsales.feature.chat.home.orchestrator.HomeOrchestrator
import com.smartsales.feature.chat.home.orchestrator.HomeOrchestratorImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ChatFeatureModule {

    @Binds
    @Singleton
    abstract fun bindAiChatService(impl: DefaultAiChatService): FeatureAiChatService

    @Binds
    @Singleton
    abstract fun bindHomeOrchestrator(impl: HomeOrchestratorImpl): HomeOrchestrator
}

@Module
@InstallIn(SingletonComponent::class)
object ChatFeatureProvidesModule {

    @Provides
    @Singleton
    fun provideQuickSkillCatalog(): QuickSkillCatalog = DefaultQuickSkillCatalog()
}
