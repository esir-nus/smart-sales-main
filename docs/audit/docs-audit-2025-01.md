# Documentation System Audit: AI-Agent Friendliness & Contract Integrity

**Audit Date**: 2025-01-27  
**Auditor Role**: Senior CS Engineer & Software Architect  
**Scope**: V1 CURRENT architecture documentation for AI coding agents  
**Target Audience**: AI coding agents consuming docs as primary source of truth

---

## Executive Verdict: **PASS WITH FIXES**

### Summary (5–10 bullet reasons)

✅ **Strengths:**
- Clear V1 CURRENT vs V7 ARCHIVED separation in main spec (`Orchestrator-V1.md`)
- Comprehensive identifier glossary (Section 6.0) with lifecycle and purpose
- Explicit invariants stated (Publisher owns truth, LLM Parser cannot mutate transcript truth)
- Schema and examples files exist and are referenced
- Versioning distinction (`schemaVersion` vs `version`) is explained in detail

⚠️ **Critical Issues Requiring Fixes:**
- **P0**: Schema-examples mismatch: `DisectorPlan` examples use deprecated `sessionId` instead of required `recordingSessionId`
- **P0**: Missing required fields in examples: `DisectorPlan` examples omit `disectorPlanId` (required per schema)
- **P0**: Contract contradiction: `api-contracts.md` §5.1 retry policy references `docs/api-contracts.md §5.1` (self-reference) but actual policy is in `Orchestrator-V1.md` §8.1
- **P1**: Identifier inconsistency: `T-Task.md` mixes V7 terminology (`SessionMemory`) with V1 (`M2BTranscriptionState`)
- **P1**: Missing terminal behavior: Chat retry policy (`maxRetries=2`) lacks explicit "what happens after 2 failures" terminal state
- **P1**: Ambiguous schema: `MachineArtifact.metadataPatch.ops` is `array<object>` with no schema, making it non-validatable
- **P2**: V7 leakage: `current-state.md` §"V1.1 目标架构提示" references concepts not yet in V1 spec
- **P2**: Missing glossary entries: `jobId`/`tingwuJobId` mentioned in identifier table but not consistently used in examples

---

## 1) Contract Integrity Audit

### 1.1 Contradictions Between Orchestrator-V1.md and api-contracts/ux-contract

#### ✅ **No Major Contradictions Found**
- `api-contracts.md` correctly references `Orchestrator-V1.md` as SoT
- `ux-contract.md` correctly states UI only renders Publisher output
- Both contracts align on HUD 3-section structure

#### ⚠️ **Minor Inconsistencies:**

1. **Retry Policy Reference Loop** (`api-contracts.md` §5.1)
   - **Issue**: States "参见 `docs/api-contracts.md` §5.1" (self-reference)
   - **Should be**: "参见 `docs/Orchestrator-V1.md` §8.1"
   - **Fix**: Update `api-contracts.md` line 122 to reference correct doc

2. **Tingwu Retry Policy Detail Level**
   - **Orchestrator-V1.md** §8.1: Specifies `maxRetries=3`, backoff array, terminal behavior
   - **api-contracts.md** §5.1: States policy but lacks terminal behavior details
   - **Recommendation**: Add explicit terminal behavior to `api-contracts.md` or remove duplication (prefer single source)

### 1.2 Schema vs Examples Mismatches

#### **P0 CRITICAL: DisectorPlan Examples Violate Schema**

**Schema Requirements** (`orchestrator-v1.schema.json` lines 214-223):
- `recordingSessionId` (required)
- `disectorPlanId` (required)
- `version` (required, const: 1)

**Examples** (`orchestrator-v1.examples.json` lines 26-78):
- ✅ Uses `recordingSessionId` (correct)
- ❌ **Missing `disectorPlanId`** (required but absent)
- ✅ Uses `version: 1` (correct)
- ⚠️ Uses deprecated `sessionId` (allowed but deprecated)

**Fix Required**: Add `disectorPlanId` to all three `DisectorPlan` examples.

#### **P0 CRITICAL: SessionMemory vs M2BTranscriptionState Naming**

**Schema** (`orchestrator-v1.schema.json`):
- Defines `SessionMemory` (lines 559-641) — **legacy name**
- Defines `M2BTranscriptionState` (lines 1263-1308) — **V1 canonical name**

**Orchestrator-V1.md** §6.2:
- Uses `M2B TranscriptionDerivedState` (canonical)

**Examples** (`orchestrator-v1.examples.json` line 216):
- Uses `SessionMemory` (legacy)

**Issue**: Schema maintains both names, but spec only mentions M2B. This creates ambiguity for agents.

**Recommendation**: 
- **Option A**: Deprecate `SessionMemory` in schema, add migration note
- **Option B**: Add explicit mapping: "`SessionMemory` is an alias for `M2BTranscriptionState`; agents should prefer `M2BTranscriptionState`"

#### **P1: MachineArtifact.metadataPatch.ops Schema Missing**

**Schema** (`orchestrator-v1.schema.json` lines 963-985):
```json
"ops": {
  "type": "array",
  "items": {
    "type": "object"  // ⚠️ No schema for object structure
  }
}
```

**Examples** (`orchestrator-v1.examples.json` lines 375-387, 445-457):
- Shows `ops` with `op` and `value` fields, but schema doesn't enforce this

**Issue**: Agents cannot validate `metadataPatch.ops` structure.

**Recommendation**: Either:
- Add minimal schema for `ops` items (at least `op: string`, `value: any`)
- Or document that `ops` is intentionally flexible and validation is parser-specific

### 1.3 Invariant Verification: "Publisher owns truth" & "LLM Parser cannot mutate transcript truth"

#### ✅ **Consistently Stated:**

1. **Orchestrator-V1.md** §2.1: "Publisher owns truth" — UI only shows Publisher output
2. **Orchestrator-V1.md** §2.2: "LLM Parser 不得修改转写真相" — explicit constraint
3. **Orchestrator-V1.md** §3.1: LLM Parser output is Metadata Hub updates only
4. **api-contracts.md** §2.2: "UI 只能展示 `PublishedChatTurn.displayMarkdown`"
5. **ux-contract.md** §1.1: "UI 只渲染 `PublishedChatTurn.displayMarkdown`"

#### ⚠️ **Missing Enforcement in Schema:**

- Schema does not prevent `MachineArtifact` from containing transcript-mutating fields
- **Recommendation**: Add schema-level comment or validation rule: "MachineArtifact must not contain fields that modify PublishedTranscript truth"

---

## 2) ID & Lifecycle Audit

### 2.1 Identifier Definitions and Consistency

#### ✅ **Well-Defined Identifiers** (from `Orchestrator-V1.md` §6.0):

| Identifier | Defined | Lifecycle Clear | Used Consistently |
|-----------|---------|----------------|-------------------|
| `chatSessionId` | ✅ | ✅ | ✅ |
| `turnId` | ✅ | ✅ | ✅ |
| `audioAssetId` | ✅ | ✅ | ✅ |
| `recordingSessionId` | ✅ | ✅ | ⚠️ (see below) |
| `disectorPlanId` | ✅ | ✅ | ❌ (missing in examples) |
| `batchAssetId` | ✅ | ✅ | ✅ |
| `batchIndex` | ✅ | ✅ | ✅ |
| `publishedPrefixBatchIndex` | ✅ | ✅ | ✅ |
| `jobId` / `tingwuJobId` | ✅ | ✅ | ⚠️ (inconsistent naming) |

#### ⚠️ **Issues Found:**

1. **`disectorPlanId` Missing in Examples**
   - **Location**: `orchestrator-v1.examples.json` lines 26-78
   - **Impact**: Agents cannot see example values for required field
   - **Fix**: Add `"disectorPlanId": "plan-demo-001-v1"` to all DisectorPlan examples

2. **`jobId` vs `tingwuJobId` Naming Inconsistency**
   - **Glossary** (`Orchestrator-V1.md` line 216): Lists both `jobId` / `tingwuJobId`
   - **Schema** (`orchestrator-v1.schema.json` line 454): Uses `jobId`
   - **Examples** (`orchestrator-v1.examples.json` line 146): Uses `jobId`
   - **Recommendation**: Standardize on `jobId` (shorter, provider-agnostic) and remove `tingwuJobId` from glossary or mark as deprecated alias

3. **Deprecated `sessionId` Still Present**
   - **Schema**: Marks `sessionId` as "DEPRECATED alias of recordingSessionId" (lines 229, 282, 440, etc.)
   - **Examples**: Still uses `sessionId` in DisectorPlan examples (lines 27, 49, 81)
   - **Recommendation**: Either remove `sessionId` from examples or add explicit note: "Examples use deprecated `sessionId` for brevity; production must use `recordingSessionId`"

### 2.2 Missing Glossary/Definitions

#### **Missing Identifier Definitions:**

1. **`chapterId`** (used in `M2BChapter`, `RecordingSourcePointer`)
   - **Status**: Referenced but not defined in glossary
   - **Recommendation**: Add to glossary: "`chapterId`: string, per-chapter identifier within a recording session, generated by LLM Parser"

2. **`artifactType`** (used throughout schema)
   - **Status**: Not in glossary, but schema defines it as discriminator
   - **Recommendation**: Add to glossary: "`artifactType`: string, discriminator field identifying the JSON object type (e.g., 'DisectorPlan', 'MachineArtifact')"

---

## 3) Failure and Retry Semantics

### 3.1 Chat Retry Policy (MachineArtifact Invalid)

#### **Specified** (`Orchestrator-V1.md` §8):
- `maxRetries = 2` (suggested default)
- Retry only for "structured part" (MachineArtifact)
- HumanDraft can be reused or rewritten

#### ⚠️ **Missing Terminal Behavior:**

**Current spec** (lines 291-295):
> "若重试耗尽仍失败：
> - 允许展示最后一次 HumanDraft（如果可抽取）
> - 标记 `artifactStatus = FAILED`
> - 禁止写入 Metadata Hub（避免污染）
> - 必须写入 Trace 事件（便于 HUD 诊断）"

**Issue**: "如果可抽取" is ambiguous. What if HumanDraft is empty or malformed?

**Recommendation**: Add explicit terminal states:
```
Terminal states after maxRetries exhausted:
1. If HumanDraft is non-empty and extractable:
   - Publish PublishedChatTurn with displayMarkdown = extracted HumanDraft
   - artifactStatus = FAILED
   - machineArtifact = null or last attempt's invalid artifact (for debugging)
2. If HumanDraft is empty or unextractable:
   - Publish PublishedChatTurn with displayMarkdown = "[Error: Unable to generate response. Please try again.]"
   - artifactStatus = FAILED
   - machineArtifact = null
3. In both cases:
   - Do NOT write to Metadata Hub
   - MUST emit Trace event with failure reason
```

### 3.2 Tingwu Batch Retry Policy

#### ✅ **Well-Specified** (`Orchestrator-V1.md` §8.1):
- `maxRetries = 3` (suggested default)
- Backoff strategy: `retryBackoffSeconds` array
- Terminal behavior: `status = FAILED`, prefix stops at last contiguous success
- Subsequent batches can run but not publish until failure resolved

#### ⚠️ **Minor Ambiguity:**

**Question**: What if batch 2 fails after 3 retries, but batch 3 succeeds?
- **Spec says**: "后续批次仍可运行并存储（即使前面有失败批次）"
- **Spec says**: "但**不得发布**（`publishedPrefixBatchIndex` 不推进）直到失败批次被解决或跳过"

**Missing**: How to "解决或跳过" a failed batch? Manual retry? Automatic skip after timeout?

**Recommendation**: Add explicit recovery mechanism:
```
Failed batch recovery:
- Option A (manual): UI provides "Retry batch N" action → resets attemptCount, retries
- Option B (automatic skip): After TBD hours, allow user to "Skip batch N" → advances prefix, marks batch as SKIPPED
- Option C (re-plan): Regenerate DisectorPlan with different batch boundaries (future enhancement)
```

---

## 4) Versioning and Evolution

### 4.1 `schemaVersion` vs `version` Clarity

#### ✅ **Well-Explained** (`Orchestrator-V1.md` §4.1):

- **`schemaVersion`**: JSON shape version, controls parsing compatibility
- **`version`**: Domain artifact version marker, does NOT control JSON compatibility

#### ✅ **Consistently Applied in Schema:**

- `MachineArtifact`: Has `schemaVersion` (required, const: 1)
- `DisectorPlan`: Has `version` (required, const: 1), no `schemaVersion`
- `M2BTranscriptionState`: Has both `schemaVersion` (required, const: 1) and `version` (not present, but schema allows `additionalProperties: false`)

#### ⚠️ **Inconsistency Found:**

**`SessionMemory`** (`orchestrator-v1.schema.json` lines 559-641):
- Has both `schemaVersion` (required, const: 1) and `version` (required, const: 1)
- **Issue**: Why does `SessionMemory` need both when `DisectorPlan` only has `version`?

**Recommendation**: Document the rule:
- Objects that are **parsed from LLM output** (MachineArtifact, M2B) need `schemaVersion`
- Objects that are **deterministically generated** (DisectorPlan, PublisherState) only need `version`
- `SessionMemory` is parsed from LLM, so `schemaVersion` is correct; but `version` may be redundant

### 4.2 Versioning Policy Recommendations

#### **Current Policy** (`Orchestrator-V1.md` lines 127-164):
- Increment `schemaVersion` only on breaking changes
- Non-breaking changes (add optional fields, new enum values) do NOT bump

#### ✅ **Sufficient for V1**

#### **Recommendation for Future**:
Add explicit migration policy:
```
When schemaVersion increments:
1. Add migration note in Orchestrator-V1.md §4.1 "Migration note" section
2. Update schema with new version
3. Update examples with new version (keep old examples in archived/)
4. Agents MUST reject objects with unsupported schemaVersion (fail fast, don't attempt to parse)
```

---

## 5) "AI-Agent Friendliness" Scorecard (0–10)

### 5.1 Clarity of SoT & Precedence Rules: **9/10**

✅ **Strengths:**
- `Orchestrator-V1.md` explicitly marked as CURRENT
- V7 clearly marked ARCHIVED
- `AGENTS.md` provides precedence hierarchy
- `api-contracts.md` and `ux-contract.md` reference V1 as SoT

⚠️ **Deductions:**
- `-1`: `T-Task.md` mixes V7 traces with V1 workstreams (confusing for agents)

### 5.2 Determinism / Invariants: **8/10**

✅ **Strengths:**
- Core invariants explicitly stated (§2)
- Disector rules are deterministic with examples (§10)
- Publisher rules are deterministic (§11)

⚠️ **Deductions:**
- `-1`: Some retry terminal behaviors are ambiguous (see §3.1)
- `-1`: Missing explicit "what happens if HumanDraft is empty" terminal state

### 5.3 Contract Completeness: **7/10**

✅ **Strengths:**
- Schema covers all major objects
- Examples cover key scenarios
- API contracts defined

⚠️ **Deductions:**
- `-1`: `metadataPatch.ops` schema is too permissive (see §1.2)
- `-1`: Missing explicit validation rules for invariants in schema
- `-1`: Some terminal behaviors underspecified (see §3)

### 5.4 Examples Coverage: **6/10**

✅ **Strengths:**
- Examples cover DisectorPlan edge cases (20m, 26m merge, 27m)
- Examples show retry scenarios (429 error)
- Examples show M2B with source pointers

⚠️ **Deductions:**
- `-2`: Missing `disectorPlanId` in DisectorPlan examples (P0)
- `-1`: Examples use deprecated `sessionId` without note
- `-1`: Missing example of Chat retry failure terminal state

### 5.5 Naming Consistency: **8/10**

✅ **Strengths:**
- Identifier glossary is comprehensive
- Most identifiers used consistently

⚠️ **Deductions:**
- `-1`: `SessionMemory` vs `M2BTranscriptionState` ambiguity (see §1.2)
- `-1`: `jobId` vs `tingwuJobId` inconsistency (see §2.1)

### 5.6 Extensibility Hooks vs Scope Control: **9/10**

✅ **Strengths:**
- `additionalProperties: true` on extensible objects (MachineArtifact, M1/M2/M3/M4)
- `additionalProperties: false` on strict objects (DisectorPlan, PublisherState)
- Future extensions clearly marked (§13)

⚠️ **Deductions:**
- `-1`: No explicit "do not extend these fields" list for critical objects

### **Overall Score: 7.8/10** (PASS WITH FIXES)

---

## 6) Prioritized Fix List

### P0 (Must Fix Now)

#### P0-1: Add `disectorPlanId` to DisectorPlan Examples
- **File**: `docs/orchestrator-v1.examples.json`
- **Lines**: 26-78 (all three DisectorPlan examples)
- **Change**: Add `"disectorPlanId": "plan-demo-001-v1"` to each DisectorPlan object
- **Reason**: Required field missing, agents cannot see example values

#### P0-2: Fix Retry Policy Self-Reference
- **File**: `docs/api-contracts.md`
- **Line**: 122
- **Change**: Replace "参见 `docs/api-contracts.md` §5.1" with "参见 `docs/Orchestrator-V1.md` §8.1"
- **Reason**: Self-reference breaks contract chain

#### P0-3: Resolve SessionMemory vs M2BTranscriptionState Ambiguity
- **File**: `docs/orchestrator-v1.schema.json`
- **Location**: Add comment to `SessionMemory` definition (line 559)
- **Change**: Add: `"$comment": "DEPRECATED: Use M2BTranscriptionState instead. SessionMemory is maintained for backward compatibility only."`
- **File**: `docs/Orchestrator-V1.md`
- **Location**: §6.2 (after line 231)
- **Change**: Add note: "**Note**: Schema defines both `SessionMemory` and `M2BTranscriptionState`. Agents should use `M2BTranscriptionState` as the canonical name. `SessionMemory` is a deprecated alias."
- **Reason**: Prevents agent confusion about which name to use

### P1 (Should Fix Soon)

#### P1-1: Add Explicit Chat Retry Terminal Behavior
- **File**: `docs/Orchestrator-V1.md`
- **Location**: §8 (after line 295)
- **Change**: Add subsection "8.2 Terminal States After Retry Exhaustion" with explicit states (see §3.1 recommendation)
- **Reason**: Eliminates ambiguity for implementers

#### P1-2: Add Schema for `metadataPatch.ops`
- **File**: `docs/orchestrator-v1.schema.json`
- **Location**: `MetadataPatch.ops.items` (line 980)
- **Change**: Replace `"type": "object"` with minimal schema:
```json
"type": "object",
"required": ["op"],
"properties": {
  "op": {"type": "string"},
  "value": {}
},
"additionalProperties": true
```
- **Reason**: Enables validation, reduces agent guessing

#### P1-3: Standardize `jobId` Naming
- **File**: `docs/Orchestrator-V1.md`
- **Location**: §6.0 Identifier Glossary (line 216)
- **Change**: Remove `tingwuJobId` from table, add note: "`jobId` is provider-agnostic; `tingwuJobId` is a deprecated alias."
- **Reason**: Reduces naming confusion

#### P1-4: Add Missing Identifier Definitions to Glossary
- **File**: `docs/Orchestrator-V1.md`
- **Location**: §6.0 (add after line 216)
- **Change**: Add entries for `chapterId` and `artifactType` (see §2.2)
- **Reason**: Completes glossary, prevents agent guessing

### P2 (Nice to Have)

#### P2-1: Remove Deprecated `sessionId` from Examples
- **File**: `docs/orchestrator-v1.examples.json`
- **Location**: Lines 27, 49, 81, 123, 142, 191, 217, 268, 305, 462
- **Change**: Remove `sessionId` fields, ensure `recordingSessionId` is present
- **Alternative**: Add comment at top of examples file: "Note: Examples use deprecated `sessionId` for brevity. Production code must use `recordingSessionId`."
- **Reason**: Reduces confusion, but low priority since schema marks it deprecated

#### P2-2: Add Failed Batch Recovery Mechanism
- **File**: `docs/Orchestrator-V1.md`
- **Location**: §8.1 (after line 320)
- **Change**: Add subsection "8.1.1 Failed Batch Recovery" with manual retry / skip / re-plan options (see §3.2 recommendation)
- **Reason**: Completes retry semantics, but can be deferred

#### P2-3: Clean Up V7 References in `current-state.md`
- **File**: `docs/current-state.md`
- **Location**: §"V1.1 目标架构提示" (lines 67-75)
- **Change**: Either remove this section (if V1.1 is not yet planned) or move to "Future Extensions" section
- **Reason**: Prevents V7 concept leakage, but low priority

---

## 7) Optional: Docs Lint Checklist (1 Screen)

```markdown
# Documentation Lint Checklist (for AI Agents)

Before coding, verify:

## Source of Truth
- [ ] Using `docs/Orchestrator-V1.md` as CURRENT spec (not V7/V6/V5)
- [ ] Referenced `docs/orchestrator-v1.schema.json` for types
- [ ] Checked `docs/orchestrator-v1.examples.json` for patterns

## Identifiers
- [ ] All IDs match glossary in `Orchestrator-V1.md` §6.0
- [ ] Using `recordingSessionId` (not deprecated `sessionId`)
- [ ] Using `M2BTranscriptionState` (not deprecated `SessionMemory`)
- [ ] Using `jobId` (not `tingwuJobId`)

## Invariants
- [ ] Publisher owns truth: UI only renders Publisher output
- [ ] LLM Parser cannot mutate transcript truth (only Metadata Hub)
- [ ] Batch ordering uses `batchIndex` (integer), not `batchId` (string)

## Schema Compliance
- [ ] All required fields present (check schema `required` array)
- [ ] `schemaVersion` vs `version` used correctly:
  - LLM-parsed objects → `schemaVersion`
  - Deterministic objects → `version`
- [ ] No additional properties on strict objects (`additionalProperties: false`)

## Examples Alignment
- [ ] Examples match schema requirements
- [ ] Examples use CURRENT naming (not deprecated aliases)
- [ ] Examples cover edge cases (retry, failure, empty states)

## Contracts
- [ ] API contracts align with `Orchestrator-V1.md`
- [ ] UX contracts reference Publisher output only
- [ ] Retry policies match spec (§8, §8.1)

## Versioning
- [ ] Breaking changes → bump `schemaVersion`
- [ ] Non-breaking changes → no version bump
- [ ] Migration notes added if `schemaVersion` changes
```

---

## Appendix: File-by-File Findings

### `docs/Orchestrator-V1.md`
- **Status**: ✅ Well-structured, clear invariants
- **Issues**: Minor terminal behavior ambiguity (§8), missing identifier definitions (§6.0)

### `docs/orchestrator-v1.schema.json`
- **Status**: ✅ Comprehensive, mostly consistent
- **Issues**: `SessionMemory` vs `M2BTranscriptionState` ambiguity, `metadataPatch.ops` too permissive

### `docs/orchestrator-v1.examples.json`
- **Status**: ⚠️ Good coverage but missing required fields
- **Issues**: Missing `disectorPlanId`, uses deprecated `sessionId`

### `docs/api-contracts.md`
- **Status**: ✅ Aligned with V1
- **Issues**: Self-reference in retry policy (§5.1)

### `docs/ux-contract.md`
- **Status**: ✅ Consistent with V1
- **Issues**: None found

### `docs/current-state.md`
- **Status**: ⚠️ Implementation status doc (not spec)
- **Issues**: V1.1 target section may reference future concepts

### `docs/T-Task.md`
- **Status**: ⚠️ Engineering trace (not spec)
- **Issues**: Mixes V7 traces with V1 workstreams (confusing but not blocking)

### `docs/AGENTS.md`
- **Status**: ✅ Clear precedence rules
- **Issues**: None found

---

## Conclusion

The documentation system is **fundamentally sound** and suitable for AI-agent consumption, but requires **P0 fixes** to eliminate schema-examples mismatches and contract ambiguities. With these fixes, the system will achieve **8.5+/10** on the AI-agent friendliness scorecard.

**Recommended Action**: Apply P0 fixes immediately, then P1 fixes within one sprint, P2 fixes as time permits.


