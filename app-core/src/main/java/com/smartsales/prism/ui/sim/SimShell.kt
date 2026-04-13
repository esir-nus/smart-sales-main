// File: app-core/src/main/java/com/smartsales/prism/ui/sim/SimShell.kt
// Module: :app-core
// Summary: SIM 运行时壳层宿主入口，持有 Hilt 视图模型并将渲染委托给 RuntimeShellContent。
// Author: created on 2026-04-13

package com.smartsales.prism.ui.sim

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartsales.prism.ui.components.DynamicIslandUiState
import com.smartsales.prism.ui.components.connectivity.ConnectivityViewModel
import com.smartsales.prism.ui.onboarding.PairingFlowViewModel

/**
 * SIM 壳层宿主入口。
 * 仅负责 Hilt 视图模型获取与顶层状态采集，不包含 UI 布局或业务规则。
 */
@Composable
internal fun SimShell(
    onForcedFirstLaunchOnboardingCompleted: () -> Unit = {}
) {
    val chatViewModel: SimAgentViewModel = hiltViewModel()
    val audioViewModel: SimAudioDrawerViewModel = hiltViewModel()
    val connectivityViewModel: ConnectivityViewModel = hiltViewModel()
    val pairingViewModel: PairingFlowViewModel = hiltViewModel()
    val schedulerViewModel: SimSchedulerViewModel = viewModel()
    val followUpOwner: SimBadgeFollowUpOwner = viewModel()
    val coroutineScope = rememberCoroutineScope()

    val dependencies = remember(chatViewModel, schedulerViewModel, audioViewModel) {
        DefaultSimShellDependencies(
            chatViewModel = chatViewModel,
            schedulerViewModel = schedulerViewModel,
            audioViewModel = audioViewModel
        )
    }

    var shellState by remember { mutableStateOf(initialRuntimeShellState(forceSetupOnLaunch = false)) }
    val connectivityState by connectivityViewModel.connectionState.collectAsStateWithLifecycle()
    val trackedPendingAudioIds = remember { SnapshotStateMap<String, String>() }
    val isImeVisible = rememberSimImeVisibility()
    val currentSessionId by chatViewModel.currentSessionId.collectAsStateWithLifecycle()
    val groupedSessions by chatViewModel.groupedSessions.collectAsStateWithLifecycle()
    val currentChatAudioId by chatViewModel.currentLinkedAudioId.collectAsStateWithLifecycle()
    val transcriptRevealState by chatViewModel.artifactTranscriptRevealState.collectAsStateWithLifecycle()
    val voiceDraftState by chatViewModel.voiceDraftState.collectAsStateWithLifecycle()
    val activeFollowUp by followUpOwner.activeFollowUp.collectAsStateWithLifecycle()

    RuntimeShellContent(
        chatViewModel = chatViewModel,
        dependencies = dependencies,
        audioViewModel = audioViewModel,
        pairingViewModel = pairingViewModel,
        connectivityViewModel = connectivityViewModel,
        connectivityState = connectivityState,
        shellState = shellState,
        activeReminderBanner = null,
        activeFollowUp = activeFollowUp,
        currentSessionId = currentSessionId,
        groupedSessions = groupedSessions,
        currentChatAudioId = currentChatAudioId,
        transcriptRevealState = transcriptRevealState,
        currentSchedulerFollowUpContext = null,
        selectedSchedulerFollowUpTaskId = null,
        voiceDraftState = voiceDraftState,
        dynamicIslandState = DynamicIslandUiState.Hidden,
        schedulerEnabled = false,
        isImeVisible = isImeVisible,
        showRuntimeIdleComposerHint = false,
        currentSessionHasAudioContextHistory = false,
        trackedPendingAudioIds = trackedPendingAudioIds,
        coroutineScope = coroutineScope,
        onImportTestAudio = {},
        onForcedFirstLaunchOnboardingCompleted = onForcedFirstLaunchOnboardingCompleted,
        onReplayOnboarding = {},
        dismissReminderBanner = {},
        clearFollowUp = { _ -> },
        closeOverlays = { shellState = closeRuntimeOverlays(shellState) },
        openScheduler = { _ -> },
        openAudioDrawer = { _ -> },
        mutateShellState = { transform -> shellState = transform(shellState) }
    )
}
