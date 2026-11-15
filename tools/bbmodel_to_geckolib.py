#!/usr/bin/env python3
"""Konversi .bbmodel (format Generic/free) -> GeckoLib geo.json + animation.json + texture png.

Pemakaian:
  python3 bbmodel_to_geckolib.py <file.bbmodel> <nama_model> <geser_xz> <out_resources_dir>
  geser_xz = titik tengah X/Z model di Blockbench (Ra = 8, Thor = 0), supaya patung berada di tengah blok.
"""
import json, sys, base64, os

src, name, shift, out = sys.argv[1], sys.argv[2], float(sys.argv[3]), sys.argv[4]
d = json.load(open(src))
els = {e["uuid"]: e for e in d["elements"]}
groups_meta = {g["uuid"]: g for g in d.get("groups", [])}
tw, th = d["resolution"]["width"], d["resolution"]["height"]

def sx(x): return x - shift  # geser X ke tengah
def sz(z): return z - shift

bones, used = [], set()
uuid_to_bone = {}

def cube_json(e):
    f, t = e["from"], e["to"]
    c = {
        "origin": [-(sx(t[0])), f[1], sz(f[2])],
        "size": [t[0] - f[0], t[1] - f[1], t[2] - f[2]],
        "uv": {},
    }
    r = e.get("rotation")
    if r and any(abs(v) > 1e-6 for v in r):
        o = e.get("origin", [0, 0, 0])
        c["pivot"] = [-(sx(o[0])), o[1], sz(o[2])]
        c["rotation"] = [-r[0], -r[1], r[2]]
    for face, fd in e["faces"].items():
        if fd.get("texture") is None:
            continue
        u = fd["uv"]
        c["uv"][face] = {"uv": [u[0], u[1]], "uv_size": [u[2] - u[0], u[3] - u[1]]}
    return c

def walk(node, parent):
    if isinstance(node, str):
        return
    meta = groups_meta.get(node.get("uuid"), {})
    gname = node.get("name") or meta.get("name") or "bone_" + node["uuid"][:6]
    origin = node.get("origin") or meta.get("origin") or [0, 0, 0]
    rot = node.get("rotation") or meta.get("rotation") or [0, 0, 0]
    base, i = gname, 2
    while gname in used:
        gname = f"{base}_{i}"; i += 1
    used.add(gname)
    uuid_to_bone[node["uuid"]] = gname
    bone = {"name": gname, "pivot": [-(sx(origin[0])), origin[1], sz(origin[2])]}
    if parent: bone["parent"] = parent
    if any(abs(v) > 1e-6 for v in rot):
        bone["rotation"] = [-rot[0], -rot[1], rot[2]]
    cubes = []
    for ch in node.get("children", []):
        if isinstance(ch, str):
            if ch in els: cubes.append(cube_json(els[ch]))
    if cubes: bone["cubes"] = cubes
    bones.append(bone)
    for ch in node.get("children", []):
        if isinstance(ch, dict): walk(ch, gname)

for n in d["outliner"]:
    if isinstance(n, dict): walk(n, None)
    # (elemen yang langsung di root tanpa group diabaikan)

# elemen yatim (tidak masuk group mana pun) -> bone "extra"
placed = set()
def collect(node):
    for ch in node.get("children", []):
        if isinstance(ch, str): placed.add(ch)
        else: collect(ch)
for n in d["outliner"]:
    if isinstance(n, dict): collect(n)
orph = [cube_json(e) for u, e in els.items() if u not in placed]
if orph:
    bones.append({"name": "extra", "pivot": [0, 0, 0], "cubes": orph})
    print("PERINGATAN: ada", len(orph), "cube tanpa group -> bone 'extra'")

geo = {"format_version": "1.12.0", "minecraft:geometry": [{
    "description": {"identifier": f"geometry.{name}", "texture_width": tw, "texture_height": th,
                    "visible_bounds_width": 12, "visible_bounds_height": 12, "visible_bounds_offset": [0, 3, 0]},
    "bones": bones}]}

def vec(dp, kind):
    def num(v):
        try: return float(v)
        except Exception: return 0.0
    x, y, z = num(dp["x"]), num(dp["y"]), num(dp["z"])
    if kind == "position": return [-x, y, z]
    if kind == "rotation": return [-x, -y, z]
    return [x, y, z]

anims = {}
for a in d.get("animations", []):
    ob = {}
    for au, an in a.get("animators", {}).items():
        bname = uuid_to_bone.get(au) or an.get("name")
        if bname not in used:
            print("PERINGATAN: animator tanpa bone:", an.get("name")); continue
        chans = {}
        for kf in an.get("keyframes", []):
            ch = kf["channel"]
            if ch not in ("position", "rotation", "scale"): continue
            v = vec(kf["data_points"][0], ch)
            key = str(round(kf["time"], 4))
            if kf.get("interpolation") == "catmullrom":
                chans.setdefault(ch, {})[key] = {"post": v, "lerp_mode": "catmullrom"}
            else:
                chans.setdefault(ch, {})[key] = v
        if chans: ob[bname] = chans
    loop = a.get("loop") == "loop"
    anims[a["name"]] = {"loop": loop, "animation_length": a["length"], "bones": ob}
anim = {"format_version": "1.8.0", "animations": anims}

for sub in ("geckolib/models/block", "geckolib/animations/block", "textures/block"):
    os.makedirs(os.path.join(out, sub), exist_ok=True)
json.dump(geo, open(os.path.join(out, f"geckolib/models/block/{name}.geo.json"), "w"), separators=(",", ":"))
json.dump(anim, open(os.path.join(out, f"geckolib/animations/block/{name}.animation.json"), "w"), indent=1)
tex = d["textures"][0]["source"].split(",", 1)[1]
open(os.path.join(out, f"textures/block/{name}.png"), "wb").write(base64.b64decode(tex))
print(name, "bone:", len(bones), "cube:", sum(len(b.get("cubes", [])) for b in bones), "anim:", list(anims))
