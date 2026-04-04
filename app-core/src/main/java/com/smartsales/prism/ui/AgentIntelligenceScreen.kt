package com.smartsales.prism.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.smartsales.prism.ui.components.DynamicIslandUiState
import com.smartsales.prism.ui.components.DynamicIslandTapAction
import com.smartsales.prism.ui.sim.SimArtifactTranscriptRevealState
import com.smartsales.prism.ui.sim.SimVoiceDraftUiState

enum class AgentIntelligenceVisualMode {
    DEFAULT,
    SIM
}

@Composable
fun AgentIntelligenceScreen(
    viewModel: IAgentViewModel,
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
    simDynamicIslandState: DynamicIslandUiState = DynamicIslandUiState.Hidden,
    showSimHeaderMenuButton: Boolean = true,
    showSimHeaderNewSessionButton: Boolean = true,
    showSimBottomComposer: Boolean = true,
    showSimIdleComposerHint: Boolean = false,
    enableSimSchedulerPullGesture: Boolean = false,
    enableSimAudioPullGesture: Boolean = false,
    onSimSchedulerPullOpen: () -> Unit = {},
    onSimAudioPullOpen: () -> Unit = {},
    transcriptRevealState: Map<String, SimArtifactTranscriptRevealState> = emptyMap(),
    onArtifactTranscriptRevealConsumed: (messageId: String, isLongTranscript: Boolean) -> Unit =
        { _, _ -> },
    simVoiceDraftStateOverride: SimVoiceDraftUiState? = null,
    simVoiceDraftEnabledOverride: Boolean = false,
    onSimVoiceDraftPermissionRequested: () -> Unit = {},
    onSimVoiceDraftPermissionResult: (Boolean) -> Unit = {},
    onSimVoiceDraftStart: () -> Boolean = { false },
    onSimVoiceDraftFinish: () -> Unit = {},
    onSimVoiceDraftCancel: () -> Unit = {}
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
    val simVoiceDraftState = simVoiceDraftStateOverride ?: SimVoiceDraftUiState()
    val simVoiceDraftEnabled = simVoiceDraftEnabledOverride

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
        onArtifactTranscriptRevealConsumed = onArtifactTranscriptRevealConsumed,
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
        simDynamicIslandState = simDynamicIslandState,
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
        onSimVoiceDraftPermissionRequested = onSimVoiceDraftPermissionRequested,
        onSimVoiceDraftPermissionResult = onSimVoiceDraftPermissionResult,
        onSimVoiceDraftStart = onSimVoiceDraftStart,
        onSimVoiceDraftFinish = onSimVoiceDraftFinish,
        onSimVoiceDraftCancel = onSimVoiceDraftCancel,
        onUpdateInput = viewModel::updateInput,
        onSend = viewModel::send,
        onConfirmPlan = viewModel::confirmAnalystPlan,
        onAmendPlan = viewModel::amendAnalystPlan,
        onSelectTaskBoardItem = viewModel::selectTaskBoardItem
    )
}
