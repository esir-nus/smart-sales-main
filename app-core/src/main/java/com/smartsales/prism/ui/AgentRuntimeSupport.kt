package com.smartsales.prism.ui

import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import com.smartsales.prism.domain.system.SystemEvent
import com.smartsales.prism.domain.system.SystemEventBus
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal class AgentRuntimeSupport(
    private val scheduledTaskRepository: ScheduledTaskRepository,
    private val eventBus: SystemEventBus,
    private val bridge: AgentUiBridge
) {

    fun refreshHeroDashboard(scope: CoroutineScope) {
        scope.launch { loadHeroDashboard() }
    }

    fun launchIdleWatcher(
        scope: CoroutineScope,
        uiState: Flow<UiState>,
        inputText: Flow<String>
    ) {
        scope.launch {
            combine(uiState, inputText) { state, _ -> state }
                .collectLatest { state ->
                    if (state is UiState.Idle) {
                        delay(15000)
                        eventBus.publish(SystemEvent.AppIdle)
                    }
                }
        }
    }

    private suspend fun loadHeroDashboard() {
        val today = LocalDate.now()
        val allUpcoming = scheduledTaskRepository
            .queryByDateRange(today, today.plusDays(3))
            .first()

        bridge.setHeroUpcoming(
            allUpcoming
                .filterIsInstance<ScheduledTask>()
                .filter { !it.isDone }
                .filter { it.urgencyLevel != UrgencyLevel.FIRE_OFF }
                .take(3)
        )
        bridge.setHeroAccomplished(scheduledTaskRepository.getRecentCompleted(2))
    }
}
