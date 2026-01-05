---
trigger: always_on
---

# Smart Sales Project Context

Project-specific rules and context for AI agents working on this codebase.

---

## Source of Truth Hierarchy

### Authoritative Specs (Current)
1. `docs/Orchestrator-V1.md` — the only current orchestrator spec
2. `docs/orchestrator-v1.schema.json` — V1 JSON schema
3. `docs/orchestrator-v1.examples.json` — V1 examples

### UX Contract Precedence
1. `docs/ux-contract.md` — interaction/layout/flows/HUD
2. `docs/Orchestrator-V1.md` — reasoning/pipeline structure
3. `docs/style-guide.md` — visual and code style
4. Existing Android implementation and tests
5. Archived implementations (reference only)

### Archived (Historical Only)
- `docs/archived/**` — Do NOT use as target behavior
- Ignore `docs/current-state.md` and `docs/T-Task.md` for implementation

---

## Provider Policy (V1)

### Default Transcription Lane
- **Default:** Tingwu + OSS
- **XFyun:** Disabled by default; enable only when explicitly turned on and validated

### Transfer-Only Guardrail
- Translate/predict/analysis: unavailable by default
- Block before request; never "send first and see"

### Third-Party Integration
- `docs/source-repo.json` — integration reference only (OSS/Tingwu/XFyun/Dashscope)
- `docs/xfyun-asr-rest-api.md` — authoritative XFyun REST details

---

## Project Structure

| Directory | Purpose |
|-----------|---------|
| `app/` | Compose shell, single entry point |
| `tingwuTestApp/` | Tingwu upload helpers |
| `data/ai-core/` | DashScope/Tingwu/OSS clients |
| `core/util` | Reusable primitives |
| `core/test` | JVM test fixtures |
| `feature/chat` | Chat feature |
| `feature/media` | Media feature |
| `feature/connectivity` | Connectivity feature |
| `docs/` | Documentation and plans |
| `third_party/maven-repo/` | Cached artifacts |

---

## Build Commands

```bash
# Debug build
./gradlew :app:assembleDebug

# Install and launch
./gradlew :app:installDebug && adb shell am start com.smartsales.aitest/.AiFeatureTestActivity

# Unit tests
./gradlew testDebugUnitTest

# Lint
./gradlew lint

# Instrumented tests
./gradlew :app:connectedDebugAndroidTest
```

---

## Code Style

### Kotlin
- 4-space indentation
- Trailing commas in multi-line parameters
- `val`-first data classes
- Compose functions: file-private unless reused
- Filename matches primary type

### Naming
- Packages: `com.smartsales.<layer>`
- Resource IDs: `lower_snake_case` (e.g., `media_server_card_title`)
- Gradle modules: `:feature:*` / `:core:*`

### File Headers
```kotlin
// File: <relative path>
// Module: <:moduleName>
// Summary: <short English summary>
// Author: created on <YYYY-MM-DD>
```

---

## Testing

| Type | Location | Runner |
|------|----------|--------|
| Unit tests | `<module>/src/test/` | JUnit 4 + kotlinx-coroutines-test |
| UI tests | `<module>/src/androidTest/` | `./gradlew :app:connectedDebugAndroidTest` |

- Reuse helpers from `core/test` for fake dispatchers
- Name suites after behavior: `DeviceConnectionManagerTest`, `TingwuRequestMapperTest`

---

## Security & Config

### Never Commit
- `local.properties` (stores DashScope, Tingwu, OSS keys)

### Environment
- `TINGWU_BASE_URL` auto-appends `/openapi/tingwu/v2/` unless proxy URL provided
- Verify `JAVA_HOME` before Gradle
- Prefer `third_party/maven-repo/` when network limited
- Scrub logs: no transcripts/payloads in commits

---

## China Network Environment

When changing build scripts or dependency config:
- Prefer Aliyun mirror or domestic mirrors
- Avoid overseas sources (slow/timeout-prone)

---

## Commit & PR Guidelines

### Commit Style
```
Scope: Imperative summary
```

### PR Requirements
- Affected modules
- Reproduction steps
- Test/lint commands
- Screenshots/logs for UI, BLE, or transcription changes
- Link issues or TODO IDs
- Highlight dependency or credential updates
