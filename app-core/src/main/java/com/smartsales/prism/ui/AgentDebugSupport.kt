package com.smartsales.prism.ui

import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.model.ClarificationType
import com.smartsales.prism.domain.model.UiState
import java.util.UUID

internal class AgentDebugSupport(
    private val bridge: AgentUiBridge
) {

    fun debugRunScenario(scenario: String) {
        when (scenario) {
            "MARKDOWN_BUBBLE" -> emitAiScenario(
                UiState.MarkdownStrategyState(
                    title = "深度分析完成",
                    markdownContent = """
                        ### 周会分析报告
                        
                        以下是为您准备的客户跟进分析：
                        
                        * **流失预警**: A客户上周未拜访。
                        * **高意向**: B客户主动索要了报价单。
                        
                        **建议行动**：
                        请尽快安排针对A客户的回访，并准备B客户的合同草案。
                    """.trimIndent()
                )
            )
            "CLARIFICATION_BUBBLE" -> emitAiScenario(
                UiState.AwaitingClarification(
                    question = "抱歉主理人，我需要一点细节，请问您想订个多长时间的会议？",
                    clarificationType = ClarificationType.MISSING_DURATION
                )
            )
            "MULTI_INTENT_PROPOSAL" -> emitAiScenario(
                UiState.Response("已为您起草更新：调度会议 [与张总沟通价格] 并更新字段 [dealStage -> Won]。请点击卡片确认。")
            )
            "BADGE_DELEGATION_HINT" -> emitAiScenario(UiState.BadgeDelegationHint)
        }
    }

    private fun emitAiScenario(uiState: UiState) {
        bridge.setUiState(uiState)
        bridge.setHistory(
            bridge.getHistory() + ChatMessage.Ai(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                uiState = uiState
            )
        )
    }
}
