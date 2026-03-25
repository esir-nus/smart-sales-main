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

- **Wave 0**: preflight docs, freeze rule, exception seeding, and Wave 1A guardrail design
- **Wave 1A**: deliver shallow structural guardrails via `GodStructureGuardrailTest`
- **Wave 1B**: clean `AgentIntelligenceScreen.kt`
- **Wave 1C**: clean `SimShell.kt`
- **Wave 1D**: clean `SimAgentViewModel.kt`
- **Wave 1E**: clean `SimSchedulerViewModel.kt`
- **Wave 2**: clean secondary business-logic god classes without touching active UI trunks
  - **Wave 2A**: clean `SchedulerLinter.kt`
  - **Wave 2B**: clean `GattBleGateway.kt` and `DeviceConnectionManager.kt`
  - **Wave 2C**: clean `SimAudioRepository.kt`
- **Wave 3A**: clean `SimAudioDrawer.kt` once the current SIM drawer UI contract is stable enough for a host/content/component split
- **Later UI Wave**: revisit `OnboardingScreen.kt` after the current UI-development window

### Wave 2 Focus

The next cleanup campaign is intentionally **business-logic only**.

Wave 2 targets:

- `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinter.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/gateway/GattBleGateway.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManager.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepository.kt`

Target outcome:

- faster prototype-to-Kotlin transplant
- clearer ownership seams
- easier agent/human comprehension
- less accidental landing into kitchen-sink files

UI-safe deferral:

- `OnboardingScreen.kt` remains tracked debt
- Wave 2 intentionally stayed business-logic-only while the repo was in an active UI-development window
- Wave 3A later reopened `SimAudioDrawer.kt` as a focused user-approved host/content/component cleanup once the drawer behavior contract had already stabilized

### Wave 0 Preflight

Wave 0 is now tracked as a docs-only preflight wave, not a production refactor wave.

Wave 0 deliverables:

- `docs/plans/god-wave0-execution-brief.md`
- seeded temporary exception rows for the four pilot files
- freeze rule for the four pilot files
- a decision-complete Wave 1A structural-test design

Wave 0 must not yet perform:

- production-file decomposition
- preview extraction
- subtree or ViewModel lane rewrites

### Wave 1A Guardrail Delivery

Wave 1A is now implemented as a shallow JVM guardrail wave.

Delivered anchor:

- `app-core/src/test/java/com/smartsales/prism/ui/GodStructureGuardrailTest.kt`

Delivered checks:

- tracker/contract validity for the four pilot exception rows
- pilot structural budget enforcement backed by valid exceptions

Wave 1A intentionally does not yet enforce preview separation, repo-wide scanning, or post-cleanup role purity.

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

## Freeze Rule for the Pilot Files

While the active cleanup campaign targets the four pilot files:

- `app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceScreen.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShell.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerViewModel.kt`

then:

- no major new UI trunk transplant may land into them
- no feature expansion may use them as the default landing zone
- only bug fixes, safety fixes, or doc-aligned hot fixes may touch them
- any hotfix must not widen the file's role mix, increase its exception scope, or normalize the file as a future landing target

If a freeze violation is unavoidable, record the reason and follow-up wave in this tracker.

---

## Tracked Files

Observed sizes below are the current audit snapshot used to seed the campaign on **2026-03-24**.

| File | Layer | Observed Size | Current Problem | Target Decomposition | Owner | Sunset | Required Tests | Wave | Status |
|------|-------|---------------|-----------------|----------------------|-------|--------|----------------|------|--------|
| `app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceScreen.kt` | UI | 108 LOC | Wave 1B moved SIM subtree, non-SIM sections, timelines, and previews out of the host entrypoint; host is now source-compatible and under budget | host + content + sections + SIM subtree + preview split | Codex | — | `GodStructureGuardrailTest`, `AgentIntelligenceStructureTest`, `SimComposerContractTest`, `SimHomeHeroExperimentContractTest` | 1B | Accepted |
| `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShell.kt` | UI Shell | 208 LOC | Wave 1C moved the large shell composition tree, reducers, telemetry, projections, shell actions, and follow-up sections out of the host entrypoint; the SIM shell now keeps runtime ownership and stays under budget | host + content + reducers + projections + telemetry + shell actions + sections | Codex | — | `GodStructureGuardrailTest`, `SimShellStructureTest`, `SimShellHandoffTest`, `SimConnectivityRoutingTest`, `SimRuntimeIsolationTest` | 1C | Accepted |
| `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt` | UI VM | 1516 LOC | One ViewModel owns session lifecycle, general chat, audio-grounded chat, follow-up behavior, and reconciliation | public VM + session coordinator + chat/audio/follow-up coordinators + projection helpers | Codex | Wave 1D | `SimAgentViewModelTest` | 1D | Exception |
| `app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerViewModel.kt` | UI VM | 1231 LOC | One ViewModel owns transcript ingress, mutation execution, reminder logic, attention projection, and warning paths | public VM + ingress + mutation + reminder + projection supports | Codex | Wave 1E | `SimSchedulerViewModelTest` | 1E | Exception |
| `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawer.kt` | UI | 136 LOC | Wave 3A moved drawer composition, card rendering, and support helpers out of the host entrypoint; the public drawer now keeps overlay/runtime wiring and delegation only | host + content + card/components + support helpers | Codex | — | `GodStructureGuardrailTest`, `SimAudioDrawerStructureTest`, `SimAudioDrawerViewModelTest`, `SimShellHandoffTest`, `:app-core:compileDebugUnitTestKotlin` | 3A | Accepted |
| `app-core/src/main/java/com/smartsales/prism/ui/onboarding/OnboardingScreen.kt` | UI | 896 LOC | Large screen with many steps; active UI development and SIM setup reuse make cleanup unsafe right now, so this stays tracked debt rather than Wave 2 scope | step host + step sections + preview/support split if needed later | Codex | later UI-safe cleanup wave | `OnboardingFlowTransitionTest`, `SimConnectivityPairingFlowTest`, `PairingFlowViewModelTest`, `OnboardingViewModelTest` | Later UI | Deferred |
| `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/gateway/GattBleGateway.kt` | Data/Transport | 1033 LOC | Transport, protocol parsing, gateway policy, and state edges are too concentrated | gateway seam + transport/session support + payload parser + gateway policy support | Codex | Wave 2B acceptance | `GattBleGatewayNotificationParsingTest`, `ConnectivityStructureTest`, `GodStructureGuardrailTest`, `:app-core:compileDebugUnitTestKotlin` | 2B | Proposed |
| `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManager.kt` | Data/Transport | 547 LOC | Under budget but still role-mixed; connection/session orchestration, reconnect/backoff policy, and notification ingress are too concentrated | manager seam + connection orchestration + reconnect/backoff policy + ingress/state support | Codex | Wave 2B acceptance | `DefaultDeviceConnectionManagerIngressTest`, `RealConnectivityBridgeTest`, `SimConnectivityRoutingTest`, `ConnectivityStructureTest`, `GodStructureGuardrailTest`, `:app-core:compileDebugUnitTestKotlin` | 2B | Proposed |
| `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinter.kt` | Domain | 100 LOC | Wave 2A moved normalization/time helpers, Uni parse lanes, and legacy compatibility out of the public seam; the host now keeps the source-compatible entrypoint and delegation only | public seam + normalize/time support + parse lane support + validation/legacy support | Codex | — | `SchedulerLinterTest`, `SchedulerLinterStructureTest`, `GodStructureGuardrailTest`, `:domain:scheduler:compileKotlin` | 2A | Accepted |
| `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepository.kt` | Data | 841 LOC | Persistence, artifact IO, binding management, and pipeline coordination are mixed | repository seam + persistence/store support + artifact IO support + session-binding support + sync/transcription coordinator | Codex | Wave 2C acceptance | `SimAudioRepositoryNamespaceTest`, `SimAudioRepositoryRecoveryTest`, `SimAudioRepositoryStructureTest`, `SimAudioDrawerViewModelTest`, `GodStructureGuardrailTest`, `:app-core:compileDebugUnitTestKotlin` | 2C | Proposed |

---

## Wave 1A Guardrail Delivery

Wave 1A now delivers two focused JVM structural checks inside:

- `app-core/src/test/java/com/smartsales/prism/ui/GodStructureGuardrailTest.kt`

1. **Tracker / Contract Validity Test**
   - verifies `docs/plans/god-tracker.md` exists
   - verifies `docs/specs/code-structure-contract.md` exists
   - verifies each pilot exception row has owner, sunset, required tests, target decomposition, and status

2. **Pilot Structural Budget Test**
   - reads the four pilot source files directly
   - measures current LOC
   - verifies each file is either under budget or has a valid exception row

Wave 1A still does not fail on preview separation, post-cleanup role purity, generic repo-wide scanning, or universal role inference.

Focused verification commands:

- `./gradlew :domain:scheduler:compileKotlin`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.GodStructureGuardrailTest`

Validation record:

- `docs/reports/tests/L1-20260324-god-wave1a-guardrails.md`

---

## Wave 1B AgentIntelligenceScreen Cleanup

Wave 1B now rewrites `AgentIntelligenceScreen.kt` into a host-only file and extracts the former mixed responsibilities into stable role files:

- `app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceContent.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceChatTimeline.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceHomeSections.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligencePreview.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentIntelligenceContent.kt`

Wave 1B also:

- moves SIM composer test tags and `resolveSimDynamicIslandIndex` ownership into the SIM UI shard
- updates the Wave 1A guardrail test so accepted rows no longer require a sunset
- adds a focused structure regression test for the extracted ownership seams
- explicitly defers stale SIM gesture androidTest seams to Wave 1C / `SimShell`

Wave 1B acceptance verification pack:

- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.GodStructureGuardrailTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.AgentIntelligenceStructureTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimComposerContractTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimHomeHeroExperimentContractTest`
- `./gradlew :app-core:compileDebugUnitTestKotlin`

Validation record:

- `docs/reports/tests/L1-20260324-god-wave1b-agent-intelligence.md`

---

## Wave 1C SimShell Cleanup

Wave 1C now rewrites `SimShell.kt` into a host-only file and extracts the former mixed shell responsibilities into stable SIM-owned support files:

- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellContent.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellReducer.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellActions.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellTelemetry.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellProjection.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellSections.kt`

Wave 1C also:

- keeps `SimShell(...)` source-compatible as the SIM runtime host mounted from `SimMainActivity`
- adds a focused structure regression test for the accepted shell split
- updates the Wave 1A guardrail test so `SimShell.kt` now must stay under the shell budget as an `Accepted` row
- keeps stale shell androidTest seams out of this acceptance bar so Wave 1C stays focused on source-owned structure

Wave 1C acceptance verification pack:

- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.GodStructureGuardrailTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimShellStructureTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimShellHandoffTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimConnectivityRoutingTest`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimRuntimeIsolationTest`
- `./gradlew :app-core:compileDebugUnitTestKotlin`

Validation record:

- `docs/reports/tests/L1-20260324-god-wave1c-sim-shell.md`

---

## Wave 2A SchedulerLinter Cleanup

Wave 2A now rewrites `SchedulerLinter.kt` into a public seam file and extracts the former mixed scheduler-linter responsibilities into stable domain-owned support files:

- `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinterSupport.kt`
- `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinterParsingSupport.kt`
- `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinterLegacySupport.kt`
- `domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinterLegacyContracts.kt`

Wave 2A also:

- keeps `SchedulerLinter` source-compatible for existing callers
- leaves the host file at `100 LOC`, below the transitional service/manager/linter/gateway budget
- adds a focused structure regression test for the accepted linter split
- repairs the tracker-owned Wave 2A row so the accepted file no longer remains a generic proposed debt item

Wave 2A acceptance verification pack:

- `./gradlew :domain:scheduler:test --tests com.smartsales.prism.domain.scheduler.SchedulerLinterTest --tests com.smartsales.prism.domain.scheduler.SchedulerLinterStructureTest`
- `./gradlew :domain:scheduler:compileKotlin`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.GodStructureGuardrailTest`

Validation record:

- `docs/reports/tests/L1-20260324-god-wave2a-scheduler-linter.md`

---

## Wave 3A SIM Audio Drawer Cleanup

Wave 3A now rewrites the SIM audio drawer slice into a thin public host file and extracts the former mixed responsibilities into stable SIM-owned UI support files:

- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerContent.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerCard.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerSupport.kt`

Wave 3A also:

- keeps `SimAudioDrawer.kt` source-compatible for current shell callers
- leaves the host file at `136 LOC`, below the transitional UI host budget
- keeps the accepted browse-vs-select drawer contract unchanged
- adds a focused drawer structure regression test for the accepted split
- repairs the tracker-owned Wave 3A row so the accepted file no longer remains deferred UI debt

Wave 3A acceptance verification pack:

- `./gradlew :app-core:compileDebugUnitTestKotlin :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimAudioDrawerStructureTest --tests com.smartsales.prism.ui.sim.SimAudioDrawerViewModelTest --tests com.smartsales.prism.ui.sim.SimShellHandoffTest --tests com.smartsales.prism.ui.GodStructureGuardrailTest`

Validation record:

- `docs/reports/tests/L1-20260324-god-wave3a-sim-audio-drawer.md`

## Related Documents

- `docs/plans/god-wave0-execution-brief.md`
- `docs/plans/god-wave1a-execution-brief.md`
- `docs/plans/god-wave1b-execution-brief.md`
- `docs/plans/god-wave1c-execution-brief.md`
- `docs/plans/god-wave2-execution-brief.md`
- `docs/plans/god-wave2a-execution-brief.md`
- `docs/plans/god-wave2b-execution-brief.md`
- `docs/plans/god-wave2c-execution-brief.md`
- `docs/plans/god-wave3a-execution-brief.md`
- `docs/specs/code-structure-contract.md`
- `docs/reports/tests/L1-20260324-god-wave1a-guardrails.md`
- `docs/reports/tests/L1-20260324-god-wave1b-agent-intelligence.md`
- `docs/reports/tests/L1-20260324-god-wave1c-sim-shell.md`
- `docs/reports/tests/L1-20260324-god-wave2a-scheduler-linter.md`
- `docs/reports/tests/L1-20260324-god-wave3a-sim-audio-drawer.md`

## Wave 1 Acceptance Bar

Wave 1 is accepted only when:

- this tracker is live and referenced from the main tracker
- shallow guardrails are added for the pilot files
- the four active trunk files are reduced below transitional caps or carry one explicit short-lived exception row
- focused behavior tests stay green
- future UI transplant targets become clearer than they are today

## Wave 0 Acceptance Bar

Wave 0 is accepted only when:

- `docs/plans/god-wave0-execution-brief.md` exists
- the four pilot files have valid temporary exception rows
- owner policy is explicit and set to `Codex`
- sunset waves are fixed to `1B / 1C / 1D / 1E`
- required verification packs are listed for each pilot file
- the freeze rule is documented
- Wave 1A has a decision-complete two-test design

---

## Related Documents

- `docs/plans/tracker.md`
- `docs/plans/ui-tracker.md`
- `docs/plans/god-wave0-execution-brief.md`
- `docs/specs/code-structure-contract.md`
- `docs/sops/ui-dev-mode.md`
- `docs/sops/ui-building.md`
- `docs/specs/prism-ui-ux-contract.md`
- `docs/specs/style-guide.md`
- `docs/specs/ui_element_registry.md`
