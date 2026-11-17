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

    // ---- konstelasi 3D (data dari assets/kontrakdewa/constellations/<dewa>.json, satuan blok) ----
    private record Figure(float[][] nodes, int[][] edges) {}
    private static final java.util.Map<God, Figure> FIGURES = new java.util.EnumMap<>(God.class);

    private static Figure figure(God god) {
        return FIGURES.computeIfAbsent(god, g -> {
            String path = "/assets/kontrakdewa/constellations/" + g.id + ".json";
            try (java.io.InputStream in = CosmosFx.class.getResourceAsStream(path)) {
                if (in == null) return new Figure(new float[0][0], new int[0][0]);
                com.google.gson.JsonObject o = com.google.gson.JsonParser.parseReader(
                        new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                com.google.gson.JsonArray n = o.getAsJsonArray("nodes"), e = o.getAsJsonArray("edges");
                float[][] nodes = new float[n.size()][2];
                for (int i = 0; i < n.size(); i++) {
                    nodes[i][0] = n.get(i).getAsJsonArray().get(0).getAsFloat();
                    nodes[i][1] = n.get(i).getAsJsonArray().get(1).getAsFloat();
                }
                int[][] edges = new int[e.size()][2];
                for (int i = 0; i < e.size(); i++) {
                    edges[i][0] = e.get(i).getAsJsonArray().get(0).getAsInt();
                    edges[i][1] = e.get(i).getAsJsonArray().get(1).getAsInt();
                }
                return new Figure(nodes, edges);
            } catch (Exception ex) {
                return new Figure(new float[0][0], new int[0][0]);
            }
        });
    }

    /**
     * Konstelasi dewa sebagai bidang 3D di belakang patung, menghadap posisi akhir kamera, lalu diproyeksikan ke layar.
     * Karena kamera mengitari patung, bidang ini tampak berputar dan menjadi tegak lurus tepat saat kamera berhenti.
     * start/dur dalam detik: garis digambar berurutan, lalu titik berdenyut.
     */
    static void constellation3d(GuiGraphicsExtractor g, int w, int h, double t, double start, double dur, God god,
                                Vec3 statueCenter, Vec3 finalCam) {
        Figure f = figure(god);
        if (f.nodes.length == 0) return;
        float prog = (float) ((t - start) / dur);
        if (prog <= 0) return;
        prog = Math.min(1f, prog);

        // bidang menghadap kamera akhir, digeser ke belakang patung
        Vec3 facing = new Vec3(finalCam.x - statueCenter.x, 0, finalCam.z - statueCenter.z);
        facing = facing.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : facing.normalize();
        Vec3 right = new Vec3(facing.z, 0, -facing.x);
        Vec3 anchor = statueCenter.subtract(0, 3.0, 0).subtract(facing.scale(BEHIND));

        Vec3[] world = new Vec3[f.nodes.length];
        for (int i = 0; i < world.length; i++) {
            world[i] = anchor.add(right.scale(f.nodes[i][0] * SCALE)).add(0, f.nodes[i][1] * SCALE, 0);
        }

        int core = argb(1f, lineRgb(god)), glow = argb(0.16f, glowRgb(god));
        float total = f.edges.length * prog;
        boolean[] reached = new boolean[world.length];
        for (int i = 0; i < f.edges.length; i++) {
            float local = Math.max(0f, Math.min(1f, total - i));
            if (local <= 0) break;
            int ia = f.edges[i][0], ib = f.edges[i][1];
            reached[ia] = true;
            if (local >= 1f) reached[ib] = true;
            Vec3 b = world[ia].add(world[ib].subtract(world[ia]).scale(local));
            float[] pa = CutsceneCamera.project(world[ia], w, h), pb = CutsceneCamera.project(b, w, h);
            if (pa == null || pb == null) continue;
            line(g, (int) pa[0], (int) pa[1], (int) pb[0], (int) pb[1], core, glow);
        }
        for (int i = 0; i < world.length; i++) {
            if (!reached[i]) continue;
            float[] p = CutsceneCamera.project(world[i], w, h);
            if (p == null) continue;
            int nx = (int) p[0], ny = (int) p[1];
            float pulse = 0.75f + 0.25f * (float) Math.sin(t * 3.0 + i);
            g.fill(nx - 3, ny - 3, nx + 4, ny + 4, argb(0.18f * pulse, glowRgb(god)));
            g.fill(nx - 2, ny - 2, nx + 3, ny + 3, argb(0.35f * pulse, lineRgb(god)));
            g.fill(nx - 1, ny - 1, nx + 2, ny + 2, 0xFFFFFFFF);
        }
    }

    // penyetelan posisi konstelasi
    private static final double SCALE = 1.15;   // pembesaran figur
    private static final double BEHIND = 4.0;   // jarak bidang di belakang patung (blok)

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
