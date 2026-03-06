package com.smartsales.prism.data.fakes

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.prism.domain.tingwu.TingwuJobState
import com.smartsales.prism.domain.tingwu.TingwuPipeline
import com.smartsales.prism.domain.tingwu.TingwuRequest
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import com.smartsales.prism.domain.tingwu.TingwuSmartSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeTingwuPipeline @Inject constructor(
    private val dispatchers: DispatcherProvider
) : TingwuPipeline {

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
                val mockArtifacts = TingwuJobArtifacts(
                    smartSummary = TingwuSmartSummary("AI生成的假数据摘要：客户决定采购该产品。")
                )
                flow.value = TingwuJobState.Completed(
                    jobId = jobId,
                    transcriptMarkdown = """
                        ## ${request.audioAssetName} 转写
                        - 语言: ${request.language}
                        - 关键句: 客户确认了下季度的采购计划。
                    """.trimIndent(),
                    artifacts = mockArtifacts
                )
            }
            Result.Success(jobId)
        }

    override fun observeJob(jobId: String): Flow<TingwuJobState> =
        jobStates.getOrPut(jobId) { MutableStateFlow<TingwuJobState>(TingwuJobState.Idle) }.asStateFlow()
}
