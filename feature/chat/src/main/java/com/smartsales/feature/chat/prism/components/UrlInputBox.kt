package com.smartsales.feature.chat.prism.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 🚧 PRISM SKELETON (MOL-11)
 * Registry Status: ✅ Verified Placeholder
 *
 * THIS IS A PLACEHOLDER.
 * Intended for Phase 1.9 verification only.
 * Awaiting replacement in Phase 3 (Real Implementation).
 *
 * Registry Contract: `url: String`, `isFetching: Bool`, `onFetch: Fn`
 */
@Composable
fun UrlInputBox(
    url: String,
    isFetching: Boolean,
    onUrlChanged: (String) -> Unit,
    onFetch: () -> Unit
) {
    if (isFetching) {
        Text("Fetching URL...", color = Color.Gray, fontSize = 12.sp)
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF222233), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = url,
                onValueChange = onUrlChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter URL", fontSize = 12.sp) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
            Button(onClick = onFetch, enabled = url.isNotBlank()) {
                Text("Fetch")
            }
        }
    }
}
