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
    val userHabits: List<Habit>,
    val clientHabits: List<Habit>,
    val suggestedDefaults: Map<String, String>
)

data class RlObservation(
    val entityId: String?,
    val key: String,
    val value: String,
    val source: ObservationSource,
    val evidence: String?
)
```

---

## You Should NOT

| ❌ Don't | ✅ Do Instead |
|----------|--------------|
| Access `UserHabit` table directly | Use `ReinforcementLearner` methods |
| Parse `rl_observations` JSON manually | Orchestrator provides parsed list |
| Call `processObservations` when section is empty | Check for null/empty first |
| Modify habit confidence manually | Let RL Module manage via observations |
