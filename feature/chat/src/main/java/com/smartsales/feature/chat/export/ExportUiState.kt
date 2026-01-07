// File: feature/chat/src/main/java/com/smartsales/feature/chat/export/ExportUiState.kt
// Module: :feature:chat
// Purpose: UI state for export operations
// Created: 2026-01-07

package com.smartsales.feature.chat.export

import com.smartsales.feature.chat.home.export.ExportGateState

/**
 * UI state for export functionality.
 */
data class ExportUiState(
    val inProgress: Boolean = false,
    val gateState: ExportGateState? = null
)
