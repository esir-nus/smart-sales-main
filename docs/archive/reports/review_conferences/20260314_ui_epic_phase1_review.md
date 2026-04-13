# 📋 Review Conference Report

**Subject**: UI Skin Modernization - Phase 1 (Component Rewrite) Implementation Plan
**Panel**: 
1. `/01-senior-reviewr` (Chair)
2. `/06-audit` (Evidence-based Audit)
3. `/08-ux-specialist` (UI/UX)

---

### Panel Input Summary

#### `/06-audit` — Evidence-based Audit
- **Insight 1 (Missing Interface SOT)**: The plan states `docs/cerb/ui/agent-chat-screen/spec.md` is the owning spec but notes it's implicit. As per the Feature Dev Planner rules (Phase 0), "ONE task = ONE spec.md + ONE interface.md. No exceptions." If the spec doesn't exist or is implicit, the planning must halt. We must find the correct existing UI spec, or use `/cerb-ui-template` to create it first.
- **Insight 2 (Current State)**: `AgentChatScreen.kt` still has `viewModel: IAgentViewModel = hiltViewModel<AgentViewModel>()` in its signature.

#### `/08-ux-specialist` — UI/UX
- **Insight 1 (Preview Rigor)**: Adding `@Preview` functions for `Idle`, `Thinking`, `ActiveTaskHorizon`, and `MascotInteraction` is excellent. It forces us to see how the UI reacts to different states of the `FakeAgentViewModel` without needing the backend or a physical device.
- **Insight 2 (State Passing)**: If we want pure components, we might go further: Instead of passing `IAgentViewModel` into `AgentChatScreen`, we could pass the raw state (`UiState`, `List<ChatMessage>`, etc.) and lambda callbacks. This makes the UI 100% agnostic to the ViewModel architecture. However, since the Epic specifically mentions "consuming `IAgentViewModel`", keeping the interface is aligned with the stated tracker goal.

#### `/01-senior-reviewr` — Pragmatism & Vibe Coding (Chair)
- **Insight 1 (The Spec Gate)**: `/06-audit` is right. The Anti-Drift Audit requires a hard link to an existing Cerb spec. The tracker says "Docs-First `IAgentViewModel` Contract". We must verify if `prism-ui-ux-contract.md` or a specific UI Cerb doc explicitly owns this.
- **Insight 2 (DI Leakage)**: `AgentChatScreen(viewModel: IAgentViewModel = hiltViewModel<AgentViewModel>())` is a classic Compose trap. The default argument leaks the Dagger/Hilt dependency (`AgentViewModel`) into the component seal. The fix is to provide the concrete implementation at the navigation level (inside `AgentShell`), and let `AgentChatScreen` just accept `viewModel: IAgentViewModel`.
- **Insight 3 (Mascot Scope)**: Phase 1 says "Nuke and pave existing screens (Chat, Agent States)". Does this include rewriting `MascotOverlay.kt`? The plan should explicitly restrict the scope to `AgentChatScreen` and `AgentShell` for this specific PR to avoid explosion.

---

### 🔴 Hard No (Consensus)
- **Implicit Spec**: You cannot declare an "Implicit UI Spec". If there is no specific `agent-chat-screen/spec.md`, you must route to the master UI contract `docs/specs/prism-ui-ux-contract.md` as the official owning spec, and cite the exact section defining the Chat Screen.

### 🟡 Yellow Flags
- **Scope Creep**: Explicitly bound what "existing screens" means. Limit Phase 1 to `AgentChatScreen` and its immediate child components (like `ResponseBubble`, `ThinkingCanvas`) getting proper previews.

### 🟢 Good Calls
- **Fake-Driven Previews**: Mandating 4 distinct `@Preview` states using the `FakeAgentViewModel` is exactly how we prove the "Parallel Proving Ground".

### 💡 Senior's Synthesis & Recommendation
The rewrite plan is directionally correct but lacks the required specification grounding. 

**What I'd Actually Do:**
1. **Fix the Spec Declaration**: Change the owning spec to `docs/specs/prism-ui-ux-contract.md` (or the equivalent SOT for UI components). 
2. **Clarify the Compose Signature**: Explicitly state that `hiltViewModel()` must be entirely removed from `AgentChatScreen.kt`'s default parameters. It must look like `fun AgentChatScreen(viewModel: IAgentViewModel, ...)` or `fun AgentChatScreen(state: AgentChatState, actions: AgentChatActions)`. Since the epic mandates `IAgentViewModel`, use the former.
3. **Isolate Previews**: Put the previews in a dedicated file if they get too large, or group them at the bottom.

---

### 🔧 Prescribed Tools
1. **Update Implementation Plan**: Revise the `implementation_plan.md` to fix the Owning Spec declaration and clarify the removal of `hiltViewModel()` from the component signature.
