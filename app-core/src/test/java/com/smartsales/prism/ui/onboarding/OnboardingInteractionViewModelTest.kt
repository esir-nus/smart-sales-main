package com.smartsales.prism.ui.onboarding

import com.smartsales.prism.domain.asr.AsrResult
import com.smartsales.prism.domain.asr.AsrService
import com.smartsales.prism.domain.config.SubscriptionTier
import com.smartsales.prism.domain.memory.UserProfile
import com.smartsales.prism.domain.repository.UserProfileRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingInteractionViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var audioCapture: FakeOnboardingAudioCapture
    private lateinit var asrService: FakeAsrService
    private lateinit var interactionService: FakeOnboardingInteractionService
    private lateinit var repository: FakeUserProfileRepository
    private lateinit var viewModel: OnboardingInteractionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        audioCapture = FakeOnboardingAudioCapture()
        asrService = FakeAsrService()
        interactionService = FakeOnboardingInteractionService()
        repository = FakeUserProfileRepository()
        viewModel = OnboardingInteractionViewModel(
            audioCapture = audioCapture,
            asrService = asrService,
            interactionService = interactionService,
            userProfileRepository = repository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `consultation success appends transcript and ai reply`() = runTest {
        asrService.nextResult = AsrResult.Success("客户预算批不下来")
        interactionService.consultationResult =
            OnboardingConsultationServiceResult.Success("可以先从同行成功案例切入。")

        assertTrue(viewModel.startConsultationRecording())
        viewModel.finishConsultationRecording()
        advanceUntilIdle()

        val state = viewModel.consultationState.value
        assertEquals(1, state.completedRounds)
        assertEquals(2, state.messages.size)
        assertEquals("客户预算批不下来", state.messages[0].text)
        assertEquals("可以先从同行成功案例切入。", state.messages[1].text)
        assertFalse(state.isProcessing)
    }

    @Test
    fun `short consultation transcript surfaces retryable error`() = runTest {
        asrService.nextResult = AsrResult.Success("嗯")

        assertTrue(viewModel.startConsultationRecording())
        viewModel.finishConsultationRecording()
        advanceUntilIdle()

        assertEquals("录音太短，请多说一点", viewModel.consultationState.value.errorMessage)
        assertEquals(0, viewModel.consultationState.value.completedRounds)
    }

    @Test
    fun `profile extraction success exposes draft and acknowledgement`() = runTest {
        asrService.nextResult = AsrResult.Success("我是王经理，做 SaaS 销售总监 8 年了，平时用微信。")
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
        assertTrue(state.hasExtractionResult)
    }

    @Test
    fun `save profile derives experience level from years`() = runTest {
        asrService.nextResult = AsrResult.Success("我是王经理，做 SaaS 销售总监 8 年了，平时用微信。")
        interactionService.profileResult = OnboardingProfileExtractionServiceResult.Success(
            acknowledgement = "已完成识别。",
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
        viewModel.saveProfileDraft()
        advanceUntilIdle()

        val profile = repository.profile.value
        assertEquals("王经理", profile.displayName)
        assertEquals("expert", profile.experienceLevel)
        assertEquals("8年", profile.experienceYears)
    }

    @Test
    fun `save profile preserves current experience level when years are not parseable`() = runTest {
        asrService.nextResult = AsrResult.Success("我做销售很多年了，平时用电话。")
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
    fun `skip after profile failure leaves repository unchanged`() = runTest {
        asrService.nextResult = AsrResult.Success("我是王经理")
        interactionService.profileResult =
            OnboardingProfileExtractionServiceResult.Failure("资料提取结果暂时不可用，请重试。")

        assertTrue(viewModel.startProfileRecording())
        viewModel.finishProfileRecording()
        advanceUntilIdle()
        viewModel.skipProfileSave()
        advanceUntilIdle()

        val profile = repository.profile.value
        assertEquals("Initial", profile.displayName)
        assertEquals("intermediate", profile.experienceLevel)
    }

    @Test
    fun `deriveExperienceLevel maps expected bands`() {
        assertEquals("beginner", deriveExperienceLevel("expert", "1年"))
        assertEquals("intermediate", deriveExperienceLevel("beginner", "5年"))
        assertEquals("expert", deriveExperienceLevel("beginner", "8年"))
        assertEquals("expert", deriveExperienceLevel("expert", "很多年"))
    }

    private class FakeOnboardingAudioCapture : OnboardingAudioCapture {
        private var recording = false
        private var fileCounter = 0

        override fun startRecording() {
            recording = true
        }

        override fun stopRecording(): File {
            recording = false
            val file = kotlin.io.path.createTempFile("onboarding-test-${fileCounter++}", ".wav").toFile()
            file.writeText("wav")
            return file
        }

        override fun cancelRecording() {
            recording = false
        }

        override fun isRecording(): Boolean = recording
    }

    private class FakeAsrService : AsrService {
        var nextResult: AsrResult = AsrResult.Success("")

        override suspend fun transcribe(file: File): AsrResult = nextResult

        override suspend fun isAvailable(): Boolean = true
    }

    private class FakeOnboardingInteractionService : OnboardingInteractionService {
        var consultationResult: OnboardingConsultationServiceResult =
            OnboardingConsultationServiceResult.Success("继续说说你当前遇到的卡点。")
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

        override suspend fun generateConsultationReply(
            transcript: String,
            round: Int
        ): OnboardingConsultationServiceResult = consultationResult

        override suspend fun extractProfile(
            transcript: String
        ): OnboardingProfileExtractionServiceResult = profileResult
    }

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
