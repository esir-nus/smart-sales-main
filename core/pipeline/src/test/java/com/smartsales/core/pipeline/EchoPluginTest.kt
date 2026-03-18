package com.smartsales.core.pipeline

import com.smartsales.prism.domain.model.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * A fake Gateway provided to plugins by the Orchestrator/Test-Harness.
 * It enforces permissions at runtime.
 */
class FakePluginGateway(
    private val grantedPermissions: Set<CoreModulePermission>
) : PluginGateway {

    override suspend fun getSessionHistory(turns: Int): String {
        if (!grantedPermissions.contains(CoreModulePermission.READ_SESSION_HISTORY)) {
            throw SecurityException("Plugin does not have READ_SESSION_HISTORY permission")
        }
        return "Simulated Session History (Last $turns turns)"
    }

    override suspend fun emitProgress(message: String) {
        // In a real app, this would push transient events to the chat UI
        println("FakePluginGateway: Transient Broadcast: $message")
    }
}

/**
 * The Test Plugin that proves the architecture.
 * Notice it has NO dependencies injected in its constructor!
 */
class EchoPlugin : PrismPlugin {
    override val metadata = AnalystTool(
        id = "ECHO_TEST",
        label = "Echo Test Tool",
        description = "A diagnostic tool to test the Gateway socket.",
        icon = "plug"
    )
    
    // The Manifest: Declaring what it needs.
    override val requiredPermissions = setOf(CoreModulePermission.READ_SESSION_HISTORY)

    override fun execute(request: PluginRequest, gateway: PluginGateway): Flow<UiState> = flow {
        // 1. Acknowledge start instantly
        emit(UiState.Thinking("Starting Echo Protocol..."))
        
        try {
            // 2. Do work using the provided Gateway (Simulating a DB call + Delay)
            gateway.emitProgress("Accessing History...")
            val history = gateway.getSessionHistory(5)
            delay(50L) // Simulate some work
            
            // 3. Complete work
            val messageToEcho = request.parameters["message"]?.toString() ?: "No message"
            val finalResult = "Echo: $messageToEcho\nContext: $history"
            
            emit(UiState.Response(finalResult))
        } catch (e: SecurityException) {
            // 4. Handle Permission Failures gracefully
            emit(UiState.Error("Plugin Execution Failed: ${e.message}", retryable = false))
        }
    }
}

class EchoPluginTest {

    @Test
    fun test_execute_with_permission_emits_success() = runTest {
        // Arrange
        val plugin = EchoPlugin()
        val request = PluginRequest(
            rawInput = "Run echo test",
            parameters = mapOf("message" to "Hello Gateway!")
        )
        // Construct the Sandbox Gateway with the required permissions
        val gateway = FakePluginGateway(plugin.requiredPermissions)

        // Act
        val states = plugin.execute(request, gateway).toList()

        // Assert
        assert(states.size == 2) { "Expected 2 states" }
        assert(states[0] is UiState.Thinking) { "Expected Thinking state" }
        assert("Starting Echo Protocol..." == (states[0] as UiState.Thinking).hint)
        
        assert(states[1] is UiState.Response) { "Expected Response state" }
        val successText = (states[1] as UiState.Response).content
        assert(successText.contains("Echo: Hello Gateway!"))
        assert(successText.contains("Simulated Session History"))
    }

    @Test
    fun test_gateway_throws_exception_if_permission_missing() = runTest {
        // Arrange
        val plugin = EchoPlugin()
        val request = PluginRequest(
            rawInput = "Run echo test",
            parameters = mapOf("message" to "Hack the Gibson")
        )
        
        // Emulate a system that didn't grant the permission
        val gateway = FakePluginGateway(emptySet())

        // Act
        val states = plugin.execute(request, gateway).toList()

        // Assert
        assert(states.size == 2) { "Expected 2 states" }
        assert(states[0] is UiState.Thinking) { "Expected Thinking state" }
        
        assert(states[1] is UiState.Error) { "Expected Error state" }
        val errorState = states[1] as UiState.Error
        assert(errorState.message.contains("does not have READ_SESSION_HISTORY permission"))
    }
}
