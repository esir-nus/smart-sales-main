# Analyst Architect Interface

> **Owner**: analyst-architect
> **Consumers**: analyst-orchestrator

## Public Interface

```kotlin
interface ArchitectService {
    /**
     * Phase 2: Generates a structured investigation plan.
     * 
     * @param input User's proven analytical intent
     * @param context RAM snapshot (EnhancedContext) loaded by Kernel
     * @param sessionHistory Previous conversation turns
     * @return Structured plan mapping to the UI PlannerTable
     */
    suspend fun generatePlan(
        input: String,
        context: EnhancedContext,
        sessionHistory: List<ChatTurn> = emptyList()
    ): PlanResult

    /**
     * Phase 3: Executes the investigation based on the plan and context.
     * 
     * @param plan The user-confirmed PlanResult from Phase 2
     * @param context RAM snapshot (EnhancedContext) loaded by Kernel
     * @param sessionHistory Previous conversation turns
     * @return Final analysis text and any suggested actionable workflows
     */
    suspend fun investigate(
        plan: PlanResult,
        context: EnhancedContext,
        sessionHistory: List<ChatTurn> = emptyList()
    ): InvestigationResult
}
```

## Data Models

```kotlin
data class PlanResult(
    val title: String,
    val summary: String,
    val steps: List<AnalysisStep> // Used by PlannerTable
)

data class InvestigationResult(
    val analysisContent: String,
    val suggestedWorkflows: List<WorkflowSuggestion> // Mounts to TaskBoard
)

data class AnalysisStep(
    val stepId: String,
    val description: String,
    val status: StepStatus
)

enum class StepStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

data class WorkflowSuggestion(
    val workflowId: String,
    val label: String
)
```

## You Should NOT

| Don't | Do Instead |
|-------|------------|
| Fetch data from SSD/Repositories | Only read from the provided `EnhancedContext` (RAM) |
| Execute workflow tools directly | Return `workflowId` strings in `suggestedWorkflows` for UI to mount |
| Skip Phase 2 (Planning) | Orchestrator guarantees human confirmation before calling `investigate` |
| Make up missing data | Flag missing data cleanly in the analysis content |
