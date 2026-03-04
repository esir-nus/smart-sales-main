# Analyst Consultant Interface

> **Owner**: analyst-consultant
> **Consumers**: analyst-orchestrator

## Public Interface

```kotlin
interface ConsultantService {
    /**
     * Phase 1 of Analyst Mode: Evaluates user intent against RAM context.
     * Determines whether there is enough information to build an investigation plan,
     * or if clarification from the user is needed.
     *
     * @param context RAM snapshot (EnhancedContext) loaded by Kernel (contains input and history)
     * @return Result indicating if information is sufficient. Returns `null` if the intent is completely unclear and requires immediate user clarification before any evaluation can proceed.
     */
    suspend fun evaluateIntent(
        context: EnhancedContext
    ): ConsultantResult?
}
```

## Data Models

```kotlin
enum class QueryQuality {
    NOISE, SIMPLE_QA, DEEP_ANALYSIS, CRM_TASK
}

data class ConsultantResult(
    val queryQuality: QueryQuality,
    val infoSufficient: Boolean,
    val response: String,
    val missingEntities: List<String> = emptyList()
)
```

## You Should NOT

| Don't | Do Instead |
|-------|------------|
| Parse complex JSON arrays | Use simple `.optBoolean("info_sufficient", false)` |
| Execute investigation logic | Defer to Phase 3 via orchestrator |
| Call ContextBuilder directly | Orchestrator passes `EnhancedContext` as input |
| Persist state | Remain stateless, return `ConsultantResult?` |
