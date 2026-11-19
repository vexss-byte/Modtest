package dev.lapergods.kontrakdewa.client;

import dev.lapergods.kontrakdewa.God;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Atmosfer badai kosmik untuk cutscene (semuanya efek layar client-side; dunia pemain tidak diubah):
 * awan badai, pulau batu melayang, kabut berlapis yang melayang di dasar patung, petir bercabang, kilatan, dan bara cahaya.
 */
final class StormFx {
    private StormFx() {}

    // kabut dan awan
    private static int mistRgb(God g) { return g == God.RA ? 0xD8B27A : 0x8FA6CC; }
    private static int cloudRgb(God g) { return g == God.RA ? 0x2A1B10 : 0x121A2C; }
    private static int rimRgb(God g) { return g == God.RA ? 0xFFC15A : 0x7FD0FF; }
    private static int boltRgb(God g) { return g == God.RA ? 0xFFE3A0 : 0xCFE6FF; }
    private static int boltGlowRgb(God g) { return g == God.RA ? 0xFF9A2A : 0x3F86FF; }

    // ------------------------------------------------------------ awan badai di langit

    static void clouds(GuiGraphicsExtractor g, int w, int h, double t, God god, float dark) {
        if (dark <= 0.02f) return;
        int rgb = cloudRgb(god);
        for (int i = 0; i < 11; i++) {
            Random r = new Random(1000 + i);
            float size = w * (0.55f + r.nextFloat() * 0.55f);
            float speed = 0.004f + r.nextFloat() * 0.006f;
            float x = (float) (((r.nextFloat() * 1.4f + t * speed * (i % 2 == 0 ? 1 : -1)) % 1.4f + 1.4f) % 1.4f - 0.2f) * w;
            float y = h * (-0.02f + r.nextFloat() * 0.34f);
            FxSprites.mist(g, x, y, size, size * 0.42f, FxSprites.argb(0.42f * dark, rgb));
        }
        // pantulan cahaya kebiruan di tepi awan
        for (int i = 0; i < 4; i++) {
            Random r = new Random(2000 + i);
            float x = (r.nextFloat() * 1.1f - 0.05f) * w, y = h * (0.04f + r.nextFloat() * 0.2f);
            float pulse = 0.6f + 0.4f * (float) Math.sin(t * (0.5 + r.nextFloat()) + i * 3);
            FxSprites.mist(g, x, y, w * 0.5f, w * 0.14f, FxSprites.argb(0.05f * dark * pulse, rimRgb(god)));
        }
    }

    // ------------------------------------------------------------ pulau batu melayang

    // {kanan, belakang, tinggi, ukuran} dalam blok, relatif terhadap dasar patung (belakang = menjauhi kamera)
    private static final double[][] ISLANDS = {
            {-14, -18, 9, 4.5}, {10, -22, 14, 6.0}, {-6, -32, 6, 6.5}, {22, -14, 8, 3.4},
            {-26, -10, 13, 4.0}, {3, -44, 19, 9.0}, {17, -34, 3, 5.0},
    };
    // siluet dalam koordinat lokal (x -1..1, y atas = positif): tepi atas bergerigi, bawah meruncing
    private static final float[][] ISLAND_SHAPE = {
            {-1.0f, 0.0f}, {-0.65f, 0.14f}, {-0.25f, 0.06f}, {0.25f, 0.16f}, {0.7f, 0.05f}, {1.0f, 0.0f},
            {0.82f, -0.32f}, {0.58f, -0.5f}, {0.42f, -1.0f}, {0.12f, -0.62f}, {-0.12f, -1.32f},
            {-0.4f, -0.58f}, {-0.62f, -0.82f}, {-0.84f, -0.36f},
    };

    static void islands(GuiGraphicsExtractor g, int w, int h, double t, God god, float dark, float flash) {
        if (dark < 0.3f) return;
        float a = FxSprites.smooth((dark - 0.3f) / 0.5f);
        Vec3 base = CutsceneCamera.base(), front = CutsceneCamera.front(), right = CutsceneCamera.right();
        for (int i = 0; i < ISLANDS.length; i++) {
            double[] d = ISLANDS[i];
            double bob = Math.sin(t * 0.45 + i * 1.9) * 0.45;
            Vec3 c = base.add(right.scale(d[0])).add(front.scale(-d[1])).add(0, d[2] + bob, 0);
            float[] s = CutsceneCamera.project(c, w, h);
            if (s == null) continue;
            float ppb = CutsceneCamera.pxPerBlock(c, w, h);
            float scale = (float) d[3] * ppb;
            if (scale < 6) continue;
            float[][] poly = new float[ISLAND_SHAPE.length][2];
            for (int k = 0; k < poly.length; k++) {
                poly[k][0] = s[0] + ISLAND_SHAPE[k][0] * scale;
                poly[k][1] = s[1] - ISLAND_SHAPE[k][1] * scale;
            }
            FxSprites.fillPolygon(g, poly, FxSprites.argb(0.90f * a, 0x070B16));
            // tepi atas diterangi cahaya badai dan kilatan
            float rim = (0.20f + 0.65f * flash) * a;
            for (int k = 0; k < 5; k++) {
                FxSprites.coreLine(g, poly[k][0], poly[k][1], poly[k + 1][0], poly[k + 1][1], 1, FxSprites.argb(rim, rimRgb(god)));
            }
            // kabut di bawah pulau
            FxSprites.mist(g, s[0], s[1] - scale * 0.7f, scale * 2.3f, scale * 0.8f, FxSprites.argb(0.22f * a, mistRgb(god)));
        }
    }

    // ------------------------------------------------------------ kabut berlapis di dasar patung

    static void mistLayers(GuiGraphicsExtractor g, int w, int h, double t, God god, float dark) {
        float fade = FxSprites.smooth((t - 1.4) / 2.2) * (0.35f + 0.65f * dark);
        if (fade <= 0.01f) return;
        Vec3 base = CutsceneCamera.base(), front = CutsceneCamera.front(), right = CutsceneCamera.right();
        int rgb = mistRgb(god);
        for (int layer = 0; layer < 3; layer++) {
            double radius = 3.2 + layer * 2.9;
            double speed = (layer % 2 == 0 ? 1 : -1) * (0.08 + 0.03 * layer);
            for (int k = 0; k < 6; k++) {
                double ang = Math.PI * 2 * k / 6 + layer * 0.9 + t * speed;
                double r = radius + Math.sin(t * 0.4 + k * 2.1 + layer) * 0.8;
                double y = 0.45 + layer * 0.35 + Math.sin(t * 0.5 + k + layer) * 0.15;
                Vec3 p = base.add(right.scale(Math.cos(ang) * r)).add(front.scale(Math.sin(ang) * r)).add(0, y, 0);
                float[] s = CutsceneCamera.project(p, w, h);
                if (s == null) continue;
                float ppb = CutsceneCamera.pxPerBlock(p, w, h);
                float width = (7.0f + layer * 2.2f) * ppb;
                float alpha = (0.17f - layer * 0.025f) * fade * (0.8f + 0.2f * (float) Math.sin(t * 0.7 + k * 1.3));
                FxSprites.mist(g, s[0], s[1], width, width * 0.38f, FxSprites.argb(alpha, rgb));
            }
        }
    }

    // ------------------------------------------------------------ cahaya biru di kaki patung

    static void statueGlow(GuiGraphicsExtractor g, int w, int h, double t, God god, float dark) {
        if (dark <= 0.05f) return;
        Vec3 p = CutsceneCamera.base().add(0, 1.0, 0);
        float[] s = CutsceneCamera.project(p, w, h);
        if (s == null) return;
        float ppb = CutsceneCamera.pxPerBlock(p, w, h);
        float pulse = 0.8f + 0.2f * (float) Math.sin(t * 1.6);
        FxSprites.square(g, FxSprites.NEBULA, FxSprites.NEBULA_SIZE, s[0], s[1], ppb * 9f, ppb * 5f, FxSprites.argb(0.20f * dark * pulse, boltGlowRgb(god)));
    }

    // ------------------------------------------------------------ petir

    private static final double[] BOLT_TIMES = {4.20, 4.90, 5.35, 5.90, 6.30, 6.95, 7.50, 8.20, 9.40, 10.6, 12.1, 14.0};

    private static float intensity(double age) {
        if (age < 0 || age > 0.36) return 0f;
        if (age < 0.05) return 1f;
        if (age < 0.09) return 0.25f;
        if (age < 0.14) return 0.9f;
        return 0.6f * (1f - (float) ((age - 0.14) / 0.22));
    }

    /** Berapa sambaran yang dimulai di antara dua waktu (untuk memutar suara guntur). */
    static int boltsBetween(double from, double to) {
        int n = 0;
        for (double bt : BOLT_TIMES) if (bt > from && bt <= to) n++;
        return n;
    }

    /** Kekuatan kilatan layar (0..1) pada detik t. */
    static float flash(double t) {
        float best = 0f;
        for (double bt : BOLT_TIMES) best = Math.max(best, intensity(t - bt));
        return best;
    }

    private static List<Vec3> path(Random r, Vec3 a, Vec3 b, int n, double jitter) {
        List<Vec3> pts = new ArrayList<>();
        Vec3 dir = b.subtract(a);
        Vec3 side = dir.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1e-6) side = new Vec3(1, 0, 0);
        side = side.normalize();
        Vec3 side2 = dir.cross(side).normalize();
        for (int i = 0; i <= n; i++) {
            double s = (double) i / n;
            double amp = jitter * Math.pow(Math.sin(Math.PI * s), 0.7);
            Vec3 p = a.add(dir.scale(s)).add(side.scale((r.nextDouble() * 2 - 1) * amp)).add(side2.scale((r.nextDouble() * 2 - 1) * amp * 0.5));
            pts.add(i == 0 ? a : i == n ? b : p);
        }
        return pts;
    }

    static void lightning(GuiGraphicsExtractor g, int w, int h, double t, God god) {
        if (god != God.THOR) return;
        Vec3 base = CutsceneCamera.base(), front = CutsceneCamera.front(), right = CutsceneCamera.right();
        for (int bi = 0; bi < BOLT_TIMES.length; bi++) {
            float inten = intensity(t - BOLT_TIMES[bi]);
            if (inten <= 0f) continue;
            Random r = new Random(bi * 7919L + 13);
            int pattern = bi % 3;
            double rx = pattern == 0 ? r.nextDouble() * 5 - 2.5 : pattern == 1 ? -9 + r.nextDouble() * 3 : 6 + r.nextDouble() * 3;
            double fz = r.nextDouble() * 4 - 4;
            Vec3 start = base.add(right.scale(rx + (r.nextDouble() * 6 - 3))).add(front.scale(fz)).add(0, 26, 0);
            Vec3 end = base.add(right.scale(rx * 0.35)).add(front.scale(fz * 0.3)).add(0, 0.3, 0);
            List<Vec3> main = path(r, start, end, 16, 2.4);
            drawBolt(g, w, h, main, inten, 2, god);
            // cabang
            for (int br = 0; br < 3; br++) {
                int from = 4 + r.nextInt(8);
                Vec3 p0 = main.get(from);
                Vec3 dest = p0.add(right.scale((r.nextDouble() * 2 - 1) * 6)).add(front.scale((r.nextDouble() * 2 - 1) * 3)).add(0, -(3 + r.nextDouble() * 6), 0);
                drawBolt(g, w, h, path(r, p0, dest, 6, 1.1), inten * 0.75f, 1, god);
            }
        }
    }

    private static void drawBolt(GuiGraphicsExtractor g, int w, int h, List<Vec3> pts, float inten, int thick, God god) {
        float[] prev = null;
        for (Vec3 p : pts) {
            float[] s = CutsceneCamera.project(p, w, h);
            if (s != null && prev != null) {
                FxSprites.glowAlong(g, prev[0], prev[1], s[0], s[1], 10f, 26f * thick, FxSprites.argb(0.085f * inten, boltGlowRgb(god)));
                FxSprites.coreLine(g, prev[0], prev[1], s[0], s[1], thick, FxSprites.argb(Math.min(1f, inten + 0.1f), boltRgb(god)));
            }
            prev = s;
        }
    }

    // ------------------------------------------------------------ bara cahaya naik (ruang layar)

    static void embers(GuiGraphicsExtractor g, int w, int h, double t, God god, float dark) {
        float fade = FxSprites.smooth((t - 1.6) / 2.0) * (0.4f + 0.6f * dark);
        if (fade <= 0.02f) return;
        int rgb = god == God.RA ? 0xFFC060 : 0x8CC8FF;
        for (int i = 0; i < 34; i++) {
            Random r = new Random(5000 + i);
            double speed = 0.03 + r.nextDouble() * 0.05;
            double ph = r.nextDouble();
            double up = (ph + t * speed) % 1.0;
            float x = (float) ((r.nextDouble() + Math.sin(t * 0.6 + i) * 0.015) * w);
            float y = (float) (h * (1.02 - up * 0.95));
            float size = 5f + r.nextFloat() * 9f;
            float tw = 0.45f + 0.55f * (float) Math.max(0, Math.sin(t * (1.5 + r.nextDouble() * 2) + i));
            float edge = (float) Math.min(1.0, Math.min(up * 6, (1 - up) * 4));
            FxSprites.square(g, FxSprites.STAR, FxSprites.STAR_SIZE, x, y, size, size, FxSprites.argb(0.55f * fade * tw * edge, rgb));
        }
    }
}
