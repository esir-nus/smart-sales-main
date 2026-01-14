package com.smartsales.domain.debug

import com.smartsales.data.aicore.debug.DebugSnapshot
import com.smartsales.data.aicore.debug.TingwuTraceSnapshot

// 文件：feature/chat/src/main/java/com/smartsales/domain/debug/DebugUiState.kt
// 模块：:feature:chat
// 说明：调试面板 UI 状态，从 HomeViewModel 中提取
// 作者：创建于 2026-01-05

/**
 * 调试面板 UI 状态
 */
data class DebugUiState(
    val visible: Boolean = false,
    val sessionMetadata: DebugSessionMetadata? = null,
    val snapshot: DebugSnapshot? = null,
    val tingwuTrace: TingwuTraceSnapshot? = null,
)

/**
 * 调试用会话元数据（用于 HUD 展示）
 */
data class DebugSessionMetadata(
    val sessionId: String,
    val title: String,
    val mainPerson: String? = null,
    val shortSummary: String? = null,
    val summaryTitle6Chars: String? = null,
    val stageLabel: String? = null,
    val riskLabel: String? = null,
    val tagsLabel: String? = null,
    val latestSourceLabel: String? = null,
    val latestAtLabel: String? = null,
    val transcriptionProviderRequested: String? = null,
    val transcriptionProviderSelected: String? = null,
    val transcriptionProviderDisabledReason: String? = null,
    val notes: List<String> = emptyList()
)
