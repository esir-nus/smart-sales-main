package com.smartsales.prism.domain.scheduler

/**
 * 旧版 lint() 兼容载体。
 */
data class ParsedClues(
    val person: String? = null,
    val company: String? = null,
    val location: String? = null,
    val briefSummary: String? = null,
    val durationMinutes: Int? = null
)

data class ParsedProfileMutation(
    val entityId: String,
    val field: String,
    val value: String
)

sealed class LintResult {
    data class Success(
        val task: ScheduledTask? = null,
        val urgencyLevel: UrgencyLevel = UrgencyLevel.L3_NORMAL,
        val parsedClues: ParsedClues = ParsedClues(),
        val profileMutations: List<ParsedProfileMutation> = emptyList()
    ) : LintResult()

    data class MultiTask(
        val tasks: List<ScheduledTask>,
        val profileMutations: List<ParsedProfileMutation> = emptyList()
    ) : LintResult()

    data class Incomplete(
        val missingField: String,
        val question: String,
        val partialClues: ParsedClues
    ) : LintResult()

    data class Error(val message: String) : LintResult()

    data class NonIntent(val reason: String) : LintResult()

    data class Deletion(val targetTitle: String) : LintResult()

    data class Reschedule(val targetTitle: String, val newInstruction: String) : LintResult()

    data class ToolDispatch(val workflowId: String, val params: Map<String, String>) : LintResult()
}
