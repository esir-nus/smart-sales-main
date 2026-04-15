# Strict Interface Integrity Spec

> **OS Layer**: L2 Testing Environment (RAM + App integration)
> **State**: SHIPPED

## Execution Scenarios

### 1. The Semantic Ambiguity Intercept
- **Inject**: Upstream input `Update the order quantity`
- **State**: `FakeInputParserService` configured to return `ParseResult.NeedsClarification` (simulating the LLM failing to resolve "which order").
- **Expect**: Pipeline natively suspends and emits `PipelineResult.DisambiguationIntercepted`. The UI state enclosed should directly contain the clarification question. SSD writes are ZERO.

### 2. The Linter Semantic Recovery (Rejection without Crash)
- **Inject**: A task intent that has structurally valid JSON, but the content makes no logical scheduling sense (e.g. `durationMinutes = -100` or missing fields).
- **State**: `SchedulerLinter` is passed the LLM content `ExecutorResult.Success`.
- **Expect**: Linter natively returns `LintResult.Error`, which the Pipeline then wraps into a safe `PipelineResult.ConversationalReply` containing the rejection message. The app does not crash, and the Fake Task Repository records ZERO writes.

### 3. The Unresolved Context Pass-Through
- **Inject**: Explicit Disambiguation resolution fails organically.
- **State**: `FakeEntityDisambiguationService` returns `DisambiguationResult.Resumed` with original input.
- **Expect**: Pipeline falls back to parsing natively, demonstrating that an abandoned clarification loop safely resumes the normal unified pipeline ETL.

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Mock Eviction Structure | ✅ SHIPPED | Setup tear-down, fakes injected |
| **2** | Scenario Writing | ✅ SHIPPED | All Context Branches written |
