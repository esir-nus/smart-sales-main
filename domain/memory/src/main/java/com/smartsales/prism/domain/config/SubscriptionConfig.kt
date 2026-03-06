package com.smartsales.prism.domain.config

/**
 * 订阅层级枚举
 * @see Prism-V1.md §5.5 SaaS Monetization
 */
enum class SubscriptionTier {
    FREE, PRO, ENTERPRISE
}

/**
 * 订阅配置 — 热区窗口 (Lazy Compaction)
 * 
 * 统一写入，分层读取：不同订阅层级看到不同时间窗口的热区数据
 */
object SubscriptionConfig {
    /**
     * 获取热区窗口天数
     * - FREE: 7天
     * - PRO: 14天 
     * - ENTERPRISE: 30天
     */
    fun getHotWindowDays(tier: SubscriptionTier): Int = when (tier) {
        SubscriptionTier.FREE -> 7
        SubscriptionTier.PRO -> 14
        SubscriptionTier.ENTERPRISE -> 30
    }
}
