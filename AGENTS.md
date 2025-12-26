# Repository Guidelines

## Role Contract Reference

This project uses a fixed multi-agent workflow:

- **Operator** = You (human developer, writes all real code)  
- **Orchestrator** = ChatGPT default mode (specs, flows, contracts, no code)  
- **Codex** = ChatGPT code-analysis mode (invoked with `Codex:`)

The full rules and invocation protocol are defined in **`role-contract.md`**.  
All agents must follow that document as the authoritative behavior contract.

- **Orchestrator spec source**: `docs/Orchestrator-V1.md` is the only authoritative spec (CURRENT). `docs/archived/Orchestrator-MetadataHub-V7.md` and earlier versions are archived and for historical reference only.
- **UX spec priority order**:
  1. `docs/ux-contract.md` (the only current UX source of truth: interaction/layout/flow)
  2. `docs/Orchestrator-V1.md` (reasoning + pipeline spec; V1 is current)
  3. `docs/style-guide.md` (visual guidelines)
  4. Existing Android implementation and tests
  5. Archived React/UI is historical only; `assistant-ux-contract.md` is archived and must not be treated as current

---

## Development Lifecycle: Feasibility → Simple Deployment → Refinement

> Note: This is a general engineering cadence (not LLM-specific). Core principle: **learn first, ship minimal, then tighten**. Avoid premature hard constraints during feasibility work, which can create false negatives and slow iteration.

### 1) Feasibility check (Learning Mode)

- Goal: prove the approach works end-to-end on real examples, and you can **see** what happened.
- Default posture: **permissive + observable**, not **strict + silent** (silent rejection is the fastest way to create false negatives).
- Avoid: early strict validators / hard rejection / closed-world schemas that block outputs so you can’t inspect results.
- Prefer: instrumentation and evidence (HUD/trace/files/unit-test assertions). Dry-run / annotated output beats enforcement.
- Must: define exactly one success metric per iteration (e.g., `pipeline runs + HUD shows key evidence`).
- Guardrail rule: all guardrails must be **fail-soft** (fallback/skip) and must not block the main flow.

### 2) Simple deployment (Clean + tweakable)

- Goal: ship a minimal version behind a flag that is clean, simple, and runtime-tweakable.
- Requirements: clear module boundaries, settings-driven knobs, still observable and reversible (for rollout and debugging).
- Avoid: overcomplicating implementation “just to cover every edge case”.

### 3) Refinement & robustness

- Goal: after feasibility is proven, add strict constraints, closed-world schemas, caps/invariants, rejection rules, and broader tests.
- Rule: any hard constraint must map to an **observed failure mode** (evidence + reproduction path). No speculative gating.

**Rule of thumb (when hard constraints are allowed)**

> Only add a hard constraint when you can name the failure it prevents and show evidence that failure actually happens. Early phases optimize for learning speed and observability; avoid “professional but ineffective” barriers.

**Glossary**

- Learning Mode: optimize for fast falsification/confirmation. Imperfections are acceptable, but the system must be observable, reversible, and diagnosable (avoid false negatives).

## Versioning & Doc Discipline

1. **Current specs**
   - CURRENT: `docs/Orchestrator-V1.md`
   - CURRENT JSON: `docs/orchestrator-v1.schema.json`, `docs/orchestrator-v1.examples.json`
   - ARCHIVED: `docs/archived/Orchestrator-MetadataHub-V7.md`
   - ARCHIVED: `docs/archived/Orchestrator-MetadataHub-V6.md`
   - ARCHIVED: `docs/archived/Orchestrator-MetadataHub-V5.md`
   - ARCHIVED: `docs/archived/Orchestrator-MetadataHub-V4.md`

2. **XFyun-first (documentation stance)**
   - XFyun REST single source of truth: `docs/xfyun-asr-rest-api.md`
   - Do not copy large parameter tables into other docs (only “link + summary”)

3. **Capability guardrails (must remain consistent)**
   - Only `transfer` is allowed today; `translate/predict/analysis` are disabled by default. Do not “send requests to try” (avoid failures like `failType=11`).

4. **Provider lanes (V1)**
   - Default transcription lane: Tingwu + OSS
   - XFyun: optional lane, disabled by default; enable only when explicitly switched on and verified available
   - OSS: used by Tingwu lane to provide stable FileUrl; XFyun lane uses direct file-stream upload (no OSS in active path)

## Core Behavior Requirements (for Codex)

1. **Prefer clean, simple, modular code**
   - Small functions, small classes, clear module boundaries.
   - Avoid over-abstraction and deep inheritance.
   - Prefer simple solutions over fancy designs.

2. **Use simple, readable language**
   - Comments and docs should be in Simplified Chinese (unless explicitly overridden by Operator).
   - Keep sentences short; avoid overly complex structures.
   - Use clear variable names that reflect real meaning.
   - All important code blocks must include short Chinese comments so teammates can quickly understand.

3. **No shortcuts: read the full relevant files**
   - Before modifying a file, read the full file.
   - Before referencing a document (e.g., `dev_plan.md`), read the full document.
   - Do not make decisions based on guesses or partial snippets.

4. **China network environment first**
   - When changing build scripts or dependency config:
     - Prefer Aliyun mirror or other mainland mirrors.
     - Avoid overseas sources that are slow or frequently time out.
   - When suggesting commands/config, you may provide mirror examples, but do not force changes to existing scripts.

5. **For Kotlin projects, use helpers appropriately**
   - Use helper scripts/tools following best practices and existing style/tests/dependency norms.
   - Do not skip quality checks for convenience; helpers accelerate work but do not replace review/verification.

6. **Constraint awareness policy (must follow)**
   - If a failure looks like a network/sandbox/credential restriction (e.g., dependency download fails, DNS/TLS errors, 401/403, 429, blocked host), only try **1–2 times** (max 1 normal attempt + 1 minimal re-check/retry).
   - Do not attempt “workarounds” (no repeated retries, mirror swapping, proxies, dependency version changes, upgrades/downgrades). Stop first.
   - Provide an evidence pack and request minimal Operator intervention: include the command, key error lines, suspected host/endpoint, and the smallest Operator action needed (e.g., confirm network/credentials/allowlist/mirror).
   - Do not leak keys/tokens/personal info in the evidence.

   --- 
## Code Style & Language Rules

- All **source files** must:
  1. Include a standard Chinese header at the top, for example:

     ```kotlin
     // 文件：<相对路径>
     // 模块：<:moduleName>
     // 说明：<简要中文说明>
     // 作者：创建于 <YYYY-MM-DD> # use real time, do not invent or guess
     ```

## Project Structure & Module Organization
The workspace is defined in `settings.gradle.kts` and intentionally lean. `app/` hosts the Compose shell and remains the single entry point, `tingwuTestApp/` provides Tingwu upload helpers, `data/ai-core/` wraps DashScope/Tingwu/OSS clients, and `core/util` plus `core/test` hold reusable primitives and JVM fixtures. Feature code stays inside `feature/chat`, `feature/media`, and `feature/connectivity`. Assets live in `app/src/main/res/`, docs and plans go under `docs/` and `plans/`, and cached artifacts belong in `third_party/maven-repo/`.

## Build, Test, and Development Commands
Run Gradle from the repo root:
- `./gradlew :app:assembleDebug` – builds the Compose shell and all dependent modules.
- `./gradlew :app:installDebug && adb shell am start com.smartsales.aitest/.AiFeatureTestActivity` – deploys and launches on a device or emulator.
- `./gradlew testDebugUnitTest` – runs JVM tests across modules; narrow the scope with `:feature:connectivity:testDebugUnitTest` while iterating.
- `./gradlew lint` – executes Android/Kotlin static analysis before review.

## Coding Style & Naming Conventions
Write Kotlin with four-space indentation, trailing commas in multi-line parameters, and `val`-first data classes. Keep Compose functions file-private unless reused and match filenames to their primary type. Packages stay under `com.smartsales.<layer>`, resource IDs use lower snake case (`media_server_card_title`), and Gradle modules should preserve the existing `:feature:*` / `:core:*` naming pattern.

## Testing Guidelines
Place unit tests in `<module>/src/test/` with JUnit 4 and `kotlinx-coroutines-test`, reusing helpers from `core/test` for fake dispatchers or result assertions. Instrumented or Compose UI tests belong in `<module>/src/androidTest/`; run them with `./gradlew :app:connectedDebugAndroidTest` on an emulator that has BLE, storage, and microphone permissions granted. Name suites after the behavior under test (`DeviceConnectionManagerTest`, `TingwuRequestMapperTest`).

## Commit & Pull Request Guidelines
Recent history (`Refactor: Clean up AiFeatureTestActivity`, `Update .gitignore…`) uses a `Scope: Imperative summary` style—keep the first verb capitalized, keep bodies wrapped near 72 columns, and add `Test:` trailers for executed commands. PRs must outline affected modules, reproduction steps, test/lint commands, and screenshots or logs for UI, BLE, or Tingwu changes. Link issues or TODO IDs and highlight dependency or credential updates explicitly.

## Security & Configuration Tips
Never commit `local.properties`; it stores DashScope, Tingwu, and OSS keys consumed by `data/ai-core`. `TINGWU_BASE_URL` automatically appends `/openapi/tingwu/v2/` unless you pass a proxy URL—note any overrides in PR descriptions. The repo pins Java 17 in `gradle.properties`; verify `JAVA_HOME` before running Gradle. Prefer the cached `third_party/maven-repo/` when network access is limited and scrub logs so chat transcripts or BLE payloads do not leak into commits.
