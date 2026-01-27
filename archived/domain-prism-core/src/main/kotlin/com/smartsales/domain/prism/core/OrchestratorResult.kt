package com.smartsales.domain.prism.core

/**
 * Orchestrator Pipeline 完整结果
 * @see Prism-V1.md §2.2 #2
 */
data class OrchestratorResult(
    val mode: Mode,
    val executorResult: ExecutorResult,
    val memoryWriteTriggered: Boolean,
    val uiState: UiState
)
