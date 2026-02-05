# RL Module — Interface

> **Consumer contract** for habit context and observation processing.

---

## Methods

### getHabitContext

```kotlin
suspend fun getHabitContext(entityIds: List<String>?): HabitContext
```

Returns habit context for LLM prompt enrichment. Called by Context Builder on every LLM call.

| Input | Type | Description |
|-------|------|-------------|
| `entityIds` | `List<String>?` | Optional entity IDs for client-specific habits |

| Output | Type |
|--------|------|
| `HabitContext` | User + client habits + suggested defaults |

---

### processObservations

```kotlin
suspend fun processObservations(observations: List<RlObservation>)
```

Processes RL observations from structured LLM output. Called by Orchestrator when `rl_observations` section exists.

| Input | Type | Description |
|-------|------|-------------|
| `observations` | `List<RlObservation>` | Parsed from LLM structured output |

---

## Output Types

```kotlin
data class HabitContext(
    val userHabits: List<UserHabit>,
    val clientHabits: List<UserHabit>,
    val suggestedDefaults: Map<String, String>
)

data class RlObservation(
    val entityId: String?,
    val key: String,
    val value: String,
    val source: ObservationSource,
    val evidence: String?
)

enum class ObservationSource {
    INFERRED,       // LLM inferred from context (weight: 1x)
    USER_POSITIVE,  // User explicitly confirmed (weight: 3x)
    USER_NEGATIVE   // User explicitly rejected (weight: -2x)
}
```

---

## Confidence Model

Confidence is calculated at **query time** using 4 rules:

| Rule | Signal | Weight |
|------|--------|--------|
| 1. Frequency | `inferredCount` | 1x |
| 2. Explicit Positive | `explicitPositive` | 3x |
| 3. Explicit Negative | `explicitNegative` | -2x |
| 4. Time Decay | Days since `lastObservedAt` | Half-life ~30 days |

**Deletion**: Habits with confidence < 0.1 are deleted on next query.

---

## You Should NOT

| ❌ Don't | ✅ Do Instead |
|----------|--------------|
| Access `UserHabit` table directly | Use `ReinforcementLearner` methods |
| Parse `rl_observations` JSON manually | Orchestrator provides parsed list |
| Call `processObservations` when section is empty | Check for null/empty first |
| Modify habit confidence manually | Let RL Module manage via observations |
| Calculate confidence yourself | RL Module calculates with decay at query time |
