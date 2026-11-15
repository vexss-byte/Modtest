package dev.lapergods.kontrakdewa.client;

import dev.lapergods.kontrakdewa.BattleService;
import dev.lapergods.kontrakdewa.Faith;
import dev.lapergods.kontrakdewa.God;
import dev.lapergods.kontrakdewa.KontrakDewa;
import dev.lapergods.kontrakdewa.WeaponService;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

/** HUD kanan-bawah: Faith, status Battle Mode, 3 slot skill (Z/C/V) dengan cooldown. */
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

        // Faith + bar XP
        int xp = Faith.xp(player);
        int lv = Faith.level(xp);
        int next = Faith.nextThreshold(lv);
        g.text(font, "Faith " + lv + (next < 0 ? " (maks)" : ""), x0, y0 - 36, god.accent, true);
        int barW = total;
        g.fill(x0, y0 - 25, x0 + barW, y0 - 22, 0xAA000000);
        float frac = next < 0 ? 1f : (float) (xp - Faith.currentThreshold(lv)) / Math.max(1, next - Faith.currentThreshold(lv));
        g.fill(x0, y0 - 25, x0 + (int) (barW * Math.min(1f, Math.max(0f, frac))), y0 - 22, god.accent);

        // Status Battle Mode + harga
        String status;
        int statusColor;
        if (!battle) { status = "Damai (B = Battle)"; statusColor = 0xFFB8B0A0; }
        else if (!WeaponService.holding(player, god)) { status = "Pegang " + god.weaponName + "!"; statusColor = 0xFFFFB347; }
        else { status = "BATTLE MODE"; statusColor = 0xFFFF6A5A; }
        g.text(font, status, x0, y0 - 18, statusColor, true);
        String price = BattleService.priceLabel(player);
        if (price != null) g.text(font, price, x0 + total - font.width(price), y0 - 18, god.accent, true);

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
            while (font.width(name) > SLOT_W - 4 && name.length() > 3) name = name.substring(0, name.length() - 1);
            g.text(font, name, x + 3, y0 + SLOT_H - 11, battle ? 0xFFE8E0D0 : 0xFF777777, false);

            double cd = ClientCooldowns.remaining(i);
            if (cd > 0) {
                g.fill(x + 1, y0 + 1, x + SLOT_W - 1, y0 + SLOT_H - 1, 0xAA000000);
                String t = cd >= 10 ? String.valueOf((int) Math.ceil(cd)) : String.format("%.1f", cd);
                g.text(font, t, x + SLOT_W - 4 - font.width(t), y0 + 3, 0xFFFFFFFF, true);
            }
        }
    }

    private SkillHud() {}
}
