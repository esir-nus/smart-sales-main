<!-- File path: docs/specs/Orchestrator-Lattice.md -->
<!-- Purpose: Lattice architecture spec — modular Box-API system -->

# Orchestrator-Lattice Architecture Spec

Version: 1.0.0  
Codename: **Lattice**  
Status: CURRENT  
V1 Status: DEPRECATED (use V1 Appendices for algorithm specs only)

---

## 0. Vision

Lattice replaces the monolithic god-object architecture with a **modular Box-API system**:
- **Boxes**: Self-contained modules with one interface, own DTOs, and a Fake for testing
- **Orchestrators**: Logic-free wiring that passes DTOs between boxes
- **Result**: High testability, clear boundaries, AI-friendly code locality

---

## 1. Core Invariants

### 1.1 Box Isolation
- Boxes never call each other directly
- All communication goes through an Orchestrator
- Each box owns its input/output DTOs (no leaking internal types)

### 1.2 Thin Orchestrator Principle
- Orchestrators are **logic-free** — they only wire boxes together
- No business logic inside orchestrators
- Sequential wiring: pass output of Box A as input to Box B

### 1.3 Fake per Interface
- Every box interface has a corresponding `Fake*` implementation
- Fakes live in the same file or `:test` source set
- Enables fast JVM tests without network/hardware dependencies

### 1.4 Inherited from V1
- **Publisher owns truth**: UI renders only Publisher output, never raw vendor JSON
- **LLM cannot mutate transcript**: LLM Parser writes metadata, not transcript truth
- **Determinism**: Disector, Publisher algorithms are deterministic and reproducible

---

## 2. The Canonical Pattern

**Reference implementation**: [`AiChatService.kt`](file:///home/cslh-frank/main_app/data/ai-core/src/main/java/com/smartsales/data/aicore/AiChatService.kt)

```kotlin
// 1. DTOs defined in same file
data class BoxInput(...)
data class BoxOutput(...)

// 2. Interface with suspend functions
interface BoxService {
    suspend fun process(input: BoxInput): Result<BoxOutput>
}

// 3. Fake implementation for testing
class FakeBoxService : BoxService {
    override suspend fun process(input: BoxInput): Result<BoxOutput> = ...
}

// 4. Real implementation in separate file
class RealBoxService @Inject constructor(...) : BoxService { ... }
```

**Rules**:
- DTOs + Interface + Fake in **one file** (high locality)
- Real implementation in **separate file** (keeps interface file clean)
- Use `Result<T>` for error handling (not exceptions)

---

## 3. The Four Layers

```
┌─────────────────────────────────────────────────────────────────┐
│  CONNECTIVITY LAYER                                              │
│  BleService · WiFiService · BadgeSyncService · AudioTransfer     │
└─────────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  PIPELINE LAYER                                                  │
│  AudioPreparer · Transcription · MetadataExtractor · Publisher   │
│                    ▲ TranscriptionOrchestrator ▲                 │
└─────────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  MEMORY LAYER                                                    │
│  SessionMemory · LongTermMemory · KnowledgeBase · ContextRetriever│
└─────────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  CHATTER LAYER                                                   │
│  PromptBuilder · LLMClient · ResponseParser                      │
│                    ▲ ChatterOrchestrator ▲                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. Module Registry

> Extraction progress tracked in [`tracker.md §7.1`](../plans/tracker.md#71-lattice-module-extraction-status)

### 4.1 Chatter Layer (Existing ✅)

| Module | Interface | Status |
|--------|-----------|--------|
| AiChatService | [`AiChatService.kt`](file:///home/cslh-frank/main_app/data/ai-core/src/main/java/com/smartsales/data/aicore/AiChatService.kt) | ✅ Extracted |

### 4.2 Pipeline Layer (In Progress 🔄)

| Module | Interface | Status |
|--------|-----------|--------|
| AudioPreparerService | — | 🔲 Planned |
| TranscriptionService | — | 🔲 Planned |
| MetadataExtractorService | — | 🔲 Planned |
| PublisherService | — | 🔲 Planned |

### 4.3 Memory Layer (Planned 📋)

| Module | Interface | Status |
|--------|-----------|--------|
| SessionMemoryService | — | 🔲 Planned |
| LongTermMemoryService | — | 🔲 Planned |
| KnowledgeBaseService | — | 🔲 Planned |

### 4.4 Connectivity Layer (To Formalize 📐)

| Module | Interface | Status |
|--------|-----------|--------|
| BleService | — | 🔲 To formalize |
| WiFiService | — | 🔲 To formalize |
| BadgeSyncService | — | 🔲 To formalize |
| AudioTransferService | — | 🔲 To formalize |

---

## 5. Testing Strategy

| Level | Scope | Speed | When |
|-------|-------|-------|------|
| **Unit** | Single box with mocked inputs | ⚡ Fast | Every change |
| **Integration** | Orchestrator + Fake boxes | ⚡ Fast | Feature complete |
| **E2E** | Full stack on device | 🐢 Slow | Final validation |

### 5.1 Test Requirements per Box

Every extracted box MUST have:

1. **`Fake*Test`** — Verifies Fake behavior is useful for testing
   - Example: [`FakeAiChatServiceTest.kt`](file:///home/cslh-frank/main_app/data/ai-core/src/test/java/com/smartsales/data/aicore/FakeAiChatServiceTest.kt)
   
2. **Unit tests for Real impl** — Verifies business logic
   - Mock dependencies, test edge cases
   
3. **Integration test (optional)** — Orchestrator + Fakes
   - Only when wiring is complex

---

## 6. Extraction Protocol

### 6.1 Pre-Extraction Audit

Before extracting any box, complete this checklist:

```markdown
## Extraction Audit: [BoxName]

### 1. Locate Existing Logic
- [ ] `grep -n "[keyword]" [god_object].kt`
- [ ] `view_file_outline` on target file
- [ ] List functions that belong to this box

### 2. Assess Code Quality
- [ ] Works in production? [YES/NO]
- [ ] Has unit tests? [YES/NO → list them]
- [ ] Known bugs? [LIST]

### 3. Assess Coupling
- [ ] `grep -rn "import.*[ClassName]"` — count importers
- [ ] Shared mutable state with other modules? [YES/NO]
- [ ] Dependencies to inject? [LIST]

### 4. Decision
Choose ONE:
- [ ] **EXTRACT** — wrap existing logic with interface
- [ ] **TRANSPLANT** — move to new location + wrap
- [ ] **INTERFACE ONLY** — logic already isolated, just add interface
- [ ] **REWRITE** — logic broken or misaligned

### 5. Deliverables
- [ ] Interface + DTOs in `[BoxName]Service.kt`
- [ ] Fake in same file
- [ ] Real impl (or delegate to existing)
- [ ] Hilt binding
- [ ] `Fake*Test` passing
- [ ] Update `lattice-interfaces.md`
- [ ] Update `tracker.md §7.1` checkbox
```

### 6.2 Extraction Flow

```
1. AUDIT    → Complete §6.1 checklist
2. EXTRACT  → Create interface + DTOs + Fake
3. WIRE     → Inject via Hilt, god object delegates
4. TEST     → Fake*Test + Unit tests
5. DOCUMENT → Update lattice-interfaces.md
6. TRACK    → Update tracker.md §7.1
```

### 6.3 Strangler Fig Rules

- **Co-existence**: God object delegates to extracted boxes during migration
- **No breakage**: All existing tests must pass after extraction
- **One box at a time**: Complete one before starting next
- **Decommission trigger**: All boxes extracted → god object is empty shell → delete

---

## Appendix: V1 Algorithm References

These algorithm specs remain valid under Lattice. Do not duplicate — reference only.

| Algorithm | V1 Section | Purpose |
|-----------|------------|---------|
| Disector batch split | [V1 Appendix A](./Orchestrator-V1.md#11-appendix-a-disector-rules-batch-split-and-micro-overlap-deterministic) | Batch planning rules |
| TranscriptPublisher | [V1 Appendix B](./Orchestrator-V1.md#12-appendix-b-transcriptpublisher-deterministic-publish-and-de-dup) | Overlap de-dup, prefix publish |
| Time invariants | [V1 Appendix C](./Orchestrator-V1.md#13-appendix-c-time-and-order-invariants) | Timestamp semantics |
| State recovery | [V1 Appendix D](./Orchestrator-V1.md#14-appendix-d-state-recovery-rules-restartresume) | Restart/resume rules |
