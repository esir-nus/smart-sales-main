# Dirty Tree Quarantine Tracker

> **Purpose**: Human-readable lane/build-state ledger for quarantining, splitting, landing, or parking the current dirty worktree before any branch-promotion step.
> **Status**: Active
> **Last Updated**: 2026-04-11
> **Primary Law**: `docs/specs/platform-governance.md`
> **Campaign Index**: `docs/plans/tracker.md`
> **Audit Report**: `docs/reports/20260404-dirty-tree-quarantine-audit.md`

---

## 1. Operating Rules

This tracker exists because the current branch is carrying one large mixed dirty tree while the normal working style uses 4-6 coding agents in parallel.

Rules:

1. One dirty lane = one bounded write scope.
2. Every dirty file must belong to exactly one lane, including any explicit `Deferred` row used to park or exclude residue from the current promotion path.
3. A paused, blocked, or transferred lane must link a current handoff file under `handoffs/`.
4. The same active lane ownership must also be recorded in `ops/lane-registry.json`.
5. If a lane needs files from another active lane, split the scope first. Do not silently share write scope.
6. `docs/plans/tracker.md` stays the campaign index only. This tracker owns lane-by-lane operational state.
7. No branch-default switch, Android beta baseline tag, or release-branch cut is allowed while this tracker still contains unassigned or mixed dirty work.
8. No lane may move to `Accepted` while its doc-code alignment state is unresolved.

---

## 2. Status Model

| Status | Meaning |
|--------|---------|
| `Proposed` | Audited dirty lane exists, but it is not the current active cleanup/build lane |
| `Active` | Lane is currently in building / cleanup inside its dedicated lane worktree |
| `Paused` | Lane is intentionally stopped, must keep its handoff current, and still reserves its owned paths |
| `Review` | Lane is no longer taking feature edits and is waiting for integration or review decisions |
| `Accepted` | Lane is stabilized enough to count as landed or promotion-ready |
| `Deferred` | Lane is intentionally parked or excluded from the current promotion path |

---

## 3. Current Audit Snapshot (2026-04-04)

## 3A. Machine Harness Contract

This tracker is the human lane ledger. The machine lane ledger is `ops/lane-registry.json`.

Harness rules:

- the integration tree is the repo root worktree and is integration-only
- feature work normally starts in a dedicated lane worktree
- each non-integration worktree should carry one lease at `.git/smart-sales/current-lane.json`
- reserved lane states are `Active`, `Paused`, and `Review`
- local hooks and CI both validate lane/path ownership through `scripts/lane_guard.py`

If this tracker and the registry disagree, treat the disagreement itself as governance debt and fix both in the same session.

---

Repo evidence from `git status --short` on `focus/20260402-runtime-home-shell-fidelity`:

- current branch is `884` commits ahead of `master` and `0` behind
- dirty tree is still mixed across code, docs, prototypes, handoffs, and repo guardrails
- rough dirty counts at audit time:
  - code paths: `102`
  - docs paths: `36`
  - prototype paths: `5`
  - handoff paths: `4`
  - other paths: `5`

Interpretation:

- this is not one safe landable batch
- the minimum safe cleanup unit is the lane board below

---

## 4. Lane Board

| Lane | Theme | Owned Write Scope | Explicit Off-Limits | Status | Alignment | Required Evidence | Current Handoff | Notes |
|------|-------|-------------------|---------------------|--------|-------------------|-----------------|-------|
| `DTQ-01` | Onboarding and quick-start | `app-core/.../ui/onboarding/**`, `app-core/.../data/onboarding/**`, onboarding tests, `docs/cerb/onboarding-interaction/**`, `docs/specs/flows/OnboardingFlow.md`, onboarding L3 report, onboarding prototype | scheduler core router, connectivity bridge/service, shared runtime shell host, cross-platform governance docs | `Active` | `Both pending` | focused onboarding unit/instrumentation evidence plus docs sync; current audit passed targeted onboarding unit suites and kept existing L3 recovery evidence in scope | `handoffs/schedule_quick_start_compose_handoff.md` | 2026-04-04 audit found no onboarding-specific doc/code contradiction, but the lane stays active until the still-dirty scope has fresh UI/device verification and a clean landing decision. |
| `DTQ-02` | Scheduler intelligence and reminder surfaces | scheduler drawer/reminder UI, `TaskReminderReceiver`, `AlarmActivity`, scheduler domain resolver, `IntentOrchestrator`, `SchedulerIntelligenceRouter`, scheduler tests, `docs/cerb/notifications/spec.md`, scheduler/reminder docs, scheduler drawer prototype | onboarding UI flow, connectivity stack, shell chrome, governance tracker docs | `Active` | `Both pending` | focused scheduler + reminder verification, plus shared-routing evidence; current audit passed targeted router/viewmodel/alarm-state suites and kept runtime reminder delivery as the remaining gap | `handoffs/notification_compose_handoff.md` | 2026-04-04 audit found no scheduler/reminder-specific doc/code contradiction, but the lane stays active until the still-dirty scope has fresh runtime/device reminder verification and a clean landing decision. |
| `DTQ-03` | Connectivity and OEM hardening | `RealConnectivityBridge`, `RealConnectivityService`, `ConnectivityViewModel`, `ConnectivityModal`, `OemCompat`, connectivity/OEM tests, `docs/cerb/connectivity-bridge/**`, `docs/cerb/sim-connectivity/spec.md`, `docs/specs/connectivity-spec.md`, `docs/plans/bug-tracker.md`, Harmony/OEM SOP docs and plan | onboarding quick-start internals, scheduler mutation/routing, shared runtime shell chrome, governance tracker docs | `Active` | `Aligned` | connectivity-focused tests and, when runtime diagnosis is involved, `adb logcat` evidence | `handoffs/oem_permission_diagnosis.md` | 2026-04-04 follow-up fixed `RealConnectivityBridge.recordingNotifications()` so raw badge events are filtered at transport-ready time, the focused connectivity/OEM unit bundle passes, and Xiaomi device logs now prove the reminder receiver/full-screen-notification branch; the lane stays active because the recording-ready bridge branch was not reproduced on-device and connectivity runtime still shows listener/readiness gaps. |
| `DTQ-04` | Runtime shell and SIM chrome | `MainActivity`, `RuntimeShell`, SIM shell/home/history/audio drawer chrome, shell reducer/content/coordinators, shell UI tests, shell/audio/core-flow docs, UI tracker, shell/audio prototypes | onboarding flow internals, connectivity transport implementation, scheduler router core, governance docs | `Active` | `Both pending` | focused shell/UI tests and visual/behavior proof for touched chrome | `handoffs/audio_drawer_compose_handoff.md` | Current dirty lane should land after focused stabilization and synced doc/code updates. |
| `DTQ-05` | Voice runtime and pipeline support | `DeviceSpeechRecognizer`, `SimRealtimeSpeechRecognizer`, `RealUnifiedPipeline`, `PromptCompiler`, `PipelineModels`, related tests, `docs/cerb/unified-pipeline/spec.md` | onboarding page composition, runtime shell chrome, connectivity service layer, governance docs | `Active` | `Both pending` | focused realtime/pipeline tests; if auth/runtime behavior is involved, capture log evidence instead of inferring | — | Keep as one lane only while the write scope stays bounded; split if alignment drifts across multiple feature owners. |
| `DTQ-06` | Governance, trackers, and repo guardrails | `AGENTS.md`, `.agent/**`, `.claude/**`, `.codex/**`, `.github/**`, `.githooks/**`, `CLAUDE.md`, `docs/specs/platform-governance.md`, `docs/sops/lane-worktree-governance.md`, `docs/sops/tracker-governance.md`, `docs/reference/platform-targets.md`, `docs/platforms/**`, `docs/plans/tracker.md`, `docs/plans/sim-tracker.md`, `docs/cerb/interface-map.md`, `ops/lane-registry.json`, `scripts/lane_guard.py`, `scripts/lane`, `scripts/install-hooks.sh`, dirty-tree audit/control-plane docs | feature-owned code lanes except for final docs sync references and lane-local handoff files | `Active` | `Aligned` | docs consistency re-read plus local and CI lane-harness verification | — | Reopened on 2026-04-11 to own the lane harness control plane, shared hooks, registry, CI enforcement, and cross-runtime registration surfaces. |
| `DTQ-07` | Cross-feature handoff/workflow assets | `.agent/workflows/compose-handoff-writer-[tool].md` and any future cross-feature handoff-generator/support assets | feature code, feature docs, branch-governance docs unless explicitly moved | `Deferred` | `Deferred` | confirm whether the asset is truly reusable project tooling rather than lane-local residue | — | Park unless it proves to be real reusable workflow infrastructure. |
| `DTQ-99` | Foreign local artifact | `wechat_4.1.1.4-2_amd64.deb` | all product code/docs | `Deferred` | `Deferred` | explicit confirmation that the file is not part of the product deliverable | — | Exclude from the current promotion path; do not treat as product work. |

---

## 5. Lane Notes and Split Rules

### `DTQ-01` Onboarding and quick-start

- Owns the onboarding feature seam end-to-end, including the quick-start staging/commit path and the one-shot shell handoff gate.
- Must not silently broaden into connectivity transport internals or shared scheduler router internals.

### `DTQ-02` Scheduler intelligence and reminders

- Owns the shared scheduler router, reminder surfaces, drawer reminder presentation, and alarm-screen behavior.
- If a change also needs onboarding staged-flow behavior, coordinate via `DTQ-01` instead of merging both into one lane.

### `DTQ-03` Connectivity and OEM hardening

- Owns transport truth, OEM settings guidance, and connectivity manager/modal behavior.
- Notification/reminder policy wording that is purely OEM-delivery-specific belongs here; shared reminder semantics stay with `DTQ-02`.

### `DTQ-04` Runtime shell and SIM chrome

- Owns shell chrome, drawer presentation, SIM visual polish, and shell-only routing affordances.
- Must not become the backdoor owner of scheduler truth, onboarding flow logic, or connectivity transport logic.

### `DTQ-05` Voice runtime and pipeline support

- This is the only current lane intentionally allowed to support both onboarding and SIM chat because the audited dirty files are runtime/pipeline seams rather than surface-specific UI files.
- If this lane grows beyond the current bounded file set, split it before continuing.

### `DTQ-06` Governance, trackers, and repo guardrails

- Owns only repo control-plane work.
- Owns the shared operator-registration surfaces used by Codex, Antigravity, and Claude so multi-agent collaboration stays inside one declared governance lane instead of becoming invisible dirty-tree drift.
- Feature lanes may update their own local feature docs, but cross-cutting tracker/index/ownership docs should flow through this lane during integration.

---

## 6. Doc-Code Alignment Appendix

This appendix applies to every lane in this tracker.

### Core rule

- no code stays ahead of doc
- no doc stays ahead of code when the doc claims current or shipped behavior
- if docs are intentionally ahead, record the gap explicitly as drift or deferred follow-up instead of pretending the lane is aligned

### Source-of-truth order

For lane alignment, use the repo's existing delivery hierarchy:

1. `docs/core-flow/**` when it exists
2. owning spec / interface docs
3. code
4. PU / focused verification
5. fix loop back through docs, then code, until aligned

### Alignment field values

| Alignment | Meaning |
|-----------|---------|
| `Aligned` | Owning docs, code, and required verification currently match |
| `Doc update required` | Code changed or is changing, and owning docs still trail it |
| `Code update required` | Docs describe current behavior that code still does not deliver |
| `Both pending` | Code and docs are both in flight and must be synchronized before `Accepted` |
| `Deferred` | Lane is intentionally parked and not claiming current shipped alignment |

### Lane update rule

Any lane that changes behavior, interfaces, states, or ownership must update the owning docs in the same working session.

Minimum sync targets, when applicable:

- owning `docs/core-flow/**`
- owning `docs/cerb/**/spec.md`
- owning `docs/cerb/**/interface.md`
- `docs/plans/tracker.md`
- `docs/cerb/interface-map.md`

### Acceptance rule

A lane may remain `Active` while alignment is pending.

A lane may move to `Accepted` only when:

- its alignment field is `Aligned`
- its owning docs and code no longer contradict each other
- its required verification evidence exists

---

## 7. Promotion Gate

Branch promotion readiness is blocked until:

- every dirty path is assigned to one lane, including any explicit `Deferred` row used for parked or excluded residue
- no lane remains implicitly mixed across multiple active lanes
- the same active lane ids, statuses, handoffs, and owned scopes are reflected in `ops/lane-registry.json`
- every paused lane has a current handoff artifact
- every `Active` lane has its focused verification surface defined
- no lane being treated as landable still has unresolved doc-code drift
- no lane moves to `Accepted` unless its alignment field is `Aligned`

When this table reaches a reviewable state, the next human/operator action is to move lanes from `Active` to `Accepted` or `Deferred` rather than continuing to code directly into the mixed global dirty tree.

---

## 8. Lane Audit Update (2026-04-04)

- `DTQ-01` through `DTQ-04` now have current handoff files backfilled to the registry contract, including `Lane ID`, owning source-of-truth docs, explicit doc-code alignment state, required evidence, and collision notes.
- `DTQ-01` was audited against `docs/specs/flows/OnboardingFlow.md`, `docs/cerb/onboarding-interaction/**`, and the live quick-start / commit / shell-handoff code. Targeted unit suites passed, and no onboarding-specific doc/code contradiction was found, but the lane remains `Active` / `Both pending` because this session did not include a fresh Compose instrumentation or device rerun for the still-dirty onboarding scope.
- `DTQ-02` was audited against the shared scheduler-routing and reminder docs plus the live `SchedulerIntelligenceRouter` / `IntentOrchestrator` / `RealUnifiedPipeline` / `TaskReminderReceiver` / `AlarmActivity` / `SimSchedulerViewModel` code. Targeted router/viewmodel/alarm-state suites passed, and no scheduler/reminder-specific doc/code contradiction was found, but the lane remains `Active` / `Both pending` because this session did not include a fresh runtime/device rerun for reminder delivery and alarm presentation in the still-dirty scope.
- `DTQ-03` was audited against `docs/specs/connectivity-spec.md`, `docs/cerb/connectivity-bridge/**`, `docs/cerb/sim-connectivity/spec.md`, the OEM reminder docs, and the live `RealConnectivityBridge` / `RealConnectivityService` / `ConnectivityViewModel` / `OemCompat` / `ReminderReliabilityAdvisor` / `TaskReminderReceiver` code. The bridge follow-up is now implemented, the targeted connectivity/OEM unit bundle passes with the transport-readiness suppression branch covered, and the lane is back to `Active` / `Aligned`. No attached device was available for fresh `adb logcat`, so OEM/runtime closure remains open.
- A DTQ-03 on-device pass on April 4, 2026 against Xiaomi `2410DPN6CC` (Android 16 / SDK 36) now adds real `adb logcat` evidence for the OEM reminder branch: `TaskReminderReceiver` logged `收到任务提醒`, `fullScreenIntent 已设置 (DEADLINE)`, and `通知已显示` for task `a3476252-efa3-4c00-8c17-5ee7545bc133`, while the package snapshot showed notification / full-screen / exact-alarm permissions granted. The same pass did not capture any `recording ready` bridge event, and the connectivity runtime still showed repeated `notification listener inactive`, unreadable phone SSID (`<unknown ssid>`), and HTTP reachability failures against the badge endpoint, so DTQ-03 remains `Active` despite the partial runtime proof.
- `DTQ-06` was reopened on 2026-04-11 as `Active` after the lane harness landed: the control-plane scope now also owns `ops/lane-registry.json`, shared `.githooks/`, `scripts/lane_guard.py`, `scripts/lane`, the CI branch/path validator, `AGENTS.md`, `.codex/**`, and the Claude/Antigravity registration surfaces that must stay aligned with the harness.
- `DTQ-07` remains `Deferred`; the current dirty asset `.agent/workflows/compose-handoff-writer-[tool].md` is still parked as reusable-tooling residue rather than current promotion-scope product work.
- `DTQ-99` remains `Deferred`; `wechat_4.1.1.4-2_amd64.deb` is still treated as foreign local residue and excluded from the product promotion path.
- `DTQ-01`, `DTQ-02`, `DTQ-04`, and `DTQ-05` remain non-accepted implementation lanes because the current dirty inventory still spans code, docs, and tests with `Both pending` alignment and without a final focused verification bundle.
- `DTQ-03` separately remains non-accepted because the runtime/device verification bundle is still missing even though the bridge-contract mismatch is now closed.
