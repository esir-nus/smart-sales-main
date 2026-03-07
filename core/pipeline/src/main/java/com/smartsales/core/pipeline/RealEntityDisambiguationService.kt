package com.smartsales.core.pipeline

import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.model.UiState
import com.smartsales.core.pipeline.*
import com.smartsales.core.pipeline.*
import com.smartsales.prism.domain.memory.EntityRef
import com.smartsales.prism.domain.model.CandidateOption
import com.smartsales.prism.domain.model.ClarificationType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealEntityDisambiguationService @Inject constructor(
    private val inputParserService: InputParserService
) : EntityDisambiguationService {

    data class PendingIntent(
        val originalInput: String,
        val mode: Mode
    )

    private var pendingIntent: PendingIntent? = null

    override suspend fun process(rawInput: String): DisambiguationResult {
        val currentIntent = pendingIntent ?: return DisambiguationResult.PassThrough

        // We are in a disambiguation state. Route the input to the parser to see if it's a cure.
        val parseResult = inputParserService.parseIntent(rawInput)

        if (parseResult is ParseResult.EntityDeclaration) {
            // Cure received! Pipeline should handle the write-back.
            val result = DisambiguationResult.Resolved(parseResult, currentIntent.originalInput, currentIntent.mode)
            pendingIntent = null
            return result
        } else if (parseResult is ParseResult.NeedsClarification) {
             // Still ambiguous
             return DisambiguationResult.Intercepted(
                 UiState.AwaitingClarification(
                     question = parseResult.clarificationPrompt,
                     clarificationType = ClarificationType.AMBIGUOUS_PERSON,
                     candidates = parseResult.suggestedMatches.map { ref ->
                         CandidateOption(
                             entityId = ref.entityId,
                             displayName = ref.displayName,
                             description = ref.entityType
                         )
                     }
                 )
             )
        }

        // If it's a Success but not a declaration, the user ignored our clarification request
        // and just sent a completely new topic. In standard voice-assistant UX,
        // this implicitly cancels the pending intent and continues with the new one.
        pendingIntent = null
        return DisambiguationResult.PassThrough
    }

    override fun startDisambiguation(
        originalInput: String,
        originalMode: Mode,
        ambiguousName: String,
        candidates: List<EntityRef>
    ): UiState {
        pendingIntent = PendingIntent(originalInput, originalMode)
        val prompt = "系统发现 '$ambiguousName' 似乎不在通讯录中，您是想提及新客户还是拼写有误？"
        return UiState.AwaitingClarification(
            question = prompt,
            clarificationType = ClarificationType.AMBIGUOUS_PERSON,
            candidates = candidates.map {
                CandidateOption(
                    entityId = it.entityId,
                    displayName = it.displayName,
                    description = it.entityType
                )
            }
        )
    }

    override fun cancel() {
        pendingIntent = null
    }
}
