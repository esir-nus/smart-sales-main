package com.smartsales.data.aicore.metahub

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/metahub/FileBackedMetaHub.kt
// 模块：:data:ai-core
// 说明：基于文件的 MetaHub 实现，保证会话元数据持久化
// 作者：创建于 2025-12-23

import com.google.gson.Gson
import com.smartsales.core.metahub.ConversationDerivedState
import com.smartsales.core.metahub.ExportMetadata
import com.smartsales.core.metahub.M2PatchRecord
import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.metahub.TokenUsage
import com.smartsales.core.metahub.TranscriptMetadata
import com.smartsales.core.metahub.applyM2PatchHistory
import java.io.File
import java.io.IOException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 文件落盘版 MetaHub：
 * - core/util 不依赖 Android，这里通过 rootDir 注入文件目录
 * - 会话元数据写入磁盘，其他元数据保持内存行为
 */
class FileBackedMetaHub(
    private val rootDir: File,
    private val gson: Gson
) : MetaHub {

    private val sessionCache = mutableMapOf<String, SessionMetadata>()
    private val lockRegistryMutex = Mutex()
    private val sessionLocks = mutableMapOf<String, Mutex>()

    private val transcriptMutex = Mutex()
    private val transcriptStoreById = mutableMapOf<String, TranscriptMetadata>()
    private val transcriptStoreBySession = mutableMapOf<String, TranscriptMetadata>()

    private val exportMutex = Mutex()
    private val exportStore = mutableMapOf<String, ExportMetadata>()

    private val usageMutex = Mutex()
    private val usageRecords = mutableListOf<TokenUsage>()

    override suspend fun upsertSession(metadata: SessionMetadata) {
        val sessionId = metadata.sessionId
        val sessionLock = lockFor(sessionId)
        sessionLock.withLock {
            val existing = sessionCache[sessionId] ?: loadSessionLocked(sessionId)
            val merged = existing?.mergeWith(metadata) ?: metadata
            sessionCache[sessionId] = merged
            runCatching { persistSessionLocked(merged) }
        }
    }

    override suspend fun getSession(sessionId: String): SessionMetadata? {
        val sessionLock = lockFor(sessionId)
        return sessionLock.withLock {
            sessionCache[sessionId] ?: loadSessionLocked(sessionId)?.also {
                sessionCache[sessionId] = it
            }
        }
    }

    override suspend fun appendM2Patch(sessionId: String, patch: M2PatchRecord) {
        val sessionLock = lockFor(sessionId)
        sessionLock.withLock {
            val existing = sessionCache[sessionId]
                ?: loadSessionLocked(sessionId)
                ?: SessionMetadata(sessionId = sessionId)
            val history = existing.m2PatchHistory + patch
            val effective = applyM2PatchHistory(history)
            val updated = existing.copy(
                effectiveM2 = effective,
                m2PatchHistory = history
            )
            sessionCache[sessionId] = updated
            // 重要：补丁写入失败不能静默吞掉，否则会造成补丁丢失与持久化不一致。
            persistSessionLocked(updated)
        }
    }

    override suspend fun getEffectiveM2(sessionId: String): ConversationDerivedState? {
        val sessionLock = lockFor(sessionId)
        return sessionLock.withLock {
            val existing = sessionCache[sessionId] ?: loadSessionLocked(sessionId)?.also {
                sessionCache[sessionId] = it
            }
            val history = existing?.m2PatchHistory ?: return@withLock null
            if (history.isEmpty()) return@withLock null
            existing.effectiveM2 ?: applyM2PatchHistory(history)
        }
    }

    override suspend fun upsertTranscript(metadata: TranscriptMetadata) {
        transcriptMutex.withLock {
            val existing = transcriptStoreById[metadata.transcriptId]
            val merged = existing?.mergeWith(metadata) ?: metadata
            transcriptStoreById[metadata.transcriptId] = merged
            merged.sessionId?.let { sessionId ->
                val existingBySession = transcriptStoreBySession[sessionId]
                val mergedSession = existingBySession?.mergeWith(merged) ?: merged
                transcriptStoreBySession[sessionId] = mergedSession
            }
        }
    }

    override suspend fun getTranscriptBySession(sessionId: String): TranscriptMetadata? =
        transcriptMutex.withLock { transcriptStoreBySession[sessionId] }

    override suspend fun upsertExport(metadata: ExportMetadata) {
        exportMutex.withLock {
            exportStore[metadata.sessionId] = metadata
        }
    }

    override suspend fun getExport(sessionId: String): ExportMetadata? =
        exportMutex.withLock { exportStore[sessionId] }

    override suspend fun logUsage(usage: TokenUsage) {
        usageMutex.withLock {
            usageRecords.add(usage)
        }
    }

    private suspend fun lockFor(sessionId: String): Mutex =
        lockRegistryMutex.withLock { sessionLocks.getOrPut(sessionId) { Mutex() } }

    private fun loadSessionLocked(sessionId: String): SessionMetadata? {
        val file = sessionFile(sessionId)
        if (!file.exists()) return null
        val raw = runCatching { file.readText(Charsets.UTF_8) }.getOrNull() ?: return null
        return runCatching { gson.fromJson(raw, SessionMetadata::class.java) }.getOrNull()
    }

    private fun persistSessionLocked(metadata: SessionMetadata) {
        ensureRootDir()
        val target = sessionFile(metadata.sessionId)
        val temp = File(target.parentFile, "${target.name}.tmp")
        val payload = gson.toJson(metadata)
        temp.writeText(payload, Charsets.UTF_8)
        // 原子写：先写临时文件再替换，防止写入中断导致文件损坏
        if (!temp.renameTo(target)) {
            if (target.exists() && !target.delete()) {
                throw IOException("无法替换旧的 MetaHub 文件：${target.name}")
            }
            if (!temp.renameTo(target)) {
                throw IOException("MetaHub 文件写入失败：${target.name}")
            }
        }
    }

    private fun sessionFile(sessionId: String): File {
        val safeId = sessionId.trim().ifBlank { "session" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(rootDir, "session_$safeId.json")
    }

    private fun ensureRootDir() {
        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }
    }
}
