# Platform Governance

> **Role**: Canonical cross-platform version-control and ownership law
> **Status**: Active governance law
> **Date**: 2026-04-11
> **Purpose**: Freeze the current Android/AOSP product line as beta-maintenance while opening a clean native HarmonyOS forward lane without forking shared product truth.
> **Related Docs**:
> - `docs/reference/platform-targets.md`
> - `docs/platforms/harmony/native-development-framework.md`
> - `docs/reference/harmonyos-platform-guide.md`
> - `docs/cerb/interface-map.md`
> - `docs/plans/tracker.md`
> - `docs/plans/harmony-tracker.md`
> - `docs/plans/dirty-tree-quarantine.md`
> - `docs/sops/tracker-governance.md`
> - `docs/sops/lane-worktree-governance.md`
> - `ops/lane-registry.json`
> - `docs/specs/base-runtime-unification.md`

---

## 1. Canonical Trunk Rule

The repo keeps exactly one canonical protected trunk.

Rules:

- The exact branch name (`main` or `master`) is an operational choice, not a product-law distinction.
- All shared docs, shared contracts, Android beta work, and future Harmony-native work must converge back to the same canonical trunk.
- Long-lived platform branches are release/stabilization branches only. They are not allowed to become alternate product-truth branches.

### Dirty-worktree quarantine gate

Before any default-branch swap, branch promotion, or baseline tagging:

- inventory all tracked and untracked work in the active line
- assign every dirty path to a bounded lane in `docs/plans/dirty-tree-quarantine.md`, including any explicit `Deferred` row used to park or exclude residue from the current promotion path
- record the same active lane ownership in `ops/lane-registry.json`
- decide explicitly what lands, what is parked, and what is deferred
- create the Android beta baseline tag only from that explicit stabilization commit

No branch-default switch is valid if the dirty-worktree quarantine step was skipped.

When parallel agents are active:

- one feature lane must normally run inside one dedicated `git worktree`
- the repo root worktree is integration-only and must not carry feature edits
- one lane has one active local lease at a time
- paused or transferred lanes must carry a current handoff file in `handoffs/`
- no lane may be treated as promotion-ready while its doc-code alignment state is unresolved
- local hooks and CI must validate the same lane ownership rules through `scripts/lane_guard.py`

---

## 2. Supported Product Targets

The repo now recognizes three different target classes:

1. **Android / AOSP native**
2. **Android app running on Huawei/Honor/Harmony devices**
3. **Harmony-native**

Interpretation:

- Android / AOSP native and Android-on-Huawei/Harmony both belong to the current Android product lineage.
- Android-on-Huawei/Harmony is a compatibility target for the Android app, not a native Harmony product.
- Harmony-native is a separate implementation target with separate platform ownership.

`docs/reference/platform-targets.md` owns the plain-language definition of these targets.

---

## 3. Delivery Status Rule

### 3.1 Android / AOSP

The current Android lineage is now **beta-maintenance**.

Allowed work:

- bug fixes
- reliability hardening
- security or privacy fixes
- tooling / CI / docs sync required to keep the line releasable
- narrow compatibility work for the shipped Android app

Disallowed by default:

- broad new platform architecture that belongs to native Harmony
- uncontrolled feature expansion that should live in the Harmony-native lane

### 3.2 Harmony-native

Harmony-native is the **forward-development** platform.

Rules:

- native Harmony work must build from shared product docs and approved shared contracts
- native Harmony work must not be implemented as a slow contamination of the current Android tree
- the first live Harmony root may be a curated transient container instead of a full-parity app
- the current approved public transient container is the Tingwu-only Harmony app rooted at `platforms/harmony/tingwu-container/`
- the repo may also host an internal Harmony UI verification app rooted at `platforms/harmony/ui-verification/` when page-native ArkUI rewriting and device UI checks need to run in parallel with backend work
- the transient container must stay honest about its reduced capability set and must not pretend scheduler, reminder, chat, or badge-hardware support exists when it does not
- the internal UI verification app must stay explicit that it is internal verification only and must not present mock-backed pages as public parity
- release branch `release/harmony-alpha` stays deferred until the Harmony program moves beyond the transient Tingwu container and the first Harmony CI lane is alive

---

## 4. Shared vs Platform-Owned Rule

### 4.1 Shared truth

These stay shared unless the product behavior truly diverges:

- product journeys and user goals
- core flows
- scheduler semantics
- domain models and business rules
- shared UI intent and interaction goals
- platform-neutral contracts and interface ownership rules

### 4.2 Platform-owned delivery

These are platform-owned by default:

- lifecycle and app-entry behavior
- permissions
- reminders / notifications / background execution
- OEM settings entrypoints
- BLE, hardware, and device integration
- packaging, signing, and platform runtime APIs
- native OS adapters and platform-specific UI constraints

### 4.3 Shared code boundary

Code may be shared only when it is platform-neutral and runtime-neutral.

Do not treat a module as safely shared if it contains or assumes:

- Android-only lifecycle rules
- Harmony-only lifecycle rules
- notification / alarm delivery specifics
- OEM settings flows
- direct OS API binding

Anything with those assumptions is platform-owned even if the logic looks reusable.

---

## 5. Repo Layout Rule

Current posture:

- the Android lineage remains in the existing Gradle/Kotlin app modules
- the Harmony-native lineage must land in a dedicated Harmony-owned root when that scaffold work starts
- the first live Harmony root is the transient Tingwu container at `platforms/harmony/tingwu-container/`
- a second Harmony-owned internal verification root may exist at `platforms/harmony/ui-verification/` for page-native ArkUI checks and must remain internal-only
- those roots are bounded Harmony-owned app containers, not proof that full Harmony parity is implemented

Hard guardrail:

- native Harmony files must not be added under `app/**`, `app-core/**`, `core/**`, `data/**`, or `domain/**`

Examples of forbidden native Harmony artifacts inside the Android tree:

- `module.json5`
- `oh-package.json5`
- `hvigorfile.ts`
- `.ets` source files
- native `ohos.*` runtime imports in the current Android lineage

---

## 6. Docs Model Rule

The repo uses one shared docs spine plus platform overlays.

### 6.1 Shared docs spine

Shared product truth stays in:

- `docs/core-flow/**`
- `docs/cerb/**`
- `docs/cerb-ui/**`
- `docs/specs/**`

### 6.2 Platform overlays

Platform deltas live in:

- `docs/platforms/android/**`
- `docs/platforms/harmony/**`

Rule:

- shared docs answer **what the product does**
- platform overlays answer **how this platform delivers it**, **what differs**, and **what is unsupported**

### 6.3 When to fork a feature doc

Use shared spec + overlay when only delivery mechanics differ.

Create a platform-specific companion spec only when:

- implementation ownership diverges heavily, or
- user-visible behavior diverges enough that overlays become misleading

### 6.4 Tracker governance

The repo uses one master ledger plus specialist standing trackers.

Rules:

- `docs/plans/tracker.md` stays the campaign index and branch/governance summary only
- specialist trackers own structure, UI, bugs, validation, dirty-lane hygiene, or Harmony-native bounded delivery according to `docs/sops/tracker-governance.md`
- execution briefs are temporary slice docs and must not replace a standing tracker
- the current Harmony program uses Stage 2 tracking: `docs/plans/harmony-tracker.md` owns Harmony program-summary state and `docs/plans/harmony-ui-translation-tracker.md` owns page-by-page ArkUI rewrite evidence; a dedicated Harmony dataflow tracker remains deferred until backend rewriting grows large enough to justify it

### 6.5 Lane harness governance

The repo now uses a lane execution harness for dirty-tree prevention.

Rules:

- `docs/plans/dirty-tree-quarantine.md` stays the human lane board and promotion ledger
- `ops/lane-registry.json` is the machine-readable lane registry used by hooks and CI
- `docs/sops/lane-worktree-governance.md` owns the operator workflow for start/resume/pause/integrate
- `.githooks/pre-commit`, `.githooks/pre-push`, and `.github/workflows/platform-governance-check.yml` must enforce the same validator logic rather than drifting into separate rule sets

---

## 7. Release Branch Rule

Current release-branch posture:

- `release/android-beta` is the stabilization line for the current Android lineage
- `release/harmony-alpha` is reserved and must not be created while Harmony delivery is still only the transient Tingwu container and before the first Harmony CI path runs

Daily delivery rule:

- short-lived feature branches fork from the canonical trunk
- release branches are for stabilization and hotfix work only
- an active transient Harmony branch may exist for bounded delivery, but it must not become an alternate product-truth branch
- lane branches should use `lane/<lane-id>/<slug>` when the harness creates a new lane worktree

### 7.1 Branch restore record

Any active transient Harmony branch must be recorded in `docs/plans/tracker.md` plus the owning Harmony tracker entry with these fields:

- `Branch`
- `Purpose`
- `Baseline Commit or Tag`
- `Restore Procedure Reference`
- `Capability Class`
- `Current Restore Confidence`

---

## 8. Review and CI Gates

Repo-stored guardrails:

- `CODEOWNERS` must route shared docs/contracts, Android lineage, Harmony overlays, lane harness files, and workflow changes through explicit review ownership
- CI must fail if native Harmony artifacts appear inside the Android tree
- governance docs, lane harness docs, and platform-target definitions must remain present in the repo
- the lane harness registry, validator, and shared hooks must remain present in the repo
- the transient Harmony Tingwu container overlay and root placeholder must remain present once introduced

Manual admin guardrails that still require GitHub settings:

- branch protection on the canonical trunk
- required status checks
- restricted direct pushes where desired

---

## 9. Acceptance Rule

This governance split is only working if:

- shared product truth remains single-source
- Android beta work stays narrow and explicit
- feature work normally starts in lane worktrees rather than the integration tree
- lane ownership is visible in both `docs/plans/dirty-tree-quarantine.md` and `ops/lane-registry.json`
- paused or transferred lanes remain resumable through current handoffs
- native Harmony work does not contaminate the current Android tree
- platform-specific constraints are documented in overlays instead of leaking into shared specs by accident
- future engineers can tell whether a change is `shared-contract`, `android-beta`, `harmony-native`, or `cross-platform-governance` without guesswork
