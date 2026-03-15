# Wave 16 Master Guide: Scheduler Decoupling (The Archival Purge)

> **Preamble**: This guide establishes the architectural constraints for severing the monolithic Scheduler's hardcoded dependencies from the Core OS Pipeline (`:core:pipeline`). 

## 1. The Core Problem
The Dual-Engine architecture (System I & System II) is currently polluted. `IntentOrchestrator`, `RealUnifiedPipeline`, and `PromptCompiler` have hardcoded imports to `:domain:scheduler` (e.g., `ScheduledTaskRepository`, `FastTrackParser`, `SchedulerLinter`). 

This violates the **Town and Highway** mental model:
- The Highway (Core Pipeline) should route traffic. It shouldn't know what the buildings in the Town look like.
- If we want to treat the Scheduler as a true Plugin (System III), the Highway cannot have compile-time dependencies on it.

## 2. The Architectural North Star

We are pivoting to a **Generic Plugin/Adapter Architecture** for the Pipeline.

### Principle 1: The Blind Router
- `IntentOrchestrator` and `RealUnifiedPipeline` must operate exclusively on generic `Data Classes` and `Interfaces` (e.g., `ToolDispatcher`, `IntentResolver`). 
- They cannot invoke `scheduledTaskRepository.insertTask()` directly. They must emit a generic `MutationEvent` or call a generic `Tool`.

### Principle 2: The Domain Hook
- The Scheduler logic (`FastTrackParser`, `SchedulerLinter`) belongs entirely inside `:domain:scheduler`.
- The App-level DI (Dependency Injection) graph will be responsible for binding the generic Pipeline interfaces to the concrete Scheduler implementations. 
- The Pipeline module (`:core:pipeline`) will literally have **zero** imports starting with `com.smartsales.prism.domain.scheduler`.

## 3. Execution Rule (The Cerb Shards)

This refactor is highly destructive. It must be executed in strict sequence:
1. **Interface Extraction:** Define the generic contracts in `:core:pipeline` first. Prove they compile.
2. **Pipeline Purge:** Rip out all Scheduler imports from the pipeline. Replace them with the generic contracts. Prove the pipeline compiles (even if the app doesn't).
3. **Scheduler Plugin Wiring:** Implement the contracts in `:domain:scheduler` and wire them via App module DI. Prove the system runs end-to-end.
