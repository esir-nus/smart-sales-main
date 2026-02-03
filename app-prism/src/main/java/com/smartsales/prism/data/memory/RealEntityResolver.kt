package com.smartsales.prism.data.memory

import com.smartsales.prism.domain.memory.EntityResolver
import com.smartsales.prism.domain.memory.RelevancyRepository
import com.smartsales.prism.domain.memory.ResolutionResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 实体解析器实现 — 基于 RelevancyRepository 查询
 * 
 * Wave 2: 简单计数逻辑
 * Wave 4: 将添加评分算法 (confirmCount + recencyDecay + contextMatch)
 */
@Singleton
class RealEntityResolver @Inject constructor(
    private val relevancyRepository: RelevancyRepository
) : EntityResolver {
    
    override suspend fun resolve(alias: String): ResolutionResult {
        val candidates = relevancyRepository.findByAlias(alias)
        
        return when {
            candidates.isEmpty() -> ResolutionResult.NotFound
            candidates.size == 1 -> ResolutionResult.AutoResolved(candidates.first())
            else -> ResolutionResult.AmbiguousMatches(candidates)
        }
    }
}
