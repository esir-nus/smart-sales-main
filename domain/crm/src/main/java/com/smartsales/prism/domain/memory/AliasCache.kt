package com.smartsales.prism.domain.memory

/**
 * L1 Cache for Entity Candidates (Sync Loop Gatekeeper)
 * 
 * Provides an instantaneous RAM lookup for entity string candidates, bypassing
 * the heavy SQL SSD layer. This is used by the Lightning Router to fast-fail
 * ambiguous candidates before launching the heavy UnifiedPipeline.
 * 
 * @see docs/cerb/lightning-router/spec.md
 */
interface AliasCache {
    /**
     * Checks if the given candidates perfectly match exactly one entity alias.
     */
    suspend fun match(candidates: List<String>): CacheResult
}

sealed class CacheResult {
    data class ExactMatch(val entityId: String) : CacheResult()
    data class Ambiguous(val candidates: List<EntityEntry>) : CacheResult()
    object Miss : CacheResult()
}
