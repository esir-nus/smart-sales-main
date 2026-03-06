## DocSync Proposal

### Context
Upgrading `interface-map.md` to serve as the definitive "Cerb Map". This prevents documentation rot by ensuring we only have one list of all modules. The "Module" column will be updated to include hyperlinks to each module's `spec.md` (or `interface.md` if no spec exists). This provides a single view of the architecture's data flow while serving as a clickable index to every spec.

### Cross-Reference Audit
- [x] Verified `docs/cerb/` contains 29 module directories.
- [x] Verified all existing links in `interface-map.md` (none currently exist in the Module column).
- [x] Identified paths for all `spec.md` and `interface.md` files to be linked.

### Docs to Update

#### 1. `docs/cerb/interface-map.md`
**Why:** To serve as the definitive, clickable index for all Cerb modules (the "Cerb Map").
**Changes:**
- Update the "Module" column in all tables to use Markdown relative links pointing to each module's directory (`[ModuleName](./<module-name>/spec.md)` or `./interface.md`).
- Ensure any missing modules discovered in `docs/cerb/` are added to the appropriate layer. (e.g., `model-routing`, `plugins`, `audio-management`, `device-pairing`, `conflict-resolver`, `coach` appear to be missing from the current map).

### Docs NOT Updated
- Individual `spec.md` and `interface.md` files: No changes required to their internal content for this index upgrade.
