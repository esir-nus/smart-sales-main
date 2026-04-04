---
name: harmony-translation-guardrail
description: Guardrail mode for Smart Sales Harmony work. Use when the user asks for Harmony mode, HarmonyOS adaptation, translation-first parallel AOSP and Harmony delivery, curated Harmony container variants such as a Tingwu-only build, Harmony-native scaffolding, branch hygiene, or contamination checks between Android and Harmony lanes.
---

# Harmony Translation Guardrail

Use this skill when a task touches Harmony-specific delivery or when the user explicitly wants "Harmony mode".

This skill encodes a constrained Harmony working mode:

- treat the current Android/AOSP line plus shared docs as the practical primary reference for current product behavior unless the user explicitly approves a real Harmony-native divergence
- treat most Harmony work as adaptation, translation, scaffolding, packaging, permissions, lifecycle, and platform-API delivery work
- allow an explicit curated Harmony container when Harmony can only support a bounded subset of the product, such as a Tingwu-focused variant with scheduler-related features disabled
- prevent Harmony-native artifacts and assumptions from contaminating the current Android tree

This is an execution guardrail, not a replacement for repo law. If the current governance docs disagree with the requested working mode, call out the mismatch and propose doc sync instead of silently overriding the repo contract.

## Load Context

Read the minimum set needed to classify the task before editing:

1. `AGENTS.md`
2. `docs/plans/tracker.md`
3. `docs/specs/platform-governance.md`
4. `docs/reference/platform-targets.md`
5. `docs/platforms/README.md`
6. `docs/platforms/harmony/README.md`
7. the relevant platform overlay in `docs/platforms/harmony/**`
8. `docs/cerb/interface-map.md` if ownership crosses modules

Read `docs/reference/harmonyos-platform-guide.md` only when the task is about the Android app running on Huawei/Honor/Harmony devices. That file is the Android compatibility path, not the native Harmony implementation owner.

## Profile Gate

Before planning code, choose one working profile:

1. `translation-first`
   - Harmony translates or adapts an already-decided behavior into Harmony delivery mechanics
2. `curated-container`
   - Harmony ships a bounded capability subset because some features are not supportable on Harmony right now
3. `governance`
   - branch policy, CODEOWNERS, CI, review ownership, doc routing, or contamination checks
4. `true-native-divergence`
   - Harmony truly needs new platform-owned implementation, scaffolding, native APIs, packaging, or approved user-visible divergence

Rules:

- default Harmony tasks to `translation-first`, not `true-native-divergence`
- default current-behavior questions to shared docs plus the live AOSP implementation
- do not treat copy translation, platform packaging, permission wiring, or native API substitution as permission to invent new product behavior
- if the task mixes AOSP-mainline implementation and Harmony-native implementation in one write scope, split the lane or explicitly record the governance reason

If the task also changes shared product semantics, mark that explicitly as shared-truth work instead of hiding it under a Harmony label.

## Translation-First Rule

When the profile is `translation-first`:

- use shared docs and the current Android behavior as the input truth for what the product should do
- translate delivery, not product semantics
- keep identifiers, state names, interface meaning, and workflow intent aligned unless the user explicitly wants a Harmony-specific divergence
- if the Android behavior is unclear, fix or clarify the shared source first instead of inventing a Harmony-only answer

Typical translation-owned work:

- copy and localization for Harmony surfaces
- Harmony packaging and scaffold files
- lifecycle and app-entry adaptation
- permission, reminder, notification, and background execution wiring
- Harmony-native protocol or API mapping
- platform-owned UI constraints that do not redefine product intent

## Curated-Container Rule

When the profile is `curated-container`:

- treat Harmony as a clean capability container, not as a parity target
- require the task to name the container purpose and the disabled capability set up front
- keep the supported capability set explicit and narrow
- document disabled capabilities as Harmony-owned limits, not as shared product truth
- remove or hard-block unsupported features instead of leaving them half-wired

Use this profile for cases like:

- a Harmony-compatible Tingwu container
- a Harmony build where scheduler-related features are intentionally disabled
- a platform-safe subset release used to avoid pretending unsupported flows still work

Minimum declarations for a curated container:

1. the container purpose, such as `tingwu-container`
2. the supported capability set
3. the disabled capability set
4. the user-visible limitations that must be documented in Harmony overlays

If scheduler is disabled on Harmony, do not quietly rewrite shared scheduler semantics. Instead:

- mark scheduler-related behavior as unsupported or absent in the Harmony variant
- keep shared scheduler truth owned by shared docs
- keep Harmony docs honest about the reduced capability

## Unsupported Capability Protocol

When Harmony cannot support a feature:

- do not treat the limitation as ordinary translation fallout
- do not hide it behind vague wording like "temporarily different"
- require an explicit Harmony variant or container framing
- list the unsupported feature set in `docs/platforms/harmony/**`
- make the Harmony surface fail closed by removing, hiding, or blocking the unsupported entrypoints instead of leaving dead affordances

Examples of limitations that must be explicit:

- scheduler creation, reschedule, reminders, or follow-up lanes are disabled
- onboarding promises a capability that Harmony cannot actually complete
- background or notification behavior is materially weaker than the shared product expectation

These are platform-contract decisions, not routine copy translation.

## Contamination Rules

Never place Harmony-native artifacts inside the Android tree:

- `app/**`
- `app-core/**`
- `core/**`
- `data/**`
- `domain/**`

Blocked examples inside the Android lineage:

- `module.json5`
- `oh-package.json5`
- `hvigorfile.ts`
- `.ets` files
- `ohos.*` runtime imports

Use the existing repo guardrails as evidence:

```bash
find app app-core core data domain -type f \( -name 'module.json5' -o -name 'oh-package.json5' -o -name 'hvigorfile.ts' -o -name '*.ets' \)
rg -n '(^|[^A-Za-z0-9_])ohos\.' app app-core core data domain -g '!**/build/**' -g '!**/*.md'
```

If a Harmony task needs those artifacts, move the work into a dedicated Harmony-owned root or stop and surface the missing root/scaffold as the real blocker.

## Branch and Lane Hygiene

Treat Harmony work as parallel but bounded.

- one write scope should serve one lane
- do not hide Harmony-native changes inside an Android feature branch
- do not hide AOSP-mainline changes inside a Harmony translation or curated-container pass
- if the task is mixed, route the control-plane parts through governance docs and keep platform implementation work separate

Preferred branch intent for draft governance:

- `android/<topic>` for Android-mainline work
- `shared/<topic>` for shared docs or contracts
- `harmony/<topic>` for Harmony translation or native platform work
- `governance/<topic>` for branch, review, CODEOWNERS, or CI guardrails

If the repo later defines a stricter branch naming policy, follow that documented policy instead of this draft.

## Doc Routing Rule

Use the narrowest doc layer that matches the change:

- shared product behavior changes: shared docs spine
- Harmony delivery differences: `docs/platforms/harmony/**`
- Harmony curated-container limitations: `docs/platforms/harmony/**`
- Android delivery differences on Huawei/Honor/Harmony devices: `docs/platforms/android/**` plus `docs/reference/harmonyos-platform-guide.md` when needed
- ownership or cross-module changes: `docs/cerb/interface-map.md`
- campaign state or governance status: `docs/plans/tracker.md`

Do not fork shared product docs just because Harmony uses different native APIs.

Create a Harmony-specific companion spec only when:

- Harmony owns most of the implementation contract, or
- the user-visible Harmony behavior genuinely diverges enough that an overlay would mislead

## Translation Protocol

When copy, flows, or interaction text are being translated for Harmony:

- keep one source phrase mapped to one translated phrase unless a platform constraint forces a rewrite
- preserve product meaning before optimizing tone
- record any forced rewrite that changes user-visible meaning in the Harmony overlay doc
- avoid renaming shared state or contract terms merely for platform flavor
- if legal, store the source phrase, translated phrase, and reason together in the working notes or doc update

Escalate before coding if translation pressure is actually hiding a product decision:

- different permission promise
- different reminder reliability guarantee
- different scheduler semantics
- different onboarding completion meaning
- removal of a whole feature family such as scheduler

Those are product or platform-contract decisions, not routine translation.

## Delivery Workflow

### 1. Classify first

- state whether the task is `translation-first`, `curated-container`, `governance`, or `true-native-divergence`
- if shared product behavior is changing, say that explicitly as shared-truth work
- identify the owning docs and the platform-owned docs before touching code

For `curated-container`, name:

- the container purpose
- the supported capability set
- the disabled capability set

### 2. Check the write boundary

- confirm whether the work belongs in shared docs, Android modules, Harmony overlays, or a future Harmony root
- if the required Harmony root does not exist yet, say so explicitly and keep the session scoped to docs, governance, scaffolding prep, or safe overlays

### 3. Implement the narrowest valid slice

- prefer overlays, adapters, and platform-owned seams over shared-doc duplication
- prefer clean capability removal over half-working unsupported features in curated containers
- keep Harmony-native protocol work local to Harmony-owned paths
- avoid widening translation work into feature invention

### 4. Sync docs in the same session

- update Harmony overlays for delivery differences
- update Harmony overlays for explicit unsupported capability lists when using a curated container
- update shared docs only if shared behavior changed
- update tracker/interface docs when ownership, status, or branch/governance rules changed

### 5. Run contamination checks before finishing

- search for Harmony-native artifacts in the Android tree
- search for `ohos.*` imports in the Android tree
- re-read touched docs for accidental product-truth drift

## Exit Checklist

Before saying the Harmony task is done, verify:

- the working profile is explicit
- Harmony work stayed translation-first or curated-container unless a true native divergence was approved
- any disabled Harmony capability is explicitly documented instead of silently dropped
- no Harmony-native artifacts leaked into the Android tree
- shared product truth did not drift accidentally
- the right overlay or companion doc was updated
- branch/lane intent is clear enough that later engineers can tell where the work belongs

## Anti-Patterns

- treating Huawei/Honor Android compatibility work as proof that native Harmony is already implemented
- hiding native Harmony scaffolding inside `app/**` or `app-core/**`
- editing shared product semantics just because Harmony delivery feels different
- using a Harmony translation task to smuggle in new product behavior
- calling a curated Harmony subset "full support" when scheduler or another feature family is intentionally absent
- leaving dead scheduler affordances in a Tingwu-only Harmony container
- mixing Android-mainline and Harmony-native work into one unbounded dirty lane
- calling a platform divergence "just translation" when it actually changes the contract
