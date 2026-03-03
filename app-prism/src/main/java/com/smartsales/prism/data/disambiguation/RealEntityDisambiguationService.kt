package com.smartsales.prism.data.disambiguation

import com.smartsales.prism.domain.disambiguation.DisambiguationResult
import com.smartsales.prism.domain.disambiguation.EntityDisambiguationService
import com.smartsales.prism.domain.memory.EntityWriter
import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.parser.InputParserService
import com.smartsales.prism.domain.parser.ParseResult
import com.smartsales.prism.domain.pipeline.EntityRef
import com.smartsales.prism.domain.model.CandidateOption
import com.smartsales.prism.domain.model.ClarificationType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealEntityDisambiguationService @Inject constructor(
    private val inputParserService: InputParserService,
    private val entityWriter: EntityWriter
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
            // Cure received! Write to the CRM.
            val upsertResult = entityWriter.upsertFromClue(
                clue = parseResult.name,
                resolvedId = null,
                type = EntityType.PERSON, // Defaults to Person for now when declared via chat
                source = "disambiguation"
            )
            val entityId = upsertResult.entityId

            // Update profile with additional info if provided
            if (!parseResult.jobTitle.isNullOrBlank() || !parseResult.company.isNullOrBlank() || !parseResult.notes.isNullOrBlank()) {
                // If company is provided, we might ideally want to upsert the account and link it.
                // For now, we store company as part of the profile or notes string to keep it simple,
                // or rely on the EntityWriter implementation to handle nested company linking later.
                val profileUpdates = mutableListOf<String>()
                parseResult.jobTitle?.let { profileUpdates.add("职位: $it") }
                parseResult.company?.let { profileUpdates.add("公司: $it") }
                parseResult.notes?.let { profileUpdates.add("备注: $it") }

                if (profileUpdates.isNotEmpty()) {
                    entityWriter.updateProfile(entityId, mapOf("notes" to profileUpdates.joinToString("\n")))
                }
            }

            // Register aliases
            if (parseResult.aliases.isNotEmpty()) {
                parseResult.aliases.forEach { alias ->
                    entityWriter.registerAlias(entityId, alias)
                }
            }
            // Clear the state and command resumption
            val result = DisambiguationResult.Resumed(currentIntent.originalInput, currentIntent.mode)
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
