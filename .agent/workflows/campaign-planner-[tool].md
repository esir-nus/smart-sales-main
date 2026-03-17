---
description: Formalizes a new Epic/Campaign with the rigorous 5-step Cerb compliance checklist (Docs, Arch, Plan, Execute, Test) depending on task nature.
---

# Campaign Planner

A **scaffolding tool** that formalizes a new Epic or Campaign based on the 5-step Cerb Compliance Lifecycle. It ensures that every new body of work follows the strict path of Docs → Architecture → Plan → Execute → Test, adapted for the specific *nature* of the work.

> **Operational Law**: No task is added to the Active Epic without a compiler error, a failing test, or a direct user command. The roadmap is adaptive but driven by evidence, not hallucination.

When invoked with `/campaign-planner`, follow these steps:

---

## Phase 1: Campaign Classification

Determine the nature of the Campaign by asking the user:
- What is the objective?
- What type of work is this? (Feature, UI/UX, Architecture Refactor, E2E Testing, Bug Fix, or Parallel Universe Validation?)

---

## Phase 2: The "Cerb Shard" Task Generation Rule

Before generating the checklist, evaluate the chunkiness of the tasks.

> **The Senior Engineer Rule against God Tasks**:
> Tasks must be atomic **Cerb Shards**, not monolithic blocks. A single task should not say "Build the Database, the ViewModel, and the UI."
> - **Wrong (God Task)**: `T1: Build the Scheduler Feature`
> - **Right (Cerb Shards)**: `T1: Scheduler Repository (Domain Isolation)` → `T2: Scheduler Linter (LLM Contract)` → `T3: Scheduler ViewModel (Reactive UI State)`
> 
> **Why?** Chunking forces you to create specific `interface-map` boundaries for each shard, allowing mechanical verification before moving to the next layer.

Based on the classification and the Cerb Shard rule, generate the exact Markdown structure to be appended to `docs/plans/tracker.md` under the Active Epic.

### Structure 1: Feature Work (The Standard Path)
```markdown
### 🌊 Wave X: [Campaign Name]
> Objective: [Context] - 🧭 **North Star**: [docs/specs/waveX-[name]-master-guide.md]()
- [ ] 🔲 **T0: Campaign North Star**
  - [ ] **Docs**: Create `docs/specs/waveX-[name]-master-guide.md` to establish architectural principles and Intricacies.
- [ ] 🔲 **T1: [Task Name]**
  - [ ] **Docs**: Create/update `docs/cerb/[feature]/spec.md` and `interface.md`.
  - [ ] **Interface Map**: Read/update `docs/cerb/interface-map.md`.
  - [ ] **Plan**: `/feature-dev-planner`
  - [ ] **Execute**: Implement feature logic and tests.
  - [ ] **Test**: L1/L2 Verification passes against the spec.
```

### Structure 2: UI/UX Skin Polish
```markdown
### 🌊 Wave X: [Campaign Name]
> Objective: [Context] - 🧭 **North Star**: [docs/specs/waveX-[name]-master-guide.md]()
- [ ] 🔲 **T0: Campaign North Star**
  - [ ] **Docs**: Create `docs/specs/waveX-[name]-master-guide.md` defining the UX/UI intent.
- [ ] 🔲 **T1: [Task Name]**
  - [ ] **Docs**: Create/update `docs/cerb-ui/[feature]/spec.md` and `contract.md`.
  - [ ] **Interface Map**: Verify `IAgentViewModel` isolation (No backend logic).
  - [ ] **Plan**: Run `/08-ux-specialist` for interaction design.
  - [ ] **Execute**: Update UI Compose files + `@Preview` testing using `Fake*ViewModel`.
  - [ ] **Test**: Mechanical Check: `grep` to prove no concrete ViewModel imports.
```

### Structure 3: Architecture Refactoring (The Clean Slate)
```markdown
### 🌊 Wave X: [Campaign Name]
> Objective: [Context] - 🧭 **North Star**: [docs/specs/waveX-[name]-master-guide.md]()
- [ ] 🔲 **T0: Campaign North Star**
  - [ ] **Docs**: Create `docs/specs/waveX-[name]-master-guide.md` outlining the Refactoring Constraints.
- [ ] 🔲 **T1: [Task Name]**
  - [ ] **Docs**: Update Master Guide (`Architecture.md`) with the new architectural paradigm.
  - [ ] **Interface Map**: Update `docs/cerb/interface-map.md` with layer decoupling rules.
  - [ ] **Plan**: Review Strategy (Extract vs Rewrite) with `/01-senior-reviewr`.
  - [ ] **Execute**: Implement physical isolation (Kotlin/Gradle changes).
  - [ ] **Test**: Verify build + L3 acceptance testing.
```

### Structure 4: E2E Test Hardening
```markdown
### 🌊 Wave X: [Campaign Name]
> Objective: [Context] - 🧭 **North Star**: [docs/specs/waveX-[name]-master-guide.md]()
- [ ] 🔲 **T0: Campaign North Star**
  - [ ] **Docs**: Create `docs/specs/waveX-[name]-master-guide.md` defining the Testing Pillars and Coverage.
- [ ] 🔲 **T1: [Task Name]**
  - [ ] **Docs**: Create `docs/cerb-e2e-test/specs/[audit-name]/spec.md` and `boundaries.md`.
  - [ ] **Interface Map**: Outline which modules are blackboxed vs simulated.
  - [ ] **Plan**: Run `/06-audit` to define test scenarios.
  - [ ] **Execute**: Implement L2 `WorldStateSeeder` datasets or L3 Instrumentation tests.
  - [ ] **Test**: Tests pass without Mockito illusions. Log results in `TER`.
```

### Structure 5: Parallel Universe Validation (PU-Testing protocol)
> *Note: This structure goes into `PU-tracker.md`, NOT `tracker.md`.*
```markdown
### 🚧 ACTIVE EPIC: Wave X [Campaign Name] ([Feature Module])
> **Context**: Validation tasklist for isolated, deterministic universe testing.
> 🧭 **North Star**: [docs/core-flow/[feature]-flow.md]()
- [ ] 🔲 **T0: The Core Flow Map (Anti-Hallucination Blueprint)**
  - [ ] **Docs**: Create `docs/core-flow/[feature]-flow.md`.
  - [ ] **Visualize**: Draw the exact ASCII map from `[User Input]` -> `[Intent/DataClass]` -> `[Pipeline State]` -> `[UI Result]`.
  - [ ] **Audit**: Present the ASCII map to the Human for verification *before* writing any test logic.

### Universe Test Matrix

| Universe ID | Core Flow Ref | Input / Condition | Expected Pipeline DTO | Accept / Result UI | `/acceptance-team` Status |
|---|---|---|---|---|---|
| **Uni-A ([Name])** | `[feature]-flow.md` | *[Input]* | `[Expected State]` | `[Expected UI]` | 🔲 Pending |
| **Uni-B ([Name])** | `[feature]-flow.md` | *[Input]* | `[Expected State]` | `[Expected UI]` | 🔲 Pending |
```

---

## Phase 3: Immediate Execution Hook

After generating the checklist structure:
1. Propose injecting this block directly into the appropriate tracker.
   - For Structures 1-4: Update `docs/plans/tracker.md`
   - For Structure 5 (Parallel Universe Validation): Update `docs/plans/PU-tracker.md`
2. Explicitly state: "I will NOT auto-update the Master Guide. I will update the appropriate tracker and await your command to start T1 (or Uni-A)."
3. Wait for the user to approve the addition.
