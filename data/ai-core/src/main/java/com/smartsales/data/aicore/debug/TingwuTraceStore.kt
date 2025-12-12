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
                lastResultUrls = resultUrls ?: current.lastResultUrls,
                updatedAtMs = now
            )
        }
    }

    fun getSnapshot(): TingwuTraceSnapshot = snapshot
}
