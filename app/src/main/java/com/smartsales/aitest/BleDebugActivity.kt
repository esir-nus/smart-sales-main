package com.smartsales.aitest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class BleDebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlaceholderScreen(
                title = "BLE Debug",
                message = "BleDebugActivity placeholder is mounted."
            )
        }
    }
}
