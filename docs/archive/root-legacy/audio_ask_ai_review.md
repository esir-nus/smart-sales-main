# Review Conference: Audio "Ask AI" Context Handoff

**Subject**: Passing Tingwu Smart Summary to Analyst Mode via "Ask AI" button.
**Panel**:
1. `/01-senior-reviewr` (Chair) — Architecture sanity, patterns
2. `/08-ux-specialist` — Interaction flows, user experience
3. `/17-lattice-review` — Layer compliance, contract integrity

---

## Conference Agenda

**Subject**: Currently, `AudioViewModel.onAskAi(audioId)` creates a new session linked to the audio ID. We need to decide if and how to pass the `TingwuSmartSummary` (or specific fields like Keywords, Key Sentences, QA, etc.) as the initial `user input` or context prompt when transitioning to Analyst Mode, avoiding the raw transcription dump.

**Questions for Each Panelist**:

- **`/08-ux-specialist`**: The user states that "summary is just a longer user input in standard workflow". When the user clicks "Ask AI", they transition to the chat. Should we pre-fill the chat input, send an invisible system message, or have the AI send the first message acknowledging the summary? What provides the best seamless transition?

- **`/17-lattice-review`**: How should `AudioViewModel` pass this data to the `PrismOrchestrator` or `HistoryRepository`? Does `HistoryRepository.createSession` need a new parameter for `initialContext: String?`, or should `AudioViewModel` directly dispatch an `Intent.UserInput` to the `PrismViewModel`/`PrismOrchestrator` using the newly created `sessionId`?

- **`/01-senior-reviewr`**: Is treating the summary as "just a longer user input" the most robust architectural choice? Or should the `PrismOrchestrator`'s `ContextBuilder` be responsible for looking up the `linkedAudioId` and fetching the summary automatically when the Analyst session starts, keeping the `AudioViewModel` purely focused on UI state and navigation?
