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
import androidx.hilt.navigation.compose.hiltViewModel

import com.smartsales.prism.ui.drawers.AudioDrawer
import com.smartsales.prism.ui.drawers.HistoryDrawer
import com.smartsales.prism.ui.drawers.HistoryViewModel
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
    onNavigateToSetup: () -> Unit = {}
) {
    // Atomic Drawer State (Mutex)
    // 初始状态: SCHEDULER — 每次进入app自动展示日程抽屉
    var activeDrawer by remember { mutableStateOf<DrawerType?>(DrawerType.SCHEDULER) }
    var showUserCenter by remember { mutableStateOf(false) }
    var showDebugHud by remember { mutableStateOf(false) }
    val prismViewModel: PrismViewModel = hiltViewModel()
    val historyViewModel: HistoryViewModel = hiltViewModel()
    
    // 每次app回到前台时自动展示日程抽屉
    // drop-down动画暗示用户可以 dismiss 查看更多
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                activeDrawer = DrawerType.SCHEDULER
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    // HistoryViewModel 提供分组数据
    val groupedSessions by historyViewModel.groupedSessions.collectAsState()
    // 用户显示名 — 从 PrismViewModel 的 heroGreeting 间接获取
    val heroGreeting by prismViewModel.heroGreeting.collectAsState()
    
    // Wave 4: Mascot UI State
    val mascotState by prismViewModel.mascotState.collectAsState()

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
                onNewSessionClick = { prismViewModel.startNewSession() },
                onAudioBadgeClick = { activeDrawer = DrawerType.CONNECTIVITY },
                onAudioDrawerClick = { activeDrawer = DrawerType.AUDIO },
                onTingwuClick = { activeDrawer = DrawerType.TINGWU },
                onArtifactsClick = { activeDrawer = DrawerType.ARTIFACTS },
                onDebugClick = { showDebugHud = !showDebugHud },
                onProfileClick = { showUserCenter = true }
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
                    displayName = prismViewModel.currentDisplayName,
                    onSessionClick = { sessionId ->
                        activeDrawer = null
                        prismViewModel.switchSession(sessionId)
                    },
                    onDeviceClick = {
                        activeDrawer = DrawerType.CONNECTIVITY
                    },
                    onSettingsClick = {
                        activeDrawer = null
                        showUserCenter = true
                    },
                    onProfileClick = {
                        activeDrawer = null
                        showUserCenter = true
                    },
                    onPinSession = { sessionId ->
                        historyViewModel.togglePin(sessionId)
                    },
                    onRenameSession = { sessionId, clientName, summary ->
                        historyViewModel.renameSession(sessionId, clientName, summary)
                    },
                    onDeleteSession = { sessionId ->
                        historyViewModel.deleteSession(sessionId)
                    }
                )
            }
        }

        // 2. Scheduler Drawer (Top)
        Box(modifier = Modifier.zIndex(PrismElevation.Drawer)) {
            SchedulerDrawer(
                isOpen = activeDrawer == DrawerType.SCHEDULER,
                onDismiss = {
                    activeDrawer = null
                    prismViewModel.refreshHeroDashboard()
                }
            )
        }

        // 3. Audio Drawer (Bottom)
        Box(modifier = Modifier.zIndex(PrismElevation.Drawer)) {
            AudioDrawer(
                isOpen = activeDrawer == DrawerType.AUDIO,
                onDismiss = { activeDrawer = null },
                onNavigateToChat = { sessionId, isNew ->
                    activeDrawer = null
                    println("PrismShell: Navigate to chat session: $sessionId")
                    if (isNew) {
                        prismViewModel.switchSession(sessionId, triggerAutoRename = true)
                    } else {
                        prismViewModel.switchSession(sessionId)
                    }
                }
            )
        }

        // 4. Connectivity Modal (Global Overlay)
        if (activeDrawer == DrawerType.CONNECTIVITY) {
            Box(modifier = Modifier.zIndex(PrismElevation.Drawer)) {
                com.smartsales.prism.ui.components.ConnectivityModal(
                    onDismiss = { activeDrawer = null },
                    onNavigateToSetup = {
                        activeDrawer = null
                        onNavigateToSetup()
                    }
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
            title = "听悟助手",
            onDismiss = { activeDrawer = null }
        )

        RightDrawerStub(
            isOpen = activeDrawer == DrawerType.ARTIFACTS,
            title = "工件箱",
            onDismiss = { activeDrawer = null }
        )

        // --- GHOST HANDLES ---
        if (activeDrawer == null && !showUserCenter) {
            // Scheduler (Top Pull) - 保持较大区域但控制在合理范围
            GhostHandle(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 60.dp) // Clear the header zone
                    .height(80.dp),
                threshold = 80f,
                onTrigger = { activeDrawer = DrawerType.SCHEDULER }
            )
        }
        
        // --- L2 DEBUG HUD (DEBUG BUILDS ONLY) ---
        com.smartsales.prism.ui.debug.L2DebugHud(
            isVisible = showDebugHud,
            onDismiss = { showDebugHud = false },
            modifier = Modifier.zIndex(PrismElevation.Drawer + 10f)
        )
        
        // --- MASCOT OVERLAY (Wave 4) ---
        // Floating slightly down from the top (120dp) to cleanly avoid the Scheduler GhostHandle
        com.smartsales.prism.ui.components.MascotOverlay(
            state = mascotState,
            onInteract = { interaction -> prismViewModel.interactWithMascot(interaction) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 120.dp)
                .zIndex(PrismElevation.Drawer + 5f) // Above content, below high-priority modals
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
                    Text("🚧 功能开发中", color = com.smartsales.prism.ui.theme.TextSecondary)
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = onDismiss) {
                        Text("关闭")
                    }
                }
            }
        }
    }
}
