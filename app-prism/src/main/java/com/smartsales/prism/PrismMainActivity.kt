package com.smartsales.prism

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.smartsales.prism.domain.core.HistoryRepository
import com.smartsales.prism.ui.PrismShell
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PrismShell(historyRepository = historyRepository)
                }
            }
        }
    }
}
