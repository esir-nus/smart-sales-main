---
description: Perform an evidence-based code audit to reduce guessing and prevent hallucinations
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
