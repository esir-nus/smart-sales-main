> **OS Layer**: —

# Test Infrastructure Spec

## 1. Description
The `:core:test-fakes` module acts as the Single Source of Truth for Dual-Engine Data Fakes. It provides a standardized `PrismTestRig` to eliminate Mockito `whenever()` boilerplate in feature tests, specifically resolving Coroutine Scope crashes and Kotlin type-erasure issues that plagued UI component testing in Layer 4 assemblies. 

Testing infrastructure is treated as a first-class Cerb feature to enforce strict Anti-Drift and Anti-Illusion protocols.

## 2. Core Components
- **PrismTestRig**: A unified dependency injection container for tests that provides pre-wired Fakes. Replaces 40+ lines of Mockito initialization logic.
- **Fake Repositories**: Concrete, state-backed implementations of domain interfaces (`FakeHistoryRepository`, `FakeEntityRegistry`, `FakeMemoryRepository`, etc.).
- **WorldStateSeeder**: (Formerly AcceptanceFixtureBuilder) The centralized, deterministic factory for generating B2B "chaos data" (aliases, overlapping context, homophones). Used to "pre-seed" memory injections into Fakes to prove routing logic handles reality and to prevent the "Empty DB Hallucination" bug.

## 3. Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Core Fakes Migration | ✅ SHIPPED | Extract existing fragmented Fake classes out of `app-core` and centralize them into `:core:test-fakes`. |
| **2** | State Completeness | ✅ SHIPPED | Built missing Fakes (`FakeMemoryRepository`, `FakeEntityWriter`). Upgraded Fakes with `Mutex` locks to guarantee thread-safety during highly parallel L2 Simulation testing (Write-Back Concurrency limits). |
| **3** | The Test Rig | 🔲 PLANNED | Build `PrismTestRig`, handle `runTest` integration seamlessly, and roll out to support `MascotService` L2 simulation test. |
| **4** | Domain Fake Isolation | 🔲 PLANNED | Split `:core:test-fakes` into `:core:test-fakes-domain` (Pure JVM) and `:core:test-fakes-platform` (Android). Allows `:domain` and `:data` modules to consume domain fakes without Android classpath contamination. |
