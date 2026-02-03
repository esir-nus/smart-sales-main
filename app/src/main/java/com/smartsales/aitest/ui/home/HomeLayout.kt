package com.smartsales.aitest.ui.home

// 文件：app/src/main/java/com/smartsales/aitest/ui/home/HomeLayout.kt
// 模块：:app
// 说明：首页主布局 — "Executive Desk" (Chapter 5 Layout)
//       整合 Header, Hero, Input Area 以及各个 Drawer
// 作者：创建于 2026-01-30 (Chapter 5 VI Guide)

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.smartsales.aitest.ui.home.components.ActiveSessionArea
import com.smartsales.aitest.ui.home.components.HomeHeader
import com.smartsales.aitest.ui.home.components.HomeHero
import com.smartsales.aitest.ui.home.components.SessionMode
import com.smartsales.aitest.ui.home.ConnectivityModal
import kotlinx.coroutines.launch

/**
 * HomeLayout — 首页顶级容器
 *
 * 包含:
 * 1. History Drawer (Left)
 * 2. Scheduler Drawer (Top - Placeholder)
 * 3. Audio Drawer (Bottom - Placeholder)
 * 4. Main Content (Header + Hero + Input)
 */
@Composable
fun HomeLayout() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // State Placeholders
    var sessionTitle by remember { mutableStateOf("Session: CEO Wang") }
    var isConnected by remember { mutableStateOf(false) }
    var showConnectionModal by remember { mutableStateOf(false) }

    if (showConnectionModal) {
        ConnectivityModal(
            isConnected = isConnected,
            onDismissRequest = { showConnectionModal = false },
            onConnectClick = {
                // Simulate Connection
                isConnected = true
                showConnectionModal = false
            }
        )
    }

    // History Drawer 作为最底层容器
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HistoryDrawer(
                onItemClick = { item ->
                    sessionTitle = item.title
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                HomeHeader(
                    title = sessionTitle,
                    isConnected = isConnected,
                    onHistoryClick = {
                        scope.launch { drawerState.open() }
                    },
                    onConnectionClick = {
                        showConnectionModal = true
                    },
                    onNewSessionClick = {
                        sessionTitle = "New Session"
                    },
                    onDebugClick = { 
                        // Toggle Debug
                    }
                )
            },
            content = { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Main Content Column
                    Column {
                        Spacer(modifier = Modifier.weight(1f))
                        
                        HomeHero(
                            userName = "Frank" // Should come from UserProfile
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        ActiveSessionArea(
                            onSend = { text, mode ->
                                // Handle Send
                            }
                        )
                    }
                    
                    // TODO: Texturing / Noise overlay could go here
                }
            }
        )
    }
}
