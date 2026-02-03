package com.smartsales.prism.ui.debug

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.BuildConfig
import com.smartsales.prism.domain.scheduler.LintResult
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay

@EntryPoint
@InstallIn(SingletonComponent::class)
interface L2DebugHudEntryPoint {
    fun schedulerLinter(): SchedulerLinter
}

/**
 * L2 Debug HUD — 独立于业务逻辑的 Pipeline 验证面板
 * 
 * 直接调用 SchedulerLinter.lint() 并展示结果，不经过 ViewModel/UX 层
 * 避免 UX 层 bug 影响测试结果
 * 
 * @param isVisible 由外部控制是否显示（header 的 bug 按钮切换）
 * @param onDismiss 关闭回调
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
    
    var lastResult by remember { mutableStateOf<String?>(null) }
    var showToast by remember { mutableStateOf(false) }
    
    // L2 测试场景
    val scenarios = remember {
        listOf(
            L2Scenario("Duration: EndTime Diff", """{"title":"测试","startTime":"2026-02-03 14:00","endTime":"2026-02-03 15:30"}""", "预期: Success, duration=90"),
            L2Scenario("Duration: Meeting Infer", """{"title":"开会","startTime":"2026-02-03 14:00"}""", "预期: Success, duration=60"),
            L2Scenario("Duration: Call Infer", """{"title":"电话","startTime":"2026-02-03 14:00"}""", "预期: Success, duration=15"),
            L2Scenario("Duration: Explicit", """{"title":"测试","startTime":"2026-02-03 14:00","duration":"45m"}""", "预期: Success, duration=45"),
            L2Scenario("Duration: Personal Default", """{"title":"其他事情","startTime":"2026-02-03 14:00"}""", "预期: Success, duration=60 (PERSONAL)"),
            L2Scenario("StartTime: Missing", """{"title":"开会"}""", "预期: Incomplete, question包含'什么时候'"),
            L2Scenario("Reminder: Smart", """{"title":"开会","startTime":"2026-02-03 14:00","reminder":"smart"}""", "预期: SMART_CASCADE, cascade=[-1h,-15m,-5m]"),
            L2Scenario("Reminder: Single", """{"title":"电话","startTime":"2026-02-03 14:00","reminder":"single"}""", "预期: SINGLE, cascade=[-15m]"),
        )
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Debug 面板 (从右侧滑入)
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 100.dp, end = 16.dp)
                    .width(300.dp)
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
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Scenarios
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(scenarios) { scenario ->
                        L2ScenarioButton(
                            scenario = scenario,
                            onClick = {
                                val result = schedulerLinter.lint(scenario.json)
                                lastResult = formatResult(scenario.name, result, scenario.expected)
                                showToast = true
                            }
                        )
                    }
                }
            }
        }
        
        // Debug Toast (顶部显示结果)
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
        Column(modifier = Modifier.padding(12.dp)) {
            Text(scenario.name, color = Color.White, fontSize = 13.sp)
            Text(scenario.expected, color = Color(0xFF9CA3AF), fontSize = 11.sp)
        }
    }
}

private fun formatResult(name: String, result: LintResult, expected: String): String {
    val status = when (result) {
        is LintResult.Success -> "✅ Success\nduration=${result.task.durationMinutes}\nreminder=${result.reminderType}\ncascade=${result.task.alarmCascade}"
        is LintResult.Incomplete -> "⚠️ Incomplete\nfield=${result.missingField}\nquestion=${result.question}"
        is LintResult.Error -> "❌ Error\nmessage=${result.message}"
    }
    return "【$name】\n$status\n\n预期: $expected"
}

private data class L2Scenario(
    val name: String,
    val json: String,
    val expected: String
)
