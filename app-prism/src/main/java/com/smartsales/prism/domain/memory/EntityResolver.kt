package com.smartsales.prism.domain.memory

/**
 * 实体解析器 — 别名消歧 (e.g., "张总" → 哪个张总?)
 * 
 * @see docs/cerb/memory-center/spec.md §3 Relevancy & Disambiguation
 */
interface EntityResolver {
    /**
     * 解析别名为具体实体
     * 
     * @param alias 用户输入的别名 (e.g., "张总", "王经理")
     * @return 解析结果: 自动解析 / 多候选 / 未找到
     */
    suspend fun resolve(alias: String): ResolutionResult
}

/**
 * 解析结果密封类
 */
sealed class ResolutionResult {
    /**
     * 自动解析 — 唯一匹配，无需用户确认
     */
    data class AutoResolved(val entry: RelevancyEntry) : ResolutionResult()
    
    /**
     * 多候选 — 需要用户从列表中选择
     */
    data class AmbiguousMatches(val candidates: List<RelevancyEntry>) : ResolutionResult()
    
    /**
     * 未找到 — 别名无匹配实体
     */
    data object NotFound : ResolutionResult()
}
