package com.smartsales.aitest

// 文件：app/src/main/java/com/smartsales/aitest/DummyTestActivity.kt
// 模块：:app
// 说明：用于排查 instrumentation 是否能正常启动 Activity 的极简测试壳
// 作者：创建于 2025-11-29

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

class DummyTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate START intent=$intent")
        super.onCreate(savedInstanceState)
        Log.d(TAG, "after super.onCreate")

        setContent {
            Log.d(TAG, "setContent start")
            DummyRoot()
            Log.d(TAG, "setContent end")
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }

    companion object {
        private const val TAG = "DummyTest"
    }
}

@Composable
private fun DummyRoot() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("DUMMY_ROOT")
    ) {
        Text("Dummy")
    }
}
