package com.smartsales.prism.ui

import com.smartsales.core.pipeline.IntentOrchestrator
import com.smartsales.core.pipeline.ToolRegistry
import com.smartsales.core.pipeline.AgentActivityController
import com.smartsales.core.pipeline.MascotService
import com.smartsales.prism.domain.repository.HistoryRepository
import com.smartsales.prism.domain.repository.UserProfileRepository
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.system.SystemEventBus
import com.smartsales.prism.domain.audio.AudioRepository
import com.smartsales.core.context.ContextBuilder
import com.smartsales.prism.domain.memory.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class AgentViewModelTest {

    @Mock private lateinit var intentOrchestrator: IntentOrchestrator
    @Mock private lateinit var historyRepository: HistoryRepository
    @Mock private lateinit var userProfileRepository: UserProfileRepository
    @Mock private lateinit var scheduledTaskRepository: ScheduledTaskRepository
    @Mock private lateinit var activityController: AgentActivityController
    @Mock private lateinit var mascotService: MascotService
    @Mock private lateinit var eventBus: SystemEventBus
    @Mock private lateinit var audioRepository: AudioRepository
    @Mock private lateinit var contextBuilder: ContextBuilder
    @Mock private lateinit var toolRegistry: ToolRegistry

    private lateinit var viewModel: AgentViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Basic default mocks
        `when`(userProfileRepository.profile).thenReturn(MutableStateFlow(mock(UserProfile::class.java)))
        `when`(activityController.activity).thenReturn(MutableStateFlow(null))
        `when`(mascotService.state).thenReturn(MutableStateFlow(com.smartsales.core.pipeline.MascotState.Hidden))
        `when`(scheduledTaskRepository.queryByDateRange(any(), any())).thenReturn(flowOf(emptyList()))

        viewModel = AgentViewModel(
            intentOrchestrator = intentOrchestrator,
            historyRepository = historyRepository,
            userProfileRepository = userProfileRepository,
            scheduledTaskRepository = scheduledTaskRepository,
            activityController = activityController,
            mascotService = mascotService,
            eventBus = eventBus,
            audioRepository = audioRepository,
            contextBuilder = contextBuilder,
            toolRegistry = toolRegistry
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `send updates text and delegates to intentOrchestrator`() = runTest {
        // Arrange
        val input = "Hello"
        `when`(intentOrchestrator.processInput(input)).thenReturn(flowOf())

        // Act
        viewModel.updateInput(input)
        viewModel.send()
        advanceUntilIdle()

        // Assert
        assertEquals("", viewModel.inputText.value)
        verify(intentOrchestrator).processInput(input)
    }
}
