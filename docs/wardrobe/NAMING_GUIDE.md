# Wardrobe naming guide

How things in the wardrobe feature are named and where they live, so new work lands in a
predictable place. It records the conventions the code already follows; where DayLoop as a
whole has a rule (ADR numbering, `ic_action_*` icons), this guide repeats it rather than
inventing another. When a name here and a name in the code disagree, fix the code or
change this guide in the same commit.

## 1. Vocabulary (one word per idea)

| Say | Means | Do not also call it |
|---|---|---|
| **item** | one thing in the wardrobe (`ClothingItem`) | piece, article, product |
| **garment** | an item as seen by recognition, before it is saved (`GarmentPrediction`, `GarmentDraft`) | detection, object |
| **details** (UI) / **attributes** (code) | optional facts about an item | metadata, properties, tags |
| **category** | one of the six `ClothingCategory` values | group, section |
| **type** | finer kind inside a category (`GarmentType`) | subtype, class |
| **outfit** | items worn or proposed together | look, combination (except "combination" for an exact id set) |
| **idea** / **suggestion** | an outfit proposed but not recorded | recommendation in UI text |
| **record** (verb), **worn** | saving an outfit against a date | log, track |
| **prediction** | raw recogniser output | result (reserved for a whole call: `RecognitionResult`) |
| **draft** | user-editable, unsaved state | form, model, temp |
| **sureness** (UI) / **confidence** (code) | how sure the recogniser is | score, probability in UI text |
| **piece** | an item's role inside one outfit (`OutfitPiece`) | — |
| **region** | where a garment sits in a photo, as fractions (`NormalizedBox`) | bbox, rect |
| **cut-out** (UI) / **cutout** (code) | the item with its background removed | mask (the mask is the per-pixel input) |

British spelling in everything a person reads ("colour", "recognised"); American in
identifiers, matching Kotlin/Android APIs (`colorHex`, `ColorFamily`).

## 2. Packages and what may depend on what

```
core/model/                       domain types, no Android imports
core/outfit/recognition/          prediction schema, shape model, colour, fusion, drafts, review session
core/outfit/styling/              recommender, requests, feedback policy
core/outfit/sync/                 merge, engine, conflict drafts, record mapping
core/database/                    Room entities, DAOs, migrations
repository/                       interfaces + Room implementations + entity mappers
feature/wardrobe/                 Compose screens, dialogs, components, pickers
feature/today/                    Today only; calls into feature/wardrobe, never the reverse
```

`core/outfit/**` and `core/model/**` stay free of `android.*` and `androidx.*` so the JVM
tests can run the exact code that ships. The single exception is
`MlKitGarmentRecognizer.kt`, named for the SDK it wraps. If a new file in `core/outfit`
needs Android, put the Android part in its own file and name that file after the platform
piece (`MlKit…`, `Room…`, `Android…`).

## 3. Kotlin files and types

One primary type or one screen per file; the file takes its name. Small private helpers
may share the file.

| Kind | Pattern | Examples |
|---|---|---|
| Domain data | noun | `GarmentAttributes`, `OutfitFeedback`, `SyncRecord` |
| Value from a model | `…Prediction`, `…Reading`, `…Evidence` | `GarmentPrediction`, `ColorReading`, `LabelEvidence` |
| Unsaved user state | `…Draft` | `GarmentDraft`, `ConflictDraft` |
| Input / output of one operation | `…Request` / `…Result` | `StylingRequest`, `StylingResult`, `RecognitionResult` |
| Stateless logic | `object`, agent noun or `…Policy` / `…Math` / `…Mapper` | `OutfitRecommender`, `PredictionFusion`, `RecognitionPolicy`, `RegionMath`, `WardrobeSyncMapper` |
| Thresholds and tunables | constants on the `…Policy` object, `UPPER_SNAKE` | `RecognitionPolicy.CONFIDENT` |
| Boundary to a platform or service | interface = plain noun; implementation = platform prefix | `GarmentRecognizer` / `MlKitGarmentRecognizer`, `OutfitFeedbackRepository` / `RoomOutfitFeedbackRepository`, `SyncStore` / `InMemorySyncStore` |
| Room | `…Entity`, `…Dao` in one file named after the entity | `ClothingAttributesEntity.kt` |
| Entity ⇄ domain | `toDomain()` / `toEntity()` extension functions, `internal` | — |
| Enum | singular noun; values `UPPER_SNAKE`; user text in a `label` property, never `name` | `Warmth.LIGHT.label` |
| Compose state holder | `…State` class + `remember…()` factory | `TodayOutfitIdeaState`, `rememberTodayOutfitIdea()` |
| Screen-level sealed state | `…State` with `Loading` / `Failed` / `Ready` | `ReviewState` |

Booleans read as statements: `included`, `ready`, `isArchived`, `drawnByUser`,
`rainExpected`. Avoid negatives (`notReady`) and bare `flag`/`status`.

A function returning "why not" is `blockingProblem: String?` (null means go). Use that
name wherever a draft can be invalid, so the UI code looks the same everywhere.

## 4. Compose

| Suffix | Use for | Examples |
|---|---|---|
| `…Screen` | full-screen destination | `WardrobeScreen`, `StylingScreen` |
| `…Dialog` | full-screen or modal flow with its own dismiss | `GarmentReviewDialog`, `GarmentDetailsDialog`, `SyncConflictDialog` |
| `…Content` | the body of a container owned elsewhere | `TodayOutfitIdeaContent`, `SyncConflictContent` |
| `…Card`, `…Row`, `…Thumbnail`, `…Editor`, `…Overview` | reusable parts, named for what they show | `SurenessRow`, `WardrobeThumbnail`, `GarmentDraftEditor`, `GarmentPhotoOverview` |
| `…Preview` | `@Preview` functions, `private`, sample data only | `SyncConflictContentPreview` |

Prefix by subject, not by technology: `Garment…` for one item being added or edited,
`Wardrobe…` for things about the whole wardrobe, `Outfit…` / `Styling…` for combinations,
`Sync…` for sync. Callbacks are `on` + past or imperative verb from the caller's view:
`onFinished`, `onCancel`, `onReview`, `onDrawn`, `onToggleIncluded`. Parameters that pass
a value down are nouns (`draft`, `session`, `enabled`).

Visibility: `internal` for anything only this feature uses; `public` only for entry points
other features call (`GarmentReviewDialog`, `StylingScreen`, `rememberTodayOutfitIdea`,
`TodayOutfitIdeaContent`).

## 5. Database

- Tables: plural `snake_case` nouns, prefixed by subject: `clothing_items`,
  `clothing_item_attributes`, `outfit_feedback`, `wardrobe_media`.
- Columns: `snake_case`; ids end `_id`; hex colours end `_hex`; timestamps are
  `created_at` / `updated_at` in epoch milliseconds; local dates are ISO text in `date`.
- Enums are stored as their Kotlin `name`. Never rename a stored enum value; add a new one
  and map the old one when reading. Readers must treat an unknown name as "not set".
- Lists in one column are comma-separated and sorted (`occasions`, `item_ids`).
- Migrations: `MIGRATION_<from>_<to>`, one step each, additive where possible, registered
  in `DefaultAppGraph`. One schema version per shipped change; never edit a migration that
  has run on a device. Schema JSON comes from the build, never by hand.
- Each migration gets `MigrationTest.migrate<from>To<to>`.

## 6. Models, assets and stored keys

- Model id: `dayloop-<what>` in kebab-case (`dayloop-garment-shape`); third-party models
  keep the vendor first (`mlkit-image-labeling-base`).
- Model version: semantic `MAJOR.MINOR.PATCH`. MAJOR when classes or input change, MINOR
  on retraining, PATCH on a re-export with identical behaviour.
- Asset file: `assets/wardrobe/<what>_v<MAJOR>.bin` (`garment_shape_v1.bin`). A new MAJOR
  is a new file; keep the old one until no stored `model_version` needs it.
- Test fixtures: `test/resources/wardrobe/<what>_<purpose>_v<MAJOR>.bin`.
- Licence notices: `assets/licenses/<source>_<licence>.txt` (`fashion_mnist_mit.txt`).
- Schema constants: `SCHEMA_VERSION` on the type they version.
- Sync field keys: `snake_case` constants on `WardrobeSyncMapper`; a key is permanent
  once any device has synced it.
- FileProvider authority: `${applicationId}.wardrobe.<purpose>`; cache folders
  `wardrobe_<purpose>/`; XML `wardrobe_<purpose>_paths.xml`.
- Logcat tags: `Wardrobe<Purpose>`, for measurements only. Never log photos, labels,
  predictions or item names.

## 7. Tests

- JVM tests mirror the package of the code and are named `<Type>Test`. Test names are
  sentences in backticks stating the behaviour:
  `` `a shape-only guess is never shown as confident` ``.
- Instrumented tests use `camelCase` method names (backtick names are not reliable on
  device): `itemAndDetailsRoundTripTogether`.
- Fakes are `Fake<Thing>` and live in the test that uses them; in-memory implementations
  shipped in `main` are `InMemory<Thing>`.
- Anything synthetic says so in its name or first line of output
  (`synthetic evaluation of contract metrics`).

## 8. User-facing text

- Buttons are verbs and say what happens: "Save to wardrobe", "Add 2 items",
  "Wear this", "Keep selected". Avoid "OK", "Submit", "Confirm".
- Sureness is always in words: "Sure", "Check", "Can't tell"; add the percentage only in
  the full form.
- Things the app could not determine are "Couldn't tell" or "Not set", never blank and
  never a guess.
- Items the user does not own are "ideas to add" and carry "(not in your wardrobe)".
- A suggestion is "an idea" until the user records it; then it is "today's outfit".
- State where data goes when it matters: "Recognised on this device", "Stays on this
  phone".

## 9. Documents and other files

| What | Where | Name |
|---|---|---|
| Decisions | `docs/adr/` | `NNNN-kebab-case-title.md`, next free number |
| Feature reference | `docs/wardrobe/` | `UPPER_SNAKE.md` by topic: `README`, `MODEL_AND_DATA`, `EVALUATION`, `NAMING_GUIDE` |
| Session handoff | `docs/` | `<FEATURE>_<TOPIC>_HANDOFF.md` |
| Model tooling | `tools/wardrobe-model/` | `snake_case.py`; outputs named as the app expects them |
| Icons | `res/drawable/` | `ic_action_<verb>` for actions, `ic_<noun>` for subjects |
| Design reference | canvas "DayLoop Wardrobe UI Reference" | built screens numbered `1..n`, ideas lettered `A..` |

## 10. Checklist before adding something

1. Is there already a word for it in section 1? Use it.
2. Does the file name equal its main type or screen?
3. Can it live in `core/` without Android imports? Then it should, with a test beside it.
4. New stored value (column, enum name, sync key, asset)? It is permanent: check
   sections 5 and 6 and add the migration and its test.
5. New text a person reads? Check section 8.

## 11. Using the theme

- **Colour:** in screens, read `MaterialTheme.dayLoopTokens.colors`. Use
  `MaterialTheme.colorScheme` only where Material needs it (component defaults) and for
  the inverse pair (`inverseSurface` / `inverseOnSurface`) on deliberately contrasting
  surfaces. No `Color(0x…)`, `Color.Black` or `Color.White` in feature code; decorative
  exceptions (confetti, calendar ink) get a one-line comment saying why.
- **Shape:** `MaterialTheme.shapes.small` (4dp) for cards, fields and tiles,
  `MaterialTheme.shapes.extraLarge` (8dp) for dialogs and sheets, or
  `dayLoopTokens.shapeScale.*`. No literal corner radii above 8dp.
- **Buttons:** `DayLoopButton` for the one primary action on a surface,
  `DayLoopOutlinedButton` for the alternative, `TextButton` for low-emphasis actions and
  dialog buttons, `DayLoopIconButton` for icon-only actions.
- **Type:** only styles defined in `Type.kt`. If a screen needs a new one, add it there
  first. Avoid per-call `fontWeight` and `.sp` overrides.
- **Spacing:** 24dp page margin; 4/8/12/16/24 inside. Prefer `dayLoopTokens.spacing.*`
  in new code.
