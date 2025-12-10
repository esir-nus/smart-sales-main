package com.smartsales.aitest

// 文件：app/src/androidTest/java/com/smartsales/aitest/FeatureTestChatModule.kt
// 模块：:app
// 说明：测试环境绑定，替换生产版 ChatFeatureModule，提供可预测的假 AiChatService
// 作者：创建于 2025-12-10

import com.smartsales.core.metahub.MetaHub
import com.smartsales.feature.chat.core.AiChatService
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatStreamEvent
import com.smartsales.feature.chat.core.DefaultQuickSkillCatalog
import com.smartsales.feature.chat.core.QuickSkillCatalog
import com.smartsales.feature.chat.home.AiSessionRepository as HomeAiSessionRepository
import com.smartsales.feature.chat.home.ChatMessageUi
import com.smartsales.feature.chat.core.ChatFeatureModule
import com.smartsales.feature.chat.core.ChatFeatureProvidesModule
import com.smartsales.feature.chat.home.orchestrator.HomeOrchestrator
import com.smartsales.feature.chat.home.orchestrator.HomeOrchestratorImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 仅供测试/演示使用的依赖替换：让 AI 立即返回固定内容，避免外部网络依赖。
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [ChatFeatureModule::class, ChatFeatureProvidesModule::class]
)
object FeatureTestChatModule {

    /**
     * 提供假 AiChatService：GENERAL 流返 Delta+Completed，SMART 返回可解析 JSON。
     */
    @Provides
    @Singleton
    fun provideAiChatService(): AiChatService {
        return object : AiChatService {
            override fun streamChat(request: ChatRequest): Flow<ChatStreamEvent> = flow {
                if (request.quickSkillId == null) {
                    emit(ChatStreamEvent.Delta("【测试环境】"))
                    emit(
                        ChatStreamEvent.Completed(
                            "【测试环境 AI 回复示例】你刚才说的是：${request.userMessage.take(30)}。后续可以继续提问或描述需求。"
                        )
                    )
                } else if (request.quickSkillId == "SMART_ANALYSIS") {
                    emit(
                        ChatStreamEvent.Completed(
                            """
                            ```json
                            {
                              "main_person": "测试客户",
                              "short_summary": "测试环境智能分析摘要",
                              "summary_title_6chars": "测试标题",
                              "highlights": ["要点一", "要点二"],
                              "actionable_tips": ["后续行动一"]
                            }
                            ```
                            """.trimIndent()
                        )
                    )
                } else {
                    emit(ChatStreamEvent.Completed("【测试环境】技能 ${request.quickSkillId} 已收到请求。"))
                }
            }
        }
    }

    /** 使用真实 Orchestrator，但输入/输出基于假 AiChatService。 */
    @Provides
    @Singleton
    fun provideHomeOrchestrator(
        aiChatService: AiChatService,
        metaHub: MetaHub
    ): HomeOrchestrator = HomeOrchestratorImpl(aiChatService, metaHub)

    /** 默认快捷技能目录。 */
    @Provides
    @Singleton
    fun provideQuickSkillCatalog(): QuickSkillCatalog = DefaultQuickSkillCatalog()

    /** Home AiSessionRepository 测试桩：返回空历史即可。 */
    @Provides
    @Singleton
    fun provideHomeAiSessionRepository(): HomeAiSessionRepository {
        return object : HomeAiSessionRepository {
            override suspend fun loadOlderMessages(currentTopMessageId: String?): List<ChatMessageUi> =
                emptyList()
        }
    }
}
