package com.smartsales.prism.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.smartsales.prism.domain.repository.HistoryRepository
import com.smartsales.prism.ui.drawers.AudioDrawer
import com.smartsales.prism.ui.drawers.HistoryDrawer
import com.smartsales.prism.ui.drawers.SchedulerDrawer

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
    // Start with SCHEDULER open (per brief/legacy behavior) or null
    var activeDrawer by remember { mutableStateOf<DrawerType?>(DrawerType.SCHEDULER) }

    val groupedSessions = remember { historyRepository.getGroupedSessions() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding() // 只处理顶部状态栏
    ) {
        // 主内容层
        Column(modifier = Modifier.fillMaxSize()) {
            // 聊天界面
            PrismChatScreen(
                onMenuClick = { activeDrawer = DrawerType.HISTORY },
                onNewSessionClick = { /* TODO: 新建会话 */ },
                onAudioBadgeClick = { activeDrawer = DrawerType.CONNECTIVITY }
            )
        }

        // --- SCRIM LAYER ---
        // Atomic Scrim: Visible if ANY drawer is open (except maybe Scheduler acting as overlay?
        // Spec says Scrim covers Home for History/Audio. Scheduler usually pushes or overlays.
        // For simplicity, we strictly follow "Scrim covers Home" for side/bottom drawers.
        if (activeDrawer == DrawerType.HISTORY || activeDrawer == DrawerType.AUDIO || activeDrawer == DrawerType.CONNECTIVITY) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
        // For now, ensuring it respects the mutex.
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
    }
}
