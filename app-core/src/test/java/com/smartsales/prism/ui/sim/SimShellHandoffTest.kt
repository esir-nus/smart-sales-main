package com.smartsales.prism.ui.sim

import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.prism.domain.audio.PipelineEvent
import com.smartsales.prism.domain.audio.SchedulerResult
import com.smartsales.prism.domain.model.SchedulerFollowUpTaskSummary
import com.smartsales.prism.domain.memory.ConflictPolicy
import com.smartsales.prism.domain.memory.DurationSource
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import com.smartsales.prism.ui.components.DynamicIslandTapAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SimShellHandoffTest {

    @Test
    fun `sim shell chrome hosted scheduler gesture only opens when shell is clear`() {
        assertTrue(canOpenSimSchedulerFromEdge(RuntimeShellState()))
        assertFalse(canOpenSimSchedulerFromEdge(RuntimeShellState(activeDrawer = RuntimeDrawerType.SCHEDULER)))
        assertFalse(canOpenSimSchedulerFromEdge(RuntimeShellState(showHistory = true)))
        assertFalse(
            canOpenSimSchedulerFromEdge(
                RuntimeShellState(activeConnectivitySurface = RuntimeConnectivitySurface.MODAL)
            )
        )
        assertFalse(canOpenSimSchedulerFromEdge(RuntimeShellState(showSettings = true)))
    }

    @Test
    fun `sim shell chrome hosted audio gesture yields to ime visibility`() {
        assertTrue(canOpenSimAudioFromEdge(RuntimeShellState(), isImeVisible = false))
        assertFalse(canOpenSimAudioFromEdge(RuntimeShellState(), isImeVisible = true))
    }

    @Test
    fun `idle composer helper hint only shows when shell is clear and ime is hidden`() {
        assertTrue(shouldShowRuntimeIdleComposerHint(RuntimeShellState(), isImeVisible = false))
        assertFalse(shouldShowRuntimeIdleComposerHint(RuntimeShellState(activeDrawer = RuntimeDrawerType.AUDIO), isImeVisible = false))
        assertFalse(shouldShowRuntimeIdleComposerHint(RuntimeShellState(showHistory = true), isImeVisible = false))
        assertFalse(shouldShowRuntimeIdleComposerHint(RuntimeShellState(showSettings = true), isImeVisible = false))
        assertFalse(
            shouldShowRuntimeIdleComposerHint(
                RuntimeShellState(activeConnectivitySurface = RuntimeConnectivitySurface.MODAL),
                isImeVisible = false
            )
        )
        assertFalse(shouldShowRuntimeIdleComposerHint(RuntimeShellState(), isImeVisible = true))
    }

    @Test
    fun `startup scheduler teaser only auto opens once when shell is clear`() {
        assertTrue(
            shouldAutoOpenRuntimeSchedulerStartupTeaser(
                state = RuntimeShellState(),
                isImeVisible = false,
                teaserPending = true
            )
        )
        assertFalse(
            shouldAutoOpenRuntimeSchedulerStartupTeaser(
                state = RuntimeShellState(activeDrawer = RuntimeDrawerType.AUDIO),
                isImeVisible = false,
                teaserPending = true
            )
        )
        assertFalse(
            shouldAutoOpenRuntimeSchedulerStartupTeaser(
                state = RuntimeShellState(),
                isImeVisible = false,
                teaserPending = false
            )
        )
    }

    @Test
    fun `post onboarding scheduler handoff only auto opens once when shell is clear`() {
        assertTrue(
            shouldAutoOpenRuntimeSchedulerPostOnboardingHandoff(
                state = RuntimeShellState(),
                isImeVisible = false,
                handoffPending = true
            )
        )
        assertFalse(
            shouldAutoOpenRuntimeSchedulerPostOnboardingHandoff(
                state = RuntimeShellState(activeDrawer = RuntimeDrawerType.AUDIO),
                isImeVisible = false,
                handoffPending = true
            )
        )
        assertFalse(
            shouldAutoOpenRuntimeSchedulerPostOnboardingHandoff(
                state = RuntimeShellState(),
                isImeVisible = true,
                handoffPending = true
            )
        )
        assertFalse(
            shouldAutoOpenRuntimeSchedulerPostOnboardingHandoff(
                state = RuntimeShellState(),
                isImeVisible = false,
                handoffPending = false
            )
        )
    }

    @Test
    fun `buildSimDynamicIslandItems keeps idle fallback when no tasks exist`() {
        val items = buildSimDynamicIslandItems(
            sessionTitle = "",
            orderedTasks = emptyList()
        )

        assertEquals(1, items.size)
        assertEquals("SIM", items.single().sessionTitle)
        assertEquals("暂无待办", items.single().schedulerSummary)
        assertTrue(items.single().isIdleEntry)
    }

    @Test
    fun `buildSimDynamicIslandItems can show idle teaching hint for scheduler discoverability`() {
        val items = buildSimDynamicIslandItems(
            sessionTitle = "",
            orderedTasks = emptyList(),
            showIdleTeachingHint = true
        )

        assertEquals(1, items.size)
        assertEquals("下滑这里查看日程", items.single().schedulerSummary)
        assertTrue(items.single().isIdleEntry)
    }

    @Test
    fun `dismissRuntimeSchedulerIslandHint only clears session teaching flag`() {
        val dismissed = dismissRuntimeSchedulerIslandHint(
            RuntimeShellState(showSchedulerIslandHint = true)
        )

        assertFalse(dismissed.showSchedulerIslandHint)
        assertNull(dismissed.activeDrawer)
    }

    @Test
    fun `buildSimDynamicIslandItems keeps scheduler ordering and emits task target`() {
        val items = buildSimDynamicIslandItems(
            sessionTitle = "客户A",
            orderedTasks = listOf(
                task(id = "l1", title = "优先回访", timeDisplay = "15:00"),
                task(id = "l2", title = "发送纪要", timeDisplay = "18:00")
            )
        )

        assertEquals(2, items.size)
        assertEquals("客户A", items.first().sessionTitle)
        assertEquals("最近：优先回访 · 15:00", items.first().schedulerSummary)
        val tapAction = items.first().tapAction as DynamicIslandTapAction.OpenSchedulerDrawer
        assertEquals("l1", tapAction.target?.taskId)
    }

    @Test
    fun `buildSimDynamicIslandItems caps entries at top three and keeps conflict first`() {
        val items = buildSimDynamicIslandItems(
            sessionTitle = "客户B",
            orderedTasks = listOf(
                task(id = "c1", title = "冲突日程", timeDisplay = "16:00", hasConflict = true),
                task(id = "n1", title = "回访", timeDisplay = "10:00"),
                task(id = "n2", title = "发报价", timeDisplay = "11:00"),
                task(id = "n3", title = "内部同步", timeDisplay = "12:00")
            )
        )

        assertEquals(listOf("c1", "n1", "n2"), items.map {
            ((it.tapAction as DynamicIslandTapAction.OpenSchedulerDrawer).target?.taskId).orEmpty()
        })
        assertTrue(items.first().isConflict)
        assertEquals("冲突：冲突日程 · 16:00", items.first().schedulerSummary)
    }

    @Test
    fun `resolveSimDynamicIslandIndex keeps current item when still present`() {
        val items = buildSimDynamicIslandItems(
            sessionTitle = "客户C",
            orderedTasks = listOf(
                task(id = "a", title = "任务A", timeDisplay = "09:00"),
                task(id = "b", title = "任务B", timeDisplay = "10:00")
            )
        )

        val resolvedIndex = resolveSimDynamicIslandIndex(
            items = items,
            currentItemKey = items[1].stableKey
        )

        assertEquals(1, resolvedIndex)
    }

    @Test
    fun `resolveSimDynamicIslandIndex resets to first item when current item disappears`() {
        val items = buildSimDynamicIslandItems(
            sessionTitle = "客户D",
            orderedTasks = listOf(
                task(id = "a", title = "任务A", timeDisplay = "09:00"),
                task(id = "b", title = "任务B", timeDisplay = "10:00")
            )
        )

        val resolvedIndex = resolveSimDynamicIslandIndex(
            items = items.take(1),
            currentItemKey = items[1].stableKey
        )

        assertEquals(0, resolvedIndex)
    }

    @Test
    fun `emitSchedulerShelfHandoffTelemetry emits request summary and log`() {
        val checkpoints = mutableListOf<Pair<PipelineValve.Checkpoint, String>>()
        val logs = mutableListOf<String>()

        PipelineValve.testInterceptor = { checkpoint, _, summary ->
            checkpoints += checkpoint to summary
        }

        try {
            emitSchedulerShelfHandoffTelemetry("i want to learn guitar") { message ->
                logs += message
            }

            assertTrue(
                checkpoints.contains(
                    PipelineValve.Checkpoint.UI_STATE_EMITTED to
                        SIM_SCHEDULER_SHELF_HANDOFF_REQUEST_SUMMARY
                )
            )
            assertEquals(
                listOf("scheduler shelf handoff requested: i want to learn guitar"),
                logs
            )
        } finally {
            PipelineValve.testInterceptor = null
        }
    }

    @Test
    fun `handleSchedulerShelfAskAiHandoff starts session and closes drawer`() {
        val emittedPrompts = mutableListOf<String>()
        val startedPrompts = mutableListOf<String>()
        var closeCount = 0

        handleSchedulerShelfAskAiHandoff(
            promptText = "i want to learn guitar",
            startSession = { startedPrompts += it },
            closeDrawer = { closeCount += 1 },
            emitTelemetry = { emittedPrompts += it }
        )

        assertEquals(listOf("i want to learn guitar"), emittedPrompts)
        assertEquals(listOf("i want to learn guitar"), startedPrompts)
        assertEquals(1, closeCount)
    }

    @Test
    fun `handleSchedulerShelfAskAiHandoff ignores blank prompt`() {
        var started = false
        var closed = false
        var emitted = false

        handleSchedulerShelfAskAiHandoff(
            promptText = "   ",
            startSession = { started = true },
            closeDrawer = { closed = true },
            emitTelemetry = { emitted = true }
        )

        assertFalse(emitted)
        assertFalse(started)
        assertFalse(closed)
    }

    private fun task(
        id: String,
        title: String,
        timeDisplay: String,
        isDone: Boolean = false,
        isVague: Boolean = false,
        hasConflict: Boolean = false
    ) = ScheduledTask(
        id = id,
        timeDisplay = timeDisplay,
        title = title,
        urgencyLevel = UrgencyLevel.L3_NORMAL,
        isDone = isDone,
        hasConflict = hasConflict,
        hasAlarm = false,
        isSmartAlarm = false,
        startTime = Instant.parse("2026-03-22T00:00:00Z"),
        endTime = null,
        durationMinutes = 0,
        durationSource = DurationSource.DEFAULT,
        conflictPolicy = ConflictPolicy.EXCLUSIVE,
        isVague = isVague
    )

    @Test
    fun `handleBadgeSchedulerFollowUpStart creates owner binding from created session`() {
        val ownerBindings = mutableListOf<String>()

        val sessionId = handleBadgeSchedulerFollowUpStart(
            seed = SimBadgeSchedulerFollowUpSeed(
                threadId = "thread_1",
                transcript = "follow up after badge request",
                tasks = listOf(
                    SchedulerFollowUpTaskSummary(
                        taskId = "task_1",
                        title = "客户回访",
                        dayOffset = 0,
                        scheduledAtMillis = 123L,
                        durationMinutes = 30
                    )
                )
            ),
            startSession = { seed ->
                assertEquals("follow up after badge request", seed.transcript)
                "session_123"
            },
            startOwner = { sessionId, threadId -> ownerBindings += "$sessionId:$threadId" }
        )

        assertEquals("session_123", sessionId)
        assertEquals(listOf("session_123:thread_1"), ownerBindings)
    }

    @Test
    fun `handleBadgeSchedulerFollowUpStart ignores blank prompt`() {
        var started = false
        var ownerStarted = false

        val sessionId = handleBadgeSchedulerFollowUpStart(
            seed = SimBadgeSchedulerFollowUpSeed(
                threadId = "thread_1",
                transcript = "   ",
                tasks = emptyList()
            ),
            startSession = {
                started = true
                "session_123"
            },
            startOwner = { _, _ -> ownerStarted = true }
        )

        assertNull(sessionId)
        assertFalse(started)
        assertFalse(ownerStarted)
    }

    @Test
    fun `emitBadgeSchedulerContinuityIngressTelemetry emits accepted summary and log`() {
        val checkpoints = mutableListOf<Pair<PipelineValve.Checkpoint, String>>()
        val logs = mutableListOf<String>()

        PipelineValve.testInterceptor = { checkpoint, _, summary ->
            checkpoints += checkpoint to summary
        }

        try {
            emitBadgeSchedulerContinuityIngressTelemetry("follow up after client badge") { message ->
                logs += message
            }

            assertTrue(
                checkpoints.contains(
                    PipelineValve.Checkpoint.UI_STATE_EMITTED to
                        SIM_BADGE_SCHEDULER_CONTINUITY_INGRESS_ACCEPTED_SUMMARY
                )
            )
            assertEquals(
                listOf("badge scheduler continuity ingress accepted: follow up after client badge"),
                logs
            )
        } finally {
            PipelineValve.testInterceptor = null
        }
    }

    @Test
    fun `handleBadgeSchedulerContinuityIngress starts session for task created completion`() {
        val ownerBindings = mutableListOf<String>()
        val telemetryPrompts = mutableListOf<String>()

        val sessionId = handleBadgeSchedulerContinuityIngress(
            event = PipelineEvent.Complete(
                result = SchedulerResult.TaskCreated(
                    taskId = "task_1",
                    title = "follow up",
                    dayOffset = 0,
                    scheduledAtMillis = 123L,
                    durationMinutes = 30
                ),
                filename = "badge_1.wav",
                transcript = "follow up after client badge"
            ),
            startSession = { seed ->
                assertEquals("follow up after client badge", seed.transcript)
                assertEquals(listOf("task_1"), seed.tasks.map { it.taskId })
                "session_123"
            },
            startOwner = { sessionId, threadId -> ownerBindings += "$sessionId:$threadId" },
            emitTelemetry = { telemetryPrompts += it }
        )

        assertEquals("session_123", sessionId)
        assertEquals(listOf("follow up after client badge"), telemetryPrompts)
        assertEquals(1, ownerBindings.size)
        assertTrue(ownerBindings.single().startsWith("session_123:"))
    }

    @Test
    fun `handleBadgeSchedulerContinuityIngress accepts non empty multi task completion`() {
        val sessionCalls = mutableListOf<String>()
        val ownerBindings = mutableListOf<String>()

        val sessionId = handleBadgeSchedulerContinuityIngress(
            event = PipelineEvent.Complete(
                result = SchedulerResult.MultiTaskCreated(
                    tasks = listOf(
                        SchedulerResult.TaskCreated(
                            taskId = "task_1",
                            title = "follow up",
                            dayOffset = 0,
                            scheduledAtMillis = 123L,
                            durationMinutes = 30
                        )
                    )
                ),
                filename = "badge_2.wav",
                transcript = "schedule both customer follow ups"
            ),
            startSession = { seed ->
                sessionCalls += seed.transcript
                assertEquals(1, seed.tasks.size)
                "session_multi"
            },
            startOwner = { sessionId, threadId -> ownerBindings += "$sessionId:$threadId" }
        )

        assertEquals("session_multi", sessionId)
        assertEquals(listOf("schedule both customer follow ups"), sessionCalls)
        assertEquals(1, ownerBindings.size)
        assertTrue(ownerBindings.single().startsWith("session_multi:"))
    }

    @Test
    fun `handleBadgeSchedulerContinuityIngress ignores non scheduler completions and blank transcript`() {
        val startedPrompts = mutableListOf<String>()
        val ownerBindings = mutableListOf<String>()
        val emittedTelemetry = mutableListOf<String>()

        val ignoredEvents = listOf(
            PipelineEvent.RecordingStarted,
            PipelineEvent.Processing("processing"),
            PipelineEvent.Error(
                stage = PipelineEvent.Stage.SCHEDULE,
                message = "boom",
                filename = "badge_3.wav"
            ),
            PipelineEvent.Complete(
                result = SchedulerResult.InspirationSaved(id = "insp_1"),
                filename = "badge_4.wav",
                transcript = "save this inspiration"
            ),
            PipelineEvent.Complete(
                result = SchedulerResult.AwaitingClarification(question = "when"),
                filename = "badge_5.wav",
                transcript = "maybe reschedule"
            ),
            PipelineEvent.Complete(
                result = SchedulerResult.Ignored,
                filename = "badge_6.wav",
                transcript = "hello there"
            ),
            PipelineEvent.Complete(
                result = SchedulerResult.MultiTaskCreated(tasks = emptyList()),
                filename = "badge_7.wav",
                transcript = "schedule several things"
            ),
            PipelineEvent.Complete(
                result = SchedulerResult.TaskCreated(
                    taskId = "task_2",
                    title = "follow up",
                    dayOffset = 0,
                    scheduledAtMillis = 456L,
                    durationMinutes = 30
                ),
                filename = "badge_8.wav",
                transcript = "   "
            )
        )

        ignoredEvents.forEach { event ->
            val sessionId = handleBadgeSchedulerContinuityIngress(
                event = event,
                startSession = { seed ->
                    startedPrompts += seed.transcript
                    "session_should_not_start"
                },
                startOwner = { sessionId, threadId -> ownerBindings += "$sessionId:$threadId" },
                emitTelemetry = { emittedTelemetry += it }
            )

            assertNull(sessionId)
        }

        assertTrue(startedPrompts.isEmpty())
        assertTrue(ownerBindings.isEmpty())
        assertTrue(emittedTelemetry.isEmpty())
    }

    @Test
    fun `buildSimDebugFollowUpEvent single creates task created completion`() {
        val event = buildSimDebugFollowUpEvent(
            scenario = SimDebugFollowUpScenario.SINGLE,
            nowMillis = 1_000L
        )

        val result = event.result as SchedulerResult.TaskCreated
        assertEquals("debug_follow_up_single", result.taskId)
        assertEquals("提醒我一会儿回访客户", event.transcript)
    }

    @Test
    fun `buildSimDebugFollowUpEvent multi creates multi task completion`() {
        val event = buildSimDebugFollowUpEvent(
            scenario = SimDebugFollowUpScenario.MULTI,
            nowMillis = 1_000L
        )

        val result = event.result as SchedulerResult.MultiTaskCreated
        assertEquals(2, result.tasks.size)
        assertEquals("安排两个客户回访", event.transcript)
    }

    @Test
    fun `handleSimNewSessionAction clears follow up before starting new session`() {
        val calls = mutableListOf<String>()

        handleSimNewSessionAction(
            activeFollowUp = followUpState(boundSessionId = "session_1"),
            clearFollowUp = { reason -> calls += "clear:$reason" },
            startNewSession = { calls += "start" }
        )

        assertEquals(
            listOf("clear:${SimBadgeFollowUpClearReason.NEW_SESSION}", "start"),
            calls
        )
    }

    @Test
    fun `handleSimSessionSwitchAction clears only when switching away from bound session`() {
        val unrelatedCalls = mutableListOf<String>()
        handleSimSessionSwitchAction(
            targetSessionId = "session_2",
            activeFollowUp = followUpState(boundSessionId = "session_1"),
            clearFollowUp = { reason -> unrelatedCalls += "clear:$reason" },
            switchSession = { unrelatedCalls += "switch:$it" }
        )
        assertEquals(
            listOf("clear:${SimBadgeFollowUpClearReason.SESSION_SWITCHED}", "switch:session_2"),
            unrelatedCalls
        )

        val sameSessionCalls = mutableListOf<String>()
        handleSimSessionSwitchAction(
            targetSessionId = "session_1",
            activeFollowUp = followUpState(boundSessionId = "session_1"),
            clearFollowUp = { reason -> sameSessionCalls += "clear:$reason" },
            switchSession = { sameSessionCalls += "switch:$it" }
        )
        assertEquals(listOf("switch:session_1"), sameSessionCalls)
    }

    @Test
    fun `handleSimSessionDeleteAction clears only when deleting bound session`() {
        val boundCalls = mutableListOf<String>()
        handleSimSessionDeleteAction(
            targetSessionId = "session_1",
            activeFollowUp = followUpState(boundSessionId = "session_1"),
            clearFollowUp = { reason -> boundCalls += "clear:$reason" },
            deleteSession = { boundCalls += "delete:$it" }
        )
        assertEquals(
            listOf("clear:${SimBadgeFollowUpClearReason.SESSION_DELETED}", "delete:session_1"),
            boundCalls
        )

        val unrelatedCalls = mutableListOf<String>()
        handleSimSessionDeleteAction(
            targetSessionId = "session_2",
            activeFollowUp = followUpState(boundSessionId = "session_1"),
            clearFollowUp = { reason -> unrelatedCalls += "clear:$reason" },
            deleteSession = { unrelatedCalls += "delete:$it" }
        )
        assertEquals(listOf("delete:session_2"), unrelatedCalls)
    }

    @Test
    fun `deriveRuntimeFollowUpSurface follows shell surface priority`() {
        assertEquals(
            SimBadgeFollowUpSurface.CHAT,
            deriveRuntimeFollowUpSurface(RuntimeShellState())
        )
        assertEquals(
            SimBadgeFollowUpSurface.SCHEDULER,
            deriveRuntimeFollowUpSurface(RuntimeShellState(activeDrawer = RuntimeDrawerType.SCHEDULER))
        )
        assertEquals(
            SimBadgeFollowUpSurface.CONNECTIVITY,
            deriveRuntimeFollowUpSurface(
                RuntimeShellState(
                    activeDrawer = RuntimeDrawerType.SCHEDULER,
                    activeConnectivitySurface = RuntimeConnectivitySurface.MODAL
                )
            )
        )
        assertEquals(
            SimBadgeFollowUpSurface.HISTORY,
            deriveRuntimeFollowUpSurface(
                RuntimeShellState(
                    activeDrawer = RuntimeDrawerType.SCHEDULER,
                    activeConnectivitySurface = RuntimeConnectivitySurface.MODAL,
                    showHistory = true
                )
            )
        )
        assertEquals(
            SimBadgeFollowUpSurface.SETTINGS,
            deriveRuntimeFollowUpSurface(
                RuntimeShellState(
                    showSettings = true
                )
            )
        )
    }

    @Test
    fun `emitSimAudioPersistedArtifactOpenedTelemetry emits summary and offline log`() {
        val checkpoints = mutableListOf<Pair<PipelineValve.Checkpoint, String>>()
        val logs = mutableListOf<Pair<String, String>>()

        PipelineValve.testInterceptor = { checkpoint, _, summary ->
            checkpoints += checkpoint to summary
        }

        try {
            emitSimAudioPersistedArtifactOpenedTelemetry(
                audioId = "audio_1",
                title = "SIM_Debug_Fallback.mp3",
                log = { tag, message -> logs += tag to message }
            )

            assertTrue(
                checkpoints.contains(
                    PipelineValve.Checkpoint.UI_STATE_EMITTED to
                        SIM_AUDIO_PERSISTED_ARTIFACT_OPENED_SUMMARY
                )
            )
            assertEquals(
                listOf(
                    "SimAudioOffline" to
                        "SIM audio persisted artifact opened: audioId=audio_1 title=SIM_Debug_Fallback.mp3"
                ),
                logs
            )
        } finally {
            PipelineValve.testInterceptor = null
        }
    }

    @Test
    fun `emitSimHistoryRouteTelemetry emits summary and log`() {
        val checkpoints = mutableListOf<Pair<PipelineValve.Checkpoint, String>>()
        val logs = mutableListOf<String>()

        PipelineValve.testInterceptor = { checkpoint, _, summary ->
            checkpoints += checkpoint to summary
        }

        try {
            emitSimHistoryRouteTelemetry(
                summary = SIM_HISTORY_DRAWER_OPENED_SUMMARY,
                detail = "source=hamburger",
                log = { message -> logs += message }
            )

            assertTrue(
                checkpoints.contains(
                    PipelineValve.Checkpoint.UI_STATE_EMITTED to
                        SIM_HISTORY_DRAWER_OPENED_SUMMARY
                )
            )
            assertEquals(
                listOf("SIM history drawer opened: source=hamburger"),
                logs
            )
        } finally {
            PipelineValve.testInterceptor = null
        }
    }

    @Test
    fun `emitSimAudioGroundedChatOpenedFromArtifactTelemetry emits summary and chat route log`() {
        val checkpoints = mutableListOf<Pair<PipelineValve.Checkpoint, String>>()
        val logs = mutableListOf<Pair<String, String>>()

        PipelineValve.testInterceptor = { checkpoint, _, summary ->
            checkpoints += checkpoint to summary
        }

        try {
            emitSimAudioGroundedChatOpenedFromArtifactTelemetry(
                audioId = "audio_2",
                title = "SIM_Debug_Missing_Sections.mp3",
                log = { tag, message -> logs += tag to message }
            )

            assertTrue(
                checkpoints.contains(
                    PipelineValve.Checkpoint.UI_STATE_EMITTED to
                        SIM_AUDIO_GROUNDED_CHAT_OPENED_FROM_ARTIFACT_SUMMARY
                )
            )
            assertEquals(
                listOf(
                    "SimAudioChatRoute" to
                        "SIM audio grounded chat opened from artifact: audioId=audio_2 title=SIM_Debug_Missing_Sections.mp3"
                ),
                logs
            )
        } finally {
            PipelineValve.testInterceptor = null
        }
    }

    @Test
    fun `canOpenSimSchedulerFromEdge only allows clean shell state`() {
        assertTrue(canOpenSimSchedulerFromEdge(RuntimeShellState()))
        assertFalse(
            canOpenSimSchedulerFromEdge(
                RuntimeShellState(activeDrawer = RuntimeDrawerType.SCHEDULER)
            )
        )
        assertFalse(
            canOpenSimSchedulerFromEdge(
                RuntimeShellState(showHistory = true)
            )
        )
        assertFalse(
            canOpenSimSchedulerFromEdge(
                RuntimeShellState(activeConnectivitySurface = RuntimeConnectivitySurface.MODAL)
            )
        )
        assertFalse(
            canOpenSimSchedulerFromEdge(
                RuntimeShellState(showSettings = true)
            )
        )
    }

    @Test
    fun `canOpenSimAudioFromEdge also blocks when ime is visible`() {
        assertTrue(
            canOpenSimAudioFromEdge(
                state = RuntimeShellState(),
                isImeVisible = false
            )
        )
        assertFalse(
            canOpenSimAudioFromEdge(
                state = RuntimeShellState(),
                isImeVisible = true
            )
        )
        assertFalse(
            canOpenSimAudioFromEdge(
                state = RuntimeShellState(activeDrawer = RuntimeDrawerType.AUDIO),
                isImeVisible = false
            )
        )
    }

    @Test
    fun `shouldTriggerSimVerticalGesture accepts downward fling for scheduler zone`() {
        assertTrue(
            shouldTriggerSimVerticalGesture(
                direction = SimVerticalGestureDirection.DOWN,
                totalDy = 12f,
                velocityY = 2_000f,
                velocityThresholdPx = 1_400f
            )
        )
        assertFalse(
            shouldTriggerSimVerticalGesture(
                direction = SimVerticalGestureDirection.DOWN,
                totalDy = -12f,
                velocityY = 2_000f,
                velocityThresholdPx = 1_400f
            )
        )
    }

    @Test
    fun `shouldTriggerSimVerticalGesture accepts upward fling for audio zone`() {
        assertTrue(
            shouldTriggerSimVerticalGesture(
                direction = SimVerticalGestureDirection.UP,
                totalDy = -12f,
                velocityY = -2_000f,
                velocityThresholdPx = 1_400f
            )
        )
        assertFalse(
            shouldTriggerSimVerticalGesture(
                direction = SimVerticalGestureDirection.UP,
                totalDy = 12f,
                velocityY = -2_000f,
                velocityThresholdPx = 1_400f
            )
        )
    }

    private fun followUpState(boundSessionId: String) = SimBadgeFollowUpState(
        threadId = "thread_1",
        origin = SimBadgeFollowUpOrigin.BADGE,
        lane = SimBadgeFollowUpLane.SCHEDULER,
        boundSessionId = boundSessionId,
        createdAt = 1L,
        updatedAt = 1L,
        lastActiveSurface = SimBadgeFollowUpSurface.SHELL
    )
}
