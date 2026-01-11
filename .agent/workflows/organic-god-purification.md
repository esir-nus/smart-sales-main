---
description: Organic God File Purification - Build new features cleanly to shrink legacy God Files
---

# Organic God File Purification Protocol

**Goal**: Shrink God Files (e.g., `TingwuRunner`, `HomeScreen`) by extracting business logic into clean, spec-compliant components during feature development.

**Philosophy**: "Don't refactor the God File; starve it."
When building a feature that touches a God File, **do not add code to the God File**. Instead, build a separate clean component and make the God File a client of that component.

---

## The Protocol

### 1. 🛑 Stop & Identify
Before writing code, ask:
- **"Am I about to add logic to a file > 500 lines?"**
- **"Does similar logic already exist in this God File?"**

If YES, trigger this workflow.

### 2. 🏛️ Design the Clean Component
Design a new, isolated component that handles *strictly* the new feature's domain logic.
- **Location**: Use the correct domain package (e.g., `data/aicore/tingwu/orchestrator/`).
- **Input/Output**: Define clean interfaces/data classes. Do not reuse God File internal state.
- **Dependencies**: Inject only what is needed. Avoid circular deps with the God File.

### 3. 🏗️ Build Isolation First
Implement the new component + unit tests **outside** the God File.
- Write the class.
- Write the tests.
- Verify it works in isolation.

### 4. 💉 Inject & Delegate (The "Strangler" Move)
Only *after* the polished component exists:
1.  **Inject** the new component into the God File's constructor.
2.  **Delegate** the call: Replace the inline logic (or the place where you *would* have put inline logic) with a single method call to your new component.
3.  **Delete** (Optional but Encouraged): If the new component replaces existing legacy logic in the God File, **delete the legacy code immediately**.

### 5. 📉 Verify Shrinkage
- **Lines Added to God File**: Should be minimal (injection + delegation lines only).
- **Lines Removed from God File**: Should be > 0 if replacing legacy logic.
- **New Lines in Clean Component**: Unlimited (it's clean code).

---

## Example: Adding Multi-Batch to `TingwuRunner` (God Class)

### ❌ The Anti-Pattern (Feeding the God)
Adding a `submitMultiBatch` private method inside `TingwuRunner.kt`.
- **Result**: `TingwuRunner` grows from 1300 -> 1400 lines. Complexity increases.

### ✅ The Purification Pattern
1.  **Build**: Create `TingwuMultiBatchOrchestrator` (New class).
2.  **Test**: Test `TingwuMultiBatchOrchestrator` independently.
3.  **Inject**: Add `orchestrator` to `TingwuRunner` constructor.
4.  **Delegate**:
    ```kotlin
    // In TingwuRunner.submit()
    if (isMultiBatch) {
       return multiBatchOrchestrator.submit(...) // 1 line of logic
    }
    ```
- **Result**: `TingwuRunner` grows by 5 lines (wiring). New complexity lives in a clean 100-line file.

---

## Checklist for Reviewers

- [ ] **Did we create a new file?** (If not, why?)
- [ ] **Did we delete code from the God File?** (Bonus points)
- [ ] **Is the God File just delegating?**
- [ ] **Are the new tests testing the new component, not the God File?**
