package com.smartsales.prism.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.domain.memory.UserProfile
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * User Center (Settings Blueprint)
 * @see prism-ui-ux-contract.md §1.7
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserCenterScreen(
    onClose: () -> Unit,
    viewModel: UserCenterViewModel = androidx.hilt.navigation.compose.hiltViewModel()
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
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("User Center", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1E1E2C) // Prism Dark
                    )
                )
            },
            containerColor = Color(0xFF1E1E2C)
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. Profile Card
                item {
                    profile?.let { userProfile ->
                        ProfileCard(
                            profile = userProfile,
                            onEdit = { isEditing = true }
                        )
                    }
                }

                // 2. Preferences
                item {
                    SettingsSection("Preferences") {
                        SettingsRowSelect("Theme", "System") { /* Mock */ }
                        SettingsRowToggle("AI Lab: Memory & Learning", true) { /* Mock */ }
                        SettingsRowToggle("Notifications & Pop-ups", true) { /* Mock */ }
                    }
                }

                // 3. Storage
                item {
                    SettingsSection("Storage") {
                        SettingsRowAction("Used: 120MB", "Clear Cache") { /* Mock */ }
                    }
                }

                // 4. Security
                item {
                    SettingsSection("Security") {
                        SettingsRowNav("Change Password") { /* Mock */ }
                        SettingsRowNav("Biometric") { /* Mock */ }
                        SettingsRowButton("Logout All Devices") { /* Mock */ }
                    }
                }

                // 5. Support
                item {
                    SettingsSection("Support") {
                        SettingsRowNav("Help Center") { /* Mock */ }
                        SettingsRowNav("Contact & Feedback") { /* Mock */ }
                    }
                }

                // 6. About
                item {
                    SettingsSection("About SmartSales") {
                        SettingsRowInfo("Version", "v1.0.0")
                        SettingsRowNav("Updates") { /* Mock */ }
                        SettingsRowNav("Privacy") { /* Mock */ }
                        SettingsRowNav("Licenses") { /* Mock */ }
                    }
                }

                // 7. Footer
                item {
                    Button(
                        onClick = { /* Mock Logout */ onClose() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Icon(Icons.Default.Logout, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Log Out")
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: UserProfile,
    onEdit: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B38)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(profile.role, color = Color.Gray, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Surface(color = Color(0xFF4CAF50).copy(alpha = 0.2f), shape = MaterialTheme.shapes.small) {
                    Text(profile.industry, color = Color(0xFF4CAF50), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
            // Edit
            TextButton(onClick = onEdit) {
                Text("Edit Profile")
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B38)),
            modifier = Modifier.fillMaxWidth()
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
        Text(label, color = Color.White)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, color = Color.Gray)
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
    Divider(color = Color.White.copy(alpha=0.05f))
}

@Composable
private fun SettingsRowToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
    Divider(color = Color.White.copy(alpha=0.05f))
}

@Composable
private fun SettingsRowNav(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White)
        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
    }
    Divider(color = Color.White.copy(alpha=0.05f))
}

@Composable
private fun SettingsRowAction(label: String, actionLabel: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White)
        OutlinedButton(onClick = onClick, modifier = Modifier.height(32.dp)) {
            Text(actionLabel)
        }
    }
    Divider(color = Color.White.copy(alpha=0.05f))
}

@Composable
private fun SettingsRowButton(label: String, onClick: () -> Unit) {
    Box(Modifier.clickable(onClick = onClick).fillMaxWidth().padding(16.dp)) {
        Text(label, color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsRowInfo(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White)
        Text(value, color = Color.Gray)
    }
    Divider(color = Color.White.copy(alpha=0.05f))
}
