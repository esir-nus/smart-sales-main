// File: feature/chat/src/main/java/com/smartsales/feature/chat/export/ExportEvent.kt
// Module: :feature:chat
// Purpose: Events emitted by ExportViewModel
// Created: 2026-01-07

package com.smartsales.feature.chat.export

import com.smartsales.core.util.Result

/**
 * Events that ExportViewModel emits for completion/error handling.
 */
sealed class ExportEvent {
    /**
     * Export completed successfully or with error.
     */
    data class ExportCompleted(val result: Result<Unit>) : ExportEvent()
}
