package com.smartsales.data.aicore.debug

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/debug/TingwuTraceStore.kt
// 模块：:data:ai-core
// 说明：调试用 Tingwu 调用痕迹存储，仅保存在内存，供 Home HUD 展示
// 作者：创建于 2025-12-12
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
                lastResultUrls = resultUrls ?: current.lastResultUrls,
                updatedAtMs = now
            )
        }
    }

    fun getSnapshot(): TingwuTraceSnapshot = snapshot
}
