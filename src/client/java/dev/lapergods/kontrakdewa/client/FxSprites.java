package dev.lapergods.kontrakdewa.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Penggambar sprite efek (awan, bintang, kabut) dan poligon untuk cutscene. Semua tekstur putih + alpha, diwarnai saat digambar. */
final class FxSprites {
    static final Identifier NEBULA = Identifier.fromNamespaceAndPath("kontrakdewa", "textures/gui/nebula_blob.png");
    static final Identifier STAR = Identifier.fromNamespaceAndPath("kontrakdewa", "textures/gui/star_glow.png");
    static final Identifier MIST = Identifier.fromNamespaceAndPath("kontrakdewa", "textures/gui/mist.png");

    static final int NEBULA_SIZE = 128, STAR_SIZE = 128, MIST_W = 256, MIST_H = 96;

    private FxSprites() {}

    static int argb(float alpha, int rgb) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    static float smooth(double x) {
        double c = Math.max(0, Math.min(1, x));
        return (float) (c * c * (3 - 2 * c));
    }

    /** Sprite persegi (NEBULA / STAR) berpusat di (cx, cy) dengan lebar/tinggi w/h piksel GUI. */
    static void square(GuiGraphicsExtractor g, Identifier tex, int texSize, float cx, float cy, float w, float h, int color) {
        if (w < 1 || h < 1 || (color >>> 24) == 0) return;
        g.blit(RenderPipelines.GUI_TEXTURED, tex, Math.round(cx - w / 2), Math.round(cy - h / 2), 0f, 0f,
                Math.round(w), Math.round(h), texSize, texSize, texSize, texSize, color);
    }

    static void mist(GuiGraphicsExtractor g, float cx, float cy, float w, float h, int color) {
        if (w < 2 || h < 2 || (color >>> 24) == 0) return;
        g.blit(RenderPipelines.GUI_TEXTURED, MIST, Math.round(cx - w / 2), Math.round(cy - h / 2), 0f, 0f,
                Math.round(w), Math.round(h), MIST_W, MIST_H, MIST_W, MIST_H, color);
    }

    /** Isi poligon (garis pindai) dengan satu warna transparan. */
    static void fillPolygon(GuiGraphicsExtractor g, float[][] p, int color) {
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (float[] v : p) { minY = Math.min(minY, v[1]); maxY = Math.max(maxY, v[1]); }
        for (int y = (int) minY; y <= (int) maxY; y += 2) {
            float xa = Float.MAX_VALUE, xb = -Float.MAX_VALUE;
            for (int i = 0; i < p.length; i++) {
                float[] a = p[i], b = p[(i + 1) % p.length];
                if ((a[1] <= y && b[1] > y) || (b[1] <= y && a[1] > y)) {
                    float x = a[0] + (y - a[1]) / (b[1] - a[1]) * (b[0] - a[0]);
                    xa = Math.min(xa, x);
                    xb = Math.max(xb, x);
                }
            }
            if (xa < xb) g.fill((int) xa, y, (int) xb, y + 2, color);
        }
    }

    /** Garis tipis berinti terang (tebal thick piksel). */
    static void coreLine(GuiGraphicsExtractor g, float x1, float y1, float x2, float y2, int thick, int color) {
        float dx = x2 - x1, dy = y2 - y1;
        int steps = (int) Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) return;
        for (int i = 0; i <= steps; i++) {
            int x = (int) (x1 + dx * i / steps), y = (int) (y1 + dy * i / steps);
            g.fill(x, y, x + thick, y + thick, color);
        }
    }

    /** Cahaya sepanjang garis memakai sprite awan: lapisan dalam (rapat, sempit) atau luar (jarang, lebar). */
    static void glowAlong(GuiGraphicsExtractor g, float x1, float y1, float x2, float y2, float spacing, float size, int color) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        int n = Math.max(1, (int) (len / spacing));
        for (int i = 0; i <= n; i++) {
            float t = (float) i / n;
            square(g, NEBULA, NEBULA_SIZE, x1 + dx * t, y1 + dy * t, size, size, color);
        }
    }
}
