# Analyzer Plugin Specification

> **OS Layer**: RAM Application (Context Extraction)
> **Invocation Type**: Direct Execution or Conversational Ambient Stream

## Execution Flow

1. **Trigger**: The Lightning Router identifies a `DEEP_ANALYSIS` intent or a specific tool request ("Give me the PDF"). 
2. **Natural Language Route (The Easy Path)**: If the user explicitly asks for a tool (e.g., "PDF report"), the Analyzer skips LLM planning. It natively maps the string intent to a static tool ID (e.g., `tool01`) and returns `DirectExecution`.
3. **Conversational Recommendation (The Chat Path)**: If the user is just chatting, the Analyzer queries the LLM to output a pure text response. The prompt enforces a "verdict" style: the LLM must explain *why* it recommends a tool, and explicitly ask the user for any missing parameters in plain text (e.g., "I recommend the Executive Report, but what was the final budget?"). No buttons or UI chips are generated.
4. **Mascot Hints (Out-of-Band)**: Independent of the LLM, the Mascot observes the context. Based on simple, hardcoded rules ("When transcript finishes -> offer tool01"), the Mascot surfaces non-intrusive UI text tips ("Try asking for a PDF").
5. **Result Delivery**: Returns `ConversationalRecommendation(text)`. The Orchestrator simply appends the text to the chat. The user converses naturally to trigger the actual tool execution.

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Core Execution Logic | 🔲 PLANNED | Interface + FakeImpl + RealImpl (LLM meta-routing prompt) |
| **2** | Plugin Registry Wiring | 🔲 PLANNED | Register `ANALYZER_META` in ToolRegistry and wire to Ambient Tool Stream renderer |
