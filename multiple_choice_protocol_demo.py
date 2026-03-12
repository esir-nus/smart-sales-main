import json
try:
    from pydantic import BaseModel, Field, ValidationError
except ImportError:
    print("pydantic not found, installing...")
    import subprocess
    import sys
    subprocess.check_call([sys.executable, "-m", "pip", "install", "pydantic"])
    from pydantic import BaseModel, Field, ValidationError

print("\n=======================================================")
print(" 1. THE SSD (Memory) CONTRACT")
print("=======================================================")
print("This is the defining blueprint (like a Kotlin data class).")
print("If the CRM team changes this, everything else adapts instantly.\n")

class QuoteMutation(BaseModel):
    quote_id: str = Field(..., description="The unique ID of the quote in the SSD")
    deal_stage: str = Field(..., description="The new stage of the deal (e.g., 'Won', 'Lost', 'Negotiation')")
    budget_amount: int = Field(..., description="The updated budget amount in integers")
    currency: str = Field(default="USD", description="The currency type, defaults to USD")

print("-> Defined QuoteMutation with fields:")
for field_name in QuoteMutation.model_fields.keys():
    print(f"   - {field_name}")

print("\n=======================================================")
print(" 2. THE PROMPT COMPILER (The Brain)")
print("=======================================================")
print("The prompt dynamically reads the SSD Contract. It NEVER hardcodes fields.\n")

def compile_prompt(user_input: str) -> str:
    # Automatically generate the "Multiple Choice" JSON schema from the SSD Contract
    schema = QuoteMutation.model_json_schema()
    
    prompt = f"""You are a CRM AI. 
The user said: "{user_input}"

Your task is to update the Quote. You MUST output STRICTLY valid JSON matching this schema:
{json.dumps(schema, indent=2)}

Do NOT output anything other than the JSON."""
    return prompt

print("-> Prompt Compiler configured to auto-read the QuoteMutation schema.")


print("\n=======================================================")
print(" 3. THE UNIFIED LINTER (The Bridge / Type Checker)")
print("=======================================================")
print("The linter just attempts to parse the LLM output back into the strictly typed SSD object.\n")

def parse_llm_output(llm_json_string: str) -> QuoteMutation:
    try:
        # Pydantic (like kotlinx.serialization) automatically validates types
        return QuoteMutation.model_validate_json(llm_json_string)
    except ValidationError as e:
        print("❌ LINTER CAUGHT DRIFT/GHOSTING BEFORE HITTING THE DATABASE!")
        print(f"   Error details: {e.errors()[0]['msg']} (Field: {e.errors()[0]['loc'][0]})")
        return None

print("-> Linter configured for strict deserialization.")


print("\n=======================================================")
print(" 4. SIMULATION EXECUTION")
print("=======================================================")

user_text = "Update quote Q-100 to Won and set the budget to 50000 EUR."

print("--- SCENARIO 1: The Happy Path ---")
print(f"User says: '{user_text}'\n")

print("[1] Prompt generated and sent to LLM...")
system_prompt = compile_prompt(user_text)

# SIMULATE LLM (Perfect response matching the auto-generated contract)
mock_llm_response = '{"quote_id": "Q-100", "deal_stage": "Won", "budget_amount": 50000, "currency": "EUR"}'
print(f"\n[2] LLM Returns Output:\n    {mock_llm_response}")

print("\n[3] Linter Parsing...")
action = parse_llm_output(mock_llm_response)
if action:
    print("✅ SUCCESS! Safely mapped back into an SSD object:")
    print(f"   -> {repr(action)}")


print("\n\n--- SCENARIO 2: The Ghosting Bug (LLM Hallucinates a Field) ---")
print(f"Context: The LLM hallucinated the field 'budget' instead of 'budget_amount'.\n")

mock_llm_bad_response = '{"quote_id": "Q-100", "deal_stage": "Won", "budget": 50000, "currency": "EUR"}'
print(f"[1] LLM Returns Output:\n    {mock_llm_bad_response}")

print("\n[2] Linter Parsing...")
action2 = parse_llm_output(mock_llm_bad_response)
if action2:
    print("✅ SUCCESS! Safely mapped back into an SSD object.")
else:
    print("\n🛡️ SAFE FAILURE! The Linter prevented a 'Ghosting' crash. The invalid JSON never touched the SSD.")
print("\n=======================================================\n")
