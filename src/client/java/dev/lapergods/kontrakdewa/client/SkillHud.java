package dev.lapergods.kontrakdewa.client;

import dev.lapergods.kontrakdewa.BattleService;
import dev.lapergods.kontrakdewa.Faith;
import dev.lapergods.kontrakdewa.God;
import dev.lapergods.kontrakdewa.KontrakDewa;
import dev.lapergods.kontrakdewa.ModAttachments;
import dev.lapergods.kontrakdewa.WeaponService;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

/**
 * HUD kanan-bawah. Baris bawah: skill 1-3 (Z/C/V) + Special Magic (H, hanya Thor).
 * Baris atas: skill 4-6 (X/R/G). Di atasnya: Faith, bar XP, status Battle Mode, harga, stack Charged.
 */
public final class SkillHud {
    private static final int SLOT_W = 48;
    private static final int SLOT_H = 26;
    private static final int GAP = 3;

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

        int total = 4 * SLOT_W + 3 * GAP;
        int x0 = sw - total - 8;
        int yBottom = sh - SLOT_H - 8;
        int yTop = yBottom - SLOT_H - GAP;

        int xp = Faith.xp(player);
        int lv = Faith.level(xp);
        int next = Faith.nextThreshold(lv);

        // Faith + bar
        g.text(font, "Faith " + lv + (next < 0 ? " (maks)" : ""), x0, yTop - 40, god.accent, true);
        g.fill(x0, yTop - 29, x0 + total, yTop - 26, 0xAA000000);
        float frac = next < 0 ? 1f : (float) (xp - Faith.currentThreshold(lv)) / Math.max(1, next - Faith.currentThreshold(lv));
        g.fill(x0, yTop - 29, x0 + (int) (total * Math.min(1f, Math.max(0f, frac))), yTop - 26, god.accent);

        // Status
        String status;
        int statusColor;
        if (!battle) { status = "Damai (B = Battle)"; statusColor = 0xFFB8B0A0; }
        else if (!WeaponService.holding(player, god)) { status = "Pegang " + god.weaponName + "!"; statusColor = 0xFFFFB347; }
        else { status = "BATTLE MODE"; statusColor = 0xFFFF6A5A; }
        g.text(font, status, x0, yTop - 22, statusColor, true);
        String price = BattleService.priceLabel(player);
        if (price != null) g.text(font, price, x0 + total - font.width(price), yTop - 22, god.accent, true);
        int charged = player.getAttachedOrElse(ModAttachments.CHARGED, 0);
        if (charged > 0) g.text(font, "Charged x" + charged, x0, yTop - 11, 0xFF7FC8FF, true);

        // Slot: 0-2 baris bawah, 6 (special) baris bawah kolom 4, 3-5 baris atas
        for (int i = 0; i < 3; i++) slot(g, font, god, player, battle, i, x0 + i * (SLOT_W + GAP), yBottom);
        if (god.special != null) slot(g, font, god, player, battle, 6, x0 + 3 * (SLOT_W + GAP), yBottom);
        for (int i = 3; i < 6; i++) slot(g, font, god, player, battle, i, x0 + (i - 3) * (SLOT_W + GAP), yTop);
    }

    private static void slot(GuiGraphicsExtractor g, Font font, God god, LocalPlayer player, boolean battle, int index, int x, int y) {
        boolean locked = Faith.level(player) < Faith.requiredFaithForSlot(index);
        boolean special = index == 6;
        g.fill(x, y, x + SLOT_W, y + SLOT_H, battle && !locked ? 0xCC141414 : 0x88141414);
        int edge = locked ? 0xFF444444 : battle ? (special ? 0xFFFFD24A : god.accent) : 0xFF555555;
        g.fill(x, y, x + SLOT_W, y + 1, edge);
        g.fill(x, y + SLOT_H - 1, x + SLOT_W, y + SLOT_H, edge);
        g.fill(x, y, x + 1, y + SLOT_H, edge);
        g.fill(x + SLOT_W - 1, y, x + SLOT_W, y + SLOT_H, edge);

        String key = ModKeys.SKILLS[index].getTranslatedKeyMessage().getString();
        g.text(font, key, x + 3, y + 3, battle && !locked ? 0xFFFFFFFF : 0xFF888888, true);

        if (locked) {
            String t = "Faith " + Faith.requiredFaithForSlot(index);
            g.text(font, t, x + 3, y + SLOT_H - 11, 0xFF777777, false);
            return;
        }
        String name = god.skillName(index);
        while (font.width(name) > SLOT_W - 4 && name.length() > 3) name = name.substring(0, name.length() - 1);
        g.text(font, name, x + 3, y + SLOT_H - 11, battle ? 0xFFE8E0D0 : 0xFF777777, false);

        double cd = ClientCooldowns.remaining(index);
        if (cd > 0) {
            g.fill(x + 1, y + 1, x + SLOT_W - 1, y + SLOT_H - 1, 0xAA000000);
            String t = cd >= 10 ? String.valueOf((int) Math.ceil(cd)) : String.format("%.1f", cd);
            g.text(font, t, x + SLOT_W - 4 - font.width(t), y + 3, 0xFFFFFFFF, true);
        }
    }

    private SkillHud() {}
}
