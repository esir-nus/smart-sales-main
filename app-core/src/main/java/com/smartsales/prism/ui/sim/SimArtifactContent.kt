package com.smartsales.prism.ui.sim

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import com.smartsales.prism.ui.components.MarkdownText
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private val simArtifactJson = Json { ignoreUnknownKeys = true }

data class SimTranscriptPresentation(
    val enableInitialReveal: Boolean = false,
    val startCollapsed: Boolean = false,
    val collapseAfterRenderedLines: Int = 4,
    val minRevealMillis: Long = 0L
)

@Composable
fun SimArtifactBubble(
    title: String,
    artifactsJson: String,
    transcriptPresentation: SimTranscriptPresentation = SimTranscriptPresentation(),
    onTranscriptRevealConsumed: (isLongTranscript: Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val artifacts = remember(artifactsJson) {
        runCatching { simArtifactJson.decodeFromString<TingwuJobArtifacts>(artifactsJson) }.getOrNull()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SimConversationSurfaceTokens.Surface)
            .border(1.dp, SimConversationSurfaceTokens.Border, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 15.dp)
    ) {
        Text(
            text = "《$title》转写结果",
            color = SimConversationSurfaceTokens.Title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
        if (artifacts == null) {
            Text(
                text = "当前无法读取该音频的结构化结果，请稍后重试。",
                color = SimConversationSurfaceTokens.BodyMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        } else {
            SimArtifactContent(
                artifacts = artifacts,
                transcriptPresentation = transcriptPresentation,
                onTranscriptRevealConsumed = onTranscriptRevealConsumed,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
fun SimArtifactContent(
    artifacts: TingwuJobArtifacts,
    transcriptPresentation: SimTranscriptPresentation = SimTranscriptPresentation(),
    onTranscriptRevealConsumed: (isLongTranscript: Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val transcript = artifacts.transcriptMarkdown?.takeIf { it.isNotBlank() }
    val summary = artifacts.smartSummary?.summary?.takeIf { it.isNotBlank() }
    val highlights = artifacts.smartSummary?.keyPoints?.takeIf { it.isNotEmpty() }
        ?.joinToString("\n") { "• $it" }
    val chapters = artifacts.chapters?.takeIf { it.isNotEmpty() }
        ?.joinToString("\n\n") { chapter ->
            buildString {
                append(formatSimChapterTime(chapter.startMs))
                append("  ")
                append(chapter.title)
                chapter.summary?.takeIf { it.isNotBlank() }?.let {
                    append("\n")
                    append(it)
                }
            }
        }
    val speakers = buildSimSpeakerSection(artifacts)
    val providerAdjacent = buildSimProviderAdjacentSection(artifacts)
    val links = artifacts.resultLinks.takeIf { it.isNotEmpty() }
        ?.joinToString("\n") { "- ${it.label}: ${it.url}" }

    Column(modifier = modifier.fillMaxWidth()) {
        transcript?.let {
            SimTranscriptSection(
                text = it,
                presentation = transcriptPresentation,
                onRevealConsumed = onTranscriptRevealConsumed
            )
        }
        summary?.let {
            HorizontalDivider(color = SimConversationSurfaceTokens.Divider)
            SimArtifactSection(title = "摘要", text = it, useMarkdown = true)
        }
        highlights?.let {
            HorizontalDivider(color = SimConversationSurfaceTokens.Divider)
            SimArtifactSection(title = "重点", text = it)
        }
        chapters?.let {
            HorizontalDivider(color = SimConversationSurfaceTokens.Divider)
            SimArtifactSection(title = "章节", text = it)
        }
        speakers?.let {
            HorizontalDivider(color = SimConversationSurfaceTokens.Divider)
            SimArtifactSection(title = "说话人", text = it)
        }
        providerAdjacent?.let {
            HorizontalDivider(color = SimConversationSurfaceTokens.Divider)
            SimArtifactSection(title = "附加结果", text = it)
        }
        links?.let {
            HorizontalDivider(color = SimConversationSurfaceTokens.Divider)
            SimArtifactSection(title = "结果链接", text = it)
        }
    }
}

@Composable
private fun SimTranscriptSection(
    text: String,
    presentation: SimTranscriptPresentation,
    onRevealConsumed: (isLongTranscript: Boolean) -> Unit
) {
    var expanded by remember(text, presentation.startCollapsed) {
        mutableStateOf(!presentation.startCollapsed)
    }
    var displayedText by remember(text, presentation.enableInitialReveal) {
        mutableStateOf(if (presentation.enableInitialReveal) "" else text)
    }
    var streamActive by remember(text, presentation.enableInitialReveal) {
        mutableStateOf(presentation.enableInitialReveal)
    }
    var renderedLineCount by remember(text) { mutableStateOf(0) }
    var longTranscriptDetected by remember(text) { mutableStateOf(false) }
    var revealStartedAtMillis by remember(text, presentation.enableInitialReveal) {
        mutableStateOf(if (presentation.enableInitialReveal) SystemClock.uptimeMillis() else 0L)
    }
    var revealReported by remember(text) { mutableStateOf(false) }

    fun consumeReveal(isLongTranscript: Boolean) {
        if (revealReported) return
        revealReported = true
        onRevealConsumed(isLongTranscript)
    }

    LaunchedEffect(text, streamActive) {
        if (!streamActive) {
            displayedText = text
            return@LaunchedEffect
        }

        displayedText = ""
        while (displayedText.length < text.length && streamActive) {
            val next = minOf(displayedText.length + 8, text.length)
            displayedText = text.substring(0, next)
            kotlinx.coroutines.delay(14)
        }

        if (streamActive) {
            if (longTranscriptDetected) {
                displayedText = text
            } else {
                streamActive = false
                consumeReveal(isLongTranscript = false)
            }
        }
    }

    LaunchedEffect(renderedLineCount, streamActive) {
        if (!streamActive) return@LaunchedEffect
        if (hasExceededTranscriptCollapseThreshold(
                renderedLineCount = renderedLineCount,
                collapseAfterRenderedLines = presentation.collapseAfterRenderedLines
            )
        ) {
            longTranscriptDetected = true
        }
    }

    LaunchedEffect(streamActive, longTranscriptDetected, revealStartedAtMillis, presentation.minRevealMillis) {
        if (!streamActive || !longTranscriptDetected) return@LaunchedEffect
        val remainingDelayMillis = remainingTranscriptRevealDwellMillis(
            revealStartedAtMillis = revealStartedAtMillis,
            nowMillis = SystemClock.uptimeMillis(),
            minRevealMillis = presentation.minRevealMillis
        )
        if (remainingDelayMillis > 0L) {
            kotlinx.coroutines.delay(remainingDelayMillis)
        }
        if (streamActive && longTranscriptDetected) {
            expanded = false
            streamActive = false
            consumeReveal(isLongTranscript = true)
        }
    }

    SimArtifactSection(
        title = "转写",
        text = displayedText.ifBlank { text },
        initiallyExpanded = expanded,
        useMarkdown = true,
        onExpandedChange = { expanded = it },
        onTextLayout = { result ->
            if (streamActive) {
                renderedLineCount = result.lineCount
            }
        }
    )
}

internal fun hasExceededTranscriptCollapseThreshold(
    renderedLineCount: Int,
    collapseAfterRenderedLines: Int
): Boolean = renderedLineCount > collapseAfterRenderedLines

internal fun remainingTranscriptRevealDwellMillis(
    revealStartedAtMillis: Long,
    nowMillis: Long,
    minRevealMillis: Long
): Long = (minRevealMillis - (nowMillis - revealStartedAtMillis)).coerceAtLeast(0L)

@Composable
private fun SimArtifactSection(
    title: String,
    text: String,
    initiallyExpanded: Boolean = false,
    useMarkdown: Boolean = false,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    onTextLayout: ((androidx.compose.ui.text.TextLayoutResult) -> Unit)? = null
) {
    var expanded by remember(title) { mutableStateOf(initiallyExpanded) }

    LaunchedEffect(initiallyExpanded) {
        expanded = initiallyExpanded
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    expanded = !expanded
                    onExpandedChange?.invoke(expanded)
                }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = SimConversationSurfaceTokens.Title,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = SimConversationSurfaceTokens.Icon
            )
        }

        AnimatedVisibility(visible = expanded) {
            Box(modifier = Modifier.padding(bottom = 10.dp)) {
                if (useMarkdown) {
                    MarkdownText(
                        text = text,
                        color = SimConversationSurfaceTokens.BodyMuted,
                        lineHeight = 20.sp,
                        onTextLayout = { result -> onTextLayout?.invoke(result) }
                    )
                } else {
                    Text(
                        text = text,
                        color = SimConversationSurfaceTokens.BodyMuted,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        onTextLayout = { result -> onTextLayout?.invoke(result) }
                    )
                }
            }
        }
    }
}

private fun formatSimChapterTime(startMs: Long): String {
    val totalSeconds = (startMs / 1000).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private fun buildSimSpeakerSection(artifacts: TingwuJobArtifacts): String? {
    if (artifacts.speakerLabels.isNotEmpty()) {
        return artifacts.speakerLabels.entries.joinToString("\n") { (speakerId, label) ->
            "- ${label.ifBlank { speakerId }} (${speakerId})"
        }
    }

    val diarized = artifacts.diarizedSegments?.takeIf { it.isNotEmpty() } ?: return null
    return diarized
        .groupBy { it.speakerId ?: "speaker_${it.speakerIndex}" }
        .entries
        .joinToString("\n") { (speakerId, segments) ->
            "- $speakerId: ${segments.size} 段"
        }
}

private fun buildSimProviderAdjacentSection(artifacts: TingwuJobArtifacts): String? {
    val raw = artifacts.meetingAssistanceRaw ?: return null

    return runCatching {
        val root = Json.parseToJsonElement(raw) as? JsonObject ?: return null
        val meetingAssistance = root["MeetingAssistance"] as? JsonObject ?: root

        val keywords = (meetingAssistance["Keywords"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            ?.takeIf { it.isNotEmpty() }

        val actions = (meetingAssistance["Actions"] as? JsonArray)
            ?.mapNotNull { element ->
                when (element) {
                    is JsonPrimitive -> element.contentOrNull
                    is JsonObject -> (element["Text"] as? JsonPrimitive)?.contentOrNull
                    else -> null
                }
            }
            ?.takeIf { it.isNotEmpty() }

        val keySentences = (meetingAssistance["KeySentences"] as? JsonArray)
            ?.mapNotNull { element ->
                (element as? JsonObject)?.get("Text")
                    ?.let { it as? JsonPrimitive }
                    ?.contentOrNull
            }
            ?.takeIf { it.isNotEmpty() }

        val classifications = (meetingAssistance["Classifications"] as? JsonObject)
            ?.entries
            ?.mapNotNull { (label, value) ->
                val score = (value as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull() ?: return@mapNotNull null
                "$label: ${(score * 100).toInt()}%"
            }
            ?.takeIf { it.isNotEmpty() }

        buildString {
            actions?.let {
                appendLine("待办事项")
                it.forEach { action -> appendLine("- $action") }
                appendLine()
            }
            keywords?.let {
                appendLine("关键词")
                appendLine(it.joinToString(" • "))
                appendLine()
            }
            keySentences?.let {
                appendLine("重点内容")
                it.forEach { sentence -> appendLine("- $sentence") }
                appendLine()
            }
            classifications?.let {
                appendLine("分类")
                it.forEach { item -> appendLine("- $item") }
            }
        }.trim().ifBlank { null }
    }.getOrNull()
}

private val JsonPrimitive.contentOrNull: String?
    get() = runCatching { content }.getOrNull()
