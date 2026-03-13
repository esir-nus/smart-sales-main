# Core Contracts Interface

> **Owner**: `:domain:core`
> **Consumers**: `:core:pipeline`, `:domain:crm`, `:domain:scheduler`

## Public Interface

This module provides the strictly typed, `@Serializable` `UnifiedMutation` data class. This class is the definitive **"One Currency"** contract between the LLM engine (`Executor`) and the application's Domain Storage (`SSD`). 

It contains no executable services or business logic. Its schema is dynamically converted to JSON instructions by the `PromptCompiler`, and it is strictly evaluated (`decodeFromString()`) by the Pipeline Linters. If the LLM output violates this exact schema, a `SerializationException` is safely caught, blocking any downstream data corruption.

## Data Models

```kotlin
package com.smartsales.prism.domain.scheduler

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * The absolute root of all LLM structured intent outputs for Project Mono.
 * The PromptCompiler will dynamically use `serializer<UnifiedMutation>().descriptor` 
 * to generate its JSON instructions.
 */
@Serializable
data class UnifiedMutation(
    /**
     * Primary intent classification. Evaluated before processing further arrays.
     * Allowed values: "schedulable", "deletion", "reschedule", "non_intent"
     */
    val classification: String = "schedulable",

    /** 
     * Populated if classification is "non_intent". 
     * Used to power the Agent Intelligence OS waiting states.
     */
    val reason: String? = null,

    /** 
     * Target task title for "deletion" or "reschedule" classification.
     */
    val targetTitle: String? = null,

    /** 
     * Detailed instruction for "reschedule" classification.
     */
    val newInstruction: String? = null,

    /**
     * CRM Fact Extraction target. Handed directly to the EntityWriter S1 flow.
     */
    @SerialName("profile_mutations")
    val profileMutations: List<ProfileMutation> = emptyList(),

    /**
     * Schedulable items. Handed to the ScheduleBoard and ConflictResolver.
     */
    val tasks: List<TaskMutation> = emptyList()
)

/**
 * Defines a specific CRM state mutation extracted from the user's implicit or explicit speech.
 */
@Serializable
data class ProfileMutation(
    val entityId: String,
    val field: String,
    val value: String
)

/**
 * Defines a scheduled event or fire-off reminder.
 * Date strings must be parseable by DateTimeFormatter ("yyyy-MM-dd HH:mm").
 */
@Serializable
data class TaskMutation(
    val title: String,
    val startTime: String,                     
    val endTime: String? = null,
    val duration: String? = null,              // e.g., "30m", "1h"
    val urgency: String = "L3",                // "L1", "L2", "L3", "FIRE_OFF"
    val location: String? = null,
    val notes: String? = null,
    val keyPerson: String? = null,
    val keyCompany: String? = null,
    val highlights: String? = null             // For meeting prep
)
```
