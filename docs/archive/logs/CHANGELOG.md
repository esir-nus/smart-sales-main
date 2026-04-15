# Documentation Changelog

Spec versioning history for Orchestrator-V1 and related contracts.

> For architecture refactoring history (Wave 1-19), see [REFACTORING-LOG.md](./REFACTORING-LOG.md).

---

## V1.3.0 - 2026-01-09

### Summary
Completed M1 Phase 2 (Tingwu Chapters & Summary Display). Implemented "Option A" (Inline Markdown) for immediate visualization of rich transcription metadata.

### Features
- **Rich Transcript Display**: Chat now renders:
  - `## 摘要（Summarization）`: Full summary with paragraphs, speaker lists, and Q&A.
  - `## 章节（AutoChapters）`: Timeline with timestamps, headlines, and summaries.
  - `## 自定义转写（CustomPrompt）`: Raw JSON result debugging.
- **Tingwu API Integration**:
  - Updated parsers to extract `Headline` and `Summary` fields (previously missing).
  - Wired `composeFinalMarkdown` to generate comprehensive markdown output.

---

## V1.2.0 - 2026-01-05

### Summary
Implemented missing V1 sections that were planned but not yet present in spec.

### Changes to docs/Orchestrator-V1.md
- **Version**: Updated from 1.1.0 → 1.2.0
- **§4.1 Versioning Glossary**: Added definitions for `schemaVersion` vs `version`, migration policy
- **§8.1 Tingwu Runner Retry Policy**: Added retry rules (maxRetries=3, backoff, error categories, terminal behavior, partial failures)
- **§13/Appendix D State Recovery Rules**: Added recovery rules for PublisherState, TingwuJobState, DisectorPlan, M2B
- **§14 Future Extensions**: Renumbered from §13

### Cross-Reference Fix
- `api-contracts.md` §5.1 reference to "Section 8.1" now resolves

---

## V1.1.0 - 2025-01-27

### Summary
Updated documentation system to ensure V1 is CURRENT and consistent, V7 is ARCHIVED, and all contracts/examples are unambiguous and validate.

### Files Modified

#### docs/orchestrator-v1.schema.json
- **M2B Schema Unification**: Replaced generic `items[]` and `sourcePointers[]` with canonical `chapters[]` and `keyPoints[]` structure
- **New Definitions**: Added `M2BChapter` and `M2BKeyPoint` schema definitions with required source pointers
- **Schema Header Fix**: Updated `$comment` to use integer `schemaVersion: 1` instead of `SchemaVersion: 1.1.0` and changed idempotency key to use `schemaVersion` instead of `version`
- **M2B Structure**: `M2BTranscriptionState` now requires `chapters[]` and `keyPoints[]` arrays, each entry must include `audioAssetId` + `recordingSessionId` + anchor (chapterId and/or timeRange)

#### docs/orchestrator-v1.examples.json
- **M2B Example Update**: Rewrote `M2BTranscriptionState` example to match new schema structure
- **Structure Changes**: 
  - Chapters now have `audioAssetId` and `recordingSessionId` at top level (not nested in `source`)
  - KeyPoints use `timeRange` object instead of nested `source` object
  - Removed deprecated `source` nesting pattern

#### docs/Orchestrator-V1.md
- **Identifier Glossary** (§6.0): Added comprehensive table with all identifiers (chatSessionId, turnId, audioAssetId, recordingSessionId, disectorPlanId, batchAssetId, batchIndex, publishedPrefixBatchIndex, jobId)
- **M2B Section Update** (§6.2): Updated to describe canonical `chapters[]` + `keyPoints[]` structure with hard rules about source pointers
- **Tingwu Retry Policy** (§8.1): Added dedicated subsection specifying:
  - maxRetries = 3 (default)
  - Retryable errors (429, 5xx, timeouts) referencing api-contracts.md
  - Terminal behavior after retries exhausted
  - Partial failure handling (prefix stops at gap, later batches can run but not publish)
- **State Recovery Rules** (§12.1): Added section explaining recovery of PublisherState, TingwuJobState, SessionMemory, DisectorPlan, and partial failure impact
- **Versioning Glossary** (§4.1): Added section explaining `schemaVersion` (JSON schema versioning) vs artifact-specific versioning, migration strategy

#### docs/style-guide.md
- **V4 Reference Fix**: Updated line 428 to reference `docs/Orchestrator-V1.md` (CURRENT) instead of `docs/archived/Orchestrator-MetadataHub-V4.md`

### Archive Consistency Verification

#### V7 Files Status
- ✅ `docs/archived/Orchestrator-MetadataHub-V7.md` - Already archived
- ✅ `docs/archived/metahub-schema-v7.json` - Already archived
- ✅ All references point to `docs/archived/` paths

#### References Updated
- `docs/AGENTS.md`: Already references `docs/archived/Orchestrator-MetadataHub-V7.md` ✓
- `docs/style-guide.md`: Updated to remove V4 reference ✓

### Validation Results

#### JSON Validation
- ✅ `docs/orchestrator-v1.schema.json` - Valid JSON
- ✅ `docs/orchestrator-v1.examples.json` - Valid JSON

#### Schema-Examples Alignment
- ✅ M2B example structure matches new schema definition
- ✅ All required fields present in examples
- ✅ Source pointers enforced in schema and examples

### Key Changes Summary

1. **M2B Canonical Structure**: Adopted Option B (chapters[] + keyPoints[]) as ONLY canonical structure
2. **Tingwu Retry**: Added explicit terminal behavior specification
3. **Identifier Glossary**: Centralized all identifier definitions with lifecycle rules
4. **State Recovery**: Documented recovery rules for all stateful components
5. **Versioning Clarity**: Unified on `schemaVersion` (integer) with clear policy
6. **Archive Consistency**: Verified all V7 references point to archived/ paths
