package com.smartsales.prism.ui.sim

import android.util.Log
import com.smartsales.core.telemetry.PipelineValve

internal const val SIM_BADGE_SCHEDULER_CONTINUITY_INGRESS_ACCEPTED_SUMMARY =
    "SIM badge scheduler continuity ingress accepted"
internal const val SIM_CONNECTIVITY_ENTRY_OPENED_SUMMARY =
    "SIM connectivity entry opened"
internal const val SIM_CONNECTIVITY_MANAGER_DIRECT_ENTRY_OPENED_SUMMARY =
    "SIM connectivity manager direct entry opened"
internal const val SIM_CONNECTIVITY_SETUP_STARTED_SUMMARY =
    "SIM connectivity setup started"
internal const val SIM_CONNECTIVITY_SETUP_COMPLETED_SUMMARY =
    "SIM connectivity setup completed to manager"
internal const val SIM_AUDIO_PERSISTED_ARTIFACT_OPENED_SUMMARY =
    "SIM audio persisted artifact opened"
internal const val SIM_AUDIO_GROUNDED_CHAT_OPENED_FROM_ARTIFACT_SUMMARY =
    "SIM audio grounded chat opened from artifact"

private const val SIM_AUDIO_OFFLINE_LOG_TAG = "SimAudioOffline"
private const val SIM_AUDIO_CHAT_ROUTE_LOG_TAG = "SimAudioChatRoute"

internal fun emitSchedulerShelfHandoffTelemetry(
    promptText: String,
    log: (String) -> Unit = { message -> Log.d("SimSchedulerShelf", message) }
) {
    PipelineValve.tag(
        checkpoint = PipelineValve.Checkpoint.UI_STATE_EMITTED,
        payloadSize = promptText.length,
        summary = SIM_SCHEDULER_SHELF_HANDOFF_REQUEST_SUMMARY,
        rawDataDump = promptText
    )
    log("scheduler shelf handoff requested: $promptText")
}

internal fun emitBadgeSchedulerContinuityIngressTelemetry(
    promptText: String,
    log: (String) -> Unit = { message -> Log.d("SimBadgeFollowUp", message) }
) {
    PipelineValve.tag(
        checkpoint = PipelineValve.Checkpoint.UI_STATE_EMITTED,
        payloadSize = promptText.length,
        summary = SIM_BADGE_SCHEDULER_CONTINUITY_INGRESS_ACCEPTED_SUMMARY,
        rawDataDump = promptText
    )
    log("badge scheduler continuity ingress accepted: $promptText")
}

internal fun emitSimConnectivityRouteTelemetry(
    summary: String,
    detail: String,
    log: (String) -> Unit = { message -> Log.d("SimConnectivityRoute", message) }
) {
    PipelineValve.tag(
        checkpoint = PipelineValve.Checkpoint.UI_STATE_EMITTED,
        payloadSize = detail.length,
        summary = summary,
        rawDataDump = detail
    )
    log("$summary: $detail")
}

internal fun emitSimAudioPersistedArtifactOpenedTelemetry(
    audioId: String,
    title: String,
    log: (String, String) -> Unit = { tag, message -> Log.d(tag, message) }
) {
    val detail = "audioId=$audioId title=$title"
    PipelineValve.tag(
        checkpoint = PipelineValve.Checkpoint.UI_STATE_EMITTED,
        payloadSize = detail.length,
        summary = SIM_AUDIO_PERSISTED_ARTIFACT_OPENED_SUMMARY,
        rawDataDump = detail
    )
    log(SIM_AUDIO_OFFLINE_LOG_TAG, "$SIM_AUDIO_PERSISTED_ARTIFACT_OPENED_SUMMARY: $detail")
}

internal fun emitSimAudioGroundedChatOpenedFromArtifactTelemetry(
    audioId: String,
    title: String,
    log: (String, String) -> Unit = { tag, message -> Log.d(tag, message) }
) {
    val detail = "audioId=$audioId title=$title"
    PipelineValve.tag(
        checkpoint = PipelineValve.Checkpoint.UI_STATE_EMITTED,
        payloadSize = detail.length,
        summary = SIM_AUDIO_GROUNDED_CHAT_OPENED_FROM_ARTIFACT_SUMMARY,
        rawDataDump = detail
    )
    log(
        SIM_AUDIO_CHAT_ROUTE_LOG_TAG,
        "$SIM_AUDIO_GROUNDED_CHAT_OPENED_FROM_ARTIFACT_SUMMARY: $detail"
    )
}
