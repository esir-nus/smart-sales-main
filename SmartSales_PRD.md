# Smart Sales App: Reverse-Engineered PRD

### 1. Product Vision & Value Proposition
**Smart Sales** is a proactive, AI-native CRM assistant built for B2B sales professionals. It abandons the traditional "data-entry" CRM paradigm in favor of an **ambient, voice-first intelligence**. By heavily leveraging hardware integration (connected audio badge) and a dual-engine architecture, it autonomously captures meeting context, manages complex B2B entity relationships (Accounts, Persons), resolves scheduling conflicts, and provides deep contextual analysis—all without blocking the user's primary workflow.

### 2. Core Architecture: The Dual-Engine Model
To balance instant reactivity with deep, multi-step reasoning, the system employs a completely bifurcated architecture known as the Dual-Engine:
- **System I (The Mascot)**: An ephemeral, read-only layer that handles `NOISE`, `GREETINGS`, and passive system notifications via an overlay. It relies on the `MascotService` and acts instantly, avoiding the chat feed. **Crucially, the Mascot NEVER initiates stateful interaction or database writes.** It may flag an event (e.g., "Conflict Detected"), but it does not resolve it.
- **System II (The Orchestrator / Analyst)**: A persistent, stateful workhorse. It navigates complex CRM tasks (e.g., entity disambiguation, tool execution) using an **Open-Loop Lifecycle** (Chat → Proposal → Confirm → Result). This guarantees the "human-in-the-loop" before any destructive actions or token-heavy investigations occur.
- **The Gatekeeper (`LightningRouter`)**: An ultra-fast, "Phase 0" routing mechanism using `ContextDepth.MINIMAL`. It intercepts user intents and routes them to System I (for noise/greeting) or System II (for analysis), or even instantly answers `SIMPLE_QA` on the fast track.

### 3. OS-Level Memory Model (Context Strategy)
The app treats LLM context like a traditional Operating System to solve token bloat via a layered memory architecture:
- **RAM (SessionWorkingSet)**: The active memory managed by the `ContextBuilder`. It loads only the strictly necessary Entities, User Habits, Client Habits, and limited chat history. The LLM only reasons over this `EnhancedContext`.
- **The In-Memory Alias Index (L1 Cache)**: A highly optimized, lightweight dictionary of `{Alias : Entity Pointer}` loaded entirely into the device's RAM. Instead of querying the full database for disambiguation, the Native code runs phonetic searches against this skinny index to generate a hyper-targeted "Contact Sheet" for the LLM prompt.
- **SSD (Memory Center / Entity Registry)**: The persistent CRM database where the actual heavy Entity objects, metrics, and relationships live. It is only queried *after* the fast Alias Index and LLM have successfully resolved the target ID.
- **The RL Module (Implicit Preference Learning)**: An OS-layer RAM application that autonomously observes the user's choices. It tracks explicit confirmations ("Yes, I prefer mornings", weighted 3x) and implicit patterns (weight 1x), applying a 30-day time decay. The LLM surfaces these habits dynamically within the `EnhancedContext` without requiring manual CRM field updates.

### 4. Key Functional Pillars
- **Hardware & Audio (`badge-audio-pipeline`)**: Captures BLE badge recordings, pipes them into ASR/Tingwu, and automatically extracts actionable tasks and CRM entities into the timeline.
- **Organic Entity Resolution (`entity-disambiguation`)**: A robust system enforcing strict CRM hygiene. If the background pipeline identifies an ambiguous entity (e.g., "Meeting with Cook"), the system simply flags it. **Human chat input is the exclusive entrance for initiating entity disambiguation and database writing.** Only when the user naturally asks "Who is Cook?" or clarifies "I meant Tim Cook" does the write-through loop trigger via `EntityWriter`.
- **Intelligent Scheduler**: Manages tasks natively. Resolves double-bookings via the `ConflictResolver` (surfacing breathing amber UI cards) rather than confusing the LLM, and correctly handles Do Not Disturb policies with visual urgency tiers.
- **Analyze Gateway & Ambient Tool Stream**: A natural, chat-first plugin ecosystem. The Orchestrator does not force rigid TaskBoards or UI buttons. As the user chats about a transcript, the LLM evaluates the context and recommends tools via pure conversational text (e.g., "I recommend generating the Executive Report, but I am missing the budget amount."). If the user types "Give me the PDF, budget is $5M", the system maps the intended tool directly to the `tool01` ID for swift execution without requiring complex JSON outputs.

### 5. Scalability & The Blackbox Philosophy
The architecture guarantees scalability and prevents regressions by treating all heavy modules as decoupled "Blackboxes" interacting strictly through OS-level APIs (via the Interface Map).
- **Data Flow Separation**: The Orchestrator does not manually extract UI state; it listens to `Flow<UiState>` emitted by isolated Plugins or the `ContextBuilder`.
- **Stateless LLM Routing**: Model selection (`ModelRouter`) is decoupled from features. A tool requests an LLM based on *task type* (e.g., `qwen3-max` for 32k context reasoning, `qwen-turbo` for fast routing), allowing the entire model tier to be swapped without rewriting business logic.
- **Fail-Safe Processing**: The system avoids monolithic points of failure. If the `Tingwu` audio pipeline crashes, the user can still type requests. If the Analyst state machine loops, the `LightningRouter` still processes `NOISE` instantly.

### 6. Data Integrity & The Linter Gate
The system enforces strict data hygiene to prevent LLM hallucinations from poisoning the CRM state via **Deterministic Grounding**:
- **Pre-Flight Lookup (Speed)**: Before the LLM processes an ambiguous request (e.g., "Schedule meeting with Cook"), native Kotlin code performs a lightning-fast fuzzy search against the local `EntityRegistry`. It injects the top results directly into the LLM prompt (e.g., "Choose only from ID:001 Tim Cook or ID:002 Chef Cook").
- **Schema Linter (Accuracy)**: A hardcoded deterministic quality gate. It validates *all* structured outputs (Entity Extraction, Scheduling JSON, Execution Plans) *before* persistence. If the LLM generates a valid JSON structure but hallucinates an ID that wasn't in the pre-flight list, the Linter fails the write, suppressing the error from the user and triggering a silent retry or passing it to Organic Disambiguation.
- **Strict Interface Ownership Rules**: Modules interact via strict interfaces. For instance, `Scheduler` does not write Entities directly; it delegates to the downstream `EntityWriter`. `ContextBuilder` provides `EnhancedContext`, forbidding individual modes from firing direct query searches against the repository.

### 7. Core Testing Philosophy: The Anti-Drift Protocol
To prevent the agent and developers from shipping "smart" code that drifts from the core CRM specifications, the system enforces a heavily guarded, three-level testing standard rooted in the **Blackbox Philosophy**.

#### Fake-First Development
No LLM latency or API costs should be burned to verify UI states and data pipelining. Elements like the Orchestrator are built using `FakeAnalystPipeline` to simulate the open-loop state machine rapidly, ensuring UI transitions work identically before real execution.

#### The Three-Level Testing Standard
1. **L1: Logic Verification (`./gradlew test`)**: Every domain module modification must be covered by unit tests. Tests ensure schemas compile, Linters trigger correctly, and data transformation happens flawlessly.
2. **L2: Simulated On-Device (The Integration Bridge)**: This is the most critical phase. Because "True" data (L3) causes false positives in LLM applications, developers *must* build Debug UI Buttons that inject *preset, simulated inputs* directly into the ViewModels. This allows the team to validate the isolated feature (e.g., executing a "Schedule Meeting" flow) without having to perform real voice recognition.
3. **L3: Full On-Device**: Once L2 passes, the app must be verified in a real environment with real UX gestures. If an L3 test fails but L2 passes, the bug sits at the UI layer (missing pointers, uncollected flows) rather than the domain logic layer.

#### The Anti-Laziness Mandate
- **Negative Testing Focus**: Success paths are cheap. The QA methodology mandates testing `null`, empty, network failures, and missing resources. If you haven't seen it fail, you don't know that it works.
- **Concrete Verification**: "Looks good" is unacceptable. All test outputs must be visible (ADB logcat, grep results) before code can be shipped.
- **Acceptance Team**: Prior to merging complex architectural changes, an automated `/acceptance-team-[tool]` workflow runs Build Examiners and Contract Examiners to ensure no legacy Android imports leak into the pure Kotlin `domain/`.

### 8. Interaction Patterns & UX Contract
- **Gestures**: Persistent pull-down from top for the `Scheduler` drawer; pull-up from bottom for the `Audio` drawer.
- **Notifications**: Snackbar system acts as an activity feed—surfacing non-blocking insights like "Relationship Change Detected" or "Conflict Spotted" without disrupting chat.
- **Invisible Heavy-Lifting**: The app favors "zero-latency ASCII" over bulky UI rendering for data-heavy operations. 

---

## Panel Review Audit Summary

### 🧑‍💼 Sales Expert Perspective
The ambient audio capture (badge + Tingwu) converting 1-hour meetings into 0-click CRM data is the core value driver for admin-averse reps. Entity disambiguation is a critical deal-saving feature, preventing cross-contamination of client data.

### 🎨 UX Specialist Perspective
The Dual-Engine separation perfectly balances system feedback without chat-feed fatigue. The Phase-4 Execution Bypass (TaskBoard) ensures the app functions like snappy, reliable software, avoiding sluggish chat-bot loops for basic tools (e.g., PDF generation).

### 🏗️ Architecture Perspective
Treating context as OS RAM (`EnhancedContext`) solves the LLM token budget limitation at scale. The Open-Loop state machine acts as an effective safety gate, protecting against destructive, unverified DB writes.

### 💡 Senior Engineer Synthesis
The system resembles an LLM operating system rather than a standard wrapper, leveraging Phase 0 fast limits (`LightningRouter`) and native implicit tracking (`RL Module`) effectively. 

However, there is a risk of **Interface Coupling Rot**.

**Current Risks/Gaps**:
- *Lattice Compliance / Layer Violations*: To prevent developers from illegally bypassing the `EntityWriter` (Layer 3) to call `EntityRepository.save()` (Layer 2), **Lattice Compliance is structurally enforced**. We utilize the Kotlin `internal` modifier on repository mutation methods and enforce regex-based CI/CD blockers via the `/acceptance-team` to guarantee Layer 4 features cannot touch the database directly.
- *Plugin Reality*: The UI and routing exist, but core workflows (`analyzer`) are migrating. To bridge the gap, the Plugin Registry implements **"Graceful Degradation."** Unregistered or `PARTIAL` tools do not crash the Open-Loop machine; they gracefully surface a snackbar ("Tool under construction") to protect user trust while workflows are built.
- *Audio Storage*: The 'zero-latency' ASCII hack relies on the physical audio file successfully uploading. To ensure bulletproof reliability, the audio pipeline MUST be bound to an **Android Foreground Service** (to prevent OS Doze kills) and utilize **chunked, resumable OSS uploads** to survive network drops without restarting.
- *LLM Disambiguation Limitations*: Heavy reliance on the LLM to output accurate JSON `missingEntities` requires robust offline fallbacks to counter hallucinations. This is partly mitigated by the Schema Linter, but semantic hallucinations (wrong person, right schema) will still bypass it.
- ~~*Deprecation Rot*: The codebase must officially purge all references to `coach` mode, legacy enums, and switchers to prevent future state-machine chaos.~~ *(✅ Resolved: Mar 2026 - All ghost UI, LLM profiles, and pipeline controllers purged)*
