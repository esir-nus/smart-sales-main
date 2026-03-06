package com.smartsales.prism.domain.session

/**
 * 实体在会话中的生命周期状态
 *
 * UNKNOWN   → 文本中检测到但未解析
 * MENTIONED → 已解析到 ID，但数据未加载
 * ACTIVE    → 数据已加载到 EnhancedContext，整个会话期间有效
 *
 * 无 STALE 状态 — 缓存在会话删除时销毁，不设超时。
 *
 * 注意：UNKNOWN 状态暂未被任何生产者写入，预留供未来 NER 集成。
 */
enum class EntityState {
    UNKNOWN,
    MENTIONED,
    ACTIVE
}
