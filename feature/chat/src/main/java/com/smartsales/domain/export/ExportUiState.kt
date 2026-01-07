package com.smartsales.domain.export

import com.smartsales.core.metahub.ExportNameSource
import com.smartsales.data.aicore.ExportFormat

// 文件：feature/chat/src/main/java/com/smartsales/domain/export/ExportUiState.kt
// 模块：:feature:chat
// 说明：导出相关的 UI 状态，从 HomeViewModel 中提取
// 作者：创建于 2026-01-05

/** 导出门禁状态：仅当智能分析就绪时允许导出。 */
data class ExportGateState(
    val ready: Boolean,
    val reason: String,
    val resolvedName: String,
    val nameSource: ExportNameSource
)

/** 导出模块的 UI 状态 */
data class ExportUiState(
    val inProgress: Boolean = false,
    val gateState: ExportGateState? = null,
    val pendingFormat: ExportFormat? = null,
    val snackbarMessage: String? = null
)
