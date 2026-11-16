package dev.lapergods.kontrakdewa.client;

import dev.lapergods.kontrakdewa.Faith;
import dev.lapergods.kontrakdewa.God;
import dev.lapergods.kontrakdewa.ModBlocks;
import dev.lapergods.kontrakdewa.net.ClaimWeaponPayload;
import dev.lapergods.kontrakdewa.net.OpenContractPayload;
import dev.lapergods.kontrakdewa.net.SignContractPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Layar kontrak dengan cutscene pembuka:
 * 0-1 dtk kamera menengok ke patung + layar menggelap, 0,2-1 dtk bintang muncul,
 * 0,6-2,6 dtk konstelasi dewa tergambar, 2,0-2,7 dtk panel kontrak meluncur dari kanan.
 */
public class ContractScreen extends Screen {
    private static final int PANEL_BG = 0xE6100C08;
    private static final int GREEN = 0xFF7FE0B0;
    private static final int RED = 0xFFFF8A80;
    private static final int GREY = 0xFFB8B0A0;
    private static final int WHITE = 0xFFFFFFFF;

    private static final double CAMERA_TIME = 1.0;
    private static final double CONSTELLATION_START = 0.6, CONSTELLATION_DUR = 2.0;
    private static final double PANEL_START = 2.0, PANEL_DUR = 0.7;

    private final God god;
    private final OpenContractPayload info;
    private final boolean signedHere;
    private final boolean hasOther;

    private final long startNanos = System.nanoTime();
    private float origYaw, origPitch;
    private boolean rotationSaved = false;
    private Vec3 statueCenter = null;
    private boolean chimed = false;

    private int px, py, pw, ph;   // px = posisi panel saat ini (termasuk geser)
    private int baseX;            // posisi panel saat sudah masuk penuh
    private Button signBtn, claimBtn;

    public ContractScreen(God god, OpenContractPayload info) {
        super(Component.literal(god.statueName));
        this.god = god;
        this.info = info;
        this.signedHere = god.id.equals(info.currentGodId());
        this.hasOther = !info.currentGodId().isEmpty() && !signedHere;
    }

    private double t() {
        return (System.nanoTime() - startNanos) / 1_000_000_000.0;
    }

    @Override
    protected void init() {
        pw = Math.min(300, (int) (width * 0.42));
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
        }).bounds(px + 10, py + ph - 70, pw - 20, 22).build();
        signBtn.active = !signedHere && info.lockMinutes() <= 0;
        addRenderableWidget(signBtn);

        if (minecraft != null && minecraft.player != null && minecraft.level != null) {
            LocalPlayer player = minecraft.player;
            if (!rotationSaved) {
                origYaw = player.getYRot();
                origPitch = player.getXRot();
                rotationSaved = true;
            }
            statueCenter = findStatue(player);
            if (statueCenter != null) {
                minecraft.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BEACON_ACTIVATE, SoundSource.AMBIENT, 0.6f, 0.6f, false);
            }
        }
    }

    /** Cari titik tengah (setinggi dada patung) dari blok patung dewa ini di dekat pemain. */
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
        return best == null ? null : new Vec3(best.getX() + 0.5, best.getY() + 3.0, best.getZ() + 0.5);
    }

    @Override
    public void tick() {
        super.tick();
        if (minecraft == null || minecraft.player == null || minecraft.level == null || statueCenter == null) return;
        LocalPlayer p = minecraft.player;
        double t = t();

        // Kamera menengok ke patung; sedikit digeser supaya patung berada di sisi kiri (kanan layar dipakai panel).
        double eased = ease(Math.min(1.0, t / CAMERA_TIME));
        Vec3 eye = p.getEyePosition();
        double dx = statueCenter.x - eye.x, dy = statueCenter.y - eye.y, dz = statueCenter.z - eye.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yawToStatue = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        float pitchToStatue = (float) -(Math.atan2(dy, horiz) * 180.0 / Math.PI);
        pitchToStatue = Math.max(-60f, Math.min(30f, pitchToStatue));

        double fov = minecraft.options.fov().get();
        double tanH = Math.tan(Math.toRadians(fov) / 2.0) * ((double) width / height);
        double dxFrac = (width / 2.0 - baseX / 2.0) / (width / 2.0); // seberapa jauh pusat sisi kiri dari tengah layar
        float offset = (float) Math.toDegrees(Math.atan(dxFrac * tanH));
        float targetYaw = yawToStatue + offset;

        float yawDiff = wrap(targetYaw - origYaw);
        p.setYRot(origYaw + (float) (yawDiff * eased));
        p.setXRot(origPitch + (float) ((pitchToStatue - origPitch) * eased));

        // Partikel naik di sekeliling patung
        if (t > 0.3 && t < 4.5) {
            for (int i = 0; i < 3; i++) {
                double a = minecraft.level.getRandom().nextDouble() * Math.PI * 2;
                double r = 1.5 + minecraft.level.getRandom().nextDouble() * 2.5;
                double x = statueCenter.x + Math.cos(a) * r;
                double z = statueCenter.z + Math.sin(a) * r;
                double y = statueCenter.y - 3.0 + 0.1;
                minecraft.level.addParticle(god == God.RA ? ParticleTypes.FLAME : ParticleTypes.ELECTRIC_SPARK,
                        x, y, z, 0, 0.06 + minecraft.level.getRandom().nextDouble() * 0.06, 0);
                if (i == 0) minecraft.level.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0.05, 0);
            }
        }
        if (!chimed && t > CONSTELLATION_START + CONSTELLATION_DUR) {
            chimed = true;
            minecraft.level.playLocalSound(p.getX(), p.getY(), p.getZ(),
                    SoundEvents.BEACON_POWER_SELECT, SoundSource.AMBIENT, 0.7f, god == God.RA ? 1.2f : 1.5f, false);
        }
    }

    private static float wrap(float deg) {
        deg = deg % 360f;
        if (deg >= 180f) deg -= 360f;
        if (deg < -180f) deg += 360f;
        return deg;
    }

    private static double ease(double x) {
        double c = Math.max(0, Math.min(1, x));
        return 1 - Math.pow(1 - c, 3);
    }

    @Override
    public void removed() {
        super.removed();
        if (rotationSaved && minecraft != null && minecraft.player != null) {
            minecraft.player.setYRot(origYaw);
            minecraft.player.setXRot(origPitch);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        // Tidak memanggil super: latar dunia dibiarkan terlihat, lalu kita tindih sendiri dengan efek kosmik.
        double t = t();
        CosmosFx.overlay(g, width, height, t, god);
        CosmosFx.stars(g, width, height, t, god);
        int size = Math.min((int) (height * 0.92), (int) (baseX * 0.95 / 0.8));
        CosmosFx.constellation(g, baseX / 2, height / 2, size, t, CONSTELLATION_START, CONSTELLATION_DUR, god);

        // panel kontrak (meluncur masuk)
        double panelE = ease((t - PANEL_START) / PANEL_DUR);
        if (panelE > 0.01) {
            g.fill(px, py, px + pw, py + ph, PANEL_BG);
            g.fill(px, py, px + pw, py + 1, god.accent);
            g.fill(px, py + ph - 1, px + pw, py + ph, god.accent);
            g.fill(px, py, px + 1, py + ph, god.accent);
            g.fill(px + pw - 1, py, px + pw, py + ph, god.accent);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        double t = t();
        double panelE = ease((t - PANEL_START) / PANEL_DUR);
        px = baseX + (int) ((1 - panelE) * (pw + 24));
        boolean show = panelE > 0.01;
        signBtn.visible = show;
        signBtn.setX(px + 10);
        if (claimBtn != null) {
            claimBtn.visible = show;
            claimBtn.setX(px + 10);
        }

        super.extractRenderState(g, mouseX, mouseY, delta);
        if (!show) return;

        int x = px + 12;
        int w = pw - 24;
        int y = py + 12;

        g.text(font, god.statueName, x, y, WHITE, true);
        y += 16;
        y = wrapped(g, "Kontrak = kamu pinjam kekuatan " + god.displayName + ". Kamu cuma bisa ikut 1 dewa.", x, y, w, GREY);

        y += 8;
        g.text(font, "Yang kamu dapat", x + 2, y, GREEN, true);
        y += 13;
        for (String line : god.gives) y = wrapped(g, "- " + line, x + 4, y, w - 4, WHITE) + 1;

        y += 8;
        g.text(font, "Harganya", x + 2, y, RED, true);
        y += 13;
        y = wrapped(g, god.price, x + 4, y, w - 4, WHITE);

        if (signedHere) {
            y += 10;
            int lv = Faith.level(info.faithXp());
            int next = Faith.nextThreshold(lv);
            g.text(font, "Faith " + lv + (next < 0 ? " (maks)" : "  " + info.faithXp() + " / " + next + " XP"), x + 2, y, god.accent, true);
            y += 12;
            String offer = info.offeredToday()
                    ? "Persembahan hari ini sudah diberikan (streak " + info.streak() + " hari)."
                    : "Persembahan harian belum diberikan: klik kanan patung sambil pegang " + offeringText() + ".";
            wrapped(g, offer, x + 2, y, w - 2, GREY);
        }

        if (hasOther) {
            y += 10;
            g.text(font, "Hukuman ganti kontrak", x + 2, y, RED, true);
            y += 12;
            y = wrapped(g, "Faith -50%, weapon lama hancur, Weakness + Slowness 30 menit, murka "
                    + (info.currentGodId().equals("ra") ? "Ra (terbakar)" : "Thor (disambar petir)")
                    + ", lalu terkunci 24 jam.", x + 2, y, w - 2, GREY);
        }

        String status;
        if (hasOther && info.lockMinutes() > 0) status = "Terkunci murka dewa: " + info.lockMinutes() + " menit lagi.";
        else if (signedHere) status = "Kamu terikat kontrak dengan " + god.displayName + ".";
        else if (hasOther) status = "Kamu sudah punya kontrak dengan dewa lain.";
        else status = "Belum punya kontrak. Tanda tangan untuk mulai.";
        wrapped(g, status, x, py + ph - 40, w, GREY);
        g.text(font, "Esc: tutup", x, py + ph - 16, GREY, false);
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
