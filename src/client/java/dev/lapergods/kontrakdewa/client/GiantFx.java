package dev.lapergods.kontrakdewa.client;

import dev.lapergods.kontrakdewa.God;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Map;

/**
 * Sosok raksasa konstelasi dewa. Data: assets/kontrakdewa/constellations/<dewa>.json (dibuat tools/make_giant.py).
 * Lapisan: aura awan nebula, cahaya luar, cahaya dalam, garis inti, titik bintang berdenyut, mata menyala, debu bintang.
 * Pembentukan: bintang muncul -> garis tergambar berurutan -> aura menebal dari kaki ke kepala -> mata menyala -> aura menguat.
 */
final class GiantFx {
    private record Figure(float[][] stars, int[][] lines, float[][] eyes, float[][] blobs, float[][] dust, int[] degree) {}

    private static final Map<God, Figure> FIGURES = new EnumMap<>(God.class);

    private GiantFx() {}

    // ------------------------------------------------------------ data

    private static Figure figure(God god) {
        return FIGURES.computeIfAbsent(god, g -> {
            Figure empty = new Figure(new float[0][0], new int[0][0], new float[0][0], new float[0][0], new float[0][0], new int[0]);
            String path = "/assets/kontrakdewa/constellations/" + g.id + ".json";
            try (java.io.InputStream in = GiantFx.class.getResourceAsStream(path)) {
                if (in == null) return empty;
                com.google.gson.JsonObject o = com.google.gson.JsonParser.parseReader(
                        new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                float[][] stars = floats(o.getAsJsonArray("stars"), 3);
                int[][] lines = ints(o.getAsJsonArray("lines"));
                int[] deg = new int[stars.length];
                for (int[] l : lines) { deg[l[0]]++; deg[l[1]]++; }
                return new Figure(stars, lines, floats(o.getAsJsonArray("eyes"), 5),
                        o.has("blobs") ? floats(o.getAsJsonArray("blobs"), 4) : new float[0][0],
                        o.has("dust") ? floats(o.getAsJsonArray("dust"), 4) : new float[0][0], deg);
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

    private static int[][] ints(com.google.gson.JsonArray a) {
        int[][] r = new int[a.size()][2];
        for (int i = 0; i < a.size(); i++) for (int j = 0; j < 2; j++) r[i][j] = a.get(i).getAsJsonArray().get(j).getAsInt();
        return r;
    }

    // ------------------------------------------------------------ warna

    private static int core(God g) { return g == God.RA ? 0xFFF3C8 : 0xE6F3FF; }
    private static int inner(God g) { return g == God.RA ? 0xFFC456 : 0x62B8FF; }
    private static int outer(God g) { return g == God.RA ? 0xC8701C : 0x2F62D6; }

    // ------------------------------------------------------------ gambar

    /** t = detik cutscene, mStart/mEnd = awal dan akhir proses manifestasi (detik). */
    static void draw(GuiGraphicsExtractor g, int w, int h, double t, God god, double mStart, double mEnd) {
        Figure f = figure(god);
        if (f.stars.length == 0 || t < mStart) return;

        double u = t - mStart;
        float starsT = FxSprites.smooth(u / 0.8);
        float linesRaw = (float) Math.max(0, Math.min(1, (u - 0.2) / (mEnd - mStart - 0.5)));
        float auraT = (float) Math.max(0, Math.min(1, (u - 0.5) / 1.7));
        float eyeT = FxSprites.smooth((u - 1.1) / 0.45);
        float settle = FxSprites.smooth((t - (mEnd - 0.4)) / 1.4);   // aura menguat setelah selesai terbentuk
        float boost = 0.65f + 0.55f * settle;

        Vec3 anchor = CutsceneCamera.giantAnchor();
        double S = CutsceneCamera.giantScale();
        Vec3 U = CutsceneCamera.right(), W = CutsceneCamera.front();
        float ppb = CutsceneCamera.pxPerBlock(anchor.add(0, 4 * S, 0), w, h);
        if (ppb <= 0) return;
        float unit = (float) (ppb * S); // piksel per satuan figur

        // posisi dunia + layar tiap bintang
        float[][] sp = new float[f.stars.length][];
        for (int i = 0; i < sp.length; i++) {
            Vec3 p = anchor.add(U.scale(f.stars[i][0] * S)).add(0, f.stars[i][1] * S, 0).add(W.scale(f.stars[i][2] * S));
            sp[i] = CutsceneCamera.project(p, w, h);
        }

        // ---- 1. aura awan nebula (dari kaki ke kepala) ----
        int outerC = outer(god), innerC = inner(god);
        for (int i = 0; i < f.blobs.length; i++) {
            float[] b = f.blobs[i];
            Vec3 p = anchor.add(U.scale(b[0] * S)).add(0, b[1] * S, 0).add(W.scale(b[2] * S));
            float[] s = CutsceneCamera.project(p, w, h);
            if (s == null) continue;
            float delay = Math.max(0f, 1f - b[1] / 9.0f) * 0.45f;
            float a = FxSprites.smooth(auraT * 1.5 - delay) * boost;
            if (a <= 0.001f) continue;
            float pulse = 0.85f + 0.15f * (float) Math.sin(t * 1.3 + i * 1.7);
            float size = b[3] * unit * 3.4f;
            FxSprites.square(g, FxSprites.NEBULA, FxSprites.NEBULA_SIZE, s[0], s[1], size, size, FxSprites.argb(0.13f * a * pulse, outerC));
            FxSprites.square(g, FxSprites.NEBULA, FxSprites.NEBULA_SIZE, s[0], s[1], size * 0.5f, size * 0.5f, FxSprites.argb(0.10f * a * pulse, innerC));
        }

        // ---- 2. garis (cahaya luar, cahaya dalam, inti) ----
        float total = f.lines.length * linesRaw;
        int coreC = core(god);
        for (int i = 0; i < f.lines.length; i++) {
            float local = Math.max(0f, Math.min(1f, total - i));
            if (local <= 0f) break;
            float[] a = sp[f.lines[i][0]], b = sp[f.lines[i][1]];
            if (a == null || b == null) continue;
            float ex = a[0] + (b[0] - a[0]) * local, ey = a[1] + (b[1] - a[1]) * local;
            float shim = 0.85f + 0.15f * (float) Math.sin(t * 2.4 + i);
            FxSprites.glowAlong(g, a[0], a[1], ex, ey, Math.max(14f, unit * 0.55f), unit * 1.5f, FxSprites.argb(0.040f * boost * shim, outerC));
            FxSprites.glowAlong(g, a[0], a[1], ex, ey, Math.max(8f, unit * 0.25f), unit * 0.55f, FxSprites.argb(0.075f * boost * shim, innerC));
            FxSprites.coreLine(g, a[0], a[1], ex, ey, 2, FxSprites.argb(0.92f * (0.8f + 0.2f * shim), coreC));
        }

        // ---- 3. mata menyala ----
        if (eyeT > 0f) {
            float ea = eyeT * (0.85f + 0.15f * (float) Math.sin(t * 4.0));
            for (float[] e : f.eyes) {
                Vec3 p0 = anchor.add(U.scale(e[0] * S)).add(0, e[1] * S, 0).add(W.scale(e[4] * S));
                Vec3 p1 = anchor.add(U.scale(e[2] * S)).add(0, e[3] * S, 0).add(W.scale(e[4] * S));
                float[] s0 = CutsceneCamera.project(p0, w, h), s1 = CutsceneCamera.project(p1, w, h);
                if (s0 == null || s1 == null) continue;
                float cx = (s0[0] + s1[0]) / 2, cy = (s0[1] + s1[1]) / 2;
                int x0 = (int) Math.min(s0[0], s1[0]), x1 = (int) Math.max(s0[0], s1[0]);
                int y0 = (int) Math.min(s0[1], s1[1]), y1 = (int) Math.max(s0[1], s1[1]);
                FxSprites.square(g, FxSprites.STAR, FxSprites.STAR_SIZE, cx, cy, (x1 - x0) * 3.2f + 24, (x1 - x0) * 3.2f + 24, FxSprites.argb(0.55f * ea, innerC));
                g.fill(x0, y0, x1 + 1, Math.max(y1 + 1, y0 + 2), FxSprites.argb(ea, 0xFFFFFF));
            }
        }

        // ---- 4. titik bintang (muncul lebih dulu, berdenyut) ----
        for (int i = 0; i < sp.length; i++) {
            if (sp[i] == null) continue;
            float appear = FxSprites.smooth(starsT * 1.6 - (i / (float) sp.length) * 0.6);
            if (appear <= 0f) continue;
            float pulse = 0.8f + 0.2f * (float) Math.sin(t * 3.1 + i * 0.9);
            float bright = 1f + 0.16f * Math.min(f.degree[i], 5);
            float size = unit * 0.55f * bright * appear * pulse;
            FxSprites.square(g, FxSprites.STAR, FxSprites.STAR_SIZE, sp[i][0], sp[i][1], size, size, FxSprites.argb(0.95f * appear, coreC));
        }

        // ---- 5. debu bintang di sekitar sosok ----
        for (int i = 0; i < f.dust.length; i++) {
            float[] d = f.dust[i];
            Vec3 p = anchor.add(U.scale(d[0] * S)).add(0, d[1] * S, 0).add(W.scale(d[2] * S));
            float[] s = CutsceneCamera.project(p, w, h);
            if (s == null) continue;
            float tw = 0.4f + 0.6f * (float) Math.max(0, Math.sin(t * (1.2 + d[3]) + i * 2.3));
            float a = starsT * tw * 0.7f;
            float size = unit * 0.22f * d[3] * 2f;
            FxSprites.square(g, FxSprites.STAR, FxSprites.STAR_SIZE, s[0], s[1], size, size, FxSprites.argb(a, innerC));
        }
    }
}
