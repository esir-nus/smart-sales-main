package com.smartsales.feature.chat.home.sessions

import com.smartsales.feature.chat.home.SessionListItemUi

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/sessions/SessionsUiState.kt
// 模块：:feature:chat
// 说明：会话列表与历史操作相关的 UI 状态
// 作者：创建于 2026-01-05

/**
 * 专门用于管理历史会话列表交互的状态（重命名弹窗、长按选中等）。
 */
data class SessionsUiState(
    // 当前长按选中的会话（用于展示 BottomSheet 操作面板）
    val historyActionSession: SessionListItemUi? = null,
    
    // 是否显示重命名弹窗
    val showHistoryRenameDialog: Boolean = false,
    
    // 重命名输入框的当前文本
    val historyRenameText: String = ""
)
