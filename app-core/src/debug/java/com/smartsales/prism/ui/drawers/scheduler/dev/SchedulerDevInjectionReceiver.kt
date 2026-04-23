package com.smartsales.prism.ui.drawers.scheduler.dev

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.smartsales.prism.BuildConfig
import com.smartsales.prism.ui.drawers.scheduler.DevInjectSource
import com.smartsales.prism.ui.drawers.scheduler.SchedulerDevInjectionBridge
import com.smartsales.prism.ui.drawers.scheduler.SchedulerDevInjectionRequest
import java.time.LocalDate

class SchedulerDevInjectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(
            "SchedulerDevReceiver",
            "onReceive action=${intent.action ?: "null"} enabled=${BuildConfig.ENABLE_SCHEDULER_DEV_TOOLS}"
        )
        if (!BuildConfig.ENABLE_SCHEDULER_DEV_TOOLS || intent.action != ACTION_INJECT_TRANSCRIPT) {
            return
        }

        val scenarioId = intent.getStringExtra(EXTRA_SCENARIO_ID)
        val scenario = scenarioId?.let(SchedulerTestScenarios::find)
        val text = intent.getStringExtra(EXTRA_TEXT)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: scenario?.utterance
            ?: return
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

        Log.d(
            "SchedulerDevReceiver",
            "inject broadcast emitted=$emitted scenarioId=${scenarioId ?: "null"} displayedDateIso=${displayedDateIso ?: "null"} text=$text"
        )
    }

    companion object {
        const val ACTION_INJECT_TRANSCRIPT = "com.smartsales.prism.dev.INJECT_TRANSCRIPT"
        const val EXTRA_TEXT = "text"
        const val EXTRA_DISPLAYED_DATE_ISO = "displayedDateIso"
        const val EXTRA_SCENARIO_ID = "scenarioId"
    }
}
