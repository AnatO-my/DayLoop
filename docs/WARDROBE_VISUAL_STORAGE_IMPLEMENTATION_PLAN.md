# Implementation Plan: Wardrobe Reliability, Visual Storage & Project Hardening

Fix wardrobe recording and history risks, then extend the existing description-based wardrobe with photos, a DayLoop outfit album and visual outfit records. Preserve a fully manual workflow; AI extraction is a later enhancement.

**Status:** Proposed implementation plan. This document does not mean the changes are implemented or verified.

## User Review Required

> [!IMPORTANT]
> **History Preservation:** Removing clothing archives it by default. Archived items remain in historical outfits but are excluded from new suggestions. Editing an item's current description or photo must not silently rewrite the historical representation of an outfit.

> [!IMPORTANT]
> **Suggestions Are Drafts:** Suggesting an outfit, importing a photo or extracting clothing does not record anything as worn. Recording requires explicit confirmation and a date.

> [!IMPORTANT]
> **Recording Scope:** Keep one outfit record per day, with multiple photos and multiple linked clothing items. Future dates are read-only. Multiple separate outfits per day and future outfit planning remain deferred.

> [!IMPORTANT]
> **Two Initial Visual Stages:** Stage 1 adds photo storage and manual item creation. Stage 2 links full-outfit photos to wardrobe items and dated records. Neither stage depends on AI.

> [!IMPORTANT]
> **Storage and Privacy:** Use app-managed image copies as the wardrobe's reliable source, with optional export to a gallery-visible `Pictures/DayLoop Outfits` album. No automatic full-library scan, cloud upload or deletion of original gallery photos.

> [!IMPORTANT]
> **Migration:** Inspect the actual database version and exported schemas before assigning migrations. Version 12 was current during review; use 12 -> 13 only if still appropriate. Split migrations by independently deliverable slices rather than reserving one version for unfinished features. Never use destructive migration or fabricate schema exports.

---

## 1. Recording Safety

### [MODIFY] `TodayScreen.kt`
- Replace immediate suggestion persistence with a preview.
- Provide Wear this, Shuffle and Cancel actions.
- Confirm replacement of an existing daily record.
- Route an empty wardrobe to item creation instead of silently doing nothing.

### [MODIFY] `OutfitDetailDialog.kt`
- Add loading, saving, empty and error states.
- Disable editing and Save until the existing record has loaded.
- Prevent duplicate submissions and retain the draft after failure.
- Reset selection state when the selected date changes; preserve drafts across supported configuration changes.
- Provide a separate Clear record action with confirmation. Empty selection must not silently delete history.
- Reject future recording at the repository boundary using the injected clock and an explicit timezone policy.

### [NEW] `OutfitRepository.kt` / `RoomOutfitRepository.kt`
- Centralize recording, replacement, clearing and date validation.
- Verify referenced clothing and image records are valid before saving.
- Save outfit membership and metadata in a database transaction.
- Expose date-specific and month-scoped observation instead of loading all history into Today.
- Distinguish an unsaved suggestion, an imported photo and a recorded outfit.

---

## 2. Wardrobe History & Management

### [MODIFY] `ClothingItem.kt` / `ClothingItemEntity.kt`
- Add `isArchived`, defaulting to false.
- Preserve stable IDs when editing or adding photos.
- Validate nonblank names, categories and optional HEX colors.
- Support descriptions and visual media together; neither photos nor AI descriptions are mandatory.
- Retain existing categories and introduce TIE as a first-class category if category filtering requires it; otherwise store it as an accessory subtype. Record the selected policy before implementation.

### [MODIFY] `ClothingDao.kt`
- Add archive/restore operations and active-item queries.
- Stop removing historical outfit associations during ordinary removal.
- Restrict permanent deletion to unused items, with confirmation.

### [MODIFY] `WardrobeScreen.kt`
- Add item editing, archive/restore and archived-item filtering.
- Add color input with swatch preview and friendly category labels.
- Add busy/error states, stable list keys and keyboard-safe layouts.
- Use a photo grid once visual storage is available, with category-icon fallbacks for text-only items.
- Keep long names, large text and accessibility labels usable without overlapping controls.

### [MODIFY] Historical Outfit Membership
- Retain links to stable clothing IDs for wear history.
- Store the recorded name/category/color snapshot and retain the referenced media version when recording an outfit.
- Current item edits affect future suggestions, not old records, unless the user explicitly edits that historical record.
- For migrated records, preserve existing links and document that any initial snapshot reflects migration-time metadata, not reconstructed historical truth.

---

## 3. Outfit Suggestions

### [MODIFY] `OutfitSuggester.kt`
- Exclude archived items.
- Keep randomness injectable for deterministic tests.
- Make optional categories selectable rather than automatically including every available category.
- Identify missing core categories while still permitting intentionally partial outfits.
- Return an unsaved draft without database side effects.
- Keep clothing photos as representations of actual items, not generated replacements.

### [NEW] `OutfitSuggestionDialog.kt`
- Show selected items grouped by category, with thumbnails where available.
- Allow optional layers to be included or removed.
- Offer Shuffle, Wear this and Cancel.
- Clearly distinguish a partial suggestion from a complete combination.

---

## 4. Visual Wardrobe - Stage 1: Photo Storage

### User Flow
1. Select Add photo.
2. Take a picture or select images through Android's Photo Picker.
3. Choose Single item or Full outfit.
4. For a single item, crop manually and enter or confirm its name, category and optional description/color.
5. Save to the wardrobe. A full-outfit photo may remain an unassigned import until Stage 2 linking.

### [NEW] Media Domain and Persistence
- Introduce media records with stable IDs, MIME type, dimensions, storage reference, creation time and optional source-photo/crop relationship.
- Introduce clothing-media associations supporting multiple views and an explicitly selected primary image.
- Store file references and metadata in Room, not full bitmap blobs.
- Preserve originals and derived crops separately; editing a crop must not overwrite its source.
- Use an installed image-loading library where suitable; otherwise select one after checking current compatibility and licensing.

### [NEW] `WardrobeMediaRepository.kt` / Import Coordinator
- Import selected images into app-managed storage with bounded decoding, orientation correction and thumbnail generation.
- Define maximum file size, image dimensions and supported formats before implementation; explain rejected inputs without crashing.
- Remove unnecessary location metadata from managed/exported derivatives.
- Handle cancelled imports, unavailable cloud-backed selections, low storage and revoked source access.
- Stage writes before publication; only expose completed imports. Clean up orphaned temporary files after interruption.
- Store a content hash where useful for duplicate detection, but do not assume identical images always mean identical wardrobe items.
- Use reference-aware cleanup: media used by an item or historical outfit must not be deleted as unused.

### [NEW] Photo Import / Crop / Item Detail UI
- Provide capture, picker import, crop, rotate, replace-primary-photo and additional-view actions.
- Keep manual category and description entry available at every stage.
- Show import progress and retryable errors.
- Item details show photos, metadata, archival state and wear history.
- Missing-image placeholders preserve the underlying text record and offer relinking or replacement.

### [NEW] Optional DayLoop Album Export
- Publish user-confirmed full-outfit photos through MediaStore under `Pictures/DayLoop Outfits`.
- Allow opting out: a shared album is visible outside DayLoop and is not private storage.
- Explain that an album may appear only after its first image is published; gallery display depends on the gallery app.
- Track exports to avoid creating duplicate copies on every save.
- Never move or delete the user's original photo during import.
- Deleting an exported gallery copy must not destroy the app-managed record; handle missing exports gracefully.
- Document that app-managed files are normally removed on uninstall, while shared album images can remain. Neither location alone preserves wardrobe relationships without backup.

### Stage 1 Acceptance
- A text-only wardrobe item can gain photos without changing identity or losing outfit links.
- Photo imports survive app restart; cancelling an import leaves no visible half-created item.
- Archive and history-preservation rules still hold.
- Album export is optional and does not request broad photo-library access unnecessarily.

---

## 5. Visual Wardrobe - Stage 2: Outfit Linking & Recording

### User Flow
1. Open an imported full-outfit photo.
2. Manually select/crop visible shirts, trousers, ties, shoes and accessories.
3. Link each piece to an existing wardrobe item or create a new one.
4. Review the original photo, linked pieces and descriptions together.
5. Save the composition, then separately choose Record as worn and a permitted date.

### [NEW] Outfit Photo Review UI
- Retain the full photo alongside individual item thumbnails.
- Support multiple items in the same category, including accessories; do not enforce the suggester's one-per-category rule on real outfit records.
- Offer existing-item matching before creating a duplicate item.
- Permit partial records and items that are not visibly represented in the photo.
- Do not infer the worn date from import time or photo metadata without confirmation.

### [MODIFY] Outfit Records and Media Associations
- Allow multiple photos per daily record, with one selected cover photo.
- Maintain the source-photo -> derived crop -> clothing item -> outfit record links.
- Reuse existing clothing records across dates instead of creating a new shirt every time it is photographed.
- Preserve historical metadata/media versions when current wardrobe items are edited or archived.
- Confirm replacement of an existing daily record and distinguish removal of a link from deletion of an image.

### [MODIFY] Today and Calendar
- Show the recorded outfit cover and linked item thumbnails without displacing primary task workflows.
- Add a restrained indicator to dates with outfit records; tapping opens the outfit and its photos.
- Preserve the existing completion-day colors and calendar dismiss interactions.
- Keep text-only outfit records fully supported.

### Stage 2 Acceptance
- One photo can link to multiple existing/new clothing items without duplicating known items.
- Cancelling linking or recording never replaces an existing outfit.
- Historical records remain intelligible after item archival or photo replacement.
- Backup/restore includes original photos, crops, associations and historical snapshots.

---

## 6. Deferred: Assisted Extraction and Smarter Suggestions

> [!NOTE]
> This stage is not a dependency of the first two visual stages and requires separate approval and a model/provider feasibility review.

### [FUTURE] Extraction Pipeline
- Keep manual cropping, background removal and garment recognition as separate capabilities.
- Produce draft regions/categories/descriptions, never silently committed wardrobe items.
- Let users correct crops, remove false detections, add missed pieces and match existing items before saving.
- Retain the manual path for overlapping, hidden or misclassified clothing.
- Define confidence handling, device performance, licensing, cost and offline behavior before selecting a model.
- Prefer on-device processing when practical. Cloud uploads require explicit informed consent, a clear retention policy and cancellation controls.
- Do not identify people or infer sensitive personal attributes from outfit photos.

### [FUTURE] Recommendation Improvements
- Use confirmed categories, user-entered attributes and wear history.
- Allow recently worn items to be avoided and unavailable/laundry items to be excluded once availability tracking exists.
- Keep recommendations editable and separate from recording what was actually worn.

---

## 7. Calendar & Today Controls

### [MODIFY] `IdleScreen.kt`
- Add previous/next-month navigation and a return-to-current-month action.
- Separate the displayed month from the actual current date.
- Keep future dates read-only; no implicit future worn records.
- Query outfit indicators by displayed month.
- Preserve dismiss gestures and provide accessible date/action labels.

### [MODIFY] Settings and Today
- Add Show wardrobe on Today, persisted through the existing settings repository.
- Keep wardrobe access available elsewhere when the card is hidden.
- Observe only today's outfit, and distinguish loading from no record.
- Do not couple outfit records to task completion counts, focus totals or task streaks.

---

## 8. Architecture & Database Migrations

### [NEW] Feature State Holders
- Move wardrobe/outfit reads and mutations out of composables.
- Expose observable loading/data/error state and explicit action callbacks.
- Reuse existing dependency-injection and injected-time conventions.
- Keep AI/network processing optional and outside deterministic recording operations.

### [MODIFY] `DayLoopDatabase.kt`, `DayLoopMigrations.kt`, `DefaultAppGraph.kt`
- Register archival, historical-snapshot and media-association migrations as their slices are delivered.
- Preserve existing clothing, outfits, tasks, completion timestamps and project links.
- Review join-table indexes, foreign keys and deletion policies; clean or report existing orphan links before adding constraints.
- New media fields must be nullable/optional for existing text-only records.
- Generate and inspect schema exports through the authorized build process.

### File/Database Consistency
- A Room transaction does not make filesystem writes atomic. Import to staging, validate, publish the file, then commit references with recoverable failure handling.
- Track unfinished imports and clean up abandoned files without deleting referenced media.
- Make retries idempotent and avoid duplicate wardrobe items or exports.
- Limit thumbnail caches and never treat cached thumbnails as the only recoverable original.

---

## 9. Complete Backup & Restore

### [MODIFY] `BackupManager.kt`
- Replace count-only export with a versioned backup of durable user data.
- Include tasks/completion timestamps, projects/goals, wardrobe items, archive states, outfit records, historical snapshots, supported preferences and media associations.
- Include managed image files in the full backup; store portable archive paths rather than device-specific absolute paths or content URIs.
- Offer an explicitly labelled metadata-only export if useful, warning that it cannot restore photos by itself.
- Use authenticated encryption and validate the complete archive before changing live data.
- Check file hashes, size limits, available storage and safe extraction paths.
- Provide a restore preview and explicit replacement confirmation.
- Stage restored media, restore database records transactionally, and recover safely from partial filesystem failures. Report preference-restoration failures separately.
- Reconcile reminders using current permissions after restoration; do not restore stale pending alarms or blindly resume an active timer.
- Recognize legacy incomplete backups and clearly explain their limitations.
- Never automatically delete or repopulate the shared gallery album during restore without a separate user choice.

---

## 10. Documentation & Handoff

### [MODIFY] `STATE.md`, `TASKS.md`, `LEARNED_CONCEPTS.md`
- Consolidate actual schema/version information and remove obsolete next-action statements.
- Separate implemented, source-reviewed, test-passed and manually accepted status.
- Record photo storage, album visibility, archival and backup compatibility rules.
- Keep each implementation slice resumable without relying on chat history.

### [NEW] Decision Record
- Document one-record-per-day scope and future-date restrictions.
- Define historical snapshot behavior and archive/permanent-delete semantics.
- Define managed originals, thumbnails, optional exports, cleanup and uninstall behavior.
- Record any future AI provider/privacy decision separately from the local wardrobe architecture.

---

## Verification Plan

### Automated Tests
- Suggestions/imports never record or replace outfits without confirmation.
- Loading, cancellation, repeated taps and failed saves preserve existing records.
- Archived items remain in history and are excluded from suggestions.
- Current metadata/photo edits do not silently rewrite historical snapshots.
- Future recording is rejected at the data boundary using a fake clock.
- Outfit membership replacement and clearing are transactional.
- Import failures, rotation, unsupported formats, oversized images and low storage are handled safely.
- Interrupted imports/exports are recoverable without duplicate items or dangling references.
- Media cleanup preserves every image still referenced by wardrobe or history.
- Multiple crops can share one source photo and map to existing items.
- Missing gallery exports or revoked source access do not erase managed records.
- Migration chains preserve both older text-only records and new media associations.
- Full backups round-trip metadata and images; corrupt, incompatible or incorrect-password backups make no changes.
- Calendar navigation covers leap years, month boundaries, midnight and timezone changes.

### Manual Verification
1. Record an outfit, request a suggestion, then cancel: the original must remain.
2. Archive worn clothing: history remains intact and suggestions exclude it.
3. Add photos to existing text-only items; inspect orientation, crops, descriptions and category filters.
4. Import a full-outfit photo, link several pieces, reuse an existing item and record the outfit for a past date.
5. Confirm optional album export appears appropriately in the device gallery without moving the source photo.
6. Delete an exported copy outside DayLoop; the managed outfit must remain usable.
7. Restart or rotate during editing/import, cancel camera/picker flows and test low-storage/error recovery.
8. Browse previous months and verify future recording is unavailable.
9. Review narrow screens, large text, keyboard visibility, TalkBack and both themes.
10. Verify backup/restore with disposable test data and photos before using real records.

> [!IMPORTANT]
> Build, automated-test execution, emulator and device checks remain approval-controlled. Do not change device dates, clear app data, install/uninstall the app or perform destructive restore tests without explicit authorization. Photo and cloud access require their normal user-facing consent flows.

## Implementation Order

1. Recording safety, repository boundary and archival/history preservation.
2. Migration regression coverage and core backup correctness.
3. Suggestion preview and optional-category controls.
4. **Visual Stage 1:** photo import/capture, manual cropping, item images, thumbnails and optional album export; extend backup for media before treating this stage as release-ready.
5. **Visual Stage 2:** full-outfit linking, dated visual records and history-preserving photo associations.
6. Calendar browsing, optional Today presentation and final accessibility review.
7. Consolidated handoff and user-controlled acceptance.
8. **Later, separately approved:** assisted extraction and smarter recommendations.

## Android References
- [Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker)
- [Shared media storage and MediaStore](https://developer.android.com/training/data-storage/shared/media)
- [Minimize permission requests](https://developer.android.com/privacy-and-security/minimize-permission-requests)

Verify current Android documentation and project dependency compatibility at implementation time.
