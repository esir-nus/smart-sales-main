## DocSync Proposal: The Hand-Off Animation Scope Ambiguity

### Context
A Review Conference (`/00-review-conference`) integrating the UX Specialist and Senior Engineer personas was conducted regarding the implementation plan for "Wave 6 T5: The Hand-Off Animation". 
The conference identified a critical spec ambiguity regarding the *source* of voice ingestion (Phone Mic vs Badge Audio Pipeline) and strongly recommended pausing UI implementation until the architectural contract for voice state ingestion is formally defined in Cerb.

### Cross-Reference Audit
- [x] No deprecated `docs/guides/` refs
- [x] No `RealizeTheArchi` refs (now `tracker.md`)
- [x] No `ux-tracker.md` refs (now `ux-experience.md`)
- [x] All file links point to existing files
- [x] `docs/plans/tracker.md` reflects current spec wave status accurately (Wave 6 T5 is currently untouched pending clarification).

### Docs to Update

#### 1. `docs/cerb-ui/agent-intelligence/spec.md`
**Why:** The PRD/Tracker requires visual bridging for voice ingestion, but the UI spec currently only defines `UiState.Loading` (which is generic and renders text "Waking up pipeline...") and `UiState.Thinking`. It lacks a dedicated state or flow for "Voice Ingestion/ASR Processing".
**Changes:**
- Add `Voice Handling UI States` section to outline the required visual cues for ASR (e.g., "Transcribing...", "Processing Audio...").
- Define the contract for how `AgentChatScreen` should display these states without violating the OS Model (e.g., relying on a new `UiState.AudioProcessing` emitted by `IntentOrchestrator`, rather than directly observing `BadgeAudioPipeline`).

#### 2. `docs/cerb/unified-pipeline/interface.md` OR `docs/cerb/badge-audio-pipeline/spec.md` (Depending on User Answer)
**Why:** The backend contract for emitting "I am transcribing" to the global UI needs to be documented before it can be implemented.

### Docs NOT Updated (and why)
- `docs/plans/tracker.md`: We cannot mark Wave 6 T5 as complete, nor can we accurately define its completion criteria, until the scope ambiguity is resolved.

