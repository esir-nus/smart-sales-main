# Kotlin Fundamentals - Lesson 1: Variables & Types (LEGO + Python Comparison)

## A) LEGO Map

- `val` = Immutable LEGO brick (once assembled, can't be disassembled)
- `var` = Mutable LEGO brick (can be reassembled)
- Type declaration = Label on the LEGO box (tells you what shape of brick it is)

## B) Python Comparison

**Python (what you know):**
```python
name = "Frank"           # Can be changed
age = 25                 # Can be changed
PI = 3.14                # Convention: uppercase = constant (but technically still mutable)
```

**Kotlin (new language):**
```kotlin
var name = "Frank"       // Can be changed (mutable)
val age = 25             // Cannot be changed (immutable)
const val PI = 3.14      // True constant
```

**Key difference:**
- Python: All variables can be changed (no true constants)
- Kotlin: `val` = assign once, then cannot reassign (safer, like a LEGO that can't be disassembled once built)

---

## C) Etymology (Word Origins) - Memory Aid

Understanding where these terms come from helps you remember them:

### `var` = **Variable**
- **Origin:** From Latin "variabilis" (changeable)
- **Meaning:** Something that can **vary** (change)
- **Memory trick:** "var" sounds like "vary" - it can vary/change
- **Example:** `var count = 0` → count can change to 1, 2, 3...

### `val` = **Value**
- **Origin:** Short for "value" (fixed value)
- **Meaning:** A **value** that is assigned once and stays fixed
- **Memory trick:** "val" = "value" - once you set the value, it's fixed
- **Example:** `val name = "Frank"` → name is fixed, can't reassign

### `const` = **Constant**
- **Origin:** From Latin "constans" (standing firm, unchanging)
- **Meaning:** A **constant** value that never changes (even at compile time)
- **Memory trick:** "const" = "constant" - it's constant/unchanging
- **Example:** `const val PI = 3.14` → PI is a constant, never changes

**Quick comparison:**
- `var` = **var**iable (can **var**y/change)
- `val` = **val**ue (fixed **val**ue, assigned once)
- `const val` = **const**ant **val**ue (never changes, even at compile time)

---

## D) Concept Explanation (LEGO Analogy)

### 1. `val` = Immutable LEGO Brick

Imagine you built a LEGO car:
- Once built, you can't remove the wheels (`val` cannot be reassigned)
- But you can add stickers to the car (object's internal properties can change)

```kotlin
val car = Car()          // Built! The `car` variable cannot point to another car
car.color = "red"        // But you can change the car's color (modify object internals)
// car = Car()           // ❌ Error! Cannot reassign
```

### 2. `var` = Mutable LEGO Box

Imagine you have a "tool box":
- You can swap the tool inside at any time

```kotlin
var currentTool = "hammer"       // Currently a hammer
currentTool = "screwdriver"      // Can swap to screwdriver
currentTool = "wrench"           // Can swap to wrench
```

### 3. Type Declaration = LEGO Box Label

**Python (dynamic typing):**
```python
name = "Frank"    # Python automatically knows this is a string
name = 25         # Can suddenly become a number (potential bug!)
```

**Kotlin (static typing):**
```kotlin
val name: String = "Frank"    // Explicitly tell Kotlin: this is a String
// name = 25                   // ❌ Error! Type mismatch
```

**LEGO analogy:** Like a LEGO box labeled "2x4 red brick" - you can only put that type of brick inside.

---

## E) Real Codebase Examples

Let's look at actual usage in the project:

### Example 1: `val` in Data Class (lines 10-13)

```kotlin
data class TranscriptionLaneDecision(
    val requestedProvider: String,      // Immutable string
    val selectedProvider: String,       // Immutable string
    val disabledReason: String?,        // Immutable string (? means nullable - next lesson)
    val xfyunEnabledSetting: Boolean,   // Immutable boolean
)
```

**Python equivalent:**
```python
@dataclass
class TranscriptionLaneDecision:
    requested_provider: str
    selected_provider: str
    disabled_reason: str | None  # Can be None
    xfyun_enabled_setting: bool
```

**LEGO analogy:** This is a "LEGO instruction template". Once you build a brick using this template, you can't change its shape (`val` cannot be reassigned).

---

### Example 2: Local Variables in Function (lines 20-23)

```kotlin
val transcription = snapshot.transcription           // Extract transcription from snapshot
val requested = transcription.provider.trim()       // Call method, store result in requested
val normalizedRequested = if (...) "" else ...      // Assign based on condition
val xfyunEnabled = transcription.xfyunEnabled       // Extract boolean value
```

**Python equivalent:**
```python
transcription = snapshot.transcription
requested = transcription.provider.strip()        # Python uses strip()
normalized_requested = "" if ... else ...
xfyun_enabled = transcription.xfyun_enabled
```

**Observation:** All use `val` because they don't need to change during function execution (safer, prevents accidental modification).

---

### Example 3: Type Inference (Kotlin can omit types)

```kotlin
val transcription = snapshot.transcription    // Kotlin automatically knows the type
// Equivalent to:
val transcription: TranscriptionSettings = snapshot.transcription
```

**Python comparison:** Python doesn't need type declarations, and Kotlin can also omit them (type inference).

---

## F) Tiny Task (10 minutes)

**Task:** Find all `val` declarations in `TranscriptionLaneSelector.kt` and answer:

1. For the 4 `val` variables on lines 20-23, what are their types? (Hint: look at what object they extract values from)
2. Why do these variables all use `val` instead of `var`? (Hint: do they need to change during function execution?)

**Steps:**
1. Open file: `data/ai-core/src/main/java/com/smartsales/data/aicore/params/TranscriptionLaneSelector.kt`
2. Look at lines 20-23
3. Answer the two questions above

---

## G) Checkpoint Question

**Question:** What's the main difference between `val` and `var` in Kotlin? Explain using the LEGO analogy.

**Hint:** Think about the difference between "assembled LEGO" vs "LEGO toolbox".

---

## H) Next Lesson Preview

Next lesson: **Function Basics** (`fun` vs Python's `def`) - parameters, return values, and how to understand "functions = LEGO assembly instructions" using the analogy.

---

**Complete the Tiny Task and tell me your answers, then we'll continue!**

