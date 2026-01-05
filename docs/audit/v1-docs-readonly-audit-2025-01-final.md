# V1 Documentation READ-ONLY Audit Report

**Date**: 2025-01-XX  
**Auditor**: AI Agent (Codex)  
**Target**: V1 CURRENT docs + schema/examples for AI-agent consumption  
**Scope**: Consistency and agent-friendliness audit (evidence-only, no fixes)

---

## Validation Status

### Examples vs Schema Validation

**Status**: ✅ **PASS**

- `docs/orchestrator-v1.schema.json`: Valid JSON
- `docs/orchestrator-v1.examples.json`: Valid JSON
- No deprecated `sessionId` found in examples (verified: 0 hits)
- Examples structure matches schema `ExamplesFile` format

---

## Findings (P0/P1/P2)

### P0 Findings (Blocking Agent Correctness)

#### P0-1: No Critical Contradictions Found

**Status**: ✅ **CLEAR**

- Idempotency keys: Schema `$comment` (line 6) and `xIndex.idempotencyKeyParsing` / `idempotencyKeyArtifact` (lines 16-23) are consistent
- Versioning: `schemaVersion` vs `version` distinction is clearly documented in `Orchestrator-V1.md` §4.1 (lines 135-170)
- No contradictory idempotency rules found

**Evidence**:
- `docs/orchestrator-v1.schema.json` line 6: `$comment` defines split idempotency keys
- `docs/orchestrator-v1.schema.json` lines 16-23: `xIndex` matches `$comment` guidance
- `docs/Orchestrator-V1.md` §4.1: Explicit versioning glossary distinguishes `schemaVersion` (parsing contract) vs `version` (artifact marker)

---

### P1 Findings (Should Fix Soon)

#### P1-1: SessionMemory Still Listed in xIndex.payloadTypes

**File**: `docs/orchestrator-v1.schema.json`  
**Section**: `xIndex.payloadTypes` (line 24-38)  
**Field**: `payloadTypes` array

**Issue**: `SessionMemory` is listed in `xIndex.payloadTypes` array (line 28), but schema marks it as deprecated (line 565: `$comment: "DEPRECATED: use M2BTranscriptionState"`)

**Impact**: Agents may see `SessionMemory` in payloadTypes list and assume it's a current canonical type

**Evidence**:
- Line 28: `"SessionMemory"` appears in `payloadTypes` array
- Line 39-41: `deprecatedPayloadTypes` array exists and contains `"SessionMemory"`
- Line 565: `SessionMemory` definition has `$comment: "DEPRECATED: use M2BTranscriptionState"`
- Line 67: `SessionMemory` still referenced in `SinglePayload.oneOf` (for backward compatibility)

**Recommendation**: Move `SessionMemory` from `payloadTypes` to `deprecatedPayloadTypes` only, or add explicit note that `payloadTypes` includes deprecated types for backward compatibility.

---

#### P1-2: PublishedAnalysis Description References SessionMemory

**File**: `docs/orchestrator-v1.schema.json`  
**Section**: `PublishedAnalysis` definition (line 783-833)  
**Field**: `description` (line 786)

**Issue**: `PublishedAnalysis.description` states: "chapter timeline and speaker map are derived from SessionMemory" (line 786), but `SessionMemory` is deprecated in favor of `M2BTranscriptionState`

**Impact**: Agents may infer that `PublishedAnalysis` depends on deprecated `SessionMemory` type

**Evidence**:
- Line 786: `"description": "Published analysis view; chapter timeline and speaker map are derived from SessionMemory."`
- Line 565: `SessionMemory` marked as deprecated
- Line 1309: `M2BTranscriptionState` is the canonical type

**Recommendation**: Update description to reference `M2BTranscriptionState` instead of `SessionMemory`.

---

#### P1-3: T-Task.md References SessionMemory in V1 Workstream

**File**: `docs/T-Task.md`  
**Section**: `WS-V1-3` (line 21)  
**Field**: Workstream description

**Issue**: WS-V1-3 mentions "M2BTranscriptionState schema" (line 21), which is correct, but previous audit reports indicate this was changed from "SessionMemory schema"

**Status**: ✅ **RESOLVED** (per current file content)

**Evidence**:
- Line 21: `"WS-V1-3 — MemoryCenter interface + M2BTranscriptionState schema"` (correct)

---

### P2 Findings (Nice to Have, Polish)

#### P2-1: Identifier Glossary Completeness

**File**: `docs/Orchestrator-V1.md`  
**Section**: `§6.0 Identifier Glossary` (lines 217-232)

**Status**: ✅ **COMPLETE**

**Evidence**:
- `chapterId`: ✅ Listed (line 230)
- `artifactType`: ✅ Listed (line 231)
- All required identifiers present: `chatSessionId`, `turnId`, `audioAssetId`, `recordingSessionId`, `disectorPlanId`, `batchAssetId`, `batchIndex`, `publishedPrefixBatchIndex`, `jobId`

---

#### P2-2: jobId vs tingwuJobId Naming

**File**: `docs/Orchestrator-V1.md`  
**Section**: `§6.0 Identifier Glossary` (line 229)

**Status**: ✅ **CLEAR**

**Evidence**:
- Line 229: Glossary entry states: `jobId` is "Provider job identifier (provider-agnostic; Tingwu provider calls it "tingwuJobId" internally, but schema uses `jobId`)"
- Schema uses `jobId` consistently (e.g., `TingwuBatchArtifact.jobId` line 460)

---

#### P2-3: Archive Hygiene

**Status**: ✅ **GOOD**

**Evidence**:
- `docs/Orchestrator-V1.md` line 4, 13: Explicitly states "V7 已弃用并归档（ARCHIVED）"
- `docs/api-contracts.md` line 6: States "V7 已归档（ARCHIVED）"
- `docs/ux-contract.md` line 5: States "V7 规范已归档"
- `docs/archived/Orchestrator-MetadataHub-V7.md` line 1, 4: Header states "ARCHIVED"
- `docs/T-Task.md` line 44: Clear ARCHIVED banner above V7 traces
- All CURRENT references point to `docs/Orchestrator-V1.md`

---

## Spec vs Schema Mismatches

### Mismatch 1: None Found

**Status**: ✅ **ALIGNED**

- Schema `$comment` (line 6) matches spec versioning guidance (`Orchestrator-V1.md` §4.1)
- Schema idempotency keys match spec invariants
- Examples validate against schema

---

### Mismatch 2: M2B Anchor Requirement Enforcement

**File**: `docs/orchestrator-v1.schema.json`  
**Section**: `M2BKeyPoint` definition (lines 1258-1308)

**Status**: ✅ **ENFORCED**

**Evidence**:
- Line 1267-1274: `anyOf` constraint enforces that `M2BKeyPoint` must have either `chapterId` OR `timeRange`
- Line 1261: Description states "Key point with required source pointers (chapterId and/or timeRange)"
- `docs/orchestrator-v1.examples.json` line 424-435: Example `M2BKeyPoint` includes both `chapterId` and `timeRange` (satisfies constraint)

**Spec Alignment**:
- `docs/Orchestrator-V1.md` §6.2 (line 252): States "锚点：`chapterId` 和/或 `[startMs, endMs]` 时间范围"
- Schema enforces this via `anyOf` constraint

---

## Missing or Ambiguous Semantics

### Ambiguity 1: None Found

**Status**: ✅ **CLEAR**

- MachineArtifact extraction: Explicitly defined in `Orchestrator-V1.md` §5.2 (lines 195-202) and `api-contracts.md` §5.2 (lines 131-135)
- HumanDraft extractable criteria: Explicitly defined in `Orchestrator-V1.md` §8 (lines 327-335) and `api-contracts.md` §5.2 (lines 137-142)
- TimeRange semantics: Explicitly defined in `Orchestrator-V1.md` §6.2 (lines 253-255) and schema descriptions (lines 1234, 1285)

---

### Ambiguity 2: MetadataOp Known Operations Guidance

**File**: `docs/orchestrator-v1.schema.json`  
**Section**: `MetadataOp` definition (lines 970-994)

**Status**: ✅ **GUIDANCE PROVIDED**

**Evidence**:
- Line 976: `$comment` states: "Unknown fields/ops are ignored unless explicitly implemented; agents should prefer xKnownOps."
- Line 977-984: `xKnownOps` array lists: `["addTag","removeTag","setRiskLevel","setMainPerson","setLocation","setStage"]`
- `docs/Orchestrator-V1.md` §7.2 (lines 300-311): Table lists known ops with semantics

**Note**: Schema allows `additionalProperties: true` (line 972) for extensibility, which is intentional.

---

## Contract Alignment Verification

### Invariant 1: Publisher Owns Truth

**Status**: ✅ **CONSISTENT**

**Evidence**:
- `docs/Orchestrator-V1.md` §2.1 (lines 49-54): "Publisher owns truth" invariant stated
- `docs/api-contracts.md` §2.2 (lines 71-75): UI only renders Publisher output
- `docs/ux-contract.md` §1.1 (lines 14-16): UI only renders `PublishedChatTurn.displayMarkdown`

---

### Invariant 2: LLM Parser Cannot Mutate Transcript Truth

**Status**: ✅ **CONSISTENT**

**Evidence**:
- `docs/Orchestrator-V1.md` §2.2 (lines 56-58): "LLM Parser 不得修改转写真相"
- `docs/Orchestrator-V1.md` §3.1.3 (lines 80-86): LLM Parser outputs Metadata Hub updates only
- `docs/Orchestrator-V1.md` §6.2 (line 249): "LLM Parser 写入 M2B；Publisher 不得修改 transcript truth"

---

### Invariant 3: Dual-Channel Output

**Status**: ✅ **CONSISTENT**

**Evidence**:
- `docs/Orchestrator-V1.md` §5.2 (lines 187-203): Defines HumanDraft (`<visible2user>`) + MachineArtifact (fenced JSON block)
- `docs/api-contracts.md` §2.2 (lines 72-75): Matches extraction rule
- `docs/ux-contract.md` §1.1 (lines 14-16): UI only renders `displayMarkdown` (extracted from `<visible2user>`)

---

### Invariant 4: Prefix Publishing

**Status**: ✅ **CONSISTENT**

**Evidence**:
- `docs/Orchestrator-V1.md` §11 Appendix B.2 (lines 439-443): "连续前缀发布（硬性不变量）"
- `docs/orchestrator-v1.schema.json` line 653: `PublisherState.publishedPrefixBatchIndex` description: "monotonic and advances only for contiguous batches"
- `docs/api-contracts.md` §3.2 (line 93): "UI 不得拼装乱序批次；只消费 `PublishedTranscript`"

---

### Invariant 5: Overlap Rules

**Status**: ✅ **CONSISTENT**

**Evidence**:
- `docs/Orchestrator-V1.md` §10 Appendix A.4 (lines 415-426): Pre-roll only overlap (10s), no post-roll
- `docs/orchestrator-v1.schema.json` line 249: `overlapMs` const: 10000
- `docs/orchestrator-v1.schema.json` line 206: `captureEndMs` description: "must equal absEndMs (no post-roll)"

---

## Extraction & Rendering Rules Verification

### Rule 1: MachineArtifact Extraction

**Status**: ✅ **CANONICAL**

**Evidence**:
- `docs/Orchestrator-V1.md` §5.2 (lines 195-202): Defines extraction algorithm (fenced ` ```json` block outside `<visible2user>`)
- `docs/api-contracts.md` §5.2 (lines 131-135): Matches extraction algorithm
- Both documents explicitly prohibit heuristic JSON extraction

---

### Rule 2: `<visible2user>` Rendering

**Status**: ✅ **CONSISTENT**

**Evidence**:
- `docs/Orchestrator-V1.md` §2.1 (line 52): "UI **只渲染** `<visible2user> ... </visible2user>` 内文本"
- `docs/api-contracts.md` §2.2 (lines 72-73): "`DisplayDelta` 必须来自 `<visible2user>` 内的内容投影"
- `docs/ux-contract.md` §1.1 (lines 14-15): "UI 只渲染 `PublishedChatTurn.displayMarkdown`" and "`<visible2user>` 标签永不出现在 UI"

---

## Versioning Semantics Verification

### Option 1: schemaVersion vs version Distinction

**Status**: ✅ **CLEAR**

**Evidence**:
- `docs/Orchestrator-V1.md` §4.1 (lines 135-170): Explicit versioning glossary
- `docs/orchestrator-v1.schema.json` line 6: `$comment` defines split idempotency keys based on versioning type
- Parsing-contract objects (MachineArtifact, M1..M4, M2BTranscriptionState): Use `schemaVersion`
- Domain artifacts (DisectorPlan, TingwuJobState, etc.): Use `version`

**Schema Examples**:
- `MachineArtifact.schemaVersion` (line 1032): Required, const: 1
- `DisectorPlan.version` (line 239): Required, const: 1
- `M2BTranscriptionState.schemaVersion` (line 1326): Required, const: 1

---

## ID Glossary & Lifecycle Verification

### Identifier Consistency

**Status**: ✅ **CONSISTENT**

**Evidence**:
- `docs/Orchestrator-V1.md` §6.0 (lines 217-232): Complete identifier glossary
- All identifiers used consistently across schema and examples:
  - `chatSessionId`: ✅ Consistent
  - `turnId`: ✅ Consistent
  - `audioAssetId`: ✅ Consistent
  - `recordingSessionId`: ✅ Consistent (no deprecated `sessionId` in examples)
  - `disectorPlanId`: ✅ Consistent
  - `batchAssetId`: ✅ Consistent
  - `batchIndex`: ✅ Consistent
  - `publishedPrefixBatchIndex`: ✅ Consistent
  - `jobId`: ✅ Consistent (not `tingwuJobId`)

**Deprecated Aliases**:
- `sessionId`: ✅ Marked deprecated in schema (lines 235, 288, 446, 584, 665, 737, 801) with description "DEPRECATED alias of recordingSessionId. Agents must not emit sessionId in new payloads."
- `sessionId`: ✅ Not found in examples (verified via grep)

---

## Summary

### Overall Status: ✅ **GOOD**

The V1 documentation, schema, and examples are **largely consistent and agent-friendly**. The main findings are:

1. **P1-1**: `SessionMemory` still listed in `xIndex.payloadTypes` (should be in `deprecatedPayloadTypes` only)
2. **P1-2**: `PublishedAnalysis.description` references deprecated `SessionMemory` (should reference `M2BTranscriptionState`)

All other audit tasks passed:
- ✅ Contract alignment: All invariants consistent
- ✅ Schema/examples integrity: Examples validate, M2B anchor enforced
- ✅ Versioning semantics: Clear distinction documented
- ✅ ID glossary: Complete and consistent
- ✅ Extraction & rendering rules: Canonical and consistent
- ✅ Archive hygiene: V7 correctly archived, no CURRENT leaks

---

## Evidence Citations

### File References

- `docs/Orchestrator-V1.md`: 482 lines
- `docs/orchestrator-v1.schema.json`: 1412 lines
- `docs/orchestrator-v1.examples.json`: 438 lines
- `docs/api-contracts.md`: 150 lines
- `docs/ux-contract.md`: 93 lines
- `docs/T-Task.md`: 401 lines (includes ARCHIVED V7 traces)
- `docs/archived/Orchestrator-MetadataHub-V7.md`: 229 lines (ARCHIVED)

### Key Sections

- `docs/Orchestrator-V1.md` §2.1: Publisher owns truth
- `docs/Orchestrator-V1.md` §4.1: Versioning glossary
- `docs/Orchestrator-V1.md` §5.2: MachineArtifact extraction
- `docs/Orchestrator-V1.md` §6.0: Identifier glossary
- `docs/Orchestrator-V1.md` §6.2: M2B structure and timeRange semantics
- `docs/Orchestrator-V1.md` §8: HumanDraft extractable criteria
- `docs/orchestrator-v1.schema.json` line 6: `$comment` idempotency keys
- `docs/orchestrator-v1.schema.json` lines 1258-1308: `M2BKeyPoint` anchor enforcement
- `docs/orchestrator-v1.schema.json` lines 970-994: `MetadataOp` guidance

---

**End of Audit Report**


