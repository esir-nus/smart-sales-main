import json
from dataclasses import dataclass, asdict
from typing import Dict, Any, List
try:
    from pydantic import BaseModel, Field, ValidationError
except ImportError:
    print("pydantic not found, installing...")
    import subprocess, sys
    subprocess.check_call([sys.executable, "-m", "pip", "install", "pydantic"])
    from pydantic import BaseModel, Field, ValidationError

print("\n=======================================================")
print(" OS MODEL PHASE 1: The SSD (Hard Drive / Storage)")
print("=======================================================")
print("The SSD is the ultimate Source of Truth for the application state.\n")

@dataclass
class SSDQuote:
    quote_id: str
    company_name: str
    deal_stage: str
    budget_amount: int
    currency: str

class MockSSDStorage:
    def __init__(self):
        self.db: Dict[str, SSDQuote] = {
            "Q-123": SSDQuote("Q-123", "Acme Corp", "Negotiation", 100000, "USD"),
            "Q-456": SSDQuote("Q-456", "Stark Industries", "Proposal", 500000, "USD")
        }
    
    def get_quote(self, quote_id: str) -> SSDQuote:
        return self.db.get(quote_id)
        
    def save_quote(self, quote: SSDQuote):
        self.db[quote.quote_id] = quote
        print(f"   💾 [SSD I/O] Successfully wrote to disk: {quote}")

# Singleton Database
SSD = MockSSDStorage()

print("-> Initial SSD State:")
for q in SSD.db.values():
    print(f"   {q}")


print("\n=======================================================")
print(" OS MODEL PHASE 2: The Contract (Body -> Brain)")
print("=======================================================")
print("The 'Multiple Choice' options strictly defined by the Data Layer.\n")

class QuoteMutation(BaseModel):
    quote_id: str = Field(..., description="The unique ID of the quote pulled from RAM context")
    new_budget_amount: int = Field(..., description="The updated budget amount in integers")
    reason: str = Field(default="", description="Why the change was made")


print("\n=======================================================")
print(" OS MODEL PHASE 3: RAM (Working Memory / Context)")
print("=======================================================")
print("Before AI thinks, we load relevant SSD data into RAM so it has 'Options' to choose from.\n")

class MockRAMContext:
    def __init__(self):
        self.active_entities: List[Dict[str, Any]] = []
        
    def load_from_ssd(self, query: str):
        # Extremely simplified "Retrieval": Load everything into RAM for now
        print(f"   🧠 [RAM I/O] Loading Context for query: '{query}'")
        for q in SSD.db.values():
            self.active_entities.append(asdict(q))
        print(f"   🧠 [RAM I/O] Loaded {len(self.active_entities)} entities into active working memory.")

    def get_context_json(self) -> str:
        return json.dumps(self.active_entities, indent=2)


print("\n=======================================================")
print(" OS MODEL PHASE 4: The Brain (Prompt Compiler & LLM)")
print("=======================================================")

def execute_brain(user_input: str, ram: MockRAMContext) -> str:
    # 1. Compile the prompt: Combine RAM context + Strict Contract Schema
    schema = QuoteMutation.model_json_schema()
    prompt = f"""You are a CRM AI. 
The user said: "{user_input}"

Here is the data currently loaded in RAM (Your Options):
{ram.get_context_json()}

Your task is to update the Quote. 
You must identify the correct quote_id from RAM context.
You MUST output STRICTLY valid JSON matching this schema:
{json.dumps(schema, indent=2)}"""

    print("   [Brain] Prompt compiled with RAM context and Mutation Schema.")
    print("   [Brain] Sending to LLM...")

    # SIMULATING THE LLM REASONING AND RESPONSE
    # It reads "Acme Corp" from context, sees it's "Q-123", calculates 100k + 20% = 120k.
    print("   [Brain] LLM finished 'Thinking'. Returning raw JSON string.\n")
    return '{"quote_id": "Q-123", "new_budget_amount": 120000, "reason": "User requested 20% increase for Acme Corp"}'


print("\n=======================================================")
print(" OS MODEL PHASE 5: The Linter & Entity Writer (The Hands)")
print("=======================================================")

def parse_and_execute(llm_json_string: str):
    try:
        # 1. THE LINTER (Compile-time Type Safety Check)
        print(f"   [Linter] Validating incoming JSON: {llm_json_string}")
        mutation: QuoteMutation = QuoteMutation.model_validate_json(llm_json_string)
        print(f"   [Linter] ✅ Validated. Converted to safe Kotlin/Python object.")
        
        # 2. THE ENTITY WRITER (Business Logic Execution)
        print(f"   [EntityWriter] Processing Mutation for Quote {mutation.quote_id}...")
        
        # Fetch actual entity from SSD (to ensure it actually exists, double check)
        target_quote = SSD.get_quote(mutation.quote_id)
        if not target_quote:
            print("   [EntityWriter] ❌ FAILED: Quote ID hallucinated!")
            return
            
        # Apply mutation
        print(f"   [EntityWriter] Old Budget: {target_quote.budget_amount} -> New Budget: {mutation.new_budget_amount}")
        target_quote.budget_amount = mutation.new_budget_amount
        
        # Save back to SSD
        SSD.save_quote(target_quote)
        print("   [EntityWriter] ✅ Execution Complete.")

    except ValidationError as e:
        print("   [Linter] ❌ INVALID JSON! Drift detected. Blocked before execution.")
        print(e)


print("\n=======================================================")
print(" 🚀 RUNNING THE FULL LIFECYCLE SIMULATION")
print("=======================================================")

user_request = "Increase Acme Corp's quote by 20%."
print(f"User Voice/Text Input: '{user_request}'\n")

# Step 1: Initialize RAM and load context
ram = MockRAMContext()
ram.load_from_ssd(user_request)

# Step 2: Execute Brain (LLM generates the multiple-choice response)
llm_raw_response = execute_brain(user_request, ram)

# Step 3: Lint and Execute (Update SSD)
parse_and_execute(llm_raw_response)

print("\n=======================================================")
print(" FINAL SYSTEM STATE (SSD Check)")
print("=======================================================")
for q in SSD.db.values():
    print(f"   {q}")
print("=======================================================\n")
