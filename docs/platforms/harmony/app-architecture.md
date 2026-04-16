# Harmony Native App Architecture

> **Role**: Architecture reference for the complete native HarmonyOS ArkTS/ArkUI app
> **Status**: Active
> **Date**: 2026-04-16
> **Primary Laws**:
> - `docs/specs/platform-governance.md`
> - `docs/specs/cross-platform-sync-contract.md`
> - `docs/platforms/harmony/native-development-framework.md`
> **Related Docs**:
> - `docs/plans/harmony-tracker.md` (H-04 entry)
> - `docs/platforms/harmony/tingwu-container.md` (pattern origin)
> - `docs/specs/Architecture.md` (Android system laws)
> - `docs/cerb/interface-map.md` (shared interface ownership)

---

## 1. App Root

```
platforms/harmony/smartsales-app/
```

Bundle ID: `com.smartsales.harmony.app`

This is the complete native HarmonyOS app. It absorbs patterns from the Tingwu container (`platforms/harmony/tingwu-container/`) and implements full product parity through phased delivery.

---

## 2. Target Module Structure

Current status on 2026-04-16: the committed scaffold includes the app root, config pipeline, translated domain models, navigation shell, placeholder pages, core utilities, and the first shared service stubs. Additional repositories, feature services, components, and media assets listed below are the intended target structure for later phases, not a claim that every file already exists in the scaffold commit.

```
smartsales-app/
├── AppScope/
│   ├── app.json5                              # App manifest (bundle ID, version)
│   └── resources/                             # App-level resources (icon, strings)
│       ├── base/element/string.json
│       └── base/media/                        # App icon
├── entry/
│   ├── oh-package.json5                       # Entry module package
│   └── src/main/
│       ├── module.json5                       # Entry ability manifest
│       ├── resources/
│       │   ├── base/element/string.json       # Module strings
│       │   ├── base/element/color.json        # Color tokens
│       │   ├── base/media/                    # Module media assets
│       │   └── base/profile/main_pages.json   # Page routing config
│       └── ets/
│           ├── entryability/
│           │   └── EntryAbility.ets           # UIAbility entry point
│           ├── pages/                         # ArkUI page components
│           │   ├── Index.ets                  # Navigation shell
│           │   ├── AudioPage.ets              # Audio/Tingwu surface
│           │   ├── SchedulerPage.ets          # Scheduler surface
│           │   ├── ChatPage.ets               # AI conversation
│           │   ├── ContactsPage.ets           # CRM/entity management
│           │   └── SettingsPage.ets           # Settings and config
│           ├── components/                    # Reusable ArkUI components
│           ├── domain/                        # ArkTS domain models
│           │   ├── Mutation.ets               # UnifiedMutation translation
│           │   ├── Scheduler.ets              # ScheduledTask, UrgencyLevel
│           │   ├── Entity.ets                 # EntityModels, EntityWriter contract
│           │   ├── Session.ets                # ChatMessage, SessionPreview
│           │   └── Audio.ets                  # Audio item, job status, artifacts
│           ├── services/                      # Backend services
│           │   ├── HttpClient.ets             # HTTP transport seam
│           │   ├── TingwuService.ets          # Tingwu API client
│           │   ├── OssService.ets             # OSS upload
│           │   ├── SchedulerService.ets       # Scheduler API client
│           │   ├── AiService.ets              # LLM/AI API client
│           │   ├── EntityService.ets          # Entity storage
│           │   └── FileStore.ets              # Local file I/O
│           ├── state/                         # App state management
│           │   ├── AppState.ets               # Central state types
│           │   └── repositories/              # Per-feature state repositories
│           │       ├── AudioRepository.ets    # Audio pipeline state
│           │       ├── SchedulerRepository.ets
│           │       ├── ChatRepository.ets
│           │       ├── EntityRepository.ets
│           │       └── SessionRepository.ets
│           ├── config/                        # Runtime configuration
│           │   ├── AppConfig.ets              # Config interface definition
│           │   ├── AppRuntimeConfig.ets       # Dynamic import loader with fallback
│           │   └── AppConfig.local.ets        # AUTO-GENERATED (in .gitignore)
│           └── utils/                         # Shared utilities
│               ├── SignatureUtils.ets         # HMAC-SHA1, request signing
│               └── FormatUtils.ets            # Date, byte, display formatters
├── oh-package.json5                           # Project package root
├── build-profile.json5                        # Build profile (products, modules)
└── hvigorfile.ts                              # Build script (config generation)
```

---

## 3. Architectural Patterns

All patterns are proven in the Tingwu container and adopted as the app-wide standard.

### 3.1 State Management: Pub-Sub Repository

Each feature has a **repository class** that owns mutable state and emits snapshots to subscribers.

```typescript
// 模式: 发布-订阅仓库 (Pub-Sub Repository)
type SnapshotListener<T> = (snapshot: T) => void;

class FeatureRepository {
  private snapshot: FeatureSnapshot = createEmptySnapshot();
  private listeners: SnapshotListener<FeatureSnapshot>[] = [];

  getSnapshot(): FeatureSnapshot { return this.snapshot; }

  subscribe(listener: SnapshotListener<FeatureSnapshot>): () => void {
    this.listeners.push(listener);
    listener(this.snapshot);
    return () => {
      const index = this.listeners.indexOf(listener);
      if (index >= 0) this.listeners.splice(index, 1);
    };
  }

  private emit(): void {
    for (const listener of this.listeners) {
      listener(this.snapshot);
    }
  }
}
```

**Rules:**
- Repositories are singletons exported as `export const featureRepository = new FeatureRepository()`
- State is immutable snapshots; mutations create new snapshot objects
- UI subscribes in `aboutToAppear()`, unsubscribes in `aboutToDisappear()`
- Async operations update snapshot progressively (loading → busy → complete/error)

### 3.2 UI: ArkUI @State-Driven Pages

Pages are `@Entry @Component struct` types that bind to repository snapshots via `@State`.

```typescript
@Entry
@Component
struct FeaturePage {
  @State snapshot: FeatureSnapshot = createEmptySnapshot();
  private unsubscribe?: () => void;

  aboutToAppear(): void {
    this.unsubscribe = featureRepository.subscribe((next) => {
      this.snapshot = next;
    });
  }

  aboutToDisappear(): void {
    this.unsubscribe?.();
  }

  build() {
    // ArkUI declarative layout driven by this.snapshot
  }
}
```

**Rules:**
- Pages own their subscription lifecycle
- Pages do not call services directly; they call repository methods
- Pages use builder methods (`renderX()`) for layout sections

### 3.3 Services: Stateless Singletons

Services handle I/O (HTTP, file, device APIs) and expose async operations.

```typescript
class TingwuService {
  async submitTask(objectKey: string): Promise<SubmitResult> { ... }
  async pollStatus(jobId: string): Promise<StatusResult> { ... }
}

export const tingwuService = new TingwuService();
```

**Rules:**
- Services are stateless — no mutable instance state
- Services expose typed return values, not raw HTTP responses
- Services use `HttpClient` as the HTTP transport seam
- Services handle their own error mapping

### 3.4 Config: Build-Time Generation with Runtime Fallback

Secrets and environment config are generated at build time from `local.properties`.

```
local.properties (repo root, not versioned)
    ↓ hvigorfile.ts reads at build time
AppConfig.local.ets (generated, gitignored)
    ↓ AppRuntimeConfig.ets loads via dynamic import
App runtime
```

**Rules:**
- `hvigorfile.ts` validates all required keys and fails the build if missing
- `AppRuntimeConfig.ets` falls back to empty config if the generated artifact is absent (fresh clones don't crash)
- Config state (missing keys) surfaces honestly in the UI

### 3.5 Domain Models: Translated from Kotlin

Domain models in `domain/*.ets` are ArkTS translations of the Kotlin `domain/` layer.

**Translation rules:**
- Kotlin `data class` → ArkTS `interface` (for read-only shapes) or `class` (for mutable types)
- Kotlin `enum class` → ArkTS `enum` or string union type
- Kotlin `sealed class` → ArkTS tagged union (`type X = A | B | C`)
- Preserve all fields and their semantics
- Preserve all validation rules and business constraints
- Use the same field names (camelCase in both languages)
- Add Chinese comments explaining the domain meaning per project language rules

### 3.6 Navigation: ArkUI Navigation Shell

The Index page acts as a navigation shell using ArkUI `Navigation` + `NavPathStack`.

```typescript
@Entry
@Component
struct Index {
  private navStack: NavPathStack = new NavPathStack();

  build() {
    Navigation(this.navStack) {
      // Tab bar or navigation content
    }
  }
}
```

Feature pages register as `NavDestination` and are pushed/popped via `navStack`.

---

## 4. Domain Model Translation Map

This table maps Kotlin domain models to their ArkTS equivalents in the complete native app.

| Kotlin Source | ArkTS Target | Status |
|---------------|-------------|--------|
| `domain/core/UnifiedMutation.kt` | `domain/Mutation.ets` | Pending |
| `domain/core/SafeEnum.kt` | `domain/Mutation.ets` (utilities) | Pending |
| `domain/scheduler/ScheduledTask.kt` | `domain/Scheduler.ets` | Pending |
| `domain/scheduler/UrgencyLevel.kt` | `domain/Scheduler.ets` | Pending |
| `domain/scheduler/ScheduleBoard.kt` | `domain/Scheduler.ets` | Pending |
| `domain/crm/EntityModels.kt` | `domain/Entity.ets` | Pending |
| `domain/crm/EntityWriter.kt` | `domain/Entity.ets` | Pending |
| `domain/session/ChatMessage.kt` | `domain/Session.ets` | Pending |
| `domain/session/SessionPreview.kt` | `domain/Session.ets` | Pending |
| `HarmonyTingwuModels.ets` (container) | `domain/Audio.ets` | Migrated from H-01 |

---

## 5. Build, Sign, Deploy Workflow

### 5.1 Build

```bash
cd platforms/harmony/smartsales-app/
hvigorw assembleHap --mode module -p product=default
```

### 5.2 Sign

Configure signing in `build-profile.json5` with AGC-generated certificate. See `docs/platforms/harmony/test-signing-ledger.md` for signing status.

### 5.3 Deploy

```bash
hdc install <path-to-signed.hap>
hdc shell aa start -a EntryAbility -b com.smartsales.harmony.app
```

### 5.4 Debug

```bash
hdc shell hilog | grep -i smartsales
```

---

## 6. Relationship to Tingwu Container

The Tingwu container (`platforms/harmony/tingwu-container/`) is the pattern origin. Code is **absorbed, not imported**.

| Container File | App Destination | Changes |
|----------------|----------------|---------|
| `HarmonyTingwuModels.ets` | `domain/Audio.ets` | Rename, generalize naming |
| `HarmonyContainerRepository.ets` | `state/repositories/AudioRepository.ets` | Extract pub-sub base pattern |
| `HarmonyTingwuService.ets` | `services/TingwuService.ets` | Direct migration |
| `HarmonyOssService.ets` | `services/OssService.ets` | Direct migration |
| `HarmonyHttpClient.ets` | `services/HttpClient.ets` | Direct migration |
| `HarmonyFileStore.ets` | `services/FileStore.ets` | Generalize for multi-feature |
| `HarmonyPickerService.ets` | `services/FileStore.ets` (merge) | Absorb into file service |
| `HarmonySignatureUtils.ets` | `utils/SignatureUtils.ets` | Direct migration |
| `HarmonyFormatUtils.ets` | `utils/FormatUtils.ets` | Direct migration |
| `HarmonyContainerRuntimeConfig.ets` | `config/AppRuntimeConfig.ets` | Generalize for all config |
| `HarmonyContainerConfig.ets` | `config/AppConfig.ets` | Expand interface |
| `hvigorfile.ts` | `hvigorfile.ts` | Expand with new config keys |
| `Index.ets` | `pages/AudioPage.ets` | Extract audio UI into dedicated page |
| `EntryAbility.ets` | `entryability/EntryAbility.ets` | Direct migration |

---

## 7. Anti-Contamination

Hard guardrails from `docs/specs/platform-governance.md`:

- No `.ets` source files under `app/**`, `app-core/**`, `core/**`, `data/**`, or `domain/**`
- No `module.json5`, `oh-package.json5`, `hvigorfile.ts` outside `platforms/harmony/`
- No `ohos.*` imports in the Android tree
- No Compose/Android imports in the Harmony tree
