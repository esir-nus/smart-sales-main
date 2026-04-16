# ArkTS Compliance Rules

> **Purpose**: Mandatory reference for any agent writing or reviewing `.ets` files in the HarmonyOS-native app.
> **When to read**: Before writing ANY ArkTS code. ArkTS is NOT TypeScript -- it bans many standard TS/JS patterns that every model defaults to.
> **Authoritative for**: `platforms/harmony/smartsales-app/` and any future Harmony-native roots.
> **Last Updated**: 2026-04-16

---

## 1. Banned Patterns and Compliant Replacements

| Rule ID | What ArkTS Bans | Compliant Replacement |
|---|---|---|
| `arkts-no-spread` | `{...obj, key: val}` object spread | Explicit field-by-field construction or `mergeItem()` helper |
| `arkts-no-delete` | `delete obj[key]` | Use `Map<K,V>` with `.delete()` method |
| `arkts-no-any-unknown` | `any`, `unknown` type annotations | `Object \| null \| undefined` or specific interfaces |
| `arkts-no-destruct-decls` | `const {a, b} = obj` or `const [a, b] = arr` | Individual assignments: `const a = obj.a; const b = arr[0];` |
| `arkts-no-props-by-index` | `str[i]` string indexing, `obj[key]` field access | `str.charAt(i)`; for config lookup use explicit `switch` |
| `arkts-no-is` | `item is T` return type predicates | Boolean filter + `as T` cast at callsite |
| `arkts-no-untyped-obj-literals` | `{ key: val }` without matching a declared interface | Declare an interface for every object shape; assign to typed variable first |
| `arkts-no-obj-literals-as-types` | `Array<{ title: string; startMs: number }>` inline types | Declare a named interface (`ChapterItem`) |
| `arkts-limited-throw` | `throw error` where `error` is `unknown`/`Object` | `throw error instanceof Error ? error : new Error(String(error))` |
| TextEncoder/TextDecoder | Global `TextEncoder`/`TextDecoder` (not available) | `import { util } from '@kit.ArkTS'`; see Section 3 |
| `@Builder` const | `const x = ...` inside `@Builder` methods | Move computation to helper methods on the struct |
| `@Entry` page config | Page in `main_pages.json` without `@Entry` decorator | Only list `@Entry` pages in `main_pages.json`; components use `@Component export struct` |

---

## 2. Before/After Examples

### Object Spread

```typescript
// BANNED
this.snapshot = { ...this.snapshot, isLoading: false, lastErrorMessage: msg };

// COMPLIANT
this.snapshot = {
  items: this.snapshot.items,
  configMissingKeys: this.snapshot.configMissingKeys,
  isLoading: false,
  isBusy: this.snapshot.isBusy,
  busyMessage: this.snapshot.busyMessage,
  lastErrorMessage: msg
};
```

For repeated merges on large interfaces, define a `mergeItem(base, patch)` helper that checks each field with `patch.field !== undefined ? patch.field : base.field`.

### Delete Operator

```typescript
// BANNED
private activeJobs: Record<string, boolean> = {};
delete this.activeJobs[audioId];

// COMPLIANT
private activeJobs: Map<string, boolean> = new Map();
this.activeJobs.delete(audioId);
this.activeJobs.set(audioId, true);   // set
this.activeJobs.get(audioId);         // read
```

### Unknown / Any Types

```typescript
// BANNED
export function cleanString(value: unknown): string | undefined { ... }

// COMPLIANT
export function cleanString(value: Object | null | undefined): string | undefined { ... }
```

### Destructuring

```typescript
// BANNED
const [transcription, chapters, summary] = await Promise.all([...]);

// COMPLIANT
const results = await Promise.all([...]);
const transcription = results[0];
const chapters = results[1];
const summary = results[2];
```

### String Indexing

```typescript
// BANNED
result += BASE64_ALPHABET[(chunk >> 18) & 63];

// COMPLIANT
result += BASE64_ALPHABET.charAt((chunk >> 18) & 63);
```

### Config Field Access by Key

```typescript
// BANNED
const value = config[key];  // key is keyof ConfigShape

// COMPLIANT
function readConfigValue(config: ConfigShape, key: keyof ConfigShape): string {
  switch (key) {
    case 'FIELD_A': return config.FIELD_A;
    case 'FIELD_B': return config.FIELD_B;
    // ... exhaustive
    default: return '';
  }
}
```

### Type Guard Predicates

```typescript
// BANNED
.filter((item: T | undefined): item is T => item !== undefined);

// COMPLIANT (use forEach + push pattern)
const out: T[] = [];
items.forEach((item: T | undefined) => {
  if (item !== undefined) { out.push(item); }
});
```

### Untyped Object Literals

```typescript
// BANNED
const body = JSON.stringify({
  AppKey: key,
  Input: { SourceLanguage: 'cn', TaskKey: taskKey, FileUrl: url }
});

// COMPLIANT -- declare interfaces first
interface TingwuInput { SourceLanguage: string; TaskKey: string; FileUrl: string; }
interface TingwuSubmitBody { AppKey: string; Input: TingwuInput; }

const input: TingwuInput = { SourceLanguage: 'cn', TaskKey: taskKey, FileUrl: url };
const submitBody: TingwuSubmitBody = { AppKey: key, Input: input };
const body = JSON.stringify(submitBody);
```

### Throw Restriction

```typescript
// BANNED
catch (error) {
  throw error;  // error is implicitly Object
}

// COMPLIANT
catch (error) {
  throw error instanceof Error ? error : new Error(String(error));
}
```

### @Builder Const Declarations

```typescript
// BANNED -- const inside @Builder
@Builder renderInventoryPanel() {
  ForEach(this.items, (item: AudioItem) => {
    const isSelected = this.selectedId === item.id;
    const color = isSelected ? '#FFF' : '#000';
    Text(item.name).fontColor(color)
  })
}

// COMPLIANT -- helper method on the struct
private primaryColor(itemId: string): string {
  return this.selectedId === itemId ? '#FFF' : '#000';
}

@Builder renderInventoryPanel() {
  ForEach(this.items, (item: AudioItem) => {
    Text(item.name).fontColor(this.primaryColor(item.id))
  })
}
```

---

## 3. Import Reference

ArkTS uses `@kit.*` module paths instead of Node/browser globals.

| Need | Import |
|---|---|
| TextEncoder | `import { util } from '@kit.ArkTS'; new util.TextEncoder().encodeInto(str)` |
| TextDecoder | `import { util } from '@kit.ArkTS'; util.TextDecoder.create('utf-8').decodeToString(bytes)` |
| File I/O | `import { fileIo as fs } from '@kit.CoreFileKit'` |
| File Picker | `import { picker } from '@kit.CoreFileKit'` |
| HTTP | `import { http } from '@kit.NetworkKit'` |
| UI Ability Context | `import { common } from '@kit.AbilityKit'` |
| UIAbility base class | `import { UIAbility } from '@kit.AbilityKit'` |
| Window | `import { window } from '@kit.ArkUI'` |

---

## 4. Component and Page Rules

### Cross-file component import

```typescript
// AudioPage.ets -- must use @Component export struct
@Component
export struct AudioPage { ... }

// Index.ets -- import and use
import { AudioPage } from './AudioPage';
// inside build():
AudioPage()
```

### main_pages.json

Only pages with `@Entry` decorator belong in `main_pages.json`. Components used inside other pages must NOT be listed.

```json
{
  "src": [
    "pages/Index"
  ]
}
```

### ForEach typing

Always provide explicit type annotation on the ForEach item callback parameter:

```typescript
// BANNED
ForEach(this.items, (item) => { ... })

// COMPLIANT
ForEach(this.items, (item: AudioItem) => { ... })
```

---

## 5. JSON.parse Return Type

ArkTS treats `JSON.parse()` return as implicit `any`. Annotate immediately:

```typescript
// BANNED
const parsed = JSON.parse(raw);

// COMPLIANT
const parsed: Object = JSON.parse(raw);
```

---

## 6. Record vs Map

| Use Case | Type | Reason |
|---|---|---|
| Immutable lookup tables (headers, labels) | `Record<string, string>` | Initialized inline, read-only after creation |
| Mutable tracking maps (active jobs) | `Map<string, boolean>` | Needs `.set()`, `.get()`, `.delete()` |

`Record<string, T>` supports bracket-notation reads (`record[key]`), but `Object.keys()` + `.forEach()` is preferred for iteration.

---

## 7. Array Operations

```typescript
// BANNED -- array spread for cloning
const copy = [...this.items];

// COMPLIANT
const copy = this.items.slice();

// BANNED -- array spread for prepending
this.items = [newItem, ...this.items];

// COMPLIANT
const next: AudioItem[] = [newItem];
this.items.forEach((existing: AudioItem) => next.push(existing));
this.items = next;
```
