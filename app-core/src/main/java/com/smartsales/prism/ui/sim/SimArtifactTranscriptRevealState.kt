package com.smartsales.prism.ui.sim

/**
 * SIM/base-runtime shell-owned artifact transcript reveal state.
 *
 * This remains outside the shared UI contracts so reusable surfaces do not need
 * to depend on a concrete ViewModel type.
 */
data class SimArtifactTranscriptRevealState(
    val consumed: Boolean = false,
    val isLongTranscript: Boolean = false
)
