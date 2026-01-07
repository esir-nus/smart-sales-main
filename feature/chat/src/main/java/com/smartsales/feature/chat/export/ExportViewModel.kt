// File: feature/chat/src/main/java/com/smartsales/feature/chat/export/ExportViewModel.kt
// Module: :feature:chat
// Purpose: ViewModel for export operations (PDF, CSV)
// Created: 2026-01-07

package com.smartsales.feature.chat.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.ExportFormat
import com.smartsales.domain.export.ExportCoordinator
import com.smartsales.domain.export.ExportGateState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for export functionality.
 * 
 * Responsibilities:
 * - Check export gate (analysis completion)
 * - Perform PDF/CSV export via ExportCoordinator
 * - Emit completion events
 * 
 * Design: Takes markdown content as parameter (no shared state coupling).
 */
@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportCoordinator: ExportCoordinator
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ExportEvent>()
    val events = _events.asSharedFlow()

    /**
     * Perform export with provided markdown content.
     * Caller provides markdown - no coupling to latestAnalysisMarkdown.
     * 
     * @param sessionId Current session ID
     * @param format PDF or CSV
     * @param userName User name for export metadata
     * @param markdown Export content (from SmartAnalysis or transcript)
     */
    fun performExport(
        sessionId: String,
        format: ExportFormat,
        userName: String,
        markdown: String
    ) {
        if (_uiState.value.inProgress) return
        
        viewModelScope.launch {
            // Check gate first
            val gate = exportCoordinator.checkExportGate(sessionId)
            _uiState.update { it.copy(gateState = gate) }
            
            if (!gate.ready) {
                // Gate not ready - don't export
                return@launch
            }
            
            // Perform export
            _uiState.update { it.copy(inProgress = true) }
            val result = exportCoordinator.performExport(
                sessionId = sessionId,
                format = format,
                userName = userName,
                markdown = markdown
            )
            
            _uiState.update { it.copy(inProgress = false) }
            _events.emit(ExportEvent.ExportCompleted(result))
        }
    }

    /**
     * Check export gate without performing export.
     * Useful for UI to show gate status.
     */
    fun checkGate(sessionId: String) {
        viewModelScope.launch {
            val gate = exportCoordinator.checkExportGate(sessionId)
            _uiState.update { it.copy(gateState = gate) }
        }
    }
}
