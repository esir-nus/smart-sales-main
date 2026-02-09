package com.smartsales.prism.domain.memory

/**
 * 实体写入器 — 集中式实体变更路径
 *
 * 所有实体写入必须通过此接口，禁止直接调用 EntityRepository.save()。
 * 内部封装：去重、别名注册、字段更新策略、来源追踪。
 *
 * @see docs/cerb/entity-writer/interface.md
 */
interface EntityWriter {
    /**
     * 从原始线索创建或更新实体
     *
     * - resolvedId 有值 → 更新已有实体
     * - resolvedId 为 null → 按别名去重，不存在则创建
     * - 内部执行 read-modify-write，保留已有字段
     *
     * @param clue 原始提及文本（如"张总"），不能为空白
     * @param resolvedId 消歧后的实体 ID（null → 自动解析）
     * @param type 实体类型
     * @param source 调用方标识（"scheduler", "coach", "audio"）
     * @return UpsertResult 包含 entityId、isNew 标记、规范 displayName
     * @throws IllegalArgumentException clue 为空白时
     */
    suspend fun upsertFromClue(
        clue: String,
        resolvedId: String?,
        type: EntityType,
        source: String
    ): UpsertResult

    /**
     * 更新实体的单个属性
     * 对 attributesJson 执行 upsert-per-key 策略
     * 以 '_' 开头的 key 为内部保留字段，外部禁用
     */
    suspend fun updateAttribute(entityId: String, key: String, value: String)

    /**
     * 为已有实体注册新别名
     * 去重 + 上限 8 个（FIFO 淘汰最旧）
     */
    suspend fun registerAlias(entityId: String, alias: String)

    /**
     * 删除实体，不存在时静默忽略
     */
    suspend fun delete(entityId: String)
}

/**
 * Upsert 操作结果
 */
data class UpsertResult(
    val entityId: String,
    val isNew: Boolean,
    val displayName: String  // 规范名称（用于回写调用方）
)
