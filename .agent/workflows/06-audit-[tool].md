---
description: Perform an evidence-based code audit to reduce guessing and prevent hallucinations
last_reviewed: 2026-04-13
---

# Evidence-Based Code Audit

Use this workflow to ground the agent in reality before making changes or answering complex questions. This reduces hallucinations by forcing verification of file existence, logic flow, and test coverage.

## When to Use
- **Before** making significant refactors.
- **When** the codebase structure is unfamiliar.
- **If** the agent feels "lost" or is making assumptions.
- **To verify** existing implementation details before extending them.

---

## Audit Protocol

### 1. Scope Identification
Identify the specific files, classes, or features relevant to the request.
- **Question**: What specific code are we talking about?
- **Action**: List the target files/classes.

### 2. Existence Proof (The "Anti-Hallucination" Check)
Verify that the target files and symbols *actually exist* where you think they do.
- **Tools**: `find_by_name`, `ls`, `grep_search`.
- **Rule**: Never assume a file path or package structure.
- **Output**:
  - `[✅ Found]` / `[❌ Missing]` for each file.
  - Correct absolute paths.

### 3. Logic Tracing (The "Read Before Write" Check)
Read the actual code to understand the implementation, not just the interface.
- **Tools**: `view_file`, `view_code_item`.
- **Action**:
  - Read the *full* file content (or relevant sections).
  - Trace dependencies (imports).
  - Check for specific handling (e.g., error cases, edge cases).
- **Rule**: Do not guess function signatures or return types.

### 4. Test Verification
Check if the logic is covered by tests.
- **Tools**: `find_by_name` (look for `*Test.kt`), `grep_search`.
- **Action**:
  - Locate corresponding test files.
  - Verify if relevant scenarios are covered.
- **Rule**: Untested code is a "Yellow Flag" that requires extra caution.

---

## Audit Report Template

Generate a report using this format:

```markdown
# 🛡️ Code Audit: [Topic/Feature]

## 1. Context & Scope
- **Target**: [Feature/Class Name]
- **Goal**: [Refactor/Extend/Fix]

## 2. Existence Verification
- [x] `FileA.kt`: Found at `.../path/to/FileA.kt`
- [ ] `FileB.kt`: ❌ NOT FOUND (checked `feature/chat` and `core`)
- [x] `functionA()`: Found in `FileA.kt` line 45

## 3. Logic Analysis
- **Implementation**: [Brief summary of how it *actually* works, based on file read]
- **Dependencies**: [List key dependencies]
- **Gaps/Risks**: [e.g., Hardcoded strings, swallowing exceptions]

## 4. Test Coverage
- **Test File**: `FileATest.kt` (Found/Missing)
- **Coverage**: [High/Partial/None] - Covers happy path? Edge cases?

## 5. Conclusion
- **Ready to Proceed?**: [YES / NO / CAUTION]
- **Missing Information**: [What do we still need to find out?]
```

---

## Example Usage

**User Request**: "Refactor the `MessageProcessor` to handle images."

**Agent Action**:
1. Run `find_by_name Pattern="MessageProcessor"` -> Found `.../domain/MessageProcessor.kt`.
2. Run `view_file .../domain/MessageProcessor.kt`.
3. Run `find_by_name Pattern="MessageProcessorTest"`.
4. **Generate Report**:
   - "Confirmed `MessageProcessor` exists in `domain`. It currently handles `Text` and `Audio` types. `Image` type is missing from the `MessageType` enum. Tests exist in `.../domain/test/`, covering text logic but not audio."

---

## Literal Spec Alignment Audit (REQUIRED for UI/UX Implementation)

> **When to Use**: Every time you audit code that implements a spec section (e.g., `prism-ui-ux-contract.md §1.4`).

### The Rule: No Summarizing. Quote Verbatim.

The agent MUST NOT summarize spec requirements. Extract **exact phrases** from the spec and compare against **exact code**.

### Protocol

1. **Read the spec section** (e.g., `view_file prism-ui-ux-contract.md` lines 356-424).
2. **Extract verbatim requirements** — copy the exact strings, patterns, and names.
3. **Read the code** and extract the **actual values** used.
4. **Create Literal Comparison Table**.

### Literal Comparison Table Template

```markdown
## Spec Alignment: [Component] vs [Spec §X.X]

| Spec Line # | Spec Says (VERBATIM) | Code Says (VERBATIM) | Match? |
|-------------|----------------------|----------------------|--------|
| L397 | Groups: `📌 置顶`, `📅 今天`, `🗓️ 最近30天`, `🗂️ YYYY-MM` | Groups: `📌 置顶`, `📅 今天`, `📅 昨天`, `🗓️ 过去7天` | ❌ NO |
| L399 | Format: `[Client/Person Name]_[Summary (max 6 chars)]` | `clientName + "_" + summary.take(6)` | ✅ YES |
| L403 | Single-line, two-tone | `Row { Text(...Bold), Text("_"), Text(...Gray) }` | ✅ YES |
```

### Hard Rules

| Condition | Action |
|-----------|--------|
| Any row is ❌ | **HARD NO** — Must fix before claiming alignment |
| Spec uses specific strings (e.g., `"最近30天"`) | Code must use **EXACT same string** |
| Spec defines a pattern (e.g., `[X]_[Y]`) | Code must follow pattern **literally** |

### Evidence Quality for Spec Alignment

| Level | Criteria |
|-------|----------|
| **HIGH** | 5+ spec lines compared verbatim, all ✅ |
| **MEDIUM** | 3-4 spec lines compared, 1-2 ⚠️ minor deviations |
| **LOW** | Summary-only, no verbatim comparison |
| **FAIL** | Any ❌ in critical spec requirement |

---

## Integration with Implementation Plan Review

When reviewing an implementation plan that modifies UI/UX code:

1. **Identify Spec Section**: Which section of the spec is this implementing?
2. **Run Literal Spec Alignment Audit** on the proposed changes.
3. **Block Approval** if any ❌ exists in the Literal Comparison Table.

> **Motto**: "If the spec says `最近30天`, the code must say `最近30天`. No synonyms. No approximations."
