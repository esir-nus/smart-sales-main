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

import com.smartsales.prism.domain.repository.HistoryRepository
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.smartsales.prism.ui.PrismShell
import com.smartsales.prism.ui.onboarding.OnboardingScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Prism Main Activity
 * 
 * Entry point for the Prism Clean Room application.
 * Hosts the PrismShell with Scheduler/History drawers.
 */
@AndroidEntryPoint
class PrismMainActivity : ComponentActivity() {
    
    @Inject
    lateinit var historyRepository: HistoryRepository

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
                    var isOnboarding by rememberSaveable { mutableStateOf(true) }

                    if (isOnboarding) {
                        OnboardingScreen(onComplete = { isOnboarding = false })
                    } else {
                        PrismShell(
                            historyRepository = historyRepository
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
