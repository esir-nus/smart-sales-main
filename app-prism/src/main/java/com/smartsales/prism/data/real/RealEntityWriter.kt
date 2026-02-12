package com.smartsales.prism.data.real

import android.util.Log
import com.smartsales.prism.domain.crm.ActivityType
import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityRepository
import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.memory.EntityWriter
import com.smartsales.prism.domain.memory.ProfileChange
import com.smartsales.prism.domain.memory.ProfileUpdateResult
import com.smartsales.prism.domain.memory.UpsertResult
import com.smartsales.prism.domain.pipeline.EntityRef
import com.smartsales.prism.domain.time.TimeProvider
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import com.smartsales.prism.domain.pipeline.KernelWriteBack
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 实体写入器实现 — read-modify-write 模式
 *
 * @see docs/cerb/entity-writer/spec.md
 */
@Singleton
class RealEntityWriter @Inject constructor(
    private val entityRepository: EntityRepository,
    private val timeProvider: TimeProvider,
    private val kernelWriteBack: KernelWriteBack // Breaks concrete coupling to RealContextBuilder
) : EntityWriter {

    companion object {
        private const val TAG = "EntityWriter"
        private const val MAX_ALIASES = 8

        // updateProfile 跟踪的字段 → ActivityType 映射
        private val TRACKED_FIELDS = mapOf(
            "displayName" to ActivityType.NAME_CHANGE,
            "jobTitle" to ActivityType.TITLE_CHANGE,
            "accountId" to ActivityType.COMPANY_CHANGE
        )
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

            // Write-through → RAM Section 1
            writeThrough(merged)

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

            // Write-through → RAM Section 1
            writeThrough(newEntry)

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

        // Write-through → RAM Section 1
        writeThrough(updated)

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

        // Write-through → RAM Section 1
        writeThrough(updated)

        Log.d(TAG, "🏷️ 别名注册: id=$entityId alias=$alias (total=${newAliases.size})")
    }

    override suspend fun updateProfile(
        entityId: String,
        updates: Map<String, String?>
    ): ProfileUpdateResult {
        val existing = entityRepository.getById(entityId)
            ?: throw IllegalArgumentException("实体不存在: $entityId")

        val changes = mutableListOf<ProfileChange>()
        var merged = existing

        // 逐字段检测变更
        for ((field, newValue) in updates) {
            if (newValue == null) continue

            val oldValue = when (field) {
                "displayName" -> existing.displayName
                "jobTitle" -> {
                    val attrs = tryParseJson(existing.attributesJson)
                    attrs.optString("jobTitle", "")
                }
                "accountId" -> {
                    val attrs = tryParseJson(existing.attributesJson)
                    attrs.optString("accountId", "")
                }
                else -> {
                    Log.w(TAG, "⚠️ updateProfile: 未知跟踪字段 $field")
                    continue
                }
            }

            // 值未变 → 跳过
            if (oldValue == newValue) continue

            changes.add(ProfileChange(field, oldValue.ifEmpty { null }, newValue))

            // 应用变更
            merged = when (field) {
                "displayName" -> {
                    // Latest-write-wins: 旧名称 → aliases
                    val aliases = parseAliases(merged.aliasesJson)
                    val updatedAliases = if (merged.displayName.isNotBlank() &&
                        !aliases.any { it.equals(merged.displayName, ignoreCase = true) }
                    ) {
                        (aliases + merged.displayName).let { list ->
                            if (list.size > MAX_ALIASES) list.drop(list.size - MAX_ALIASES) else list
                        }
                    } else aliases

                    merged.copy(
                        displayName = newValue,
                        aliasesJson = JSONArray(updatedAliases).toString()
                    )
                }
                "jobTitle", "accountId" -> {
                    val attrs = tryParseJson(merged.attributesJson)
                    attrs.put(field, newValue)
                    merged.copy(attributesJson = attrs.toString())
                }
                else -> merged
            }
        }

        if (changes.isNotEmpty()) {
            val finalEntry = merged.copy(lastUpdatedAt = timeProvider.now.toEpochMilli())
            entityRepository.save(finalEntry)
            writeThrough(finalEntry)

            // Kernel 回调: 记录历史事件
            for (change in changes) {
                val activityType = TRACKED_FIELDS[change.field] ?: continue
                kernelWriteBack.recordActivity(
                    entityId = entityId,
                    type = activityType,
                    summary = "${change.oldValue ?: "（空）"} → ${change.newValue}"
                )
            }

            Log.d(TAG, "👤 Profile更新: id=$entityId changes=${changes.size}")
        }

        return ProfileUpdateResult(entityId = entityId, changes = changes)
    }

    override suspend fun delete(entityId: String) {
        entityRepository.delete(entityId)
        // Write-through → 从 RAM Section 1 移除
        kernelWriteBack.removeEntityFromSession(entityId)
        Log.d(TAG, "🗑️ 删除实体: id=$entityId")
    }

    // --- Private helpers ---

    /**
     * Write-through: SSD 写入后同步 RAM Section 1
     */
    private suspend fun writeThrough(entry: EntityEntry) {
        kernelWriteBack.updateEntityInSession(
            entry.entityId,
            EntityRef(entry.entityId, entry.displayName, entry.entityType.name)
        )
    }

    /**
     * 解析实体: resolvedId → 别名匹配 → displayName 匹配 → null (新建)
     * Resolution Cascade — see entity-writer/spec.md §1
     */
    private suspend fun resolveEntity(clue: String, resolvedId: String?): EntityEntry? {
        // Step 1: resolvedId 优先
        if (resolvedId != null) {
            val entry = entityRepository.getById(resolvedId)
            if (entry != null) return entry
            // 过期 ID → 回退到别名搜索
            Log.w(TAG, "⚠️ 过期 resolvedId=$resolvedId, 回退别名搜索")
        }

        // Step 2: 别名精确匹配 (curated aliases)
        val aliasMatches = entityRepository.findByAlias(clue)
        if (aliasMatches.isNotEmpty()) return aliasMatches.first()

        // Step 3: displayName 精确匹配 (同音字消歧)
        val nameMatches = entityRepository.findByDisplayName(clue)
        if (nameMatches.isNotEmpty()) {
            if (nameMatches.size > 1) {
                Log.w(TAG, "⚠️ 多重 displayName 匹配: clue=$clue, count=${nameMatches.size}")
            }
            Log.d(TAG, "🔗 displayName 消歧: clue=$clue → id=${nameMatches.first().entityId}")
            return nameMatches.first()  // sorted by lastUpdatedAt DESC
        }

        return null
    }

    /**
     * 合并字段 — 仅更新来源追踪元数据
     *
     * displayName: Immutable (canonical) — upsertFromClue 不覆盖
     * aliasesJson: Immutable (curated) — 仅 registerAlias 可修改
     * attributesJson: Upsert source tracking keys only
     */
    @Suppress("UNUSED_PARAMETER") // clue 保留参数签名一致性，不再用于修改 displayName/aliasesJson
    private fun mergeFields(
        existing: EntityEntry,
        clue: String,
        source: String,
        now: Long
    ): EntityEntry {
        // 仅更新来源追踪元数据，不修改 displayName 和 aliasesJson
        val attrs = tryParseJson(existing.attributesJson)
        attrs.put("_source", source)
        attrs.put("_last_seen", now)
        if (!attrs.has("_first_seen")) {
            attrs.put("_first_seen", now)
        }

        return existing.copy(
            attributesJson = attrs.toString(),
            lastUpdatedAt = now
            // displayName: 保持 canonical name 不变
            // aliasesJson: 保持 curated aliases 不变
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
