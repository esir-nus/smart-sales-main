package com.smartsales.aitest.ui.screens.user

// 文件：app/src/main/java/com/smartsales/aitest/ui/screens/user/UserCenterScreen.kt
// 模块：:app
// 说明：底部导航用户中心页（本地模拟资料与设置列表）
// 作者：创建于 2025-12-02

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.smartsales.aitest.ui.components.ProfileHeaderCard
import com.smartsales.aitest.ui.components.SettingsMenuItem
import com.smartsales.aitest.ui.screens.user.model.SubscriptionTierUi
import com.smartsales.aitest.ui.screens.user.model.UserProfileUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserCenterScreen() {
    var userProfile by remember {
        mutableStateOf(
            UserProfileUi(
                id = "user-001",
                fullName = "张三",
                email = "zhangsan@example.com",
                phoneNumber = "13800000000",
                avatarUrl = null,
                role = com.smartsales.aitest.ui.screens.user.model.UserRoleUi.USER,
                subscriptionTier = SubscriptionTierUi.PREMIUM,
                joinedDate = System.currentTimeMillis()
            )
        )
    }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val deviceServiceItems = listOf(
        SettingsItemData(
            icon = Icons.Filled.Devices,
            title = "设备管理",
            subtitle = "管理连接的设备和文件"
        ),
        SettingsItemData(
            icon = Icons.Filled.ManageAccounts,
            title = "订阅管理",
            subtitle = userProfile.subscriptionTier.label()
        )
    )
    val privacyItems = listOf(
        SettingsItemData(
            icon = Icons.Filled.PrivacyTip,
            title = "隐私设置",
            subtitle = "管理数据和权限"
        ),
        SettingsItemData(
            icon = Icons.Filled.Security,
            title = "账号安全",
            subtitle = "密码和验证设置"
        )
    )
    val generalItems = listOf(
        SettingsItemData(
            icon = Icons.Filled.Settings,
            title = "常规设置",
            subtitle = "语言、通知等"
        ),
        SettingsItemData(
            icon = Icons.Filled.Info,
            title = "关于应用",
            subtitle = "版本 1.0.0"
        )
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text(text = "用户中心") }) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("page_user_center"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                ProfileHeaderCard(
                    userProfile = userProfile,
                    onEditClick = { /* TODO: 导航到编辑资料 */ },
                    onLoginClick = { /* TODO: 登录 */ },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            item { SectionHeader(text = "设备与服务") }
            items(deviceServiceItems) { item ->
                SettingsMenuItem(
                    icon = item.icon,
                    title = item.title,
                    subtitle = item.subtitle,
                    onClick = { /* TODO: 导航到对应页面，如切换到底部设备页 */ },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
            item { SectionHeader(text = "隐私与安全") }
            items(privacyItems) { item ->
                SettingsMenuItem(
                    icon = item.icon,
                    title = item.title,
                    subtitle = item.subtitle,
                    onClick = { /* TODO: 隐私与安全设置 */ },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
            item { SectionHeader(text = "通用设置") }
            items(generalItems) { item ->
                SettingsMenuItem(
                    icon = item.icon,
                    title = item.title,
                    subtitle = item.subtitle,
                    onClick = { /* TODO: 通用设置 */ },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
            item {
                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "退出登录",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(text = "确认退出") },
            text = { Text(text = "确定要退出登录吗？") },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false /* TODO: 执行退出 */ }) {
                    Text(text = "确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(text = "取消")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
    )
}

private data class SettingsItemData(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val subtitle: String?
)
