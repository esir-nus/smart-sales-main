# Prism UX Narrative: The Executive Desk & The Invisible AI

> **"The best interface is the one that disappears when you're working, and appears exactly when you need it."**

## 1. The Core Metaphor: The Executive Desk

Imagine a CEO's desk. It is not covered in sticky notes of yesterday's meetings (a "Session List"). It is clean.

*    **The Base Layer (Home Hero):** A clean surface. "Good Afternoon, Frank." A breathing aura that says "I am listening." This is the **Zero State**.
*   **The Drawers (Contexts):**
    *   **Top Drawer (Scheduler):** Your Agenda. It's the first thing you check in the morning.
    *   **Right Drawer (Tools/Artifacts):** Where you keep your documents (PDFs, Reports). Context-aware tools that slide out when needed.
    *   **Bottom Drawer (Audio):** Your dictation recorder. Always ready to capture.
    *   **Left Drawer (Archives):** The filing cabinet (History). Tucked away, not blocking.

## 2. The Flow: Agenda First, Then Focus

### The "Good Morning" Sequence
Most apps dump you into a list of old chats. Prism knows you have a job to do.
1.  **Launch:** The **Scheduler Drawer** auto-drops. "Here is your day."
2.  **Review:** You check your 10:00 AM meeting. You see a conflict. You resolve it.
3.  **Dismiss:** You swipe the scheduler up.
4.  **Result:** You are left with a **Clean Desk**. The mental load of the schedule is gone. You are ready to create.

### The "Capture" Flow
You have an idea while walking.
1.  **Trigger:** You pull the bottom drawer (or tap Mic).
2.  **Capture:** You speak. "Note for Q4 budget..."
3.  **Invisible Work:** The Audio Drawer slides down. You don't wait for transcription. The system handles it in the background (`[Box] -> [Cloud] -> [Memory]`).
4.  **Result:** You trust it's there.

## 3. The "Two Brains" Model: Coach vs. Analyst

We explicitly separate "Fast Thinking" from "Slow Thinking".

### The Coach (Fast, Purple)
*   **Feel:** Conversational, empathetic, immediate.
*   **UI:** No "Thinking Box". Bubbles appear as he speaks.
*   **Use Case:** "Remind me who Zhang is." "Draft a quick reply."
*   **Friction:** Zero. It feels like chatting with a smart colleague.

### The Analyst (Deep, Blue)
*   **Feel:** Systematic, structured, thorough.
*   **UI:** The **Thinking Box** (`🧠`) appears. You *see* the brain working. "Reading PDF...", "Comparing dates...".
*   **Use Case:** "Analyze this year's sales trends." "Build a recovery plan."
*   **Friction:** Intentional. The delay signals *depth*. The **Plan Card** creates a shared workspace.

## 4. Friction Analysis (Why we killed the Session List)

The old "Session List Home" was a **Blocking State**.
*   *User Thought:* "I want to work."
*   *Old UI:* "Here are 50 things you did yesterday. Pick one or click New."
*   *Friction:* Decision paralysis.

The new "Executive Desk":
*   *User Thought:* "I want to work."
*   *New UI:* "Type to start." (Or drop a file).
*   *Friction:* **Zero.** The context is fresh by default (`[➕]` logic implicitly built-in). History is tucked away (`[☰]`), available but not blocking.

## 5. The "Invisible AI" (Relevancy Library)

When you type "Send that report to him":
1.  **System:** Checks **Relevancy Library**.
2.  **Context:** "Him" = "Zhang" (based on last meeting). "Report" = "Q4_Budget.pdf" (created yesterday).
3.  **Action:** It drafts the email with the attachment.
4.  **User Experience:** Magic. You didn't select the file. You didn't name the person. It just worked.

---

**Summary:** Prism is not a chatbot. It is an **Operating System for your Intent**. It anticipates (Scheduler), listens (Audio), thinks (Analyst), and remembers (Relevancy), all while keeping your desk clean.
