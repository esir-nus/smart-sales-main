package com.smartsales.prism.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * PipelineEvent 编译测试
 * 
 * Wave 1: 验证所有变体可编译
 */
class PipelineEventTest {
    
    @Test
    fun `all event variants compile`() {
        val events = listOf(
            PipelineEvent.RecordingStarted,
            PipelineEvent.Downloading("test.wav"),
            PipelineEvent.Transcribing("test.wav", 1024L),
            PipelineEvent.Processing("hello"),
            PipelineEvent.Complete(
                SchedulerResult.TaskCreated("1", "Test Task", dayOffset = 1, scheduledAtMillis = System.currentTimeMillis(), durationMinutes = 30),
                "test.wav",
                "hello"
            ),
            PipelineEvent.Error(PipelineEvent.Stage.DOWNLOAD, "err", null)
        )
        
        assertEquals(6, events.size)
        assertNotNull(events[0])
    }
    
    @Test
    fun `all SchedulerResult variants compile`() {
        val results = listOf(
            SchedulerResult.TaskCreated("1", "Meeting", dayOffset = 0, scheduledAtMillis = System.currentTimeMillis(), durationMinutes = 60),
            SchedulerResult.MultiTaskCreated(listOf(
                SchedulerResult.TaskCreated("1", "Task A", dayOffset = 0, scheduledAtMillis = System.currentTimeMillis(), durationMinutes = 30),
                SchedulerResult.TaskCreated("2", "Task B", dayOffset = 1, scheduledAtMillis = System.currentTimeMillis() + 86_400_000, durationMinutes = 60)
            )),
            SchedulerResult.InspirationSaved("inspiration-1"),
            SchedulerResult.AwaitingClarification("请问时间？")
        )
        
        assertEquals(4, results.size)
    }
    
    @Test
    fun `all PipelineState values compile`() {
        val states = PipelineState.values()
        assertEquals(4, states.size)
    }
}
