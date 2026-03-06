package com.smartsales.prism.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import com.smartsales.prism.ui.theme.*

/**
 * Session Item (Sleek Glass Version)
 * Clean, light-theme compatible row.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SessionItem(
    clientName: String,
    summary: String,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        // Client Name - Primary Text
        Text(
            text = clientName,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp
        )
        // Separator
        Text(
            text = " · ",
            color = TextTertiary,
            fontSize = 14.sp
        )
        // Summary - Muted Text
        Text(
            text = summary.take(10),
            color = TextSecondary,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
