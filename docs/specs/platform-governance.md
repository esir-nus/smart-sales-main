# Platform Governance

> **Role**: Canonical cross-platform version-control and ownership law
> **Status**: Active governance law
> **Date**: 2026-04-16
> **Purpose**: Maintain Android/AOSP as beta-maintenance while HarmonyOS-native is the primary forward platform. HarmonyOS NEXT drops the Android compatibility layer; complete native ArkTS/ArkUI migration is now urgent.
> **Related Docs**:
> - `docs/reference/platform-targets.md`
> - `docs/platforms/harmony/native-development-framework.md`
> - `docs/platforms/harmony/app-architecture.md`
> - `docs/reference/harmonyos-platform-guide.md`
> - `docs/cerb/interface-map.md`
> - `docs/plans/tracker.md`
> - `docs/projects/harmony-native/tracker.md`
> - `docs/specs/base-runtime-unification.md`

---

## 1. Canonical Trunk Rule

The repo keeps exactly one canonical protected trunk.

Rules:

- The exact branch name (`main` or `master`) is an operational choice, not a product-law distinction.
- All shared docs, shared contracts, Android beta work, and Harmony-native work must converge back to the same canonical trunk via the branch model below.
- Long-lived platform branches (`platform/harmony`) are integration trunks for platform-specific work, not alternate product-truth branches.

### Branch model

```
master (protected, promotion-only — requires PR)
  └── develop (Android maintenance + shared contracts, daily trunk)
        └── platform/harmony (HarmonyOS integration trunk, daily Harmony work)
              ├── harmony/feat-x (feature branches, PR back to platform/harmony)
              └── harmony/feat-y
```

Rules:

- `develop` is the Android maintenance trunk and the source of shared contracts
- `platform/harmony` is the HarmonyOS-native integration trunk; all daily Harmony work lands here
- shared contracts flow `develop → platform/harmony` via deliberate merge, at least weekly; never the reverse
- feature branches fork from `develop` (for Android) or `platform/harmony` (for Harmony) and PR back to origin branch
- `master` receives promotions from `develop` via PR only; no direct commits

### Historical note

The previous Dirty-Tree Quarantine (DTQ) lane harness was decommissioned on 2026-04-15. Archived docs are in `docs/archive/dtq-era/` for reference only.

### Feature-branch freshness rule

A feature branch that has not merged back (or been rebased onto its origin trunk) within **5 calendar days of its most recent commit** is treated as stale. Stale feature branches are a governance failure, not a work-in-progress signal.

Rules:

- If a branch genuinely needs to live longer than 5 days (architectural rewrite, phased migration), declare it an integration trunk and document its scope and merge cadence in `docs/plans/tracker.md`.
- A stale branch must either merge, rebase on its trunk, or be converted to an integration trunk before any further commits land on it.
- Multi-platform isolation is not an excuse for drift: Harmony work and Android work proceed independently, but neither gets to hoard fixes on a side branch.

Rationale: long-lived feature branches accumulate unmerged user-visible fixes. When a build is installed from a different branch, those fixes appear to have "disappeared," which is an integration failure diagnosed in 2026-04-20 (see `docs/reference/agent-lessons-details.md`).

### Multi-platform isolation

Android work and Harmony work are isolated by construction. Uncommitted work on one platform must not block a build or fix on the other.

Operational pattern: use git worktrees (`docs/sops/worktree-usage.md`) to build, install, or verify a different branch without disturbing the current working tree. This is the sanctioned way to respect platform isolation without pausing in-flight work.

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

Harmony-native is the **primary forward platform (urgent migration)**.

Context: HarmonyOS NEXT drops the Android compatibility layer entirely. The Android-on-Harmony compatibility target is deprecated. Complete native ArkTS/ArkUI is the only viable path forward.

Rules:

- native Harmony work must build from shared product docs and approved shared contracts
- native Harmony work must not be implemented as a slow contamination of the current Android tree
- the migration target is a complete native ArkTS/ArkUI app with full product parity, not a bounded container
- the existing Tingwu container (`platforms/harmony/tingwu-container/`) provides proven patterns and is the foundation for the complete native app (`platforms/harmony/smartsales-app/`)
- each delivered feature must be honest about its current capability set; features not yet implemented must be hidden or blocked, not faked
- release branch `release/harmony-alpha` may be created once the native app shell and at least one feature lane (audio pipeline) are device-verified

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
- the Harmony-native lineage lives under `platforms/harmony/`
- the complete native app is rooted at `platforms/harmony/smartsales-app/`
- the existing Tingwu container at `platforms/harmony/tingwu-container/` is the pattern foundation; its code will be absorbed into the complete native app
- `platforms/harmony/ui-verification/` may exist for internal page-native ArkUI checks during development

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
- specialist trackers own structure, UI, bugs, and Harmony-native delivery
- execution briefs are temporary slice docs and must not replace a standing tracker
- the Harmony program uses `docs/projects/harmony-native/tracker.md` for program-summary state and `docs/projects/harmony-ui-translation/tracker.md` for page-by-page ArkUI rewrite evidence

### 6.5 Historical note

The previous lane harness governance system (DTQ, lane-registry.json, lane_guard.py, .githooks) was decommissioned on 2026-04-15. Archived documentation is in `docs/archive/dtq-era/`. The current governance model uses feature branches and PRs per section 1.

---

## 7. Release Branch Rule

Current release-branch posture:

- `release/android-beta` is the stabilization line for the current Android lineage
- `release/harmony-alpha` may be created once the native app shell and at least one feature lane (audio pipeline) are device-verified and Harmony CI is running

Daily delivery rule:

- short-lived feature branches fork from `develop` (for Android) or `platform/harmony` (for Harmony)
- release branches are for stabilization and hotfix work only
- `platform/harmony` is the Harmony integration trunk, not a release branch

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

- `CODEOWNERS` must route shared docs/contracts, Android lineage, and Harmony overlays through explicit review ownership
- CI must fail if native Harmony artifacts appear inside the Android tree
- governance docs and platform-target definitions must remain present in the repo
- the Harmony app root and platform overlay docs must remain present once introduced

Manual admin guardrails that still require GitHub settings:

- branch protection on the canonical trunk
- required status checks
- restricted direct pushes where desired

---

## 9. Acceptance Rule

This governance split is only working if:

- shared product truth remains single-source (docs > code > guessing)
- Android beta-maintenance work stays narrow and explicit
- Harmony-native work progresses on `platform/harmony` without contaminating the Android tree
- shared contract syncs from `develop → platform/harmony` happen at least weekly
- platform-specific constraints are documented in overlays instead of leaking into shared specs
- future engineers can tell whether a change is `shared-contract`, `android-beta`, `harmony-native`, or `cross-platform-governance` without guesswork
- the cross-platform development contract (`docs/specs/cross-platform-sync-contract.md`) is followed for all shared truth changes
