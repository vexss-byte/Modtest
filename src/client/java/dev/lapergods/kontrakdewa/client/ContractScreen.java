package dev.lapergods.kontrakdewa.client;

import dev.lapergods.kontrakdewa.God;
import dev.lapergods.kontrakdewa.net.SignContractPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Layar kontrak: panel di kanan seperti di video (judul, deskripsi, "Yang kamu dapat", "Harganya", Sign Contract). */
public class ContractScreen extends Screen {
    private static final int PANEL_BG = 0xE6100C08;
    private static final int GREEN = 0xFF7FE0B0;
    private static final int RED = 0xFFFF8A80;
    private static final int GREY = 0xFFB8B0A0;
    private static final int WHITE = 0xFFFFFFFF;

    private final God god;
    private final String currentGodId; // "" = belum berkontrak

    private int px, py, pw, ph;

    public ContractScreen(God god, String currentGodId) {
        super(Component.literal(god.statueName));
        this.god = god;
        this.currentGodId = currentGodId == null ? "" : currentGodId;
    }

    @Override
    protected void init() {
        pw = Math.min(300, (int) (width * 0.42));
        ph = height - 24;
        px = width - pw - 12;
        py = 12;

        boolean already = god.id.equals(currentGodId);
        Component label = already ? Component.literal("Kontrak aktif") : Component.literal("Sign Contract");
        Button sign = Button.builder(label, b -> {
            ClientPlayNetworking.send(new SignContractPayload(god.id));
            onClose();
        }).bounds(px + 10, py + ph - 70, pw - 20, 22).build();
        sign.active = !already;
        addRenderableWidget(sign);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g, mouseX, mouseY, delta);
        // panel + garis aksen warna dewa
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

        y += 10;
        g.text(font, "Yang kamu dapat", x + 2, y, GREEN, true);
        y += 14;
        for (String line : god.gives) {
            y = wrapped(g, "- " + line, x + 4, y, w - 4, WHITE) + 2;
        }

        y += 10;
        g.text(font, "Harganya", x + 2, y, RED, true);
        y += 14;
        y = wrapped(g, god.price, x + 4, y, w - 4, WHITE);

        // status di bawah tombol
        String status;
        if (god.id.equals(currentGodId)) status = "Kamu sudah terikat kontrak dengan " + god.displayName + ".";
        else if (!currentGodId.isEmpty()) status = "Kamu sudah punya kontrak dengan dewa lain.";
        else status = "Belum punya kontrak. Tanda tangan untuk mulai.";
        wrapped(g, status, x, py + ph - 40, w, GREY);

        g.text(font, "Esc: tutup", x, py + ph - 16, GREY, false);
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
