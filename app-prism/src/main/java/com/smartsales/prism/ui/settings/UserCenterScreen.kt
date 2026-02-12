package com.smartsales.prism.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.domain.memory.UserProfile
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartsales.prism.ui.components.*
import com.smartsales.prism.ui.theme.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke

/**
 * User Center (Sleek Glass Version)
 * @see prism-ui-ux-contract.md §1.7
 * 
 * Updates:
 * - Replaced Scaffold with Full-Screen Glass Sheet
 * - Uses PrismCard for settings clusters
 * - Pro Max Aesthetic
 */
@Composable
fun UserCenterScreen(
    onClose: () -> Unit,
    viewModel: UserCenterViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsState()
    var isEditing by remember { mutableStateOf(false) }

    // Handle system back gesture
    androidx.activity.compose.BackHandler(onBack = {
        if (isEditing) isEditing = false else onClose()
    })

    if (isEditing && profile != null) {
        EditProfileScreen(
            profile = profile!!,
            onSave = { name, role, industry, exp, platform ->
                viewModel.updateProfile(name, role, industry, exp, platform)
                isEditing = false
            },
            onBack = { isEditing = false }
        )
    } else {
        // Main Glass Sheet Container
        // We use a high-alpha surface to mimic the sheet sliding in
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundApp) // Base Background
                .statusBarsPadding()
        ) {
             // --- Aurora Blobs (Decorative) ---
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 100.dp, y = (-50).dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x228B5CF6), Color.Transparent) // Violet
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = (-50).dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x2214B8A6), Color.Transparent) // Teal
                        )
                    )
            )

            Column(Modifier.fillMaxSize()) {
                // Glass Header (Tall & Scenic)
                PrismSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                    backgroundColor = BackgroundSurface.copy(alpha = 0.5f), // increased transparency
                    elevation = 0.dp // Flat glass
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        // Top Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                             IconButton(onClick = onClose) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = TextPrimary)
                            }
                            // Edit Button (Small)
                            PrismButton(
                                text = "编辑",
                                onClick = { isEditing = true },
                                style = PrismButtonStyle.GHOST,
                                modifier = Modifier.height(32.dp)
                            )
                        }
                        
                        Spacer(Modifier.height(24.dp))

                        // Profile Row
                        profile?.let { user ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Avatar
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(BackgroundSurfaceActive)
                                        .border(1.dp, BorderSubtle, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "FC", // Frank Chen
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                
                                Spacer(Modifier.width(16.dp))
                                
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = user.displayName,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        // PRO Badge
                                        Surface(
                                            color = AccentPrimary.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(4.dp),
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle)
                                        ) {
                                            Text(
                                                text = "PRO",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = AccentPrimary
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${user.role} • ${user.industry}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Settings Content
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // 1. Preferences
                    item {
                        SettingsSection("偏好设置") {
                            SettingsRowSelect("主题", "跟随系统") {}
                            SettingsRowToggle("AI 实验室", true) {}
                            // 通知开关 — 读取真实系统状态，点击打开系统通知设置
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val lifecycleOwner = LocalLifecycleOwner.current
                            var notificationsEnabled by remember { mutableStateOf(false) }
                            
                            // 监听生命周期 ON_RESUME，从系统设置返回时刷新状态
                            DisposableEffect(lifecycleOwner) {
                                val observer = LifecycleEventObserver { _, event ->
                                    if (event == Lifecycle.Event.ON_RESUME) {
                                        val manager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE)
                                            as android.app.NotificationManager
                                        notificationsEnabled = manager.areNotificationsEnabled()
                                    }
                                }
                                lifecycleOwner.lifecycle.addObserver(observer)
                                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                            }
                            
                            SettingsRowToggle("通知", notificationsEnabled) {
                                // 打开系统通知设置页
                                val intent = android.content.Intent().apply {
                                    action = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                context.startActivity(intent)
                            }
                        }
                    }

                    // 2. Storage
                    item {
                        SettingsSection("存储") {
                            SettingsRowAction("本地缓存", "清除 (128MB)") {}
                        }
                    }
                    
                    // 3. Security
                    item {
                        SettingsSection("安全") {
                            SettingsRowNav("修改密码") {}
                            SettingsRowNav("面容 ID") {}
                        }
                    }

                    // 4. About
                    item {
                        SettingsSection("关于") {
                            SettingsRowInfo("版本", "Prism v1.2 (Pro Max)")
                            SettingsRowNav("帮助中心") {}
                        }
                    }

                    // 5. Logout
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onClose() } // Mock Logout
                                .background(SurfaceDanger, RoundedCornerShape(16.dp))
                                .border(1.dp, AccentDanger.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.Logout, null, tint = AccentDanger, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("退出登录", color = AccentDanger, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(Modifier.height(48.dp))
                    }
                }
            }
        }
    }
}

// ... (Helper Components: ProfileCard, SettingsSection/Rows kept same as previous, just overwritten to ensure consistency)
@Composable
private fun ProfileCard(
    profile: UserProfile,
    onEdit: () -> Unit
) {
    PrismCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit // Make whole card clickable or just button? Let's make whole card for fluid UX
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(BackgroundSurfaceActive),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.width(20.dp))
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.displayName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text(profile.role, color = TextSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                // Glass Tag
                Surface(
                    color = AccentGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = profile.industry,
                        color = AccentGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            // Edit Chevron
            Icon(Icons.Default.ChevronRight, null, tint = TextTertiary)
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = title, 
            color = TextTertiary, 
            fontSize = 13.sp, 
            fontWeight = FontWeight.SemiBold, 
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        PrismCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = {} // Container only
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsRowSelect(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            Icon(Icons.Default.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(20.dp))
        }
    }
    Divider(color = BorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SettingsRowToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentBlue,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = BackgroundSurfaceActive,
                uncheckedBorderColor = BorderSubtle
            )
        )
    }
    Divider(color = BorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SettingsRowNav(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
        Icon(Icons.Default.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(20.dp))
    }
    Divider(color = BorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SettingsRowAction(label: String, actionLabel: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onClick, modifier = Modifier.height(32.dp)) {
            Text(actionLabel, color = AccentBlue)
        }
    }
    Divider(color = BorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SettingsRowButton(label: String, onClick: () -> Unit) {
    Box(Modifier.clickable(onClick = onClick).fillMaxWidth().padding(16.dp)) {
        Text(label, color = AccentBlue, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SettingsRowInfo(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
    Divider(color = BorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
}
