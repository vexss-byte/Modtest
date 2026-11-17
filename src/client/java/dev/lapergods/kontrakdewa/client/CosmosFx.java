package dev.lapergods.kontrakdewa.client;

import dev.lapergods.kontrakdewa.God;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/** Efek kosmik untuk cutscene patung: tirai gelap, bintang berkelap-kelip, dan konstelasi dewa yang tergambar perlahan. */
final class CosmosFx {
    private CosmosFx() {}

    // ---- bintang latar ----
    private static final int STAR_COUNT = 140;
    private static final float[] SX = new float[STAR_COUNT], SY = new float[STAR_COUNT], SP = new float[STAR_COUNT];
    private static final int[] SSIZE = new int[STAR_COUNT];
    static {
        Random r = new Random(1337);
        for (int i = 0; i < STAR_COUNT; i++) {
            SX[i] = r.nextFloat();
            SY[i] = r.nextFloat();
            SP[i] = r.nextFloat() * 6.28f;
            SSIZE[i] = r.nextInt(10) == 0 ? 2 : 1;
        }
    }

    private static int argb(float alpha, int rgb) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static int tintRgb(God god) {
        return god == God.RA ? 0x1E1006 : 0x050B22;
    }

    private static int lineRgb(God god) {
        return god == God.RA ? 0xFFD36A : 0xBFE0FF;
    }

    private static int glowRgb(God god) {
        return god == God.RA ? 0xFF9A2A : 0x3F7FE0;
    }

    /**
     * Tirai gelap + kabut + vignette. spot = {cx, cy, rx, ry} posisi/ukuran patung di layar: bagian itu dibiarkan terang
     * (patung tetap terlihat jelas), sekelilingnya makin gelap. null = gelap merata.
     */
    static void overlay(GuiGraphicsExtractor g, int w, int h, double t, God god, float[] spot) {
        float fade = (float) Math.min(1.0, t / 0.9);
        int tint = tintRgb(god);
        if (spot == null) {
            g.fill(0, 0, w, h, argb(fade * 0.62f, tint));
        } else {
            float inner = 0.06f * fade, outer = 0.78f * fade;
            g.fill(0, 0, w, h, argb(inner, tint));
            int steps = 14;
            float each = (outer - inner) / steps;
            for (int k = 0; k < steps; k++) {
                float grow = 1f + 0.30f * k;
                int x0 = Math.max(0, (int) (spot[0] - spot[2] * grow)), x1 = Math.min(w, (int) (spot[0] + spot[2] * grow));
                int y0 = Math.max(0, (int) (spot[1] - spot[3] * grow)), y1 = Math.min(h, (int) (spot[1] + spot[3] * grow));
                int c = argb(each, tint);
                g.fill(0, 0, w, y0, c);          // atas
                g.fill(0, y1, w, h, c);          // bawah
                g.fill(0, y0, x0, y1, c);        // kiri
                g.fill(x1, y0, w, y1, c);        // kanan
            }
        }
        // kabut tipis di dasar layar (seperti awan gelap di sekitar patung pada referensi)
        int fogRgb = god == God.RA ? 0x3A2A18 : 0x2A3550;
        float fogA = (float) Math.min(1.0, t / 1.6);
        for (int k = 0; k < 16; k++) {
            int y0 = (int) (h * (0.60 + 0.025 * k));
            g.fill(0, y0, w, (int) (h * (0.60 + 0.025 * (k + 1))) + 1, argb(fogA * 0.012f * (k + 1), fogRgb));
        }
        // vignette: pita gelap di tepi layar
        int bands = 8;
        for (int k = 0; k < bands; k++) {
            float va = fade * 0.07f;
            int bw = (int) (w * 0.012f * (bands - k)), bh = (int) (h * 0.012f * (bands - k));
            int c = argb(va, 0x000000);
            g.fill(0, 0, bw, h, c);
            g.fill(w - bw, 0, w, h, c);
            g.fill(0, 0, w, bh, c);
            g.fill(0, h - bh, w, h, c);
        }
    }

    static void stars(GuiGraphicsExtractor g, int w, int h, double t, God god) {
        float fade = (float) Math.max(0, Math.min(1, (t - 0.2) / 0.8));
        if (fade <= 0) return;
        int rgb = god == God.RA ? 0xFFE7B8 : 0xDDEBFF;
        for (int i = 0; i < STAR_COUNT; i++) {
            float tw = 0.55f + 0.45f * (float) Math.sin(t * 2.2 + SP[i]);
            int x = (int) (SX[i] * w), y = (int) (SY[i] * h);
            g.fill(x, y, x + SSIZE[i], y + SSIZE[i], argb(fade * tw, rgb));
        }
    }

    // ---- sosok raksasa konstelasi (data: assets/kontrakdewa/constellations/<dewa>.json; dibuat tools/make_giant.py) ----
    private record Figure(float[][] stars, int[][] lines, float[][] eyes, int[][] polys) {}
    private static final java.util.Map<God, Figure> FIGURES = new java.util.EnumMap<>(God.class);

    private static Figure figure(God god) {
        return FIGURES.computeIfAbsent(god, g -> {
            Figure empty = new Figure(new float[0][0], new int[0][0], new float[0][0], new int[0][0]);
            String path = "/assets/kontrakdewa/constellations/" + g.id + ".json";
            try (java.io.InputStream in = CosmosFx.class.getResourceAsStream(path)) {
                if (in == null) return empty;
                com.google.gson.JsonObject o = com.google.gson.JsonParser.parseReader(
                        new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                return new Figure(floats(o.getAsJsonArray("stars"), 3), ints(o.getAsJsonArray("lines")), floats(o.getAsJsonArray("eyes"), 5), polys(o.getAsJsonArray("polys")));
            } catch (Exception ex) {
                return empty;
            }
        });
    }

    private static float[][] floats(com.google.gson.JsonArray a, int n) {
        float[][] r = new float[a.size()][n];
        for (int i = 0; i < a.size(); i++) for (int j = 0; j < n; j++) r[i][j] = a.get(i).getAsJsonArray().get(j).getAsFloat();
        return r;
    }

    private static int[][] polys(com.google.gson.JsonArray a) {
        if (a == null) return new int[0][0];
        int[][] r = new int[a.size()][];
        for (int i = 0; i < a.size(); i++) {
            com.google.gson.JsonArray p = a.get(i).getAsJsonArray();
            r[i] = new int[p.size()];
            for (int j = 0; j < p.size(); j++) r[i][j] = p.get(j).getAsInt();
        }
        return r;
    }

    private static int[][] ints(com.google.gson.JsonArray a) {
        int[][] r = new int[a.size()][2];
        for (int i = 0; i < a.size(); i++) for (int j = 0; j < 2; j++) r[i][j] = a.get(i).getAsJsonArray().get(j).getAsInt();
        return r;
    }

    // penyetelan sosok raksasa
    private static final double SCALE = 1.4;       // pembesaran terhadap ukuran patung
    private static final double SHIFT_LEFT = 2.4;  // geser ke kiri layar dari patung (blok)
    private static final double BEHIND = 3.0;      // jarak di belakang patung (blok)
    private static final double LIFT = -0.4;       // geser vertikal figur (blok)

    /**
     * Sosok raksasa konstelasi: figur 3D (kepala kotak bermata putih, palu/tongkat terangkat) berdiri di belakang patung,
     * menghadap kamera akhir, dengan cahaya kebiruan/keemasan. Garis digambar berurutan, titik berdenyut, mata menyala.
     */
    static void giant(GuiGraphicsExtractor g, int w, int h, double t, double start, double dur, God god,
                      Vec3 statueCenter, Vec3 front, Vec3 right) {
        Figure f = figure(god);
        if (f.stars.length == 0) return;
        float prog = (float) ((t - start) / dur);
        if (prog <= 0) return;
        prog = Math.min(1f, prog);

        Vec3 base = statueCenter.subtract(0, 3.0, 0);
        Vec3 anchor = base.subtract(right.scale(SHIFT_LEFT)).subtract(front.scale(BEHIND)).add(0, LIFT, 0);
        Vec3[] world = new Vec3[f.stars.length];
        for (int i = 0; i < world.length; i++) {
            world[i] = anchor.add(right.scale(f.stars[i][0] * SCALE)).add(0, f.stars[i][1] * SCALE, 0).add(front.scale(f.stars[i][2] * SCALE));
        }

        // awan cahaya: poligon tubuh/kepala/senjata diisi tipis (muncul bersamaan dengan garis)
        float cloud = Math.min(1f, prog * 1.6f);
        for (int[] poly : f.polys) {
            float[][] pts = new float[poly.length][];
            boolean ok = true;
            for (int i = 0; i < poly.length; i++) {
                pts[i] = CutsceneCamera.project(world[poly[i]], w, h);
                if (pts[i] == null) { ok = false; break; }
            }
            if (ok) fillPolygon(g, pts, argb(0.075f * cloud, glowRgb(god)));
        }

        int core = argb(0.95f, lineRgb(god));
        float total = f.lines.length * prog;
        boolean[] reached = new boolean[world.length];
        // lapisan cahaya lebar dulu, lalu garis inti
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < f.lines.length; i++) {
                float local = Math.max(0f, Math.min(1f, total - i));
                if (local <= 0) break;
                int ia = f.lines[i][0], ib = f.lines[i][1];
                if (pass == 0) {
                    reached[ia] = true;
                    if (local >= 1f) reached[ib] = true;
                }
                Vec3 b = world[ia].add(world[ib].subtract(world[ia]).scale(local));
                float[] pa = CutsceneCamera.project(world[ia], w, h), pb = CutsceneCamera.project(b, w, h);
                if (pa == null || pb == null) continue;
                if (pass == 0) glowLine(g, (int) pa[0], (int) pa[1], (int) pb[0], (int) pb[1], glowRgb(god));
                else coreLine(g, (int) pa[0], (int) pa[1], (int) pb[0], (int) pb[1], core);
            }
        }

        // mata menyala setelah garis tergambar separuh
        if (prog > 0.5f) {
            float ea = Math.min(1f, (prog - 0.5f) * 3f) * (0.8f + 0.2f * (float) Math.sin(t * 4.0));
            for (float[] e : f.eyes) {
                Vec3[] c = {
                        anchor.add(right.scale(e[0] * SCALE)).add(0, e[1] * SCALE, 0).add(front.scale(e[4] * SCALE)),
                        anchor.add(right.scale(e[2] * SCALE)).add(0, e[3] * SCALE, 0).add(front.scale(e[4] * SCALE))};
                float[] p0 = CutsceneCamera.project(c[0], w, h), p1 = CutsceneCamera.project(c[1], w, h);
                if (p0 == null || p1 == null) continue;
                int x0 = (int) Math.min(p0[0], p1[0]), x1 = (int) Math.max(p0[0], p1[0]);
                int y0 = (int) Math.min(p0[1], p1[1]), y1 = (int) Math.max(p0[1], p1[1]);
                g.fill(x0 - 3, y0 - 3, x1 + 3, y1 + 3, argb(0.18f * ea, glowRgb(god)));
                g.fill(x0, y0, x1 + 1, y1 + 1, argb(ea, 0xFFFFFF));
            }
        }

        for (int i = 0; i < world.length; i++) {
            if (!reached[i]) continue;
            float[] p = CutsceneCamera.project(world[i], w, h);
            if (p == null) continue;
            int nx = (int) p[0], ny = (int) p[1];
            float pulse = 0.75f + 0.25f * (float) Math.sin(t * 3.0 + i);
            g.fill(nx - 3, ny - 3, nx + 4, ny + 4, argb(0.20f * pulse, glowRgb(god)));
            g.fill(nx - 2, ny - 2, nx + 3, ny + 3, argb(0.38f * pulse, lineRgb(god)));
            g.fill(nx - 1, ny - 1, nx + 2, ny + 2, 0xFFFFFFFF);
        }
    }

    private static void glowLine(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int rgb) {
        int dx = x2 - x1, dy = y2 - y1, steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) return;
        int c = argb(0.035f, rgb);
        for (int i = 0; i <= steps; i += 4) {
            int x = x1 + dx * i / steps, y = y1 + dy * i / steps;
            g.fill(x - 7, y - 7, x + 8, y + 8, c);
        }
    }

    private static void coreLine(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int core) {
        int dx = x2 - x1, dy = y2 - y1, steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) return;
        for (int i = 0; i <= steps; i++) {
            int x = x1 + dx * i / steps, y = y1 + dy * i / steps;
            g.fill(x, y, x + 2, y + 2, core);
        }
    }

    /** Isi poligon (garis pindai) dengan satu warna transparan. */
    private static void fillPolygon(GuiGraphicsExtractor g, float[][] p, int color) {
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (float[] v : p) { minY = Math.min(minY, v[1]); maxY = Math.max(maxY, v[1]); }
        for (int y = (int) minY; y <= (int) maxY; y += 2) {
            float xa = Float.MAX_VALUE, xb = -Float.MAX_VALUE;
            for (int i = 0; i < p.length; i++) {
                float[] a = p[i], b = p[(i + 1) % p.length];
                if ((a[1] <= y && b[1] > y) || (b[1] <= y && a[1] > y)) {
                    float x = a[0] + (y - a[1]) / (b[1] - a[1]) * (b[0] - a[0]);
                    xa = Math.min(xa, x); xb = Math.max(xb, x);
                }
            }
            if (xa < xb) g.fill((int) xa, y, (int) xb, y + 2, color);
        }
    }

    private static void line(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int core, int glow) {
        int dx = x2 - x1, dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) return;
        for (int i = 0; i <= steps; i++) {
            int x = x1 + dx * i / steps, y = y1 + dy * i / steps;
            if ((i & 1) == 0) g.fill(x - 1, y - 1, x + 2, y + 2, glow);
            g.fill(x, y, x + 1, y + 1, core);
        }
    }
}
