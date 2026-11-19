package dev.lapergods.kontrakdewa.client;

import dev.lapergods.kontrakdewa.God;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Random;

/** Tirai gelap badai (dengan sorotan di patung), gradasi dasar, vignette, dan bintang latar. */
final class CosmosFx {
    private CosmosFx() {}

    private static final int STAR_COUNT = 150;
    private static final float[] SX = new float[STAR_COUNT], SY = new float[STAR_COUNT], SP = new float[STAR_COUNT];
    private static final int[] SSIZE = new int[STAR_COUNT];
    static {
        Random r = new Random(1337);
        for (int i = 0; i < STAR_COUNT; i++) {
            SX[i] = r.nextFloat();
            SY[i] = r.nextFloat() * 0.75f;
            SP[i] = r.nextFloat() * 6.28f;
            SSIZE[i] = r.nextInt(12) == 0 ? 2 : 1;
        }
    }

    /** Seberapa gelap suasana (0..1): mulai detik 1,2 dan penuh di detik 5. */
    static float dark(double t) {
        return FxSprites.smooth((t - 1.2) / 3.8);
    }

    private static int tintRgb(God god) {
        return god == God.RA ? 0x1B0F06 : 0x040918;
    }

    /**
     * spot = {cx, cy, rx, ry}: area patung dibiarkan terang, sekelilingnya makin gelap. null = gelap merata.
     */
    static void overlay(GuiGraphicsExtractor g, int w, int h, double t, God god, float[] spot) {
        float dark = dark(t);
        if (dark <= 0.001f) return;
        int tint = tintRgb(god);
        if (spot == null) {
            g.fill(0, 0, w, h, FxSprites.argb(dark * 0.62f, tint));
        } else {
            float inner = 0.05f * dark, outer = 0.80f * dark;
            g.fill(0, 0, w, h, FxSprites.argb(inner, tint));
            int steps = 14;
            float each = (outer - inner) / steps;
            for (int k = 0; k < steps; k++) {
                float grow = 1f + 0.32f * k;
                int x0 = Math.max(0, (int) (spot[0] - spot[2] * grow)), x1 = Math.min(w, (int) (spot[0] + spot[2] * grow));
                int y0 = Math.max(0, (int) (spot[1] - spot[3] * grow)), y1 = Math.min(h, (int) (spot[1] + spot[3] * grow));
                int c = FxSprites.argb(each, tint);
                g.fill(0, 0, w, y0, c);
                g.fill(0, y1, w, h, c);
                g.fill(0, y0, x0, y1, c);
                g.fill(x1, y0, w, y1, c);
            }
        }
        // gradasi gelap di dasar layar (menempelkan komposisi ke tanah)
        for (int k = 0; k < 14; k++) {
            int y0 = (int) (h * (0.66 + 0.024 * k));
            g.fill(0, y0, w, (int) (h * (0.66 + 0.024 * (k + 1))) + 1, FxSprites.argb(dark * 0.014f * (k + 1), 0x050912));
        }
        // vignette
        int bands = 8;
        for (int k = 0; k < bands; k++) {
            int bw = (int) (w * 0.012f * (bands - k)), bh = (int) (h * 0.012f * (bands - k));
            int c = FxSprites.argb(0.07f * Math.max(dark, 0.4f), 0x000000);
            g.fill(0, 0, bw, h, c);
            g.fill(w - bw, 0, w, h, c);
            g.fill(0, 0, w, bh, c);
            g.fill(0, h - bh, w, h, c);
        }
    }

    static void stars(GuiGraphicsExtractor g, int w, int h, double t, God god) {
        float fade = FxSprites.smooth((t - 2.2) / 2.2) * dark(t);
        if (fade <= 0f) return;
        int rgb = god == God.RA ? 0xFFE7B8 : 0xDDEBFF;
        for (int i = 0; i < STAR_COUNT; i++) {
            float tw = 0.5f + 0.5f * (float) Math.sin(t * 2.2 + SP[i]);
            int x = (int) (SX[i] * w), y = (int) (SY[i] * h);
            g.fill(x, y, x + SSIZE[i], y + SSIZE[i], FxSprites.argb(fade * tw, rgb));
        }
    }
}
