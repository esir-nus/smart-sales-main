package com.smartsales.data.prismlib.pipeline

import com.smartsales.domain.prism.core.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Prism 调度器 — 无状态路由，组装管道步骤
 * @see Prism-V1.md §2.2 #2
 */
@Singleton
class PrismOrchestrator @Inject constructor(
    private val contextBuilder: ContextBuilder,
    private val executor: Executor,
    private val memoryWriter: MemoryWriter
) : Orchestrator {

    private val _currentMode = MutableStateFlow(Mode.COACH)
    override val currentMode: StateFlow<Mode> = _currentMode.asStateFlow()

    override suspend fun processUserIntent(input: String): OrchestratorResult {
        val mode = _currentMode.value

        // Step 1: Build Context
        val context = try {
            contextBuilder.buildContext(input, mode)
        } catch (e: Exception) {
            return OrchestratorResult(
                mode = mode,
                executorResult = ExecutorResult(
                    displayContent = "上下文构建失败: ${e.message}",
                    structuredJson = null
                ),
                memoryWriteTriggered = false,
                uiState = UiState.Error(e.message ?: "Unknown error")
            )
        }

        // Step 2: Execute LLM
        val result = try {
            executor.execute(context)
        } catch (e: Exception) {
            return OrchestratorResult(
                mode = mode,
                executorResult = ExecutorResult(
                    displayContent = "执行失败: ${e.message}",
                    structuredJson = null
                ),
                memoryWriteTriggered = false,
                uiState = UiState.Error(e.message ?: "Unknown error")
            )
        }

        // Step 3: Persist to Memory (Fire-and-Forget)
        memoryWriter.persist(context, result)

        return OrchestratorResult(
            mode = mode,
            executorResult = result,
            memoryWriteTriggered = true,
            uiState = UiState.Response(result.displayContent, result.structuredJson)
        )
    }

    override suspend fun switchMode(newMode: Mode) {
        _currentMode.value = newMode
    }
}
