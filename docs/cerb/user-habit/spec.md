# User Habit

> **Cerb-compliant spec** — Behavioral pattern learning.

---

## Overview

Tracks and learns user behavioral patterns for personalization. Used by Context Builder to provide nudges and defaults.

---

## Domain Model

```kotlin
data class UserHabit(
    val habitKey: String,          // e.g., "preferred_meeting_time"
    val habitValue: String,        // e.g., "morning"
    val entityId: String?,         // For per-client habits (null = global)
    val isExplicit: Boolean,       // true = user-set, false = inferred
    val confidence: Float,         // 0.0-1.0
    val observationCount: Int,
    val rejectionCount: Int,
    val lastObservedAt: Long,
    val createdAt: Long
)
```

---

## Field Descriptions

| Field | Purpose |
|-------|---------|
| `habitKey` | Behavior category (meeting_time, duration, location) |
| `habitValue` | The preference value |
| `confidence` | Strength: obs / (obs + rej) |
| `isExplicit` | User explicitly set vs system inferred |
| `entityId` | Habit specific to one client vs global (null) |

---

## Habit Categories

| Category | Key | Example Values |
|----------|-----|----------------|
| **Meeting Time** | `preferred_meeting_time` | morning, afternoon, evening |
| **Duration** | `default_duration` | 30, 45, 60 (minutes) |
| **Location** | `preferred_location` | office, client_site, remote |
| **Follow-up** | `follow_up_interval` | 3, 7, 14 (days) |

---

## Learning Flow (Future)

```
User Action → Observe Pattern
       │
       ▼
Update Habit:
├─ Matches existing → observationCount++
├─ Conflicts with existing → rejectionCount++
└─ New pattern → Create habit (confidence = 0.5)
       │
       ▼
Re-calculate confidence: obs / (obs + rej)
```

---

## Wave Plan

| Wave | Focus | Status |
|------|-------|--------|
| **1** | Schema + Repository | ✅ SHIPPED |
| **2** | Observation Hook | 🔲 |
| **3** | Nudge Integration | 🔲 |
| **4** | Time Decay (Fading) | 🔲 |

---

## Usage (Future)

Context Builder queries habits before LLM call:

```kotlin
val habits = habitRepository.getGlobalHabits()
val clientHabits = habitRepository.getByEntity(clientId)
// Inject into LLM prompt for personalization
```
