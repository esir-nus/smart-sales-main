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
     * @param input User's chat message intent
     * @param context RAM snapshot (EnhancedContext) loaded by Kernel
     * @param sessionHistory Previous conversation turns
     * @return Result indicating if information is sufficient, plus optional clarification message
     */
    suspend fun evaluateIntent(
        input: String,
        context: EnhancedContext,
        sessionHistory: List<ChatTurn> = emptyList()
    ): ConsultantResult
}
```

## Data Models

```kotlin
data class ConsultantResult(
    val infoSufficient: Boolean,
    val clarificationMessage: String?
)
```

## You Should NOT

| Don't | Do Instead |
|-------|------------|
| Parse complex JSON arrays | Use simple `.optBoolean("info_sufficient", false)` |
| Execute investigation logic | Defer to Phase 3 via orchestrator |
| Call ContextBuilder directly | Orchestrator passes `EnhancedContext` as input |
| Persist state | Remain stateless, return `ConsultantResult` |
