package com.smartsales.core.pipeline

import com.smartsales.core.context.ContextBuilder
import com.smartsales.core.context.ContextDepth
import com.smartsales.prism.domain.model.Mode
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * IntentOrchestrator (Phase 0 Gateway)
 * 
 * Sits at Layer 3 above the UnifiedPipeline and below the Presentation layer.
 * Evaluates intents using LightningRouter to short-circuit noise and greetings to the MascotService,
 * protecting the heavy UnifiedPipeline from processing conversational filler.
 */
class IntentOrchestrator @Inject constructor(
    private val contextBuilder: ContextBuilder,
    private val lightningRouter: LightningRouter,
    private val mascotService: MascotService,
    private val unifiedPipeline: UnifiedPipeline
) {
    suspend fun processInput(input: String): Flow<PipelineResult> {
        return flow {
        // Build minimal context for latency-sensitive phase 0 evaluation
        val context = contextBuilder.build(input, Mode.ANALYST, depth = ContextDepth.MINIMAL)
        val routerResult = lightningRouter.evaluateIntent(context)

        when (routerResult?.queryQuality) {
            QueryQuality.NOISE, QueryQuality.GREETING -> {
                // Short-circuit to System I (Mascot)
                mascotService.interact(MascotInteraction.Text(input))
                // No PipelineResult emitted, as Mascot handles it ambiently
            }
            QueryQuality.VAGUE -> {
                // Route back to user for clarification or immediate answer
                emit(PipelineResult.ConversationalReply(routerResult.response))
            }
            else -> {
                // System II Task (Deep Analysis, Simple QA, or Direct Task)
                val pipelineInput = PipelineInput(
                    rawText = input,
                    isVoice = false,
                    intent = routerResult?.queryQuality ?: QueryQuality.DEEP_ANALYSIS
                )
                
                // Delegate to the heavy-duty pipeline and forward its results
                unifiedPipeline.processInput(pipelineInput).collect { result ->
                    emit(result)
                }
            }
        }
    }
}
}
