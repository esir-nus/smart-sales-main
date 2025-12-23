package com.smartsales.core.metahub

// 文件：core/util/src/main/java/com/smartsales/core/metahub/InMemoryMetaHub.kt
// 模块：:core:util
// 说明：内存版元数据中心实现，线程安全的 Map 存储
// 作者：创建于 2025-12-04

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 内存存储实现，仅用于原型和测试。
 * 不落盘，也不携带原始内容。
 */
class InMemoryMetaHub : MetaHub {
    private val sessionMutex = Mutex()
    private val sessionStore = mutableMapOf<String, SessionMetadata>()

    private val transcriptMutex = Mutex()
    private val transcriptStoreById = mutableMapOf<String, TranscriptMetadata>()
    private val transcriptStoreBySession = mutableMapOf<String, TranscriptMetadata>()

    private val exportMutex = Mutex()
    private val exportStore = mutableMapOf<String, ExportMetadata>()

    private val usageMutex = Mutex()
    private val usageRecords = mutableListOf<TokenUsage>()

    override suspend fun upsertSession(metadata: SessionMetadata) {
        sessionMutex.withLock {
            val existing = sessionStore[metadata.sessionId]
            val merged = existing?.mergeWith(metadata) ?: metadata
            sessionStore[metadata.sessionId] = merged
        }
    }

    override suspend fun getSession(sessionId: String): SessionMetadata? =
        sessionMutex.withLock { sessionStore[sessionId] }

    override suspend fun appendM2Patch(sessionId: String, patch: M2PatchRecord) {
        sessionMutex.withLock {
            val existing = sessionStore[sessionId] ?: SessionMetadata(sessionId = sessionId)
            val history = existing.m2PatchHistory + patch
            val effective = applyM2PatchHistory(history)
            sessionStore[sessionId] = existing.copy(
                effectiveM2 = effective,
                m2PatchHistory = history
            )
        }
    }

    override suspend fun getEffectiveM2(sessionId: String): ConversationDerivedState? =
        sessionMutex.withLock {
            val existing = sessionStore[sessionId] ?: return@withLock null
            if (existing.m2PatchHistory.isEmpty()) return@withLock null
            existing.effectiveM2 ?: applyM2PatchHistory(existing.m2PatchHistory)
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

    /**
     * 仅用于测试场景，方便读取已记录用量。
     */
    internal suspend fun dumpUsage(): List<TokenUsage> =
        usageMutex.withLock { usageRecords.toList() }
}
