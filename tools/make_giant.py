#!/usr/bin/env python3
"""Membuat figur konstelasi raksasa (titik bintang 3D + garis + mata) untuk Ra dan Thor.

Koordinat figur (satuan blok, sebelum diperbesar): u = kanan (dilihat dari kamera depan), v = atas, w = ke arah kamera.
Hasil: assets/kontrakdewa/constellations/<dewa>.json  { "stars":[[u,v,w]], "lines":[[a,b]], "eyes":[[u0,v0,u1,v1,w]] }
Ubah definisi di bawah lalu jalankan ulang untuk menyetel bentuk. Pratinjau: python3 make_giant.py preview
"""
import json, math, sys, os

class Fig:
    def __init__(self):
        self.stars, self.lines, self.eyes, self.polys = [], [], [], []
    def n(self, u, v, w=0.0):
        p = [round(u, 3), round(v, 3), round(w, 3)]
        for i, q in enumerate(self.stars):
            if abs(q[0]-p[0]) < .02 and abs(q[1]-p[1]) < .02 and abs(q[2]-p[2]) < .02: return i
        self.stars.append(p); return len(self.stars)-1
    def l(self, a, b):
        if a != b and [a, b] not in self.lines and [b, a] not in self.lines: self.lines.append([a, b])
    def path(self, *pts):
        ids = [self.n(*p) for p in pts]
        for a, b in zip(ids, ids[1:]): self.l(a, b)
        return ids
    def box(self, cu, cv, cw, su, sv, sw):
        x0, x1, y0, y1, z0, z1 = cu-su/2, cu+su/2, cv-sv/2, cv+sv/2, cw-sw/2, cw+sw/2
        c = {}
        for i, (x, y, z) in enumerate([(x0,y0,z0),(x1,y0,z0),(x1,y1,z0),(x0,y1,z0),(x0,y0,z1),(x1,y0,z1),(x1,y1,z1),(x0,y1,z1)]):
            c[i] = self.n(x, y, z)
        for a, b in [(0,1),(1,2),(2,3),(3,0),(4,5),(5,6),(6,7),(7,4),(0,4),(1,5),(2,6),(3,7)]: self.l(c[a], c[b])
        return c
    def ring(self, cu, cv, cw, r, n):
        ids = [self.n(cu+math.cos(2*math.pi*i/n)*r, cv+math.sin(2*math.pi*i/n)*r, cw) for i in range(n)]
        for i in range(n): self.l(ids[i], ids[(i+1) % n])
        return ids
    def poly(self, *ids): self.polys.append(list(ids))
    def eye(self, u0, v0, u1, v1, w): self.eyes.append([round(u0,3), round(v0,3), round(u1,3), round(v1,3), round(w,3)])

def body(f, head_v=5.55):
    # kepala kotak + mata
    hb = f.box(0, head_v, 0, 1.5, 1.4, 1.5)
    f.poly(hb[4], hb[5], hb[6], hb[7])
    f.eye(-0.55, head_v+0.05, -0.12, head_v+0.2, 0.76); f.eye(0.12, head_v+0.05, 0.55, head_v+0.2, 0.76)
    neck = f.n(0, 4.85, 0)
    f.l(f.n(0, head_v-0.7, 0), neck)
    SL, SR = f.n(-1.65, 4.4), f.n(1.65, 4.4)
    C, P = f.n(0, 3.5), f.n(0, 2.2)
    HL, HR = f.n(-0.85, 2.1), f.n(0.85, 2.1)
    for a, b in [(neck,SL),(neck,SR),(SL,SR),(neck,C),(SL,C),(SR,C),(C,P),(SL,HL),(SR,HR),(P,HL),(P,HR),(HL,HR)]: f.l(a, b)
    f.poly(SL, SR, HR, HL)
    return dict(SL=SL, SR=SR, HL=HL, HR=HR, C=C, P=P)

def thor():
    f = Fig(); b = body(f)
    # kaki
    kl, kr, fl, fr = f.n(-0.9, 1.0), f.n(0.9, 1.0), f.n(-1.0, 0.0), f.n(1.0, 0.0)
    f.l(b["HL"], kl); f.l(kl, fl); f.l(b["HR"], kr); f.l(kr, fr)
    f.poly(b["HL"], b["P"], b["HR"], kr, kl)
    # lengan kiri turun
    el = f.n(-2.4, 3.2); f.l(b["SL"], el); f.l(el, f.n(-1.9, 2.0))
    # lengan kanan terangkat memegang palu
    er = f.n(2.7, 5.3); hand = f.n(2.9, 6.6)
    f.l(b["SR"], er); f.l(er, hand)
    hm = f.box(2.9, 7.9, 0, 2.4, 1.0, 1.0)
    f.poly(hm[4], hm[5], hm[6], hm[7])
    f.l(hand, f.n(2.9, 7.4, 0))
    # lambang palu (salib kecil di muka palu)
    f.path((2.65, 7.95, 0.52), (3.15, 7.95, 0.52)); f.path((2.9, 7.65, 0.52), (2.9, 8.2, 0.52))
    return f

def ra():
    f = Fig(); b = body(f)
    # cakram matahari di belakang kepala + sinar pendek
    ids = f.ring(0, 5.75, -0.9, 1.55, 16)
    for i in range(0, 16, 2):
        a = 2*math.pi*i/16
        f.l(ids[i], f.n(math.cos(a)*2.05, 5.75+math.sin(a)*2.05, -0.9))
    # jubah melebar
    hl, hr = f.n(-1.7, 0.0), f.n(1.7, 0.0)
    f.l(b["HL"], hl); f.l(b["HR"], hr); f.l(hl, hr); f.l(b["P"], f.n(0, 0.0))
    f.poly(b["HL"], b["HR"], hr, hl)
    f.poly(*ids)
    # lengan kiri turun
    el = f.n(-2.3, 3.2); f.l(b["SL"], el); f.l(el, f.n(-1.7, 2.3))
    # lengan kanan memegang tongkat; tongkat tinggi dengan kait
    er = f.n(2.5, 3.8); hand = f.n(2.8, 3.0)
    f.l(b["SR"], er); f.l(er, hand)
    f.path((2.8, 0.0), (2.8, 3.0), (2.8, 6.0), (2.8, 8.4), (2.3, 8.95), (1.8, 8.6))
    return f

def save(f, god):
    out = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/kontrakdewa/constellations", god + ".json")
    json.dump({"stars": f.stars, "lines": f.lines, "eyes": f.eyes, "polys": f.polys}, open(out, "w"), separators=(",", ":"))
    print(god, "bintang:", len(f.stars), "garis:", len(f.lines), "mata:", len(f.eyes), "poligon:", len(f.polys))

if __name__ == "__main__":
    figs = {"ra": ra(), "thor": thor()}
    for g, f in figs.items(): save(f, g)
    if len(sys.argv) > 1 and sys.argv[1] == "preview":
        import matplotlib; matplotlib.use("Agg"); import matplotlib.pyplot as plt
        fig, axs = plt.subplots(1, 2, figsize=(9, 7), facecolor="#05081a")
        for ax, (g, f) in zip(axs, figs.items()):
            ax.set_facecolor("#05081a")
            for a, b in f.lines: ax.plot([f.stars[a][0], f.stars[b][0]], [f.stars[a][1], f.stars[b][1]], color="#8fc4ff", lw=1)
            ax.scatter([s[0] for s in f.stars], [s[1] for s in f.stars], color="white", s=10, zorder=3)
            for e in f.eyes: ax.fill([e[0], e[2], e[2], e[0]], [e[1], e[1], e[3], e[3]], color="white")
            ax.set_aspect("equal"); ax.axis("off"); ax.set_title(g, color="white")
        fig.savefig("/home/claude/prev_giant.png", dpi=100, facecolor="#05081a")
