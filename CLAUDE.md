# Smart Sales -- Claude Code Instructions

## Project Identity

Smart Sales is a voice-first sales operating app for sales operators. It captures real-world sales activity with low friction, turns it into review/follow-up/scheduling surfaces, and keeps AI assistive rather than autonomous.

Platform posture:
- **Android**: primary platform, cerb-compliant (code and docs are written together and kept aligned)
- **HarmonyOS**: backend-centric port, references Android docs as product truth, UI docks to backend
- **iOS**: anticipated, same model as HarmonyOS (not yet started)

Android is the canonical implementation. HarmonyOS and iOS consume Android's docs and specs, then rewrite into their native frameworks.

Read `SmartSales_PRD.md` for full product identity.

## Branch Model

```
master (protected, promotion-only — requires PR)
  └── develop (Android + shared code, daily work)
        ├── platform/harmony (HarmonyOS platform work)
        └── platform/ios (future)
```

- **develop**: All Android and shared-contract work happens here. Cerb-compliance applies.
- **platform/harmony**: HarmonyOS-specific work. Receives shared contracts from develop via merge. Never merges back into develop.
- **master**: Protected. Receives promotions from develop via PR only. No direct commits.
- Feature work: create branch from `develop` (or `platform/harmony`), PR back.
- Shared contracts flow: `develop → platform/*`, never the reverse.

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
- `platforms/harmony/tingwu-container/` -- HarmonyOS ArkTS transient app

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
