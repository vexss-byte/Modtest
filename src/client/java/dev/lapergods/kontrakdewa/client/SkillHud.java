package dev.lapergods.kontrakdewa.client;

import dev.lapergods.kontrakdewa.BattleService;
import dev.lapergods.kontrakdewa.God;
import dev.lapergods.kontrakdewa.KontrakDewa;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

/** HUD kanan-bawah: status Battle Mode + 3 slot skill (Z/C/V), mirip video referensi. */
public final class SkillHud {
    private static final int SLOT_W = 50;
    private static final int SLOT_H = 30;
    private static final int GAP = 4;

    public static void init() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, KontrakDewa.id("skill_hud"), SkillHud::extract);
    }

    private static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        God god = BattleService.godOf(player);
        if (god == null) return;

        Font font = mc.font;
        boolean battle = BattleService.inBattle(player);
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        int total = 3 * SLOT_W + 2 * GAP;
        int x0 = sw - total - 8;
        int y0 = sh - SLOT_H - 8;

        // Baris status di atas slot
        String status = battle ? "BATTLE MODE" : "Damai (B = Battle)";
        g.text(font, status, x0, y0 - 24, battle ? 0xFFFF6A5A : 0xFFB8B0A0, true);
        String price = BattleService.priceLabel(player);
        if (price != null) g.text(font, price, x0, y0 - 12, god.accent, true);

        for (int i = 0; i < 3; i++) {
            int x = x0 + i * (SLOT_W + GAP);
            g.fill(x, y0, x + SLOT_W, y0 + SLOT_H, battle ? 0xCC141414 : 0x88141414);
            int edge = battle ? god.accent : 0xFF555555;
            g.fill(x, y0, x + SLOT_W, y0 + 1, edge);
            g.fill(x, y0 + SLOT_H - 1, x + SLOT_W, y0 + SLOT_H, edge);
            g.fill(x, y0, x + 1, y0 + SLOT_H, edge);
            g.fill(x + SLOT_W - 1, y0, x + SLOT_W, y0 + SLOT_H, edge);

            String key = ModKeys.SKILLS[i].getTranslatedKeyMessage().getString();
            g.text(font, key, x + 3, y0 + 3, battle ? 0xFFFFFFFF : 0xFF888888, true);

            String name = god.skills.get(i);
            // potong nama kalau kepanjangan
            while (font.width(name) > SLOT_W - 4 && name.length() > 3) name = name.substring(0, name.length() - 1);
            g.text(font, name, x + 3, y0 + SLOT_H - 11, battle ? 0xFFE8E0D0 : 0xFF777777, false);
        }
    }

    private SkillHud() {}
}
