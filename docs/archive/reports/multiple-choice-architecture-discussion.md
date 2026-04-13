# Multiple Choice Architecture: SSD-Centric Design Analysis

## 1. Input Modalities vs. "Intents"
Your observation is 100% correct. Voice, once passed through ASR, is just text. The "reusability" isn't about *how* the text got there (microphone vs keyboard vs plugin API). The reusability is about the **Destination (The SSD Entity)**.

In a normal architecture, if a Plugin wants to update a Quote, the Plugin developer has to learn your custom `UpdateQuoteInterface`. If the LLM wants to update a Quote, it has to learn the custom `LLM_JSON_Quote_Schema`. 

In your philosophy, the `Quote` entity is the Universal Contract. The LLM output, the Plugin API, and the UI state all map to the exact same SSD model. You don't write "Prompt JSON" and "Plugin API JSON". You just expose the SSD's "Multiple Choice" options to everything. 

## 2. Do we still need a Linter?
**Yes, but its job completely changes.**

Currently, your Linters (like `SchedulerLinter`) are doing "Essay Grading." They are reading unpredictable JSON and applying heavy regex/Math to guess what the AI meant. They are complex and brittle.

In the Multiple Choice world, the Linter becomes a simple **Type Checker (Deserializer)**.
*   **Old Linter Job:** "Did the AI say 'tomorrow' or 'next day'? Let me calculate the timestamp. Did it spell `deal_stage` correctly?"
*   **New Linter Job:** `kotlinx.serialization.decodeFromString<QuoteMutation>(llmOutput)`. If it deserializes successfully, it's valid. If it fails, you throw an exception. 

The Linter goes from being 300 lines of complex string-parsing logic to 1 line of Kotlin serialization. The complexity moves out of the Linter and into the strictness of the Prompt Compiler.

## 3. Impact on Interfaces (The `interface-map.md`)
This has a MASSIVE, positive impact on your interfaces.

Right now, your interfaces are largely defined by *how* things happen (e.g., "The Orchestrator calls the Pipeline"). 

In your SSD-centric philosophy, your interfaces become **Data-Driven Contracts**. 
1.  **The API Boundary is the Entity:** The interface between the LLM Module and the CRM Module is no longer a method call `processText()`. The interface becomes the `QuoteMutation` data class itself.
2.  **Decentralized Development:** If Team A owns the CRM and Team B owns the LLM Pipeline, Team A just publishes their Kotlin Data Classes. Team B's Prompt Compiler automatically absorbs them. Team A and Team B never need to have a meeting to sync on JSON formats. 

### Conclusion
Your philosophy turns the system from a "Behavioral Architecture" (where code tells other code what to do) into a "Data Architecture" (where the SSD Memory defines the rules of reality, and everything else just plays by those rules).
