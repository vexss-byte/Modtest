#!/usr/bin/env python3
"""Membuat data konstelasi (titik bintang + garis) otomatis dari file .bbmodel patung.

Cara kerja: proyeksi depan semua cube (kecuali alas/pedestal dan efek) dijadikan gambar siluet; kontur siluet
disederhanakan jadi poligon (titik bintang di sudutnya), lalu rangka dari titik tengah tiap group (kepala, torso,
lengan, dst.) ditambahkan sebagai garis dalam. Hasil: JSON berisi nodes [x,y] dalam satuan blok (x sudah dicerminkan
supaya terbaca dari depan patung) dan edges [a,b].

Pemakaian:
  python3 make_constellation.py <file.bbmodel> <geser_x> <keluaran.json> [epsilon] [preview.png]
"""
import json, sys
import numpy as np
import cv2

src, shift, out = sys.argv[1], float(sys.argv[2]), sys.argv[3]
eps = float(sys.argv[4]) if len(sys.argv) > 4 else 2.2
preview = sys.argv[5] if len(sys.argv) > 5 else None

d = json.load(open(src))
els = {e["uuid"]: e for e in d["elements"]}
gm = {g["uuid"]: g for g in d.get("groups", [])}
SKIP = {"pedestal", "base", "altar", "fx_bolts", "fx_runes", "root"}

groups = []  # (name, parent_index, cubes)
def walk(n, parent):
    if isinstance(n, str): return
    meta = gm.get(n.get("uuid"), {})
    name = (n.get("name") or meta.get("name") or "").lower()
    cubes = [els[c] for c in n.get("children", []) if isinstance(c, str) and c in els]
    idx = len(groups)
    groups.append((name, parent, cubes))
    for c in n.get("children", []):
        if isinstance(c, dict): walk(c, idx)
for n in d["outliner"]:
    if isinstance(n, dict): walk(n, None)

# ---- siluet ----
SC = 4  # piksel gambar per piksel model
W, H = 120 * SC, 120 * SC
ox = W // 2
mask = np.zeros((H, W), np.uint8)
def X(v): return int(round(ox + (-(v - shift)) * SC))      # cermin: terbaca dari depan
def Y(v): return int(round(H - 6 * SC - v * SC))
bbs = {}
for gi, (name, parent, cubes) in enumerate(groups):
    if name in SKIP or not cubes: continue
    xs, ys = [], []
    for e in cubes:
        x0, x1 = e["from"][0], e["to"][0]
        y0, y1 = e["from"][1], e["to"][1]
        cv2.rectangle(mask, (X(x1), Y(y1)), (X(x0), Y(y0)), 255, -1)
        xs += [x0, x1]; ys += [y0, y1]
    bbs[gi] = ((min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2)
mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, np.ones((3 * SC, 3 * SC), np.uint8))
cnts, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

nodes, edges = [], []
def add_node(px, py):
    # ke satuan blok; titik yang sangat dekat digabung
    bx, by = (px - ox) / SC / 16.0, (H - 6 * SC - py) / SC / 16.0
    for i, (nx, ny) in enumerate(nodes):
        if abs(nx - bx) < 0.12 and abs(ny - by) < 0.12: return i
    nodes.append([round(bx, 3), round(by, 3)])
    return len(nodes) - 1

for c in cnts:
    if cv2.contourArea(c) < 40 * SC * SC: continue
    ap = cv2.approxPolyDP(c, eps * SC, True)[:, 0, :]
    idxs = [add_node(p[0], p[1]) for p in ap]
    for i in range(len(idxs)):
        a, b = idxs[i], idxs[(i + 1) % len(idxs)]
        if a != b and [a, b] not in edges and [b, a] not in edges: edges.append([a, b])

# ---- rangka dalam: titik tengah group dihubungkan ke titik tengah induknya ----
center_node = {}
for gi, (cx, cy) in bbs.items():
    center_node[gi] = add_node(ox + (-(cx - shift)) * SC, H - 6 * SC - cy * SC)
for gi, nidx in center_node.items():
    p = groups[gi][1]
    while p is not None and p not in center_node: p = groups[p][1]
    if p is not None and center_node[p] != nidx:
        a, b = center_node[p], nidx
        if [a, b] not in edges and [b, a] not in edges: edges.append([a, b])

json.dump({"nodes": nodes, "edges": edges}, open(out, "w"), separators=(",", ":"))
print(out, "node:", len(nodes), "garis:", len(edges), "tinggi(blok): %.2f" % max(n[1] for n in nodes))

if preview:
    import matplotlib; matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    fig, ax = plt.subplots(figsize=(4, 6), facecolor="#05081a"); ax.set_facecolor("#05081a")
    for a, b in edges: ax.plot([nodes[a][0], nodes[b][0]], [nodes[a][1], nodes[b][1]], color="#bfe0ff", lw=1)
    ax.scatter([n[0] for n in nodes], [n[1] for n in nodes], color="white", s=14, zorder=3)
    ax.set_aspect("equal"); ax.axis("off"); fig.savefig(preview, dpi=110, facecolor="#05081a")
