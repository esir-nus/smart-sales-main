package com.smartsales.prism.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

import com.smartsales.prism.domain.repository.HistoryRepository
import com.smartsales.prism.ui.drawers.AudioDrawer
import com.smartsales.prism.ui.drawers.HistoryDrawer
import com.smartsales.prism.ui.drawers.SchedulerDrawer
import com.smartsales.prism.ui.settings.UserCenterScreen
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.smartsales.prism.ui.theme.BackgroundApp

/**
 * Drawer Types for Atomic Exclusion
 */
enum class DrawerType {
    HISTORY,
    SCHEDULER,
    AUDIO,
    CONNECTIVITY, // v2.5
    TINGWU,    // v2.6.5 Stub
    ARTIFACTS  // v2.6.5 Stub
}

/**
 * Prism Shell — The Core Container (Sleek Glass Version)
 *
 * Updates:
 * - Uses `BackgroundApp` (Light/Aurora) as the root canvas.
 * - Manages atomic drawers and glass interop.
 * @see prism-ui-ux-contract.md §1.1
 */
@Composable
fun PrismShell(
    historyRepository: HistoryRepository
) {
    // Atomic Drawer State (Mutex)
    var activeDrawer by remember { mutableStateOf<DrawerType?>(null) }
    var showUserCenter by remember { mutableStateOf(false) }
    var showDebugHud by remember { mutableStateOf(false) }
    
    // Refresh Trigger
    var sessionRefreshKey by remember { mutableIntStateOf(0) }
    val groupedSessions = remember(sessionRefreshKey) { historyRepository.getGroupedSessions() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundApp) // Global Aurora/Light Background
            .statusBarsPadding()
    ) {
        // Main Content Layer
        Column(modifier = Modifier.fillMaxSize()) {
            PrismChatScreen(
                onMenuClick = { activeDrawer = DrawerType.HISTORY },
                onNewSessionClick = { /* TODO: New Session */ },
                onAudioBadgeClick = { activeDrawer = DrawerType.CONNECTIVITY },
                onTingwuClick = { activeDrawer = DrawerType.TINGWU },
                onArtifactsClick = { activeDrawer = DrawerType.ARTIFACTS },
                onDebugClick = { showDebugHud = !showDebugHud }
            )
        }

        // --- SCRIM LAYER ---
        // Atomic Scrim: Visible if ANY drawer is open
        if (activeDrawer != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(PrismElevation.Scrim)
                    .background(Color.Black.copy(alpha = 0.4f)) // Spec: 0.4 Black Dim
                    .clickable { activeDrawer = null }
            )
        }

        // --- DRAWERS ---

        // 1. History Drawer (Left)
        if (activeDrawer == DrawerType.HISTORY) {
            Box(modifier = Modifier.zIndex(PrismElevation.Drawer)) {
                HistoryDrawer(
                    groupedSessions = groupedSessions,
                    onSessionClick = { sessionId ->
                        activeDrawer = null
                        // TODO: Switch Session
                    },
                    onDeviceClick = {
                        activeDrawer = DrawerType.CONNECTIVITY
                    },
                    onSettingsClick = {
                        activeDrawer = null
                        showUserCenter = true
                    },
                    onPinSession = { sessionId ->
                        historyRepository.togglePin(sessionId)
                        sessionRefreshKey++
                    },
                    onRenameSession = { sessionId, clientName, summary ->
                        historyRepository.renameSession(sessionId, clientName, summary)
                        sessionRefreshKey++
                    },
                    onDeleteSession = { sessionId ->
                        historyRepository.deleteSession(sessionId)
                        sessionRefreshKey++
                    }
                )
            }
        }

        // 2. Scheduler Drawer (Top)
        Box(modifier = Modifier.zIndex(PrismElevation.Drawer)) {
            SchedulerDrawer(
                isOpen = activeDrawer == DrawerType.SCHEDULER,
                onDismiss = { activeDrawer = null }
            )
        }

        // 3. Audio Drawer (Bottom)
        Box(modifier = Modifier.zIndex(PrismElevation.Drawer)) {
            AudioDrawer(
                isOpen = activeDrawer == DrawerType.AUDIO,
                onDismiss = { activeDrawer = null },
                onNavigateToChat = { sessionId ->
                    activeDrawer = null
                    println("PrismShell: Navigate to chat session: $sessionId")
                }
            )
        }

        // 4. Connectivity Modal (Global Overlay)
        if (activeDrawer == DrawerType.CONNECTIVITY) {
            Box(modifier = Modifier.zIndex(PrismElevation.Drawer)) {
                com.smartsales.prism.ui.components.ConnectivityModal(
                    onDismiss = { activeDrawer = null }
                )
            }
        }

        // 5. User Center (Glass Sheet Overlay)
        // 5. User Center (Glass Sheet Overlay)
        androidx.compose.animation.AnimatedVisibility(
            visible = showUserCenter,
            enter = androidx.compose.animation.slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = androidx.compose.animation.core.tween(300)
            ) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = androidx.compose.animation.core.tween(300)
            ) + androidx.compose.animation.fadeOut(),
            modifier = Modifier.zIndex(PrismElevation.Drawer + 1f)
        ) {
            UserCenterScreen(
                onClose = { showUserCenter = false }
            )
        }

        // 6. Right Stubs
        RightDrawerStub(
            isOpen = activeDrawer == DrawerType.TINGWU,
            title = "Tingwu Assistant",
            onDismiss = { activeDrawer = null }
        )

        RightDrawerStub(
            isOpen = activeDrawer == DrawerType.ARTIFACTS,
            title = "Artifacts Box",
            onDismiss = { activeDrawer = null }
        )

        // --- GHOST HANDLES ---
        if (activeDrawer == null && !showUserCenter) {
            // Scheduler (Top Pull) - 更大触摸区域
            GhostHandle(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp) // Clear the header zone
                    .height(120.dp), // 更大的触摸区域
                threshold = 100f, // 降低阈值，更容易触发
                onTrigger = { activeDrawer = DrawerType.SCHEDULER }
            )

            // Audio (Bottom Pull)
            GhostHandle(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .height(48.dp),
                threshold = -150f,
                onTrigger = { activeDrawer = DrawerType.AUDIO }
            )
        }
        
        // --- L2 DEBUG HUD (DEBUG BUILDS ONLY) ---
        com.smartsales.prism.ui.debug.L2DebugHud(
            isVisible = showDebugHud,
            onDismiss = { showDebugHud = false },
            modifier = Modifier.zIndex(PrismElevation.Drawer + 10f)
        )
    }
}

@Composable
private fun GhostHandle(
    modifier: Modifier = Modifier,
    threshold: Float,
    onTrigger: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(PrismElevation.Handles)
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onDragEnd = { totalDrag = 0f }
                ) { _, dragAmount ->
                    totalDrag += dragAmount
                    if ((threshold > 0 && totalDrag > threshold) || 
                        (threshold < 0 && totalDrag < threshold)) {
                        onTrigger()
                        totalDrag = 0f
                    }
                }
            }
    )
}

@Composable
private fun RightDrawerStub(
    isOpen: Boolean,
    title: String,
    onDismiss: () -> Unit
) {
    if (isOpen) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            com.smartsales.prism.ui.components.PrismSurface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
                backgroundColor = com.smartsales.prism.ui.theme.BackgroundSurface.copy(alpha=0.95f)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(title, color = com.smartsales.prism.ui.theme.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("🚧 Feature In Progress", color = com.smartsales.prism.ui.theme.TextSecondary)
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
