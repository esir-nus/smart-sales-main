# Smart Sales -- Claude Code Instructions

## Project Identity

Smart Sales is a voice-first sales operating app for sales operators. It captures real-world sales activity with low friction, turns it into review/follow-up/scheduling surfaces, and keeps AI assistive rather than autonomous.

Platform posture:
- **Android**: beta-maintenance platform, cerb-compliant (code and docs are written together and kept aligned). Foundation and source of shared truth.
- **HarmonyOS**: primary forward platform, complete native ArkTS/ArkUI rewrite. Translates shared product truth into native implementation.
- **iOS**: anticipated, same translation-first model as HarmonyOS (not yet started)

Android is the foundation implementation and source of shared product truth. HarmonyOS and iOS consume shared docs, domain semantics, and Android behavior as reference, then rewrite natively into their platform frameworks. See `docs/specs/cross-platform-sync-contract.md`.

Read `SmartSales_PRD.md` for full product identity.

## Branch Model

```
master (protected, promotion-only — requires PR)
  └── develop (Android maintenance + shared contracts, daily trunk)
        └── platform/harmony (HarmonyOS integration trunk, daily Harmony work)
              ├── harmony/feat-x (feature branches, PR back to platform/harmony)
              └── harmony/feat-y
```

- **develop**: Android maintenance and shared-contract work. Cerb-compliance applies.
- **platform/harmony**: HarmonyOS-native integration trunk. Receives shared contracts from develop via merge. Never merges back into develop. All Harmony daily work lands here.
- **master**: Protected. Receives promotions from develop via PR only. No direct commits.
- Feature work: create branch from `develop` (for Android) or `platform/harmony` (for Harmony), PR back to origin branch.
- Shared contracts flow: `develop → platform/harmony`, never the reverse. Sync at least weekly.
- Harmony-native is the primary forward platform. Android is beta-maintenance.

## Declaration-First Shipping

Every test/build/ship command must declare its **Lane** (`android`, `harmony`, or `docs`) and **Ship Scope** (explicit files/modules). Declarations are per-command, never sticky.

`android` and `harmony` are platform delivery lanes. `docs` is a shared-infrastructure lane — documentation and repo-root markdown always land on `develop` regardless of which platform they describe.

Before starting a declared task, run the pre-flight scope conflict check against `docs/plans/active-lanes.md` and the dirty tree. File-level overlap with in-flight work refuses the task; same-directory overlap warns and asks.

Branch name is advisory — the declared lane decides what ships, not the branch. Unrelated dirty files are reported, not blocked. See `docs/specs/declaration-first-shipping.md` for the full contract and philosophy (friction upfront, not downstream).

## Source of Truth

Docs > Code > Guessing.

Resolution order when documents conflict:
0. `docs/specs/harness-manifesto.md` -- harness operating protocol (governs how to read everything else)
1. `SmartSales_PRD.md` -- product identity, surfaces, journeys, UX laws
2. `docs/specs/base-runtime-unification.md` -- base-runtime vs Mono boundary
3. `docs/core-flow/**` -- behavioral north star (may be ahead of spec and code)
4. `docs/cerb/**`, `docs/cerb-ui/**` -- implementation contracts (cerb-era: reference for intent, not backend authority)
5. `docs/specs/Architecture.md` -- deeper system laws (typed mutation, RAM/SSD, plugins)
6. Code and validation evidence

Core Flow can be ahead of specs and code. When they conflict, treat lower layers as drift candidates before assuming the Core Flow is wrong.

## Doc-Reading Protocol

Before writing code:
1. Read the owning `docs/core-flow/` doc if one exists
2. Read the owning spec or SOP (`docs/cerb/**`, `docs/specs/**`, `docs/sops/**`)
3. Read `docs/cerb/interface-map.md` if the change spans modules
4. Read `docs/plans/tracker.md` for campaign state

After implementation, sync all docs touched by the change in the same session (Android/cerb-compliant work only).

## Module Structure

Gradle modules (`settings.gradle.kts`):
- `:app`, `:app-core` -- application shell and orchestrator
- `:core/*` -- util, telemetry, notifications, llm, context, pipeline, database, test, test-fakes-domain, test-fakes-platform
- `:data/*` -- ai-core, oss, habit, memory, crm, session, connectivity, asr, tingwu
- `:domain/*` -- core, crm, memory, habit, session, scheduler

Key layout:
- `app-core/` -- core shell, orchestration, features, UI (Compose)
- `domain/` -- pure Kotlin, NO Android imports
- `data/` -- Room, network, platform services
- `core/` -- shared utilities and infrastructure
- `platforms/harmony/smartsales-app/` -- HarmonyOS complete native ArkTS/ArkUI app
- `platforms/harmony/tingwu-container/` -- HarmonyOS Tingwu container (pattern foundation, being absorbed into smartsales-app)

## Language Rules

- Agent communication: English
- Documentation prose: English (no emoji in docs, plans, trackers, changelogs)
- Code comments: Simplified Chinese
- File headers and inline annotations: Simplified Chinese

## Code Style

Kotlin file header:
```kotlin
// File: <relative path>
// Module: <:moduleName>
// Summary: <Simplified Chinese summary>
// Author: created on <YYYY-MM-DD>
```

Conventions:
- 4-space indentation, trailing commas, `val`-first
- Packages: `com.smartsales.<layer>`
- Resource IDs: `lower_snake_case`

## Architectural Boundaries

1. **domain/ purity**: `domain/` must never import `android.*`, legacy `feature.*`, or `com.smartsales.legacy.*`
2. **One product truth**: no SIM-vs-full behavioral fork for non-Mono work. See `docs/specs/base-runtime-unification.md`
3. **Harmony isolation**: native Harmony files (`.ets`, `module.json5`, `ohos.*` imports) must not appear under `app/`, `app-core/`, `core/`, `data/`, or `domain/`
4. **Interface map discipline**: check `docs/cerb/interface-map.md` before any cross-module change

## Build and Test

```bash
./gradlew :app:assembleDebug          # Android debug build
./gradlew :app:installDebug           # install to device
./gradlew testDebugUnitTest           # unit tests
./gradlew lint                        # lint check
```

Harmony build uses hvigor inside `platforms/harmony/tingwu-container/`.

## Debugging

For Android runtime bugs, `adb logcat` is mandatory evidence. Screenshots and user recollection are supporting evidence only.

For compiler/build failures, compiler output is primary evidence. For pure unit-test failures, test output is primary.

If current logs are insufficient, add targeted tags/logging and rerun instead of guessing.

## Key References

| Resource | Path |
|----------|------|
| Product north star | `SmartSales_PRD.md` |
| Main tracker | `docs/plans/tracker.md` |
| Interface ownership | `docs/cerb/interface-map.md` |
| UX contract | `docs/specs/prism-ui-ux-contract.md` |
| Style guide | `docs/specs/style-guide.md` |
| UI element registry | `docs/specs/ui_element_registry.md` |
| Feature dev SOP | `docs/sops/feature-development.md` |
| Debugging SOP | `docs/sops/debugging.md` |
| Code structure | `docs/specs/code-structure-contract.md` |
| Platform governance | `docs/specs/platform-governance.md` |
| Glossary | `docs/specs/GLOSSARY.md` |
| Lessons learned | `docs/reference/agent-lessons-details.md` |
| Harmony tracker | `docs/plans/harmony-tracker.md` |
| Harmony container spec | `docs/platforms/harmony/tingwu-container.md` |

## Multi-Agent Coexistence

This repo hosts configurations for multiple AI agent runtimes. Each has its own instruction layer; all share `docs/`. Codex, Antigravity, and Claude are expected to collaborate on the same repo state.

Do not modify without intent:
- `.agent/` (Antigravity rules and workflows) unless the task explicitly includes Antigravity registration or sync
- `.codex/` (Codex skills)
- `.gemini/`, `.kiro/`, `.roo/`, `.windsurf/`
- `AGENTS.md` (Codex root instructions)

When those files contain useful project knowledge, read and apply the knowledge.

## Security

- Never commit `local.properties` (contains API keys)
- Scrub logs so transcripts do not leak into commits
