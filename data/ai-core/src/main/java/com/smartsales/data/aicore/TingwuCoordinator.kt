package com.smartsales.data.aicore

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 文件路径: data/ai-core/src/main/java/com/smartsales/data/aicore/TingwuCoordinator.kt
// 文件作用: 暴露Tingwu任务协调接口并提供假实现
// 文件作者: Codex
// 最近修改: 2025-02-14
data class TingwuRequest(
    val audioAssetName: String,
    val language: String = "zh-CN",
    val ossObjectKey: String? = null,
    val fileUrl: String? = null
)

data class TingwuJobArtifacts(
    val outputMp3Path: String? = null,
    val outputMp4Path: String? = null,
    val outputThumbnailPath: String? = null,
    val outputSpectrumPath: String? = null,
    val resultLinks: List<TingwuResultLink> = emptyList(),
    val transcriptionUrl: String? = null,
    val autoChaptersUrl: String? = null,
    val extraResultUrls: Map<String, String> = emptyMap(),
    val chapters: List<TingwuChapter>? = null,
    val smartSummary: TingwuSmartSummary? = null
)

data class TingwuResultLink(
    val label: String,
    val url: String
)

sealed class TingwuJobState {
    data object Idle : TingwuJobState()
    data class InProgress(
        val jobId: String,
        val progressPercent: Int,
        val statusLabel: String? = null,
        val artifacts: TingwuJobArtifacts? = null
    ) : TingwuJobState()

    data class Completed(
        val jobId: String,
        val transcriptMarkdown: String,
        val artifacts: TingwuJobArtifacts? = null,
        val statusLabel: String? = null
    ) : TingwuJobState()

    data class Failed(
        val jobId: String,
        val error: AiCoreException,
        val errorCode: String? = null
    ) : TingwuJobState() {
        val reason: String = error.userFacingMessage
    }
}

interface TingwuCoordinator {
    suspend fun submit(request: TingwuRequest): Result<String>
    fun observeJob(jobId: String): Flow<TingwuJobState>
}

@Singleton
class FakeTingwuCoordinator @Inject constructor(
    private val dispatchers: DispatcherProvider
) : TingwuCoordinator {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)
    private val jobStates = mutableMapOf<String, MutableStateFlow<TingwuJobState>>()

    override suspend fun submit(request: TingwuRequest): Result<String> =
        withContext(dispatchers.io) {
            val jobId = "tingwu-${jobStates.size + 1}"
            val flow = MutableStateFlow<TingwuJobState>(TingwuJobState.InProgress(jobId, 10))
            jobStates[jobId] = flow
            scope.launch {
                delay(400)
                flow.value = TingwuJobState.InProgress(jobId, 60)
                delay(400)
                flow.value = TingwuJobState.Completed(
                    jobId = jobId,
                    transcriptMarkdown = """
                        ## ${request.audioAssetName} 转写
                        - 语言: ${request.language}
                        - 关键句: 客户确认了下季度的采购计划。
                    """.trimIndent()
                )
            }
            Result.Success(jobId)
        }

    override fun observeJob(jobId: String): Flow<TingwuJobState> =
        jobStates.getOrPut(jobId) { MutableStateFlow<TingwuJobState>(TingwuJobState.Idle) }.asStateFlow()
}
