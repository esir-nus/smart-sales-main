---
description: Clean environment for Prism development - prevents legacy contamination
trigger: always_on
---

# Prism Contamination Avoidance

## Golden Rule
> **Prism code must not import legacy code.**

---

## Module Boundaries

```
┌─────────────────────────────────────────────────┐
│  domain/   (Pure Kotlin - NO Android imports)   │
│  ─────────────────────────────────────────────  │
│  Allowed: kotlinx.*, javax.inject.*, dagger.*   │
│  Forbidden: android.*, feature.*, legacy.*      │
└─────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────┐
│  platform/  (Android-specific implementations)  │
│  ─────────────────────────────────────────────  │
│  Room DAOs, Retrofit clients, Bluetooth, etc.   │
└─────────────────────────────────────────────────┘
```

---

## Allowed Imports in `domain/`

| Package | Purpose |
|---------|---------|
| `kotlinx.coroutines.*` | Async primitives |
| `kotlinx.serialization.*` | JSON parsing |
| `javax.inject.*` | DI annotations |
| `dagger.hilt.*` | Hilt annotations |
| `domain.*` | Internal domain code |
| `core.*` | Shared core utilities |

---

## Forbidden Imports in `domain/`

| Package | Reason |
|---------|--------|
| `android.*` | Breaks testability (Lattice §7) |
| `feature.*` | Legacy package structure |
| `com.smartsales.legacy.*` | Old code patterns |
| `androidx.room.*` | Room is platform-specific |

---

## Verification Commands

```bash
# Run before any merge
grep -rn "android\." app/src/main/java/**/domain/ && echo "FAIL: Android in domain"
grep -rn "feature\." app/src/main/java/**/domain/ && echo "FAIL: Legacy in domain"
```

---

## Rewrite Rule

When implementing Prism code:

1. **Read legacy** to understand **WHAT** it does
2. **Read Prism spec** to understand the target contract
3. **Write fresh** following current architecture
4. **Delete old** entirely
5. **Verify** behavior

> Never copy-paste legacy code. Rewrite from spec.

---

## Legacy Reference Policy

| Okay to Reference | Not Okay to Import |
|-------------------|-------------------|
| Dictionary lookups | Direct class imports |
| Reading old tests | Copying test code |
| Understanding flows | Inheriting patterns |
