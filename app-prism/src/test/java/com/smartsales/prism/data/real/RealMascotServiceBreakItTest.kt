package com.smartsales.prism.data.real

import com.smartsales.prism.domain.mascot.MascotInteraction
import com.smartsales.prism.domain.mascot.MascotState
import com.smartsales.prism.domain.pipeline.Executor
import com.smartsales.prism.domain.pipeline.ExecutorResult
import com.smartsales.prism.domain.pipeline.TokenUsage
import com.smartsales.prism.domain.memory.UserProfile
import com.smartsales.prism.domain.repository.UserProfileRepository
import com.smartsales.prism.domain.system.SystemEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class RealMascotServiceBreakItTest {

    private lateinit var eventBus: RealSystemEventBus
    private lateinit var executor: Executor
    private lateinit var userProfileRepo: UserProfileRepository
    private lateinit var mascotService: RealMascotService

    @Before
    fun setup() {
        eventBus = RealSystemEventBus()
        executor = mock()
        userProfileRepo = mock()
        
        whenever(userProfileRepo.profile).thenReturn(MutableStateFlow(UserProfile(
            displayName = "TestUser",
            role = "Agent",
            industry = "Tech",
            experienceLevel = "Senior",
            updatedAt = 0L
        )))
        
        mascotService = RealMascotService(eventBus, executor, userProfileRepo)
        mascotService.startObserving()
    }

    @Test
    fun `break-it multiple rapid AppIdle events only prompt once`() = runBlocking {
        delay(50) // Allow IO dispatcher to subscribe
        whenever(executor.execute(any(), any())).thenReturn(
            ExecutorResult.Success("Hello from AI", TokenUsage(100, 50))
        )
        
        // Rapidly fire 5 AppIdle events
        repeat(5) {
            eventBus.publish(SystemEvent.AppIdle)
        }
        
        // Allow routines to run in real time for Flow collection
        delay(200)
        
        // Executor should only have been called ONCE due to the latch
        verify(executor, times(1)).execute(any(), any())
        
        assertTrue(mascotService.state.value is MascotState.Active)
        assertEquals("Hello from AI", (mascotService.state.value as MascotState.Active).message)
    }

    @Test
    fun `break-it any interaction resets the AppIdle latch`() = runBlocking {
        delay(50) // Allow IO dispatcher to subscribe
        whenever(executor.execute(any(), any())).thenReturn(
            ExecutorResult.Success("First", TokenUsage())
        )
        
        // 1. First Idle
        eventBus.publish(SystemEvent.AppIdle)
        delay(200)
        verify(executor, times(1)).execute(any(), any())
        
        // 2. Second Idle shouldn't trigger
        eventBus.publish(SystemEvent.AppIdle)
        delay(200)
        verify(executor, times(1)).execute(any(), any()) // Still 1
        
        // 3. User interacts (resets latch)
        mascotService.interact(MascotInteraction.Tap)
        
        // Give new prompt
        whenever(executor.execute(any(), any())).thenReturn(
            ExecutorResult.Success("Second", TokenUsage())
        )
        
        // 4. Third Idle should trigger now
        eventBus.publish(SystemEvent.AppIdle)
        delay(200)
        verify(executor, times(2)).execute(any(), any()) // Now 2!
        
        assertEquals("Second", (mascotService.state.value as MascotState.Active).message)
    }
}
