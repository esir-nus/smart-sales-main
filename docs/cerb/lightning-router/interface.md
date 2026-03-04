# Lightning Router Interface

> **Owner**: lightning-router
> **Consumers**: analyst-orchestrator

## Public Interface

```kotlin
interface LightningRouter {
    /**
     * Phase 0 of PrismOrchestrator: Evaluates user intent against MINIMAL RAM context.
     * Determines whether the input is NOISE, GREETING, SIMPLE_QA, or a TASK/ANALYSIS.
     *
     * @param context MINIMAL RAM snapshot loaded by Kernel
     * @return Result indicating the intent classification and sufficiency for Phase 2.
     */
    suspend fun evaluateIntent(
        context: EnhancedContext
    ): RouterResult?
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
