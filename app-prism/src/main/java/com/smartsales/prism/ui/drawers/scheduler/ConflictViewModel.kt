package com.smartsales.prism.ui.drawers.scheduler

import androidx.lifecycle.ViewModel
import com.smartsales.prism.domain.scheduler.ConflictResolutionService
import com.smartsales.prism.domain.scheduler.ResolutionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Conflict ViewModel — 注入ConflictResolutionService
 */
@HiltViewModel
class ConflictViewModel @Inject constructor(
    private val conflictService: ConflictResolutionService
) : ViewModel() {
    
    /**
     * 解决冲突
     */
    suspend fun resolveConflict(userMessage: String): ResolutionResult {
        return conflictService.resolveConflict(userMessage)
    }
}
