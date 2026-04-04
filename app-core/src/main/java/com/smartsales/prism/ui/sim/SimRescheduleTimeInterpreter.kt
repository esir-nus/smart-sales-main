package com.smartsales.prism.ui.sim

import com.smartsales.core.pipeline.RealUniAExtractionService
import com.smartsales.core.pipeline.SchedulerRescheduleTimeInterpreter
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.time.TimeProvider
import java.time.Instant

/**
 * SIM 改期时间解释器。
 *
 * 说明：保留 SIM 兼容入口，真实语义委托给核心层共享解释器，
 * 避免不同 surface 的改期时间规则继续漂移。
 */
internal object SimRescheduleTimeInterpreter {

    sealed interface Result {
        data class Success(
            val startTime: Instant,
            val durationMinutes: Int? = null
        ) : Result

        data object Unsupported : Result
        data object InvalidExactTime : Result
    }

    suspend fun resolve(
        originalTask: ScheduledTask,
        transcript: String,
        displayedDateIso: String,
        timeProvider: TimeProvider,
        uniAExtractionService: RealUniAExtractionService
    ): Result {
        return when (
            val result = SchedulerRescheduleTimeInterpreter.resolveNaturalInstruction(
                originalTask = originalTask,
                transcript = transcript,
                displayedDateIso = displayedDateIso,
                timeProvider = timeProvider,
                uniAExtractionService = uniAExtractionService
            )
        ) {
            is SchedulerRescheduleTimeInterpreter.Result.Success -> {
                Result.Success(
                    startTime = result.startTime,
                    durationMinutes = result.durationMinutes
                )
            }
            SchedulerRescheduleTimeInterpreter.Result.Unsupported -> Result.Unsupported
            SchedulerRescheduleTimeInterpreter.Result.InvalidExactTime -> Result.InvalidExactTime
        }
    }
}
