# Dirty Tree Quarantine Tracker

> **Purpose**: Human-readable lane/build-state ledger for quarantining, splitting, landing, or parking the current dirty worktree before any branch-promotion step.
> **Status**: Active
> **Last Updated**: 2026-04-13
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
| `Awaiting evidence` | Work is complete enough for review but required proof for the declared evidence class is still pending; lane still reserves its owned paths |
| `Review` | Lane is no longer taking feature edits and is waiting for integration or review decisions |
| `Accepted` | Lane is stabilized enough to count as landed or promotion-ready |
| `Rejected` | Lane was evaluated and found deficient; must return to `Active` or be `Deferred` |
| `Integrated` | Lane has landed in trunk |
| `Deferred` | Lane is intentionally parked or excluded from the current promotion path |

---

## 3. Current Audit Snapshot (2026-04-04)

## 3A. Machine Harness Contract

This tracker is the human lane ledger. The machine lane ledger is `ops/lane-registry.json`.

Harness rules:

- the integration tree is the repo root worktree and is integration-only
- feature work normally starts in a dedicated lane worktree
- each non-integration worktree should carry one lease at `.git/smart-sales/current-lane.json`
- reserved lane states are `Active`, `Paused`, `Awaiting evidence`, and `Review`
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

### 3B. Lane Isolation Completion (2026-04-13)

The `harmony/tingwu-container` branch (893 commits) was merged to `master` via fast-forward. The 200-file dirty tree was then sorted:

- admin-path and governance files committed directly on `master` (101 files)
- remaining admin residue, dashboards, and registry updates committed on `master` (6 files)
- lane registry updated with branch and worktree assignments for all active lanes

All feature-lane files were committed into dedicated worktrees:

| Lane | Branch | Worktree | Files |
|------|--------|----------|-------|
| DTQ-01 | `lane/DTQ-01/onboarding-quick-start` | `~/lane-worktrees/DTQ-01-onboarding-quick-start` | 5 |
| DTQ-02 | `lane/DTQ-02/scheduler-intelligence` | `~/lane-worktrees/DTQ-02-scheduler-intelligence` | 13 |
| DTQ-03 | `lane/DTQ-03/connectivity-oem` | `~/lane-worktrees/DTQ-03-connectivity-oem` | 13 |
| DTQ-04 | `lane/DTQ-04/runtime-shell-sim` | `~/lane-worktrees/DTQ-04-runtime-shell-sim` | 19 |
| DTQ-05 | `lane/DTQ-05/shared-runtime-session-contracts` | `~/lane-worktrees/DTQ-05-shared-runtime-session-contracts` | 30 |
| DTQ-08 | `lane/DTQ-08/harmony-native-bounded-delivery` | `~/lane-worktrees/DTQ-08-harmony-native-bounded-delivery` | 50 |

Lane leases are attached, registry validates, and lane guard passed on every commit. Master is clean and pushed to origin. `.gitignore` updated to exclude `.hvigor/`, `hvigor/`, `__pycache__/`, and Harmony toolchain dumps. Admin paths widened to cover `.gitignore`, `.gemini/**`, `.kiro/**`, `.roo/**`, `.windsurf/**`, `app-core/lint-baseline.xml`, `app/src/main/java/**`, and `data/ai-core/src/main/AndroidManifest.xml`.

---

## 4. Lane Board

| Lane | Theme | Owned Write Scope | Explicit Off-Limits | Status | Alignment | Evidence Class | Required Evidence | Current Handoff | Notes |
|------|-------|-------------------|---------------------|--------|-----------|----------------|-------------------|-----------------|-------|
| `DTQ-01` | Onboarding and quick-start | `app-core/.../ui/onboarding/**`, `app-core/.../data/onboarding/**`, onboarding tests, `docs/cerb/onboarding-interaction/**`, `docs/specs/flows/OnboardingFlow.md`, onboarding L3 report, onboarding prototype | scheduler core router, connectivity bridge/service, shared runtime shell host, cross-platform governance docs | `Active` | `Both pending` | `ui-visible` | focused onboarding unit/instrumentation evidence plus docs sync; current audit passed targeted onboarding unit suites and kept existing L3 recovery evidence in scope | `handoffs/schedule_quick_start_compose_handoff.md` | 2026-04-04 audit found no onboarding-specific doc/code contradiction, but the lane stays active until the still-dirty scope has fresh UI/device verification and a clean landing decision. |
| `DTQ-02` | Shared scheduler contracts and reminder surfaces | scheduler drawer/reminder UI, `TaskReminderReceiver`, `AlarmActivity`, scheduler domain resolver, shared active-task retrieval/index truth, scheduler core-flow and SIM scheduler docs, scheduler drawer prototype | onboarding UI flow, connectivity stack, shell chrome, governance tracker docs | `Active` | `Both pending` | `contract-test` | focused scheduler + reminder verification, plus shared-contract evidence for the tightened explicit-target reschedule rule | `handoffs/notification_compose_handoff.md` | 2026-04-11 governance pass widened this lane to own the tightened shared scheduler contract docs, retrieval-board surfaces, and SIM tracker history while the integration tree still awaits real lane isolation. |
| `DTQ-03` | Connectivity and OEM hardening | `RealConnectivityBridge`, `RealConnectivityService`, `ConnectivityViewModel`, `ConnectivityModal`, `OemCompat`, connectivity/OEM tests, `docs/cerb/connectivity-bridge/**`, `docs/cerb/sim-connectivity/spec.md`, `docs/specs/connectivity-spec.md`, `docs/plans/bug-tracker.md`, Harmony/OEM SOP docs and plan | onboarding quick-start internals, scheduler mutation/routing, shared runtime shell chrome, governance tracker docs | `Active` | `Aligned` | `platform-runtime` | connectivity-focused tests and, when runtime diagnosis is involved, `adb logcat` evidence | `handoffs/oem_permission_diagnosis.md` | 2026-04-04 follow-up fixed `RealConnectivityBridge.recordingNotifications()` so raw badge events are filtered at transport-ready time, the focused connectivity/OEM unit bundle passes, and Xiaomi device logs now prove the reminder receiver/full-screen-notification branch; the lane stays active because the recording-ready bridge branch was not reproduced on-device and connectivity runtime still shows listener/readiness gaps. |
| `DTQ-04` | Runtime shell and SIM chrome | `MainActivity`, `RuntimeShell`, shell/audio sync presentation glue, SIM shell/home/history/audio drawer chrome, shell reducer/content/coordinators, shell UI tests, shell/audio/core-flow docs, UI tracker, shell/audio prototypes | onboarding flow internals, connectivity transport implementation, scheduler router core, governance docs | `Active` | `Both pending` | `ui-visible` | focused shell/UI tests and visual/behavior proof for touched chrome | `handoffs/audio_drawer_compose_handoff.md` | The 2026-04-11 governance pass keeps shell presentation ownership here while making the Harmony compat flags and session-title semantics explicit cross-lane dependencies instead of hidden authority. |
| `DTQ-05` | Shared runtime, pipeline, and session contracts | shared pipeline hosts, session-title contract cleanup, session persistence/history docs, parser/session tests, `domain/session/**`, `docs/cerb/input-parser/spec.md`, `docs/cerb/session-history/spec.md`, `docs/cerb/unified-pipeline/**` | onboarding page composition, runtime shell chrome, connectivity service layer, Harmony-native roots, governance docs | `Active` | `Both pending` | `contract-test` | focused pipeline/session tests plus contract re-read for parser, session-title, and persistence drift | `handoffs/shared_runtime_session_contracts_handoff.md` | The 2026-04-11 governance pass widened this lane so the parser/session-title cleanup, session persistence changes, and their tests no longer sit ungoverned between shell and scheduler lanes. |
| `DTQ-06` | Governance, trackers, and repo guardrails | repo law docs, tracker/index ownership, lane harness control plane, Codex/Antigravity rule surfaces, Harmony governance overlays, and Android/Harmony compat registration surfaces such as flavor/build-manifest gates | feature-owned code lanes except for explicit governance-owned registration/control-plane surfaces and lane-local handoff files | `Active` | `Aligned` | `governance-proof` | docs consistency re-read plus local and CI lane-harness verification | — | Reopened on 2026-04-11 to own the lane harness control plane, shared hooks, registry, CI enforcement, and the cross-runtime registration surfaces that govern Android-vs-Harmony capability splits. |
| `DTQ-07` | Cross-feature handoff/workflow assets | `.agent/workflows/compose-handoff-writer-[tool].md` and any future cross-feature handoff-generator/support assets | feature code, feature docs, branch-governance docs unless explicitly moved | `Deferred` | `Deferred` | — | confirm whether the asset is truly reusable project tooling rather than lane-local residue | — | Park unless it proves to be real reusable workflow infrastructure. |
| `DTQ-08` | Harmony-native bounded delivery | `platforms/harmony/tingwu-container/**`, `platforms/harmony/ui-verification/**`, Harmony program trackers, Harmony scheduler backend-first overlay docs, Harmony UI verification overlays | Android beta code lanes, shared scheduler semantics, repo governance law surfaces | `Paused` | `Both pending` | `platform-runtime` | Harmony lane isolation plus registry/handoff truth before any new feature continuation | `handoffs/harmony_native_bounded_delivery_handoff.md` | 2026-04-13: Harmony dirty work is now physically isolated in its own worktree at `~/lane-worktrees/DTQ-08-harmony-native-bounded-delivery` on branch `lane/DTQ-08/harmony-native-bounded-delivery`. Lane lease attached, 50 files committed. Stays paused pending Harmony-native verification. |
| `DTQ-99` | Deferred local artifact and toolchain residue | `wechat_4.1.1.4-2_amd64.deb`, Harmony commandline-tool dumps, handoff PDFs, local cache residue such as `scripts/__pycache__/` | all product code/docs | `Deferred` | `Deferred` | — | explicit confirmation that the path is local residue rather than product deliverable | — | Exclude from the current promotion path; do not treat as product work or as a hidden feature lane. |

---

## 5. Lane Notes and Split Rules

### `DTQ-01` Onboarding and quick-start

- Owns the onboarding feature seam end-to-end, including the quick-start staging/commit path and the one-shot shell handoff gate.
- Must not silently broaden into connectivity transport internals or shared scheduler router internals.

### `DTQ-02` Scheduler intelligence and reminders

- Owns the shared scheduler contract seam, reminder surfaces, retrieval-board truth, and alarm-screen behavior.
- The tightened reschedule law lives here: explicit target, exact new time, global active-exact resolution, and safe-fail on omitted target / ambiguity / no match / non-exact time.
- If a change also needs onboarding staged-flow behavior, coordinate via `DTQ-01` instead of merging both into one lane.

### `DTQ-03` Connectivity and OEM hardening

- Owns transport truth, OEM settings guidance, and connectivity manager/modal behavior.
- Notification/reminder policy wording that is purely OEM-delivery-specific belongs here; shared reminder semantics stay with `DTQ-02`.

### `DTQ-04` Runtime shell and SIM chrome

- Owns shell chrome, drawer presentation, SIM visual polish, and shell-only routing affordances.
- Keeps shell consumers of Harmony compat gating and session-title presentation, but must not silently absorb the underlying compatibility law or session-title semantics.
- Must not become the backdoor owner of scheduler truth, onboarding flow logic, or connectivity transport logic.

### `DTQ-05` Shared runtime, pipeline, and session contracts

- Owns the parser/session-title cleanup bundle, shared pipeline hosts, session persistence/history contract changes, and their direct test/doc surfaces.
- Shell/session UI consumers may stay in `DTQ-04`, but the session-title generator removal and parser/session contract truth belong here.
- If this lane grows beyond the current bounded file set, split it before continuing.

### `DTQ-06` Governance, trackers, and repo guardrails

- Owns repo control-plane work plus the cross-runtime registration surfaces that decide Android-vs-Harmony capability exposure.
- Feature lanes may update their own local feature docs, but cross-cutting tracker/index/ownership docs should flow through this lane during integration.

### `DTQ-08` Harmony-native bounded delivery

- Owns the current Harmony-native app roots plus the Harmony program/verification overlays that describe their bounded capability.
- Stays paused until the Harmony dirty work is physically isolated into its own lane worktree; registry ownership alone is not enough to legalize Harmony feature edits in the integration tree.

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
- `DTQ-06` was reopened on 2026-04-11 as `Active` after the lane harness landed: the control-plane scope now also owns `ops/lane-registry.json`, shared `.githooks/`, `scripts/lane_guard.py`, `scripts/lane`, the CI branch/path validator, and the Claude/Antigravity registration surfaces that must stay aligned with the harness.
- 2026-04-11 governance unblock widened `DTQ-02` to the shared scheduler-contract seams now touched by the explicit-target reschedule tightening, widened `DTQ-05` to the parser/session-title/session-history cleanup bundle, and narrowed `DTQ-06` so Harmony-native program docs and roots can move to a dedicated paused lane instead of staying implicit governance residue.
- `DTQ-08` is now the explicit paused owner of the current Harmony-native dirty roots plus the Harmony program-summary / backend-first / UI-verification overlays. This makes the blocked state explicit without pretending the integration tree may continue to host Harmony feature work safely.
- `DTQ-07` remains `Deferred`; the current dirty asset `.agent/workflows/compose-handoff-writer-[tool].md` is still parked as reusable-tooling residue rather than current promotion-scope product work.
- `DTQ-99` now also parks Harmony commandline-tool residue, handoff PDFs, and `scripts/__pycache__/` instead of leaving those paths ungoverned.
- `DTQ-01`, `DTQ-02`, `DTQ-04`, and `DTQ-05` remain non-accepted implementation lanes because the current dirty inventory still spans code, docs, and tests with `Both pending` alignment and without a final focused verification bundle.
- `DTQ-03` separately remains non-accepted because the runtime/device verification bundle is still missing even though the bridge-contract mismatch is now closed.

---

## 9. Lane Isolation Completion (2026-04-13)

The dirty-tree quarantine is now physically resolved. All feature-lane files have been committed into dedicated worktrees with lane leases and the integration tree (master) is clean.

Actions taken:
- `harmony/tingwu-container` (893 commits) merged to `master` via fast-forward
- 200-file dirty tree sorted into admin commit (101 files) + 6 lane branches (130 files total)
- `.gitignore` updated: `.hvigor/`, `hvigor/`, `__pycache__/`, Harmony toolchain dumps
- `admin_paths` widened: `.gitignore`, `.gemini/**`, `.kiro/**`, `.roo/**`, `.windsurf/**`, `app-core/lint-baseline.xml`, `app/src/main/java/**`, `data/ai-core/src/main/AndroidManifest.xml`, `docs/platforms/harmony/hui-02-tingwu-docking-contract.md`
- DTQ-06 `owned_paths` updated to match widened `admin_paths`
- DTQ-08 `owned_paths` updated to include `hui-02-tingwu-docking-contract.md`
- Lane registry validates (`lane_guard.py validate-registry` passes)
- Master pushed to origin

Remaining work per lane:
- DTQ-01 through DTQ-05: `Both pending` alignment; need focused verification in their worktrees
- DTQ-03: `Aligned` but runtime/device evidence still missing
- DTQ-06: `Aligned`; governance proof via lane guard passing
- DTQ-08: `Paused`; now physically isolated, awaiting Harmony-native verification
- DTQ-07, DTQ-99: `Deferred`; no action needed
