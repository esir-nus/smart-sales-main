package com.smartsales.core.test.fakes

import com.smartsales.prism.domain.memory.TargetResolutionRequest
import com.smartsales.prism.domain.scheduler.ActiveTaskContext
import com.smartsales.prism.domain.scheduler.ActiveTaskResolveResult
import com.smartsales.prism.domain.scheduler.ActiveTaskRetrievalIndex

class FakeActiveTaskRetrievalIndex : ActiveTaskRetrievalIndex {
    var nextShortlist: List<ActiveTaskContext> = emptyList()
    var nextResolveResult: ActiveTaskResolveResult? = null
    var nextClockAnchorResolveResult: ActiveTaskResolveResult? = null
    var lastShortlistTranscript: String? = null
    var lastResolveTarget: TargetResolutionRequest? = null
    var lastSuggestedTaskId: String? = null
    var lastClockCue: String? = null
    var lastNowIso: String? = null
    var lastTimezone: String? = null
    var lastDisplayedDateIso: String? = null

    override suspend fun buildShortlist(
        transcript: String,
        limit: Int
    ): List<ActiveTaskContext> {
        lastShortlistTranscript = transcript
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

    override suspend fun resolveTargetByClockAnchor(
        clockCue: String,
        nowIso: String,
        timezone: String,
        displayedDateIso: String?
    ): ActiveTaskResolveResult {
        lastClockCue = clockCue
        lastNowIso = nowIso
        lastTimezone = timezone
        lastDisplayedDateIso = displayedDateIso
        return nextClockAnchorResolveResult ?: ActiveTaskResolveResult.NoMatch(clockCue)
    }
}
