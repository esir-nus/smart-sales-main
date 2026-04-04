package com.smartsales.prism.data.onboarding

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared production onboarding-to-shell handoff gate。
 *
 * Keeps the post-onboarding scheduler reveal as a one-shot shell handoff
 * instead of a direct onboarding callback into the runtime drawer.
 */
interface RuntimeOnboardingHandoffGate {
    fun shouldAutoOpenSchedulerAfterOnboarding(): Boolean
    fun markSchedulerAutoOpenPending()
    fun consumeSchedulerAutoOpenPending()
}

@Singleton
class BaseRuntimeOnboardingHandoffGate @Inject constructor(
    @ApplicationContext private val context: Context
) : RuntimeOnboardingHandoffGate {

    override fun shouldAutoOpenSchedulerAfterOnboarding(): Boolean {
        return allPrefs().any { prefs -> prefs.getBoolean(KEY_SCHEDULER_AUTO_OPEN_PENDING, false) }
    }

    override fun markSchedulerAutoOpenPending() {
        writeFlag(true)
    }

    override fun consumeSchedulerAutoOpenPending() {
        writeFlag(false)
    }

    private fun writeFlag(value: Boolean) {
        allPrefs().forEach { prefs ->
            prefs.edit().putBoolean(KEY_SCHEDULER_AUTO_OPEN_PENDING, value).apply()
        }
    }

    private fun allPrefs() = PREFS_NAMES.map { name ->
        context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_SCHEDULER_AUTO_OPEN_PENDING = "scheduler_auto_open_pending"
        private val PREFS_NAMES = listOf(
            "base_runtime_onboarding_handoff_gate",
            "prism_onboarding_handoff_gate"
        )
    }
}
