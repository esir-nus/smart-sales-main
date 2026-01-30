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
                title = { Text("Edit Profile", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        onSave(displayName, role, industry, experience, platform)
                        onBack()
                    }) {
                        Text("Save", color = Color(0xFF4CAF50))
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
                Text("Professional Info", color = Color.Gray, style = MaterialTheme.typography.labelLarge)
            }
            item {
                PrismTextField(label = "Display Name", value = displayName, onValueChange = { displayName = it })
            }
            item {
                PrismTextField(label = "Role / Position", value = role, onValueChange = { role = it })
            }
            item {
                PrismTextField(label = "Industry", value = industry, onValueChange = { industry = it })
            }
            item {
                PrismTextField(
                    label = "Working Experience",
                    value = experience,
                    onValueChange = { experience = it },
                    placeholder = "e.g., 5-10 years"
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text("Preferences", color = Color.Gray, style = MaterialTheme.typography.labelLarge)
            }
            
            item {
                PrismTextField(
                    label = "Communication Platform",
                    value = platform,
                    onValueChange = { platform = it },
                    helperText = "Used for contact suggestions (e.g., WeChat)"
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
