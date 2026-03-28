package com.smartsales.core.test.fakes

import com.smartsales.prism.domain.memory.TargetResolutionRequest
import com.smartsales.prism.domain.scheduler.ActiveTaskContext
import com.smartsales.prism.domain.scheduler.ActiveTaskResolveResult
import com.smartsales.prism.domain.scheduler.ActiveTaskRetrievalIndex

class FakeActiveTaskRetrievalIndex : ActiveTaskRetrievalIndex {
    var nextShortlist: List<ActiveTaskContext> = emptyList()
    var nextResolveResult: ActiveTaskResolveResult? = null
    var lastShortlistTranscript: String? = null
    var lastPreferredTaskIds: Set<String> = emptySet()
    var lastResolveTarget: TargetResolutionRequest? = null
    var lastSuggestedTaskId: String? = null

    override suspend fun buildShortlist(
        transcript: String,
        preferredTaskIds: Set<String>,
        limit: Int
    ): List<ActiveTaskContext> {
        lastShortlistTranscript = transcript
        lastPreferredTaskIds = preferredTaskIds
        return nextShortlist.take(limit)
    }

    override suspend fun resolveTarget(
        target: TargetResolutionRequest,
        suggestedTaskId: String?
    ): ActiveTaskResolveResult {
        lastResolveTarget = target
        lastSuggestedTaskId = suggestedTaskId
        return nextResolveResult ?: ActiveTaskResolveResult.NoMatch(target.describeForFailure())
    }
}
