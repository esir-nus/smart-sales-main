package com.smartsales.prism

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.prism.domain.audio.BadgeAudioPipeline
import com.smartsales.prism.data.sim.SimOnboardingGate
import com.smartsales.prism.data.sim.SimShellDiscoverabilityGate
import com.smartsales.prism.ui.sim.SimDebugFollowUpScenario
import com.smartsales.prism.ui.sim.SimShell
import com.smartsales.prism.ui.theme.PrismTheme
import com.smartsales.prism.ui.theme.PrismSystemBarsEffect
import com.smartsales.prism.ui.theme.ThemePreferenceStore
import com.smartsales.prism.ui.theme.resolvePrismDarkTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * SIM 入口 Activity。
 * 保持同一应用进程，但使用独立 Shell 根节点。
 */
@AndroidEntryPoint
class SimMainActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_DEBUG_FOLLOW_UP_SINGLE = "sim_debug_followup_single"
        private const val EXTRA_DEBUG_FOLLOW_UP_MULTI = "sim_debug_followup_multi"
    }

    @Inject
    lateinit var badgeAudioPipeline: BadgeAudioPipeline

    @Inject
    lateinit var discoverabilityGate: SimShellDiscoverabilityGate

    @Inject
    lateinit var simOnboardingGate: SimOnboardingGate

    @Inject
    lateinit var themePreferenceStore: ThemePreferenceStore

    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.values.all { it }) {
            Toast.makeText(this, "日历权限被拒绝，SIM Scheduler 仅展示壳层能力", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(TRANSPARENT, TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(TRANSPARENT, TRANSPARENT)
        )

        setContent {
            val themeMode by themePreferenceStore.themeMode.collectAsStateWithLifecycle()
            val hasStoredThemeMode by themePreferenceStore.hasStoredThemeMode.collectAsStateWithLifecycle()
            val simOnboardingCompleted by simOnboardingGate.completedFlow.collectAsStateWithLifecycle()
            val systemDarkTheme = isSystemInDarkTheme()
            var calendarPermissionsRequested by remember { mutableStateOf(false) }
            val darkTheme = remember(themeMode, hasStoredThemeMode, systemDarkTheme) {
                resolveSimDarkTheme(
                    themeMode = themeMode,
                    hasStoredThemeMode = hasStoredThemeMode,
                    systemDarkTheme = systemDarkTheme
                )
            }

            LaunchedEffect(simOnboardingCompleted, calendarPermissionsRequested) {
                if (shouldRequestSimCalendarPermissions(simOnboardingCompleted, calendarPermissionsRequested)) {
                    calendarPermissionsRequested = true
                    requestCalendarPermissions()
                }
            }

            PrismTheme(darkTheme = darkTheme) {
                PrismSystemBarsEffect(
                    activity = this,
                    darkTheme = darkTheme
                )
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SimShell(
                        badgeAudioPipeline = badgeAudioPipeline,
                        debugFollowUpScenario = parseDebugFollowUpScenario(),
                        forceSetupOnLaunch = !simOnboardingCompleted,
                        onForcedSetupCompleted = simOnboardingGate::markCompleted,
                        shouldShowFirstLaunchSchedulerTeaser = discoverabilityGate.shouldShowFirstLaunchSchedulerTeaser(),
                        onFirstLaunchSchedulerTeaserShown = discoverabilityGate::markFirstLaunchSchedulerTeaserSeen
                    )
                }
            }
        }
    }

    private fun requestCalendarPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            calendarPermissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    private fun parseDebugFollowUpScenario(): SimDebugFollowUpScenario? {
        if (!BuildConfig.DEBUG) return null
        return when {
            intent?.getBooleanExtra(EXTRA_DEBUG_FOLLOW_UP_MULTI, false) == true ->
                SimDebugFollowUpScenario.MULTI
            intent?.getBooleanExtra(EXTRA_DEBUG_FOLLOW_UP_SINGLE, false) == true ->
                SimDebugFollowUpScenario.SINGLE
            else -> null
        }
    }
}

internal fun shouldRequestSimCalendarPermissions(
    simOnboardingCompleted: Boolean,
    requestAlreadyAttempted: Boolean
): Boolean = simOnboardingCompleted && !requestAlreadyAttempted

internal fun resolveSimDarkTheme(
    themeMode: com.smartsales.prism.ui.theme.PrismThemeMode,
    hasStoredThemeMode: Boolean,
    systemDarkTheme: Boolean
): Boolean {
    val effectiveThemeMode = if (hasStoredThemeMode) {
        themeMode
    } else {
        com.smartsales.prism.ui.theme.PrismThemeMode.DARK
    }
    return resolvePrismDarkTheme(
        themeMode = effectiveThemeMode,
        systemDarkTheme = systemDarkTheme
    )
}
