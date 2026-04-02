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
import com.smartsales.prism.data.onboarding.BaseRuntimeOnboardingGate
import com.smartsales.prism.data.onboarding.BaseRuntimeDiscoverabilityGate
import com.smartsales.prism.ui.sim.SimDebugFollowUpScenario
import com.smartsales.prism.ui.RuntimeShell
import com.smartsales.prism.ui.theme.PrismTheme
import com.smartsales.prism.ui.theme.PrismSystemBarsEffect
import com.smartsales.prism.ui.theme.ThemePreferenceStore
import com.smartsales.prism.ui.theme.resolvePrismDarkTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Shared production entry activity.
 *
 * Owns the single launcher/root contract for the unified base runtime shell.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_DEBUG_FOLLOW_UP_SINGLE = "sim_debug_followup_single"
        private const val EXTRA_DEBUG_FOLLOW_UP_MULTI = "sim_debug_followup_multi"
    }

    @Inject
    lateinit var badgeAudioPipeline: BadgeAudioPipeline

    @Inject
    lateinit var discoverabilityGate: BaseRuntimeDiscoverabilityGate

    @Inject
    lateinit var onboardingGate: BaseRuntimeOnboardingGate

    @Inject
    lateinit var themePreferenceStore: ThemePreferenceStore

    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.values.all { it }) {
            Toast.makeText(this, "日历权限被拒绝，Scheduler 功能受限", Toast.LENGTH_LONG).show()
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
            val onboardingCompleted by onboardingGate.completedFlow.collectAsStateWithLifecycle()
            val systemDarkTheme = isSystemInDarkTheme()
            var calendarPermissionsRequested by remember { mutableStateOf(false) }
            val darkTheme = remember(themeMode, hasStoredThemeMode, systemDarkTheme) {
                resolveBaseRuntimeDarkTheme(
                    themeMode = themeMode,
                    hasStoredThemeMode = hasStoredThemeMode,
                    systemDarkTheme = systemDarkTheme
                )
            }

            LaunchedEffect(onboardingCompleted, calendarPermissionsRequested) {
                if (shouldRequestBaseRuntimeCalendarPermissions(onboardingCompleted, calendarPermissionsRequested)) {
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
                    RuntimeShell(
                        badgeAudioPipeline = badgeAudioPipeline,
                        debugFollowUpScenario = parseDebugFollowUpScenario(),
                        forceSetupOnLaunch = !onboardingCompleted,
                        onForcedSetupCompleted = onboardingGate::markCompleted,
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

internal fun shouldRequestBaseRuntimeCalendarPermissions(
    onboardingCompleted: Boolean,
    requestAlreadyAttempted: Boolean
): Boolean = onboardingCompleted && !requestAlreadyAttempted

internal fun resolveBaseRuntimeDarkTheme(
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
