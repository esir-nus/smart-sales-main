package com.smartsales.prism.ui.drawers.scheduler.dev

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.smartsales.prism.BuildConfig
import com.smartsales.prism.ui.drawers.scheduler.DevInjectSource
import com.smartsales.prism.ui.drawers.scheduler.ISchedulerViewModel
import com.smartsales.prism.ui.drawers.scheduler.LocalSchedulerDrawerVisuals

@Composable
fun SchedulerDevToolsPanel(
    viewModel: ISchedulerViewModel,
    isSimVisualMode: Boolean,
    currentDisplayedDateIso: String
) {
    if (!BuildConfig.ENABLE_SCHEDULER_DEV_TOOLS) {
        return
    }

    val visuals = LocalSchedulerDrawerVisuals.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    var scenarioMenuExpanded by remember { mutableStateOf(false) }
    var transcript by rememberSaveable { mutableStateOf("") }
    var selectedScenarioId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedScenario = remember(selectedScenarioId) {
        selectedScenarioId?.let(SchedulerTestScenarios::find)
    }
    val selectedDisplayedDateIso = remember(selectedScenario) {
        selectedScenario?.displayedDateOffset?.let { offset ->
            java.time.LocalDate.now().plusDays(offset.toLong()).toString()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = visuals.drawerContentHorizontalPadding, vertical = 8.dp),
        color = visuals.debugPanelColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Scheduler Dev Tools", color = visuals.taskTitleColor, fontSize = 14.sp)
                Text(text = if (expanded) "Hide" else "Show", color = visuals.taskContextColor, fontSize = 12.sp)
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { scenarioMenuExpanded = true },
                            shape = RoundedCornerShape(8.dp),
                            color = visuals.debugPanelButtonColor
                        ) {
                            Text(
                                text = selectedScenario?.let { "Scenario ${it.id}" } ?: "Choose Scenario",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                color = visuals.taskTitleColor
                            )
                        }
                        DropdownMenu(
                            expanded = scenarioMenuExpanded,
                            onDismissRequest = { scenarioMenuExpanded = false }
                        ) {
                            SchedulerTestScenarios.all.forEach { scenario ->
                                DropdownMenuItem(
                                    text = { Text("${scenario.id} · ${scenario.utterance}") },
                                    onClick = {
                                        selectedScenarioId = scenario.id
                                        transcript = scenario.utterance
                                        scenarioMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = transcript,
                        onValueChange = { transcript = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Inject transcript") },
                        supportingText = {
                            Text(
                                "displayedDateIso=${selectedDisplayedDateIso ?: currentDisplayedDateIso}",
                                color = visuals.taskContextColor
                            )
                        }
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = transcript.isNotBlank()) {
                                viewModel.injectTranscript(
                                    text = transcript.trim(),
                                    displayedDateIso = selectedDisplayedDateIso ?: currentDisplayedDateIso,
                                    source = DevInjectSource.DEV_PANEL
                                )
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = visuals.debugPanelButtonColor
                    ) {
                        Text(
                            text = "Send",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            color = visuals.taskTitleColor
                        )
                    }

                    if (!isSimVisualMode) {
                        SchedulerDebugRecordButton(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun SchedulerDebugRecordButton(
    viewModel: ISchedulerViewModel
) {
    val visuals = LocalSchedulerDrawerVisuals.current
    val devContext = LocalContext.current
    var isRecordingMic by remember { mutableStateOf(false) }
    val recorder = remember { com.smartsales.prism.data.audio.PhoneAudioRecorder(devContext) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && isRecordingMic) {
                recorder.cancel()
                isRecordingMic = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (isRecordingMic) {
                recorder.cancel()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            recorder.startRecording()
            isRecordingMic = true
        } else {
            Toast.makeText(devContext, "需要录音权限", Toast.LENGTH_SHORT).show()
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = visuals.debugPanelButtonColor,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isRecordingMic) visuals.debugPanelButtonActiveColor else visuals.debugPanelButtonColor,
                    RoundedCornerShape(10.dp)
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                devContext,
                                android.Manifest.permission.RECORD_AUDIO
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                            if (!hasPermission) {
                                permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                return@detectTapGestures
                            }

                            recorder.startRecording()
                            isRecordingMic = true

                            val released = tryAwaitRelease()
                            isRecordingMic = false
                            if (released) {
                                val wavFile = recorder.stopRecording()
                                viewModel.processAudio(wavFile)
                            } else {
                                recorder.cancel()
                            }
                        }
                    )
                }
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isRecordingMic) "松开结束测试录音..." else "REC 测试录音",
                color = visuals.taskTitleColor
            )
        }
    }
}
