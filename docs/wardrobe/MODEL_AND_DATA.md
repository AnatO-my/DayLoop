# Model and dataset choices

Research date: 2026-11-10. Claims with consequences cite their source at the end.

## Constraints found in the repository

Android only, minSdk 26, Kotlin/Compose, no INTERNET permission, no backend. ML Kit image
labelling (bundled, 17.0.9) and subject segmentation (Play services, 16.0.0-beta1) were
already dependencies. No training data of DayLoop's own exists.

## Approaches compared

No accuracy figures are given for options that were not run here.

| | A. ML Kit labels only | B. Chosen: shape CNN + ML Kit labels + colour analysis | C. Fashion CLIP-style model on device | D. Cloud vision model |
|---|---|---|---|---|
| Offline | yes | yes | yes | no |
| New dependency | none | none | LiteRT/ONNX runtime | INTERNET, API key, backend |
| Added app size | 0 | 424 KB (measured) | hundreds of MB (0.2B float32 parameters [6]) | small |
| Vocabulary | 400+ generic labels; no plain "shirt"/"T-shirt"/"trousers" [1] | 10 shape classes + the clothing labels in [1] + 17 colour families | open | open |
| Privacy | on device | on device | on device | photos leave the phone; needs consent |
| Cost | none | none | none | per request |
| Complexity | lowest | low | high (conversion, tokenizer, memory) | medium + policy work |
| Real-photo accuracy | unmeasured | unmeasured | unmeasured here | unmeasured here |

B was chosen because it fills A's vocabulary gap for the most common garments, adds no
dependency or network use, is small enough to verify line by line, and its training data
is unambiguously licensed. C is the likely next step once there is a labelled set of real
wardrobe photos to justify its size. D conflicts with the app's offline design.

A LiteRT runtime was considered for B and rejected: the model is about 1.1 M multiply-adds,
so a plain-Kotlin forward pass avoids a native dependency and lets the JVM unit tests check
the exact code that ships.

## Datasets

| Dataset | Licence / terms | Used? |
|---|---|---|
| Fashion-MNIST | MIT, © 2017 Zalando SE [2] | Yes, training and evaluation. Notice shipped in `assets/licenses/fashion_mnist_mit.txt`. Data not redistributed |
| DeepFashion | "available for non-commercial research purposes only"; signed agreement required; images "not property of MMLAB" [3] | No |
| DeepFashion2 | Access by form and password; repository states no licence [4] | No, rights unclear |
| Fashionpedia | Annotations CC BY 4.0; project "does not own the copyright of the images" [5] | No, image rights vary per source |
| Polyvore Outfits (outfit compatibility) | No clear licence located during this research | No. This is why recommendations are rule-based, not learned |

## Components in use

- **Shape model** `dayloop-garment-shape` 1.0.0: conv 3x3x16, pool, conv 3x3x32, pool,
  dense 64, dense 10; 105,866 parameters; temperature 0.7977 fitted on a 5,000-image
  validation split. Trained with silhouette/texture/geometry augmentation so that the
  input the phone can produce (a filled silhouette with faint texture) is in distribution.
  Classes: T-shirt/top, trouser, pullover, dress, coat, sandal, shirt, sneaker, bag,
  ankle boot.
- **ML Kit image labelling** (bundled base model, "400+ labels", about 5.7 MB already in
  the app) [7]. `LabelEvidenceMapper` maps the clothing-related labels in the published
  label map [1]; labels with no honest wardrobe equivalent are ignored. Person-related
  labels disable the shape model for that photo.
- **ML Kit subject segmentation** (beta; model delivered by Google Play services; minimum
  512x512 input recommended) [8]. Multi-subject confidence masks give per-item regions.
- **ML Kit terms**: processing "fully happens on-device"; Google may collect API metrics
  and the app must inform users [9].

## What is deliberately not inferred

Material (only a hint from texture labels, never pre-filled), fit, brand, size, gender,
price, and anything about the person in a photo. Season is replaced by user-set warmth,
with a per-type default that is always labelled as an assumption.

## Improving the model

1. Add an explicit opt-in to keep `RecognitionStamp` corrections (already stored locally:
   model version, confidence, whether the user corrected it) together with the cut-out.
2. Build an evaluation set first (a few hundred user-consented photos across the
   categories), measure the current system, then decide between fine-tuning a small
   image backbone and option C.
3. Use only data with clear rights: consented user photos, or CC0/CC BY sources with
   attribution recorded per image.

## Sources

1. ML Kit image labelling label map — https://developers.google.com/ml-kit/vision/image-labeling/label-map
2. Fashion-MNIST licence — https://raw.githubusercontent.com/zalandoresearch/fashion-mnist/master/LICENSE
3. DeepFashion project page — https://mmlab.ie.cuhk.edu.hk/projects/DeepFashion.html
4. DeepFashion2 README — https://raw.githubusercontent.com/switchablenorms/DeepFashion2/master/README.md
5. Fashionpedia terms — https://fashionpedia.github.io/home/data_license.html
6. Marqo-FashionSigLIP model card (Apache-2.0, 0.2B params) — https://huggingface.co/Marqo/marqo-fashionSigLIP
7. ML Kit image labelling for Android — https://developers.google.com/ml-kit/vision/image-labeling/android
8. ML Kit subject segmentation for Android — https://developers.google.com/ml-kit/vision/subject-segmentation/android
9. ML Kit terms — https://developers.google.com/ml-kit/terms
