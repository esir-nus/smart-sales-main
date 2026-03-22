package com.smartsales.prism

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.smartsales.prism.domain.audio.BadgeAudioPipeline
import com.smartsales.prism.ui.sim.SimDebugFollowUpScenario
import com.smartsales.prism.ui.sim.SimShell
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

    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.values.all { it }) {
            Toast.makeText(this, "日历权限被拒绝，SIM Scheduler 仅展示壳层能力", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestCalendarPermissions()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SimShell(
                        badgeAudioPipeline = badgeAudioPipeline,
                        debugFollowUpScenario = parseDebugFollowUpScenario()
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
