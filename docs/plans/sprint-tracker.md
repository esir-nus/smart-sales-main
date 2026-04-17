# Harmony Sprint Tracker

> **Purpose**: Persistent log of HarmonyOS sprint contracts (feature + debug), in execution order. Each entry is a sprint contract extended with outcome and governance-residue fields.
> **Status**: Active
> **Last Updated**: 2026-04-17
> **Primary Laws**:
> - `docs/specs/harness-manifesto.md` (sprint contract semantics)
> - `docs/specs/platform-governance.md`
> - `docs/specs/cross-platform-sync-contract.md`
> **Related**:
> - `.claude/commands/sprint.md` (entry point — `/sprint` produces contracts in the shape below)
> - `docs/plans/harmony-ui-translation-tracker.md` (page-level UI detail, referenced from UI sprints)
> - `docs/plans/tracker.md` (cross-platform campaign hub)
> - `docs/sops/debugging.md` (debug sprint evidence discipline)

---

## 1. Operating Posture

HarmonyOS-native is the primary forward platform. Every nontrivial Harmony task enters through `/sprint`, which produces a sprint contract. This tracker is where those contracts persist: active, blocked, accepted, deferred, absorbed.

Rules:
- Android uses `tracker.md`, `god-tracker.md`, and friends; this tracker is Harmony only.
- Feature and debug sprints share the same entry schema. Debug sprints set `Engine: Debug`, `Work Class: debug`.
- `harmony-ui-translation-tracker.md` owns page-by-page ArkUI rewrite evidence and is referenced from UI sprints; do not duplicate page pass detail here.
- Branch rules per `CLAUDE.md`: feature branches fork from `platform/harmony` and PR back; no direct commits to the trunk.
- iOS will eventually use an equivalent `IS-NNN` namespace in its own tracker; Harmony sprint IDs use `HS-NNN`.
- Enforcement: `.claude/hooks/harmony-sprint-gate.sh` blocks `platforms/harmony/**` edits (Write/Edit/NotebookEdit) unless at least one entry below has `Status: Active` or `Status: Blocked`. See `docs/specs/platform-governance.md` §3.3.

---

## 2. Status Model

| Status | Meaning |
|---|---|
| `Proposed` | Contract drafted, not yet executing |
| `Active` | Operator executing inside the contract bounds |
| `Blocked` | Waiting on capability, tooling, signing, or contract clarification |
| `Accepted` | Success criteria met, evaluator confirmed, bounded slice stable |
| `Deferred` | Parked intentionally, no parity claim |
| `Absorbed` | Superseded by a later sprint; retained as historical record |

---

## 3. Sprint Entry Schema

Every entry uses this shape. Fields marked *(conditional)* appear only when relevant. Backend-specific sections apply to `Engine: Backend/Pipeline` only.

```markdown
### HS-NNN: [Slice Name]

- ID: HS-NNN
- Engine: Backend/Pipeline | UI | Architecture | Governance | Doc | Debug
- Work Class: feature | debug
- Branch: platform/harmony | harmony/{name}
- Evidence class: L1 | L2 | L3
- Status: Proposed | Active | Blocked | Accepted | Deferred | Absorbed
- Owner: <role or lane>
- Opened: YYYY-MM-DD
- Last Updated: YYYY-MM-DD

**Source of Truth**:
- <core-flow / cerb / spec paths read before planning>

**Scope**:
- <bounded, specific>

**Pipeline contract** *(backend only)*:
- <typed mutations>

**Critical dataflow joints** *(backend only)*:
1. <joint 1>
2. <joint 2>

**Mini-lab scenarios** *(backend only)*:
- <scenario -> expected outcome>
- <negative scenario -> expected safe-fail>

**Success criteria**:
- <what the evaluator checks>

**Deferred scope**:
- <explicit non-goals — absorbs "Does Not Own" and "User-visible Limitation">

**Roles**:
- Planner / Operator / Evaluator

**Evidence log** *(appended during execution)*:
- YYYY-MM-DD: <build/deploy/hilog/test result, link or commit>

**Branch Restore Snapshot** *(conditional — long-lived or cross-session sprints)*:
- Baseline commit | Current head | Restore confidence

**Disabled Set** *(conditional — when sprint intentionally hides features to preserve honesty)*:
- <features hidden/blocked during this sprint>

**Outcome / Drift** *(filled at Accepted / Absorbed / Deferred)*:
- <what actually happened, what drifted from scope>
```

### Debug sprint variant

- `Engine: Debug`, `Work Class: debug`.
- `Scope:` = "Root-cause and fix <symptom>".
- `Mini-lab scenarios` hold reproduction steps + expected post-fix behavior.
- `Success criteria` express red/green: bug reproduced on record, fix verified with runtime evidence (hilog per `docs/sops/debugging.md`).
- `Evidence log` carries the runtime proof — mandatory for L3 debug sprints.

---

## 4. Index

| ID | Title | Engine | Status | Last Updated |
|---|---|---|---|---|
| HS-001 | Tingwu container (pattern foundation) | Backend/Pipeline | Absorbed (into HS-005) | 2026-04-11 |
| HS-002 | Scheduler backend-first Phase 1 | Backend/Pipeline | Active | 2026-04-11 |
| HS-003 | ArkUI UI verification lane | UI | Active | 2026-04-12 |
| HS-004 | Complete-native app — Phase 1 shell | Architecture | Accepted | 2026-04-16 |
| HS-005 | Complete-native app — Phase 2A audio pipeline | Backend/Pipeline | Active | 2026-04-17 |
| HS-006 | Complete-native app — Phase 2B scheduler | Backend/Pipeline | Proposed | 2026-04-17 |
| HS-007 | Complete-native app — Phase 2C AI/chat | Backend/Pipeline | Proposed | 2026-04-17 |
| HS-008 | Complete-native app — Phase 2D CRM | Backend/Pipeline | Proposed | 2026-04-17 |
| HS-009 | Complete-native app — Phase 2E device/onboarding/settings | Architecture | Proposed (Blocked on BLE) | 2026-04-17 |

---

## 5. Full Entries

### HS-001: Tingwu container (pattern foundation)

- ID: HS-001 (legacy alias: H-01)
- Engine: Backend/Pipeline
- Work Class: feature
- Branch: `harmony/tingwu-container`
- Evidence class: L3
- Status: Absorbed (into HS-005 and successors under HS-004)
- Owner: Harmony-native audio pipeline lane
- Opened: 2025 (inherited)
- Last Updated: 2026-04-11

**Source of Truth**:
- `docs/platforms/harmony/native-development-framework.md`
- `docs/platforms/harmony/tingwu-container.md`
- shared Tingwu/audio contracts referenced by the overlay

**Scope**:
- Transient HarmonyOS-native Tingwu container: document-picker ingress, Harmony-local file persistence, OSS upload, Tingwu polling, artifact reopen, Harmony-owned runtime config generation.

**Pipeline contract**:
- Contract-preserving native rewrite of the shared Tingwu/audio ingress → upload → poll → artifact pipeline.

**Critical dataflow joints**:
1. Document-picker ingress → local persistence
2. Local file → OSS upload
3. OSS object → Tingwu task submission + polling
4. Artifact persistence → reopen without rerunning Tingwu

**Mini-lab scenarios**:
- Local/phone audio import → Tingwu submission → progress observation → transcript + artifact rendering.
- Persisted artifact reopen without rerunning Tingwu.

**Success criteria**:
- Evaluator confirms the bounded Tingwu capability set runs on a signed Harmony app with hilog evidence.

**Deferred scope** *(absorbs "Does Not Own" + "User-visible Limitation")*:
- Scheduler create/reschedule/follow-up/reminder/discovery surfaces.
- Scheduler-related onboarding promises and handoff flows.
- Ask-AI, audio-grounded chat, session-binding lanes.
- Badge pairing/sync/download, `ConnectivityBridge`, `BadgeAudioPipeline`.
- Shared scheduler semantics, shared onboarding truth, the internal ArkUI UI verification package, Android compatibility behavior on Huawei/Honor/Harmony devices, repo-wide branch governance outside this Harmony lane.
- User-visible posture: this is a Tingwu container, not a parity Harmony version of the Android app.

**Roles**:
- Planner/Operator/Evaluator: Harmony-native audio pipeline lane.

**Branch Restore Snapshot**:
- Baseline: `401772ab`
- Current head at absorption: `adbecd0a`
- Restore confidence: manual git restore is possible; CI-backed restore remains deferred until a Harmony lane exists in CI.

**Disabled Set**:
- Scheduler create, reschedule, follow-up, reminder, discovery surfaces.
- Scheduler-related onboarding promises or handoff flows.
- Ask-AI, audio-grounded chat, session-binding lanes.
- Badge pairing/sync/download, `ConnectivityBridge`, `BadgeAudioPipeline`.

**Outcome / Drift**:
- Absorbed into HS-005 as the architectural pattern foundation for the complete-native Phase 2A audio pipeline. Public capability boundary remained unchanged through Stage 2 tracking. Dedicated ArkUI UI rewrite lane moved to HS-003 (internal root, separate page tracker) rather than widening this app silently.

---

### HS-002: Scheduler backend-first Phase 1

- ID: HS-002 (legacy alias: H-02)
- Engine: Backend/Pipeline
- Work Class: feature
- Branch: `harmony/tingwu-container` (bounded proof root)
- Evidence class: L3 (signed `smartsales.HOS.test` lane + hilog)
- Status: Active
- Owner: Harmony scheduler backend verification lane
- Opened: 2026-04
- Last Updated: 2026-04-11

**Source of Truth**:
- `docs/platforms/harmony/scheduler-backend-first.md`
- `docs/core-flow/scheduler-fast-track-flow.md`
- `docs/cerb/scheduler-path-a-spine/spec.md`
- `docs/cerb/scheduler-path-a-spine/interface.md`
- `docs/cerb-ui/scheduler/contract.md`
- `docs/plans/harmony-scheduler-backend-phase1-brief.md`

**Scope**:
- Operator-only scheduler backend mini-lab that proves dataflow observability for exact-create and vague-create paths on Harmony, without claiming reminder or UI parity.

**Pipeline contract**:
- Scheduler ingress → classification (exact vs vague) → Path A commit → local Harmony persistence.
- Collapsed internal scaffold covers conflict, reschedule, and null safe-fail backend completion.

**Critical dataflow joints**:
1. Ingress owner registration and owner-chain visibility
2. Classification of exact vs vague create
3. Path A commit visibility
4. Local persistence of scheduler verification tasks + trace events under the Harmony root

**Mini-lab scenarios**:
- Operator-seeded exact-create execution → visible commit + trace.
- Operator-seeded vague-create execution → visible commit + trace.
- Collapsed internal conflict/reschedule/null-fail triggers → backend completion without widening public scope.

**Success criteria**:
- Critical-joint telemetry exists for ingress, classification, commit, local persistence.
- Local Harmony snapshots prove dataflow is observable without claiming reminder or UI parity.
- Toolchain/device proof stays aligned with `docs/platforms/harmony/test-signing-ledger.md`, including the mini-lab evidence note for the signed `smartsales.HOS.test` lane.

**Deferred scope**:
- User-facing scheduler parity; reminder and alarm delivery parity; onboarding quick-start scheduler handoff; badge-driven scheduler ingress.
- Shared scheduler semantics (owned by develop); Harmony reminder/alarm adapter work; onboarding scheduler promises; Android scheduler implementation shape.
- User-visible posture: operator-only scheduler backend verification sandbox, not the real Harmony scheduler product surface.

**Roles**:
- Planner/Operator/Evaluator: Harmony scheduler backend verification lane.

**Branch Restore Snapshot**:
- Baseline: `adbecd0a`
- Current head: working tree scaffold after scheduler backend-first slice
- Restore confidence: source-level restore straightforward; build/device confidence unverified until Harmony toolchain runs locally.

**Disabled Set**:
- User-facing scheduler parity.
- Reminder/alarm delivery parity.
- Onboarding quick-start scheduler handoff.
- Badge-driven scheduler ingress.

**Notes / Drift**:
- Slice exists because backend/dataflow certainty is more valuable than UI parity at the current Harmony scheduler stage.
- Broader scaffold cases (conflict/reschedule/null-fail) intentionally collapsed behind internal verification controls so backend completion proceeds without widening the default visible Harmony scheduler scope.
- Current successful compile → signing → `hdc` deploy → launch → `hilog` chain recorded in `docs/platforms/harmony/test-signing-ledger.md`; do not transfer that proof to `smartsales.HOS.ui` until the UI package has its own signing lane.
- If reminder/alarm delivery starts, reclassify the new slice (likely a new HS-NNN platform-adapter sprint) instead of quietly widening this row.

---

### HS-003: ArkUI UI verification lane

- ID: HS-003 (legacy alias: H-03)
- Engine: UI
- Work Class: feature
- Branch: `harmony/tingwu-container` (internal UI verification root)
- Evidence class: L3 (signed HAP + device install pending)
- Status: Active
- Owner: Harmony internal UI verification lane
- Opened: 2026-04
- Last Updated: 2026-04-12

**Source of Truth**:
- `docs/platforms/harmony/ui-verification.md`
- `docs/plans/harmony-ui-translation-tracker.md`
- `docs/specs/prism-ui-ux-contract.md`
- relevant shared `docs/cerb-ui/**` and `docs/core-flow/**` docs per page
- `docs/platforms/harmony/hui-02-tingwu-docking-contract.md` (HUI-02 docking)

**Scope**:
- Internal-only page-by-page native ArkUI rewrite and on-device verification, distinct from backend mini-labs. Dedicated bundle identity (`smartsales.HOS.ui`) separate from HS-002 so UI checks can stay isolated.

**Success criteria**:
- Every page pass recorded in `docs/plans/harmony-ui-translation-tracker.md`.
- Mock-vs-docked status explicit per page.
- Build/install/log evidence recorded before any page is called `Pass`.

**Deferred scope**:
- Public scheduler parity; reminder/alarm parity; public onboarding scheduler handoff; any claim that mock-backed pages are real backend-complete delivery.
- Shared product semantics; backend/dataflow proof; public capability widening for the Tingwu container; Android compatibility on Huawei/Honor/Harmony devices.
- User-visible posture: internal Harmony UI verification package, not the public Harmony product app.

**Roles**:
- Planner/Operator/Evaluator: Harmony internal UI verification lane.

**Evidence log**:
- 2026-04-12: local UI-lane signed HAP proof exists; install script refuses missing/wrong-bundle AGC assets; device-accepted install proof still pending.
- 2026-04: HUI-02 Tingwu docking contract defined at `docs/platforms/harmony/hui-02-tingwu-docking-contract.md` — projection from backend snapshot to page-facing adapter state.

**Branch Restore Snapshot**:
- Baseline: `adbecd0a`
- Current head: working tree scaffold after Stage 2 Harmony UI split
- Restore confidence: source-level restore straightforward; local signed-HAP generation repeatable; AGC preflight lane wired; full device-install confidence blocked until a real `smartsales.HOS.ui` AGC asset set exists.

**Disabled Set**:
- Public scheduler parity.
- Reminder/alarm parity.
- Public onboarding scheduler handoff.
- Any claim that mock-backed pages are real backend-complete delivery.

**Notes / Drift**:
- Page-pass detail delegated to `docs/plans/harmony-ui-translation-tracker.md`.
- Bundle identity separate from the mini-lab so later on-device UI checks stay isolated.
- HUI-02 no longer uses inline mock cards; it runs through a dedicated Tingwu page adapter seam.
- HUI-03 scheduler docking contract deferred until the scheduler backend (HS-002) graduates from operator-only mode.

---

### HS-004: Complete-native app — Phase 1 shell

- ID: HS-004 (legacy alias: H-04 Phase 1)
- Engine: Architecture
- Work Class: feature
- Branch: `platform/harmony`
- Evidence class: L3
- Status: Accepted
- Owner: Harmony-native complete app lane
- Opened: 2026-03
- Last Updated: 2026-04-16

**Source of Truth**:
- `docs/platforms/harmony/app-architecture.md`
- `docs/platforms/harmony/native-development-framework.md`
- `docs/specs/cross-platform-sync-contract.md`

**Scope**:
- App shell, navigation, lifecycle, and runtime config generation for `platforms/harmony/smartsales-app/`. Foundation on which Phase 2A+ feature sprints build.

**Success criteria**:
- App scaffold builds and launches on device.
- Navigation, lifecycle, and config generation wired.

**Deferred scope**:
- All Phase 2 feature surfaces (audio, scheduler, AI/chat, CRM, device/onboarding).
- Shared product semantics (owned by develop); Android implementation (owned by Android lineage); cross-platform governance (owned by `docs/specs/platform-governance.md`).

**Roles**:
- Planner/Operator/Evaluator: Harmony-native complete app lane.

**Branch Restore Snapshot**:
- Baseline: `30353b3e1` (post-migration-restructure sync)

**Outcome / Drift**:
- Phase 1 accepted. Phase 2A audio pipeline (HS-005) now builds on this foundation and absorbs HS-001 Tingwu container patterns.

---

### HS-005: Complete-native app — Phase 2A audio pipeline

- ID: HS-005 (legacy alias: H-04 Phase 2A)
- Engine: Backend/Pipeline
- Work Class: feature
- Branch: `platform/harmony`
- Evidence class: L3 (device hilog pending)
- Status: Active
- Owner: Harmony-native complete app lane
- Opened: 2026-04
- Last Updated: 2026-04-17

**Source of Truth**:
- `docs/platforms/harmony/app-architecture.md`
- `docs/platforms/harmony/native-development-framework.md`
- `docs/specs/cross-platform-sync-contract.md`
- shared `docs/core-flow/**` and `docs/cerb/**` audio contracts
- HS-001 patterns (absorbed)

**Scope**:
- Native ArkTS audio pipeline for the complete-native app: Tingwu import, OSS upload, polling, artifact rendering. Migrates HS-001 Tingwu container patterns into `platforms/harmony/smartsales-app/`.

**Pipeline contract**:
- Document-picker ingress → local persistence → OSS upload → Tingwu submission/poll → artifact persistence and reopen.

**Critical dataflow joints**:
1. Picker → `FileStore` local persistence
2. `FileStore` → `OssService` upload
3. `OssService` object → `TingwuService` submit + poll
4. `AudioRepository` → `AudioPage` rendering and reopen

**Mini-lab scenarios**:
- Local audio import → upload → poll → transcript + artifact on device.
- Persisted artifact reopen without rerunning Tingwu.

**Success criteria**:
- hvigor build succeeds; `hdc install` accepted on device; hilog shows each pipeline joint firing end to end.
- Domain model translations match Kotlin `domain/` semantics (`SignatureUtils`, `FormatUtils`, `Audio.ets`).

**Deferred scope**:
- Phases 2B–2E (scheduler, AI/chat, CRM, device/onboarding).
- Badge pairing (blocked on HarmonyOS NEXT BLE API).

**Roles**:
- Planner/Operator/Evaluator: Harmony-native complete app lane.

**Evidence log**:
- 2026-04-16: service layer complete — `HttpClient`, `FileStore`, `Picker`, `OssService`, `TingwuService`, `AudioRepository`, `AudioPage`, `Index.ets` wiring. Builds and type-checks.
- 2026-04-16: `SignatureUtils`, `FormatUtils`, `Audio.ets`, `AppConfig`/`AppRuntimeConfig` migrated and in place.
- Pending: hvigor build + `hdc install` + hilog end-to-end device evidence.

**Branch Restore Snapshot**:
- Baseline: `30353b3e1`
- Current head: Phase 2A service layer complete
- Restore confidence: builds and type-checks; device-evidence gate next.

**Notes / Drift**:
- Phase 2A supersedes HS-001 as the primary Tingwu delivery vehicle inside the complete-native app.
- Phase 2A Disabled Set inherits from HS-001 until the phase reaches Accepted.

---

### HS-006: Complete-native app — Phase 2B scheduler

- ID: HS-006 (legacy alias: H-04 Phase 2B)
- Engine: Backend/Pipeline
- Work Class: feature
- Branch: `platform/harmony`
- Evidence class: L3 (planned)
- Status: Proposed
- Owner: Harmony-native complete app lane
- Opened: 2026-04-17
- Last Updated: 2026-04-17

**Source of Truth**:
- `docs/core-flow/scheduler-fast-track-flow.md`
- `docs/cerb/scheduler-path-a-spine/spec.md`, `interface.md`
- `docs/cerb-ui/scheduler/contract.md`
- HS-002 backend proof outputs

**Scope**:
- Ship the scheduler (create, conflict, reminders) inside the complete-native app, graduating HS-002's operator-only backend proof into a user-facing surface.

**Success criteria**:
- Exact-create and vague-create paths hit the complete-native scheduler UI.
- Conflict and reschedule paths honor shared semantics.
- Reminder/alarm delivery via Harmony platform adapter (may split into a separate HS-NNN sprint if it grows).

**Deferred scope**:
- Badge-driven scheduler ingress (blocked on HS-009 BLE availability).

**Roles**:
- To be assigned on activation.

---

### HS-007: Complete-native app — Phase 2C AI/chat

- ID: HS-007 (legacy alias: H-04 Phase 2C)
- Engine: Backend/Pipeline
- Work Class: feature
- Branch: `platform/harmony`
- Evidence class: L3 (planned)
- Status: Proposed
- Owner: Harmony-native complete app lane
- Opened: 2026-04-17
- Last Updated: 2026-04-17

**Source of Truth**:
- Shared `docs/core-flow/**` chat/session docs
- `docs/cerb/**` LLM/session contracts

**Scope**:
- LLM conversation and session binding in the complete-native app.

**Success criteria**:
- Chat flows round-trip through LLM pipeline with session persistence; parity with Android session semantics.

**Deferred scope**:
- Audio-grounded chat beyond basic session binding until HS-005 reaches Accepted.

---

### HS-008: Complete-native app — Phase 2D CRM

- ID: HS-008 (legacy alias: H-04 Phase 2D)
- Engine: Backend/Pipeline
- Work Class: feature
- Branch: `platform/harmony`
- Evidence class: L3 (planned)
- Status: Proposed
- Owner: Harmony-native complete app lane
- Opened: 2026-04-17
- Last Updated: 2026-04-17

**Source of Truth**:
- Shared CRM domain and data contracts

**Scope**:
- CRM / entity management surfaces in the complete-native app.

---

### HS-009: Complete-native app — Phase 2E device/onboarding/settings

- ID: HS-009 (legacy alias: H-04 Phase 2E)
- Engine: Architecture
- Work Class: feature
- Branch: `platform/harmony`
- Evidence class: L3 (planned)
- Status: Proposed (Blocked on HarmonyOS NEXT BLE API for badge pairing)
- Owner: Harmony-native complete app lane
- Opened: 2026-04-17
- Last Updated: 2026-04-17

**Source of Truth**:
- Shared onboarding and settings contracts
- Badge pairing / `ConnectivityBridge` / `BadgeAudioPipeline` specs (for the badge slice)

**Scope**:
- Device integration, onboarding, settings. Badge pairing is a child slice that stays deferred until HarmonyOS NEXT exposes the required BLE APIs — likely a separate HS-NNN sprint when unblocked.

**Deferred scope**:
- Badge pairing, badge sync, badge download until HarmonyOS NEXT BLE API availability.
- Real badge Wi-Fi provisioning push after the current audio-sync failure prompt lands; the present Phase 2E placeholder may open the Wi-Fi dialog and validate user input, but it must keep the submit path stubbed and explicitly labeled until the BLE / device-integration lane is available.
