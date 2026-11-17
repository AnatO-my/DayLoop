# Garment-shape model tooling

Reproduces `app/src/main/assets/wardrobe/garment_shape_v1.bin`, the parity fixture used by
`ShapeClassifierTest`, and every number in `docs/wardrobe/EVALUATION.md`.

```bash
# 1. Get Fashion-MNIST (MIT licence, Zalando SE). Do not commit it.
mkdir -p /tmp/fmnist && cd /tmp/fmnist
for f in train-images-idx3-ubyte train-labels-idx1-ubyte t10k-images-idx3-ubyte t10k-labels-idx1-ubyte; do
  curl -LO https://raw.githubusercontent.com/zalandoresearch/fashion-mnist/master/data/fashion/$f.gz
done

# 2. Train, calibrate, evaluate, export (about 7 minutes on 2 CPU cores, no GPU needed).
pip install torch numpy scipy
python tools/wardrobe-model/train_shape_model.py --data /tmp/fmnist --out /tmp/shape-out

# 3. Install the outputs.
cp /tmp/shape-out/garment_shape_v1.bin app/src/main/assets/wardrobe/
cp /tmp/shape-out/shape_parity_v1.bin  app/src/test/resources/wardrobe/
cp /tmp/shape-out/eval_report.json     tools/wardrobe-model/
```

After retraining, update the expected fixture count in `ShapeClassifierTest`
(`parity_fixture_accuracy` x 200), the thresholds' evidence in `RecognitionPolicy`, and
bump `ShapeModelWeights.MODEL_VERSION`.

`eval_report.json` here is the report from the run that produced the committed weights
(seed 20261110, torch 2.15.0, sha256 of weights recorded inside).

The weights file is a plain little-endian float dump read by `ShapeModelWeights.kt`; there
is no TFLite/LiteRT or ONNX runtime involved.
