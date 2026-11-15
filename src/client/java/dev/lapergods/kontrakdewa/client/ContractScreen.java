package dev.lapergods.kontrakdewa.client;

import dev.lapergods.kontrakdewa.Faith;
import dev.lapergods.kontrakdewa.God;
import dev.lapergods.kontrakdewa.net.ClaimWeaponPayload;
import dev.lapergods.kontrakdewa.net.OpenContractPayload;
import dev.lapergods.kontrakdewa.net.SignContractPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Layar kontrak: panel di kanan (judul, deskripsi, "Yang kamu dapat", "Harganya", Faith, tombol). */
public class ContractScreen extends Screen {
    private static final int PANEL_BG = 0xE6100C08;
    private static final int GREEN = 0xFF7FE0B0;
    private static final int RED = 0xFFFF8A80;
    private static final int GREY = 0xFFB8B0A0;
    private static final int WHITE = 0xFFFFFFFF;

    private final God god;
    private final OpenContractPayload info;
    private final boolean signedHere;   // sudah berkontrak dengan dewa patung ini
    private final boolean hasOther;     // berkontrak dengan dewa lain

    private int px, py, pw, ph;

    public ContractScreen(God god, OpenContractPayload info) {
        super(Component.literal(god.statueName));
        this.god = god;
        this.info = info;
        this.signedHere = god.id.equals(info.currentGodId());
        this.hasOther = !info.currentGodId().isEmpty() && !signedHere;
    }

    @Override
    protected void init() {
        pw = Math.min(300, (int) (width * 0.42));
        ph = height - 24;
        px = width - pw - 12;
        py = 12;

        Component label = signedHere ? Component.literal("Kontrak aktif")
                : hasOther ? Component.literal("Ganti Kontrak (HUKUMAN)").withStyle(net.minecraft.ChatFormatting.RED)
                : Component.literal("Sign Contract");
        Button sign = Button.builder(label, b -> {
            ClientPlayNetworking.send(new SignContractPayload(god.id));
            onClose();
        }).bounds(px + 10, py + ph - 70, pw - 20, 22).build();
        sign.active = !signedHere && info.lockMinutes() <= 0;
        addRenderableWidget(sign);

        if (signedHere && !info.hasWeapon()) {
            Button claim = Button.builder(Component.literal("Ambil " + god.weaponName), b -> {
                ClientPlayNetworking.send(ClaimWeaponPayload.INSTANCE);
                onClose();
            }).bounds(px + 10, py + ph - 96, pw - 20, 22).build();
            addRenderableWidget(claim);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g, mouseX, mouseY, delta);
        g.fill(px, py, px + pw, py + ph, PANEL_BG);
        g.fill(px, py, px + pw, py + 1, god.accent);
        g.fill(px, py + ph - 1, px + pw, py + ph, god.accent);
        g.fill(px, py, px + 1, py + ph, god.accent);
        g.fill(px + pw - 1, py, px + pw, py + ph, god.accent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);

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

        // Faith (hanya kalau sudah berkontrak dengan dewa ini)
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
