package dev.lapergods.kontrakdewa.client;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.lapergods.kontrakdewa.God;
import dev.lapergods.kontrakdewa.ModConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Shader GLSL layar penuh untuk cutscene: langit badai kosmik (awan berputar, nebula, bintang, kabut, kilat internal)
 * dengan "lubang" berbentuk siluet patung supaya patung tetap terlihat. File: assets/kontrakdewa/shaders/core/storm.vsh/.fsh.
 *
 * Dikirim lewat satu quad GUI. Parameter dibawa warna vertex (tanpa uniform kustom):
 *   R = x tengah patung (0..1), G = y tengah patung (0..1), B = setengah tinggi patung / 0,8,
 *   A = (kegelapan 5 bit << 3) | (kilatan 3 bit). Waktu dibaca shader dari Globals.GameTime.
 * Pipeline didaftarkan sebagai OPSIONAL: kalau shader gagal dikompilasi, game tidak crash; efek bawaan (CosmosFx) dipakai.
 */
final class ShaderFx {
    private static final Identifier SHADER = Identifier.fromNamespaceAndPath("kontrakdewa", "core/storm");

    static final RenderPipeline STORM_THOR = RenderPipelines.registerOptional(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("kontrakdewa", "pipeline/storm_thor"))
            .withVertexShader(SHADER)
            .withFragmentShader(SHADER)
            .build());

    static final RenderPipeline STORM_RA = RenderPipelines.registerOptional(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("kontrakdewa", "pipeline/storm_ra"))
            .withVertexShader(SHADER)
            .withFragmentShader(SHADER)
            .withShaderDefine("RA")
            .build());

    private ShaderFx() {}

    /** Memaksa class ter-load saat mod client start (supaya pipeline terdaftar sebelum shader dikompilasi). */
    static void init() {
    }

    static boolean enabled() {
        return ModConfig.get().cutsceneShader;
    }

    private static int channel(float v, float max) {
        return Math.max(0, Math.min(255, Math.round(v / max * 255f)));
    }

    /**
     * @param spotX,spotY   pusat patung di layar (0..1)
     * @param halfHeight    setengah tinggi patung dibanding tinggi layar
     * @param dark          0..1 seberapa gelap suasana
     * @param flash         0..1 kilatan petir
     */
    static void draw(GuiGraphicsExtractor g, int w, int h, God god, float spotX, float spotY, float halfHeight, float dark, float flash) {
        int d5 = Math.max(0, Math.min(31, Math.round(dark * 31f)));
        int f3 = Math.max(0, Math.min(7, Math.round(flash * 7f)));
        int a = (d5 << 3) | f3;
        if (a == 0) return; // belum perlu digambar
        int color = (a << 24) | (channel(spotX, 1f) << 16) | (channel(spotY, 1f) << 8) | channel(halfHeight, 0.8f);
        RenderPipeline pipeline = god == God.RA ? STORM_RA : STORM_THOR;
        g.blit(pipeline, FxSprites.STAR, 0, 0, 0f, 0f, w, h,
                FxSprites.STAR_SIZE, FxSprites.STAR_SIZE, FxSprites.STAR_SIZE, FxSprites.STAR_SIZE, color);
    }
}
