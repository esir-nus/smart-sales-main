# Prism UI Alignment Table (Phase 2 Output)

> **Purpose**: Bridge between Prism Core (Phase 2) and UI Layer (Chunk G).
> **Audience**: UI/UX Specialist, UI Director, UI Implementer.

---

## 🏗️ Core Pipeline Connection

The UI connects to Prism via **one unified entry point**: `PrismOrchestrator`.

| UI Component | Data Source | Action Trigger |
|--------------|-------------|----------------|
| **Any Screen** | `orchestrator.currentMode` (Flow<Mode>) | `orchestrator.switchMode(newMode)` |
| **Active Mode** | `orchestrator.uiState` (Flow<UiState>) | `orchestrator.processUserIntent(input)` |

---

## 🎨 UI State Mapping

Each `UiState` variant maps to a specific Compose UI state.

| UiState Variant | Compose Screen/Component | Visual Behavior |
|-----------------|--------------------------|-----------------|
| `Idle` | `StandardInputBar` | Show microphone/text input, suggestions |
| `Thinking(hint)` | `ThinkingCanvas` | Show "Thinking..." animation, hints |
| `Streaming` | `StreamingBubble` | Typewriter effect text generation |
| `Response` | `ChatBubble` (Coach) / `ReportView` (Analyst) | Render final markdown content |
| `PlanCard` | `PlanCardView` | Interactive checklist with progress |
| `Error` | `ErrorBanner` / `RetryButton` | Show specialized error UI |

---

## 🛠️ Input Tools Integration

How the UI feeds data into the `ContextBuilder`.

| Input Type | UI Action | Prism Interface |
|------------|-----------|-----------------|
| **Voice** | Hold Mic Button | `TingwuRunner.transcribe(audioFile)` |
| **Image** | Camera/Gallery | `VisionAnalyzer.analyze(imageFile)` |
| **Link** | Paste URL | `UrlFetcher.fetch(url)` |
| **Text** | Type + Send | `processUserIntent(text)` |

---

## 📱 Screen-to-Mode Mapping

| Screen | Active Mode | Description |
|--------|-------------|-------------|
| **Coach Screen** | `Mode.COACH` | Conversational sales advice |
| **Analyst Screen** | `Mode.ANALYST` | Deep data analysis reports |
| **Scheduler Screen** | `Mode.SCHEDULER` | Tasks, Calendar, Alarms |

---

## 🔄 ViewModel Responsibilities (Chunk G)

Access `PrismOrchestrator` and expose UI-friendly state.

```kotlin
class PrismViewModel(
    private val orchestrator: Orchestrator
) : ViewModel() {
    
    // UI observes this
    val uiState = orchestrator.uiState
        .stateIn(viewModelScope, SharingStarted.Lazily, UiState.Idle)

    fun onSend(text: String) {
        viewModelScope.launch {
            orchestrator.processUserIntent(text)
        }
    }
}
```
