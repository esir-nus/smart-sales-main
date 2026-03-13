package com.smartsales.core.test.fakes

import com.smartsales.prism.domain.memory.AliasCache
import com.smartsales.prism.domain.memory.CacheResult
import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityType

class FakeAliasCache : AliasCache {
    var nextResult: CacheResult = CacheResult.Miss
    val receivedCandidates = mutableListOf<List<String>>()

    override suspend fun match(candidates: List<String>): CacheResult {
        receivedCandidates.add(candidates)
        return nextResult
    }
}
