# Wardrobe recognition and styling

Status 2026-11-10: source implemented in two passes. Pure-Kotlin logic is compiled and
unit-tested outside Gradle (75 tests pass). The first pass's Android code compiled in a
local Gradle build (class files and schema 18 present); the second pass (quick confirm,
several items, Today idea, feedback, sync conflict screen, schema 19) has **not** been
compiled yet. Nothing has been run on a device. See
`docs/WARDROBE_RECOGNITION_HANDOFF.md` for the exact state and `NAMING_GUIDE.md` for where
new code goes and what to call it.

## What the user can do

1. Wardrobe > **Take photo** or **From photos**.
2. The photo is analysed on the phone.
   - One item the app is sure about: a short "Navy jeans?" sheet with a sureness meter per
     detail; **Looks right, save**, **Edit details**, or **Start from blank**.
   - One item it is unsure about: the full form, with "couldn't tell" where it has no answer.
   - Several items: the photo with numbered outlines and a tick list. Unsure items start
     unticked. **Review** opens the form; **Missed something? Draw around it** analyses a
     box you drag; **Add N items** saves the ticked, complete ones together.
3. Nothing is saved until you press a save button, and every field can be changed first.
4. Item menu > **Edit details** to change them later; **Delete** removes item and details.
5. Item menu > **Style this**, or the sparkle icon > **Outfit ideas**: choose optional
   occasion, weather, style, rain, comfort; get up to three outfits with reasons, cautions,
   unmet requests, and separately labelled "ideas to add" that are not owned.
6. **Wear this today** records the outfit after confirmation.
7. Today > **Suggest** now shows an outfit idea from the same engine: pictures, a
   one-sentence reason, quick context chips (Casual, Work, Hot, Rain), **Next idea**,
   **Why**, and **Not for me**, which asks for a reason and stops that exact combination
   being suggested (30 days for taste reasons, the same day for weather or "not today").

The earlier manual paths (the + button, per-tile photo with crop / Magic Clean, archive,
export/import) are unchanged.

## Components

| Concern | Code | Android-free |
|---|---|---|
| Attribute model | `core/model/GarmentAttributes.kt` | yes |
| Prediction schema, thresholds | `core/outfit/recognition/GarmentPrediction.kt` | yes |
| Shape model (weights reader, CNN, input builder) | `.../recognition/ShapeClassifier.kt` | yes |
| Colour and pattern | `.../recognition/ColorAnalyzer.kt` | yes |
| ML Kit label mapping | `.../recognition/LabelEvidence.kt` | yes |
| Fusion and uncertainty rules | `.../recognition/PredictionFusion.kt` | yes |
| Review draft and validation | `.../recognition/GarmentDraft.kt` | yes |
| Quick-confirm summary, several-item session, region geometry | `.../recognition/ReviewSession.kt` | yes |
| Image loading + ML Kit calls | `.../recognition/MlKitGarmentRecognizer.kt` | no |
| Recommender, feedback policy | `core/outfit/styling/*` | yes |
| Sync core, conflict draft, record mapping | `core/outfit/sync/*` | yes |
| Storage | `core/database/ClothingAttributesEntity.kt`, `OutfitFeedbackEntity.kt`, `repository/WardrobeAttributesRepository.kt`, `OutfitFeedbackRepository.kt`, `MIGRATION_17_18`, `MIGRATION_18_19` | no |
| Add and edit UI | `feature/wardrobe/GarmentReviewDialog.kt`, `GarmentQuickConfirm.kt`, `GarmentPhotoOverview.kt`, `GarmentEditor.kt`, `GarmentDetailsDialog.kt`, `ClothingImagePicker.kt`, edits in `WardrobeScreen.kt` | no |
| Styling UI | `feature/wardrobe/StylingScreen.kt`, `TodayOutfitIdea.kt`, `WardrobeThumbnail.kt`, edits in `feature/today/TodayScreen.kt` | no |
| Conflict UI (not reachable yet) | `feature/wardrobe/SyncConflictDialog.kt` | no |

## Data flow

```
camera app / Photo Picker --(content URI)--> MlKitGarmentRecognizer
   bounded decode (<=1024 px), EXIF orientation
   |-- ML Kit image labels (bundled)  --> LabelEvidenceMapper
   |-- ML Kit subject segmentation    --> up to 4 item masks
   per item: mask+pixels -> ShapeInputBuilder -> ShapeClassifier (28x28 CNN)
             mask+pixels -> ColorAnalyzer
             PredictionFusion -> GarmentPrediction (schema 1)
--> GarmentDraft --(user edits, Save)--> WardrobeAttributesRepository.saveItem (one Room transaction)
                                     --> WardrobeMediaRepository.importBitmap (cut-out PNG)

wardrobe + details + wear history --> OutfitRecommender --> StylingScreen
"Wear this today" --(confirm)--> OutfitRepository.recordOutfit
```

Image handling: only the item cut-out is stored, and only on Save. If no cut-out could be
made, the chosen photo is stored instead (the review screen says so). Camera captures are
written to a private cache file, deleted when the review closes, and swept after 24 h.
The decoded bitmap is recycled after recognition. Photos, labels and predictions are never
logged.

## Offline and online

| Feature | Offline | Needs a connection |
|---|---|---|
| Browse, add, edit, archive, delete items | yes | no |
| Category/type/colour/pattern recognition | yes | no |
| Item cut-out (background removal) | yes, after the model is present | once: Google Play services downloads the ML Kit segmentation model (the manifest already requests it at install). Until then recognition still works without a cut-out and shows a note |
| Styling and outfit ideas, wear history | yes | no |
| Weather | user-entered | not fetched |
| Multi-device sync | not available | would need a backend; core logic only, see below |

DayLoop declares no INTERNET permission, so the app itself cannot send wardrobe data or
photos anywhere. ML Kit processes images on the device; per Google's ML Kit terms it may
send usage/performance metrics through Play services, which the app's privacy disclosure
should mention.

## Sync design (implemented as a core, not connected)

`WardrobeSyncEngine` works against two interfaces, `SyncStore` (durable local state) and
`SyncTransport` (the backend). Only an in-memory store exists; no transport ships.

- **Local-first:** every edit applies immediately and queues one pending upload per record.
- **Duplicates:** each queued upload has an `opId` that is reused on retry, so a server
  can drop repeats; redelivered remote changes at or below the known revision are ignored.
- **Retries:** exponential backoff with jitter, 30 s doubling to a 1 h cap; the queue is
  never dropped.
- **Conflicts:** field-level three-way merge against the last agreed version. Different
  fields merge. The same field changed differently on two devices, or delete-versus-edit,
  becomes a conflict: the local value stays on screen, the upload is held, and
  `resolveConflict` applies the user's choice. Nothing is overwritten silently.
- **Lost acknowledgements:** an upload applied by the server whose reply was lost is
  recognised when it comes back, so a device does not conflict with itself.

The conflict screen exists (`SyncConflictDialog`, driven by `ConflictDraft`) and
`WardrobeSyncMapper` converts items to and from sync records, but nothing shows the screen
yet because no sync runs. To turn sync on later: persist `SyncStore` in Room (two tables),
implement `SyncTransport` over HTTPS with token auth from the Android Keystore, add
consent, show `SyncConflictDialog` for each record with open conflicts, and schedule runs
with WorkManager on connectivity.

## Security and privacy

- Data stays in app-private storage (`files/wardrobe/`, Room). No third-party upload.
- No new permissions. Photo Picker and the camera intent need none.
- The capture FileProvider is not exported and exposes one cache sub-folder.
- The model file is validated (magic, version, shapes, finite values, exact length) and
  falls back to label-only recognition if it is missing or corrupt.
- Existing consideration, not changed here: the manifest has `allowBackup="true"`, so
  Android cloud backup may include the database and photos.

## Accessibility

Controls are at least 48 dp; colour swatches carry spoken names and radio semantics;
section titles are headings; loading, error and "what's missing" texts are live regions;
confidence is given in words and percent, not colour alone. Not yet checked with TalkBack
or large fonts on a device.

## Setup, build, test (Android only; DayLoop has no iOS target)

No new dependencies or keys. From the repository root on Windows:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.dayloop.app.core.outfit.*"   # 75 JVM tests
.\gradlew.bat :app:assembleDebug            # also writes app/schemas/.../19.json
.\gradlew.bat :app:connectedDebugAndroidTest --tests "*.MigrationTest" --tests "*.WardrobeAttributesTest"
.\scripts\verify-local.ps1                  # project verification command
```

`adb logcat -s WardrobeBench` shows on-device model latency from `WardrobeAttributesTest`.
Model retraining: `tools/wardrobe-model/README.md`.
