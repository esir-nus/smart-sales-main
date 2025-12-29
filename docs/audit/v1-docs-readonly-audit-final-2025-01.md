# V1 Documentation Read-Only Audit: Consistency & Agent-Friendliness (Post-Fix)

**Audit Date**: 2025-01-27  
**Auditor Role**: Senior CS Engineer & Software Architect  
**Scope**: READ-ONLY findings only (no fixes proposed)  
**Target**: V1 CURRENT docs + schema/examples for AI-agent consumption  
**Context**: Audit performed after P0/P1/P2 fixes applied

---

## Validation Status: Examples vs Schema

### ✅ **Examples Validate Against Schema**

- All examples in `docs/orchestrator-v1.examples.json` validate against `docs/orchestrator-v1.schema.json`
- No deprecated `sessionId` found in examples (verified: 0 hits)
- M2BKeyPoint examples include required anchors (`chapterId` or `timeRange`)
- All required fields present in examples

---

## Findings (P0/P1/P2)

### P0 - Critical Contradictions or Missing Required Fields

**None found** - All P0 issues from previous audit have been resolved.

### P1 - Schema vs Spec Mismatches or Ambiguities

#### P1-1: Chat Retry Terminal Behavior Still Ambiguous
- **Location**: `docs/Orchestrator-V1.md` §8 (lines 305-309)
- **Spec States**: "若重试耗尽仍失败：允许展示最后一次 HumanDraft（如果可抽取）"
- **Ambiguity**: "如果可抽取" remains undefined
  - What conditions make HumanDraft extractable?
  - What if HumanDraft is empty or malformed?
  - What if `<visible2user>` tags are present but content is empty?
- **Missing**: Explicit terminal states for all failure scenarios
- **Evidence**: Line 306 uses conditional "如果可抽取" without defining extraction criteria

#### P1-2: MachineArtifact Extraction Rule Inconsistency
- **Location**: `docs/Orchestrator-V1.md` §5.2 (line 195-196)
- **Spec States**: "MachineArtifact 必须位于 fenced code block 中：\`\`\`json ... \`\`\`"
- **Location**: `docs/api-contracts.md` §2.2 (line 74)
- **Contract States**: "Publisher 从 LLM 输出中提取第一个位于 `<visible2user>` 之外的 \`\`\`json fenced block"
- **Issue**: Both documents specify the same rule, but:
  - Spec says "必须位于" (must be located in) - implies LLM must produce it this way
  - Contract says "提取" (extract) - implies Publisher extracts it
  - **Missing**: What if LLM doesn't produce fenced block? Fallback behavior undefined
- **Evidence**: Both documents reference the rule but neither defines failure/fallback behavior

#### P1-3: SessionMemory Still Listed in xIndex.payloadTypes
- **Location**: `docs/orchestrator-v1.schema.json` line 28
- **Issue**: `SessionMemory` is listed in `xIndex.payloadTypes` array
- **Schema Status**: `SessionMemory` is marked deprecated (line 564: `$comment: "DEPRECATED: use M2BTranscriptionState"`)
- **Impact**: Agents may see `SessionMemory` in payloadTypes list and assume it's valid
- **Evidence**: Line 28 includes `"SessionMemory"` in payloadTypes; line 564 marks it deprecated

#### P1-4: MetadataOp Schema Allows Additional Properties Without Constraints
- **Location**: `docs/orchestrator-v1.schema.json` lines 968-983
- **Schema Definition**: `MetadataOp` has `additionalProperties: true` (line 970)
- **Issue**: While `op` is required, agents can add arbitrary fields without validation
- **Impact**: No schema-level enforcement of operation semantics
- **Evidence**: Schema allows any additional properties beyond `op`, `path`, `value`

### P2 - Minor Inconsistencies or Missing Documentation

#### P2-1: Missing Time Range Semantics Documentation
- **Location**: `docs/Orchestrator-V1.md` §6.2 (lines 235-241)
- **Spec States**: M2B entries must include "锚点：`chapterId` 和/或 `[startMs, endMs]` 时间范围"
- **Missing**: Explicit documentation of whether `timeRange.startMs`/`endMs` are:
  - Absolute milliseconds since recording start?
  - Relative to session start?
  - Relative to chapter start?
- **Schema**: `M2BKeyPoint.timeRange` (lines 1268-1280) defines structure but not semantics
- **Evidence**: Spec mentions time ranges but doesn't define absolute vs relative semantics

#### P2-2: Retry Policy Cross-Reference Issue
- **Location**: `docs/Orchestrator-V1.md` §8.1 (line 315)
- **Spec States**: "**重试规则**（参见 `docs/api-contracts.md` §5.1）"
- **Location**: `docs/api-contracts.md` §5.1 (lines 122-125)
- **Contract States**: Retry rules but references `docs/Orchestrator-V1.md` §8.1
- **Issue**: Circular reference - V1.md references api-contracts.md, api-contracts.md references V1.md
- **Evidence**: Line 315 in V1.md references api-contracts.md §5.1; api-contracts.md §5.1 references V1.md §8.1

#### P2-3: T-Task.md Still References SessionMemory in V1 Workstream
- **Location**: `docs/T-Task.md` line 21
- **Issue**: WS-V1-3 mentions "SessionMemory schema" but spec uses `M2BTranscriptionState`
- **Impact**: May confuse agents about canonical type name
- **Evidence**: Line 21: "WS-V1-3 — MemoryCenter interface + SessionMemory schema"

---

## Spec vs Schema Mismatches

### Mismatch 1: Chat Retry Terminal Behavior Underspecified
- **Spec** (`Orchestrator-V1.md` §8 lines 305-309): States "如果可抽取" without defining extraction criteria
- **Schema**: No schema-level validation for retry terminal states
- **Gap**: Missing explicit terminal state machine for retry exhaustion

### Mismatch 2: MachineArtifact Extraction Fallback Undefined
- **Spec** (`Orchestrator-V1.md` §5.2 line 195): States "必须位于 fenced code block"
- **Contract** (`api-contracts.md` §2.2 line 74): States extraction rule
- **Gap**: Neither document defines what happens if LLM doesn't produce fenced block

### Mismatch 3: Time Range Semantics Undefined
- **Spec** (`Orchestrator-V1.md` §6.2): Mentions `[startMs, endMs]` time ranges
- **Schema** (`orchestrator-v1.schema.json` lines 1268-1280): Defines `timeRange` structure
- **Gap**: No documentation of whether times are absolute or relative

---

## Missing or Ambiguous Semantics

### Ambiguity 1: HumanDraft Extraction Criteria
- **Location**: `docs/Orchestrator-V1.md` §8 (line 306)
- **Spec States**: "允许展示最后一次 HumanDraft（如果可抽取）"
- **Missing Definition**:
  - What makes HumanDraft "extractable"?
  - Is empty `<visible2user></visible2user>` considered extractable?
  - What if tags are malformed?
  - What if content exists but is only whitespace?

### Ambiguity 2: MachineArtifact Extraction Fallback
- **Location**: `docs/Orchestrator-V1.md` §5.2 (line 195), `docs/api-contracts.md` §2.2 (line 74)
- **Spec States**: MachineArtifact "必须位于 fenced code block"
- **Missing**: What if LLM output doesn't contain ` ```json` fenced block?
  - Should Publisher fail immediately?
  - Should Publisher attempt to extract JSON from other formats?
  - Should Publisher mark artifact as INVALID and retry?

### Ambiguity 3: Time Range Absolute vs Relative Semantics
- **Location**: `docs/Orchestrator-V1.md` §6.2 (lines 235-241)
- **Spec Mentions**: `[startMs, endMs]` time ranges as anchors
- **Missing**: Explicit statement of whether:
  - `startMs`/`endMs` are absolute milliseconds since recording start
  - Or relative to some other reference point
- **Schema**: Defines structure but not semantics (lines 1268-1280)

### Ambiguity 4: Retry Policy Circular Reference
- **Location**: `docs/Orchestrator-V1.md` §8.1 (line 315), `docs/api-contracts.md` §5.1
- **Issue**: V1.md references api-contracts.md §5.1, api-contracts.md references V1.md §8.1
- **Missing**: Single source of truth for retry policy details

---

## Contract Alignment Summary

### ✅ **Invariants Stated in Spec**

1. **Publisher owns truth** (`Orchestrator-V1.md` §2.1): ✅ Stated clearly
2. **LLM Parser cannot mutate transcript truth** (`Orchestrator-V1.md` §2.2): ✅ Stated clearly
3. **Dual-channel output** (`Orchestrator-V1.md` §5.2): ✅ Stated clearly
4. **Prefix publishing** (`Orchestrator-V1.md` §11.2): ✅ Stated clearly
5. **Overlap rules** (`Orchestrator-V1.md` §10.4): ✅ Stated clearly with deterministic formulas

### ✅ **Invariants Consistent Across Contracts**

- `api-contracts.md` §2.2: Aligns with Publisher owns truth
- `ux-contract.md` §1.1: Aligns with Publisher owns truth
- Both contracts reference same extraction rule for MachineArtifact

### ⚠️ **Invariants Not Enforced in Schema**

- None of the invariants have schema-level validation
- Schema relies on spec documentation only (acceptable per design)

---

## Schema/Examples Integrity

### ✅ **Examples Validate Against Schema**

- All examples pass schema validation
- Required fields present
- Type constraints satisfied
- Enum values valid

### ✅ **M2B Anchor Requirements Enforced**

- **Schema** (`orchestrator-v1.schema.json` lines 1253-1260): `anyOf` constraint requires either `chapterId` or `timeRange`
- **Examples** (`orchestrator-v1.examples.json` lines 424-434): All `keyPoints[]` entries include anchors
- **Verification**: Schema validation would fail if anchor missing

### ⚠️ **Schema Permissiveness Issues**

1. **MetadataOp.additionalProperties: true** (line 970): Allows arbitrary fields beyond `op`, `path`, `value`
2. **MachineArtifact.additionalProperties: true** (line 1007): Allows arbitrary fields (intentional for extensibility)

---

## Versioning Semantics

### ✅ **Consistent Distinction**

- **Spec** (`Orchestrator-V1.md` §4.1): Clearly distinguishes `schemaVersion` vs `version`
- **Schema** (`orchestrator-v1.schema.json` line 6): `$comment` correctly splits parsing-contract vs domain artifacts
- **Schema xIndex** (lines 16-23): Split into `idempotencyKeyParsing` and `idempotencyKeyArtifact`

### ✅ **Correct Application**

- Parsing-contract objects (MachineArtifact, M1..M4, M2BTranscriptionState): Use `schemaVersion`
- Domain artifacts (DisectorPlan, TingwuJobState, etc.): Use `version`
- No contradictions found

---

## ID Glossary & Lifecycle

### ✅ **Identifiers Consistent**

- All identifiers from glossary (`Orchestrator-V1.md` §6.0) are used consistently:
  - `chatSessionId`: ✅ Consistent
  - `turnId`: ✅ Consistent
  - `audioAssetId`: ✅ Consistent
  - `recordingSessionId`: ✅ Consistent (no deprecated `sessionId` in examples)
  - `disectorPlanId`: ✅ Consistent
  - `batchAssetId`: ✅ Consistent
  - `batchIndex`: ✅ Consistent
  - `publishedPrefixBatchIndex`: ✅ Consistent
  - `jobId`: ✅ Consistent (glossary correctly notes provider calls it "tingwuJobId" internally)

### ✅ **Glossary Completeness**

- `chapterId`: ✅ Added to glossary (line 224)
- `artifactType`: ✅ Added to glossary (line 225)
- All identifiers documented

---

## Extraction & Rendering Rules

### ✅ **MachineArtifact Extraction Rule Defined**

- **Spec** (`Orchestrator-V1.md` §5.2 line 195-196): Defines fenced code block extraction
- **Contract** (`api-contracts.md` §2.2 line 74): References same rule
- **Consistency**: Both documents specify same extraction method

### ⚠️ **Missing Fallback Behavior**

- **Issue**: Neither document defines what happens if fenced block is missing
- **Location**: `docs/Orchestrator-V1.md` §5.2, `docs/api-contracts.md` §2.2

### ✅ **`<visible2user>` Rendering Rule Consistent**

- **Spec** (`Orchestrator-V1.md` §2.1 line 52): "UI **只渲染** `<visible2user> ... </visible2user>` 内文本"
- **Contract** (`api-contracts.md` §2.2 line 71): "UI 只能展示 `PublishedChatTurn.displayMarkdown`"
- **UX Contract** (`ux-contract.md` §1.1 line 14-15): "UI 只渲染 `PublishedChatTurn.displayMarkdown`" and "`<visible2user>` 标签永不出现在 UI"
- **Consistency**: All three documents align on Publisher extracts from `<visible2user>`, UI renders `displayMarkdown`

---

## Archive Hygiene

### ✅ **V7 Correctly Marked as ARCHIVED**

- `docs/Orchestrator-V1.md` line 4, 13: Explicitly states "V7 已弃用并归档（ARCHIVED）"
- `docs/api-contracts.md` line 6: States "V7 已归档（ARCHIVED）"
- `docs/ux-contract.md` line 5: States "V7 规范已归档"
- `docs/archived/Orchestrator-MetadataHub-V7.md` line 1, 4: Header states "ARCHIVED"
- `docs/T-Task.md` line 44: Clear ARCHIVED banner above V7 traces

### ✅ **No CURRENT Pointers Leak to V7**

- All CURRENT references point to `docs/Orchestrator-V1.md`
- No V7 references in CURRENT contracts
- `docs/AGENTS.md` correctly lists V7 as ARCHIVED (line 23-24)

### ⚠️ **Minor Archive Reference Issue**

- `docs/T-Task.md` line 21: WS-V1-3 mentions "SessionMemory schema" but should reference `M2BTranscriptionState` per CURRENT spec
- **Impact**: Low - workstream description, not spec reference

---

## Evidence Summary

### Files Audited
- `docs/Orchestrator-V1.md` (456 lines)
- `docs/orchestrator-v1.schema.json` (1396 lines)
- `docs/orchestrator-v1.examples.json` (438 lines)
- `docs/api-contracts.md` (141 lines)
- `docs/ux-contract.md` (93 lines)
- `docs/AGENTS.md` (130 lines)
- `AGENTS.md` (root, redirect only)
- `docs/T-Task.md` (406 lines)
- `docs/archived/Orchestrator-MetadataHub-V7.md` (229 lines)

### Critical Issues Found
- **P0**: 0 issues (all previous P0 issues resolved)
- **P1**: 4 issues (terminal behavior ambiguity, extraction fallback, deprecated type in index, schema permissiveness)
- **P2**: 3 issues (time semantics, circular reference, workstream naming)

### Overall Assessment
- **Contract alignment**: ✅ Invariants stated and consistent across contracts
- **Schema/examples integrity**: ✅ Examples validate, M2B anchors enforced
- **Versioning semantics**: ✅ Consistent distinction and application
- **ID glossary**: ✅ Complete and consistent
- **Extraction rules**: ✅ Defined but missing fallback behavior
- **Archive hygiene**: ✅ V7 correctly archived, minor workstream naming issue

---

**End of READ-ONLY Audit**

