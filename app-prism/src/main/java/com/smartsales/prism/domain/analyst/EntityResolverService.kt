package com.smartsales.prism.domain.analyst

import com.smartsales.prism.domain.memory.EntityEntry

/**
 * Wave 5: Entity Resolver Service
 * Provides lightweight fuzzy matching for entity names against known CRM candidates.
 */
interface EntityResolverService {
    /**
     * Resolves a query name against a list of candidate entities.
     * @param query The missing or ambiguous entity name.
     * @param candidates The list of known entities from the CRM.
     * @return The best matching EntityEntry, or null if no confident match is found.
     */
    suspend fun resolve(query: String, candidates: List<EntityEntry>): EntityEntry?
}
