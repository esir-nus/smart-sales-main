---
description: Comprehensive Fake I/O audit - checks interface coverage, fake implementations, UI purity, and test coupling
---

# Fake I/O Audit

**Purpose**: Verify that async/external behaviors follow the **Interface → Fake → DI** pattern, enabling one-binding swaps for real implementations.

---

## When to Invoke

- Before shipping any feature that touches external services (Bluetooth, Network, Storage)
- During `/prism-check` Gate 3 (Lattice)
- When reviewing implementation plans involving async behavior
- If you suspect "lazy works" (inline delays, hardcoded mocks)

---

## The Audit Protocol

### Step 1: Identify External Dependencies

List all external/async behaviors in the scope:

```markdown
| Dependency | Type | Interface Expected | Location |
|------------|------|-------------------|----------|
| Bluetooth Pairing | Hardware | `ConnectivityService` | `domain/` or `platform/` |
| Firmware Check | Network | `FirmwareRepository` | `domain/` |
| WiFi Config | Hardware | `WifiConfigService` | `platform/` |
```

// turbo
Run: `grep -rn "delay\|suspend fun" app-prism/src/main/java/` to identify async points.

---

### Step 2: Interface Existence Check

For each dependency, verify an interface exists:

// turbo
```bash
find app-prism/src/main/java -name "*Service.kt" -o -name "*Repository.kt" | head -20
```

**Scoring:**
| Found | Score |
|-------|-------|
| Interface exists | +20 |
| Interface missing | 0 |

---

### Step 3: Fake Implementation Check

For each interface, verify a Fake exists:

// turbo
```bash
find app-prism/src/main/java -name "Fake*.kt" | head -20
```

**Scoring:**
| Found | Score |
|-------|-------|
| Fake exists for interface | +20 |
| Fake missing | 0 |

---

### Step 4: UI Purity Check (Critical)

Verify NO simulation logic lives in UI layer:

// turbo
```bash
grep -rn "delay(" app-prism/src/main/java/**/ui/ 2>/dev/null || echo "Clean"
grep -rn "Thread.sleep" app-prism/src/main/java/**/ui/ 2>/dev/null || echo "Clean"
```

**Scoring:**
| Result | Score |
|--------|-------|
| No delays in UI | +30 |
| Delays found in UI | **-50** (Hard Fail) |

---

### Step 5: DI Binding Check

Verify Hilt module binds Interface → Fake:

// turbo
```bash
grep -rn "@Binds\|@Provides" app-prism/src/main/java/**/di/ | head -10
```

**Scoring:**
| Found | Score |
|-------|-------|
| Binding exists | +15 |
| Binding missing | 0 |

---

### Step 6: Test Coupling Check

Run tests that exercise the Fakes:

```bash
./gradlew :app-prism:testDebugUnitTest --tests "*Fake*" 2>&1 | tail -20
```

**Scoring:**
| Result | Score |
|--------|-------|
| Tests pass | +15 |
| Tests fail | 0 |
| No tests exist | -10 |

---

### Step 7: Blackbox Audit Check (Prism-Specific)

Verify all Prism blackboxes follow the **Interface → Fake → Real** pattern per [Prism-V1.md §2.2](file:///home/cslh-frank/main_app/docs/specs/Prism-V1.md).

**Canonical Blackbox List** (from spec):

| Category | Blackboxes | Interface Location |
|----------|-----------|-------------------|
| **A: Core Pipeline** | ContextBuilder, SessionCache, Orchestrator, Executor, Publisher, MemoryWriter, SchemaLinter, MemoryCenterNotifier | `domain/pipeline/` |
| **B: Input Tools** | TingwuRunner, VisionAnalyzer, UrlFetcher, BleConnector | `domain/inputs/` |
| **C: Linters** | EntityLinter, PlanLinter, SchedulerLinter, RelevancyLinter | `domain/linters/` |
| **D: Repositories** | RelevancyRepository, MemoryEntryRepository, UserProfileRepository, UserHabitRepository, ArtifactRepository, SessionsRepository, ScheduledTaskRepository, InspirationRepository | `domain/repositories/` |
| **E: FSM Controllers** | AnalystFlowController, CoachFlowController, SchedulerFlowController, SystemNotificationController | `domain/pipeline/` |

// turbo
**Check Interface Existence**:
```bash
for iface in Executor ContextBuilder Publisher MemoryWriter Orchestrator; do
  find app-prism/src/main/java -name "${iface}.kt" | head -1 || echo "❌ Missing: ${iface}"
done
```

// turbo
**Check Fake Existence**:
```bash
find app-prism/src/main/java -path "*/fakes/*" -name "*.kt" | wc -l
```

// turbo
**Check Real Existence** (Phase 3):
```bash
find app-prism/src/main/java -path "*/real/*" -name "*.kt" 2>/dev/null | wc -l || echo "0 (Phase 2)"
```

**Blackbox Status Table**:

Generate this table for each blackbox:

| Blackbox | Interface | Fake | Real | DI Binding | Status |
|----------|-----------|------|------|------------|--------|
| `Executor` | ✅ | ✅ `FakeExecutor` | ❌ | ✅ `PrismModule:52` | Phase 2 |
| `ContextBuilder` | ✅ | ✅ `FakeContextBuilder` | ❌ | ✅ | Phase 2 |
| ... | ... | ... | ... | ... | ... |

**Scoring:**
| Result | Score |
|--------|-------|
| All interfaces exist | +10 |
| All have Fakes | +10 |
| Fakes bound via DI | +10 |
| Missing any interface | **-30** (Hard Fail) |

---

## Output: Audit Report

Generate this report:

```markdown
# 🔌 Fake I/O Audit Report

## Scope
- **Feature**: [Feature Name]
- **Dependencies Identified**: [Count]

## Checklist

| Check | Status | Score |
|-------|--------|-------|
| Interfaces Defined | ✅/❌ | +20 |
| Fakes Implemented | ✅/❌ | +20 |
| UI Purity | ✅/❌ | +30 |
| DI Bindings | ✅/❌ | +15 |
| Tests Coupled | ✅/❌ | +15 |
| **Blackbox Audit** | ✅/❌ | +30 |
| **Total** | | **/130** |

## Verdict

| Score | Action |
|-------|--------|
| **115-130** | ✅ SHIP — Ready for real implementation swap |
| **90-114** | ⚠️ PROCEED — Minor gaps, log as debt |
| **65-89** | 🔧 FIX — Create missing Fakes/Bindings |
| **<65** | 🔴 BLOCK — UI contaminated or no abstraction |

## Next Steps
1. [Specific action based on gaps]
2. [Specific action based on gaps]
```

---

## Quick Reference: The Pattern

```
┌─────────────────────────────────────────────────────────────┐
│  UI Layer (Composables)                                     │
│  ───────────────────────────────────────────────────────── │
│  ❌ NO: delay(), mock data, hardcoded responses             │
│  ✅ YES: Call injected service interface                    │
└─────────────────────────────────────────────────────────────┘
                        │
                        │ @Inject
                        ▼
┌─────────────────────────────────────────────────────────────┐
│  Interface (domain/ or platform/)                           │
│  ───────────────────────────────────────────────────────── │
│  interface ConnectivityService {                            │
│      suspend fun checkForUpdate(): UpdateResult             │
│      suspend fun reconnect(): ReconnectResult               │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
                        │
                        │ @Binds (Hilt Module)
                        ▼
┌─────────────────────────────────────────────────────────────┐
│  Fake Implementation (platform/fake/)                       │
│  ───────────────────────────────────────────────────────── │
│  class FakeConnectivityService : ConnectivityService {      │
│      override suspend fun checkForUpdate(): UpdateResult {  │
│          delay(1500)  // ✅ Delay lives HERE                │
│          return UpdateResult.Found("v1.3")                  │
│      }                                                      │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
```

---

## Troubleshooting

**Q: "The feature is simple, do I really need an interface?"**
**A: YES.** If it's async or external, it needs an interface. No exceptions.

**Q: "Can the Fake live in the same file as the interface?"**
**A: For small interfaces, yes.** But prefer `fake/` directory for discoverability.

**Q: "What if real implementation doesn't exist yet?"**
**A: That's the point.** Fake-first development. Real impl swaps in later via DI binding.
