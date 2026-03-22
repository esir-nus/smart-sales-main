package com.smartsales.prism.ui.sim

import android.util.Log
import androidx.lifecycle.ViewModel
import com.smartsales.core.telemetry.PipelineValve
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal const val SIM_BADGE_FOLLOW_UP_OWNER_STARTED_SUMMARY =
    "SIM badge scheduler follow-up owner started"
internal const val SIM_BADGE_FOLLOW_UP_OWNER_SURFACE_UPDATED_SUMMARY =
    "SIM badge scheduler follow-up owner surface updated"
internal const val SIM_BADGE_FOLLOW_UP_OWNER_CLEARED_SUMMARY =
    "SIM badge scheduler follow-up owner cleared"

private const val SIM_BADGE_FOLLOW_UP_LOG_TAG = "SimBadgeFollowUp"

enum class SimBadgeFollowUpOrigin {
    BADGE
}

enum class SimBadgeFollowUpLane {
    SCHEDULER
}

enum class SimBadgeFollowUpSurface {
    CHAT,
    SCHEDULER,
    HISTORY,
    CONNECTIVITY,
    SETTINGS,
    SHELL
}

enum class SimBadgeFollowUpClearReason {
    NEW_SESSION,
    SESSION_SWITCHED,
    SESSION_DELETED
}

data class SimBadgeFollowUpState(
    val threadId: String,
    val origin: SimBadgeFollowUpOrigin,
    val lane: SimBadgeFollowUpLane,
    val boundSessionId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastActiveSurface: SimBadgeFollowUpSurface
)

/**
 * SIM 胸牌跟进绑定持有者。
 * 仅保存跨界面连续性元数据，不保存会话内容。
 */
class SimBadgeFollowUpOwner : ViewModel() {

    private val _activeFollowUp = MutableStateFlow<SimBadgeFollowUpState?>(null)
    val activeFollowUp: StateFlow<SimBadgeFollowUpState?> = _activeFollowUp.asStateFlow()

    fun startBadgeSchedulerFollowUp(
        boundSessionId: String,
        threadId: String = UUID.randomUUID().toString(),
        initialSurface: SimBadgeFollowUpSurface = SimBadgeFollowUpSurface.SHELL
    ) {
        if (boundSessionId.isBlank()) return
        val now = System.currentTimeMillis()
        val state = SimBadgeFollowUpState(
            threadId = threadId,
            origin = SimBadgeFollowUpOrigin.BADGE,
            lane = SimBadgeFollowUpLane.SCHEDULER,
            boundSessionId = boundSessionId,
            createdAt = now,
            updatedAt = now,
            lastActiveSurface = initialSurface
        )
        _activeFollowUp.value = state
        emitBadgeFollowUpTelemetry(
            summary = SIM_BADGE_FOLLOW_UP_OWNER_STARTED_SUMMARY,
            rawDataDump = "sessionId=$boundSessionId surface=$initialSurface"
        )
        Log.d(
            SIM_BADGE_FOLLOW_UP_LOG_TAG,
            "follow-up owner started: sessionId=$boundSessionId surface=$initialSurface"
        )
    }

    fun markSurface(surface: SimBadgeFollowUpSurface) {
        val current = _activeFollowUp.value ?: return
        if (current.lastActiveSurface == surface) return

        val updated = current.copy(
            updatedAt = System.currentTimeMillis(),
            lastActiveSurface = surface
        )
        _activeFollowUp.value = updated
        emitBadgeFollowUpTelemetry(
            summary = SIM_BADGE_FOLLOW_UP_OWNER_SURFACE_UPDATED_SUMMARY,
            rawDataDump = "threadId=${current.threadId} surface=$surface"
        )
        Log.d(
            SIM_BADGE_FOLLOW_UP_LOG_TAG,
            "follow-up owner surface updated: threadId=${current.threadId} surface=$surface"
        )
    }

    fun clear(reason: SimBadgeFollowUpClearReason) {
        val current = _activeFollowUp.value ?: return
        _activeFollowUp.value = null
        emitBadgeFollowUpTelemetry(
            summary = SIM_BADGE_FOLLOW_UP_OWNER_CLEARED_SUMMARY,
            rawDataDump = "threadId=${current.threadId} reason=$reason"
        )
        Log.d(
            SIM_BADGE_FOLLOW_UP_LOG_TAG,
            "follow-up owner cleared: threadId=${current.threadId} reason=$reason"
        )
    }

    private fun emitBadgeFollowUpTelemetry(summary: String, rawDataDump: String) {
        PipelineValve.tag(
            checkpoint = PipelineValve.Checkpoint.UI_STATE_EMITTED,
            payloadSize = rawDataDump.length,
            summary = summary,
            rawDataDump = rawDataDump
        )
    }
}
