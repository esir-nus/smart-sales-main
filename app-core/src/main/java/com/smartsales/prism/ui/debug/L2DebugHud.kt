package com.smartsales.prism.ui.debug

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.BuildConfig
import com.smartsales.prism.domain.unifiedpipeline.UnifiedPipeline
import com.smartsales.prism.domain.unifiedpipeline.PipelineInput
import com.smartsales.prism.domain.unifiedpipeline.PipelineResult
import com.smartsales.prism.domain.analyst.QueryQuality
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.scheduler.LintResult
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "L2DebugHud"

@EntryPoint
@InstallIn(SingletonComponent::class)
interface L2DebugHudEntryPoint {
    fun schedulerLinter(): SchedulerLinter
    fun unifiedPipeline(): UnifiedPipeline
    fun systemEventBus(): com.smartsales.prism.domain.system.SystemEventBus
}

/**
 * L2 Debug HUD — Pipeline 验证面板
 * 
 * 两种测试模式:
 * 1. 直接 Linter 测试 — 输入 JSON，验证 Linter 解析
 * 2. 全 Pipeline 测试 — 输入自然语言，走完整 UnifiedPipeline 流程，结果输出到 logcat
 * 
 * 使用方式:
 * - 打开 Debug HUD → 输入自然语言 → 点发送
 * - 查看 logcat: `adb logcat -s L2DebugHud:D`
 * 
 * @see .agent/rules/anti-drift-protocol.md §Three-Level Testing
 */
@Composable
fun L2DebugHud(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!BuildConfig.DEBUG) return
    
    val context = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, L2DebugHudEntryPoint::class.java)
    }
    val schedulerLinter = remember { entryPoint.schedulerLinter() }
    val unifiedPipeline = remember { entryPoint.unifiedPipeline() }
    val eventBus = remember { entryPoint.systemEventBus() }
    val scope = rememberCoroutineScope()
    
    var lastResult by remember { mutableStateOf<String?>(null) }
    var showToast by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    
    // Linter-only 测试场景 (直接 JSON 输入)
    val linterScenarios = remember {
        listOf(
            L2Scenario("Duration: Meeting", """{"title":"开会","startTime":"2026-02-03 14:00"}""", "duration=60"),
            L2Scenario("Reminder: Smart", """{"title":"开会","startTime":"2026-02-03 14:00","reminder":"smart"}""", "cascade=[-1h,-15m,-5m]"),
            L2Scenario("Reminder: Single", """{"title":"电话","startTime":"2026-02-03 14:00","reminder":"single"}""", "cascade=[-15m]"),
        )
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Debug 面板
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 100.dp, end = 16.dp)
                    .width(320.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xE6111827))
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🧪 L2 Pipeline Tests", color = Color.White, fontSize = 16.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = Color.White)
                    }
                }
                
                // ═══════════════════════════════════════════════════
                // Section 1: Full Pipeline Simulation (Natural Language Input)
                // ═══════════════════════════════════════════════════
                Text(
                    "📥 Full Pipeline (simulate hardware input)",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1F2937), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (inputText.isEmpty()) {
                                Text("明天下午2点部门会议", color = Color.Gray, fontSize = 14.sp)
                            }
                            innerTextField()
                        }
                    )
                    
                    IconButton(
                        onClick = {
                            val text = inputText.ifEmpty { "明天下午2点部门会议" }
                            Log.d(TAG, "════════════════════════════════════════")
                            Log.d(TAG, "🧪 L2 TEST: Full Pipeline Simulation")
                            Log.d(TAG, "📥 Input: $text")
                            Log.d(TAG, "════════════════════════════════════════")
                            
                            scope.launch {
                                isProcessing = true
                                try {
                                    val pipelineInput = PipelineInput(text, isVoice = false, intent = QueryQuality.CRM_TASK)
                                    var hasResult = false
                                    unifiedPipeline.processInput(pipelineInput).collect { pResult ->
                                        when (pResult) {
                                            is PipelineResult.SchedulerTaskCreated -> {
                                                Log.d(TAG, "✅ Task Created!")
                                                Log.d(TAG, "   title: ${pResult.title}")
                                                Log.d(TAG, "   dayOffset: ${pResult.dayOffset}")
                                                Log.d(TAG, "   taskId: ${pResult.taskId}")
                                                lastResult = "✅ Task: ${pResult.title}"
                                                showToast = true
                                                hasResult = true
                                            }
                                            is PipelineResult.ClarificationNeeded -> {
                                                Log.d(TAG, "⚠️ Clarification needed: ${pResult.question}")
                                                lastResult = "⚠️ ${pResult.question}"
                                                showToast = true
                                                hasResult = true
                                            }
                                            is PipelineResult.DisambiguationIntercepted -> {
                                                Log.d(TAG, "⚠️ Clarification needed: ${pResult.uiState}")
                                                lastResult = "⚠️ Disambiguation required"
                                                showToast = true
                                                hasResult = true
                                            }
                                            is PipelineResult.SchedulerMultiTaskCreated -> {
                                                Log.d(TAG, "✅ Tasks Created! ${pResult.tasks.size}")
                                                lastResult = "✅ ${pResult.tasks.size} Tasks Created"
                                                showToast = true
                                                hasResult = true
                                            }
                                            else -> {
                                                Log.d(TAG, "📦 State: $pResult")
                                            }
                                        }
                                    }
                                    if (!hasResult) {
                                        lastResult = "🚫 Missing concrete result"
                                        showToast = true
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "❌ Exception: ${e.message}", e)
                                    lastResult = "❌ ${e.message}"
                                    showToast = true
                                } finally {
                                    isProcessing = false
                                    Log.d(TAG, "════════════════════════════════════════")
                                }
                            }
                        },
                        enabled = !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Send, "Send", tint = Color(0xFF6366F1))
                        }
                    }
                }
                
                Text(
                    "💡 logcat: adb logcat -s L2DebugHud:D",
                    color = Color(0xFF4B5563),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Divider(
                    color = Color(0xFF374151),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // ═══════════════════════════════════════════════════
                // Section 1.5: System Events (Out Of Band)
                // ═══════════════════════════════════════════════════
                Text(
                    "🤖 System Events (Mascot Triggers)",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Button(
                    onClick = {
                        scope.launch {
                            eventBus.publish(com.smartsales.prism.domain.system.SystemEvent.AppIdle)
                            lastResult = "📡 Emitted AppIdle event"
                            showToast = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Text("Emit AppIdle (Trigger Mascot)", color = Color.White)
                }
                
                // ═══════════════════════════════════════════════════
                // Section 2: Direct Linter Tests (JSON Input)
                // ═══════════════════════════════════════════════════
                Text(
                    "🔬 Direct Linter (JSON bypass)",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(linterScenarios) { scenario ->
                        L2ScenarioButton(
                            scenario = scenario,
                            onClick = {
                                val result = schedulerLinter.lint(scenario.json)
                                val formatted = formatResult(scenario.name, result, scenario.expected)
                                Log.d(TAG, "🔬 Linter Test: ${scenario.name}\n$formatted")
                                lastResult = formatted
                                showToast = true
                            }
                        )
                    }
                }
            }
        }
        
        // Toast 显示结果
        AnimatedVisibility(
            visible = showToast && lastResult != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
        ) {
            LaunchedEffect(showToast) {
                delay(5000)
                showToast = false
            }
            
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xE6111827),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clickable { showToast = false }
            ) {
                Text(
                    text = lastResult ?: "",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun L2ScenarioButton(
    scenario: L2Scenario,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1F2937),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(scenario.name, color = Color.White, fontSize = 12.sp)
            Text(scenario.expected, color = Color(0xFF9CA3AF), fontSize = 10.sp)
        }
    }
}

private fun formatResult(name: String, result: LintResult, expected: String): String {
    val status = when (result) {
        is LintResult.Success -> "✅ duration=${result.task.durationMinutes}, cascade=${result.task.alarmCascade}"
        is LintResult.MultiTask -> "🔢 ${result.tasks.size} tasks: ${result.tasks.map { it.title }}"
        is LintResult.Incomplete -> "⚠️ ${result.missingField}: ${result.question}"
        is LintResult.Error -> "❌ ${result.message}"
        is LintResult.NonIntent -> "🚫 non_intent: ${result.reason}"
        is LintResult.Inspiration -> "💡 inspiration: ${result.content}"
        is LintResult.Deletion -> "🗑️ deletion: ${result.targetTitle}"
        is LintResult.Reschedule -> "🔄 reschedule: ${result.targetTitle} → ${result.newInstruction}"
    }
    return "[$name] $status (预期: $expected)"
}

private data class L2Scenario(
    val name: String,
    val json: String,
    val expected: String
)
