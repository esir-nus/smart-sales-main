# Cross-Platform Development Sync Contract

> **Role**: Canonical contract for managing shared product truth across Android (foundation), HarmonyOS, and iOS platforms
> **Status**: Active governance law
> **Date**: 2026-04-16
> **Primary Law**: `docs/specs/platform-governance.md`
> **Related Docs**:
> - `docs/platforms/harmony/native-development-framework.md`
> - `docs/platforms/harmony/app-architecture.md`
> - `docs/cerb/interface-map.md`
> - `docs/projects/harmony-native/tracker.md`
> - `CLAUDE.md`

---

## 1. Foundation Principle

Android is the **foundation and source** of shared product truth. HarmonyOS and iOS are **translation platforms** that consume shared truth and rewrite natively.

```
Android (foundation, source of shared truth)
    ↓ shared docs, domain semantics, behavioral contracts
HarmonyOS (translation platform, complete native ArkTS/ArkUI)
iOS (translation platform, complete native Swift/SwiftUI — future)
```

No platform may fork shared product truth. When behavior must diverge, it is documented in a platform overlay, not silently changed.

---

## 2. What Is Shared

These artifacts are shared across all platforms and owned by the `develop` branch:

### 2.1 Product truth (docs)

| Artifact | Location | Owned By |
|----------|----------|----------|
| Product journeys and user goals | `SmartSales_PRD.md` | Product |
| Core flows (behavioral north star) | `docs/core-flow/**` | Product |
| Implementation contracts | `docs/cerb/**`, `docs/cerb-ui/**` | Android + Shared |
| Specs and architecture | `docs/specs/**` | Governance |
| Interface ownership map | `docs/cerb/interface-map.md` | Architecture |

### 2.2 Domain semantics (code-as-spec)

The `domain/` layer (pure Kotlin, zero platform imports) serves as the **behavioral specification** that platform implementations translate from.

| Module | Key Contracts | Translation Target |
|--------|---------------|-------------------|
| `domain/core` | `UnifiedMutation`, `SafeEnum` | LLM output contract — every platform must produce and consume the same mutation shape |
| `domain/crm` | `EntityWriter`, `EntityRepository`, `EntityModels` | Entity resolution rules, dedup/merge/alias semantics |
| `domain/memory` | `MemoryRepository`, `MemoryModels` | Conversation memory lifecycle |
| `domain/habit` | `UserHabit`, `ReinforcementLearner` | Behavioral pattern observation |
| `domain/scheduler` | `ScheduledTask`, `ScheduledTaskRepository`, `ScheduleBoard`, `UrgencyLevel` | Scheduler semantics, path A/B routing, conflict rules |
| `domain/session` | `ChatMessage`, `UserProfile`, `SessionPreview` | Session and conversation state |

**Translation rule**: platform implementations must preserve the **semantic meaning** of domain contracts, not the Kotlin syntax. An ArkTS `ScheduledTask` interface must have the same fields and validation rules as the Kotlin `ScheduledTask` data class.

### 2.3 API contracts

- LLM prompt schemas and mutation output format
- Backend API endpoints, request/response shapes
- OSS upload protocol
- Tingwu submission and polling protocol

### 2.4 Not shared (platform-owned)

These are platform-specific and must be rewritten natively:

- UI composition (Compose / ArkUI / SwiftUI)
- Navigation shell and lifecycle
- Local storage engine (Room / relationalStore / CoreData)
- HTTP client implementation
- Permissions, notifications, background execution
- Device integration (BLE, hardware)
- Build, sign, deploy toolchain

---

## 3. Sync Flow

### 3.1 Branch sync

```
develop (Android maintenance + shared contracts)
    ↓ git merge (at least weekly)
platform/harmony (HarmonyOS integration trunk)
```

When `develop` changes shared docs or domain contracts, the sync to `platform/harmony` should happen within the same week. Breaking changes to shared contracts require immediate sync.

### 3.2 Domain model translation sync

When a Kotlin domain model changes on `develop`:

1. The change lands on `develop` with updated shared docs
2. During the weekly `develop → platform/harmony` merge, the shared docs arrive on `platform/harmony`
3. The corresponding ArkTS domain model in `platforms/harmony/smartsales-app/entry/src/main/ets/domain/` must be updated to match
4. The Harmony tracker must record the domain model sync

For iOS (future): same pattern, with Swift domain models in the iOS app root.

### 3.3 Triggering immediate sync

Shared contract changes that require immediate sync (not waiting for weekly cadence):

- **Breaking changes** to `UnifiedMutation` or any domain interface
- **New fields** added to API contracts
- **Behavioral changes** to core flows (scheduler semantics, entity resolution rules)
- **Security fixes** that affect API authentication or data handling

---

## 4. Translation Protocol

### 4.1 Before translating a feature

1. Read the owning `docs/core-flow/` doc
2. Read the owning `docs/cerb/` spec and interface
3. Read the Android implementation as behavioral reference
4. Identify what is shared truth (must match) vs platform-owned (rewrite natively)
5. Check for any platform overlay in `docs/platforms/harmony/` (or `docs/platforms/ios/`)

### 4.2 During translation

- Translate domain semantics faithfully (same fields, same validation, same business rules)
- Rewrite UI, navigation, storage, and platform services natively
- Do not mechanically port code structure; follow platform idioms
- When behavior cannot be supported, declare it explicitly and remove/block the entrypoint

### 4.3 After translation

- Update the Harmony overlay doc in `docs/platforms/harmony/`
- Update the Harmony tracker with delivery status
- If translation revealed a gap in shared docs, file a doc-update on `develop`

---

## 5. Breaking Change Protocol

When a shared contract changes in a way that affects platform implementations:

### 5.1 On develop (foundation platform)

1. Update the shared docs and domain models
2. Add a note to the commit message: `[shared-contract-change]`
3. If the change is breaking, add to `docs/specs/cross-platform-changelog.md` (see section 7)

### 5.2 On platform branches (after sync)

1. During merge, review the changelog for breaking entries
2. Update the platform's translated domain models
3. Run platform builds to verify compatibility
4. Update platform overlay docs if behavior changed

---

## 6. Platform Overlay Rules

### 6.1 When to use overlays

Use a platform overlay (`docs/platforms/harmony/*.md` or `docs/platforms/ios/*.md`) when:

- delivery mechanics differ from Android
- a feature is partially supported or unsupported
- platform-specific constraints affect user-visible behavior

### 6.2 When to create platform-specific specs

Create a platform-specific companion spec only when:

- implementation ownership diverges so heavily that overlays become misleading
- user-visible behavior diverges enough that the shared spec no longer describes the platform's product

### 6.3 Overlay format

Each overlay must include:

- **Supported set**: what this platform delivers from the shared feature
- **Unsupported set**: what is not delivered and why
- **Platform-specific constraints**: any divergence from Android behavior
- **Translation notes**: any notable differences in how the feature is implemented

---

## 7. Cross-Platform Changelog

File: `docs/specs/cross-platform-changelog.md`

Purpose: record shared contract changes that affect platform implementations. This is separate from the main `CHANGELOG.md` which tracks user-visible changes.

Format:

```markdown
## YYYY-MM-DD

### [shared-contract-change] Title
- **What changed**: description of the shared contract change
- **Affected platforms**: HarmonyOS, iOS, or both
- **Domain models affected**: list of changed types
- **Migration action**: what platform implementations need to do
```

---

## 8. Acceptance

This contract is working if:

- shared product truth remains single-source on `develop`
- platform implementations faithfully translate domain semantics without silent forks
- breaking changes are communicated through the cross-platform changelog
- weekly syncs from `develop → platform/*` actually happen
- platform overlay docs stay current with actual implementation state
- engineers on any platform can find the shared truth and their platform's translation status without guesswork
