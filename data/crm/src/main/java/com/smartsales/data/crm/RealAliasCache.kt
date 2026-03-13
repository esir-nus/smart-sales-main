package com.smartsales.data.crm

import android.util.Log
import com.smartsales.prism.domain.memory.AliasCache
import com.smartsales.prism.domain.memory.CacheResult
import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class RealAliasCache @Inject constructor(
    private val entityRepository: EntityRepository,
    @Named("AppScope") private val applicationScope: CoroutineScope
) : AliasCache {

    private val TAG = "RealAliasCache"
    
    // Lowcase Alias -> List of Entity IDs that share this alias
    private val index = ConcurrentHashMap<String, MutableList<String>>()
    private val fullEntityMap = ConcurrentHashMap<String, EntityEntry>()
    
    @Volatile
    private var isHydrated = false
    private val mutex = Mutex()

    init {
        // Pre-warm the cache immediately upon instantiation to ensure L1 speed
        applicationScope.launch(Dispatchers.IO) {
            hydrateLocked()
        }
    }

    override suspend fun match(candidates: List<String>): CacheResult {
        if (candidates.isEmpty()) return CacheResult.Miss
        
        // Safety net: if not hydrated yet, do it blocking just once
        if (!isHydrated) {
            hydrateLocked()
        }

        val matchingEntityIds = mutableSetOf<String>()
        
        for (candidate in candidates) {
            val key = candidate.trim().lowercase()
            val ids = index[key]
            if (ids != null) {
                matchingEntityIds.addAll(ids)
            }
        }

        return when {
            matchingEntityIds.isEmpty() -> {
                Log.d(TAG, "match: Cache Miss for candidates $candidates")
                CacheResult.Miss
            }
            matchingEntityIds.size == 1 -> {
                val entityId = matchingEntityIds.first()
                Log.d(TAG, "match: ExactMatch found for candidates $candidates -> $entityId")
                CacheResult.ExactMatch(entityId)
            }
            else -> {
                val entries = matchingEntityIds.mapNotNull { fullEntityMap[it] }
                Log.d(TAG, "match: Ambiguous! Candidates $candidates mapped to ${matchingEntityIds.size} entities")
                CacheResult.Ambiguous(entries)
            }
        }
    }

    private suspend fun hydrateLocked() {
        if (isHydrated) return
        
        mutex.withLock {
            if (isHydrated) return@withLock
            
            try {
            Log.d(TAG, "hydrateLocked: Pre-warming AliasCache from EntityRepository...")
            val allEntities = entityRepository.getAll(limit = 1000)
            
            index.clear()
            fullEntityMap.clear()
            
            allEntities.forEach { entity ->
                fullEntityMap[entity.entityId] = entity
                
                // 1. Index the canonical display name
                addAlias(entity.displayName, entity.entityId)
                
                // 2. Index the JSON array of aliases
                try {
                    val elements = Json.parseToJsonElement(entity.aliasesJson).jsonArray
                    elements.forEach { element ->
                        addAlias(element.jsonPrimitive.content, entity.entityId)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse aliasesJson for ${entity.entityId}: ${entity.aliasesJson}")
                }
            }
            
            isHydrated = true
            Log.d(TAG, "hydrateLocked: Pre-warming complete. Loaded ${allEntities.size} entities into L1 Cache.")
        } catch (e: Exception) {
            Log.e(TAG, "hydrateLocked: Failed to pre-warm L1 Cache", e)
        }
        }
    }
    
    private fun addAlias(alias: String, entityId: String) {
        val key = alias.trim().lowercase()
        if (key.isNotEmpty()) {
            index.getOrPut(key) { mutableListOf() }.add(entityId)
        }
    }
}
