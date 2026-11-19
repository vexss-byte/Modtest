package dev.lapergods.kontrakdewa.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.lapergods.kontrakdewa.Faith;
import dev.lapergods.kontrakdewa.God;
import dev.lapergods.kontrakdewa.GodStatueBlock;
import dev.lapergods.kontrakdewa.ModBlocks;
import dev.lapergods.kontrakdewa.net.OpenContractPayload;
import dev.lapergods.kontrakdewa.net.SignContractPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Layar kontrak dengan cutscene sinematik 8 detik. Satu jam (CutsceneCamera.elapsed) mengatur semuanya:
 *   0.0-1.5  pembuka: patung utuh, dolly pelan            | 1.5-3.5  orbit halus ke depan-kanan
 *   3.5-5.0  transisi atmosfer: gelap, badai, pulau melayang, kabut; shot lebar
 *   5.0-6.8  manifestasi: bintang -> garis -> aura -> mata sosok raksasa konstelasi
 *   6.8-8.0  panel kontrak meluncur dari kanan; tombol muncul saat panel sudah cukup terlihat
 * Spasi / Enter melompati cutscene. Esc menutup.
 */
public class ContractScreen extends Screen {
    /** true selama layar kontrak terbuka (dipakai CameraMixin; tidak bergantung API Minecraft.screen). */
    private static volatile boolean open = false;

    public static boolean isOpen() {
        return open;
    }

    // ---- timeline (detik) ----
    private static final double MANIFEST_START = 5.0, MANIFEST_END = 6.8;
    private static final double PANEL_START = 6.8, PANEL_DUR = 1.0;
    private static final double BUTTONS_AT = 0.6;   // tombol tampil setelah panel masuk sekitar 60%

    private final God god;
    private final OpenContractPayload info;
    private final boolean signedHere;
    private final boolean hasOther;

    private final long fallbackStart = System.nanoTime();
    private Vec3 statueCenter = null;
    private Direction statueFront = Direction.NORTH;
    private boolean cinematic = false;
    private double lastT = 0;

    private int px, py, pw, ph;
    private int baseX;
    private Button signBtn;

    public ContractScreen(God god, OpenContractPayload info) {
        super(Component.literal(god.statueName));
        this.god = god;
        this.info = info;
        this.signedHere = god.id.equals(info.currentGodId());
        this.hasOther = !info.currentGodId().isEmpty() && !signedHere;
    }

    // ------------------------------------------------------------ tema warna
    private int accent() { return god == God.RA ? 0xFFE0A030 : 0xFF63D2FF; }
    private int panelBg() { return god == God.RA ? 0xEE1A1008 : 0xEE0A1226; }
    private int panelBg2() { return god == God.RA ? 0xEE24160A : 0xEE101C38; }
    private int textMain() { return god == God.RA ? 0xFFFFF1D6 : 0xFFE3F0FF; }
    private int textMuted() { return god == God.RA ? 0xFFC9B48E : 0xFF9FB4D0; }
    private int green() { return 0xFF7FE0B0; }
    private int red() { return 0xFFFF9A86; }

    private double t() {
        return cinematic ? CutsceneCamera.elapsed() : (System.nanoTime() - fallbackStart) / 1_000_000_000.0;
    }

    private static double ease(double x) {
        double c = Math.max(0, Math.min(1, x));
        return 1 - Math.pow(1 - c, 3);
    }

    // ------------------------------------------------------------ init

    @Override
    protected void init() {
        open = true;
        pw = Math.max(210, Math.min(300, (int) (width * 0.42)));
        ph = height - 24;
        baseX = width - pw - 12;
        px = baseX + pw + 20;
        py = 12;

        Component label = signedHere ? Component.literal("Kontrak aktif")
                : hasOther ? Component.literal("Ganti Kontrak (HUKUMAN)").withStyle(net.minecraft.ChatFormatting.RED)
                : Component.literal("Sign Contract");
        signBtn = Button.builder(label, b -> {
            ClientPlayNetworking.send(new SignContractPayload(god.id));
            onClose();
        }).bounds(px + 12, py + ph - 70, pw - 24, 22).build();
        signBtn.active = !signedHere && info.lockMinutes() <= 0;
        signBtn.visible = false;
        addRenderableWidget(signBtn);

        if (!cinematic && minecraft != null && minecraft.player != null && minecraft.level != null) {
            LocalPlayer player = minecraft.player;
            statueCenter = findStatue(player);
            if (statueCenter != null) {
                try {
                    CutsceneCamera.begin(statueCenter, statueFront, player.getEyePosition(), player.getYRot(), player.getXRot(),
                            minecraft.options.fov().get(), (double) width / height, (double) baseX / width);
                    cinematic = true;
                    minecraft.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BEACON_ACTIVATE, SoundSource.AMBIENT, 0.6f, 0.6f, false);
                } catch (Throwable e) {
                    CutsceneCamera.stop();
                    cinematic = false;
                }
            }
        }
    }

    /** Cari titik tengah (setinggi dada) dari blok patung dewa ini di dekat pemain, dan arah hadapnya. */
    private Vec3 findStatue(LocalPlayer player) {
        Block target = god == God.RA ? ModBlocks.RA_STATUE : ModBlocks.THOR_STATUE;
        BlockPos c = player.blockPosition();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -8; dx <= 8; dx++)
            for (int dy = -8; dy <= 8; dy++)
                for (int dz = -8; dz <= 8; dz++) {
                    m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    if (minecraft.level.getBlockState(m).is(target)) {
                        double d = dx * dx + dy * dy + dz * dz;
                        if (d < bestD) { bestD = d; best = m.immutable(); }
                    }
                }
        if (best == null) return null;
        var state = minecraft.level.getBlockState(best);
        if (state.hasProperty(GodStatueBlock.FACING)) statueFront = state.getValue(GodStatueBlock.FACING);
        return new Vec3(best.getX() + 0.5, best.getY() + 3.0, best.getZ() + 0.5);
    }

    // ------------------------------------------------------------ tick: partikel dunia, suara

    @Override
    public void tick() {
        super.tick();
        if (!cinematic || minecraft == null || minecraft.player == null || minecraft.level == null || statueCenter == null) return;
        double t = t();
        LocalPlayer p = minecraft.player;
        var rnd = minecraft.level.getRandom();
        Vec3 base = statueCenter.subtract(0, 3.0, 0);

        // partikel di sekitar patung: energi naik, percikan listrik, cahaya kecil (jumlah dibatasi)
        if (t > 0.8 && t < 14) {
            double k = Math.min(1.0, (t - 0.8) / 3.0);
            int count = 1 + (int) (3 * k);
            ParticleOptions main = god == God.RA ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME;
            ParticleOptions spark = god == God.RA ? ParticleTypes.END_ROD : ParticleTypes.ELECTRIC_SPARK;
            for (int i = 0; i < count; i++) {
                double a = rnd.nextDouble() * Math.PI * 2;
                double r = 1.2 + rnd.nextDouble() * 3.4;
                double x = base.x + Math.cos(a) * r, z = base.z + Math.sin(a) * r, y = base.y + 0.1 + rnd.nextDouble() * 0.4;
                minecraft.level.addParticle(main, x, y, z, (rnd.nextDouble() - 0.5) * 0.01, 0.03 + rnd.nextDouble() * 0.07, (rnd.nextDouble() - 0.5) * 0.01);
                if (i % 2 == 0) minecraft.level.addParticle(spark, x, y + rnd.nextDouble() * 2.5, z, 0, 0.01 + rnd.nextDouble() * 0.02, 0);
            }
            if (t > 3.5 && minecraft.level.getGameTime() % 3 == 0) {
                minecraft.level.addParticle(ParticleTypes.END_ROD, base.x + (rnd.nextDouble() - 0.5) * 3, base.y + 2 + rnd.nextDouble() * 5,
                        base.z + (rnd.nextDouble() - 0.5) * 3, 0, 0.02, 0);
            }
        }

        // guntur sinkron dengan sambaran petir (hanya Thor)
        if (god == God.THOR) {
            int bolts = StormFx.boltsBetween(lastT, t);
            for (int i = 0; i < bolts; i++) {
                minecraft.level.playLocalSound(p.getX(), p.getY(), p.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.AMBIENT,
                        0.55f, 0.9f + rnd.nextFloat() * 0.5f, false);
            }
        }
        if (lastT < MANIFEST_START + 1.2 && t >= MANIFEST_START + 1.2) {
            minecraft.level.playLocalSound(p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.AMBIENT,
                    0.7f, god == God.RA ? 1.2f : 1.5f, false);
        }
        lastT = t;
    }

    @Override
    public void removed() {
        open = false;
        super.removed();
        CutsceneCamera.stop();
        cinematic = false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // Spasi / Enter: lompat ke akhir cutscene (panel langsung tampil)
        if (cinematic && t() < PANEL_START + PANEL_DUR
                && (event.key() == InputConstants.KEY_SPACE || event.key() == InputConstants.KEY_RETURN)) {
            CutsceneCamera.skipTo(PANEL_START + PANEL_DUR + 0.1);
            return true;
        }
        return super.keyPressed(event);
    }

    // ------------------------------------------------------------ gambar

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        // super tidak dipanggil: dunia tetap terlihat; efek kosmik digambar sendiri di atasnya
        double t = t();
        float dark = CosmosFx.dark(t);

        float[] spot = null;
        float[] spotNorm = null; // {x, y, setengah tinggi} dalam pecahan layar, untuk shader
        if (cinematic && statueCenter != null) {
            float[] sc = CutsceneCamera.project(statueCenter.add(0, 0.2, 0), width, height);
            float ppb = CutsceneCamera.pxPerBlock(statueCenter, width, height);
            if (sc != null && ppb > 0) {
                spot = new float[]{sc[0], sc[1], Math.max(height * 0.10f, ppb * 2.8f), Math.max(height * 0.16f, ppb * 4.6f)};
                spotNorm = new float[]{sc[0] / width, sc[1] / height, Math.max(0.05f, Math.min(0.79f, 3.2f * ppb / height))};
            }
        }

        float flash = god == God.THOR ? StormFx.flash(t) : 0f;
        boolean useShader = cinematic && statueCenter != null && spotNorm != null && ShaderFx.enabled();
        if (useShader) {
            // shader GLSL: langit badai + gelap + kilat; lubang berbentuk siluet patung
            ShaderFx.draw(g, width, height, god, spotNorm[0], spotNorm[1], spotNorm[2], dark, flash);
        } else {
            CosmosFx.overlay(g, width, height, t, god, spot);
        }
        if (cinematic && statueCenter != null) {
            if (!useShader) {
                StormFx.clouds(g, width, height, t, god, dark);
                CosmosFx.stars(g, width, height, t, god);
            }
            StormFx.islands(g, width, height, t, god, dark, flash);
            StormFx.statueGlow(g, width, height, t, god, dark);
            GiantFx.draw(g, width, height, t, god, MANIFEST_START, MANIFEST_END);
            StormFx.lightning(g, width, height, t, god);
            StormFx.mistLayers(g, width, height, t, god, dark);
            StormFx.embers(g, width, height, t, god, dark);
        }
        if (flash > 0f) g.fill(0, 0, width, height, FxSprites.argb(flash * (useShader ? 0.06f : 0.16f), 0xBFD8FF));

        // petunjuk skip selama cutscene berjalan
        if (cinematic && t < PANEL_START) {
            g.text(font, "Spasi / Enter: lewati", 10, height - 14, FxSprites.argb(0.55f, 0xFFFFFF), false);
        }

        // panel kontrak (meluncur masuk dengan easing)
        double panelE = ease((t - PANEL_START) / PANEL_DUR);
        if (panelE > 0.01) drawPanelBase(g);
    }

    private void drawPanelBase(GuiGraphicsExtractor g) {
        int a = accent();
        g.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, FxSprites.argb(0.18f, a & 0xFFFFFF));        // aura tepi
        g.fill(px, py, px + pw, py + ph, panelBg());
        g.fill(px, py, px + pw, py + 44, panelBg2());                                                // header
        g.fill(px, py, px + pw, py + 1, a);
        g.fill(px, py + ph - 1, px + pw, py + ph, a);
        g.fill(px, py, px + 1, py + ph, a);
        g.fill(px + pw - 1, py, px + pw, py + ph, a);
        g.fill(px + 12, py + 43, px + pw - 12, py + 44, FxSprites.argb(0.55f, a & 0xFFFFFF));        // garis pemisah header
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        double t = t();
        double panelE = ease((t - PANEL_START) / PANEL_DUR);
        px = baseX + (int) ((1 - panelE) * (pw + 24));
        boolean show = panelE > 0.01;
        signBtn.setX(px + 12);
        signBtn.visible = panelE > BUTTONS_AT;

        super.extractRenderState(g, mouseX, mouseY, delta);
        if (!show) return;

        int x = px + 14;
        int w = pw - 28;
        int y = py + 9;
        int a = accent();

        // ---- header ----
        g.text(font, "KONTRAK DEWA", x, y, a, false);
        g.text(font, god.statueName, x, y + 13, textMain(), true);
        y = py + 52;

        y = wrapped(g, "Kontrak = kamu pinjam kekuatan " + god.displayName + ". Kamu cuma bisa ikut 1 dewa.", x, y, w, textMuted());

        // ---- yang kamu dapat ----
        y += 9;
        y = section(g, "YANG KAMU DAPAT", x, y, green());
        for (String line : god.gives) y = bullet(g, line, x, y, w, textMain(), green()) + 1;

        // ---- harganya ----
        y += 8;
        y = section(g, "HARGANYA", x, y, red());
        y = bullet(g, god.price, x, y, w, textMain(), red());

        // ---- Faith ----
        if (signedHere) {
            y += 10;
            int lv = Faith.level(info.faithXp());
            int next = Faith.nextThreshold(lv);
            y = section(g, "FAITH " + lv + (next < 0 ? " (MAKS)" : ""), x, y, a);
            int barW = w;
            float frac = next < 0 ? 1f : (float) (info.faithXp() - Faith.currentThreshold(lv)) / Math.max(1, next - Faith.currentThreshold(lv));
            g.fill(x, y, x + barW, y + 5, 0xAA000000);
            g.fill(x, y, x + (int) (barW * Math.min(1f, Math.max(0f, frac))), y + 5, a);
            y += 8;
            if (next >= 0) g.text(font, info.faithXp() + " / " + next + " XP", x, y, textMuted(), false);
            y += 12;
            String offer = info.offeredToday()
                    ? "Persembahan hari ini sudah diberikan (streak " + info.streak() + " hari)."
                    : "Persembahan harian belum diberikan: klik kanan patung sambil pegang " + offeringText() + ".";
            y = wrapped(g, offer, x, y, w, textMuted());
        }

        // ---- hukuman ganti kontrak ----
        if (hasOther) {
            y += 10;
            y = section(g, "HUKUMAN GANTI KONTRAK", x, y, red());
            y = wrapped(g, "Faith -50%, weapon lama hancur, Weakness + Slowness 30 menit, murka "
                    + (info.currentGodId().equals("ra") ? "Ra (terbakar)" : "Thor (disambar petir)")
                    + ", lalu terkunci 24 jam.", x, y, w, textMuted());
        }

        // ---- status + penutup ----
        String status;
        if (hasOther && info.lockMinutes() > 0) status = "Terkunci murka dewa: " + info.lockMinutes() + " menit lagi.";
        else if (signedHere) status = "Kamu terikat kontrak dengan " + god.displayName + ".";
        else if (hasOther) status = "Kamu sudah punya kontrak dengan dewa lain.";
        else status = "Belum punya kontrak. Tanda tangan untuk mulai.";
        g.fill(x, py + ph - 46, x + w, py + ph - 45, FxSprites.argb(0.35f, a & 0xFFFFFF));
        wrapped(g, status, x, py + ph - 40, w, textMuted());
        g.text(font, "Esc: tutup", x, py + ph - 14, textMuted(), false);
    }

    private int section(GuiGraphicsExtractor g, String title, int x, int y, int color) {
        g.fill(x, y + 1, x + 3, y + 8, color);
        g.text(font, title, x + 7, y, color, true);
        return y + 13;
    }

    private int bullet(GuiGraphicsExtractor g, String text, int x, int y, int width, int color, int dot) {
        List<FormattedCharSequence> lines = font.split(Component.literal(text), width - 10);
        for (int i = 0; i < lines.size(); i++) {
            if (i == 0) g.fill(x + 1, y + 3, x + 4, y + 6, dot);
            g.text(font, lines.get(i), x + 9, y, color, true);
            y += font.lineHeight + 1;
        }
        return y;
    }

    private String offeringText() {
        return god == God.RA ? "emas, gandum, atau bunga matahari" : "besi, copper, atau lightning rod";
    }

    private int wrapped(GuiGraphicsExtractor g, String text, int x, int y, int width, int color) {
        List<FormattedCharSequence> lines = font.split(Component.literal(text), width);
        for (FormattedCharSequence l : lines) {
            g.text(font, l, x, y, color, true);
            y += font.lineHeight + 1;
        }
        return y;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
