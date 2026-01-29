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
import com.smartsales.prism.domain.connectivity.ConnectivityService
import com.smartsales.prism.domain.repository.HistoryRepository
import com.smartsales.prism.ui.drawers.AudioDrawer
import com.smartsales.prism.ui.drawers.HistoryDrawer
import com.smartsales.prism.ui.drawers.SchedulerDrawer
import com.smartsales.prism.ui.settings.UserCenterScreen
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

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
 * Prism 主界面壳 — 管理抽屉状态
 *
 * 结构: SchedulerDrawer(顶部) + ChatScreen(主体) + AudioDrawer(底部) + HistoryDrawer(左侧)
 * @see prism-ui-ux-contract.md §1.1
 */
@Composable
fun PrismShell(
    historyRepository: HistoryRepository,
    connectivityService: ConnectivityService
) {
    // Atomic Drawer State (Mutex)
    // Start with null (Idle) to ensure handles are visible
    var activeDrawer by remember { mutableStateOf<DrawerType?>(null) }
    var showUserCenter by remember { mutableStateOf(false) }
    
    // 会话列表刷新触发器
    var sessionRefreshKey by remember { mutableIntStateOf(0) }
    val groupedSessions = remember(sessionRefreshKey) { historyRepository.getGroupedSessions() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // 主内容层
        Column(modifier = Modifier.fillMaxSize()) {
            // 聊天界面
            PrismChatScreen(
                onMenuClick = { activeDrawer = DrawerType.HISTORY },
                onNewSessionClick = { /* TODO: 新建会话 */ },
                onAudioBadgeClick = { activeDrawer = DrawerType.CONNECTIVITY },
                // onAvatarClick removed (Profile in History)
                onTingwuClick = { activeDrawer = DrawerType.TINGWU },
                onArtifactsClick = { activeDrawer = DrawerType.ARTIFACTS }
            )
        }

        // --- SCRIM LAYER ---
        // Atomic Scrim: Visible if ANY drawer is open
        // Atomic Scrim: Visible if ANY drawer is open (History, Audio, Connectivity, Tingwu, Artifacts)
        if (activeDrawer == DrawerType.HISTORY || activeDrawer == DrawerType.AUDIO || activeDrawer == DrawerType.CONNECTIVITY ||
            activeDrawer == DrawerType.TINGWU || activeDrawer == DrawerType.ARTIFACTS) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(PrismElevation.Scrim)
                    .background(Color.Black.copy(alpha = 0.4f)) // Spec: 0.4 Black
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
                        // TODO: 切换会话
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
        // Note: Scheduler usually persists until dismissed.
        SchedulerDrawer(
            isOpen = activeDrawer == DrawerType.SCHEDULER,
            onDismiss = { activeDrawer = null }
        )

        // 3. Audio Drawer (Bottom)
        // We use a gesture or specific trigger to open this usually.
        Box(modifier = Modifier.zIndex(PrismElevation.Drawer)) {
            AudioDrawer(
                isOpen = activeDrawer == DrawerType.AUDIO,
                onDismiss = { activeDrawer = null },
                onNavigateToChat = { sessionId ->
                    // 关闭 Drawer 并导航到分析会话
                    activeDrawer = null
                    // TODO Phase 3: 实际导航 navController.navigate("chat/$sessionId?mode=analyst")
                    println("PrismShell: Navigate to chat session: $sessionId")
                }
            )
        }

        // 4. Connectivity Modal (Global Overlay)
        if (activeDrawer == DrawerType.CONNECTIVITY) {
            Box(modifier = Modifier.zIndex(PrismElevation.Drawer)) {
                com.smartsales.prism.ui.components.ConnectivityModal(
                    connectivityService = connectivityService,
                    onDismiss = { activeDrawer = null }
                )
            }
        }

        // 5. User Center (Full Screen Overlay)
        if (showUserCenter) {
            UserCenterScreen(
                onClose = { showUserCenter = false }
            )
        }

        // 6. Tingwu Drawer (Right Stub)
        RightDrawerStub(
            isOpen = activeDrawer == DrawerType.TINGWU,
            title = "Tingwu Assistant",
            onDismiss = { activeDrawer = null }
        )

        // 7. Artifacts Drawer (Right Stub)
        RightDrawerStub(
            isOpen = activeDrawer == DrawerType.ARTIFACTS,
            title = "Artifacts Box",
            onDismiss = { activeDrawer = null }
        )

        // --- GHOST HANDLES (Clean Desk) ---
        // Mutex: Only active when NO drawer is open (Safe Blockage)
        if (activeDrawer == null) {
            
            // 1. Scheduler Ghost Handle (Top) - Pull Down
            GhostHandle(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .height(48.dp), // Reduced from 120dp to avoid blocking Header
                threshold = 150f, // Positive = Down drag
                onTrigger = { activeDrawer = DrawerType.SCHEDULER }
            )

            // 2. Audio Ghost Handle (Bottom) - Pull Up
            GhostHandle(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .height(48.dp), // Reduced from 120dp (and removed padding) to avoid blocking Input Bar
                threshold = -150f, // Negative = Up drag
                onTrigger = { activeDrawer = DrawerType.AUDIO }
            )
        }
    }
}

/**
 * Invisible Gesture Trigger for "Clean Desk" UI.
 * @param threshold Drag distance threshold (Positive = Down/Right, Negative = Up/Left)
 */
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
                    // Check threshold direction
                    if ((threshold > 0 && totalDrag > threshold) || 
                        (threshold < 0 && totalDrag < threshold)) {
                        onTrigger()
                        totalDrag = 0f // Reset to prevent multi-trigger
                    }
                }
            }
        // No Content: Invisible (Ghost)
    )
}

/**
 * Stub Drawer for Right Side (Tingwu / Artifacts)
 */
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
            // Scrim handled by parent

            // Drawer Content
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
                    .background(Color(0xFF1A1A2E))
                    .padding(16.dp)
            ) {
                Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))
                Text("🚧 Feature In Progress", color = Color.Gray)
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }
}
