package com.smartsales.prism.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartsales.prism.domain.model.SchedulerFollowUpContext
import com.smartsales.prism.ui.components.DynamicIslandItem
import com.smartsales.prism.ui.components.DynamicIslandTapAction
import com.smartsales.prism.ui.sim.SimAgentViewModel
import com.smartsales.prism.ui.sim.SimVoiceDraftUiState

enum class AgentIntelligenceVisualMode {
    DEFAULT,
    SIM
}

@Composable
fun AgentIntelligenceScreen(
    viewModel: IAgentViewModel = hiltViewModel<AgentViewModel>(),
    onMenuClick: () -> Unit = {},
    onNewSessionClick: () -> Unit = {},
    onSchedulerClick: (DynamicIslandTapAction) -> Unit = {},
    onAudioBadgeClick: () -> Unit = {},
    onAudioDrawerClick: () -> Unit = {},
    onAttachClick: () -> Unit = onAudioDrawerClick,
    onTingwuClick: () -> Unit = {},
    onArtifactsClick: () -> Unit = {},
    onDebugClick: () -> Unit = {},
    onOnboardingDesignClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    showDebugButton: Boolean = true,
    visualMode: AgentIntelligenceVisualMode = AgentIntelligenceVisualMode.DEFAULT,
    simDynamicIslandItems: List<DynamicIslandItem> = emptyList(),
    showSimHeaderMenuButton: Boolean = true,
    showSimHeaderNewSessionButton: Boolean = true,
    showSimBottomComposer: Boolean = true,
    showSimIdleComposerHint: Boolean = false,
    enableSimSchedulerPullGesture: Boolean = false,
    enableSimAudioPullGesture: Boolean = false,
    onSimSchedulerPullOpen: () -> Unit = {},
    onSimAudioPullOpen: () -> Unit = {},
    simVoiceDraftStateOverride: SimVoiceDraftUiState? = null,
    simVoiceDraftEnabledOverride: Boolean? = null,
    onSimVoiceDraftPermissionRequested: (() -> Unit)? = null,
    onSimVoiceDraftPermissionResult: ((Boolean) -> Unit)? = null,
    onSimVoiceDraftStart: (() -> Boolean)? = null,
    onSimVoiceDraftFinish: (() -> Unit)? = null,
    onSimVoiceDraftCancel: (() -> Unit)? = null
) {
    val history by viewModel.history.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val sessionTitle by viewModel.sessionTitle.collectAsState()
    val agentActivity by viewModel.agentActivity.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val taskBoardItems by viewModel.taskBoardItems.collectAsState()
    val heroUpcoming by viewModel.heroUpcoming.collectAsState()
    val heroAccomplished by viewModel.heroAccomplished.collectAsState()
    val heroGreeting by viewModel.heroGreeting.collectAsState()
    val simViewModel = viewModel as? SimAgentViewModel
    val transcriptRevealState by (simViewModel?.artifactTranscriptRevealState?.collectAsState()
        ?: remember {
            mutableStateOf(
                emptyMap<String, SimAgentViewModel.ArtifactTranscriptRevealState>()
            )
        })
    val currentSchedulerFollowUpContext by (simViewModel?.currentSchedulerFollowUpContext?.collectAsState()
        ?: remember { mutableStateOf<SchedulerFollowUpContext?>(null) })
    val simVoiceDraftObservedState by (simViewModel?.voiceDraftState?.collectAsState()
        ?: remember { mutableStateOf(SimVoiceDraftUiState()) })
    val simVoiceDraftState = simVoiceDraftStateOverride ?: simVoiceDraftObservedState
    val simVoiceDraftEnabled = simVoiceDraftEnabledOverride
        ?: (visualMode == AgentIntelligenceVisualMode.SIM && currentSchedulerFollowUpContext == null)

    val context = LocalContext.current
    androidx.compose.runtime.LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    AgentIntelligenceContent(
        history = history,
        uiState = uiState,
        inputText = inputText,
        isSending = isSending,
        errorMessage = errorMessage,
        sessionTitle = sessionTitle,
        agentActivity = agentActivity,
        taskBoardItems = taskBoardItems,
        heroUpcoming = heroUpcoming,
        heroAccomplished = heroAccomplished,
        heroGreeting = heroGreeting,
        isSimShell = visualMode == AgentIntelligenceVisualMode.SIM,
        transcriptRevealState = transcriptRevealState,
        onArtifactTranscriptRevealConsumed = { messageId, isLongTranscript ->
            simViewModel?.markArtifactTranscriptRevealConsumed(messageId, isLongTranscript)
        },
        onMenuClick = onMenuClick,
        onNewSessionClick = onNewSessionClick,
        onSchedulerClick = onSchedulerClick,
        onAudioBadgeClick = onAudioBadgeClick,
        onAudioDrawerClick = onAudioDrawerClick,
        onAttachClick = onAttachClick,
        onTingwuClick = onTingwuClick,
        onArtifactsClick = onArtifactsClick,
        onDebugClick = onDebugClick,
        onOnboardingDesignClick = onOnboardingDesignClick,
        onProfileClick = onProfileClick,
        showDebugButton = showDebugButton,
        simDynamicIslandItems = simDynamicIslandItems,
        showSimHeaderMenuButton = showSimHeaderMenuButton,
        showSimHeaderNewSessionButton = showSimHeaderNewSessionButton,
        showSimBottomComposer = showSimBottomComposer,
        showSimIdleComposerHint = showSimIdleComposerHint,
        enableSimSchedulerPullGesture = enableSimSchedulerPullGesture,
        enableSimAudioPullGesture = enableSimAudioPullGesture,
        onSimSchedulerPullOpen = onSimSchedulerPullOpen,
        onSimAudioPullOpen = onSimAudioPullOpen,
        simVoiceDraftState = simVoiceDraftState,
        simVoiceDraftEnabled = simVoiceDraftEnabled,
        onSimVoiceDraftPermissionRequested = onSimVoiceDraftPermissionRequested
            ?: simViewModel?.let { it::onVoiceDraftPermissionRequested }
            ?: {},
        onSimVoiceDraftPermissionResult = onSimVoiceDraftPermissionResult
            ?: simViewModel?.let { it::onVoiceDraftPermissionResult }
            ?: {},
        onSimVoiceDraftStart = onSimVoiceDraftStart
            ?: simViewModel?.let { it::startVoiceDraft }
            ?: { false },
        onSimVoiceDraftFinish = onSimVoiceDraftFinish
            ?: simViewModel?.let { it::finishVoiceDraft }
            ?: {},
        onSimVoiceDraftCancel = onSimVoiceDraftCancel
            ?: simViewModel?.let { it::cancelVoiceDraft }
            ?: {},
        onUpdateInput = viewModel::updateInput,
        onSend = viewModel::send,
        onConfirmPlan = viewModel::confirmAnalystPlan,
        onAmendPlan = viewModel::amendAnalystPlan,
        onSelectTaskBoardItem = viewModel::selectTaskBoardItem
    )
}
