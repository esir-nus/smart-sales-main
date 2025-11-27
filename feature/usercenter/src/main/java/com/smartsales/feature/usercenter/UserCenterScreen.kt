package com.smartsales.feature.usercenter

// 文件：feature/usercenter/src/main/java/com/smartsales/feature/usercenter/UserCenterScreen.kt
// 模块：:feature:usercenter
// 说明：用户中心 Compose 界面
// 作者：创建于 2025-11-21

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Divider
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

object UserCenterTestTags {
    const val ROOT = "user_center_screen_root"
}

private object UserCenterPalette {
    val BackgroundStart = Color(0xFFF7F9FF)
    val BackgroundEnd = Color(0xFFEFF2F7)
    val Card = Color(0xFFFFFFFF)
    val CardMuted = Color(0xFFF8F9FB)
    val Border = Color(0xFFE5E5EA)
    val Accent = Color(0xFF4B7BEC)
}

private object UserCenterShapes {
    val Card = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    val Button = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
}

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
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(UserCenterTestTags.ROOT),
        containerColor = Color.Transparent,
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
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(UserCenterPalette.BackgroundStart, UserCenterPalette.BackgroundEnd)
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.isSaving) {
                UserCenterCard {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            uiState.errorMessage?.let { message ->
                UserCenterCard {
                    ErrorBanner(
                        message = message,
                        onDismiss = onErrorDismissed,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            UserCenterCard { ProfileHeader(uiState.userName) }
            UserCenterCard {
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
            UserCenterCard {
                FeatureFlagCard(
                    flags = uiState.featureFlags,
                    onToggle = onToggleFeatureFlag,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            UserCenterCard {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onSaveClicked,
                        enabled = !uiState.isSaving,
                        shape = UserCenterShapes.Button,
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
                text = "管理个人信息与功能开关",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FeatureFlagCard(
    flags: Map<String, Boolean>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
                    Divider()
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

@Composable
private fun UserCenterCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, shape = UserCenterShapes.Card, clip = false),
        shape = UserCenterShapes.Card,
        border = BorderStroke(1.dp, UserCenterPalette.Border),
        colors = CardDefaults.cardColors(containerColor = UserCenterPalette.Card)
    ) {
        content()
    }
}
