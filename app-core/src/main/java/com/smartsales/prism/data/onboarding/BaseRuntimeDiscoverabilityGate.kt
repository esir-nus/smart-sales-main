package com.smartsales.prism.data.onboarding

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared shell discoverability gate.
 *
 * Preserves the previous scheduler-teaser completion signal while moving the
 * production root onto one canonical host.
 */
@Singleton
class BaseRuntimeDiscoverabilityGate @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun shouldShowFirstLaunchSchedulerTeaser(): Boolean {
        return allPrefs().none { prefs -> prefs.getBoolean(KEY_SCHEDULER_TEASER_SEEN, false) }
    }

    fun markFirstLaunchSchedulerTeaserSeen() {
        allPrefs().forEach { prefs ->
            prefs.edit().putBoolean(KEY_SCHEDULER_TEASER_SEEN, true).apply()
        }
    }

    private fun allPrefs() = PREFS_NAMES.map { name ->
        context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_SCHEDULER_TEASER_SEEN = "scheduler_teaser_seen"
        private val PREFS_NAMES = listOf(
            "base_runtime_shell_discoverability_gate",
            "sim_shell_discoverability_gate"
        )
    }
}
