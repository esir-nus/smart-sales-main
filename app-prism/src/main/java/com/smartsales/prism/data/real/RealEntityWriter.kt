package com.smartsales.prism.data.real

import android.util.Log
import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityRepository
import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.memory.EntityWriter
import com.smartsales.prism.domain.memory.UpsertResult
import com.smartsales.prism.domain.time.TimeProvider
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 实体写入器实现 — read-modify-write 模式
 *
 * 重要: EntityRepository.save() 底层是 @Insert(onConflict=REPLACE)，
 * 会覆盖整行。所以每次写入前必须先 getById() 读取现有数据，合并后再 save()。
 *
 * @see docs/cerb/entity-writer/spec.md
 */
@Singleton
class RealEntityWriter @Inject constructor(
    private val entityRepository: EntityRepository,
    private val timeProvider: TimeProvider
) : EntityWriter {

    companion object {
        private const val TAG = "EntityWriter"
        private const val MAX_ALIASES = 8
    }

    override suspend fun upsertFromClue(
        clue: String,
        resolvedId: String?,
        type: EntityType,
        source: String
    ): UpsertResult {
        require(clue.isNotBlank()) { "clue 不能为空白" }
        
        val now = timeProvider.now.toEpochMilli()
        
        // 1. 解析实体 ID
        val existingEntry = resolveEntity(clue, resolvedId)
        
        return if (existingEntry != null) {
            // 已有实体 → read-modify-write
            val merged = mergeFields(existingEntry, clue, source, now)
            entityRepository.save(merged)
            Log.d(TAG, "📝 更新实体: id=${merged.entityId} name=${merged.displayName} source=$source")
            
            UpsertResult(
                entityId = merged.entityId,
                isNew = false,
                displayName = merged.displayName
            )
        } else {
            // 新实体 → 创建
            val entityId = generateEntityId(type)
            val aliasesJson = JSONArray().put(clue).toString()
            val sourceJson = buildSourceJson(source, now)
            
            val newEntry = EntityEntry(
                entityId = entityId,
                entityType = type,
                displayName = clue,
                aliasesJson = aliasesJson,
                attributesJson = sourceJson,
                lastUpdatedAt = now,
                createdAt = now
            )
            entityRepository.save(newEntry)
            Log.d(TAG, "✨ 创建实体: id=$entityId name=$clue type=$type source=$source")
            
            UpsertResult(
                entityId = entityId,
                isNew = true,
                displayName = clue
            )
        }
    }

    override suspend fun updateAttribute(entityId: String, key: String, value: String) {
        require(!key.startsWith("_")) { "以 '_' 开头的 key 为内部保留字段" }
        
        val existing = entityRepository.getById(entityId) ?: run {
            Log.w(TAG, "⚠️ updateAttribute: 实体不存在 id=$entityId")
            return
        }
        
        // Upsert-per-key: 合并到现有 attributesJson
        val attrs = tryParseJson(existing.attributesJson)
        attrs.put(key, value)
        
        val updated = existing.copy(
            attributesJson = attrs.toString(),
            lastUpdatedAt = timeProvider.now.toEpochMilli()
        )
        entityRepository.save(updated)
        Log.d(TAG, "📝 属性更新: id=$entityId key=$key")
    }

    override suspend fun registerAlias(entityId: String, alias: String) {
        val existing = entityRepository.getById(entityId) ?: run {
            Log.w(TAG, "⚠️ registerAlias: 实体不存在 id=$entityId")
            return
        }
        
        val aliases = parseAliases(existing.aliasesJson)
        
        // 去重: 已存在则跳过
        if (aliases.any { it.equals(alias, ignoreCase = true) }) {
            return
        }
        
        // FIFO 淘汰: 超过上限时移除最旧的
        val newAliases = (aliases + alias).let { list ->
            if (list.size > MAX_ALIASES) list.drop(list.size - MAX_ALIASES) else list
        }
        
        val updated = existing.copy(
            aliasesJson = JSONArray(newAliases).toString(),
            lastUpdatedAt = timeProvider.now.toEpochMilli()
        )
        entityRepository.save(updated)
        Log.d(TAG, "🏷️ 别名注册: id=$entityId alias=$alias (total=${newAliases.size})")
    }

    override suspend fun delete(entityId: String) {
        entityRepository.delete(entityId)
        Log.d(TAG, "🗑️ 删除实体: id=$entityId")
    }

    // --- Private helpers ---

    /**
     * 解析实体: resolvedId 优先 → 别名搜索 → null (新建)
     */
    private suspend fun resolveEntity(clue: String, resolvedId: String?): EntityEntry? {
        // 优先使用 resolvedId
        if (resolvedId != null) {
            val entry = entityRepository.getById(resolvedId)
            if (entry != null) return entry
            // 过期 ID → 回退到别名去重
            Log.w(TAG, "⚠️ 过期 resolvedId=$resolvedId, 回退别名搜索")
        }
        
        // 别名去重: 取第一个匹配
        val matches = entityRepository.findByAlias(clue)
        return matches.firstOrNull()
    }

    /**
     * 合并字段 — 执行所有字段更新策略
     */
    private fun mergeFields(
        existing: EntityEntry,
        clue: String,
        source: String,
        now: Long
    ): EntityEntry {
        // displayName: first-write-wins（保留现有）
        // aliasesJson: append unique, cap 8
        val aliases = parseAliases(existing.aliasesJson)
        val newAliases = if (aliases.any { it.equals(clue, ignoreCase = true) }) {
            aliases
        } else {
            (aliases + clue).let { list ->
                if (list.size > MAX_ALIASES) list.drop(list.size - MAX_ALIASES) else list
            }
        }
        
        // attributesJson: upsert source tracking keys
        val attrs = tryParseJson(existing.attributesJson)
        attrs.put("_source", source)
        attrs.put("_last_seen", now)
        if (!attrs.has("_first_seen")) {
            attrs.put("_first_seen", now)
        }
        
        return existing.copy(
            aliasesJson = JSONArray(newAliases).toString(),
            attributesJson = attrs.toString(),
            lastUpdatedAt = now
        )
    }

    private fun generateEntityId(type: EntityType): String {
        val prefix = when (type) {
            EntityType.PERSON -> "p"
            EntityType.PRODUCT -> "pd"
            EntityType.LOCATION -> "l"
            EntityType.EVENT -> "e"
            EntityType.ACCOUNT -> "a"
            EntityType.CONTACT -> "c"
            EntityType.DEAL -> "d"
        }
        return "$prefix-${UUID.randomUUID().toString().take(8)}"
    }

    private fun buildSourceJson(source: String, now: Long): String {
        val json = JSONObject()
        json.put("_source", source)
        json.put("_first_seen", now)
        json.put("_last_seen", now)
        return json.toString()
    }

    private fun parseAliases(aliasesJson: String): List<String> {
        return try {
            val arr = JSONArray(aliasesJson)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun tryParseJson(json: String): JSONObject {
        return try {
            JSONObject(json)
        } catch (e: Exception) {
            JSONObject()
        }
    }
}
