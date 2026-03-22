package com.smartsales.prism.data.notification

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealExactAlarmPermissionGate @Inject constructor(
    @ApplicationContext private val context: Context
) : ExactAlarmPermissionGate {

    private val prompted = AtomicBoolean(false)

    override fun shouldPromptForExactAlarm(): Boolean {
        if (ReminderReliabilityAdvisor.fromContext(context) == null) return false
        return prompted.compareAndSet(false, true)
    }
}
