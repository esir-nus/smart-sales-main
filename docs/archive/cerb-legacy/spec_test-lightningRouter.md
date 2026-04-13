# E2E Test Spec: Lightning Router Fast-Track

> **State**: SHIPPED
> **Target Flow**: Pillar 1 (The Lightning Fast-Track)

---

## 1. Cerb Scope Declaration
- **Test Target**: `LightningRouter`
- **Target Feature Spec**: `docs/cerb/lightning-router/spec.md` (Assumed)
- **Target Interface**: `docs/cerb/lightning-router/interface.md`
- **Topology Map**: `docs/cerb/interface-map.md`

## 2. Testing Objectives & 3-Level Standard

### L1: Domain Logic
- **Objective**: Ensure NOISE and GREETING intents are mapped natively without hitting the LLM model array.
- **Verification**: JUnit tests asserting `PipelineIntent` parsing logic.

### L2: Simulated E2E (AcceptanceFixtureBuilder)
- **Objective**: Assert that injecting a `GREETING` short-circuits to `MascotService` and never invokes the Deep Analysis `Executor` or standard CRM pipelines.
- **Constraint**: Must use isolated Data Fakes, proving no persistent SSD side effects occur for trivial routing.

### L3: Manual Crucible (On-Device)
- **Objective**: Quick physical badge tap scenario.
- **Verification Criteria**: Instant auditory reply with ZERO UI spinning wheel / "Thinking..." delay.

## 3. Acceptance Team Criteria
- [x] **Contract Team**: Must visually confirm via `grep` that NO database `EntityWriter.save` / `Dao` writes are triggered in this flow.
- [x] **Break-It Team**: Hostile spam test (rapid button double-taps with garbage noise) must cleanly drop inputs without crashing the Orchestrator's internal finite state machine.
