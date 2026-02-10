---
description: Anti-laziness standards for verification
trigger: always_on
---

# Anti-Laziness Standards (防偷懒铁律)

> **Core Principle**: Verification must be **concrete**, **comprehensive**, and **negative**.
> "Looks good" is not a verification.

## 1. Concrete Standards (具体标准)
- **Never** say "verified manually."
- **Always** provide the exact command run and its output.
- **Rule**: If you can't paste the `grep` or test output, you didn't verify it.

## 2. Comprehensive Check (全面检查)
- **Unit Tests**: Must run the *full* suite for the affected module, not just one test case.
- **Spec Check**: Verify *every* field in a data class against the spec, not just the one you changed.
- **Output**: Report *all* failures, not just the first one.

## 3. Negative Testing (负向测试)
- **Success is cheap.** Proving it *can't* fail is the goal.
- **Always** test:
  - `null` / `empty` input
  - Network failure / offline state
  - Invalid IDs / missing resources
- **Rule**: If you haven't seen it fail, you don't know if it works.

## 4. Explicit Instructions (显示指令)
- You **MUST** treat verification as a hostile act against your own code.
- You **MUST** assume your code is broken until proven otherwise.
- You **MUST** use the `Acceptance Team` workflow before delivering distinct chunks of work.

## 5. The "Whitebox" Trap
- **Don't** use your internal knowledge of *how* it was built to skip checks.
- **Do** test as if you are an external QA engineer who hates the developer.
