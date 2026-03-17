# Parallel Universe Testing Protocol (PU-Testing)

> **Core Principle**: Decouple UI from Business Logic. State Space Explosion and Context Collapse are the root causes of hallucination. We mitigate this by testing one deterministic "universe" (a completely streamlined path) at a time. Testing multiple universes simultaneously introduces hallucination.

## 1. The Pre-Condition: UI/BL Decoupling
UI rendering is full of side effects (animations, recompositions), while the Business Logic (BL) and Data Pipeline (`IntentOrchestrator`, `RealUnifiedPipeline`) MUST remain pure and deterministic.
- **Rule**: Pipeline logic must be verifiable in a complete vacuum without drawing a single pixel. We achieve this by testing dataflow explicitly.

## 2. Define the Parallel Universes via `docs/core-flow/`
A "Parallel Universe" is a strictly isolated, end-to-end execution path characterized by mathematically precise starting conditions. It maps directly to an L2 Scenario.
- **New SOT (Source of Truth)**: The universes are no longer just scattered acceptances in `spec.md`. They must be formally defined in a dedicated `docs/core-flow/[feature]-flow.md` file.
- **ASCII Maps Required**: The flow document MUST contain an ASCII architecture map showing the linear path from `[User Input]` -> `[Linter]` -> `[DTO]` -> `[Pipeline]` -> `[UI State]`.
- **Mathematical Constraints**: The ASCII map MUST use explicit Data Class and Intent names (e.g., `Intent.Schedule`, `CreateTasksParams`) to mathematically constrain the test parameters.

**Example (Scheduler Path A):**
- **Universe A (Specific)**: `[Input: "Buy milk tomorrow at 3pm"]` -> `[Pipeline: Intent.Schedule(exact)]` -> `[UI: Card.Scheduled]`.
- **Universe B (Vague)**: `[Input: "Remind me to buy milk"]` -> `[Pipeline: Intent.Schedule(vague)]` -> `[UI: Card.RedFlagged]`.
- **Universe C (Inspiration)**: `[Input: "That's a good idea"]` -> `[Pipeline: Intent.Inspiration]` -> `[UI: Card.Inspiration]`.

## 3. Instrument the Observability Tools
Before teleporting into a universe, ensure the environment is instrumented to capture the dataflow without manual tracing.
- **Linter-Prompt**: Validates that the deterministic DTOs adhere perfectly to the Brain/Body contract (No free-form JSON).
- **GPS Telemetry**: `PipelineValve.tag` must be planted at module boundaries and state emissions (`DB_WRITE_EXECUTED`, `UI_STATE_EMITTED`) to mathematically prove the path was taken.
- **Interface Alignment**: The `docs/cerb/interface-map.md` serves as the strict blueprint; any writes or module crossings out of bounds represent a failing universe.

## 4. Execute the L2 Scenario Runner
Build debug runners or L2 simulated paths to drop the system straight into the isolated timeline.
- **Rule**: Reset pipeline state entirely before executing the next universe. Avoid bleeding context.

```kotlin
// Example L2 Teleportation
fun teleportToUniverse(universe: SchedulerUniverse) {
    pipeline.resetState() // Kill other universe context
    when (universe) {
        Universe.VAGUE_TIME -> {
            simulateVoiceInput("Remind me to buy milk")
            assertDataflowOutputs(expectedState = VagueTaskState)
        }
        // ...
    }
}
```

## 5. Integration with `/acceptance-team`
The final gate for a universe is the `@Acceptance Team`.
Use `PU-tracker.md` to feed the Acceptance Team a *single* isolated testing task (one universe).
The Acceptance Team evaluates the universe based on standard principles:
- **Assessment of the Testing Tasklist**: Did the universe hit all expected waypoints (Spec Examiner)? Did it respect the interface boundaries (Contract Examiner)?
- **Gaps & Risks**: Did the Break-It Examiner expose edge cases within this specific universe (e.g., specific time but date is out of range)?
- **Proactive Suggestions**: Can the Scenario Runner be made more deterministic?

## Summary
1. Define the Universes in the spec -> generating the **Testing Tasklist**.
2. Plant the observability tools (GPS, Linters, Interface Map) to log dataflow.
3. Teleport into the *single* isolated universe via L2 Runners.
4. Run the `/acceptance-team` targeting only that universe.
5. Track progress in the standalone `PU-tracker.md`.
