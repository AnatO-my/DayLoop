# Evaluation

Recognition and recommendation are evaluated separately. Every number below was produced
on 2026-11-10 by the committed scripts and tests; none describes real phone photos.

## 1. Recognition

### 1.1 Shape model on Fashion-MNIST (measured, in-distribution)

Dataset: Fashion-MNIST official 10,000-image test set (MIT). Training used 55,000 images;
5,000 were held out only to fit the temperature. Seed 20261110, 24 epochs,
105,866 parameters, weights sha256 `e182af2f6807a498…`.
Source: `tools/wardrobe-model/eval_report.json`.

| Input condition | Top-1 | Top-2 | Top-3 | Macro-F1 | 6-category accuracy | ECE before | ECE after temperature |
|---|---|---|---|---|---|---|---|
| Input the app builds (silhouette + faint texture) | 0.8955 | 0.9734 | 0.9926 | 0.8949 | 0.9578 | 0.0246 | 0.0120 |
| Raw Fashion-MNIST | 0.8972 | 0.9735 | 0.9917 | 0.8963 | 0.9572 | 0.0307 | 0.0110 |
| Silhouette only | 0.8647 | 0.9636 | 0.9893 | 0.8642 | 0.9407 | 0.0293 | 0.0148 |
| App input, texture polarity inverted | 0.8798 | 0.9660 | 0.9911 | 0.8794 | 0.9508 | 0.0376 | 0.0077 |

"6-category accuracy" is after mapping the ten classes onto DayLoop's categories.
ECE is expected calibration error over 15 bins.

Per class, app-input condition:

| Class | Precision | Recall | F1 | Support |
|---|---|---|---|---|
| T_SHIRT_TOP | 0.863 | 0.824 | 0.843 | 1000 |
| TROUSER | 0.985 | 0.986 | 0.986 | 1000 |
| PULLOVER | 0.798 | 0.858 | 0.827 | 1000 |
| DRESS | 0.871 | 0.927 | 0.898 | 1000 |
| COAT | 0.835 | 0.780 | 0.807 | 1000 |
| SANDAL | 0.966 | 0.984 | 0.975 | 1000 |
| SHIRT | 0.724 | 0.696 | 0.710 | 1000 |
| SNEAKER | 0.947 | 0.964 | 0.955 | 1000 |
| BAG | 0.980 | 0.987 | 0.984 | 1000 |
| ANKLE_BOOT | 0.982 | 0.949 | 0.965 | 1000 |

Shirt, T-shirt, pullover and coat are confused with one another; this is why the app can
offer "Tops" while leaving the exact type empty.

Selective prediction (what a confidence threshold buys), app-input condition:

| Threshold | Types shown | Type accuracy when shown | Categories shown | Category accuracy when shown |
|---|---|---|---|---|
| 0.5 | 0.972 | 0.909 | 0.996 | 0.960 |
| 0.6 | 0.925 | 0.929 | 0.977 | 0.967 |
| 0.7 | 0.874 | 0.949 | 0.955 | 0.975 |
| 0.8 | 0.818 | 0.963 | 0.926 | 0.983 |
| 0.9 | 0.730 | 0.982 | 0.876 | 0.992 |

`RecognitionPolicy` uses 0.55 (tentative) and 0.80 (confident) from this table, and caps
shape-only guesses below "confident".

**What this does not show.** Fashion-MNIST images are centred studio product shots. Phone
photos differ in background, lighting, folds, hangers and people. Accuracy on real
wardrobe photos is **unmeasured**. The segmentation step and silhouette-style input are
intended to narrow that gap, but that is a design intention, not a result.

### 1.2 Implementation parity (measured)

`ShapeClassifierTest` runs the shipped Kotlin forward pass on 200 fixed test images and
compares with the training framework: largest logit difference under 1e-3, and 184 of 200
correct, identical to the export-time figure.

### 1.3 Colour, labels, fusion (tested on synthetic inputs only)

`ColorAnalyzerTest`, `PredictionFusionTest`, `GarmentDraftTest` use constructed pixels,
label lists and probability vectors. They verify behaviour (shaded navy reads as solid
navy; background outside the mask is ignored; disagreement lowers confidence; a person in
frame disables the shape model; uncertain fields stay empty; material is never pre-filled).
They are not accuracy measurements.

### 1.4 Plan for real-world recognition evaluation (not done)

1. Collect 300+ consented photos: flat-lay, hanger and worn, in daylight and indoor light,
   at least 30 per wardrobe category, labelled by the owner through the review screen.
2. Report per-class precision/recall, macro-F1 and top-3 for type; accuracy for category;
   colour-family agreement with the owner's label; share of items left "not sure" and how
   often those would have been wrong (the review of uncertain predictions).
3. Re-fit the temperature and thresholds on real data and re-check ECE.
4. Track the correction rate from `RecognitionStamp.userCorrected` per model version.

## 2. Recommendation

There is no ground truth for taste, so the engine is evaluated on its contract.

### 2.1 Synthetic contract evaluation (measured; random wardrobes, not user data)

`OutfitRecommenderTest.synthetic evaluation of contract metrics`, seed 20261110, 300
random wardrobes (4–24 items) with random requests. An independent brute-force oracle
decides whether a fully compliant outfit exists.

| Criterion | Result |
|---|---|
| Uses only owned, active, non-excluded items; no item twice | 300 / 300 requests (asserted) |
| Complete outfit returned when the wardrobe allows one | 206 / 206 |
| Top outfit meets every occasion/weather/rain constraint when some outfit can | 111 / 111 |
| Outfits claiming "no unmet constraint" while breaking one | 0 |
| Wardrobe coverage: share of active items appearing across the returned outfits | 0.366 |
| Diversity: mean pairwise Jaccard distance between returned outfits | 0.690 (259 requests with more than one outfit) |

### 2.2 Behavioural tests (pass)

Anchor item always included; hot weather avoids warm pieces and layers; cold adds a layer;
rain avoids sandals; clashing strong colours rank below a neutral pairing and are
explained; recently worn items are marked down and flagged; missing footwear gives a
partial outfit plus a separate "idea to add"; a dress fills top and bottom; unknown
attributes are neutral and reported; output is deterministic.

### 2.3 Plan for user-feedback evaluation (not done)

Log locally, with consent: which suggestion was accepted ("Wear this today"), which was
skipped, and edits made before recording. Report acceptance rate by rank and by which
cautions were present, and use it to re-weight the scoring components.

## 3. Offline, sync and performance

- **Offline:** recognition, storage, styling and sync logic have no network calls; the
  app has no INTERNET permission. Device airplane-mode walk-through: not done.
- **Sync/conflict tests (pass, against an in-memory fake server):** different-field edits
  merge; same-field edits conflict without overwriting; delete-versus-edit conflicts;
  lost acknowledgement retried without double apply; offline edits coalesce and back off;
  duplicate delivery ignored.
- **Migration 17 -> 18:** the build-generated `18.json` exists and its CREATE statement is
  identical to `MIGRATION_17_18`; the instrumented test itself has not been run.
- **Migration 18 -> 19 (`outfit_feedback`):** test authored; `19.json` not generated yet.
- **Second-pass logic (pass):** quick-confirm eligibility and sureness rows, several-item
  session (unsure items start unticked; ticked-but-incomplete items are reported, never
  saved), drag-to-region geometry, rejected combinations never re-suggested, feedback
  windows, conflict draft and record mapping. 19 further tests, 75 in total.
- **Performance, measured on a 2-core cloud x86 JVM (Xeon 2.1 GHz), not a phone:**
  shape forward pass median 1.33 ms, p95 1.93 ms; input builder on a 1024x1024 mask
  3.9 ms; colour analysis 1.1 ms. Model file 423,576 bytes.
- **Not measured (no device available):** on-phone latency, ML Kit labelling and
  segmentation time, memory, battery, APK size delta. `WardrobeAttributesTest` logs
  on-device model latency under the `WardrobeBench` tag when run.

## 4. Known limitations and likely failure cases

- Worn-on-body photos: the shape model is switched off when a person is detected; if the
  person cue is missed, the silhouette is a body, not a garment, and the guess may be wrong.
- Types outside the ten shape classes and the ML Kit vocabulary (skirts, shorts without a
  label hit, suits, traditional dress) come back as category-only or "not sure".
- Several overlapping items may be segmented as one.
- Colour shifts under warm or dim light; white/grey/beige are the most affected.
- "Patterned" is inferred from two unrelated colours and can be triggered by colour
  blocking or leaked background; it is always tentative.
- Softmax confidence on out-of-distribution photos can be overconfident, which is why
  shape-only results are capped.
- The recommender encodes common Western colour and formality guidelines. They are
  conventions, presented as such, and will not match every culture or personal style.
- Accessories and headwear are only included when the user styles from one.
