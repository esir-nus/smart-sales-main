# Repository Guidelines

## Source of Truth (SoT) and Archived Docs

- SoT: The only authoritative spec is `docs/specs/Orchestrator-V1.md` (CURRENT) plus the V1 schema/examples.
- Ignore `docs/archived/**` for implementation behavior and contracts. Archived docs are historical only and must not be used as a target spec.
- Ignore non-normative docs such as `docs/current-state.md` and `docs/T-Task.md` for implementation behavior; use SoT.

---

## Doc Sources and Precedence (Docs > Code > Guessing)

- Orchestrator contract sources (CURRENT):
  - `docs/specs/Orchestrator-V1.md` (the only current authoritative spec)
- V1 JSON (CURRENT):
  - `docs/specs/orchestrator-v1.schema.json`
  - `docs/specs/orchestrator-v1.examples.json`
- ARCHIVED (historical reference only; not target behavior):
  - Archived specs live under `docs/archived/**` (historical only).

- UX contract source precedence:
  1. `docs/specs/Orchestrator-V1.md` (reasoning and pipeline structure/boundaries)
  2. `docs/specs/ux-contract.md` (🔒 canonical: data contracts, pipelines, feature boundaries — locked, requires Product/Eng approval)
  3. `docs/guides/ux-experience.md` (📝 experience: state inventories, microcopy, timing, layout — UX-owned, modifiable)
  4. `docs/guides/style-guide.md` (visual and code style rules)
  5. Existing Android implementation and tests
  6. Archived UI/history implementations for reference only

---

## Documentation language

- Documentation prose must be English.
- Chinese is allowed only inside fenced code blocks as code comments, and should be Simplified Chinese.

---

## Third-Party Integration Registry

- The role of `docs/specs/source-repo.json` and `docs/source-repo.schema.json`:
  - Third-party service integration reference only (OSS / Tingwu / XFyun / Dashscope + placeholders)
  - Records: endpoint, key request/response fields, guardrails, failure modes, test evidence, and code location index
  - Prohibited: Orchestrator/MetaHub product specs, M1/M2/M3 schema, business semantics definitions
- The only authoritative source for XFyun REST details remains `docs/archived/xfyun-asr-rest-api.md` (deprecated; XFyun lane is disabled by default)
  - Do not duplicate its parameter tables in other docs (only link + conclusion summary)

---

---

## Purification Mindset

> These rules apply to ALL coding work. Not a "mode" — a default behavior.

### Pre-Coding Audit
Before modifying code, always:
1. Read the relevant tracker for architecture status
2. Read the relevant spec for module contracts
3. `grep` to verify assumptions — never guess

### Rewrite > Extract
When touching legacy code:
- **Rewrite**: When code is misaligned with target architecture or tightly coupled
- **Extract**: Only when code is already aligned and loosely coupled
- **Default to rewrite** — old patterns don't deserve preservation

### Organic Debt Payment
- Fix debt **when it blocks** feature delivery, not proactively
- Mark deviations in tracker, don't hunt them
- Ship features first, refactor post-ship

### Evidence-Based Decisions
| Before | Do This |
|--------|---------|
| Claiming "X exists" | `grep -rn "X" feature/` |
| Claiming "complete" | Build passes |
| Modifying a file | `view_file` to read it first |

---

## Architecture Workflow Order

**Priority**: Finish spec → Ship features → Check deviations

### Workflow Rules

1. **Spec-First**: Finish `Orchestrator-V1.md` before major purity refactors
   - Writing spec surfaces design gaps early
   - Don't refactor toward incomplete/aspirational specs

2. **Ship Features**: Implement features following current patterns
   - Structure guardrails prevent new god files
   - Purity issues will surface organically during development

3. **Post-Ship Deviation Check**: After feature complete, audit spec vs reality
   - Only fix deviations that **hurt** (coupling, duplication, complexity)
   - Document acceptable deviations inline in spec

### Module Status Convention

When documenting V1 modules in specs, include implementation status:

```markdown
### 3.2.3 Sanitizer
**Status:** IMPLEMENTED as TranscriptFormatter in data:ai-core
**Deviation:** Not a separate module; coupled to TingwuRunner
**Reason:** Single consumer, no benefit to extraction yet
```

Valid status values:
- `IMPLEMENTED` — fully realized per spec
- `PARTIAL` — partially implemented
- `DEVIATION` — implemented differently than spec (document reason)
- `DEFERRED` — not yet implemented (document trigger condition)
- `DEPRECATED` — spec section no longer applies

### Anti-Pattern

❌ **Don't** refactor toward theoretical purity in incomplete specs  
✅ **Do** refactor toward working software with documented deviations

---

## Provider Policy (V1)

1) Default transcription lane
- Default provider lane: Tingwu + OSS
- XFyun lane: disabled by default; enable only when explicitly turned on and validated available

2) Transfer-only guardrail (must be consistent)
- Translate/predict/analysis are treated as unavailable/disabled by default
- Must block before request; do not "send first and see" (avoid quota/capability mismatch failures)

---

## Core Behavior Requirements (for AI Agents)

1) Prefer clean, simple, modular code
- Small functions, small classes, clear module boundaries.
- Avoid excessive abstraction and deep inheritance.
- Use simple solutions when they work; no flashy design.

2) Use simple, clear language
- Code comments use Simplified Chinese.
- Keep sentences short; avoid long or complex structures.
- Variable names must be clear and reflect real meaning.
- All important code blocks must include short Simplified Chinese comments for quick understanding.

3) Do not be lazy; always read relevant files fully
- Before modifying a file, read the whole file.
- Before referencing a doc, read the whole doc.
- Do not decide based on guesses or fragments.

4) China network environment first
- When changing build scripts or dependency config:
  - Prefer Aliyun mirror or other domestic mirrors.
  - Avoid overseas sources that are slow or frequently time out.
- In guidance, you may provide example mirror settings but do not force existing scripts to change.

5) Kotlin project: use helpers appropriately
- Use helper scripts/tools per best practice; follow code style, tests, and dependency rules.
- Do not skip quality checks for convenience; helpers accelerate, not replace, review and validation.

6) Limited-perception strategy (must follow)
- If failure looks like network/sandbox/credential limits (e.g., dependency download failure, DNS/TLS errors, 401/403, 429 rate limit, host blocked), try only 1-2 times (max 1 normal attempt + 1 minimal recheck/retry).
- Do not try to bypass (no repeated retries, mirror changes, proxying, dependency version changes, upgrades/downgrades, etc.) - stop first.
- Output an "evidence pack" and request minimal human intervention from Operator: include command, key error lines, suspected restricted host/endpoint, and the minimal action needed (e.g., confirm network/credentials/allowlist/mirror).
- Do not leak secrets/tokens/personal info in evidence.

---

## Code Style and Language Rules

- All source files must:
  1. Include a unified file header at the top, for example:

     ```kotlin
     // File: <relative path>
     // Module: <:moduleName>
     // Summary: <short English summary>
     // Author: created on <YYYY-MM-DD> # use real time, do not invent or guess
     ```

## Project Structure and Module Organization
The workspace is defined in `settings.gradle.kts` and intentionally lean. `app/` hosts the Compose shell and remains the single entry point, `tingwuTestApp/` provides Tingwu upload helpers, `data/ai-core/` wraps DashScope/Tingwu/OSS clients, and `core/util` plus `core/test` hold reusable primitives and JVM fixtures. Feature code stays inside `feature/chat`, `feature/media`, and `feature/connectivity`. Assets live in `app/src/main/res/`, docs and plans go under `docs/` and `plans/`, and cached artifacts belong in `third_party/maven-repo/`.

## Build, Test, and Development Commands
Run Gradle from the repo root:
- `./gradlew :app:assembleDebug`
- `./gradlew :app:installDebug && adb shell am start com.smartsales.aitest/.AiFeatureTestActivity`
- `./gradlew testDebugUnitTest`
- `./gradlew lint`

## Coding Style and Naming Conventions
Write Kotlin with four-space indentation, trailing commas in multi-line parameters, and `val`-first data classes. Keep Compose functions file-private unless reused and match filenames to their primary type. Packages stay under `com.smartsales.<layer>`, resource IDs use lower snake case (`media_server_card_title`), and Gradle modules should preserve the existing `:feature:*` / `:core:*` naming pattern.

## Testing Guidelines
Place unit tests in `<module>/src/test/` with JUnit 4 and `kotlinx-coroutines-test`, reusing helpers from `core/test` for fake dispatchers or result assertions. Instrumented or Compose UI tests belong in `<module>/src/androidTest/`; run them with `./gradlew :app:connectedDebugAndroidTest` on an emulator that has BLE, storage, and microphone permissions granted. Name suites after the behavior under test (`DeviceConnectionManagerTest`, `TingwuRequestMapperTest`).

## Commit and Pull Request Guidelines
Keep commit titles in `Scope: Imperative summary` style. PRs must outline affected modules, reproduction steps, test/lint commands, and screenshots or logs for UI, BLE, or transcription changes. Link issues or TODO IDs and highlight dependency or credential updates explicitly.

## Security and Configuration Tips
Never commit `local.properties`; it stores DashScope, Tingwu, and OSS keys consumed by `data/ai-core`. `TINGWU_BASE_URL` automatically appends `/openapi/tingwu/v2/` unless you pass a proxy URL - note any overrides in PR descriptions. Verify `JAVA_HOME` before running Gradle. Prefer the cached `third_party/maven-repo/` when network access is limited and scrub logs so transcripts or payloads do not leak into commits.
