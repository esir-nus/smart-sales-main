package com.smartsales.feature.chat.home

import com.smartsales.core.metahub.ExportNameSource
import com.smartsales.domain.config.QuickSkillId
import com.smartsales.feature.usercenter.SalesPersona
import com.smartsales.data.aicore.debug.DebugSnapshot
import com.smartsales.data.aicore.debug.TingwuTraceSnapshot
import com.smartsales.data.aicore.debug.XfyunTraceSnapshot
import com.smartsales.feature.chat.home.debug.DebugSessionMetadata

import com.smartsales.feature.chat.home.export.ExportGateState

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeUiState.kt
// 模块：:feature:chat
// 说明：Home 页面的 UI 状态定义
// 作者：提取自 HomeViewModel.kt 2026-01-05

/** Home 发出的单次导航请求。 */
sealed class HomeNavigationRequest {
    data object DeviceSetup : HomeNavigationRequest()
    data object DeviceManager : HomeNavigationRequest()
    data object AudioFiles : HomeNavigationRequest()
    data object ChatHistory : HomeNavigationRequest()
    data object UserCenter : HomeNavigationRequest()
}

data class CurrentSessionUi(
    val id: String,
    val title: String,
    val isTranscription: Boolean
)

/** UI 模型：会话列表的一条摘要。 */
data class SessionListItemUi(
    val id: String,
    val title: String,
    val lastMessagePreview: String,
    val updatedAtMillis: Long,
    val isCurrent: Boolean,
    val isTranscription: Boolean,
    val pinned: Boolean = false
)

/** Home 页的不可变 UI 状态。 */
data class HomeUiState(
    val chatMessages: List<ChatMessageUi> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val isStreaming: Boolean = false,
    val isInputBusy: Boolean = false,
    val isBusy: Boolean = false,
    val isLoadingHistory: Boolean = false,
    val quickSkills: List<QuickSkillUi> = emptyList(),
    val selectedSkill: QuickSkillUi? = null,
    val snackbarMessage: String? = null,
    val chatErrorMessage: String? = null,
    val navigationRequest: HomeNavigationRequest? = null,
    val currentSession: CurrentSessionUi = CurrentSessionUi(
        id = "new_chat", // DEFAULT_SESSION_ID constant not visible here, hardcoding or using string
        title = "新对话", // DEFAULT_SESSION_TITLE
        isTranscription = false
    ),
    val userName: String = "用户",
    val exportInProgress: Boolean = false,
    val exportGateState: ExportGateState? = null, // Using nullable or default from elsewhere?
    val showWelcomeHero: Boolean = true,
    val isSmartAnalysisMode: Boolean = false,
    val smartAnalysisGoal: String? = null,  // P3.8: Synced from ConversationState via bridge
    val showDebugMetadata: Boolean = false,
    val debugSessionMetadata: DebugSessionMetadata? = null,
    val debugSnapshot: DebugSnapshot? = null,
    val xfyunTrace: XfyunTraceSnapshot? = null,
    val tingwuTrace: TingwuTraceSnapshot? = null,

    val smartReasoningText: String? = null,
    val isInputFocused: Boolean = false,
    val salesPersona: SalesPersona? = null,
    val showRawAssistantOutput: Boolean = false
)
