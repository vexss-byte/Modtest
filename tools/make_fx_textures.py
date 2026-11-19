#!/usr/bin/env python3
"""Membuat tekstur efek cutscene (putih + alpha; warna diberikan saat digambar):
   nebula_blob.png (awan lembut), star_glow.png (bintang berkilau + silang), mist.png (kabut lebar)."""
import numpy as np, os
from scipy.ndimage import zoom, gaussian_filter
from PIL import Image

out = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/kontrakdewa/textures/gui")
os.makedirs(out, exist_ok=True)
rng = np.random.default_rng(2024)

def fbm(h, w, octaves=5, base=4, persistence=0.55):
    total = np.zeros((h, w)); amp = 1.0; norm = 0.0
    for o in range(octaves):
        n = base * (2 ** o)
        g = rng.random((max(2, int(n * h / max(h, w))) + 1, max(2, int(n * w / max(h, w))) + 1))
        z = zoom(g, (h / g.shape[0], w / g.shape[1]), order=3)[:h, :w]
        total += amp * z; norm += amp; amp *= persistence
    t = total / norm
    return (t - t.min()) / (t.max() - t.min())

def save(arr_alpha, name):
    a = np.clip(arr_alpha, 0, 1)
    img = np.zeros(a.shape + (4,), np.uint8)
    img[..., :3] = 255
    img[..., 3] = (a * 255).astype(np.uint8)
    Image.fromarray(img, "RGBA").save(os.path.join(out, name))

# awan lembut bulat
S = 128
y, x = np.mgrid[0:S, 0:S]; r = np.hypot(x - S / 2 + .5, y - S / 2 + .5) / (S / 2)
f = fbm(S, S, 5, 3)
falloff = np.clip(1 - r, 0, 1) ** 1.6
save(falloff * (0.35 + 0.9 * f), "nebula_blob.png")

# bintang: inti + halo + silang tipis
S = 128
y, x = np.mgrid[0:S, 0:S]; dx = (x - S / 2 + .5) / (S / 2); dy = (y - S / 2 + .5) / (S / 2)
r = np.hypot(dx, dy)
core = np.exp(-(r * 9) ** 2)
halo = np.exp(-r * 5) * 0.55
cross = (np.exp(-np.abs(dx) * 38) * np.exp(-np.abs(dy) * 3.2) + np.exp(-np.abs(dy) * 38) * np.exp(-np.abs(dx) * 3.2)) * 0.9
save(np.clip(core + halo + cross, 0, 1) * np.clip(1.15 - r, 0, 1), "star_glow.png")

# kabut lebar bergumpal
W, H = 256, 96
f = fbm(H, W, 6, 5)
yy, xx = np.mgrid[0:H, 0:W]
edge = np.clip(1 - np.abs(yy - H / 2) / (H / 2), 0, 1) ** 1.2 * np.clip(1 - np.abs(xx - W / 2) / (W / 2), 0, 1) ** 0.6
m = gaussian_filter(np.clip((f - 0.38) * 2.2, 0, 1), 1.5)
save(m * edge * 0.9, "mist.png")
print("tekstur dibuat di", out)
