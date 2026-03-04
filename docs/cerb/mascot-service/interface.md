# MascotService Interface (Blackbox Contract)

> **Owner**: Mascot (System I)
> **Consumers**: Top-level UI (for rendering), EventBus (for observing triggers)

## Public Interface

```kotlin
interface MascotService {
    /**
     * Initializes the Mascot service to begin observing the system EventBus
     * and idle state changes.
     */
    fun startObserving()

    /**
     * Manually push an interaction to the Mascot (e.g. from the UI when the user taps on it).
     */
    suspend fun interact(input: MascotInteraction): MascotResponse

    /**
     * Observe the current state of the Mascot for UI rendering.
     */
    val state: StateFlow<MascotState>
}
```

## Data Models

```kotlin
sealed class MascotInteraction {
    data class Text(val content: String) : MascotInteraction()
    data object Tap : MascotInteraction()
}

sealed class MascotResponse {
    data class Speak(val text: String, val emotion: String = "neutral") : MascotResponse()
    data class Suggestion(val quickReplies: List<String>) : MascotResponse()
    data object Ignore : MascotResponse() // e.g. when tapped too quickly or NOISE is truly ignorable
}

sealed class MascotState {
    data object Hidden : MascotState()
    data class Active(val message: String, val emotion: String) : MascotState()
}
```

## You Should NOT

| ❌ Don't | ✅ Do Instead |
|----------|--------------|
| Feed it complex context or audio transcripts | Route complex tasks to `AnalystOrchestrator` (System II) |
| Save its interactions to the 'SessionHistory' | Treat it as ephemeral and off-the-record |
| Use it for system unrecoverable errors | Use standard dialogs for critical issues |
| Rely on it to confirm critical system actions | Use OS Toasts (`NotificationService`) |
