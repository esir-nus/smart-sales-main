# 🔍 Interface Map Audit Report

**Date:** 2026-03-05
**Scope:** `interface-map.md` vs `app-prism` Codebase
**Methodology:** Exhaustive `grep` and file inspection across all 5 layers defined in the interface map.

## 🚨 Major Architecture Violations & False Positives

1. **RLModule (Layer 5)**
   - **False Positive!** The map lists this as `✅ Shipped`. However, `PrismModule.kt` statically binds `FakeReinforcementLearner`. This is a skeleton implementation masquerading as production code. It must be downgraded to `📐 Interface only`.
2. **NotificationService (Layer 1)**
   - **Lattice Violation!** `domain/notification/NotificationService.kt` imports `android.app.PendingIntent`. Prism rules strictly forbid Android imports in the `domain` layer.
3. **OSS Module Ownership**
   - **Misplaced Domain!** The map lists OSS under Prism Infrastructure, but `OssUploader.kt` and `RealOssUploadClient.kt` live entirely inside the `data/oss` and `data/ai-core` modules. Prism has no OSS abstractions.

---

## 📉 Interface Drift (Spec vs. Reality)

*Code has evolved significantly past the static documentation. ALMOST ALL interfaces have signature drift.*

### Layer 1: Infrastructure
| Module | Spec Signature | Actual Code Signature | Verdict |
|--------|---------------|-----------------------|---------|
| **ConnectivityBridge** | `connectUsingSession` | Signature deleted; replaced by `val connectionState` & `suspend fun isReady()` | ❌ Drift (Code ahead) |
| **NotificationService** | `show(NotificationPayload)` | `show(id, title, body, channel, priority, intent)` | ❌ Drift (No Payload object) |
| **ASR** | `Flow<Transcription>` | `suspend fun transcribe(File): AsrResult` | ❌ Drift (Sync instead of Flow) |
| **TingwuPipeline** | `submit(AudioUrl) -> PipelineResult` | `submit(TingwuRequest) -> Result<String>` | ❌ Drift |
| **PipelineTelemetry** | `recordEvent(TelemetryEvent)` | `recordEvent(PipelinePhase, String)` | ❌ Drift |

### Layer 2: Data Services
| Module | Spec Signature | Actual Code Signature | Verdict |
|--------|---------------|-----------------------|---------|
| **EntityWriter** | `upsertFromClue(ParsedClue) -> EntityId` | `upsertFromClue(clue: String...) -> UpsertResult` | ❌ Drift |
| **EntityRegistry** | `findByAlias(String) -> List<EntityInfo>` | `findByAlias(String) -> List<EntityEntry>` | ❌ Drift |
| **MemoryCenter** | `search(MemoryQuery)` | `search(query: String)` | ❌ Drift |
| **UserHabit** | `observe() -> Flow` | `suspend fun observe(...) -> Unit` | ❌ Drift |
| **SessionHistory** | `getGroupedSessions()` | `getGroupedSessionsFlow() -> Flow<Map>` | ❌ Drift |
| **SessionContext** | *Standalone Module* | Does not exist; merged inside `ContextBuilder.kt` | ❌ Drift (Missing module) |

### Layer 3: Core Pipeline
| Module | Spec Signature | Actual Code Signature | Verdict |
|--------|---------------|-----------------------|---------|
| **ContextBuilder** | `build(ContextRequest)` | `build(userText, mode, ids, depth)` | ❌ Drift |
| **InputParser** | `-> ParsedIntent` | `-> ParseResult` | ❌ Drift |
| **EntityDisambiguator**| `process(PendingIntent)` | `process(rawInput: String) -> DisambiguationResult`| ❌ Drift |
| **LightningRouter** | `-> IntentTier` | `-> RouterResult?` | ❌ Drift |
| **EntityResolver** | `resolve(Clues, Candidates)`| `resolve(query: String, candidates: List<EntityEntry>)`| ❌ Drift |
| **Executor** | `execute(Prompt, ModelProfile)`| `execute(LlmProfile, EnhancedContext) -> ExecutorResult`| ❌ Drift |
| **UnifiedPipeline** | `processInput(PipelineInput) -> Flow`| Exact match ✅ | ✅ Aligned |

### Layer 4 & 5
| Module | Spec Signature | Actual Code Signature | Verdict |
|--------|---------------|-----------------------|---------|
| **ConflictResolver** (L4) | `Flow<ConflictState>` | `suspend fun resolve(...) -> ConflictResolution` | ❌ Drift |
| **ClientProfileHub** (L5) | `-> ClientProfile` | `-> FocusedContext` | ❌ Drift |
| **RLModule** (L5) | `-> List<Habit>` | `-> HabitContext` | ❌ Drift |

---

## 🛠️ Required Remediation

1. **`interface-map.md` Update:**
   - Downgrade `RLModule` from ✅ to 📐.
   - Update the "Key Interface" column for *all* drifting modules to match the exact Kotlin function signatures.
   - Adjust `SessionContext` module existence.
   - Fix `NotificationService` Android import violation or document it as isolated tech debt.

2. **`tracker.md` Update:**
   - Add Tech Debt item for `NotificationService` Android import violation in Prism Domain layer.
   - Flag `RLModule` as `Phase 2 (Fakes)` pending real implementation.

## Next Steps
This report serves as the evidence artifact. Proceeding to `/04-doc-sync` to apply these realities to `interface-map.md` and `tracker.md`.
