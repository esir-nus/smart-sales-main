# Test Infrastructure Interface

> **Owner**: `:core:test-fakes`
> **Consumers**: All `src/test/` source sets across `:core`, `:data`, `:domain`, and `app-core`.

## Public Interface

```kotlin
/**
 * The unified testing rig that replaces fragmented Mockito setups.
 * Automatically injects centralized, state-backed Fakes and manages
 * Coroutine test scopes to prevent `runBlocking` crashes.
 */
interface PrismTestRig {
    val fakeHistoryRepository: FakeHistoryRepository
    val fakeEntityRegistry: FakeEntityRegistry
    val fakeEntityWriter: FakeEntityWriter
    val fakeMemoryRepository: FakeMemoryRepository
    
    // Core setup/teardown mechanics for L2 Simulation
    fun setup()
    fun tearDown()
}
```

## Architectural Contract

1. **No Production Leaks**: This module is strictly deployed via `testImplementation`. Production code (`src/main/`) must NEVER import `:core:test-fakes`.
2. **Fakes Over Mocks**: This API strictly enforces the use of fully fleshed-out Fake implementations holding real Kotlin state (e.g., `MutableStateFlow` and internal `HashMap` collections). Generic Mockito `whenever()` stubs are forbidden for Layer 2 simulated testing logic to prevent "Testing Illusions".
