package com.smartsales.prism.ui.onboarding

import com.smartsales.prism.data.audio.DeviceSpeechFailureReason
import com.smartsales.prism.data.audio.DeviceSpeechRecognitionEvent
import com.smartsales.prism.data.audio.DeviceSpeechMode
import com.smartsales.prism.data.audio.DeviceSpeechRecognitionResult
import com.smartsales.prism.data.audio.DeviceSpeechRecognizer
import com.smartsales.prism.data.notification.ReminderReliabilityAdvisor
import com.smartsales.prism.data.onboarding.RuntimeOnboardingHandoffGate
import com.smartsales.prism.domain.config.SubscriptionTier
import com.smartsales.prism.domain.memory.UserProfile
import com.smartsales.prism.domain.repository.UserProfileRepository
import com.smartsales.prism.domain.time.TimeProvider
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingInteractionViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var speechRecognizer: FakeDeviceSpeechRecognizer
    private lateinit var interactionService: FakeOnboardingInteractionService
    private lateinit var repository: FakeUserProfileRepository
    private lateinit var quickStartService: FakeOnboardingQuickStartService
    private lateinit var quickStartCommitter: FakeOnboardingSchedulerQuickStartCommitter
    private lateinit var quickStartReminderGuideCoordinator: FakeOnboardingQuickStartReminderGuideCoordinator
    private lateinit var quickStartCalendarExporter: FakeOnboardingQuickStartCalendarExporter
    private lateinit var onboardingHandoffGate: FakeRuntimeOnboardingHandoffGate
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var viewModel: OnboardingInteractionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        speechRecognizer = FakeDeviceSpeechRecognizer()
        interactionService = FakeOnboardingInteractionService()
        repository = FakeUserProfileRepository()
        quickStartService = FakeOnboardingQuickStartService()
        quickStartCommitter = FakeOnboardingSchedulerQuickStartCommitter()
        quickStartReminderGuideCoordinator = FakeOnboardingQuickStartReminderGuideCoordinator()
        quickStartCalendarExporter = FakeOnboardingQuickStartCalendarExporter()
        onboardingHandoffGate = FakeRuntimeOnboardingHandoffGate()
        timeProvider = FakeTimeProvider()
        viewModel = OnboardingInteractionViewModel(
            speechRecognizer = speechRecognizer,
            interactionService = interactionService,
            userProfileRepository = repository,
            quickStartService = quickStartService,
            quickStartCommitter = quickStartCommitter,
            quickStartReminderGuideCoordinator = quickStartReminderGuideCoordinator,
            quickStartCalendarExporter = quickStartCalendarExporter,
            onboardingHandoffGate = onboardingHandoffGate,
            timeProvider = timeProvider
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `consultation success appends transcript and ai reply`() = runTest {
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success("客户预算批不下来")
        interactionService.consultationResult =
            OnboardingConsultationServiceResult.Success("先别急着压价格，先确认客户卡点。")

        assertTrue(viewModel.startConsultationRecording())
        viewModel.finishConsultationRecording()
        advanceUntilIdle()

        val state = viewModel.consultationState.value
        assertEquals(1, state.completedRounds)
        assertEquals(2, state.messages.size)
        assertEquals("客户预算批不下来", state.messages[0].text)
        assertEquals("先别急着压价格，先确认客户卡点。", state.messages[1].text)
        assertEquals(OnboardingTranscriptOrigin.DEVICE_SPEECH, state.lastTranscriptOrigin)
        assertEquals(OnboardingGenerationOrigin.LLM, state.lastGenerationOrigin)
        assertFalse(state.isProcessing)
        assertEquals(OnboardingMicInteractionMode.TAP_TO_SEND, state.micInteractionMode)
        assertEquals(DeviceSpeechMode.FUN_ASR_REALTIME, speechRecognizer.lastMode)
    }

    @Test
    fun `consultation moves from recognizing to local reply phase before result`() = runTest {
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success("客户预算批不下来")
        interactionService.consultationDelayMillis = 1_000L
        interactionService.consultationResult =
            OnboardingConsultationServiceResult.Success("先别急着压价格，先确认客户卡点。")

        assertTrue(viewModel.startConsultationRecording())
        viewModel.finishConsultationRecording()
        dispatcher.scheduler.runCurrent()

        val state = viewModel.consultationState.value
        assertTrue(state.isProcessing)
        assertEquals(OnboardingProcessingPhase.BUILDING_CONSULTATION_REPLY, state.processingPhase)
        assertTrue(state.messages.isEmpty())

        advanceUntilIdle()

        assertFalse(viewModel.consultationState.value.isProcessing)
        assertEquals(2, viewModel.consultationState.value.messages.size)
    }

    @Test
    fun `consultation recognizer timeout surfaces calm retry after bounded processing`() = runTest {
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success("客户预算批不下来")
        speechRecognizer.finishDelayMillis = 5_100L

        assertTrue(viewModel.startConsultationRecording())
        viewModel.finishConsultationRecording()

        dispatcher.scheduler.advanceTimeBy(4_999L)
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.consultationState.value.isProcessing)
        assertEquals(OnboardingProcessingPhase.RECOGNIZING, viewModel.consultationState.value.processingPhase)

        dispatcher.scheduler.advanceTimeBy(1L)
        dispatcher.scheduler.runCurrent()

        val state = viewModel.consultationState.value
        assertFalse(state.isProcessing)
        assertTrue(state.messages.isEmpty())
        assertEquals("这次语音识别没有完成，请再试一次。", state.errorMessage)
        assertNull(state.lastTranscriptOrigin)
        assertNull(state.lastGenerationOrigin)
        assertFalse(speechRecognizer.isListening())
        assertTrue(viewModel.startConsultationRecording())
    }

    @Test
    fun `consultation failure while recording returns to tap retry state and stale stop is no op`() = runTest {
        assertTrue(viewModel.startConsultationRecording())

        speechRecognizer.emitEvent(
            DeviceSpeechRecognitionEvent.Failure(
                reason = DeviceSpeechFailureReason.UNAVAILABLE,
                message = "当前无法获取语音识别凭证，请稍后重试",
                backend = com.smartsales.prism.data.audio.DeviceSpeechBackend.FUN_ASR_REALTIME
            )
        )
        advanceUntilIdle()

        val failedState = viewModel.consultationState.value
        assertFalse(failedState.isRecording)
        assertFalse(failedState.isProcessing)
        assertEquals("当前语音识别暂不可用，请稍后重试。", failedState.errorMessage)
        assertTrue(failedState.messages.isEmpty())
        assertEquals(OnboardingMicInteractionMode.TAP_TO_SEND, failedState.micInteractionMode)

        viewModel.finishConsultationRecording()
        advanceUntilIdle()

        val afterRelease = viewModel.consultationState.value
        assertEquals(failedState, afterRelease)
    }

    @Test
    fun `consultation cancelled after processing begins clears processing and shows retry error`() = runTest {
        speechRecognizer.finishGate = CompletableDeferred()

        assertTrue(viewModel.startConsultationRecording())
        viewModel.finishConsultationRecording()
        dispatcher.scheduler.runCurrent()

        assertTrue(viewModel.consultationState.value.isProcessing)
        assertEquals(
            OnboardingProcessingPhase.RECOGNIZING,
            viewModel.consultationState.value.processingPhase
        )

        speechRecognizer.finishGate?.complete(
            DeviceSpeechRecognitionResult.Failure(
                reason = DeviceSpeechFailureReason.CANCELLED,
                message = "语音识别已取消"
            )
        )
        advanceUntilIdle()

        val state = viewModel.consultationState.value
        assertFalse(state.isProcessing)
        assertEquals(OnboardingProcessingPhase.NONE, state.processingPhase)
        assertTrue(state.messages.isEmpty())
        assertNull(state.lastTranscriptOrigin)
        assertNull(state.lastGenerationOrigin)
        assertEquals("这次语音识别没有完成，请再试一次。", state.errorMessage)
        assertEquals(OnboardingMicInteractionMode.TAP_TO_SEND, state.micInteractionMode)
    }

    @Test
    fun `consultation llm failure keeps real transcript and surfaces retry without synthetic ai reply`() = runTest {
        val transcript = "客户预算批不下来"
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success(transcript)
        interactionService.consultationResult =
            OnboardingConsultationServiceResult.Failure("llm unavailable")

        assertTrue(viewModel.startConsultationRecording())
        viewModel.finishConsultationRecording()
        advanceUntilIdle()

        val state = viewModel.consultationState.value
        assertEquals(transcript, state.messages.first().text)
        assertEquals(OnboardingTranscriptOrigin.DEVICE_SPEECH, state.lastTranscriptOrigin)
        assertNull(state.lastGenerationOrigin)
        assertEquals(1, state.messages.size)
        assertEquals("当前 AI 咨询暂时没有返回，请再试一次。", state.errorMessage)
    }

    @Test
    fun `consultation llm deadline keeps real transcript and surfaces retry`() = runTest {
        val transcript = "客户预算批不下来"
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success(transcript)
        interactionService.consultationDelayMillis = 2_600L

        assertTrue(viewModel.startConsultationRecording())
        viewModel.finishConsultationRecording()

        dispatcher.scheduler.advanceTimeBy(2_499L)
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.consultationState.value.isProcessing)
        assertEquals(
            OnboardingProcessingPhase.BUILDING_CONSULTATION_REPLY,
            viewModel.consultationState.value.processingPhase
        )

        dispatcher.scheduler.advanceTimeBy(1L)
        advanceUntilIdle()

        val state = viewModel.consultationState.value
        assertEquals(transcript, state.messages.first().text)
        assertEquals(OnboardingTranscriptOrigin.DEVICE_SPEECH, state.lastTranscriptOrigin)
        assertNull(state.lastGenerationOrigin)
        assertEquals("当前 AI 咨询暂时没有返回，请再试一次。", state.errorMessage)
    }

    @Test
    fun `consultation permission grant returns to idle and asks for fresh tap`() {
        viewModel.onConsultationMicPermissionRequested()
        assertTrue(viewModel.consultationState.value.awaitingMicPermission)

        viewModel.onConsultationMicPermissionResult(granted = true)

        val state = viewModel.consultationState.value
        assertFalse(state.isRecording)
        assertFalse(state.awaitingMicPermission)
        assertEquals(OnboardingMicInteractionMode.TAP_TO_SEND, state.micInteractionMode)
        assertFalse(speechRecognizer.isListening())
        assertEquals("麦克风已开启，请点击开始说话", state.guidanceMessage)
        assertEquals(null, speechRecognizer.lastMode)
    }

    @Test
    fun `consultation realtime partial transcript updates live state before send`() = runTest {
        assertTrue(viewModel.startConsultationRecording())

        speechRecognizer.emitEvent(
            DeviceSpeechRecognitionEvent.PartialTranscript(
                text = "帮我搞定这个客户",
                backend = com.smartsales.prism.data.audio.DeviceSpeechBackend.FUN_ASR_REALTIME
            )
        )
        advanceUntilIdle()

        val state = viewModel.consultationState.value
        assertTrue(state.isRecording)
        assertEquals("帮我搞定这个客户", state.liveTranscript)
        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun `consultation final transcript remains visible through processing until result renders`() = runTest {
        val transcript = "帮我搞定这个客户"
        speechRecognizer.finishGate = CompletableDeferred()
        interactionService.consultationDelayMillis = 1_000L
        interactionService.consultationResult =
            OnboardingConsultationServiceResult.Success("先别急着压价格，先确认客户卡点。")

        assertTrue(viewModel.startConsultationRecording())
        viewModel.finishConsultationRecording()
        dispatcher.scheduler.runCurrent()

        speechRecognizer.emitEvent(
            DeviceSpeechRecognitionEvent.FinalTranscript(
                text = transcript,
                backend = com.smartsales.prism.data.audio.DeviceSpeechBackend.FUN_ASR_REALTIME
            )
        )
        speechRecognizer.finishGate?.complete(DeviceSpeechRecognitionResult.Success(transcript))
        dispatcher.scheduler.runCurrent()

        val processingState = viewModel.consultationState.value
        assertTrue(processingState.isProcessing)
        assertEquals(OnboardingProcessingPhase.BUILDING_CONSULTATION_REPLY, processingState.processingPhase)
        assertEquals(transcript, processingState.liveTranscript)

        advanceUntilIdle()

        val finalState = viewModel.consultationState.value
        assertEquals("", finalState.liveTranscript)
        assertEquals(transcript, finalState.messages.first().text)
    }

    @Test
    fun `consultation capture limit moves state into processing and later extra stop is no op`() = runTest {
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success("客户预算批不下来")
        speechRecognizer.finishDelayMillis = 1_000L

        assertTrue(viewModel.startConsultationRecording())
        speechRecognizer.emitEvent(DeviceSpeechRecognitionEvent.CaptureLimitReached)
        dispatcher.scheduler.runCurrent()

        assertTrue(viewModel.consultationState.value.isProcessing)
        assertFalse(viewModel.consultationState.value.isRecording)

        viewModel.finishConsultationRecording()
        advanceUntilIdle()

        val state = viewModel.consultationState.value
        assertEquals(1, state.completedRounds)
        assertEquals("客户预算批不下来", state.messages.first().text)
        assertEquals(OnboardingMicInteractionMode.TAP_TO_SEND, state.micInteractionMode)
    }

    @Test
    fun `profile permission denial clears pending state and shows error`() {
        viewModel.onProfileMicPermissionRequested()
        assertTrue(viewModel.profileState.value.awaitingMicPermission)

        viewModel.onProfileMicPermissionResult(granted = false)

        val state = viewModel.profileState.value
        assertFalse(state.awaitingMicPermission)
        assertFalse(state.isRecording)
        assertEquals(OnboardingMicInteractionMode.TAP_TO_SEND, state.micInteractionMode)
        assertEquals("无法录音：未授予麦克风权限", state.errorMessage)
    }

    @Test
    fun `profile permission grant returns to idle and asks for fresh tap`() {
        viewModel.onProfileMicPermissionRequested()
        assertTrue(viewModel.profileState.value.awaitingMicPermission)

        viewModel.onProfileMicPermissionResult(granted = true)

        val state = viewModel.profileState.value
        assertFalse(state.isRecording)
        assertFalse(state.awaitingMicPermission)
        assertEquals(OnboardingMicInteractionMode.TAP_TO_SEND, state.micInteractionMode)
        assertFalse(speechRecognizer.isListening())
        assertEquals("麦克风已开启，请点击开始说话", state.guidanceMessage)
    }

    @Test
    fun `quick start permission grant returns to idle and asks for fresh tap`() {
        viewModel.onQuickStartMicPermissionRequested()
        assertTrue(viewModel.quickStartState.value.awaitingMicPermission)

        viewModel.onQuickStartMicPermissionResult(granted = true)

        val state = viewModel.quickStartState.value
        assertFalse(state.isRecording)
        assertFalse(state.awaitingMicPermission)
        assertEquals(OnboardingMicInteractionMode.TAP_TO_SEND, state.micInteractionMode)
        assertFalse(speechRecognizer.isListening())
        assertEquals("麦克风已开启，请点击开始说话", state.guidanceMessage)
    }

    @Test
    fun `profile extraction success exposes draft and acknowledgement`() = runTest {
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success(
            "我是王经理，做 SaaS 销售总监 8 年了，平时用微信。"
        )
        interactionService.profileResult = OnboardingProfileExtractionServiceResult.Success(
            acknowledgement = "谢谢您的分享，我已经为您建立好了专属档案。",
            draft = OnboardingProfileDraft(
                displayName = "王经理",
                role = "销售总监",
                industry = "SaaS",
                experienceYears = "8年",
                communicationPlatform = "微信"
            )
        )

        assertTrue(viewModel.startProfileRecording())
        viewModel.finishProfileRecording()
        advanceUntilIdle()

        val state = viewModel.profileState.value
        assertEquals("我是王经理，做 SaaS 销售总监 8 年了，平时用微信。", state.transcript)
        assertEquals("谢谢您的分享，我已经为您建立好了专属档案。", state.acknowledgement)
        assertEquals("王经理", state.draft?.displayName)
        assertEquals(OnboardingTranscriptOrigin.DEVICE_SPEECH, state.transcriptOrigin)
        assertEquals(OnboardingGenerationOrigin.LLM, state.generationOrigin)
        assertTrue(state.hasExtractionResult)
        assertEquals(OnboardingMicInteractionMode.TAP_TO_SEND, state.micInteractionMode)
    }

    @Test
    fun `profile moves from recognizing to local extraction phase before result`() = runTest {
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success(
            "我是王经理，做 SaaS 销售总监 8 年了，平时用微信。"
        )
        interactionService.profileDelayMillis = 1_000L
        interactionService.profileResult = OnboardingProfileExtractionServiceResult.Success(
            acknowledgement = "谢谢您的分享，我已经为您建立好了专属档案。",
            draft = OnboardingProfileDraft(
                displayName = "王经理",
                role = "销售总监",
                industry = "SaaS",
                experienceYears = "8年",
                communicationPlatform = "微信"
            )
        )

        assertTrue(viewModel.startProfileRecording())
        viewModel.finishProfileRecording()
        dispatcher.scheduler.runCurrent()

        val state = viewModel.profileState.value
        assertTrue(state.isProcessing)
        assertEquals(OnboardingProcessingPhase.BUILDING_PROFILE_RESULT, state.processingPhase)
        assertFalse(state.hasExtractionResult)

        advanceUntilIdle()

        assertFalse(viewModel.profileState.value.isProcessing)
        assertTrue(viewModel.profileState.value.hasExtractionResult)
    }

    @Test
    fun `profile failure while recording returns to tap retry state and stale stop is no op`() = runTest {
        assertTrue(viewModel.startProfileRecording())

        speechRecognizer.emitEvent(
            DeviceSpeechRecognitionEvent.Failure(
                reason = DeviceSpeechFailureReason.ERROR,
                message = "录音输入异常，请检查麦克风权限或设备占用",
                backend = com.smartsales.prism.data.audio.DeviceSpeechBackend.FUN_ASR_REALTIME
            )
        )
        advanceUntilIdle()

        val failedState = viewModel.profileState.value
        assertFalse(failedState.isRecording)
        assertFalse(failedState.isProcessing)
        assertEquals("这次语音识别没有完成，请再试一次。", failedState.errorMessage)
        assertFalse(failedState.hasExtractionResult)
        assertEquals(OnboardingMicInteractionMode.TAP_TO_SEND, failedState.micInteractionMode)

        viewModel.finishProfileRecording()
        advanceUntilIdle()

        assertEquals(failedState, viewModel.profileState.value)
    }

    @Test
    fun `profile cancelled after processing begins clears processing and shows retry error`() = runTest {
        speechRecognizer.finishGate = CompletableDeferred()

        assertTrue(viewModel.startProfileRecording())
        viewModel.finishProfileRecording()
        dispatcher.scheduler.runCurrent()

        assertTrue(viewModel.profileState.value.isProcessing)
        assertEquals(
            OnboardingProcessingPhase.RECOGNIZING,
            viewModel.profileState.value.processingPhase
        )

        speechRecognizer.finishGate?.complete(
            DeviceSpeechRecognitionResult.Failure(
                reason = DeviceSpeechFailureReason.CANCELLED,
                message = "语音识别已取消"
            )
        )
        advanceUntilIdle()

        val state = viewModel.profileState.value
        assertFalse(state.isProcessing)
        assertEquals(OnboardingProcessingPhase.NONE, state.processingPhase)
        assertNull(state.transcriptOrigin)
        assertNull(state.generationOrigin)
        assertFalse(state.hasExtractionResult)
        assertEquals("这次语音识别没有完成，请再试一次。", state.errorMessage)
        assertEquals(OnboardingMicInteractionMode.TAP_TO_SEND, state.micInteractionMode)
    }

    @Test
    fun `profile llm failure keeps real transcript and surfaces retry without synthetic extraction`() = runTest {
        val transcript = "我是王经理，做 SaaS 销售总监 8 年了，平时主要用微信联系客户。"
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success(transcript)
        interactionService.profileResult =
            OnboardingProfileExtractionServiceResult.Failure("llm unavailable")

        assertTrue(viewModel.startProfileRecording())
        viewModel.finishProfileRecording()
        advanceUntilIdle()

        val extracted = viewModel.profileState.value
        assertEquals(transcript, extracted.transcript)
        assertEquals(OnboardingTranscriptOrigin.DEVICE_SPEECH, extracted.transcriptOrigin)
        assertNull(extracted.generationOrigin)
        assertNull(extracted.draft)
        assertEquals("", extracted.acknowledgement)
        assertEquals("当前 AI 资料提取暂时没有返回，请再试一次。", extracted.errorMessage)
    }

    @Test
    fun `profile llm deadline keeps real transcript and surfaces retry`() = runTest {
        val transcript = "我是王经理，做 SaaS 销售总监 8 年了，平时主要用微信联系客户。"
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success(transcript)
        interactionService.profileDelayMillis = 3_600L

        assertTrue(viewModel.startProfileRecording())
        viewModel.finishProfileRecording()

        dispatcher.scheduler.advanceTimeBy(3_499L)
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.profileState.value.isProcessing)
        assertEquals(
            OnboardingProcessingPhase.BUILDING_PROFILE_RESULT,
            viewModel.profileState.value.processingPhase
        )

        dispatcher.scheduler.advanceTimeBy(1L)
        advanceUntilIdle()

        val state = viewModel.profileState.value
        assertEquals(transcript, state.transcript)
        assertEquals(OnboardingTranscriptOrigin.DEVICE_SPEECH, state.transcriptOrigin)
        assertNull(state.generationOrigin)
        assertNull(state.draft)
        assertEquals("", state.acknowledgement)
        assertEquals("当前 AI 资料提取暂时没有返回，请再试一次。", state.errorMessage)
    }

    @Test
    fun `quick start uses real transcript service and keeps continue available for more edits`() = runTest {
        val createdItems = listOf(
            OnboardingQuickStartItem(
                stableId = "task-1",
                title = "带合同见老板",
                timeLabel = "09:00",
                dateLabel = "明天",
                dateIso = "2026-04-04",
                urgencyLevel = com.smartsales.prism.domain.scheduler.UrgencyLevel.L2_IMPORTANT,
                startHour = 9,
                startMinute = 0
            )
        )
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success("明天早上九点带合同见老板")
        quickStartReminderGuideCoordinator.nextGuide = notificationSettingsGuide()
        quickStartService.nextResult = OnboardingQuickStartServiceResult.Success(
            items = createdItems,
            touchedExactTask = true,
            mutationKind = OnboardingQuickStartServiceResult.Success.MutationKind.CREATE
        )

        assertTrue(viewModel.startQuickStartRecording())
        viewModel.finishQuickStartRecording()
        advanceUntilIdle()

        val state = viewModel.quickStartState.value
        assertEquals("明天早上九点带合同见老板", quickStartService.lastTranscript)
        assertEquals(createdItems, state.items)
        assertTrue(state.canContinue)
        assertEquals(OnboardingMicInteractionMode.TAP_TO_SEND, state.micInteractionMode)
        assertEquals(OnboardingTranscriptOrigin.DEVICE_SPEECH, state.lastTranscriptOrigin)
        assertEquals(OnboardingGenerationOrigin.SCHEDULER_PATH_A, state.lastGenerationOrigin)
        assertTrue(quickStartCommitter.stagedItems.isNotEmpty())
        assertEquals("应用通知被系统关闭", state.reminderGuide?.title)
        assertTrue(state.reminderGuidePrompted)
        assertEquals(1, quickStartReminderGuideCoordinator.consumeCalls)
        assertEquals(1, state.calendarPermissionRequestToken)
    }

    @Test
    fun `quick start exact success supports exact alarm reminder guide branch`() = runTest {
        val createdItems = listOf(
            OnboardingQuickStartItem(
                stableId = "task-1",
                title = "带合同见老板",
                timeLabel = "09:00",
                dateLabel = "明天",
                dateIso = "2026-04-04",
                urgencyLevel = com.smartsales.prism.domain.scheduler.UrgencyLevel.L2_IMPORTANT,
                startHour = 9,
                startMinute = 0
            )
        )
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success("明天早上九点带合同见老板")
        quickStartReminderGuideCoordinator.nextGuide = exactAlarmGuide()
        quickStartService.nextResult = OnboardingQuickStartServiceResult.Success(
            items = createdItems,
            touchedExactTask = true,
            mutationKind = OnboardingQuickStartServiceResult.Success.MutationKind.CREATE
        )

        assertTrue(viewModel.startQuickStartRecording())
        viewModel.finishQuickStartRecording()
        advanceUntilIdle()

        val state = viewModel.quickStartState.value
        assertEquals(OnboardingMicInteractionMode.TAP_TO_SEND, state.micInteractionMode)
        assertEquals(ReminderReliabilityAdvisor.Action.EXACT_ALARM, state.reminderGuide?.primaryAction)
        assertEquals("闹钟权限", state.reminderGuide?.primaryLabel)
    }

    @Test
    fun `quick start llm deadline keeps transcript and surfaces retry`() = runTest {
        val transcript = "后天联系王经理"
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success(transcript)
        quickStartService.delayMillis = 10_100L
        quickStartService.nextResult = OnboardingQuickStartServiceResult.Success(
            items = emptyList(),
            touchedExactTask = false,
            mutationKind = OnboardingQuickStartServiceResult.Success.MutationKind.CREATE
        )

        assertTrue(viewModel.startQuickStartRecording())
        viewModel.finishQuickStartRecording()

        dispatcher.scheduler.advanceTimeBy(4_999L)
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.quickStartState.value.isProcessing)
        assertEquals(
            OnboardingProcessingPhase.BUILDING_QUICK_START_RESULT,
            viewModel.quickStartState.value.processingPhase
        )

        dispatcher.scheduler.advanceTimeBy(5_001L)
        advanceUntilIdle()

        val state = viewModel.quickStartState.value
        assertEquals(OnboardingMicInteractionMode.TAP_TO_SEND, state.micInteractionMode)
        assertEquals(transcript, state.transcript)
        assertEquals(OnboardingTranscriptOrigin.DEVICE_SPEECH, state.lastTranscriptOrigin)
        assertNull(state.lastGenerationOrigin)
        assertEquals("当前日程整理暂时没有返回，请再试一次。", state.errorMessage)
        assertTrue(state.items.isEmpty())
        assertTrue(quickStartCommitter.stagedItems.isEmpty())
    }

    @Test
    fun `quick start tolerates slower shared scheduler routing within expanded deadline`() = runTest {
        val transcript = "明天早上九点带合同见老板"
        val createdItems = listOf(
            OnboardingQuickStartItem(
                stableId = "task-1",
                title = "带合同见老板",
                timeLabel = "09:00",
                dateLabel = "明天",
                dateIso = "2026-04-04",
                startHour = 9,
                startMinute = 0,
                urgencyLevel = com.smartsales.prism.domain.scheduler.UrgencyLevel.L2_IMPORTANT
            )
        )
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success(transcript)
        quickStartService.delayMillis = 7_000L
        quickStartService.nextResult = OnboardingQuickStartServiceResult.Success(
            items = createdItems,
            touchedExactTask = true,
            mutationKind = OnboardingQuickStartServiceResult.Success.MutationKind.CREATE
        )

        assertTrue(viewModel.startQuickStartRecording())
        viewModel.finishQuickStartRecording()

        dispatcher.scheduler.advanceTimeBy(6_999L)
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.quickStartState.value.isProcessing)

        dispatcher.scheduler.advanceTimeBy(1L)
        advanceUntilIdle()

        val state = viewModel.quickStartState.value
        assertEquals(createdItems, state.items)
        assertNull(state.errorMessage)
        assertEquals(OnboardingGenerationOrigin.SCHEDULER_PATH_A, state.lastGenerationOrigin)
        assertEquals(createdItems, quickStartCommitter.stagedItems)
    }

    @Test
    fun `quick start exception keeps transcript and surfaces retry`() = runTest {
        val transcript = "后天联系王经理"
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success(transcript)
        quickStartService.throwable = IllegalStateException("boom")

        assertTrue(viewModel.startQuickStartRecording())
        viewModel.finishQuickStartRecording()
        advanceUntilIdle()

        val state = viewModel.quickStartState.value
        assertEquals(OnboardingMicInteractionMode.TAP_TO_SEND, state.micInteractionMode)
        assertEquals(transcript, state.transcript)
        assertEquals(OnboardingTranscriptOrigin.DEVICE_SPEECH, state.lastTranscriptOrigin)
        assertNull(state.lastGenerationOrigin)
        assertEquals("当前日程整理暂时没有返回，请再试一次。", state.errorMessage)
        assertTrue(state.items.isEmpty())
        assertTrue(quickStartCommitter.stagedItems.isEmpty())
    }

    @Test
    fun `quick start realtime cancellation while recording returns to tap retry state`() = runTest {
        assertTrue(viewModel.startQuickStartRecording())

        speechRecognizer.emitEvent(DeviceSpeechRecognitionEvent.Cancelled)
        advanceUntilIdle()

        val state = viewModel.quickStartState.value
        assertFalse(state.isRecording)
        assertFalse(state.isProcessing)
        assertEquals(OnboardingMicInteractionMode.TAP_TO_SEND, state.micInteractionMode)
        assertEquals("这次语音识别没有完成，请再试一次。", state.errorMessage)
    }

    @Test
    fun `quick start permission grant records calendar sync capability without blocking flow`() {
        viewModel.onQuickStartCalendarPermissionResult(granted = true)

        val state = viewModel.quickStartState.value
        assertTrue(state.calendarPermissionGranted)
        assertEquals(OnboardingMicInteractionMode.TAP_TO_SEND, state.micInteractionMode)
        assertEquals("系统日历已开启，完成后会同步到系统日历。", state.transientNoticeMessage)
        assertNull(state.guidanceMessage)
    }

    @Test
    fun `quick start success clears stale calendar notice`() = runTest {
        viewModel.onQuickStartCalendarPermissionResult(granted = true)
        val createdItems = listOf(
            OnboardingQuickStartItem(
                stableId = "task-1",
                title = "带合同见老板",
                timeLabel = "09:00",
                dateLabel = "明天",
                dateIso = "2026-04-04",
                urgencyLevel = com.smartsales.prism.domain.scheduler.UrgencyLevel.L2_IMPORTANT,
                startHour = 9,
                startMinute = 0
            )
        )
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success("明天早上九点带合同见老板")
        quickStartService.nextResult = OnboardingQuickStartServiceResult.Success(
            items = createdItems,
            touchedExactTask = true,
            mutationKind = OnboardingQuickStartServiceResult.Success.MutationKind.CREATE
        )

        assertTrue(viewModel.startQuickStartRecording())
        viewModel.finishQuickStartRecording()
        advanceUntilIdle()

        val state = viewModel.quickStartState.value
        assertEquals(OnboardingMicInteractionMode.TAP_TO_SEND, state.micInteractionMode)
        assertNull(state.transientNoticeMessage)
        assertNull(state.guidanceMessage)
    }

    @Test
    fun `quick start vague success does not surface reminder guide`() = runTest {
        val createdItems = listOf(
            OnboardingQuickStartItem(
                stableId = "task-1",
                title = "联系王经理",
                timeLabel = "尽快",
                dateLabel = "后天",
                dateIso = "2026-04-05",
                urgencyLevel = com.smartsales.prism.domain.scheduler.UrgencyLevel.L2_IMPORTANT
            )
        )
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success("后天联系王经理")
        quickStartReminderGuideCoordinator.nextGuide = notificationSettingsGuide()
        quickStartService.nextResult = OnboardingQuickStartServiceResult.Success(
            items = createdItems,
            touchedExactTask = false,
            mutationKind = OnboardingQuickStartServiceResult.Success.MutationKind.CREATE
        )

        assertTrue(viewModel.startQuickStartRecording())
        viewModel.finishQuickStartRecording()
        advanceUntilIdle()

        val state = viewModel.quickStartState.value
        assertNull(state.reminderGuide)
        assertFalse(state.reminderGuidePrompted)
        assertEquals(OnboardingMicInteractionMode.TAP_TO_SEND, state.micInteractionMode)
        assertEquals(0, quickStartReminderGuideCoordinator.consumeCalls)
    }

    @Test
    fun `dismissing quick start reminder guide keeps staged items and continue state`() = runTest {
        val createdItems = listOf(
            OnboardingQuickStartItem(
                stableId = "task-1",
                title = "带合同见老板",
                timeLabel = "09:00",
                dateLabel = "明天",
                dateIso = "2026-04-04",
                urgencyLevel = com.smartsales.prism.domain.scheduler.UrgencyLevel.L2_IMPORTANT,
                startHour = 9,
                startMinute = 0
            )
        )
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success("明天早上九点带合同见老板")
        quickStartReminderGuideCoordinator.nextGuide = notificationSettingsGuide()
        quickStartService.nextResult = OnboardingQuickStartServiceResult.Success(
            items = createdItems,
            touchedExactTask = true,
            mutationKind = OnboardingQuickStartServiceResult.Success.MutationKind.CREATE
        )

        assertTrue(viewModel.startQuickStartRecording())
        viewModel.finishQuickStartRecording()
        advanceUntilIdle()

        viewModel.dismissQuickStartReminderGuide()

        val state = viewModel.quickStartState.value
        assertNull(state.reminderGuide)
        assertTrue(state.canContinue)
        assertEquals(createdItems, state.items)
    }

    @Test
    fun `opening quick start reminder action clears dialog and delegates to coordinator`() = runTest {
        val createdItems = listOf(
            OnboardingQuickStartItem(
                stableId = "task-1",
                title = "带合同见老板",
                timeLabel = "09:00",
                dateLabel = "明天",
                dateIso = "2026-04-04",
                urgencyLevel = com.smartsales.prism.domain.scheduler.UrgencyLevel.L2_IMPORTANT,
                startHour = 9,
                startMinute = 0
            )
        )
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success("明天早上九点带合同见老板")
        quickStartReminderGuideCoordinator.nextGuide = notificationSettingsGuide()
        quickStartService.nextResult = OnboardingQuickStartServiceResult.Success(
            items = createdItems,
            touchedExactTask = true,
            mutationKind = OnboardingQuickStartServiceResult.Success.MutationKind.CREATE
        )

        assertTrue(viewModel.startQuickStartRecording())
        viewModel.finishQuickStartRecording()
        advanceUntilIdle()

        viewModel.openQuickStartReminderAction(ReminderReliabilityAdvisor.Action.APP_NOTIFICATION_SETTINGS)

        val state = viewModel.quickStartState.value
        assertNull(state.reminderGuide)
        assertEquals(
            ReminderReliabilityAdvisor.Action.APP_NOTIFICATION_SETTINGS,
            quickStartReminderGuideCoordinator.openedActions.single()
        )
    }

    @Test
    fun `quick start reminder guide is not reconsumed after first exact success`() = runTest {
        val createdItems = listOf(
            OnboardingQuickStartItem(
                stableId = "task-1",
                title = "带合同见老板",
                timeLabel = "09:00",
                dateLabel = "明天",
                dateIso = "2026-04-04",
                urgencyLevel = com.smartsales.prism.domain.scheduler.UrgencyLevel.L2_IMPORTANT,
                startHour = 9,
                startMinute = 0
            )
        )
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success("明天早上九点带合同见老板")
        quickStartReminderGuideCoordinator.nextGuide = notificationSettingsGuide()
        quickStartService.nextResult = OnboardingQuickStartServiceResult.Success(
            items = createdItems,
            touchedExactTask = true,
            mutationKind = OnboardingQuickStartServiceResult.Success.MutationKind.CREATE
        )

        assertTrue(viewModel.startQuickStartRecording())
        viewModel.finishQuickStartRecording()
        advanceUntilIdle()
        viewModel.dismissQuickStartReminderGuide()

        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success("改到明天上午十点")
        quickStartReminderGuideCoordinator.nextGuide = exactAlarmGuide()
        quickStartService.nextResult = OnboardingQuickStartServiceResult.Success(
            items = createdItems.map {
                it.copy(timeLabel = "10:00", startHour = 10, highlightToken = 1)
            },
            touchedExactTask = true,
            mutationKind = OnboardingQuickStartServiceResult.Success.MutationKind.UPDATE
        )

        assertTrue(viewModel.startQuickStartRecording())
        viewModel.finishQuickStartRecording()
        advanceUntilIdle()

        val state = viewModel.quickStartState.value
        assertNull(state.reminderGuide)
        assertEquals(1, quickStartReminderGuideCoordinator.consumeCalls)
    }

    @Test
    fun `finalization commits quick start marks shell handoff and clears sandbox for unified onboarding`() = runTest {
        viewModel.bindHost(OnboardingHost.SIM_CONNECTIVITY)
        val createdItems = listOf(
            OnboardingQuickStartItem(
                stableId = "task-1",
                title = "带合同见老板",
                timeLabel = "09:00",
                dateLabel = "明天",
                dateIso = "2026-04-04",
                urgencyLevel = com.smartsales.prism.domain.scheduler.UrgencyLevel.L2_IMPORTANT,
                startHour = 9,
                startMinute = 0
            )
        )
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success("明天早上九点带合同见老板")
        quickStartService.nextResult = OnboardingQuickStartServiceResult.Success(
            items = createdItems,
            touchedExactTask = true,
            mutationKind = OnboardingQuickStartServiceResult.Success.MutationKind.CREATE
        )
        assertTrue(viewModel.startQuickStartRecording())
        viewModel.finishQuickStartRecording()
        advanceUntilIdle()
        quickStartCommitter.nextResult =
            OnboardingSchedulerQuickStartCommitResult.Success(taskIds = listOf("task-1"))
        viewModel.onQuickStartCalendarPermissionResult(granted = true)

        val error = viewModel.finalizeFullAppCompletion()

        assertNull(error)
        assertEquals(1, quickStartCommitter.commitCalls)
        assertTrue(quickStartCommitter.cleared)
        assertTrue(onboardingHandoffGate.pending)
        assertEquals(listOf("task-1"), quickStartCalendarExporter.exportedTaskIds)
        assertTrue(viewModel.quickStartState.value.items.isEmpty())
    }

    @Test
    fun `finalization failure keeps staged state for retry and does not arm shell handoff`() = runTest {
        viewModel.bindHost(OnboardingHost.SIM_CONNECTIVITY)
        quickStartCommitter.nextResult =
            OnboardingSchedulerQuickStartCommitResult.Failure("体验日程同步失败，请稍后重试。")
        quickStartCommitter.stagedItems = listOf(
            OnboardingQuickStartItem(
                stableId = "task-1",
                title = "带合同见老板",
                timeLabel = "09:00",
                dateLabel = "明天",
                dateIso = "2026-04-04",
                urgencyLevel = com.smartsales.prism.domain.scheduler.UrgencyLevel.L2_IMPORTANT,
                startHour = 9,
                startMinute = 0
            )
        )
        quickStartService.nextResult = OnboardingQuickStartServiceResult.Success(
            items = quickStartCommitter.stagedItems,
            touchedExactTask = true,
            mutationKind = OnboardingQuickStartServiceResult.Success.MutationKind.CREATE
        )
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success("明天早上九点带合同见老板")
        assertTrue(viewModel.startQuickStartRecording())
        viewModel.finishQuickStartRecording()
        advanceUntilIdle()

        val error = viewModel.finalizeFullAppCompletion()

        assertEquals("体验日程同步失败，请稍后重试。", error)
        assertEquals(1, quickStartCommitter.commitCalls)
        assertFalse(onboardingHandoffGate.pending)
        assertFalse(viewModel.quickStartState.value.items.isEmpty())
    }

    @Test
    fun `finalization blocks incomplete quick start sandbox`() = runTest {
        viewModel.bindHost(OnboardingHost.SIM_CONNECTIVITY)

        val error = viewModel.finalizeFullAppCompletion()

        assertEquals("请先完成一次真实日程体验。", error)
        assertEquals(0, quickStartCommitter.commitCalls)
        assertFalse(onboardingHandoffGate.pending)
    }

    @Test
    fun `reset invalidates stale consultation result`() = runTest {
        val delayedReply = CompletableDeferred<OnboardingConsultationServiceResult>()
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success("客户预算批不下来")
        interactionService.consultationGate = delayedReply

        assertTrue(viewModel.startConsultationRecording())
        viewModel.finishConsultationRecording()
        dispatcher.scheduler.runCurrent()

        assertTrue(viewModel.consultationState.value.isProcessing)

        viewModel.resetInteractionState()
        delayedReply.complete(OnboardingConsultationServiceResult.Success("这条旧结果不该落地。"))
        dispatcher.scheduler.runCurrent()

        val state = viewModel.consultationState.value
        assertFalse(state.isProcessing)
        assertTrue(state.messages.isEmpty())
        assertEquals(0, state.completedRounds)
        assertNull(state.errorMessage)
    }

    @Test
    fun `save profile preserves current experience level when years are not parseable`() = runTest {
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success("我做销售很多年了，平时用电话。")
        interactionService.profileResult = OnboardingProfileExtractionServiceResult.Success(
            acknowledgement = "已完成识别。",
            draft = OnboardingProfileDraft(
                displayName = "赵经理",
                role = "销售经理",
                industry = "制造业",
                experienceYears = "很多年",
                communicationPlatform = "电话"
            )
        )

        assertTrue(viewModel.startProfileRecording())
        viewModel.finishProfileRecording()
        advanceUntilIdle()
        viewModel.saveProfileDraft()
        advanceUntilIdle()

        assertEquals("intermediate", repository.profile.value.experienceLevel)
    }

    @Test
    fun `cancel active recording clears pending permission and interaction mode`() {
        viewModel.onConsultationMicPermissionRequested()
        viewModel.onConsultationMicPermissionResult(granted = true)
        viewModel.onQuickStartMicPermissionRequested()
        viewModel.onQuickStartMicPermissionResult(granted = true)
        assertFalse(viewModel.consultationState.value.isRecording)

        viewModel.cancelActiveRecording()

        val consultation = viewModel.consultationState.value
        val profile = viewModel.profileState.value
        val quickStart = viewModel.quickStartState.value
        assertFalse(consultation.isRecording)
        assertFalse(consultation.awaitingMicPermission)
        assertNull(consultation.guidanceMessage)
        assertEquals(OnboardingMicInteractionMode.TAP_TO_SEND, consultation.micInteractionMode)
        assertEquals(OnboardingProcessingPhase.NONE, consultation.processingPhase)
        assertFalse(profile.awaitingMicPermission)
        assertNull(profile.guidanceMessage)
        assertEquals(OnboardingMicInteractionMode.TAP_TO_SEND, profile.micInteractionMode)
        assertFalse(quickStart.awaitingMicPermission)
        assertNull(quickStart.guidanceMessage)
        assertEquals(OnboardingMicInteractionMode.TAP_TO_SEND, quickStart.micInteractionMode)
        assertFalse(speechRecognizer.isListening())
    }

    @Test
    fun `deriveExperienceLevel maps expected bands`() {
        assertEquals("beginner", deriveExperienceLevel("expert", "1年"))
        assertEquals("intermediate", deriveExperienceLevel("beginner", "5年"))
        assertEquals("expert", deriveExperienceLevel("beginner", "8年"))
        assertEquals("expert", deriveExperienceLevel("expert", "很多年"))
    }

    private class FakeDeviceSpeechRecognizer : DeviceSpeechRecognizer {
        var startFailure: Exception? = null
        var nextResult: DeviceSpeechRecognitionResult = DeviceSpeechRecognitionResult.Success("")
        var finishDelayMillis: Long = 0L
        var finishGate: CompletableDeferred<DeviceSpeechRecognitionResult>? = null
        var lastMode: DeviceSpeechMode? = null
        private var listening = false
        private val mutableEvents = MutableSharedFlow<DeviceSpeechRecognitionEvent>(extraBufferCapacity = 8)

        override val events: Flow<DeviceSpeechRecognitionEvent> = mutableEvents

        override fun startListening(mode: DeviceSpeechMode) {
            startFailure?.let { throw it }
            lastMode = mode
            listening = true
        }

        override suspend fun finishListening(): DeviceSpeechRecognitionResult {
            finishGate?.let {
                val result = it.await()
                listening = false
                return result
            }
            if (finishDelayMillis > 0L) {
                delay(finishDelayMillis)
            }
            listening = false
            return nextResult
        }

        override fun cancelListening() {
            listening = false
            finishGate?.takeIf { !it.isCompleted }?.complete(
                DeviceSpeechRecognitionResult.Failure(
                    reason = DeviceSpeechFailureReason.CANCELLED,
                    message = "语音识别已取消"
                )
            )
        }

        override fun isListening(): Boolean = listening

        fun emitEvent(event: DeviceSpeechRecognitionEvent) {
            mutableEvents.tryEmit(event)
        }
    }

    private class FakeOnboardingInteractionService : OnboardingInteractionService {
        var consultationResult: OnboardingConsultationServiceResult =
            OnboardingConsultationServiceResult.Success("继续说说你当前遇到的卡点。")
        var consultationDelayMillis: Long = 0L
        var consultationGate: CompletableDeferred<OnboardingConsultationServiceResult>? = null

        var profileResult: OnboardingProfileExtractionServiceResult =
            OnboardingProfileExtractionServiceResult.Success(
                acknowledgement = "已完成识别。",
                draft = OnboardingProfileDraft(
                    displayName = "Frank",
                    role = "经理",
                    industry = "科技",
                    experienceYears = "3年",
                    communicationPlatform = "微信"
                )
            )
        var profileDelayMillis: Long = 0L

        override suspend fun generateConsultationReply(
            transcript: String,
            round: Int
        ): OnboardingConsultationServiceResult {
            consultationGate?.let { return it.await() }
            if (consultationDelayMillis > 0L) {
                delay(consultationDelayMillis)
            }
            return consultationResult
        }

        override suspend fun extractProfile(
            transcript: String
        ): OnboardingProfileExtractionServiceResult {
            if (profileDelayMillis > 0L) {
                delay(profileDelayMillis)
            }
            return profileResult
        }
    }

    private class FakeOnboardingSchedulerQuickStartCommitter :
        OnboardingSchedulerQuickStartCommitter {
        var stagedItems: List<OnboardingQuickStartItem> = emptyList()
        var nextResult: OnboardingSchedulerQuickStartCommitResult =
            OnboardingSchedulerQuickStartCommitResult.Success(emptyList())
        var commitCalls: Int = 0
        var cleared: Boolean = false

        override fun stage(items: List<OnboardingQuickStartItem>) {
            stagedItems = items
            cleared = false
        }

        override fun clear() {
            stagedItems = emptyList()
            cleared = true
        }

        override suspend fun commitIfNeeded(): OnboardingSchedulerQuickStartCommitResult {
            commitCalls += 1
            return nextResult
        }
    }

    private class FakeOnboardingQuickStartService : OnboardingQuickStartService {
        var nextResult: OnboardingQuickStartServiceResult =
            OnboardingQuickStartServiceResult.Failure("not configured")
        var lastTranscript: String? = null
        var delayMillis: Long = 0L
        var throwable: Throwable? = null

        override suspend fun applyTranscript(
            transcript: String,
            currentItems: List<OnboardingQuickStartItem>
        ): OnboardingQuickStartServiceResult {
            lastTranscript = transcript
            if (delayMillis > 0L) {
                delay(delayMillis)
            }
            throwable?.let { throw it }
            return nextResult
        }
    }

    private class FakeOnboardingQuickStartReminderGuideCoordinator :
        OnboardingQuickStartReminderGuideCoordinator {
        var nextGuide: ReminderReliabilityAdvisor.ReminderReliabilityGuide? = null
        var consumeCalls = 0
        val openedActions = mutableListOf<ReminderReliabilityAdvisor.Action>()

        override fun consumeGuideIfNeeded(): ReminderReliabilityAdvisor.ReminderReliabilityGuide? {
            consumeCalls += 1
            return nextGuide
        }

        override fun openAction(action: ReminderReliabilityAdvisor.Action): Boolean {
            openedActions += action
            return true
        }
    }

    private class FakeOnboardingQuickStartCalendarExporter :
        OnboardingQuickStartCalendarExporter {
        var exportedTaskIds: List<String> = emptyList()

        override suspend fun exportCommittedTaskIds(taskIds: List<String>): Boolean {
            exportedTaskIds = taskIds
            return true
        }
    }

    private class FakeRuntimeOnboardingHandoffGate : RuntimeOnboardingHandoffGate {
        var pending = false

        override fun shouldAutoOpenSchedulerAfterOnboarding(): Boolean = pending

        override fun markSchedulerAutoOpenPending() {
            pending = true
        }

        override fun consumeSchedulerAutoOpenPending() {
            pending = false
        }
    }

    private class FakeTimeProvider : TimeProvider {
        override val now: Instant = Instant.parse("2026-04-03T08:00:00Z")
        override val today: LocalDate = LocalDate.parse("2026-04-03")
        override val currentTime: LocalTime = LocalTime.of(16, 0)
        override val zoneId: ZoneId = ZoneId.of("Asia/Shanghai")

        override fun formatForLlm(): String = "2026年4月3日（周五）16:00"
    }

    private fun notificationSettingsGuide() = ReminderReliabilityAdvisor.ReminderReliabilityGuide(
        title = "应用通知被系统关闭",
        message = "系统当前已禁用本应用通知。即使闹钟已触发、Receiver 已运行，系统仍会直接拦截展示。",
        checklist = listOf("系统已关闭本应用通知，请先在应用通知设置中重新开启，否则提醒会被系统直接拦截。"),
        primaryAction = ReminderReliabilityAdvisor.Action.APP_NOTIFICATION_SETTINGS,
        primaryLabel = "通知设置",
        secondaryAction = ReminderReliabilityAdvisor.Action.AUTO_START,
        secondaryLabel = "自启动"
    )

    private fun exactAlarmGuide() = ReminderReliabilityAdvisor.ReminderReliabilityGuide(
        title = "精确闹钟权限",
        message = "未授予精确闹钟权限时，提醒可能延迟最多 1 小时。",
        checklist = listOf("开启“闹钟和提醒 / 精确闹钟”权限，避免提醒被延迟。"),
        primaryAction = ReminderReliabilityAdvisor.Action.EXACT_ALARM,
        primaryLabel = "闹钟权限"
    )

    private class FakeUserProfileRepository : UserProfileRepository {
        private val state = MutableStateFlow(
            UserProfile(
                id = 1,
                displayName = "Initial",
                role = "sales",
                industry = "Retail",
                experienceLevel = "intermediate",
                preferredLanguage = "zh-CN",
                updatedAt = 1L,
                subscriptionTier = SubscriptionTier.PRO,
                communicationPlatform = "微信",
                experienceYears = "3年"
            )
        )

        override val profile: StateFlow<UserProfile> = state

        override suspend fun getProfile(): UserProfile = state.value

        override suspend fun updateProfile(profile: UserProfile) {
            state.value = profile
        }
    }
}
