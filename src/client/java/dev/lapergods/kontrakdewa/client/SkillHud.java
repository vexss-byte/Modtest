package dev.lapergods.kontrakdewa.client;

import dev.lapergods.kontrakdewa.BattleService;
import dev.lapergods.kontrakdewa.Faith;
import dev.lapergods.kontrakdewa.God;
import dev.lapergods.kontrakdewa.KontrakDewa;
import dev.lapergods.kontrakdewa.ModAttachments;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

/**
 * HUD kontrak.
 * - Selalu (kalau berkontrak): kartu Faith + bar XP + status di kanan-bawah.
 * - Saat Battle Mode: hotbar bawaan diganti hotbar Battle (slot weapon + 3 skill Z/C/V), seperti mod Ultimate Battle.
 */
public final class SkillHud {
    private static final int WEAPON_W = 28;
    private static final int SKILL_W = 66;
    private static final int SLOT_H = 28;
    private static final int GAP = 4;

    public static void init() {
        // Hotbar bawaan disembunyikan saat Battle Mode
        HudElementRegistry.replaceElement(VanillaHudElements.HOTBAR, original -> (g, delta) -> {
            if (isBattle()) return;
            original.extractRenderState(g, delta);
        });
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, KontrakDewa.id("skill_hud"), SkillHud::extract);
    }

    private static boolean isBattle() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null && BattleService.godOf(p) != null && BattleService.inBattle(p);
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

        // ---- kartu Faith (kanan-bawah) ----
        int cardW = 150;
        int x0 = sw - cardW - 8;
        int y0 = sh - 74;
        int xp = Faith.xp(player);
        int lv = Faith.level(xp);
        int next = Faith.nextThreshold(lv);
        g.text(font, "Faith " + lv + (next < 0 ? " (maks)" : "  " + xp + "/" + next), x0, y0, god.accent, true);
        g.fill(x0, y0 + 11, x0 + cardW, y0 + 14, 0xAA000000);
        float frac = next < 0 ? 1f : (float) (xp - Faith.currentThreshold(lv)) / Math.max(1, next - Faith.currentThreshold(lv));
        g.fill(x0, y0 + 11, x0 + (int) (cardW * Math.min(1f, Math.max(0f, frac))), y0 + 14, god.accent);
        g.text(font, battle ? "BATTLE MODE" : "Damai (B = Battle)", x0, y0 + 18, battle ? 0xFFFF6A5A : 0xFFB8B0A0, true);
        String price = BattleService.priceLabel(player);
        if (price != null) g.text(font, price, x0, y0 + 30, god.accent, true);

        if (!battle) return;

        // ---- hotbar Battle (tengah-bawah) ----
        int total = WEAPON_W + 3 * (SKILL_W + GAP);
        int x = sw / 2 - total / 2;
        int y = sh - SLOT_H - 4;

        // slot weapon (selalu terpilih)
        g.fill(x, y, x + WEAPON_W, y + SLOT_H, 0xCC141414);
        frame(g, x, y, WEAPON_W, SLOT_H, 0xFFFFFFFF);
        frame(g, x + 1, y + 1, WEAPON_W - 2, SLOT_H - 2, god.accent);
        WeaponIcons.draw(g, god, x + (WEAPON_W - 16) / 2, y + (SLOT_H - 16) / 2);

        int charged = player.getAttachedOrElse(ModAttachments.CHARGED, 0);
        if (charged > 0) g.text(font, "Charged x" + charged, x, y - 11, 0xFF7FC8FF, true);

        for (int i = 0; i < 3; i++) {
            int sx = x + WEAPON_W + GAP + i * (SKILL_W + GAP);
            g.fill(sx, y, sx + SKILL_W, y + SLOT_H, 0xCC141414);
            frame(g, sx, y, SKILL_W, SLOT_H, god.accent);

            String key = ModKeys.SKILLS[i].getTranslatedKeyMessage().getString();
            g.text(font, key, sx + 3, y + 3, 0xFFFFFFFF, true);

            String name = god.skillName(i);
            while (font.width(name) > SKILL_W - 6 && name.length() > 3) name = name.substring(0, name.length() - 1);
            g.text(font, name, sx + 3, y + SLOT_H - 11, 0xFFE8E0D0, false);

            double cd = ClientCooldowns.remaining(i);
            if (cd > 0) {
                g.fill(sx + 1, y + 1, sx + SKILL_W - 1, y + SLOT_H - 1, 0xAA000000);
                String t = cd >= 10 ? String.valueOf((int) Math.ceil(cd)) : String.format("%.1f", cd);
                g.text(font, t, sx + SKILL_W - 4 - font.width(t), y + 3, 0xFFFFFFFF, true);
            }
        }
    }

    private static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    private SkillHud() {}
}
