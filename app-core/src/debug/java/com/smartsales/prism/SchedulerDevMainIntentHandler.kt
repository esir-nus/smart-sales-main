package com.smartsales.prism

import android.content.Intent
import android.util.Log
import com.smartsales.prism.ui.components.DynamicIslandSchedulerTarget
import com.smartsales.prism.ui.drawers.scheduler.DevInjectSource
import com.smartsales.prism.ui.drawers.scheduler.SchedulerDevInjectionBridge
import com.smartsales.prism.ui.drawers.scheduler.SchedulerDevInjectionRequest
import com.smartsales.prism.ui.drawers.scheduler.dev.SchedulerTestScenarios
import java.time.LocalDate

object SchedulerDevMainIntentHandler {
    const val EXTRA_TEXT = "scheduler_dev_text"
    const val EXTRA_DISPLAYED_DATE_ISO = "scheduler_dev_displayedDateIso"
    const val EXTRA_SCENARIO_ID = "scheduler_dev_scenarioId"
    const val EXTRA_OPEN_SCHEDULER = "scheduler_dev_openScheduler"
    const val EXTRA_OPEN_SCHEDULER_DATE_ISO = "scheduler_dev_openSchedulerDateIso"

    fun handleIntent(intent: Intent?): Boolean {
        if (!BuildConfig.ENABLE_SCHEDULER_DEV_TOOLS || intent == null) {
            return false
        }

        val scenarioId = intent.getStringExtra(EXTRA_SCENARIO_ID)
        val scenario = scenarioId?.let(SchedulerTestScenarios::find)
        val text = intent.getStringExtra(EXTRA_TEXT)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: scenario?.utterance
            ?: return false
        val displayedDateIso = intent.getStringExtra(EXTRA_DISPLAYED_DATE_ISO)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: scenario?.displayedDateOffset?.let { offset ->
                LocalDate.now().plusDays(offset.toLong()).toString()
            }

        val emitted = SchedulerDevInjectionBridge.emit(
            SchedulerDevInjectionRequest(
                text = text,
                displayedDateIso = displayedDateIso,
                scenarioId = scenarioId,
                source = DevInjectSource.BROADCAST
            )
        )
        Log.i(
            "SchedulerDevIntent",
            "handleIntent emitted=$emitted scenarioId=${scenarioId ?: "null"} displayedDateIso=${displayedDateIso ?: "null"} text=$text"
        )
        return emitted
    }

    fun shouldOpenScheduler(intent: Intent?): Boolean {
        return BuildConfig.ENABLE_SCHEDULER_DEV_TOOLS &&
            intent?.getBooleanExtra(EXTRA_OPEN_SCHEDULER, false) == true
    }

    fun schedulerAutoOpenTarget(intent: Intent?): DynamicIslandSchedulerTarget? {
        if (!BuildConfig.ENABLE_SCHEDULER_DEV_TOOLS) {
            return null
        }
        val dateIso = intent?.getStringExtra(EXTRA_OPEN_SCHEDULER_DATE_ISO)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        val targetDate = runCatching { LocalDate.parse(dateIso) }.getOrNull() ?: return null
        return DynamicIslandSchedulerTarget(date = targetDate)
    }
}
