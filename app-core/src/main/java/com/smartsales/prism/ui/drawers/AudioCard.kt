package com.smartsales.prism.ui.drawers

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import com.smartsales.prism.ui.components.MarkdownText
import com.smartsales.prism.ui.components.PrismSurface
import com.smartsales.prism.ui.components.PrismButton
import com.smartsales.prism.ui.components.PrismButtonStyle
import com.smartsales.prism.ui.components.PrismSurfaceSubtle
import com.smartsales.prism.ui.drawers.audio.AudioViewModel
import com.smartsales.prism.ui.theme.*

/**
 * Audio Card Hub System (Sleek Glass Version)
 * Facade switching between Compact (List) and Expanded (Hub) views.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AudioCard(
    item: AudioItemState,
    isExpanded: Boolean,
    viewModel: AudioViewModel,
    onClick: () -> Unit,
    onStarClick: (String) -> Unit,
    onAskAi: (String) -> Unit,
    onTranscribe: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRename: (String) -> Unit
) {
    // 1. Shake Animation State
    val shakeAnim = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    // 2. Expansion Logic: Only expand if Transcribed
    val isExpandable = item.status == AudioStatus.TRANSCRIBED
    
    // 3. Swipe Logic (Only active when NOT expanded)
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.25f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { // L->R: Transcribe
                    if (item.status == AudioStatus.PENDING) {
                        onTranscribe(item.id)
                        false // Snap back after triggering action
                    } else {
                        false
                    }
                }
                SwipeToDismissBoxValue.EndToStart -> { // R->L: Delete
                    onDelete(item.id)
                    false 
                }
                else -> false
            }
        }
    )
    
    // Swipe Wrapper
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !isExpanded && item.status == AudioStatus.PENDING,
        enableDismissFromEndToStart = !isExpanded,
        backgroundContent = {
           val direction = dismissState.dismissDirection
           // Glass colors for actions
           val color = when (direction) {
               SwipeToDismissBoxValue.StartToEnd -> AccentSecondary.copy(alpha = 0.1f) // Green Tint
               SwipeToDismissBoxValue.EndToStart -> AccentDanger.copy(alpha = 0.1f) // Red Tint
               else -> Color.Transparent
           }
           val alignment = when (direction) {
               SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
               SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
               else -> Alignment.Center
           }
           val icon = when (direction) {
               SwipeToDismissBoxValue.StartToEnd -> Icons.Outlined.AutoAwesome
               SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
               else -> Icons.Default.Delete
           }
           val tint = when (direction) {
               SwipeToDismissBoxValue.StartToEnd -> AccentSecondary
               SwipeToDismissBoxValue.EndToStart -> AccentDanger
               else -> Color.Transparent
           }
           
           Box(
               modifier = Modifier
                   .fillMaxSize()
                   .background(color, RoundedCornerShape(20.dp)) // Match GlassCardShape
                   .padding(horizontal = 20.dp),
               contentAlignment = alignment
           ) {
               Icon(
                   imageVector = icon,
                   contentDescription = null,
                   tint = tint
               )
           }
        },
        content = {
            // Shared Card Container -> Using PrismSurface manually to add combinedClickable
            Box(
                 modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = shakeAnim.value.dp)
                    .shadow(
                        elevation = if (isExpanded) 12.dp else 4.dp,
                        shape = GlassCardShape,
                        spotColor = Color.Black.copy(alpha = 0.05f)
                    )
                    .clip(GlassCardShape)
                    .background(if (isExpanded) BackgroundSurfaceHover else BackgroundSurface)
                    // Border
                    .then(Modifier.border(0.5.dp, BorderSubtle, GlassCardShape))
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                    .combinedClickable(
                        onClick = {
                            if (isExpandable) {
                                onClick()
                            } else {
                                // Trigger Shake (Reject)
                                scope.launch {
                                    shakeAnim.snapTo(0f)
                                    shakeAnim.animateTo(0f, androidx.compose.animation.core.keyframes {
                                        durationMillis = 400
                                        0f at 0
                                        (-10f) at 50
                                        10f at 100
                                        (-10f) at 150
                                        5f at 200
                                        0f at 250
                                    })
                                }
                            }
                        },
                        onLongClick = { onRename(item.id) }
                    )
            ) {
                if (isExpanded) {
                    ExpandedAudioHub(item, viewModel, onAskAi, onClick)
                } else {
                    CompactAudioCard(item, onStarClick)
                }
            }
        }
    )
}

/**
 * Compact View (List Item)
 */
@Composable
private fun CompactAudioCard(
    item: AudioItemState,
    onStarClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Row 1: Icon, Filename, Time
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AudioIcon(item = item, onClick = { onStarClick(item.id) })
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.filename,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = item.timeDisplay,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Row 2: Status-Based Content
        when (item.status) {
            AudioStatus.PENDING -> ShimmerSwipePrompt()
            AudioStatus.TRANSCRIBING -> TranscribingProgressBar(item.progress ?: 0f)
            AudioStatus.TRANSCRIBED -> {
                Text(
                    text = item.summary ?: "无摘要內容...",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 52.dp)
                )
            }
        }
    }
}

@Composable
private fun ShimmerSwipePrompt() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        modifier = Modifier.padding(start = 52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "右滑开始转写 >>>",
            color = AccentBlue.copy(alpha = alpha),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TranscribingProgressBar(progress: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 52.dp, end = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "正在转写...",
                color = AccentSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (progress > 0f) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { if (progress > 0f) progress else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = AccentSecondary,
            trackColor = AccentSecondary.copy(alpha = 0.1f)
        )
    }
}

/**
 * Expanded View (The Hub)
 */
@Composable
private fun ExpandedAudioHub(
    item: AudioItemState,
    viewModel: AudioViewModel,
    onAskAi: (String) -> Unit,
    onCollapse: () -> Unit
) {
    var artifacts by remember { mutableStateOf<TingwuJobArtifacts?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val aiInsightsMarkdown = remember(artifacts) {
        val maRaw = artifacts?.meetingAssistanceRaw
        var insights: String? = null
        if (maRaw != null) {
            try {
                val root = kotlinx.serialization.json.Json.parseToJsonElement(maRaw) as kotlinx.serialization.json.JsonObject
                val ma = root["MeetingAssistance"] as? kotlinx.serialization.json.JsonObject ?: root
                val keywords = (ma["Keywords"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { 
                    (it as? kotlinx.serialization.json.JsonPrimitive)?.content 
                }
                val classesObj = ma["Classifications"] as? kotlinx.serialization.json.JsonObject
                val classes = classesObj?.entries?.mapNotNull { (k, vElement) ->
                    val v = (vElement as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull()
                    if (v != null) k to v else null
                }
                
                val actionsArray = ma["Actions"] as? kotlinx.serialization.json.JsonArray
                val actions = actionsArray?.mapNotNull { element ->
                     if (element is kotlinx.serialization.json.JsonPrimitive) {
                         element.content
                     } else if (element is kotlinx.serialization.json.JsonObject) {
                         element["Text"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null }
                     } else {
                         null
                     }
                }

                val keySentencesArray = ma["KeySentences"] as? kotlinx.serialization.json.JsonArray
                val keySentences = keySentencesArray?.mapNotNull { element ->
                     (element as? kotlinx.serialization.json.JsonObject)?.get("Text")?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null }
                }
                
                val sb = java.lang.StringBuilder()
                if (!actions.isNullOrEmpty()) {
                    sb.append("**待办事项 (Actions)**\n")
                    actions.forEach { actionText ->
                        sb.append("- $actionText\n")
                    }
                    sb.append("\n")
                }
                if (!keywords.isNullOrEmpty()) {
                    sb.append("**核心关键词 (Keywords)**\n")
                    sb.append(keywords.joinToString(" • "))
                    sb.append("\n\n")
                }
                if (!keySentences.isNullOrEmpty()) {
                    sb.append("**重点内容 (Key Sentences)**\n")
                    keySentences.forEach { sentence ->
                        sb.append("- $sentence\n")
                    }
                    sb.append("\n")
                }
                if (!classes.isNullOrEmpty()) {
                    sb.append("**场景分类 (Classifications)**\n")
                    classes.sortedByDescending { it.second }.forEach { (k, v) ->
                        sb.append("- $k: ${(v * 100).toInt()}%\n")
                    }
                }
                insights = sb.toString().trim()
            } catch(e: Exception) {}
        }
        val oldHighlights = artifacts?.smartSummary?.keyPoints?.takeIf { it.isNotEmpty() }?.joinToString("\n") { "• $it" }
        if (oldHighlights != null) {
            insights = (insights?.plus("\n\n") ?: "") + "**摘要重点 (Key Points)**\n" + oldHighlights
        }
        insights
    }

    LaunchedEffect(item.status) {
        if (item.status == AudioStatus.TRANSCRIBED) {
            isLoading = true
            artifacts = viewModel.getArtifacts(item.id)
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        
        // 1. Collapse Handle (Visual Cue)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCollapse() }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
             Icon(
                 imageVector = Icons.Default.ExpandLess,
                 contentDescription = "Collapse",
                 tint = TextTertiary
             )
        }

        // 2. Player Header (Pinned Top) -> Sleek Subtle Surface
        PrismSurfaceSubtle(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = BackgroundSurfaceMuted
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                val spectrumUrl = artifacts?.outputSpectrumPath
                if (spectrumUrl != null) {
                    AsyncImage(
                        model = spectrumUrl,
                        contentDescription = "Audio Spectrum",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.5f)) // Dim background for tinting
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp).fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = AccentBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "00:12 / ${item.timeDisplay}",
                        fontSize = 12.sp,
                        color = if (spectrumUrl != null) Color.White else TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (spectrumUrl == null) {
                        // Fake Waveform if no real wave available
                        Text(
                            text = "|||||·|||||||·||||",
                            fontSize = 10.sp,
                            color = TextTertiary,
                            letterSpacing = 2.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.VolumeUp,
                        contentDescription = null,
                        tint = if (spectrumUrl != null) Color.White else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Portal Button (Ask AI) -> PrismButton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            PrismButton(
                text = "问AI (Ask AI)",
                onClick = {
                    onAskAi(item.id)
                },
                modifier = Modifier.fillMaxWidth(),
                style = PrismButtonStyle.SOLID,
                leadingIcon = {
                     Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Accordions
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            val transcriptContent = artifacts?.let { "transcript" } // Signal content is ready
            AudioCardAccordion(
                title = "转写 (Transcription)",
                icon = "TR",
                markdownContent = artifacts?.transcriptMarkdown,
                status = item.status,
                initiallyExpanded = true
            )
            Divider(color = BorderSubtle)
            
            AudioCardAccordion(
                title = "摘要 (Summary)",
                icon = "SU",
                textContent = artifacts?.smartSummary?.summary,
                status = item.status,
                initiallyExpanded = false
            )
            Divider(color = BorderSubtle)
            
            val chaptersOutput = artifacts?.chapters?.takeIf { it.isNotEmpty() }?.joinToString("\n") { 
                
                val totalSeconds = (it.startMs / 1000).toInt()
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60
                val timeStr = if (hours > 0) {
                    "%02d:%02d:%02d".format(hours, minutes, seconds)
                } else {
                    "%02d:%02d".format(minutes, seconds)
                }
                
                "- ${it.title} ($timeStr)" 
            }
            AudioCardAccordion(
                title = "章节 (Chapters)",
                icon = "CH",
                textContent = chaptersOutput ?: "无章节内容",
                status = item.status,
                initiallyExpanded = false,
                disableStreaming = true
            )
            Divider(color = BorderSubtle)

            AudioCardAccordion(
                title = "AI 洞察 (AI Insights)",
                icon = "AI",
                markdownContent = aiInsightsMarkdown?.ifBlank { null } ?: "正在分析或无洞察内容...",
                status = item.status,
                initiallyExpanded = false,
                disableStreaming = true
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AudioCardAccordion(
    title: String,
    icon: String,
    textContent: String? = null,
    markdownContent: String? = null,
    status: AudioStatus,
    initiallyExpanded: Boolean = false,
    disableStreaming: Boolean = false
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    // Auto-folding logic: When transcribing finishes, transcription auto-expands
    LaunchedEffect(status) {
        if (status == AudioStatus.TRANSCRIBED && title == "转写 (Transcription)") {
            expanded = true
        } else if (status == AudioStatus.TRANSCRIBED) {
            expanded = false // Keep others closed initially
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AccentBlue.copy(alpha = 0.12f)
            ) {
                Text(
                    text = icon,
                    fontSize = 10.sp,
                    color = AccentBlue,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = TextTertiary
            )
        }
        
        AnimatedVisibility(visible = expanded) {
            val hasMeaningfulContent = !textContent.isNullOrBlank() || !markdownContent.isNullOrBlank()
            if (status == AudioStatus.TRANSCRIBING || (!hasMeaningfulContent && status == AudioStatus.TRANSCRIBED)) {
                // Shimmer Buffer animation
                Box(modifier = Modifier.padding(bottom = 12.dp, start = 24.dp)) {
                    ShimmeringBuffer()
                }
            } else {
                // Final Text
                val streamContent = textContent ?: markdownContent ?: ""
                var displayedText by remember(streamContent) { mutableStateOf("") }
                
                LaunchedEffect(streamContent) {
                    if (disableStreaming) {
                        displayedText = streamContent
                    } else if (displayedText.length < streamContent.length) {
                        while (displayedText.length < streamContent.length) {
                            val chunkSize = 3
                            val remaining = streamContent.length - displayedText.length
                            val take = minOf(chunkSize, remaining)
                            displayedText += streamContent.substring(displayedText.length, displayedText.length + take)
                            kotlinx.coroutines.delay(10) // Chunked streaming to reduce recomposition pressure
                        }
                    } else {
                        displayedText = streamContent
                    }
                }

                val renderContent = if (displayedText.length < streamContent.length) "$displayedText ▋" else displayedText

                Box(modifier = Modifier.padding(bottom = 12.dp, start = 24.dp)) {
                    if (markdownContent != null) {
                        MarkdownText(
                            text = renderContent,
                            color = TextSecondary,
                            lineHeight = 22.sp
                        )
                    } else {
                        Text(
                            text = renderContent.ifBlank { "无对应内容" },
                            fontSize = 14.sp,
                            color = TextSecondary,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmeringBuffer(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    Box(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .height(16.dp)
            .background(BorderSubtle.copy(alpha = alpha), RoundedCornerShape(4.dp))
    )
}

@Composable
private fun AudioIcon(item: AudioItemState, onClick: () -> Unit) {
    val bgColor = if (item.isStarred) AccentBlue.copy(alpha = 0.1f) else BackgroundSurfaceMuted
    val iconTint = if (item.isStarred) AccentBlue else TextMuted
    
    Box(
        modifier = Modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        // 1. Star Circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(bgColor, CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Star, 
                contentDescription = if (item.isStarred) "Starred" else "Unstarred",
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
        
        // 2. Source Badge
        val sourceIcon = when (item.source) {
            AudioSource.PHONE -> Icons.Outlined.Smartphone
            AudioSource.SMARTBADGE -> Icons.Outlined.Cloud
        }
        
        Icon(
            imageVector = sourceIcon,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier
                .size(14.dp)
                .align(Alignment.BottomEnd)
                .offset(x = (-4).dp, y = (4).dp)
        )
    }
}
