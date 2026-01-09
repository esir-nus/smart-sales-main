package com.smartsales.feature.chat.home.components

import com.smartsales.feature.chat.home.HomeUiState

/**
 * Defines the visual states for the ChromaWave and the rules for transitioning between them.
 *
 * Design Intent:
 * - **Strict Minimalism**: The wave should ONLY appear when the AI is actively "Alive" (Listening or Thinking).
 * - **Hidden in Idle**: When waiting for user input (Idle), the wave should recede completely to avoid visual noise.
 * - **Hidden on Hero**: The Welcome Hero screen has its own branding; the wave should not compete.
 */
enum class ChromaWaveVisualState {
    Hidden,
    Listening, // Microphone active
    Thinking,  // AI Processing active
    Error      // Error state
}

object ChromaWaveRules {
    /**
     * strict mapping of App State -> Visual State.
     *
     * Rules:
     * 1. If Hero Screen is visible -> Hidden.
     * 2. If Listening (Microphone on) -> Listening.
     * 3. If Thinking (Streaming/Sending) -> Thinking.
     * 4. If Error -> Error.
     * 5. Else (Idle/Init) -> Hidden.
     */
    fun mapToVisualState(
        showWelcomeHero: Boolean,
        waveState: MotionState // The raw MotionState from ViewModel (which might be Idle)
    ): ChromaWaveVisualState {
        if (showWelcomeHero) return ChromaWaveVisualState.Hidden

        return when (waveState) {
            MotionState.Listening -> ChromaWaveVisualState.Listening
            MotionState.Thinking -> ChromaWaveVisualState.Thinking
            MotionState.Error -> ChromaWaveVisualState.Error
            MotionState.Idle -> ChromaWaveVisualState.Hidden // Explicitly Hide in Idle
            MotionState.Hidden -> ChromaWaveVisualState.Hidden
        }
    }
}

/**
 * The raw motion state driven by the ViewModel.
 * This is mapped to [ChromaWaveVisualState] by [ChromaWaveRules].
 */
enum class MotionState {
    Hidden,
    Idle,      // Gentle harmonic wave
    Listening, // High amplitude, fast
    Thinking,  // Medium amplitude, lateral flow
    Error      // Jitter
}
