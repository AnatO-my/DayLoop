# ADR 0006 — Wardrobe recognition, styling and sync boundary

Status: Proposed (implemented in source 2026-11-10; Android build and device checks pending)

## Context

The wardrobe stored text items, photos and unused ML Kit labels, and suggested outfits by
picking one random item per category. The request was clothing recognition from photos,
editable results, styling/outfit recommendations, and defined offline/online behaviour,
integrated only into the wardrobe feature. DayLoop is offline by design: it declares no
INTERNET permission and has no backend or accounts.

## Decisions

1. **Recognition runs only on the device.** Three sources feed one versioned prediction
   schema (`GarmentPrediction`, schema 1):
   - a 106k-parameter garment-shape CNN trained on Fashion-MNIST (MIT), executed by a
     plain-Kotlin forward pass (`ShapeClassifier`), no new dependency;
   - ML Kit's bundled base image labeller, already a dependency;
   - deterministic colour/pattern analysis of the garment's pixels.
   ML Kit subject segmentation (already a dependency) supplies per-item masks.
2. **"Not sure" is a result.** Below 0.55 confidence a field is left empty. A shape-only
   guess is never shown as confident, because its calibration was measured on studio
   product images, not phone photos. Material is never pre-filled from a photo.
3. **Nothing is saved without review.** Recognition produces a `GarmentDraft`; the user
   edits and confirms it. This follows the repository's AI rule: model output is untrusted
   input routed through validation and a repository.
4. **Details live in one additive table** (`clothing_item_attributes`, schema 17 -> 18).
   `clothing_items`, `ClothingCategory`, backups and existing rows are unchanged. A dress
   is a `GarmentType` flagged `fullBody` rather than a seventh category.
5. **Warmth replaces calendar seasons**, which do not describe every climate.
6. **Recommendations are rule-based and explainable** (`OutfitRecommender`). No licensed
   outfit-compatibility data is available to train on, and every score must be traceable
   to a stated guideline. Stated occasion/weather constraints outrank style score. Items
   the user does not own appear only as separately labelled "ideas to add".
7. **Sync is specified and implemented as a tested, transport-free core**
   (`WardrobeSyncEngine`), not connected to Room or the UI. Shipping a transport requires
   adding INTERNET, an account system and a backend, which is a product decision outside
   the wardrobe feature. Weather is user-entered for the same reason.

## Consequences

- No new Gradle dependencies; APK grows by the 424 KB weights file.
- Recognition and styling work with no connectivity. The one network-dependent step is
  Google Play services downloading the segmentation model once; until then recognition
  degrades to labels plus centre-crop colour and says so.
- Wardrobe details are not yet part of the encrypted wardrobe export/import.
- Real-world recognition accuracy is unmeasured; see docs/wardrobe/EVALUATION.md.

## Unresolved (owner decision)

- Whether DayLoop should ever gain network access for sync. If yes: choose backend and
  auth, add consent UI, persist `SyncStore` in Room, and review `allowBackup`.
- Whether to collect consented correction data to train a better classifier.

## Addendum 2026-11-10 (second pass)

- Schema 18 -> 19 adds `outfit_feedback` (worn / not-for-me with an optional reason),
  stored on the device only. "Not for me" hides that exact combination: 30 days for taste
  reasons, the same day for weather, recently-worn or "not this piece today". Reasons are
  not yet used to re-weight scoring; that needs real feedback volume first.
- Today's suggestion card now uses `OutfitRecommender` through `TodayOutfitIdeaState`.
  The legacy card path (plain item list, Wear this / Cancel) remains when no idea state is
  supplied, so existing Today tests keep their meaning. `OutfitSuggester` is no longer
  called from Today.
- A confident single-item recognition opens a short confirmation instead of the full form.
  Saving from it is still an explicit user action, so decision 3 holds.
- The conflict screen and `WardrobeSyncMapper` exist but stay unreachable until the sync
  decision above is made.
- Naming and placement rules for further work: `docs/wardrobe/NAMING_GUIDE.md`.
