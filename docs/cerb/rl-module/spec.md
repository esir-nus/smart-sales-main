# RL Module

> **Cerb-compliant spec** — Reinforcement learning from structured LLM output.

---

## Overview

RL Module learns user and client preferences from structured output in LLM responses. Only activates when `rl_observations` section exists (no wasted parsing).

---

## Related Cerb Specs

| Spec | Responsibility |
|------|----------------|
| [User Habit](../user-habit/spec.md) | Storage (entityId, key, confidence) |
| [Client Profile Hub](../client-profile-hub/spec.md) | Profile data (on-demand) |

---

## Learning Sources

| Source | entityId | Example |
|--------|----------|---------|
| User's own actions | `null` (global) | User schedules at 10am |
| User describes client | Contact/Account ID | "张总喜欢早上开会" |
| LLM infers from context | Contact/Account ID | 3 meetings all in morning |

---

## Structured Output Integration

LLM includes `rl_observations` when it detects learnable preferences:

```json
{
  "displayContent": "好的，已安排明天10点...",
  "schedulerJson": { ... },
  "rl_observations": [
    {
      "entityId": "c-001",
      "key": "preferred_meeting_time",
      "value": "morning",
      "source": "USER_INPUT",
      "evidence": "用户说张总只有早上有空"
    }
  ]
}
```

---

## Domain Models

### RlObservation

```kotlin
data class RlObservation(
    val entityId: String?,          // null = user global, else client ID
    val key: String,                // Habit key
    val value: String,              // Observed value
    val source: ObservationSource,  // USER_INPUT or INFERRED
    val evidence: String?           // Original text (debugging)
)

enum class ObservationSource {
    USER_INPUT,   // User explicitly said it
    INFERRED      // LLM inferred from context
}
```

### HabitContext

```kotlin
data class HabitContext(
    val userHabits: List<Habit>,      // Global preferences
    val clientHabits: List<Habit>,    // Per-entity preferences
    val suggestedDefaults: Map<String, String>
)
```

---

## Interface

```kotlin
interface ReinforcementLearner {
    // Called by Orchestrator after LLM response
    suspend fun processObservations(observations: List<RlObservation>)
    
    // Called by Context Builder (every LLM call)
    suspend fun getHabitContext(entityIds: List<String>?): HabitContext
}
```

---

## Learning Flow

```
LLM Response
    │
    ├── rl_observations present?
    │   ├── YES → RL Module processes
    │   └── NO  → Skip (no-op)
    │
    ▼
For each observation:
    ├── Matches existing habit → observationCount++
    ├── Conflicts → rejectionCount++
    └── New → Create with confidence = 0.5
    │
    ▼
Recalculate: confidence = obs / (obs + rej)
```

---

## Confidence Weighting

```kotlin
fun calculateConfidence(habit: UserHabit): Float {
    val explicitWeight = 2.0f  // USER_INPUT counts double
    val inferredWeight = 1.0f
    
    val weightedObs = habit.explicitCount * explicitWeight + 
                      habit.inferredCount * inferredWeight
    val weightedRej = habit.rejectionCount * inferredWeight
    
    return weightedObs / (weightedObs + weightedRej)
}
```

---

## Wave Plan

| Wave | Focus | Status |
|------|-------|--------|
| **1** | Interface + RlObservation Schema | 🔲 |
| **2** | Orchestrator Integration | 🔲 |
| **3** | Context Builder Integration | 🔲 |
| **4** | Time Decay (Fading) | 🔲 |
