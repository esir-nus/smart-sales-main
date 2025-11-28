package com.smartsales.feature.usercenter

// 文件：feature/usercenter/src/main/java/com/smartsales/feature/usercenter/UserCenterScreen.kt
// 模块：:feature:usercenter
// 说明：用户中心 Compose 界面
// 作者：创建于 2025-11-21

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun UserCenterScreen(
    uiState: UserCenterUiState,
    onUserNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onToggleFeatureFlag: (String) -> Unit,
    onSaveClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
    onErrorDismissed: () -> Unit,
    onOpenDeviceManager: () -> Unit = {},
    onOpenSubscription: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenGeneral: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(UserCenterTestTags.ROOT),
        topBar = {
            TopAppBar(
                title = { Text(text = "用户中心") },
                actions = {
                    IconButton(onClick = onSaveClicked, enabled = !uiState.isSaving) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "管理账号、订阅与隐私设置，查看剩余配额。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (uiState.isSaving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            uiState.errorMessage?.let { message ->
                ErrorBanner(
                    message = message,
                    onDismiss = onErrorDismissed,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            ProfileHeader(uiState.userName)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.userName,
                        onValueChange = onUserNameChanged,
                        label = { Text(text = "用户名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = onEmailChanged,
                        label = { Text(text = "邮箱") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = uiState.tokensRemaining?.let { "剩余 Tokens：$it" } ?: "剩余 Tokens：--",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            FeatureFlagCard(
                flags = uiState.featureFlags,
                onToggle = onToggleFeatureFlag,
                modifier = Modifier.fillMaxWidth()
            )
            ShortcutMenuCard(
                onOpenDeviceManager = onOpenDeviceManager,
                onOpenSubscription = onOpenSubscription,
                onOpenPrivacy = onOpenPrivacy,
                onOpenGeneral = onOpenGeneral,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onSaveClicked,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = if (uiState.isSaving) "保存中..." else "保存")
                }
                TextButton(
                    onClick = onLogoutClicked,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "退出登录")
                }
            }
        }
    }
}

object UserCenterTestTags {
    const val ROOT = "user_center_screen_root"
}

@Composable
private fun ProfileHeader(userName: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = userName.take(1).ifBlank { "U" },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (userName.isBlank()) "未命名用户" else userName,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "订阅管理、隐私与通用设置",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShortcutMenuCard(modifier: Modifier = Modifier) {
    ShortcutMenuCard(
        onOpenDeviceManager = {},
        onOpenSubscription = {},
        onOpenPrivacy = {},
        onOpenGeneral = {},
        modifier = modifier
    )
}

@Composable
private fun ShortcutMenuCard(
    onOpenDeviceManager: () -> Unit,
    onOpenSubscription: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenGeneral: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "快捷入口", style = MaterialTheme.typography.titleMedium)
            ShortcutRow(
                title = "设备管理",
                subtitle = "管理已配对设备与文件",
                onClick = onOpenDeviceManager
            )
            HorizontalDivider()
            ShortcutRow(
                title = "订阅管理",
                subtitle = "查看和续费订阅套餐",
                onClick = onOpenSubscription
            )
            HorizontalDivider()
            ShortcutRow(
                title = "隐私与安全",
                subtitle = "密码、双重认证与数据控制",
                onClick = onOpenPrivacy
            )
            HorizontalDivider()
            ShortcutRow(
                title = "通用设置",
                subtitle = "语言与通知偏好",
                onClick = onOpenGeneral
            )
        }
    }
}

@Composable
private fun ShortcutRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FeatureFlagCard(
    flags: Map<String, Boolean>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "功能开关", style = MaterialTheme.typography.titleMedium)
            if (flags.isEmpty()) {
                Text(
                    text = "暂无可配置的功能开关。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                flags.entries.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = entry.key, style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = entry.value,
                            onCheckedChange = { onToggle(entry.key) }
                        )
                    }
                    if (index != flags.size - 1) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
        TextButton(onClick = onDismiss) {
            Text(text = "关闭")
        }
    }
}
