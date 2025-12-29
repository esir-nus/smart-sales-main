# AI Agent Documentation System Audit Report

**Date**: 2025-01-27  
**Auditor**: Senior CS Engineer / Software Architect  
**Scope**: Documentation system evaluation for AI coding agent consumption  
**Target**: V1 CURRENT architecture; V7 ARCHIVED status verification

---

## 1) Executive Verdict

**VERDICT: Pass with Fixes**

### Summary Bullets

- ✅ **Strong foundation**: V1 spec is well-structured, contract-driven, and clearly marks V7 as ARCHIVED
- ✅ **Schema-examples alignment**: JSON schema and examples are consistent; required fields match
- ✅ **Invariant clarity**: "Publisher owns truth" and "LLM Parser cannot mutate transcript truth" are clearly stated
- ⚠️ **Versioning ambiguity**: `schemaVersion` vs `version` dual usage creates confusion; needs explicit policy
- ⚠️ **ID glossary missing**: Identifiers (`chatSessionId`, `turnId`, `audioAssetId`, etc.) lack centralized glossary/definitions
- ⚠️ **Retry semantics incomplete**: Chat retry policy is clear, but Tingwu retry policy lacks concrete terminal behaviors
- ⚠️ **V7 leakage**: Minor references to V7 in `style-guide.md` and `T-Task.md` that could confuse agents
- ⚠️ **Contract gaps**: Some behavioral details (e.g., `publishedPrefixBatchIndex` advancement rules) are underspecified
- ✅ **Examples coverage**: Good coverage of edge cases (26m merge, 27m edge, range filtering)
- ⚠️ **Metadata Hub structure**: M2B source pointers requirement is stated but not enforced in schema

---

## 2) Contract Integrity Audit

### 2.1 Contradictions Between Orchestrator-V1.md and api-contracts/ux-contract

**Finding**: No major contradictions found. Minor inconsistencies:

1. **HUD Section naming**:
   - `Orchestrator-V1.md` §9.1: "Effective Run Snapshot" / "Raw Output" / "Publisher-ready / Published Snapshot"
   - `api-contracts.md` §4.1: Same names ✓
   - `ux-contract.md` §5: Same names ✓
   - **Status**: Consistent ✓

2. **Chat retry policy**:
   - `Orchestrator-V1.md` §8: `maxRetries = 2` (suggested default)
   - `api-contracts.md` §5.2: `maxRetries = 2` (default suggestion)
   - **Status**: Consistent ✓

3. **Tingwu retry policy**:
   - `Orchestrator-V1.md`: Mentions retry but no concrete policy
   - `api-contracts.md` §5.1: 429 retry with longer backoff; 4xx (except 429) non-retryable; 5xx/timeout retryable
   - **Status**: No contradiction, but V1 spec should reference api-contracts for Tingwu retry details

### 2.2 Schema vs Examples Mismatches

**Finding**: Schema and examples are well-aligned. Minor issues:

1. **Missing required fields in examples**:
   - `M2BTranscriptionState` example (line 460-502) uses `chapters` and `keyPoints` fields not defined in schema
   - Schema defines `items` and `sourcePointers` but example uses different structure
   - **Impact**: P1 - Agents may generate invalid M2B structures

2. **`sessionId` deprecation**:
   - Schema marks `sessionId` as DEPRECATED alias of `recordingSessionId`
   - Examples still use `sessionId` in many places (e.g., DisectorPlan examples)
   - **Impact**: P2 - Confusing but not breaking

3. **`version` vs `schemaVersion`**:
   - `MachineArtifact` requires `schemaVersion` (const: 1)
   - `SessionMemory` requires both `version` (const: 1) and `schemaVersion` (const: 1)
   - Examples match, but dual usage is confusing
   - **Impact**: P1 - Agents may use wrong field name

### 2.3 Invariant Verification

**"Publisher owns truth"**:
- ✅ Stated in `Orchestrator-V1.md` §2.1
- ✅ Reinforced in `api-contracts.md` §2.2 (UI only renders Publisher output)
- ✅ Reinforced in `ux-contract.md` §1.1 (UI only renders `displayMarkdown`)
- **Status**: Consistently stated ✓

**"LLM Parser cannot mutate transcript truth"**:
- ✅ Stated in `Orchestrator-V1.md` §2.2
- ✅ Reinforced in `Orchestrator-V1.md` §3.1 (LLM Parser constraint)
- ⚠️ Not explicitly stated in `api-contracts.md` or `ux-contract.md`
- **Impact**: P2 - Should be mentioned in contracts for completeness

---

## 3) ID & Lifecycle Audit

### 3.1 Identifier Definitions

**Finding**: Identifiers are defined but scattered; no centralized glossary.

| Identifier | Definition Location | Consistency | Issues |
|------------|---------------------|-------------|--------|
| `chatSessionId` | Schema: `ChatSessionId` def (line 851-855) | ✅ Consistent | None |
| `turnId` | Schema: `TurnId` def (line 856-860) | ✅ Consistent | None |
| `audioAssetId` | Schema: `AudioAssetId` def (line 831-835); V1.md §6.1 | ✅ Consistent | None |
| `recordingSessionId` | Schema: `RecordingSessionId` def (line 836-840); V1.md §6.1 | ✅ Consistent | ⚠️ `sessionId` deprecated alias still in examples |
| `disectorPlanId` | Schema: `DisectorPlanId` def (line 841-845); V1.md §6.1 | ✅ Consistent | None |
| `batchAssetId` | Schema: `BatchAssetId` def (line 846-850); V1.md §6.1 | ✅ Consistent | None |
| `batchIndex` | Schema: `BatchIndex` def (line 155-159); V1.md §12 | ✅ Consistent | ✅ Explicitly marked as ordering primitive |

**Missing Glossary**: No single place where all identifiers are defined with their lifecycle rules.

### 3.2 Lifecycle Rules

**Finding**: Lifecycle rules are partially specified:

1. **`audioAssetId` lifecycle**:
   - ✅ Defined: Content identity for deduplication/caching
   - ✅ Constraint: "audioAssetId 不应由 Disector 决定" (V1.md §6.1)
   - ⚠️ Missing: Who generates it? When? Format constraints?

2. **`recordingSessionId` lifecycle**:
   - ✅ Defined: Processing instance ID (may be new even if audioAssetId cached)
   - ⚠️ Missing: Generation rules, uniqueness guarantees, format

3. **`publishedPrefixBatchIndex` lifecycle**:
   - ✅ Defined: Monotonic, advances only for contiguous batches (V1.md §B.2)
   - ✅ Schema: `PublisherState` requires it (line 651)
   - ⚠️ Missing: What happens on restart? How is it recovered?

4. **`batchIndex` lifecycle**:
   - ✅ Defined: Ordering primitive (integer, minimum 1)
   - ✅ Constraint: Must use numeric sort, not batchId string sort
   - ✅ Examples: Consistent (1, 2, 3...)
   - **Status**: Well-defined ✓

### 3.3 Naming Inconsistencies

**Finding**: Minor inconsistencies:

1. **`sessionId` deprecation**: Still used in examples; should be phased out
2. **`chapterId`**: Used in examples (e.g., `M2BTranscriptionState` line 466) but not defined in schema
3. **`jobId`**: Used in `TingwuBatchArtifact` (required field) but lifecycle not specified

---

## 4) Failure and Retry Semantics

### 4.1 Chat Retry Policy

**Status**: ✅ Well-specified

- **Trigger**: MachineArtifact cannot be parsed/validated
- **Strategy**: "策略 B" (maxRetries = 2, suggested default)
- **Scope**: Retry only structured part; HumanDraft can be reused/rewritten
- **Terminal behavior**: 
  - If retries exhausted: `artifactStatus = FAILED`
  - Allow displaying last HumanDraft if extractable
  - Prohibit writing to Metadata Hub
  - Must write Trace event
- **Location**: `Orchestrator-V1.md` §8, `api-contracts.md` §5.2

**Gap**: No explicit backoff strategy mentioned (though not critical for Chat)

### 4.2 Tingwu Retry Policy

**Status**: ⚠️ Partially specified

- **Location**: `api-contracts.md` §5.1
- **Rules**:
  - 429: Retryable with longer backoff
  - Other 4xx (except 429): Non-retryable by default
  - 5xx/network timeout: Retryable with deterministic backoff
- **Schema support**: `TingwuJobState` has `retryBackoffSeconds` array (line 329-335)
- **Gaps**:
  - No explicit max retry count
  - No terminal behavior specification (what happens after all retries?)
  - No explicit backoff formula (though schema suggests array)

**Recommendation**: Add to `Orchestrator-V1.md` §8 or create new section for Tingwu retry policy.

### 4.3 Terminal Behaviors

**Finding**: Chat terminal behavior is clear; Tingwu terminal behavior is underspecified.

**Missing**:
- What happens when Tingwu batch fails after all retries?
- Should `PublishedTranscript` include partial results?
- How does `publishedPrefixBatchIndex` advance if a batch fails permanently?

---

## 5) Versioning and Evolution

### 5.1 `schemaVersion` vs `version` Analysis

**Finding**: Dual usage creates confusion.

**Current Usage**:

1. **`schemaVersion`** (integer, const: 1):
   - `MachineArtifact` (required)
   - `M1UserMetadata` (required)
   - `M2ConversationState` (required)
   - `M2BTranscriptionState` (required)
   - `M3SessionState` (required)
   - `M4ExternalKnowledge` (required)
   - `SessionMemory` (required, alongside `version`)

2. **`version`** (integer, const: 1):
   - `DisectorPlan` (required)
   - `TingwuJobState` (required)
   - `TingwuBatchArtifact` (required)
   - `PublisherState` (required)
   - `PublishedTranscript` (required)
   - `PublishedAnalysis` (required)
   - `PublishedChatTurn` (required)
   - `SessionMemory` (required, alongside `schemaVersion`)

**Schema comment** (line 6): Mentions "SchemaVersion: 1.1.0" but schema uses integer 1.

**V1.md guidance** (line 128): "每个 JSON 顶层带 `schemaVersion`（整数），仅在 breaking change 时递增"

**Contradiction**: V1.md recommends `schemaVersion` for all JSON, but schema uses `version` for many artifacts.

### 5.2 Versioning Policy Recommendation

**Recommendation**: Unify on `schemaVersion` for all artifacts, OR document the distinction:

- **Option A** (Preferred): Use `schemaVersion` for all artifacts. Update schema to replace `version` with `schemaVersion`.
- **Option B**: Document that `version` is artifact-specific versioning (e.g., DisectorPlan version tied to rule version), while `schemaVersion` is JSON schema versioning.

**Current state**: `SessionMemory` uses both, which is confusing.

### 5.3 Evolution Strategy

**Finding**: No explicit evolution/migration policy.

**Missing**:
- How to handle breaking changes?
- Migration path from V1.1.0 to future versions?
- Backward compatibility guarantees?

**Recommendation**: Add a "Versioning Policy" section to `Orchestrator-V1.md` or create `docs/versioning-policy.md`.

---

## 6) AI-Agent Friendliness Scorecard

### 6.1 Clarity of SoT & Precedence Rules

**Score: 9/10**

- ✅ V1.md clearly states it is "唯一权威 / AI coding SoT"
- ✅ V7 is clearly marked ARCHIVED
- ✅ `AGENTS.md` has precedence rules
- ⚠️ Minor: `style-guide.md` references V4 (archived) in checklist

**Improvement**: Remove V4 reference from `style-guide.md` §9.

### 6.2 Determinism / Invariants

**Score: 8/10**

- ✅ Core invariants clearly stated (§2)
- ✅ Disector rules are deterministic with examples (§A)
- ✅ Publisher rules are deterministic (§B)
- ⚠️ Some behavioral details underspecified (e.g., `publishedPrefixBatchIndex` recovery)

**Improvement**: Add explicit recovery rules for stateful fields.

### 6.3 Contract Completeness

**Score: 7/10**

- ✅ Core contracts are well-defined
- ✅ Schema enforces types and required fields
- ⚠️ Missing: Tingwu retry terminal behavior
- ⚠️ Missing: State recovery rules
- ⚠️ Missing: Error code mappings

**Improvement**: Add missing behavioral specifications.

### 6.4 Examples Coverage

**Score: 9/10**

- ✅ Good coverage: short audio, 26m merge, 27m edge, range filtering
- ✅ Examples match schema
- ⚠️ Missing: M2B example structure mismatch (see §2.2)

**Improvement**: Fix M2B example to match schema.

### 6.5 Naming Consistency

**Score: 8/10**

- ✅ Identifiers are consistently used
- ✅ Schema definitions are clear
- ⚠️ Missing: Centralized glossary
- ⚠️ `sessionId` deprecation still in examples

**Improvement**: Add glossary; phase out `sessionId` from examples.

### 6.6 Extensibility Hooks vs Scope Control

**Score: 8/10**

- ✅ Future extensions clearly marked (§13)
- ✅ `additionalProperties: true` used appropriately
- ✅ Non-goals clearly stated (§0.2)
- ⚠️ Some extension points underspecified (e.g., M4)

**Improvement**: Add explicit extension guidelines.

**Overall Score: 8.2/10** (Good, with room for improvement)

---

## 7) Prioritized Fix List

### P0 (Must Fix Now)

#### P0-1: Fix M2B Example Structure Mismatch
- **File**: `docs/orchestrator-v1.examples.json`
- **Location**: Lines 460-502 (`M2BTranscriptionState` example)
- **Issue**: Example uses `chapters` and `keyPoints` fields not in schema
- **Fix**: Update example to use `items` and `sourcePointers` as per schema, OR update schema to match example (if example is correct)
- **Suggested wording**: Replace `chapters` array with `items` array; replace `keyPoints` with entries in `items` that have `sourcePointers`

#### P0-2: Unify Versioning Fields
- **File**: `docs/orchestrator-v1.schema.json`
- **Location**: Multiple artifacts using `version` instead of `schemaVersion`
- **Issue**: Dual usage of `schemaVersion` and `version` creates confusion
- **Fix**: Replace all `version` fields with `schemaVersion` in artifacts (DisectorPlan, TingwuJobState, TingwuBatchArtifact, PublisherState, PublishedTranscript, PublishedAnalysis, PublishedChatTurn), OR document the distinction clearly
- **Suggested wording**: Add comment to schema: "All artifacts use `schemaVersion` (integer) for JSON schema versioning. Increment only on breaking changes. Artifact-specific versioning (e.g., DisectorPlan rule version) is tracked separately via `disectorPlanId`."

#### P0-3: Add Tingwu Retry Terminal Behavior
- **File**: `docs/Orchestrator-V1.md`
- **Location**: After §8 (add new §8.1 or extend §8)
- **Issue**: Tingwu retry policy lacks terminal behavior specification
- **Fix**: Add explicit terminal behavior: "After all retries exhausted: mark batch as FAILED, do not advance `publishedPrefixBatchIndex`, allow partial transcript display if available, write Trace event"
- **Suggested wording**: 
```markdown
### 8.1 Tingwu Batch Retry Policy

When a Tingwu batch fails:
- Retry rules: See `api-contracts.md` §5.1 (429 retryable with backoff; 4xx non-retryable except 429; 5xx/timeout retryable)
- Terminal behavior (after all retries exhausted):
  - Mark `TingwuJobState.status = FAILED`
  - Do not advance `publishedPrefixBatchIndex` (prefix remains at last successful batch)
  - Allow displaying partial `PublishedTranscript` if available
  - Write Trace event with failure reason
  - UI may show error message but must not display incomplete batches as complete
```

### P1 (Should Fix Soon)

#### P1-1: Add Identifier Glossary
- **File**: `docs/Orchestrator-V1.md`
- **Location**: Add new §6.0 before §6.1
- **Issue**: Identifiers are defined but scattered
- **Fix**: Add centralized glossary table
- **Suggested wording**:
```markdown
### 6.0 Identifier Glossary

| Identifier | Type | Lifecycle | Purpose | Generated By |
|------------|------|-----------|---------|-------------|
| `audioAssetId` | string | Immutable | Content identity for deduplication/caching | Audio upload system (before Disector) |
| `recordingSessionId` | string | Per-processing-instance | Processing instance ID (may be new even if audioAssetId cached) | Orchestrator on transcription start |
| `disectorPlanId` | string | Per-plan-version | Plan identity (bound to rule version) | Disector |
| `batchAssetId` | string | Per-batch | Stable batch identity from DisectorPlan | Disector (derived from plan + batchIndex) |
| `batchIndex` | integer | Per-batch | Ordering primitive (1-based, numeric sort only) | Disector |
| `chatSessionId` | string | Per-chat-session | Chat session identity | Chat system |
| `turnId` | string | Per-turn | Unique turn/message ID | Chat system |
| `publishedPrefixBatchIndex` | integer | Monotonic | Last published batch index (prefix boundary) | TranscriptPublisher |
```

#### P1-2: Document State Recovery Rules
- **File**: `docs/Orchestrator-V1.md`
- **Location**: Add to §11 (Appendix C) or new §11.1
- **Issue**: Recovery rules for stateful fields are missing
- **Fix**: Add explicit recovery rules
- **Suggested wording**:
```markdown
### 11.1 State Recovery Rules

On app restart or crash recovery:
- `publishedPrefixBatchIndex`: Recover from `PublisherState` persisted state. If missing, recompute from `PublishedTranscript.items` (max `batchIndex`).
- `TingwuJobState`: Recover from persisted job states. Missing states imply batch not started.
- `SessionMemory`: Recover from persisted M2B. Missing implies no parsing completed.
- `DisectorPlan`: Recompute deterministically from `audioAssetId` + `totalMs` + current rule version.
```

#### P1-3: Remove V7 References from Non-Archived Docs
- **File**: `docs/style-guide.md`
- **Location**: Line 428
- **Issue**: References V4 (archived) in checklist
- **Fix**: Remove V4 reference or update to V1
- **Suggested wording**: Change "`docs/ux-contract.md` 和 `docs/archived/Orchestrator-MetadataHub-V4.md`" to "`docs/ux-contract.md` 和 `docs/Orchestrator-V1.md`"

#### P1-4: Add LLM Parser Constraint to Contracts
- **File**: `docs/api-contracts.md`
- **Location**: Add to §2 (Chat 事件流) or new §2.3
- **Issue**: "LLM Parser cannot mutate transcript truth" not in contracts
- **Fix**: Add explicit constraint
- **Suggested wording**: Add bullet: "LLM Parser may only write to Metadata Hub (M2/M2B/M3); it must not modify PublishedTranscript truth. Transcript modifications are Publisher-only."

### P2 (Nice to Have)

#### P2-1: Phase Out `sessionId` from Examples
- **File**: `docs/orchestrator-v1.examples.json`
- **Location**: All DisectorPlan, TingwuJobState, TingwuBatchArtifact examples
- **Issue**: `sessionId` is deprecated but still in examples
- **Fix**: Replace `sessionId` with `recordingSessionId` in all examples
- **Note**: Low priority as schema marks it deprecated, but examples should match best practice

#### P2-2: Add Versioning Policy Document
- **File**: Create `docs/versioning-policy.md`
- **Location**: New file
- **Issue**: No explicit versioning/migration policy
- **Fix**: Create policy document
- **Suggested content**:
```markdown
# Versioning Policy (Orchestrator-V1)

## Schema Versioning

- All artifacts use `schemaVersion` (integer)
- Increment only on breaking changes (field removal, type change, required field addition)
- Non-breaking changes (optional field addition, description updates) do not increment version

## Migration Strategy

- V1.1.0 → V1.2.0: Add migration guide in release notes
- Breaking changes: Provide migration scripts/examples
- Backward compatibility: V1.x artifacts must be readable by V1.x+ parsers (forward compatibility not required)

## Artifact-Specific Versioning

- `disectorPlanId` encodes rule version (e.g., "plan-demo-001-v1")
- Rule version changes trigger new `disectorPlanId` even if schema version unchanged
```

#### P2-3: Add Error Code Mapping Table
- **File**: `docs/api-contracts.md`
- **Location**: Add to §5 (错误语义)
- **Issue**: Error codes are mentioned but not mapped
- **Fix**: Add mapping table
- **Suggested wording**: Add table mapping HTTP status codes to retryability and user-facing messages

---

## 8) Optional: Docs Lint Checklist

Create `docs/docs-lint-checklist.md`:

```markdown
# Documentation Lint Checklist (AI Agent Pre-Coding)

Before implementing any feature based on `docs/Orchestrator-V1.md`, verify:

## 1. Source of Truth
- [ ] Read `docs/Orchestrator-V1.md` as primary spec
- [ ] Check `docs/AGENTS.md` for precedence rules
- [ ] Verify no V7 references (V7 is ARCHIVED)
- [ ] Confirm `docs/orchestrator-v1.schema.json` matches spec

## 2. Identifiers
- [ ] All identifiers match glossary in V1.md §6.0 (or schema definitions)
- [ ] No `sessionId` usage (use `recordingSessionId`)
- [ ] `batchIndex` is integer, not string
- [ ] Identifiers follow lifecycle rules (immutable vs mutable)

## 3. Invariants
- [ ] "Publisher owns truth" enforced (UI only renders Publisher output)
- [ ] "LLM Parser cannot mutate transcript truth" enforced (Parser only writes Metadata Hub)
- [ ] Deterministic rules followed (Disector, Publisher)
- [ ] No breaking of half-open intervals `[absStartMs, absEndMs)`

## 4. Contracts
- [ ] Chat: Dual-channel output (`<visible2user>` + MachineArtifact)
- [ ] Chat: Retry policy (`maxRetries=2`, terminal: `FAILED` + no Metadata Hub write)
- [ ] Transcription: Prefix-only publishing (no out-of-order batches)
- [ ] Transcription: Range filtering for overlap (pre-roll only)

## 5. Schema Compliance
- [ ] All required fields present
- [ ] Types match schema (integer vs string, etc.)
- [ ] `schemaVersion` used (not `version` unless documented exception)
- [ ] Examples match schema structure

## 6. Versioning
- [ ] Breaking changes increment `schemaVersion`
- [ ] Non-breaking changes (optional fields) do not increment version
- [ ] Migration path considered if breaking

## 7. Error Handling
- [ ] Terminal behaviors specified (what happens after retries exhausted?)
- [ ] State recovery rules followed (on restart/crash)
- [ ] Trace events written for failures

## 8. Testing
- [ ] Edge cases covered (26m merge, 27m edge, range filtering)
- [ ] Examples validate against schema
- [ ] Deterministic behavior testable (Disector, Publisher)

---

**If any item fails, stop and clarify with Operator/Orchestrator before coding.**
```

---

## Conclusion

The documentation system is **well-structured and mostly consistent**, with clear V1 CURRENT vs V7 ARCHIVED separation. The main issues are:

1. **Versioning confusion** (P0): Dual `schemaVersion`/`version` usage needs unification
2. **Example-schema mismatch** (P0): M2B example structure doesn't match schema
3. **Missing specifications** (P1): Terminal behaviors, recovery rules, glossary

With the P0 and P1 fixes applied, the documentation will be **highly AI-agent friendly** and ready for production use.

**Recommended Action**: Apply P0 fixes immediately, P1 fixes within one sprint, P2 fixes as time permits.

