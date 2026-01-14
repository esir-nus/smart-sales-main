// File: data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/polling/FakePollingLoop.kt
// Module: :data:ai-core
// Summary: Fake implementation of PollingLoop for testing (Lattice pattern)
// Author: created on 2026-01-14

package com.smartsales.data.aicore.tingwu.polling

import com.smartsales.data.aicore.TingwuJobState
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * FakePollingLoop: Test double for PollingLoop.
 * 
 * Provides stub behavior, call tracking, and reset for test isolation.
 */
class FakePollingLoop : PollingLoop {
    
    /** Override to inject specific terminal state. */
    var stubTerminalState: TingwuJobState? = null
    
    /** Track all poll calls. */
    val calls = mutableListOf<String>()
    
    override suspend fun poll(
        jobId: String,
        stateFlow: MutableStateFlow<TingwuJobState>,
        onTerminal: suspend (TingwuJobState) -> Unit
    ) {
        calls.add(jobId)
        
        val terminal = stubTerminalState ?: TingwuJobState.Completed(
            jobId = jobId,
            transcriptMarkdown = "Fake transcript for $jobId"
        )
        
        stateFlow.value = terminal
        onTerminal(terminal)
    }
    
    /** Reset state between tests. */
    fun reset() {
        stubTerminalState = null
        calls.clear()
    }
}
