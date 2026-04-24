# Smart Sales E2E Testing Protocol

> **Purpose**: This is the constitutional source of truth for the `cerb-e2e-test` domain. It defines how we treat End-To-End (E2E) testing as a first-class feature governed by the same strict rules, OS alignments, and Interface Maps as production code.

---

## 1. Testing Philosophy & Mental Model

### The "Testing Illusion" Trap
Traditional mocking in LLM applications creates a "Testing Illusion." If a test mocks intermediate steps (like intent parsing or database IO) to always return a fake "Success," the test script passes, but the pipeline crashes in production. **Traditional unit testing is insufficient for Dual-Engine architectures.**

### The Blackbox E2E Principle
Every test exists solely to protect a real End-to-End user experience across the OS Model (RAM vs SSD). Data-heavy operations (Layer 2 Data Services) and the Orchestrator (Layer 3) must be treated as blackboxes. 
- **Fail-First**: The system must handle missing JSON fields, null inputs, and hallucinated IDs gracefully via the native `ClarificationNeeded` fallback explicitly without crashing the app.
- **Fake Over Mock**: Instead of `whenever(x).thenReturn(Y)`, we provide explicitly seeded states to Data `Fakes`, forcing the pipeline to *earn* the output.

---

## 2. The Three-Level Testing Standard

Our QA protocol strictly divides testing into three boundaries:

1. **L1: Domain Tunnels (Logic Verification)**
   - *Scope*: Isolated Linters, Map/Reduce functions, Schema Resolvers.
   - *Goal*: Does it compile? Does the `SchemaLinter` reject missing JSON keys natively before hitting the SSD?
   - *Execution*: Standard JUnit (`./gradlew testDebugUnitTest`).

2. **L2: Pipeline Flows (Simulated E2E)**
   - *Scope*: The `UnifiedPipeline` Dataflow, OS Memory Model syncs, and UI state emissions.
   - *Goal*: Rapid, deterministic E2E simulation. We inject Fake constraints (e.g., Fake network drop, Fake multi-entity prompt) to verify structural movement without burning LLM tokens or requiring a phone.
   - *Execution*: AcceptanceFixtureBuilder workflows in JUnit.

3. **L3: Edge Case Chaos (True On-Device E2E)**
   - *Scope*: Hardware interrupts, Lock-screen Doze, LTE->WiFi handovers, background Audio processing.
   - *Goal*: The ultimate crucible. Automated UI tools (Appium) cannot simulate walking into an elevator.
   - *Execution*: Manual, hostile user-flow scripting based on the Matrix below.

---

## 3. User Flow E2E Matrix (The 6 Pillars)

> *Every L2 and L3 test written MUST trace back to one of these defining E2E Realities.*

### Flow 1: The Lightning Fast-Track
**Core Engine**: `LightningRouter`
**Objective**: Can the app deterministically short-circuit heavy LLM analysis for trivial tasks?
- **E2E Scenario**: User says a quick greeting or asks the time.
- **L2 Sim Goal**: Assert intent is routed instantly to `MascotService` without an Orchestrator timeout.
- **L3 Manual Goal**: Quick badge tap → Instant audio reply with ZERO UI spinning wheel.

### Flow 2: The Dual-Engine Bridge
**Core Engine**: `EntityWriter`
**Objective**: Does the pipeline extract data, persist it to SSD, and instantly surface it back into the RAM (SessionContext) for the *next* conversation turn?
- **E2E Scenario**: Multi-turn conversation adjusting a CRM record value mid-stream.
- **L2 Sim Goal**: Cross-turn context accumulation test without database deadlocking.
- **L3 Manual Goal**: Natural multi-sentence interruption verifying the CRM accurately tracked the delta.

### Flow 3: Strict Interface Integrity
**Core Engine**: Internal Connectors (SSD ↔ RAM)
**Objective**: Does the architecture strictly reject poisoned cross-contamination?
- **E2E Scenario**: LLM generates a hallucinated target ID.
- **L2 Sim Goal**: Interface layer perfectly catches missing IDs, aborts the SSD write, and requests clarification natively.
- **L3 Manual Goal**: Speak a highly confusing, contradictory prompt tracking the app's degradation response.

### Flow 4: The Adaptive Habit Loop
**Core Engine**: RL Module
**Objective**: Does the system record preference corrections and default to them on next boot?
- **E2E Scenario**: User rejects a generated proposal 3 times in a row.
- **L2 Sim Goal**: Verify underlying `HabitScore` shifts on the SSD, altering the pipeline's default heuristics entirely.
- **L3 Manual Goal**: Explicitly retrain the Agent over repeated days.

### Flow 5: The Efficiency Overload
**Core Engine**: Alias Library
**Objective**: Do native Alias queries perfectly bypass the LLM Executor to save time and money?
- **E2E Scenario**: High-frequency super user demands a critical daily report.
- **L2 Sim Goal**: Assert Alias routing mechanism natively returns pre-compiled DB results. 
- **L3 Manual Goal**: Sub-second UI loading via shortcut tap comparing standard LLM latency.

### Flow 6: The Transparent Mind
**Core Engine**: Agent Activity States
**Objective**: Does the UI Thinking Box perfectly reflect the pipeline's internal state machine?
- **E2E Scenario**: A multi-tool, heavy deep-analysis question.
- **L2 Sim Goal**: EventBus emits exact state arrays match for match (`STATE_RETRIEVING`, `STATE_EXECUTING`) alongside the Orchestration.
- **L3 Manual Goal**: Watch the UX screen as the query runs.

---

## 4. Domain Storage Structure & Governance

To prevent testing from becoming a monolithic diary, the `cerb-e2e-test` domain is strictly decoupled into three local layers plus two project-level governance mirrors:

### 1. The Static Constitution (`testing-protocol.md`)
- **Rules & Principles**: This file. It defines the Blackbox mindset, the 3-Level Standard, and the 6 Core End-to-End pillars. It never changes unless the architecture's fundamental rules change.

### 2. The Dynamic Index Ledger (`tasklist_log.md`)
- **Tracking & Automation**: The active execution ledger for QA operations. It mirrors the current testing targets, links evidence, and records when a Wave is SHIPPED. This file is dynamic and constantly evolving.

### 3. The Isolated Test Spec (`spec_test-[feature].md`)
- **Execution Target**: A meticulously crafted, single-purpose specification. E.g., `spec_test-lightningRouter.md`. When testing a pillar, we create a dedicated spec for it. It hardcodes pointers to the target feature's interfaces and specs (e.g., `docs/cerb/lightning-router/interface.md` and `docs/cerb/interface-map.md`), allowing the agent workflow to strictly scope its execution.

### 4. The Active Project Tracker (`docs/plans/tracker.md`)
- **Active Status Owner**: The tracker owns whether a testing wave is still open, blocked, or closed in the live project plan. If a testing wave is still unchecked or marked open in `tracker.md`, it is not closed, even if a changelog line already says SHIPPED.

### 5. The Historical Shipment Log (`CHANGELOG.md`)
- **History Only**: The changelog records that a shipment happened and links historical artifacts. It does **not** own active open/closed status for testing work, and it must never be used as the tie-breaker when tracker or ledger state drifts.

### Status Ownership & Drift Law

When the same testing wave appears in multiple places, ownership is:

1. `docs/plans/tracker.md` owns active status and remaining work.
2. `docs/cerb-e2e-test/tasklist_log.md` owns the operational mirror: spec path, evidence path, and the last synced test-wave record.
3. `CHANGELOG.md` is append-only historical narrative.

If any of these disagree:

- treat the wave as **not fully closed**
- repair `tracker.md` and `tasklist_log.md` first
- only then keep or add the changelog entry as historical evidence

---

## 5. Workflows & Verification

### The E2E Execution Loop
Writing an E2E test requires just as much architectural rigor as writing a feature. The development loop is:
1. **Target Selection**: Create/Define the target test spec (e.g., `spec_test-lightningRouter.md`) tracking one of the 6 Pillar Flows. Log it in `tasklist_log.md`.
2. **Planner Initiation**: Invoke `@[/feature-dev-planner]` aimed explicitly at the isolated test spec. The planner reads the test spec, maps the `interface-map.md`, and enforces OS Layer rules.
3. **Acceptance Team**: The E2E test suite itself must be verified by `@[/acceptance-team]` to ensure it is valid, robust, and includes Break-It (hallucination/poison) tests.
4. **Logcat Tracing**: You MUST debug using `adb logcat -s Tag:D`. We trace the exact data crossing the boundaries; no guessing allowed.

### Testing Wave Ship Gate

A testing wave may be marked `✅ SHIPPED` only when all of the following are true in the same closeout session:

1. The acceptance evidence exists and is linkable.
2. `tasklist_log.md` mirrors the shipped wave with the active spec path and evidence path.
3. `docs/plans/tracker.md` closes the corresponding testing wave or task without leaving contradictory open boxes.
4. `CHANGELOG.md` is updated only as the historical shipment record, never as the active status owner.

Failure of any one item means the wave remains open, even if the changelog already says SHIPPED.
