package com.smartsales.prism.ui.sim

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartsales.prism.ui.AgentIntelligenceScreen
import com.smartsales.prism.ui.PrismElevation
import com.smartsales.prism.ui.drawers.HistoryDrawer
import com.smartsales.prism.ui.drawers.SchedulerDrawer
import com.smartsales.prism.ui.settings.UserCenterScreen
import com.smartsales.prism.ui.theme.BackgroundApp

@Composable
fun SimShell() {
    val chatViewModel: SimAgentViewModel = viewModel()
    val schedulerViewModel: SimSchedulerViewModel = viewModel()
    val audioViewModel: SimAudioDrawerViewModel = viewModel()
    val dependencies = remember(chatViewModel, schedulerViewModel, audioViewModel) {
        DefaultSimShellDependencies(
            chatViewModel = chatViewModel,
            schedulerViewModel = schedulerViewModel,
            audioViewModel = audioViewModel
        )
    }
    var shellState by remember { mutableStateOf(SimShellState()) }
    val groupedSessions by chatViewModel.groupedSessions.collectAsStateWithLifecycle()

    fun closeOverlays() {
        shellState = shellState.copy(
            activeDrawer = null,
            showHistory = false,
            showConnectivity = false,
            showSettings = false
        )
    }

    fun openScheduler() {
        shellState = shellState.copy(
            activeDrawer = SimDrawerType.SCHEDULER,
            showHistory = false,
            showConnectivity = false,
            showSettings = false
        )
    }

    fun openAudioDrawer() {
        shellState = shellState.copy(
            activeDrawer = SimDrawerType.AUDIO,
            showHistory = false,
            showConnectivity = false,
            showSettings = false
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundApp)
    ) {
        AgentIntelligenceScreen(
            viewModel = dependencies.chatViewModel,
            onMenuClick = {
                shellState = shellState.copy(
                    activeDrawer = null,
                    showHistory = true,
                    showConnectivity = false,
                    showSettings = false
                )
            },
            onNewSessionClick = {
                chatViewModel.startNewSession()
                closeOverlays()
            },
            onAudioBadgeClick = {
                shellState = shellState.copy(
                    activeDrawer = null,
                    showHistory = false,
                    showConnectivity = true,
                    showSettings = false
                )
            },
            onAudioDrawerClick = { openAudioDrawer() },
            onProfileClick = {
                shellState = shellState.copy(
                    activeDrawer = null,
                    showHistory = false,
                    showConnectivity = false,
                    showSettings = true
                )
            },
            onDebugClick = {},
            showDebugButton = false
        )

        val showScrim = shellState.activeDrawer != null ||
            shellState.showHistory ||
            shellState.showConnectivity

        AnimatedVisibility(
            visible = showScrim,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(PrismElevation.Scrim)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { closeOverlays() }
            )
        }

        if (shellState.showHistory) {
            Box(modifier = Modifier.zIndex(PrismElevation.Drawer)) {
                HistoryDrawer(
                    groupedSessions = groupedSessions,
                    displayName = chatViewModel.currentDisplayName,
                    onSessionClick = { sessionId ->
                        closeOverlays()
                        chatViewModel.switchSession(sessionId)
                    },
                    onDeviceClick = {
                        shellState = shellState.copy(
                            showHistory = false,
                            showConnectivity = true
                        )
                    },
                    onSettingsClick = {
                        closeOverlays()
                        shellState = shellState.copy(showSettings = true)
                    },
                    onProfileClick = {
                        closeOverlays()
                        shellState = shellState.copy(showSettings = true)
                    },
                    onPinSession = chatViewModel::togglePin,
                    onRenameSession = chatViewModel::renameSession,
                    onDeleteSession = chatViewModel::deleteSession
                )
            }
        }

        Box(modifier = Modifier.zIndex(PrismElevation.Drawer)) {
            SchedulerDrawer(
                isOpen = shellState.activeDrawer == SimDrawerType.SCHEDULER,
                onDismiss = { shellState = shellState.copy(activeDrawer = null) },
                viewModel = dependencies.schedulerViewModel
            )
        }

        Box(modifier = Modifier.zIndex(PrismElevation.Drawer)) {
            SimAudioDrawer(
                isOpen = shellState.activeDrawer == SimDrawerType.AUDIO,
                onDismiss = { shellState = shellState.copy(activeDrawer = null) },
                viewModel = dependencies.audioViewModel,
                onAskAi = { discussion ->
                    val sessionId = chatViewModel.openAudioDiscussion(
                        audioId = discussion.audioId,
                        title = discussion.title,
                        summary = discussion.summary
                    )
                    shellState = shellState.copy(
                        activeDrawer = null,
                        activeChatAudioId = discussion.audioId
                    )
                    chatViewModel.switchSession(sessionId)
                }
            )
        }

        if (shellState.showConnectivity) {
            Box(modifier = Modifier.zIndex(PrismElevation.Drawer)) {
                com.smartsales.prism.ui.components.ConnectivityModal(
                    onDismiss = { shellState = shellState.copy(showConnectivity = false) },
                    onNavigateToSetup = {
                        shellState = shellState.copy(showConnectivity = false)
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = shellState.showSettings,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.zIndex(PrismElevation.Drawer + 1f)
        ) {
            UserCenterScreen(
                onClose = { shellState = shellState.copy(showSettings = false) }
            )
        }

        if (!showScrim && !shellState.showSettings) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(84.dp)
                    .zIndex(PrismElevation.Handles)
                    .pointerInput(Unit) {
                        var accumulatedDrag = 0f
                        detectVerticalDragGestures(
                            onDragEnd = { accumulatedDrag = 0f },
                            onDragCancel = { accumulatedDrag = 0f }
                        ) { _, dragAmount ->
                            if (dragAmount > 0) {
                                accumulatedDrag += dragAmount
                                if (accumulatedDrag > 60f) {
                                    openScheduler()
                                    accumulatedDrag = 0f
                                }
                            }
                        }
                    }
            )
        }
    }
}
