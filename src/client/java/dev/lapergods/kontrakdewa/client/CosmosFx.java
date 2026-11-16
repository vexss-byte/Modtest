package dev.lapergods.kontrakdewa.client;

import dev.lapergods.kontrakdewa.God;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Random;

/** Efek kosmik untuk cutscene patung: tirai gelap, bintang berkelap-kelip, dan konstelasi dewa yang tergambar perlahan. */
final class CosmosFx {
    private CosmosFx() {}

    // ---- data konstelasi (koordinat 0..1 dalam kotak; titik lalu garis penghubung berurutan) ----
    private static final float[][] RA_NODES = {
            {0.50f, 0.10f}, {0.38f, 0.18f}, {0.62f, 0.18f}, {0.30f, 0.34f}, {0.70f, 0.34f}, {0.50f, 0.42f},
            {0.20f, 0.56f}, {0.80f, 0.56f}, {0.50f, 0.64f}, {0.32f, 0.84f}, {0.68f, 0.84f},
            {0.90f, 0.14f}, {0.90f, 0.92f}, {0.80f, 0.08f},
            // cakram matahari (cincin 8 titik) di kiri atas
            {0.16f, 0.20f}, {0.23f, 0.12f}, {0.32f, 0.10f}, {0.38f, 0.17f}, {0.36f, 0.26f}, {0.28f, 0.32f}, {0.19f, 0.30f}, {0.12f, 0.25f},
    };
    private static final int[][] RA_EDGES = {
            {0, 1}, {0, 2}, {1, 3}, {2, 4}, {3, 5}, {4, 5}, {1, 2}, {3, 6}, {4, 7}, {5, 8}, {6, 8}, {7, 8},
            {6, 9}, {7, 10}, {9, 10}, {8, 9}, {8, 10},
            {11, 12}, {11, 13}, {7, 11},
            {14, 15}, {15, 16}, {16, 17}, {17, 18}, {18, 19}, {19, 20}, {20, 21}, {21, 14}, {17, 3}, {19, 3},
    };

    private static final float[][] THOR_NODES = {
            {0.40f, 0.20f}, {0.52f, 0.20f}, {0.46f, 0.32f},          // kepala
            {0.28f, 0.46f}, {0.62f, 0.50f}, {0.46f, 0.64f},          // bahu kiri, bahu kanan, pusat
            {0.30f, 0.86f}, {0.64f, 0.88f}, {0.26f, 1.00f}, {0.68f, 1.00f},
            {0.72f, 0.36f}, {0.78f, 0.22f},                           // lengan terangkat
            {0.60f, 0.06f}, {0.92f, 0.06f}, {0.92f, 0.22f}, {0.60f, 0.22f}, // kepala palu
            {0.76f, 0.22f},
            {0.18f, 0.60f}, {0.34f, 0.40f},                           // lengan kiri
    };
    private static final int[][] THOR_EDGES = {
            {0, 1}, {1, 2}, {2, 0}, {2, 3}, {2, 4}, {3, 5}, {4, 5}, {3, 4},
            {3, 7}, {4, 6}, {5, 6}, {5, 7}, {6, 8}, {7, 9},
            {4, 10}, {10, 11}, {11, 16},
            {12, 13}, {13, 14}, {14, 15}, {15, 12}, {16, 14}, {16, 15},
            {3, 17}, {18, 3},
    };

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

    /** Tirai gelap + vignette. t = detik sejak cutscene mulai. */
    static void overlay(GuiGraphicsExtractor g, int w, int h, double t, God god) {
        float a = (float) Math.min(1.0, t / 0.9) * 0.62f;
        g.fill(0, 0, w, h, argb(a, tintRgb(god)));
        // vignette: pita gelap di tepi layar
        int bands = 8;
        for (int k = 0; k < bands; k++) {
            float va = (float) Math.min(1.0, t / 0.9) * 0.07f;
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

    /**
     * Konstelasi dewa dalam kotak berukuran size (tinggi), berpusat di (cx, cy).
     * start/dur dalam detik: garis digambar berurutan, lalu titik berdenyut.
     */
    static void constellation(GuiGraphicsExtractor g, int cx, int cy, int size, double t, double start, double dur, God god) {
        float[][] nodes = god == God.RA ? RA_NODES : THOR_NODES;
        int[][] edges = god == God.RA ? RA_EDGES : THOR_EDGES;
        float prog = (float) ((t - start) / dur);
        if (prog <= 0) return;
        prog = Math.min(1f, prog);

        int boxW = (int) (size * 0.8f);
        int x0 = cx - boxW / 2, y0 = cy - size / 2;
        int core = argb(1f, lineRgb(god)), glow = argb(0.16f, glowRgb(god));

        float total = edges.length * prog;
        for (int i = 0; i < edges.length; i++) {
            float local = Math.max(0f, Math.min(1f, total - i));
            if (local <= 0) break;
            float[] a = nodes[edges[i][0]], b = nodes[edges[i][1]];
            int ax = x0 + (int) (a[0] * boxW), ay = y0 + (int) (a[1] * size);
            int bx = x0 + (int) (b[0] * boxW), by = y0 + (int) (b[1] * size);
            line(g, ax, ay, ax + (int) ((bx - ax) * local), ay + (int) ((by - ay) * local), core, glow);
        }

        // titik (muncul saat garis yang menyentuhnya sudah mulai tergambar)
        boolean[] reached = new boolean[nodes.length];
        for (int i = 0; i < edges.length && i < total; i++) {
            reached[edges[i][0]] = true;
            if (total - i >= 1f) reached[edges[i][1]] = true;
        }
        for (int i = 0; i < nodes.length; i++) {
            if (!reached[i]) continue;
            int nx = x0 + (int) (nodes[i][0] * boxW), ny = y0 + (int) (nodes[i][1] * size);
            float pulse = 0.75f + 0.25f * (float) Math.sin(t * 3.0 + i);
            g.fill(nx - 3, ny - 3, nx + 4, ny + 4, argb(0.18f * pulse, glowRgb(god)));
            g.fill(nx - 2, ny - 2, nx + 3, ny + 3, argb(0.35f * pulse, lineRgb(god)));
            g.fill(nx - 1, ny - 1, nx + 2, ny + 2, 0xFFFFFFFF);
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
