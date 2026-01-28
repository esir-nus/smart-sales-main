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

/**
 * Drawer Types for Atomic Exclusion
 */
enum class DrawerType {
    HISTORY,
    SCHEDULER,
    AUDIO,
    CONNECTIVITY, // v2.5
    TINGWU,    // Future
    ARTIFACTS  // Future
}

/**
 * Prism 主界面壳 — 管理抽屉状态
 *
 * 结构: SchedulerDrawer(顶部) + ChatScreen(主体) + AudioDrawer(底部) + HistoryDrawer(左侧)
 * @see prism-ui-ux-contract.md §1.1
 */
@Composable
fun PrismShell(
    historyRepository: HistoryRepository
) {
    // Atomic Drawer State (Mutex)
    // Start with null (Idle) to ensure handles are visible
    var activeDrawer by remember { mutableStateOf<DrawerType?>(null) }
    var showUserCenter by remember { mutableStateOf(false) }

    val groupedSessions = remember { historyRepository.getGroupedSessions() }

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
                onAvatarClick = { showUserCenter = true }
            )
        }

        // --- SCRIM LAYER ---
        // Atomic Scrim: Visible if ANY drawer is open
        if (activeDrawer == DrawerType.HISTORY || activeDrawer == DrawerType.AUDIO || activeDrawer == DrawerType.CONNECTIVITY) {
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
            HistoryDrawer(
                groupedSessions = groupedSessions,
                onSessionClick = { sessionId ->
                    activeDrawer = null
                    // TODO: 切换会话
                },
                onSettingsClick = { /* TODO: 打开设置 */ }
            )
        }

        // 2. Scheduler Drawer (Top)
        // Note: Scheduler usually persists until dismissed.
        SchedulerDrawer(
            isOpen = activeDrawer == DrawerType.SCHEDULER,
            onDismiss = { activeDrawer = null }
        )

        // 3. Audio Drawer (Bottom)
        // We use a gesture or specific trigger to open this usually.
        AudioDrawer(
            isOpen = activeDrawer == DrawerType.AUDIO,
            onDismiss = { activeDrawer = null }
        )

        // 4. Connectivity Modal (Global Overlay)
        if (activeDrawer == DrawerType.CONNECTIVITY) {
            com.smartsales.prism.ui.components.ConnectivityModal(
                onDismiss = { activeDrawer = null }
            )
        }

        // 5. User Center (Full Screen Overlay)
        if (showUserCenter) {
            UserCenterScreen(
                onClose = { showUserCenter = false }
            )
        }

        // --- VISUAL HANDLES ---
        // Top Handle (Scheduler) - Visible if Scheduler NOT open
        if (activeDrawer != DrawerType.SCHEDULER) {
            Box( // Top Handle (Scheduler)
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .align(Alignment.TopCenter)
                    .zIndex(PrismElevation.Handles) // Explicit Z-Layer
                    .pointerInput(Unit) {
                        var totalDrag = 0f
                        detectVerticalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onDragEnd = { totalDrag = 0f }
                        ) { _, dragAmount ->
                            totalDrag += dragAmount
                            if (totalDrag > 150f) { // Spec: >50px (approx 50.dp * density ~ 150px)
                                activeDrawer = DrawerType.SCHEDULER
                                totalDrag = 0f // Reset to prevent multi-trigger
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Visual Pill
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .background(Color.Gray.copy(alpha = 0.3f), androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                )
            }
        }

        // Bottom Handle (Audio) - Visible if Audio NOT open
        if (activeDrawer != DrawerType.AUDIO) {
            // Industrial Standard: Large Hit Slop for gestures
            // Expanded zone: 120dp height (~15% screen), positioned just above default input bar height
            Box( 
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp) // HUGE Hit Area (User Request: "Bigger area")
                    .align(Alignment.BottomCenter) 
                    .zIndex(PrismElevation.Handles) // Explicit Z-Layer
                    .padding(bottom = 60.dp) // Lowered to overlap likely thumb zone, but clear nav bar
                    .pointerInput(Unit) {
                        var totalDrag = 0f
                        detectVerticalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onDragEnd = { totalDrag = 0f }
                        ) { _, dragAmount ->
                            totalDrag += dragAmount
                            // Threshold: -50dp (approx -150f)
                            if (totalDrag < -150f) { 
                                activeDrawer = DrawerType.AUDIO
                                totalDrag = 0f
                            }
                        }
                    },
                contentAlignment = Alignment.BottomCenter // Align Visual Pill to bottom of hit zone
            ) {
                // Visual Pill (centered horizontally, near bottom of hit zone)
                Box(
                    modifier = Modifier
                        .padding(bottom = 24.dp) // Visual offset within the hit box
                        .width(64.dp) // Wider pill for better affordance
                        .height(6.dp) // Thicker pill
                        .background(Color.White.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                )
            }
        }
    }
}
