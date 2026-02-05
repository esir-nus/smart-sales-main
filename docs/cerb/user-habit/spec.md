# User Habit

> **Cerb-compliant spec** — Behavioral pattern storage.

---

## Overview

Stores learned user behavioral patterns for personalization. Learning logic is in [RL Module](../rl-module/spec.md); this spec covers storage only.

---

## Domain Model

```kotlin
data class UserHabit(
    val habitKey: String,           // e.g., "preferred_meeting_time"
    val habitValue: String,         // e.g., "morning"
    val entityId: String?,          // For per-client habits (null = global)
    
    // 4-Rule Weighting Signals
    val inferredCount: Int = 0,     // Rule 1: Frequency (LLM inferred)
    val explicitPositive: Int = 0,  // Rule 2: User confirmed
    val explicitNegative: Int = 0,  // Rule 3: User rejected
    
    // Rule 4: Recency
    val lastObservedAt: Long,       // Epoch millis
    val createdAt: Long             // Epoch millis
)
```

---

## Field Descriptions

| Field | Purpose |
|-------|---------|
| `habitKey` | Behavior category (meeting_time, duration, location) |
| `habitValue` | The preference value |
| `inferredCount` | LLM observed this pattern N times |
| `explicitPositive` | User explicitly confirmed N times (3x weight) |
| `explicitNegative` | User explicitly rejected N times (-2x weight) |
| `lastObservedAt` | For time decay calculation |
| `entityId` | Habit specific to one client vs global (null) |

---

## Confidence Calculation

Confidence is calculated at **query time** by RL Module (not stored):

```kotlin
fun calculateConfidence(habit: UserHabit, now: Instant): Float {
    val daysSince = ChronoUnit.DAYS.between(
        Instant.ofEpochMilli(habit.lastObservedAt), now
    )
    val decayFactor = 1.0f / (1 + daysSince / 30f)  // Half-life ~30 days
    
    val rawScore = (habit.inferredCount * 1.0f) +
                   (habit.explicitPositive * 3.0f) +
                   (habit.explicitNegative * -2.0f)
    
    val maxPossible = (habit.inferredCount + habit.explicitPositive) * 3.0f
    val normalized = if (maxPossible > 0) rawScore / maxPossible else 0.5f
    
    return (normalized * decayFactor).coerceIn(0f, 1f)
}
```

---

## Habit Categories

| Category | Key | Example Values |
|----------|-----|----------------|
| **Meeting Time** | `preferred_meeting_time` | morning, afternoon, evening |
| **Duration** | `default_duration` | 30, 45, 60 (minutes) |
| **Location** | `preferred_location` | office, client_site, remote |
| **Follow-up** | `follow_up_interval` | 3, 7, 14 (days) |

---

## Repository Interface

```kotlin
interface UserHabitRepository {
    suspend fun getGlobalHabits(): List<UserHabit>
    suspend fun getByEntity(entityId: String): List<UserHabit>
    
    // Called by RL Module
    suspend fun observe(key: String, value: String, entityId: String?, source: ObservationSource)
}
```

---

## Wave Plan

| Wave | Focus | Status |
|------|-------|--------|
| **1** | Schema + Repository | ✅ SHIPPED |
| **1.5** | Update schema for 4-rule model | 🔲 |
| **2+** | Moved to [RL Module](../rl-module/spec.md) | — |
