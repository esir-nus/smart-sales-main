# God Tracker

> **Purpose**: Track god-file cleanup, structural exceptions, and refactor-first trunk rewrites that improve prototype-to-Kotlin transplant speed.
>
> **Scope**: Large UI hosts, shell coordinators, heavy ViewModels, and oversized business-logic classes whose mixed responsibilities materially slow understanding, editing, or safe delivery.
>
> **Status**: Active
>
> **Last Updated**: 2026-03-24

---

## Why This Tracker Exists

This repo now has a battle-tested UI workflow:

- `docs/sops/ui-dev-mode.md`
- `docs/sops/ui-building.md`
- `docs/specs/style-guide.md`
- `docs/specs/ui_element_registry.md`

The current bottleneck is no longer only visual direction.

The current bottleneck is structural:

- prototype transplant targets are too hard to understand quickly
- several host files mix too many roles
- shared shell/UI/ViewModel trunk files are becoming god files
- large mixed files slow agents and humans during transplant, review, and safe edits

This tracker exists so structure cleanup becomes explicit project work rather than an unowned “clean up later” wish.

---

## Governance Split

Use these layers together, not interchangeably:

1. **UI SOP stack** owns UI workflow
   - prototype-first exploration
   - screenshot critique
   - approval
   - production transplant

2. **Cerb / cerb-ui** owns feature-local UI truth
   - exact literals
   - surface behavior
   - ownership boundaries
   - per-feature invariants

3. **God Tracker + structure contract** own code shape
   - file-role clarity
   - anti-god-file guardrails
   - temporary exceptions
   - refactor-wave sequencing

Rule:

- do not remove UI from Cerb entirely
- do not use Cerb as the main UI-building SOP
- do not restate full feature behavior in this tracker

---

## Operating Rules

1. Major new UI trunk transplant work should pause while an active god-file cleanup wave is rewriting the same trunk files.
2. Leaf-level UI polish may continue during cleanup only when it avoids the targeted trunk files.
3. Every tracked god file must have an owner, a target decomposition, and a wave/status.
4. Every temporary exception must include a sunset wave/date and verification surface.
5. This tracker records structural cleanup only. Feature behavior still belongs in `docs/cerb-ui/**`, `docs/cerb/**`, and `docs/core-flow/**`.

---

## Status Model

| Status | Meaning |
|--------|---------|
| Proposed | Identified but not yet accepted into an active refactor wave |
| Active | Current cleanup wave target |
| Guardrail Pending | Needs structural enforcement before or during cleanup |
| Exception | Temporarily allowed above budget or with mixed roles; must carry sunset |
| Refactoring | Implementation rewrite/extraction is actively in progress |
| Accepted | Brought under the intended structural shape for the current wave |
| Deferred | Tracked for a later wave; not a current blocker |

---

## Current Campaign Direction

### Refactor-First Rule

The current decision is **refactor first, then continue major UI trunk transplants**.

Reason:

- UI SOP is now battle-tested
- prototype transplant has already succeeded
- existing UI verification is strong enough to support structural cleanup
- god files are now slowing future UI transplant speed and target clarity

### Planned Wave Order

- **Wave 0**: open campaign, formalize tracker, sync references
- **Wave 1A**: add shallow structural guardrails
- **Wave 1B**: clean `AgentIntelligenceScreen.kt`
- **Wave 1C**: clean `SimShell.kt`
- **Wave 1D**: clean `SimAgentViewModel.kt`
- **Wave 1E**: clean `SimSchedulerViewModel.kt`
- **Wave 2**: clean secondary business-logic god classes

### Immediate Focus

The active trunk-cleanup wave targets:

- `app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceScreen.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShell.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerViewModel.kt`

Target outcome:

- faster prototype-to-Kotlin transplant
- clearer ownership seams
- easier agent/human comprehension
- less accidental landing into kitchen-sink files

---

## Transitional Guardrails for Wave 1

These are the initial structural targets to enforce in the first cleanup wave:

### Budgets

- UI host/shell: `550` LOC
- UI sections/components: `350` LOC
- ViewModel/coordinator: `650` LOC
- service/manager/linter/gateway: `650` LOC

These are transitional Wave 1 budgets, not final forever numbers.

### Forbidden Mixes

Block these first:

- host screen + component library + previews in one file
- shell host + reducers + telemetry + projections in one file
- ViewModel + multiple heavy feature lanes in one file
- service + parser + state machine + retry/telemetry policy in one file

### Anti-Overfragmentation Rule

Do not split a file only to satisfy a number.

Create a new file only when it owns:

- a stable role, or
- a proven responsibility cluster

Optimize for discoverability and transplant target clarity, not raw shard count.

### Structure Contract

The long-lived ownership/cap rules are now formalized in:

- `docs/specs/code-structure-contract.md`

This tracker remains the active campaign memory for the cleanup wave and its exceptions.

---

## Exception Template

Use this shape for every temporary exception:

| Path | Role | Current Size | Reason | Owner | Sunset | Required Tests | Status |
|------|------|--------------|--------|-------|--------|----------------|--------|
| `path/to/File.kt` | Host / VM / Service | `NNN LOC` | Why temporary | person/agent | wave/date | focused verification pack | Exception |

Rule:

- no sunset = invalid exception
- no required tests = invalid exception

---

## Tracked Files

Observed sizes below are the current audit snapshot used to seed the campaign on **2026-03-24**.

| File | Layer | Observed Size | Current Problem | Target Decomposition | Wave | Status |
|------|-------|---------------|-----------------|----------------------|------|--------|
| `app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceScreen.kt` | UI | 1861 LOC | Shared host mixes state collection, side effects, SIM subtree, non-SIM subtree, header/input components, hero/dashboard, previews | host + content + sections + SIM subtree + preview split | 1B | Active |
| `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShell.kt` | UI Shell | 1092 LOC | Shell host mixes reducers, telemetry, projections, routing, follow-up ownership, and drawer orchestration | host + reducers + projections + telemetry + shell actions | 1C | Active |
| `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt` | UI VM | 1516 LOC | One ViewModel owns session lifecycle, general chat, audio-grounded chat, follow-up behavior, and reconciliation | public VM + session coordinator + chat/audio/follow-up coordinators + projection helpers | 1D | Active |
| `app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerViewModel.kt` | UI VM | 1231 LOC | One ViewModel owns transcript ingress, mutation execution, reminder logic, attention projection, and warning paths | public VM + ingress + mutation + reminder + projection supports | 1E | Active |
| `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawer.kt` | UI | 968 LOC | Large but still feature-local; should not expand further until trunk pattern is proven | drawer host + sections/components only if Wave 1 pattern proves reusable | 2 | Deferred |
| `app-core/src/main/java/com/smartsales/prism/ui/onboarding/OnboardingScreen.kt` | UI | 896 LOC | Large screen with many steps; tracked but not immediate trunk blocker | step host + step sections + preview/support split if needed later | 2 | Deferred |
| `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/gateway/GattBleGateway.kt` | Data/Transport | 1033 LOC | Transport, protocol parsing, gateway policy, and state edges are too concentrated | transport + parser + gateway policy split | 2 | Deferred |
| `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManager.kt` | Data/Transport | 547 LOC | Connection/session state machine and related policy are too concentrated | connection state machine + orchestration + policy split | 2 | Deferred |
| `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinter.kt` | Domain | 1004 LOC | Normalization, temporal parsing, DTO assembly, and validation policy are too concentrated | normalize + parse + assemble + validate split | 2 | Deferred |
| `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepository.kt` | Data | 841 LOC | Persistence, artifact IO, binding management, and pipeline coordination are mixed | persistence + artifact IO + binding support + coordination split | 2 | Deferred |

---

## Wave 1 Acceptance Bar

Wave 1 is accepted only when:

- this tracker is live and referenced from the main tracker
- shallow guardrails are added for the pilot files
- the four active trunk files are reduced below transitional caps or carry one explicit short-lived exception row
- focused behavior tests stay green
- future UI transplant targets become clearer than they are today

---

## Related Documents

- `docs/plans/tracker.md`
- `docs/plans/ui-tracker.md`
- `docs/specs/code-structure-contract.md`
- `docs/sops/ui-dev-mode.md`
- `docs/sops/ui-building.md`
- `docs/specs/prism-ui-ux-contract.md`
- `docs/specs/style-guide.md`
- `docs/specs/ui_element_registry.md`
