package com.smartsales.prism.ui.onboarding

import android.content.Context
import com.smartsales.prism.data.notification.ExactAlarmPermissionGate
import com.smartsales.prism.data.notification.ReminderReliabilityAdvisor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * quick start 首轮后的提醒可靠性引导协调器。
 */
interface OnboardingQuickStartReminderGuideCoordinator {
    fun consumeGuideIfNeeded(): ReminderReliabilityAdvisor.ReminderReliabilityGuide?
    fun openAction(action: ReminderReliabilityAdvisor.Action): Boolean
}

@Singleton
class RealOnboardingQuickStartReminderGuideCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exactAlarmPermissionGate: ExactAlarmPermissionGate
) : OnboardingQuickStartReminderGuideCoordinator {

    override fun consumeGuideIfNeeded(): ReminderReliabilityAdvisor.ReminderReliabilityGuide? {
        val guide = ReminderReliabilityAdvisor.fromContext(context) ?: return null
        if (!exactAlarmPermissionGate.shouldPromptForExactAlarm()) return null
        return guide
    }

    override fun openAction(action: ReminderReliabilityAdvisor.Action): Boolean {
        return ReminderReliabilityAdvisor.openAction(context, action)
    }
}
