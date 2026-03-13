package com.smartsales.prism.domain.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * The absolute root of all LLM structured intent outputs for Project Mono.
 * The PromptCompiler dynamically uses `serializer<UnifiedMutation>().descriptor` 
 * to generate its JSON instructions.
 */
@Serializable
data class UnifiedMutation(
    // ----------------------------------------------------
    // Analyst Engine metadata & 4-Tier Intent Gateway
    // ----------------------------------------------------
    @SerialName("query_quality")
    val queryQuality: String = "schedulable",
    
    @SerialName("missing_entities")
    val missingEntities: List<String> = emptyList(),
    
    val thought: String? = null,
    val response: String? = null,

    // ----------------------------------------------------
    // Scheduler engine classification & attributes
    // ----------------------------------------------------
    val classification: String = "schedulable",
    val reason: String? = null,
    val targetTitle: String? = null,
    val newInstruction: String? = null,

    // ----------------------------------------------------
    // The core instruction payloads
    // ----------------------------------------------------
    @SerialName("profile_mutations")
    val profileMutations: List<ProfileMutation> = emptyList(),
    
    val tasks: List<TaskMutation> = emptyList(),
    
    // ----------------------------------------------------
    // Analyst Engine tool recommendations (Mono Contract)
    // ----------------------------------------------------
    @SerialName("recommended_workflows")
    val recommendedWorkflows: List<WorkflowRecommendation> = emptyList()
)

/**
 * Defines an explicitly structured tool dispatch payload for the Analyst engine.
 */
@Serializable
data class WorkflowRecommendation(
    val workflowId: String,
    val reason: String? = null,
    val parameters: Map<String, String> = emptyMap()
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
