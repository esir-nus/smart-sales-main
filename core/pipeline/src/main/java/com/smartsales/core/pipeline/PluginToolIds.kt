package com.smartsales.core.pipeline

object PluginToolIds {
    const val ARTIFACT_GENERATE = "artifact.generate"
    const val AUDIO_ANALYZE = "audio.analyze"
    const val CRM_SHEET_GENERATE = "crm.sheet.generate"
    const val SIMULATION_TALK = "simulation.talk"

    const val EXPORT_CSV = "EXPORT_CSV"
    const val DRAFT_EMAIL = "DRAFT_EMAIL"

    private val aliases = mapOf(
        "GENERATE_PDF" to ARTIFACT_GENERATE,
        "TALK_SIMULATOR" to SIMULATION_TALK
    )

    private val directDispatchLanes = setOf(
        ARTIFACT_GENERATE,
        AUDIO_ANALYZE,
        CRM_SHEET_GENERATE,
        SIMULATION_TALK
    )

    fun canonicalize(toolId: String): String = aliases[toolId] ?: toolId

    fun isDirectDispatchLane(toolId: String): Boolean = canonicalize(toolId) in directDispatchLanes
}
