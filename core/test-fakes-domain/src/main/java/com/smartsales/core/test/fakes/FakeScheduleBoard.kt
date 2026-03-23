package com.smartsales.core.test.fakes

import com.smartsales.prism.domain.memory.ConflictResult
import com.smartsales.prism.domain.memory.ScheduleBoard
import com.smartsales.prism.domain.memory.ScheduleItem
import com.smartsales.prism.domain.memory.TargetResolution
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeScheduleBoard : ScheduleBoard {
    private val _upcomingItems = MutableStateFlow<List<ScheduleItem>>(emptyList())
    override val upcomingItems: StateFlow<List<ScheduleItem>> = _upcomingItems
    
    var nextConflictResult: ConflictResult = ConflictResult.Clear
    var lastProposedStart: Long? = null
    var lastDurationMinutes: Int? = null
    var lastExcludeId: String? = null

    override suspend fun checkConflict(
        proposedStart: Long,
        durationMinutes: Int,
        excludeId: String?
    ): ConflictResult {
        lastProposedStart = proposedStart
        lastDurationMinutes = durationMinutes
        lastExcludeId = excludeId
        return nextConflictResult
    }
    
    override suspend fun refresh() {
        // No-op
    }

    var nextLexicalMatch: ScheduleItem? = null
    var nextTargetResolution: TargetResolution? = null

    override suspend fun findLexicalMatch(targetQuery: String): ScheduleItem? {
        return nextLexicalMatch
    }

    override suspend fun resolveTarget(
        targetQuery: String,
        preferredDayOffset: Int?
    ): TargetResolution {
        return nextTargetResolution
            ?: nextLexicalMatch?.let(TargetResolution::Resolved)
            ?: TargetResolution.NoMatch(targetQuery)
    }
}
