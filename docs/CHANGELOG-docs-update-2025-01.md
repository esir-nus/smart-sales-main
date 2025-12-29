# Documentation Update Changelog - 2025-01-27

## Summary
Updated documentation system to ensure V1 is CURRENT and consistent, V7 is ARCHIVED, and all contracts/examples are unambiguous and validate.

## Files Modified

### 1. docs/orchestrator-v1.schema.json
- **M2B Schema Unification**: Replaced generic `items[]` and `sourcePointers[]` with canonical `chapters[]` and `keyPoints[]` structure
- **New Definitions**: Added `M2BChapter` and `M2BKeyPoint` schema definitions with required source pointers
- **Schema Header Fix**: Updated `$comment` to use integer `schemaVersion: 1` instead of `SchemaVersion: 1.1.0` and changed idempotency key to use `schemaVersion` instead of `version`
- **M2B Structure**: `M2BTranscriptionState` now requires `chapters[]` and `keyPoints[]` arrays, each entry must include `audioAssetId` + `recordingSessionId` + anchor (chapterId and/or timeRange)

### 2. docs/orchestrator-v1.examples.json
- **M2B Example Update**: Rewrote `M2BTranscriptionState` example to match new schema structure
- **Structure Changes**: 
  - Chapters now have `audioAssetId` and `recordingSessionId` at top level (not nested in `source`)
  - KeyPoints use `timeRange` object instead of nested `source` object
  - Removed deprecated `source` nesting pattern

### 3. docs/Orchestrator-V1.md
- **Identifier Glossary** (§6.0): Added comprehensive table with all identifiers (chatSessionId, turnId, audioAssetId, recordingSessionId, disectorPlanId, batchAssetId, batchIndex, publishedPrefixBatchIndex, jobId)
- **M2B Section Update** (§6.2): Updated to describe canonical `chapters[]` + `keyPoints[]` structure with hard rules about source pointers
- **Tingwu Retry Policy** (§8.1): Added dedicated subsection specifying:
  - maxRetries = 3 (default)
  - Retryable errors (429, 5xx, timeouts) referencing api-contracts.md
  - Terminal behavior after retries exhausted
  - Partial failure handling (prefix stops at gap, later batches can run but not publish)
- **State Recovery Rules** (§12.1): Added section explaining recovery of PublisherState, TingwuJobState, SessionMemory, DisectorPlan, and partial failure impact
- **Versioning Glossary** (§4.1): Added section explaining `schemaVersion` (JSON schema versioning) vs artifact-specific versioning, migration strategy

### 4. docs/style-guide.md
- **V4 Reference Fix**: Updated line 428 to reference `docs/Orchestrator-V1.md` (CURRENT) instead of `docs/archived/Orchestrator-MetadataHub-V4.md`

## Archive Consistency Verification

### V7 Files Status
- ✅ `docs/archived/Orchestrator-MetadataHub-V7.md` - Already archived
- ✅ `docs/archived/metahub-schema-v7.json` - Already archived
- ✅ All references point to `docs/archived/` paths

### References Updated
- `docs/AGENTS.md`: Already references `docs/archived/Orchestrator-MetadataHub-V7.md` ✓
- `docs/T-Task.md`: Already references `docs/archived/Orchestrator-MetadataHub-V7.md` ✓
- `docs/style-guide.md`: Updated to remove V4 reference ✓

## Validation Results

### JSON Validation
- ✅ `docs/orchestrator-v1.schema.json` - Valid JSON
- ✅ `docs/orchestrator-v1.examples.json` - Valid JSON

### Schema-Examples Alignment
- ✅ M2B example structure matches new schema definition
- ✅ All required fields present in examples
- ✅ Source pointers enforced in schema and examples

## Key Changes Summary

1. **M2B Canonical Structure**: Adopted Option B (chapters[] + keyPoints[]) as ONLY canonical structure
2. **Tingwu Retry**: Added explicit terminal behavior specification
3. **Identifier Glossary**: Centralized all identifier definitions with lifecycle rules
4. **State Recovery**: Documented recovery rules for all stateful components
5. **Versioning Clarity**: Unified on `schemaVersion` (integer) with clear policy
6. **Archive Consistency**: Verified all V7 references point to archived/ paths

## Files NOT Modified (as requested)
- `docs/role-contract.md` - Unchanged
- `docs/orchestrator-sample-response.md` - Unchanged

