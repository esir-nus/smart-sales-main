package com.smartsales.domain.export

import com.smartsales.core.util.Result
import com.smartsales.data.aicore.ExportFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fake implementation for testing. Supports stubbing and call tracking.
 */
class FakeExportCoordinator : ExportCoordinator {
    private val _exportState = MutableStateFlow(ExportUiState())
    override val exportState: StateFlow<ExportUiState> = _exportState

    var stubGateState: ExportGateState = ExportGateState(
        ready = true,
        reason = "",
        resolvedName = "Test",
        nameSource = com.smartsales.core.metahub.ExportNameSource.FALLBACK
    )
    var stubExportResult: Result<Unit> = Result.Success(Unit)
    
    val checkGateCalls = mutableListOf<String>()
    val exportCalls = mutableListOf<ExportParams>()
    var clearSnackbarCalls = 0

    data class ExportParams(
        val sessionId: String,
        val format: ExportFormat,
        val userName: String,
        val markdown: String
    )

    override suspend fun checkExportGate(sessionId: String): ExportGateState {
        checkGateCalls.add(sessionId)
        return stubGateState
    }

    override suspend fun performExport(
        sessionId: String,
        format: ExportFormat,
        userName: String,
        markdown: String
    ): Result<Unit> {
        exportCalls.add(ExportParams(sessionId, format, userName, markdown))
        return stubExportResult
    }

    override fun clearSnackbar() {
        clearSnackbarCalls++
    }

    fun reset() {
        checkGateCalls.clear()
        exportCalls.clear()
        clearSnackbarCalls = 0
    }
}
