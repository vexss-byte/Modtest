#!/usr/bin/env python3
"""Pratinjau shader storm.fsh dengan OpenGL headless (Mesa llvmpipe): me-render shader, lalu menumpuknya di atas
'dunia' tiruan (langit siang + tanah + siluet patung) persis seperti blending GUI di game.
Pemakaian: python3 preview_shader.py <thor|ra> <keluaran.png> [detik kegelapan_0..1 kilatan_0..1 ...]"""
import sys, re, os
import numpy as np, moderngl
from PIL import Image

god = sys.argv[1] if len(sys.argv) > 1 else "thor"
out = sys.argv[2] if len(sys.argv) > 2 else "/tmp/shader_prev.png"
W, H = 960, 540
here = os.path.dirname(os.path.abspath(__file__))
src = open(os.path.join(here, "../src/main/resources/assets/kontrakdewa/shaders/core/storm.fsh")).read()

# ubah ke bentuk uji: uniform biasa
src = src.replace("#extension GL_ARB_separate_shader_objects : require\n", "")
src = re.sub(r"layout\(std140\) uniform Globals \{.*?\};", "uniform float GameTime;\nuniform vec2 ScreenSize;", src, flags=re.S)
src = re.sub(r"layout\(location = \d\) (in|out)", r"\1", src)
if god == "ra": src = src.replace("#version 330\n", "#version 330\n#define RA\n", 1)
vs = """#version 330
in vec2 pos; out vec2 texCoord0; out vec4 vertexColor; uniform vec4 Param;
void main(){ gl_Position=vec4(pos*2.0-1.0,0.0,1.0); texCoord0=vec2(pos.x,1.0-pos.y); vertexColor=Param; }"""

ctx = moderngl.create_standalone_context(backend="egl")
prog = ctx.program(vertex_shader=vs, fragment_shader=src)
vbo = ctx.buffer(np.array([0,0, 1,0, 0,1, 1,1], dtype="f4").tobytes())
vao = ctx.vertex_array(prog, [(vbo, "2f", "pos")])
tex = ctx.texture((2, 2), 4, bytes([255]*16)); tex.use(0)
if "Sampler0" in prog: prog["Sampler0"].value = 0
fbo = ctx.simple_framebuffer((W, H), components=4)

rad_g = 0.30
def world(spot):
    """langit siang + tanah + siluet patung (kotak gelap) di posisi sorotan"""
    y = np.linspace(0, 1, H)[:, None]; x = np.linspace(0, 1, W)[None, :]
    ones = np.ones((H, W))
    sky = np.stack([0.45*ones, (0.65 + 0.15*(1-y))*ones, (0.95 - 0.1*y)*ones], -1)
    ground = (y > 0.62)[..., None] * np.ones((1, W, 1))
    img = sky * (1 - ground) + np.array([0.25, 0.55, 0.22]) * ground
    cx, cy = spot[0]*W, spot[1]*H
    hh = rad_g * H
    X = np.arange(W)[None, :] - cx; Y = np.arange(H)[:, None] - cy
    body = (abs(X) < 0.42*hh) & (abs(Y) < hh)
    ped = (abs(X) < 0.60*hh) & (abs(Y - 0.78*hh) < 0.24*hh)
    img[body | ped] = np.array([0.20, 0.20, 0.23])
    return img

def render(dark, flash, t, spot=(0.40, 0.60), rad=0.30):
    prog["GameTime"].value = t / 1200.0
    prog["ScreenSize"].value = (W, H)
    a = (int(round(dark * 31)) << 3) | int(round(flash * 7))
    prog["Param"].value = (spot[0], spot[1], rad / 0.8, a / 255.0)
    fbo.use(); ctx.clear(0, 0, 0, 0); vao.render(moderngl.TRIANGLE_STRIP)
    d = np.frombuffer(fbo.read(components=4, dtype="f1"), dtype=np.uint8).reshape(H, W, 4)[::-1].astype(np.float32) / 255.0
    bg = world(spot)
    al = d[..., 3:4]
    return np.clip(bg * (1 - al) + d[..., :3] * al, 0, 1)

shots = []
frames = [(0.10, 0.0, 2.0), (0.55, 0.0, 3.6), (1.0, 0.0, 5.2), (1.0, 0.0, 6.8), (1.0, 0.85, 7.1), (1.0, 0.0, 7.8)]
for dk, fl, t in frames: shots.append(render(dk, fl, t + 300))
rows = [np.concatenate(shots[i:i+3], 1) for i in (0, 3)]
img = (np.concatenate(rows, 0) * 255).astype(np.uint8)
Image.fromarray(img).save(out)
print("tersimpan", out, img.shape)
