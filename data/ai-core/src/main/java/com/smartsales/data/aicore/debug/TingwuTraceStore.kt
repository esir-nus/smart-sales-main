package com.smartsales.data.aicore.debug

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/debug/TingwuTraceStore.kt
// 模块：:data:ai-core
// 说明：调试用 Tingwu 调用痕迹存储，仅保存在内存，供 Home HUD 展示
// 作者：创建于 2025-12-12
import com.smartsales.core.metahub.BatchPlanItem
import com.smartsales.core.metahub.SuspiciousBoundary
import javax.inject.Inject
import javax.inject.Singleton

data class TingwuTraceSnapshot(
    val lastTaskId: String? = null,
    val lastCreateTaskRequestJson: String? = null,
    val lastCreateTaskResponseJson: String? = null,
    val lastGetTaskInfoResponseJson: String? = null,
    // 重要：Tingwu 原始转写仅保留落盘路径，不在内存/HUD 内保存原文。
    val transcriptionDumpPath: String? = null,
    val transcriptionDumpBytes: Long? = null,
    val transcriptionDumpSavedAtMs: Long? = null,
    val transcriptDumpPath: String? = null,
    val transcriptDumpBytes: Long? = null,
    val transcriptDumpSavedAtMs: Long? = null,
    // 重要：伪流式批次仅存“规则/进度”，避免输出大段内容。
    val batchPlanRule: String? = null,
    val batchPlanBatchSize: Int? = null,
    val batchPlanTotalBatches: Int? = null,
    val batchPlanCurrentBatchIndex: Int? = null,
    // 说明：V1 窗口计划摘要（10min/10s），用于 HUD 展示，优先于旧行批次。
    val v1BatchPlanRule: String? = null,
    val v1BatchDurationMs: Long? = null,
    val v1OverlapMs: Long? = null,
    val v1BatchPlanTotalBatches: Int? = null,
    val v1BatchPlanCurrentBatchIndex: Int? = null,
    val batchPlan: List<BatchPlanItem> = emptyList(),
    val suspiciousBoundaries: List<SuspiciousBoundary> = emptyList(),
    val lastResultUrls: Map<String, String> = emptyMap(),
    val updatedAtMs: Long = 0L
)

@Singleton
class TingwuTraceStore @Inject constructor() {
    @Volatile
    private var snapshot: TingwuTraceSnapshot = TingwuTraceSnapshot()

    fun record(
        taskId: String? = null,
        createRequestJson: String? = null,
        createResponseJson: String? = null,
        getTaskInfoJson: String? = null,
        transcriptionDumpPath: String? = null,
        transcriptionDumpBytes: Long? = null,
        transcriptionDumpSavedAtMs: Long? = null,
        transcriptDumpPath: String? = null,
        transcriptDumpBytes: Long? = null,
        transcriptDumpSavedAtMs: Long? = null,
        batchPlanRule: String? = null,
        batchPlanBatchSize: Int? = null,
        batchPlanTotalBatches: Int? = null,
        batchPlanCurrentBatchIndex: Int? = null,
        v1BatchPlanRule: String? = null,
        v1BatchDurationMs: Long? = null,
        v1OverlapMs: Long? = null,
        v1BatchPlanTotalBatches: Int? = null,
        v1BatchPlanCurrentBatchIndex: Int? = null,
        batchPlan: List<BatchPlanItem>? = null,
        suspiciousBoundaries: List<SuspiciousBoundary>? = null,
        resultUrls: Map<String, String>? = null
    ) {
        val now = System.currentTimeMillis()
        synchronized(this) {
            val current = snapshot
            snapshot = current.copy(
                lastTaskId = taskId ?: current.lastTaskId,
                lastCreateTaskRequestJson = createRequestJson ?: current.lastCreateTaskRequestJson,
                lastCreateTaskResponseJson = createResponseJson ?: current.lastCreateTaskResponseJson,
                lastGetTaskInfoResponseJson = getTaskInfoJson ?: current.lastGetTaskInfoResponseJson,
                transcriptionDumpPath = transcriptionDumpPath ?: current.transcriptionDumpPath,
                transcriptionDumpBytes = transcriptionDumpBytes ?: current.transcriptionDumpBytes,
                transcriptionDumpSavedAtMs = transcriptionDumpSavedAtMs ?: current.transcriptionDumpSavedAtMs,
                transcriptDumpPath = transcriptDumpPath ?: current.transcriptDumpPath,
                transcriptDumpBytes = transcriptDumpBytes ?: current.transcriptDumpBytes,
                transcriptDumpSavedAtMs = transcriptDumpSavedAtMs ?: current.transcriptDumpSavedAtMs,
                batchPlanRule = batchPlanRule ?: current.batchPlanRule,
                batchPlanBatchSize = batchPlanBatchSize ?: current.batchPlanBatchSize,
                batchPlanTotalBatches = batchPlanTotalBatches ?: current.batchPlanTotalBatches,
                batchPlanCurrentBatchIndex = batchPlanCurrentBatchIndex ?: current.batchPlanCurrentBatchIndex,
                v1BatchPlanRule = v1BatchPlanRule ?: current.v1BatchPlanRule,
                v1BatchDurationMs = v1BatchDurationMs ?: current.v1BatchDurationMs,
                v1OverlapMs = v1OverlapMs ?: current.v1OverlapMs,
                v1BatchPlanTotalBatches = v1BatchPlanTotalBatches ?: current.v1BatchPlanTotalBatches,
                v1BatchPlanCurrentBatchIndex = v1BatchPlanCurrentBatchIndex
                    ?: current.v1BatchPlanCurrentBatchIndex,
                batchPlan = batchPlan?.let { normalizeBatchPlan(it) } ?: current.batchPlan,
                suspiciousBoundaries = suspiciousBoundaries?.let { normalizeSuspiciousBoundaries(it) }
                    ?: current.suspiciousBoundaries,
                lastResultUrls = resultUrls ?: current.lastResultUrls,
                updatedAtMs = now
            )
        }
    }

    fun getSnapshot(): TingwuTraceSnapshot = snapshot

    private fun normalizeBatchPlan(input: List<BatchPlanItem>): List<BatchPlanItem> {
        if (input.isEmpty()) return emptyList()
        val isSorted = input.map { it.batchId }
            .zipWithNext()
            .all { (prev, next) -> prev <= next }
        // 说明：优先保留原顺序，不稳定时按 batchId 排序。
        return if (isSorted) input else input.sortedBy { it.batchId }
    }

    private fun normalizeSuspiciousBoundaries(input: List<SuspiciousBoundary>): List<SuspiciousBoundary> {
        if (input.isEmpty()) return emptyList()
        // 说明：按 index 升序确保顺序确定。
        return input.sortedBy { it.index }
    }
}
