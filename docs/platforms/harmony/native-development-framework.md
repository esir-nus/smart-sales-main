# Harmony Native Development Framework

> **Role**: North-star operating framework for native Harmony development in this repo
> **Status**: Active platform framework
> **Date**: 2026-04-08
> **Primary Law**: `docs/specs/platform-governance.md`
> **Related Docs**:
> - `docs/reference/platform-targets.md`
> - `docs/platforms/harmony/README.md`
> - `docs/platforms/harmony/tingwu-container.md`
> - `docs/sops/harmony-operator-runbook.md`
> - `docs/reference/harmonyos-platform-guide.md`
> - `docs/cerb/interface-map.md`
> - `docs/plans/tracker.md`

---

## 1. Framework Stance

Native Harmony work in this repo is **translation-first product delivery** and **doc-first native rewriting**.

Interpretation:

- Harmony does not become a second product truth owner
- Harmony does not mechanically port Kotlin Compose code into ArkTS
- Harmony rewrites implementation natively while translating the existing product intent honestly

Working rule:

- rewrite the code
- translate the behavior
- preserve the product truth

This repo must treat Harmony as a platform-owned implementation lane, not as a speculative cross-platform rewrite program.

---

## 2. Source-of-Truth Ladder

When building native Harmony features, use this truth order:

1. shared product docs and contracts
2. shared core-flow docs when they exist
3. current Android behavior as the best live implementation reference
4. Harmony-native overlay docs and constraints
5. local Harmony implementation

Interpretation:

- shared docs define what the product means
- the Android app is the current reality check when docs are incomplete
- Harmony implementation must follow that truth unless a Harmony-only limit is explicitly documented

If the Android app is the only place where a behavior is clear, extract the behavior into docs before treating the Harmony implementation as settled.

---

## 3. Rewrite vs Translate Rule

### 3.1 Translate these

These should stay shared or be translated from shared truth:

- product journeys and user goals
- state meaning and workflow intent
- backend contracts and payload meaning
- copy intent and terminology
- capability boundaries
- scheduler semantics and other domain rules unless explicitly unsupported on Harmony

### 3.2 Rewrite these natively

These must be rewritten for Harmony instead of ported structurally from Android:

- UI composition and layout
- navigation shell
- lifecycle wiring
- permission flow wiring
- local storage and file access
- device integration and native APIs
- build, deploy, logging, and debugging workflow

### 3.3 Do not share the UI runtime by default

For the current repo phase, do **not** treat a shared JS UI repo as the default answer.

Reason:

- that would create a re-platforming program, not a translation lane
- it would add a third truth surface between Android and Harmony
- current product truth already lives in shared docs plus the Android implementation

If a shared layer emerges later, prefer sharing:

- docs
- schemas
- config
- strings
- prompts
- capability matrices

Do not default to sharing:

- platform UI trees
- shell architecture
- widget implementations

---

## 4. Vibe-Coding Rulebook

When agents or humans work in Harmony-native mode, the prompt model must be:

- start from feature intent
- restate supported capability
- rewrite natively in ArkTS/ArkUI
- verify against the docs and the current Android behavior

Do **not** drive the work with prompts like:

- "port these Compose files"
- "translate this ViewModel 1:1"
- "mirror the Android package structure"

Prefer prompts like:

- "build the Harmony-native version of this feature from the shared spec"
- "implement the supported Harmony slice of this flow"
- "rewrite this UI natively while preserving the current behavior contract"

Agent rule:

- when given Android code, read it as behavior evidence
- do not treat it as the target architecture for Harmony

---

## 5. Shared Layer Posture

The preferred shared layer between Android and Harmony is **semantic**, not **visual-runtime**.

Current allowed shared truth surfaces:

- shared docs spine
- domain semantics
- backend/API contracts
- terminology and copy intent
- test scenarios and acceptance criteria
- platform capability declarations

Current disallowed assumption:

- "If we can share the UI runtime, we should."

That assumption is not valid for the repo's current phase.

The current default is:

- one shared product truth
- one Android implementation reference
- one native Harmony implementation lane

---

## 6. Native Harmony Delivery Workflow

Human operator companion:

- `docs/sops/harmony-operator-runbook.md` owns the human-facing workflow for classifying Harmony slices, briefing agents, reviewing evidence, and advancing tracker state

For each Harmony-native slice:

1. classify whether the work is shared truth, Harmony-native delivery, or governance
2. read the owning shared docs first
3. identify the supported and unsupported Harmony capability set
4. write or update the Harmony overlay before or alongside code
5. implement only inside the Harmony-owned root
6. verify with the Harmony CLI toolchain

Backend-first allowance:

- when the main uncertainty is scheduler dataflow, state ownership, or mutation correctness, start with a backend/dataflow verification slice before broad ArkUI translation
- in that case, seed telemetry at the critical ownership joints so Harmony dataflow does not stay a black box

Current CLI-first delivery posture:

- build with `hvigorw`
- control devices and app lifecycle with `hdc`
- inspect logs with `hdc shell hilog`
- inspect runtime/system state with `hidumper`
- use subsystem shell tools such as `hdc shell ime` when Huawei documents them

This workflow is especially important on Ubuntu because DevEco Studio is not the primary local operating environment.

---

## 7. Anti-Contamination Rules

Native Harmony work must not contaminate the Android lineage.

Rules:

- do not copy Compose architecture forward just because it already exists
- do not place Harmony-native files under `app/**`, `app-core/**`, `core/**`, `data/**`, or `domain/**`
- do not quietly redefine shared product behavior inside Harmony implementation
- do not hide unsupported capability behind fake parity UI

If Harmony cannot support a feature yet:

- declare it explicitly
- remove or block the entrypoint
- document the limitation in the Harmony overlay

Honesty is more important than parity theater.

---

## 8. Current Repo Default

Current repo posture:

- the Android app remains the best live behavior reference
- the Android-on-Huawei/Harmony compatibility path is legacy and not the future native owner
- the native Harmony lane is the forward platform
- the first live Harmony root remains a bounded transient container
- a second internal Harmony UI verification root may coexist for page-native ArkUI checks while staying explicit that it is not the public product app
- neither Harmony root is proof of full-product parity

Until a broader Harmony-native app exists, every new Harmony slice must answer these questions first:

1. what behavior is being translated from shared truth
2. what must be rewritten natively
3. what is unsupported and therefore must fail closed

If those answers are not explicit, the slice is not ready to implement.
