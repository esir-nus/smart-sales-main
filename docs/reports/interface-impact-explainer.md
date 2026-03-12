## How the "Multiple Choice" SSD Doctrine Changes Interfaces

### Step 1: The Old Way (Behavioral Interfaces)
**What**: Teams define interfaces based on *actions* (e.g., `Pipeline.process(text)`). The CRM team hopes the LLM team remembers to send the right JSON for a Deal Stage.
**Result**: Tight coupling. If the CRM team adds a `currency` field, they have to call a meeting with the LLM team to update the prompt.

### Step 2: The New Way (Data-Driven Interfaces)
**What**: The CRM team defines a pure Kotlin blueprint: `data class QuoteMutation(id, amount, currency)`. This blueprint *is* the interface.
**Result**: The LLM team doesn't write a custom prompt. Their `PromptCompiler` auto-reads the CRM blueprint and turns it into the Multiple Choice options for the AI.

### Step 3: The Universal Handshake
**What**: Now, when the 3rd-party Plugin team wants to update a quote, they don't look up a REST API document. They just import the exact same `QuoteMutation` blueprint and send it.
**Result**: One interface rules them all. The LLM, the Plugins, and the Mobile UI all use the exact same SSD-defined data object.

## 🗺️ Visual Flow Diagram

```text
       ┌────────────────────────┐
       │   CRM Team defines     │
       │   QuoteMutation        │
       └─────┬────────────┬─────┘
             │            │
 ┌───────────▼─┐        ┌─▼───────────┐
 │ LLM Prompt  │        │ Plugin API  │
 │ generates   │        │ requires    │
 │ JSON rules  │        │ Payload     │
 │ from Quote  │        │ from Quote  │
 └─────────────┘        └─────────────┘
```
