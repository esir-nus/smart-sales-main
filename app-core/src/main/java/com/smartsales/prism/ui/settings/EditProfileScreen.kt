package com.smartsales.prism.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.smartsales.prism.domain.memory.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    profile: UserProfile,
    onSave: (String, String, String, String, String) -> Unit,
    onBack: () -> Unit
) {
    var displayName by remember { mutableStateOf(profile.displayName) }
    var role by remember { mutableStateOf(profile.role) }
    var industry by remember { mutableStateOf(profile.industry) }
    var experience by remember { mutableStateOf(profile.experienceYears) }
    var platform by remember { mutableStateOf(profile.communicationPlatform) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑资料", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        onSave(displayName, role, industry, experience, platform)
                        onBack()
                    }) {
                        Text("保存", color = Color(0xFF4CAF50))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E2C)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("职业信息", color = Color.Gray, style = MaterialTheme.typography.labelLarge)
            }
            item {
                PrismTextField(label = "显示名称", value = displayName, onValueChange = { displayName = it })
            }
            item {
                PrismTextField(label = "职位 / 角色", value = role, onValueChange = { role = it })
            }
            item {
                PrismTextField(label = "行业", value = industry, onValueChange = { industry = it })
            }
            item {
                PrismTextField(
                    label = "工作经验",
                    value = experience,
                    onValueChange = { experience = it },
                    placeholder = "例如：5-10年"
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text("偏好设置", color = Color.Gray, style = MaterialTheme.typography.labelLarge)
            }
            
            item {
                PrismTextField(
                    label = "沟通平台",
                    value = platform,
                    onValueChange = { platform = it },
                    helperText = "用于联系建议（例如：微信）"
                )
            }
        }
    }
}

@Composable
private fun PrismTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String? = null,
    helperText: String? = null
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, color = Color.Gray) },
            placeholder = if (placeholder != null) { { Text(placeholder, color = Color.DarkGray) } } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF2B2B38),
                unfocusedContainerColor = Color(0xFF2B2B38),
                focusedBorderColor = Color(0xFF4CAF50),
                unfocusedBorderColor = Color.Gray
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        if (helperText != null) {
            Text(
                text = helperText,
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
