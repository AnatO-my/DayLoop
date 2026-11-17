# Wardrobe Recognition and Styling Handoff (2026-11-10)

## Scope
On-device clothing recognition with an editable review step, per-item details, explainable
outfit recommendations, camera capture, and a transport-free sync core. Wardrobe feature
only. Design record: `docs/adr/0006-wardrobe-recognition-styling-sync.md`. Architecture,
data flow and offline/online table: `docs/wardrobe/README.md`. Model, datasets, licences:
`docs/wardrobe/MODEL_AND_DATA.md`. Results and limits: `docs/wardrobe/EVALUATION.md`.

The Guided Build teach-before-write step was waived for this scope by the request, which
asked for implementation after a reported plan.

## Verification status (be exact)
| Item | Status |
|---|---|
| Pure Kotlin: recognition, styling, sync, draft (56 JVM tests) | PASSED with kotlinc 2.3.21 + JUnit 4.13.2 outside Gradle, in the agent's cloud workspace |
| Shape model training and evaluation | RAN; numbers in EVALUATION.md |
| `:app:testDebugUnitTest` through Gradle | NOT RUN |
| Compose UI, Room entity/DAO/migration, ML Kit recogniser, manifest, picker | NOT COMPILED. Written against the current sources by reading them |
| Schema export `18.json` | NOT GENERATED (comes from the next build; do not hand-write) |
| `MigrationTest.migrate17To18`, `WardrobeAttributesTest` | AUTHORED, NOT RUN |
| Lint, device, TalkBack, airplane-mode, performance on a phone | NOT RUN |

Reason: neither the agent workspace nor the device shell could reach Google Maven or an
Android SDK.

## Files
New (main): `core/model/GarmentAttributes.kt`; `core/outfit/recognition/{GarmentPrediction,
ShapeClassifier,ColorAnalyzer,LabelEvidence,PredictionFusion,GarmentDraft,
MlKitGarmentRecognizer}.kt`; `core/outfit/styling/{StylingModels,OutfitRecommender}.kt`;
`core/outfit/sync/{ThreeWayMerge,WardrobeSyncEngine}.kt`;
`core/database/ClothingAttributesEntity.kt`; `repository/WardrobeAttributesRepository.kt`;
`feature/wardrobe/{GarmentEditor,GarmentReviewDialog,StylingScreen}.kt`;
`res/xml/wardrobe_capture_paths.xml`; `assets/wardrobe/garment_shape_v1.bin`;
`assets/licenses/fashion_mnist_mit.txt`.

New (tests): `test/.../core/outfit/{recognition,styling,sync}/*Test.kt`,
`test/resources/wardrobe/shape_parity_v1.bin`,
`androidTest/.../feature/wardrobe/WardrobeAttributesTest.kt`.

New (other): `tools/wardrobe-model/*`, `docs/wardrobe/*`, ADR 0006, this file.

Edited (narrow, originals copied to `build/wardrobe-ai-backup-20261110/`):
`DayLoopDatabase.kt` (entity, version 18, DAO), `DayLoopMigrations.kt` (MIGRATION_17_18),
`ClothingDao.kt` (delete details with item), `AppGraph.kt` + `DefaultAppGraph.kt` (two
members, migration), `AndroidManifest.xml` (non-exported FileProvider),
`ClothingImagePicker.kt` (rewritten: camera + cleanup; `launchGallery()` unchanged),
`WardrobeScreen.kt` (review flow for new photos, Take photo, Edit details, Style this,
Outfit ideas), `MigrationTest.kt` (one test).

Not touched: Today's suggestion card still uses `OutfitSuggester`; `OutfitDetailDialog`;
`BackupManager` (details are not in the wardrobe export yet).

## Behaviour to know
- Database goes 17 -> 18 with one new table and no change to existing tables. Installing
  this build over real data runs the migration; an older APK cannot open the upgraded
  database afterwards. Test on a disposable profile first.
- "From photos" at the bottom of the wardrobe now opens the review screen instead of the
  name/category dialog. The + button still opens the manual dialog.
- Recognition degrades without failing: no segmentation model -> no cut-out, note shown;
  missing/corrupt weights -> labels only; unreadable image -> message and retry.

## Next single action
On the Windows machine run `.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest`
and fix any compile errors in the files marked NOT COMPILED. Then build, confirm
`app/schemas/.../18.json` matches MIGRATION_17_18, run `MigrationTest` and
`WardrobeAttributesTest` on a device, and walk the manual checklist below on disposable data.

## Manual checklist
1. Take photo and From photos: single flat item, item on hanger, two items, person wearing
   clothes, very dark photo, non-image file. Check notes, "couldn't tell" states, editing.
2. Cancel at camera, picker and review; rotate; background the app during capture.
3. Save; confirm item, details and cut-out appear; Edit details; Delete.
4. Airplane mode: repeat 1-3 and Outfit ideas.
5. Outfit ideas with each occasion/weather/style; Style this; Wear this today, then check
   Today and the calendar; confirm replacing an existing record is clearly stated.
6. TalkBack, 200% font, small screen, both themes.
7. Upgrade a copy of real v17 data; confirm nothing is lost.

## Deferred
Network sync transport and conflict UI (owner decision, ADR 0006); attributes in wardrobe
export/import; replacing Today's random suggester with `OutfitRecommender`; real-photo
evaluation set and model improvement; accessories in automatic outfits; removing the now
unused photo branch of the manual add dialog in `WardrobeScreen`.

---

# Second pass (2026-11-10): design ideas A-D and naming guide

## What changed
- **A Quick confirm:** `GarmentQuickConfirm.kt`, `ReviewSummary`/`Sureness` in `ReviewSession.kt`.
- **B Several items:** `GarmentPhotoOverview.kt`, `ReviewSession`, `RegionMath`;
  `GarmentRecognizer.recognize(uri, region)`; `RecognizedPhoto` carries the image size.
- **C Today idea:** `TodayOutfitIdea.kt` (state + content), `WardrobeThumbnail.kt`,
  `OutfitFeedback.kt`, `OutfitFeedbackEntity.kt`, `OutfitFeedbackRepository.kt`,
  `MIGRATION_18_19`; six small edits in `feature/today/TodayScreen.kt`.
- **D Sync conflict:** `SyncConflictDialog.kt`, `ConflictResolution.kt` (`ConflictDraft`,
  `WardrobeSyncMapper`), `MergeSide` on merge results. Not reachable in the app.
- `GarmentReviewDialog.kt` rewritten around a `ReviewSession`; `GarmentDetailsDialog`
  moved to its own file; `docs/wardrobe/NAMING_GUIDE.md` added.
- Originals of files edited in this pass: `build/wardrobe-ai-backup-20261110b/`.

## Verification status
| Item | Status |
|---|---|
| First-pass Android code | COMPILED in a local build at about 02:50 (class files under `app/build/tmp/kotlin-classes/debug`; `18.json` generated and matching `MIGRATION_17_18`). No APK newer than 6 Nov, so not installed from that build as far as the folder shows |
| Pure Kotlin, both passes (75 JVM tests) | PASSED outside Gradle |
| Second-pass Android code (files above, `TodayScreen.kt` edits, schema 19) | NOT COMPILED |
| `19.json`, `MigrationTest.migrate18To19` | NOT GENERATED / NOT RUN |
| Device, TalkBack, drag-to-draw gesture inside the scrolling page | NOT RUN |

## Things to check first on a device
1. Drawing a box on the photo while the page can scroll: the drag should draw, not scroll.
2. Outline positions on portrait and landscape photos (EXIF-rotated camera shots especially).
3. Today: Suggest, chips, Next idea, Why, Not for me, Wear this; the older
   `TodayLayoutRegressionTest` cases should still pass unchanged.
4. Upgrade from schema 18 (if that build was installed anywhere) and from 17.

## Next single action
`.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest`, fix anything in the
second-pass files, then build and confirm `19.json` matches `MIGRATION_18_19`.
