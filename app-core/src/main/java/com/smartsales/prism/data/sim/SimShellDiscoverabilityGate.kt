package com.smartsales.prism.data.sim

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SIM 壳层一次性可发现性提示开关。
 */
@Singleton
class SimShellDiscoverabilityGate @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun shouldShowFirstLaunchSchedulerTeaser(): Boolean {
        return !prefs.getBoolean(KEY_SCHEDULER_TEASER_SEEN, false)
    }

    fun markFirstLaunchSchedulerTeaserSeen() {
        prefs.edit().putBoolean(KEY_SCHEDULER_TEASER_SEEN, true).apply()
    }

    companion object {
        private const val PREFS_NAME = "sim_shell_discoverability_gate"
        private const val KEY_SCHEDULER_TEASER_SEEN = "scheduler_teaser_seen"
    }
}
