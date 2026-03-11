# RL Module — Interface

> **Consumer contract** for habit context and observation processing.  
> **OS Layer**: RAM Application

---

## Methods

### loadUserHabits & loadClientHabits

```kotlin
suspend fun loadUserHabits(): HabitContext
suspend fun loadClientHabits(entityIds: List<String>): HabitContext
```

Returns habit context for Kernel RAM population. Called by Kernel when caching SessionWorkingSet.

**OS Model Note**: Reads global habits and entity-specific habits from `UserHabitRepository` (SSD) to populate `SessionWorkingSet` Sections 2 and 3.

| Output | Type | Description |
|--------|------|-------------|
| `HabitContext` | `UserHabit` list | Specific habits loaded from SSD |

---

### processObservations

```kotlin
suspend fun processObservations(observations: List<RlObservation>)
```

Processes RL observations from structured LLM output. Called by Orchestrator when `rl_observations` section exists.

**OS Model Note**: Implements **Write-Through**:
1. Updates `SessionWorkingSet` (RAM) immediately.
2. Persists to `UserHabitRepository` (SSD) asynchronously.

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
| Access `UserHabit` table directly | READ the RAM (Section 2/3) or WRITE via `processObservations` |
| Pass `entityIds` arbitrarily | Let the Kernel handle `loadClientHabits` with active Entities |
| Parse `rl_observations` JSON manually | Orchestrator provides parsed list |
| Call `processObservations` when section is empty | Check for null/empty first |
| Modify habit confidence manually | Let RL Module manage via observations |
