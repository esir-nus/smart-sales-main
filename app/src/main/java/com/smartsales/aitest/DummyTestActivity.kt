package com.smartsales.aitest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class DummyTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlaceholderScreen(
                title = "Dummy Test",
                message = "DummyTestActivity placeholder is mounted."
            )
        }
    }
}
