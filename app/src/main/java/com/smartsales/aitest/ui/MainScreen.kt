package com.smartsales.aitest.ui

// 文件：app/src/main/java/com/smartsales/aitest/ui/MainScreen.kt
// 模块：:app
// 说明：底部导航壳，承载四个主入口的占位页
// 作者：创建于 2025-12-02

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smartsales.aitest.navigation.Screen
import com.smartsales.aitest.ui.screens.history.ChatHistoryShell
import com.smartsales.aitest.ui.screens.audio.AudioFilesScreen
import com.smartsales.aitest.ui.screens.device.DeviceManagerScreen
import com.smartsales.aitest.ui.screens.home.HomeScreen
import com.smartsales.aitest.ui.screens.user.UserCenterScreen
import com.smartsales.feature.chat.home.HomeScreenViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val items = Screen.items
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(imageVector = screen.icon, contentDescription = screen.label) },
                        label = { Text(text = screen.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { backStackEntry ->
                val homeViewModel: HomeScreenViewModel = hiltViewModel(backStackEntry)
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToHistory = { navController.navigate(Screen.ChatHistory.route) }
                )
            }
            composable(Screen.Audio.route) { AudioFilesScreen() }
            composable(Screen.Device.route) { DeviceManagerScreen() }
            composable(Screen.User.route) { UserCenterScreen() }
            composable(Screen.ChatHistory.route) {
                val homeBackStackEntry = remember { navController.getBackStackEntry(Screen.Home.route) }
                val homeViewModel: HomeScreenViewModel = hiltViewModel(homeBackStackEntry)
                ChatHistoryShell(
                    homeViewModel = homeViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

// 可选预览：在后续需要时可包装 AppTheme 进行预览
