package com.smartsales.core.pipeline

import com.smartsales.core.context.EnhancedContext
import kotlinx.coroutines.CoroutineScope

/**
 * Background listener for the Reinforcement Learning subsystem.
 * Extracts rl_observations from the interaction context without blocking the main pipeline.
 *
 * OS Model: RAM Application (Background Daemon)
 */
interface HabitListener {
    /**
     * Spawns a background LLM assessment of the user's input against the context.
     * Fires `ReinforcementLearner.processObservations()` autonomously.
     *
     * @param rawInput The user's original speech/text
     * @param context The assembled workspace (S1/S2/S3)
     * @param coroutineScope An independent architectural scope that survives pipeline cancellation
     */
    fun analyzeAsync(rawInput: String, context: EnhancedContext, coroutineScope: CoroutineScope)
}
