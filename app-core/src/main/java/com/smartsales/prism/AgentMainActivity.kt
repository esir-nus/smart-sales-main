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
import androidx.compose.runtime.*

import com.smartsales.prism.data.onboarding.OnboardingGate
import com.smartsales.prism.ui.AgentShell
import com.smartsales.prism.ui.onboarding.OnboardingScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Agent Main Activity
 * 
 * Entry point for the Agent Clean Room application.
 * Hosts the AgentShell with Scheduler/History drawers.
 */
@AndroidEntryPoint
class AgentMainActivity : ComponentActivity() {
    
    @Inject
    lateinit var onboardingGate: OnboardingGate

    // 日历权限请求
    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, "日历权限被拒绝，Scheduler 功能受限", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 请求日历权限
        requestCalendarPermissions()
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val onboardingCompleted by onboardingGate.completedFlow.collectAsState()

                    if (!onboardingCompleted) {
                        OnboardingScreen(onComplete = { 
                            onboardingGate.markCompleted()
                        })
                    } else {
                        AgentShell(
                            onNavigateToSetup = { onboardingGate.reset() }
                        )
                    }
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
}
