---
name: plain-chinese-change-explainer
description: Explain technical changes, bug fixes, refactors, or delivery work in plain Simplified Chinese for non-technical or mixed audiences. Use when the user asks why a task was more work than it looked, what changed behind the scenes, what workload the change caused, or wants a Chinese explanation for a boss, PM, tester, or team chat.
---

# Plain Chinese Change Explainer

## Overview

Use this skill when the user wants a grounded, human-readable Chinese explanation of engineering work.
The goal is not to restate code diffs. The goal is to explain, in plain language, what looked small, why it was not small, what changed underneath, and what that means operationally.

## When To Use

Trigger this skill when the user asks for things like:

- "用中文解释这次改动到底做了什么"
- "为什么这个任务工作量这么大"
- "把这些技术改动讲给老板/PM/测试听"
- "解释背后改了哪些东西，不要太技术"
- "把这次 bug fix 的实际工作量讲清楚"
- "说明表面问题和真正改动之间的差别"

Do not use this skill for:

- pure translation with no explanation layer
- highly formal design/spec writing
- investor-style business storytelling that is not grounded in actual technical work

## Input Discipline

Before explaining, identify the real facts first:

- What was the visible user problem?
- What systems or layers actually changed?
- Was the work a single-point fix or a chain-of-contract fix?
- What new checks, logging, tests, or docs were added?
- What is solved now, and what is still pending?

If the evidence is incomplete, say so plainly in Chinese. Do not fake certainty.

## Default Output Pattern

Default to this shape unless the user asks for something else:

1. What it looked like on the surface
2. Why it was actually more work
3. What changed behind the scenes
4. Why it could not be solved by changing only one place
5. One-sentence plain-language summary

Good phrasing patterns:

- "这件事表面上看像是在修……，但实际上是在重做……"
- "真正的工作量不在表层提示，而在底层判断规则 / 状态流 / 校验链路 / 测试补齐"
- "以前系统是在‘看起来成功就放行’，现在改成‘必须真的验证成功才算成功’"
- "这不是改一个按钮，而是改一条完整链路"
- "背后不仅改了代码，还补了日志、错误分类、测试和文档"

## Writing Rules

- Always write in Simplified Chinese unless the user asks otherwise.
- Prefer plain words over jargon. If technical terms are necessary, immediately explain them in everyday language.
- Explain cause and effect, not just file changes.
- Separate "表面问题" from "真正改动".
- Distinguish "已经解决" from "还需要验证".
- Do not exaggerate. Make the workload sound real, not dramatic.
- Prefer short paragraphs or flat bullets over dense technical dump.
- When useful, use contrast sentences:
  - "不是……而是……"
  - "不只是……还包括……"
  - "以前……现在……"

## Audience Adaptation

If the audience is clear, tilt the explanation:

- Boss / manager:
  - emphasize why it was not a one-line fix
  - emphasize risk reduction, false-success removal, and verification cost
  - keep implementation details minimal
- PM:
  - emphasize behavior contract, user impact, edge cases, and what changed in flow
  - mention follow-up validation if still pending
- Tester / QA:
  - emphasize what behavior changed, what should now pass/fail, and what to verify on device
  - mention logs and evidence when relevant
- Teammate / engineer:
  - keep the same plain language style, but allow one extra layer of technical precision
  - mention major layers touched without turning the answer into a changelog

## Optional Variants

When the user asks for a specific tone, adapt:

- Brief version:
  - 1 short paragraph + 1 summary sentence
- Chat / team-message version:
  - slightly more conversational
  - still factual and grounded
- Report version:
  - use 3 to 5 flat bullets or short titled sections
- "Explain like I’m not technical":
  - avoid file names unless they materially help
  - lean harder on visible effect and hidden chain-of-work explanation

## Example Prompt Patterns

Examples that should work well with this skill:

- "Use $plain-chinese-change-explainer to explain why this fix was more work than it looked."
- "Use $plain-chinese-change-explainer and explain these code changes for my boss in plain Chinese."
- "Use $plain-chinese-change-explainer to describe the hidden workload behind this bug fix."
- "Use $plain-chinese-change-explainer to turn this technical diff into a tester-facing Chinese summary."
