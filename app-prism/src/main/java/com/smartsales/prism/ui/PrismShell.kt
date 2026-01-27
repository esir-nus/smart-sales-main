package com.smartsales.prism.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartsales.prism.domain.core.HistoryRepository
import com.smartsales.prism.ui.drawers.HistoryDrawer
import com.smartsales.prism.ui.drawers.SchedulerDrawer

/**
 * Prism 主界面壳 — 管理抽屉状态
 * 
 * 结构: SchedulerDrawer(顶部) + ChatScreen(主体) + HistoryDrawer(左侧)
 * @see prism-ui-ux-contract.md §1.1
 */
@Composable
fun PrismShell(
    historyRepository: HistoryRepository
) {
    var schedulerOpen by remember { mutableStateOf(true) } // 启动时自动下拉
    var historyOpen by remember { mutableStateOf(false) }
    
    val groupedSessions = remember { historyRepository.getGroupedSessions() }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()  // 处理状态栏遮挡
    ) {
        // 主内容层
        Column(modifier = Modifier.fillMaxSize()) {
            // 头部
            ChatHeader(
                onMenuClick = { historyOpen = true },
                onNewSessionClick = { /* TODO: 新建会话 */ }
            )
            
            // 聊天界面（已存在的 PrismChatScreen 内容）
            Box(modifier = Modifier.weight(1f)) {
                PrismChatScreen()
            }
        }
        
        // 历史抽屉（左侧，条件渲染）
        if (historyOpen) {
            // 半透明遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { historyOpen = false }
            )
            
            HistoryDrawer(
                groupedSessions = groupedSessions,
                onSessionClick = { sessionId ->
                    historyOpen = false
                    // TODO: 切换会话
                },
                onSettingsClick = { /* TODO: 打开设置 */ }
            )
        }
        
        // 日程抽屉（顶部，覆盖层）
        SchedulerDrawer(
            isOpen = schedulerOpen,
            onDismiss = { schedulerOpen = false }
        )
    }
}

/**
 * 聊天头部 — 匹配 spec §1.1
 */
@Composable
private fun ChatHeader(
    onMenuClick: () -> Unit,
    onNewSessionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D0D1A))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧: 菜单 + 设备状态
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "History",
                    tint = Color.White
                )
            }
            
            Text(
                text = "📶",
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        // 中间: 会话标题
        Text(
            text = "Session: 新对话",
            color = Color.White,
            fontSize = 14.sp
        )
        
        // 右侧: 新建会话
        IconButton(onClick = onNewSessionClick) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "New Session",
                tint = Color.White
            )
        }
    }
}
