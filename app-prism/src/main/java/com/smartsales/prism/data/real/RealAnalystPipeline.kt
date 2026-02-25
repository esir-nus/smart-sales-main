package com.smartsales.prism.data.real

import android.util.Log
import com.smartsales.prism.domain.analyst.AnalystPipeline
import com.smartsales.prism.domain.analyst.AnalystResponse
import com.smartsales.prism.domain.analyst.AnalystState
import com.smartsales.prism.domain.pipeline.ChatTurn
import com.smartsales.prism.domain.pipeline.ContextBuilder
import com.smartsales.prism.domain.pipeline.Executor
import com.smartsales.prism.domain.pipeline.ExecutorResult
import com.smartsales.prism.domain.model.Mode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import javax.inject.Inject

/**
 * Wave 2: Real Pipeline (Phase 1: Consultant)
 * This holds the rigid state machine logic and routes the LLM evaluation.
 */
class RealAnalystPipeline @Inject constructor(
    private val contextBuilder: ContextBuilder,
    private val executor: Executor
) : AnalystPipeline {

    private val TAG = "RealAnalystPipeline"

    private val _state = MutableStateFlow(AnalystState.IDLE)
    override val state = _state.asStateFlow()

    override suspend fun handleInput(
        input: String,
        sessionHistory: List<ChatTurn>
    ): AnalystResponse {
        Log.d(TAG, "handleInput: state=${_state.value} input=$input")

        return when (_state.value) {
            AnalystState.IDLE -> {
                _state.value = AnalystState.CONSULTING
                val context = contextBuilder.build(input, Mode.ANALYST)
                val result = executor.execute(context)

                if (result is ExecutorResult.Failure) {
                    _state.value = AnalystState.IDLE
                    return AnalystResponse.Chat("网络通信异常，请重试。")
                }

                val content = (result as ExecutorResult.Success).content
                val sanitized = content.replace("```json", "").replace("```", "").trim()

                try {
                    val json = JSONObject(sanitized)
                    val infoSufficient = json.optBoolean("info_sufficient", false)
                    val response = json.optString("response", "")

                    if (!infoSufficient) {
                        _state.value = AnalystState.IDLE
                        return AnalystResponse.Chat(response)
                    } else {
                        // Phase 2 (Wave 3) will do LLM Generation. Stub for now.
                        _state.value = AnalystState.PROPOSAL
                        return AnalystResponse.Plan(
                            title = "📋 分析计划",
                            summary = response.ifEmpty { "即将基于您的需求生成分析计划。" },
                            steps = emptyList() // Or stubbed steps
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse Analyst JSON: $sanitized", e)
                    _state.value = AnalystState.IDLE
                    return AnalystResponse.Chat("我没完全明白，能再详细说说你想分析的内容吗？")
                }
            }
            AnalystState.PROPOSAL -> {
                _state.value = AnalystState.IDLE
                return AnalystResponse.Chat("执行引擎正在升级中 (Wave 3)，目前仅支持到计划生成阶段。")
            }
            else -> {
                Log.w(TAG, "Invalid statemachine state. Resetting.")
                _state.value = AnalystState.IDLE
                return AnalystResponse.Chat("状态机发生错误，已重置。")
            }
        }
    }
}
