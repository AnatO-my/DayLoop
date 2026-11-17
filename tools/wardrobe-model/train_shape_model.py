#!/usr/bin/env python3
"""Train, calibrate, evaluate and export DayLoop's garment-shape classifier.

Data: Fashion-MNIST (Zalando SE, MIT licence). The dataset is NOT stored in the
DayLoop repository; pass --data pointing at a folder holding the four original
*.gz files from https://github.com/zalandoresearch/fashion-mnist (data/fashion).

Outputs (see --out):
  garment_shape_v1.bin       weights consumed by ShapeModelWeights.kt
  shape_parity_v1.bin        fixed inputs + reference logits for the Kotlin parity test
  eval_report.json           every number quoted in docs/wardrobe/EVALUATION.md

The numbers describe Fashion-MNIST only. They are not real-world phone-photo accuracy.
"""
import argparse, gzip, hashlib, json, struct, time
from pathlib import Path

import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F
from scipy.ndimage import binary_fill_holes

LABELS = ["T_SHIRT_TOP", "TROUSER", "PULLOVER", "DRESS", "COAT",
          "SANDAL", "SHIRT", "SNEAKER", "BAG", "ANKLE_BOOT"]
# DayLoop wardrobe category for each Fashion-MNIST class (mirrors GarmentType.kt).
COARSE = ["TOP", "BOTTOM", "TOP", "TOP", "OUTERWEAR",
          "FOOTWEAR", "TOP", "FOOTWEAR", "ACCESSORY", "FOOTWEAR"]
MASK_THRESHOLD = 10 / 255.0
DEVICE_LIFT = 0.6          # must equal ShapeInputBuilder.SILHOUETTE_FLOOR
SEED = 20261110


def read_idx(path):
    with gzip.open(path, "rb") as f:
        magic, n = struct.unpack(">II", f.read(8))
        if magic == 2051:
            r, c = struct.unpack(">II", f.read(8))
            return np.frombuffer(f.read(), dtype=np.uint8).reshape(n, r, c)
        return np.frombuffer(f.read(), dtype=np.uint8)


def silhouettes(x):
    """Filled garment masks; light garments have dark interiors in Fashion-MNIST."""
    return np.stack([binary_fill_holes(img > MASK_THRESHOLD) for img in x]).astype(np.float32)


def lift(x, m, a, invert=False):
    """What the phone can reproduce: a silhouette with a little interior texture."""
    t = (1.0 - x) if invert else x
    return m * (a + (1.0 - a) * t)


class ShapeNet(nn.Module):
    def __init__(self):
        super().__init__()
        self.conv1 = nn.Conv2d(1, 16, 3, padding=1)
        self.conv2 = nn.Conv2d(16, 32, 3, padding=1)
        self.fc1 = nn.Linear(32 * 7 * 7, 64)
        self.fc2 = nn.Linear(64, 10)
        self.drop = nn.Dropout(0.3)

    def forward(self, x):
        x = F.max_pool2d(F.relu(self.conv1(x)), 2)
        x = F.max_pool2d(F.relu(self.conv2(x)), 2)
        x = x.flatten(1)                      # NCHW flatten: channel, row, column
        x = self.drop(F.relu(self.fc1(x)))
        return self.fc2(x)


def augment(x, m, gen):
    n = x.shape[0]
    # appearance: raw / lifted / lifted with inverted texture
    mode = torch.rand(n, 1, 1, 1, generator=gen)
    a = 0.3 + 0.7 * torch.rand(n, 1, 1, 1, generator=gen)
    out = torch.where(mode < 0.3, x, torch.where(mode < 0.8, lift(x, m, a), lift(x, m, a, True)))
    # geometry: small rotation, scale, shift, mirror
    ang = (torch.rand(n, generator=gen) - 0.5) * 2 * np.deg2rad(8)
    sc = 1.0 + torch.rand(n, generator=gen) * 0.18          # >1 shrinks the garment
    flip = torch.where(torch.rand(n, generator=gen) < 0.5, -1.0, 1.0)
    tx = (torch.rand(n, generator=gen) - 0.5) * 2 * (2 / 14)
    ty = (torch.rand(n, generator=gen) - 0.5) * 2 * (2 / 14)
    theta = torch.zeros(n, 2, 3)
    theta[:, 0, 0] = torch.cos(ang) * sc * flip
    theta[:, 0, 1] = -torch.sin(ang) * sc
    theta[:, 1, 0] = torch.sin(ang) * sc * flip
    theta[:, 1, 1] = torch.cos(ang) * sc
    theta[:, 0, 2] = tx
    theta[:, 1, 2] = ty
    grid = F.affine_grid(theta, out.shape, align_corners=False)
    return F.grid_sample(out, grid, align_corners=False, padding_mode="zeros")


def logits_of(model, x):
    model.eval()
    with torch.no_grad():
        return torch.cat([model(x[i:i + 2000]) for i in range(0, len(x), 2000)])


def ece(probs, y, bins=15):
    conf, pred = probs.max(1)
    acc = (pred == y).float()
    edges = torch.linspace(0, 1, bins + 1)
    total = 0.0
    for lo, hi in zip(edges[:-1], edges[1:]):
        sel = (conf > lo) & (conf <= hi)
        if sel.any():
            total += sel.float().mean().item() * abs(acc[sel].mean().item() - conf[sel].mean().item())
    return total


def evaluate(logits, y, temperature):
    probs = F.softmax(logits / temperature, 1)
    conf, pred = probs.max(1)
    k = len(LABELS)
    cm = np.zeros((k, k), dtype=int)
    for t, p in zip(y.tolist(), pred.tolist()):
        cm[t, p] += 1
    per_class = {}
    f1s = []
    for i, name in enumerate(LABELS):
        tp = cm[i, i]
        prec = tp / max(cm[:, i].sum(), 1)
        rec = tp / max(cm[i].sum(), 1)
        f1 = 2 * prec * rec / max(prec + rec, 1e-12)
        f1s.append(f1)
        per_class[name] = {"precision": round(float(prec), 4), "recall": round(float(rec), 4),
                           "f1": round(float(f1), 4), "support": int(cm[i].sum())}
    topk = {f"top{j}": round((logits.topk(j, 1).indices == y[:, None]).any(1).float().mean().item(), 4)
            for j in (1, 2, 3)}
    coarse_true = [COARSE[i] for i in y.tolist()]
    names = sorted(set(COARSE))
    coarse_probs = torch.stack([probs[:, [i for i, c in enumerate(COARSE) if c == n]].sum(1) for n in names], 1)
    cconf, cidx = coarse_probs.max(1)
    coarse_ok = torch.tensor([names[i] == t for i, t in zip(cidx.tolist(), coarse_true)])
    coverage = {}
    for thr in (0.5, 0.6, 0.7, 0.8, 0.9):
        keep = conf >= thr
        ckeep = cconf >= thr
        coverage[str(thr)] = {
            "type_coverage": round(keep.float().mean().item(), 4),
            "type_accuracy_when_shown": round((pred[keep] == y[keep]).float().mean().item(), 4),
            "category_coverage": round(ckeep.float().mean().item(), 4),
            "category_accuracy_when_shown": round(coarse_ok[ckeep].float().mean().item(), 4),
        }
    return {
        "n": int(len(y)), **topk,
        "macro_f1": round(float(np.mean(f1s)), 4),
        "category_accuracy": round(coarse_ok.float().mean().item(), 4),
        "ece_uncalibrated": round(ece(F.softmax(logits, 1), y), 4),
        "ece_calibrated": round(ece(probs, y), 4),
        "per_class": per_class,
        "confusion_matrix_rows_true_cols_pred": cm.tolist(),
        "selective_prediction": coverage,
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--epochs", type=int, default=24)
    args = ap.parse_args()
    data, out = Path(args.data), Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    torch.manual_seed(SEED); np.random.seed(SEED)
    gen = torch.Generator().manual_seed(SEED)

    xtr = read_idx(data / "train-images-idx3-ubyte.gz").astype(np.float32) / 255.0
    ytr = read_idx(data / "train-labels-idx1-ubyte.gz").astype(np.int64)
    xte = read_idx(data / "t10k-images-idx3-ubyte.gz").astype(np.float32) / 255.0
    yte = read_idx(data / "t10k-labels-idx1-ubyte.gz").astype(np.int64)
    mtr, mte = silhouettes(xtr), silhouettes(xte)

    perm = np.random.permutation(len(xtr))
    val_idx, tr_idx = perm[:5000], perm[5000:]          # validation only fits the temperature
    T = lambda a: torch.from_numpy(a)
    x, m, y = T(xtr[tr_idx])[:, None], T(mtr[tr_idx])[:, None], T(ytr[tr_idx])
    xv, mv, yv = T(xtr[val_idx])[:, None], T(mtr[val_idx])[:, None], T(ytr[val_idx])
    xt, mt, yt = T(xte)[:, None], T(mte)[:, None], T(yte)

    model = ShapeNet()
    opt = torch.optim.AdamW(model.parameters(), lr=2e-3, weight_decay=1e-4)
    sched = torch.optim.lr_scheduler.OneCycleLR(opt, max_lr=4e-3, total_steps=args.epochs * ((len(x) + 127) // 128))
    t0 = time.time()
    for epoch in range(args.epochs):
        model.train()
        order = torch.randperm(len(x), generator=gen)
        for i in range(0, len(x), 128):
            idx = order[i:i + 128]
            loss = F.cross_entropy(model(augment(x[idx], m[idx], gen)), y[idx], label_smoothing=0.05)
            opt.zero_grad(); loss.backward(); opt.step(); sched.step()
        vacc = (logits_of(model, lift(xv, mv, DEVICE_LIFT)).argmax(1) == yv).float().mean().item()
        print(f"epoch {epoch + 1:2d}  val(device input) acc {vacc:.4f}  {time.time() - t0:.0f}s", flush=True)

    # Temperature scaling on the validation split, using the input the phone produces.
    val_logits = logits_of(model, lift(xv, mv, DEVICE_LIFT))
    log_t = torch.zeros(1, requires_grad=True)
    lbfgs = torch.optim.LBFGS([log_t], lr=0.1, max_iter=200)
    def closure():
        lbfgs.zero_grad()
        l = F.cross_entropy(val_logits / log_t.exp(), yv)
        l.backward()
        return l
    lbfgs.step(closure)
    temperature = float(log_t.exp().item())

    conditions = {
        "device_input_lift_0.6": lift(xt, mt, DEVICE_LIFT),
        "raw_fashion_mnist": xt,
        "pure_silhouette": lift(xt, mt, 1.0),
        "device_input_inverted_texture": lift(xt, mt, DEVICE_LIFT, True),
    }
    report = {
        "model_id": "dayloop-garment-shape", "model_version": "1.0.0",
        "dataset": "Fashion-MNIST (Zalando SE, MIT licence)",
        "split": "official 10,000-image test set; 55,000 train / 5,000 validation (temperature only)",
        "caveat": "In-distribution studio product images. Not a measure of accuracy on phone photos.",
        "seed": SEED, "epochs": args.epochs, "temperature": round(temperature, 4),
        "parameters": int(sum(p.numel() for p in model.parameters())),
        "torch": torch.__version__, "train_seconds_cpu": round(time.time() - t0, 1),
        "conditions": {k: evaluate(logits_of(model, v), yt, temperature) for k, v in conditions.items()},
    }

    # ---- export weights: little-endian, see ShapeModelWeights.kt ----
    sd = model.state_dict()
    blob = bytearray(b"DLGS") + struct.pack("<if", 1, temperature)
    names = ["conv1.weight", "conv1.bias", "conv2.weight", "conv2.bias",
             "fc1.weight", "fc1.bias", "fc2.weight", "fc2.bias"]
    blob += struct.pack("<i", len(names))
    for n in names:
        w = sd[n].numpy().astype("<f4")
        blob += struct.pack("<i", w.ndim) + struct.pack(f"<{w.ndim}i", *w.shape) + w.tobytes()
    (out / "garment_shape_v1.bin").write_bytes(blob)
    report["weights_sha256"] = hashlib.sha256(blob).hexdigest()
    report["weights_bytes"] = len(blob)

    # ---- parity fixture: 200 fixed test images as bytes + float32 reference logits ----
    idx = np.arange(0, 10000, 50)
    q = np.round(lift(xt[idx], mt[idx], DEVICE_LIFT).numpy() * 255).astype(np.uint8)
    ref = logits_of(model, torch.from_numpy(q.astype(np.float32) / 255.0)).numpy().astype("<f4")
    fx = bytearray(b"DLPF") + struct.pack("<i", len(idx)) + q.tobytes() + ref.tobytes() + yte[idx].astype(np.uint8).tobytes()
    (out / "shape_parity_v1.bin").write_bytes(fx)
    report["parity_fixture_accuracy"] = round(float((ref.argmax(1) == yte[idx]).mean()), 4)

    (out / "eval_report.json").write_text(json.dumps(report, indent=1))
    c = report["conditions"]["device_input_lift_0.6"]
    print("temperature", temperature, "| device-input top1", c["top1"], "macroF1", c["macro_f1"],
          "category acc", c["category_accuracy"], "ECE", c["ece_uncalibrated"], "->", c["ece_calibrated"])
    print("raw top1", report["conditions"]["raw_fashion_mnist"]["top1"],
          "| silhouette top1", report["conditions"]["pure_silhouette"]["top1"])


if __name__ == "__main__":
    main()
