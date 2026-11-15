package dev.lapergods.kontrakdewa;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** Faith (level 1-10): XP dari memakai kekuatan dewa + persembahan harian (reset 00:00 WIB). */
public final class Faith {
    private static final ZoneId WIB = ZoneId.of("Asia/Jakarta");

    public static long today() {
        return LocalDate.now(WIB).toEpochDay();
    }

    public static int xp(Player p) {
        return p.getAttachedOrElse(ModAttachments.FAITH_XP, 0);
    }

    public static int level(int xp) {
        int[] t = ModConfig.get().faithThresholds;
        int lv = 1;
        for (int i = 1; i < t.length; i++) if (xp >= t[i]) lv = i + 1;
        return lv;
    }

    public static int level(Player p) {
        return level(xp(p));
    }

    /** XP minimum untuk level berikutnya, atau -1 kalau sudah Faith 10. */
    public static int nextThreshold(int level) {
        int[] t = ModConfig.get().faithThresholds;
        return level >= t.length ? -1 : t[level];
    }

    public static int currentThreshold(int level) {
        return ModConfig.get().faithThresholds[Math.max(0, level - 1)];
    }

    /** Skill 1-3 terbuka dari awal; skill 4, 5, 6 di Faith 3, 6, 9. */
    public static int requiredFaithForSlot(int slot) {
        return switch (slot) {
            case 3 -> 3;
            case 4 -> 6;
            case 5 -> 9;
            default -> 1;
        };
    }

    public static void addXp(ServerPlayer p, int amount) {
        if (amount <= 0) return;
        int before = xp(p);
        int after = before + amount;
        p.setAttached(ModAttachments.FAITH_XP, after);
        int lvBefore = level(before), lvAfter = level(after);
        if (lvAfter > lvBefore) {
            God god = BattleService.godOf(p);
            p.sendSystemMessage(Component.literal("Faith naik ke " + lvAfter + "!"
                    + (god != null ? " " + god.displayName + " semakin dekat denganmu." : "")), true);
            p.sendSystemMessage(Component.literal("Faith kamu naik ke level " + lvAfter + "."));
        }
    }

    /** XP dari pertarungan; dibatasi per hari. */
    public static void addBattleXp(ServerPlayer p, int amount) {
        ModConfig cfg = ModConfig.get();
        long today = today();
        if (p.getAttachedOrElse(ModAttachments.DAILY_DAY, 0L) != today) {
            p.setAttached(ModAttachments.DAILY_DAY, today);
            p.setAttached(ModAttachments.DAILY_XP, 0);
        }
        int used = p.getAttachedOrElse(ModAttachments.DAILY_XP, 0);
        int allowed = Math.min(amount, cfg.dailyBattleXpCap - used);
        if (allowed <= 0) return;
        p.setAttached(ModAttachments.DAILY_XP, used + allowed);
        addXp(p, allowed);
    }

    // ---- persembahan ----
    public static List<Item> offeringItems(God god) {
        return god == God.RA
                ? List.of(Items.GOLD_INGOT, Items.WHEAT, Items.SUNFLOWER)
                : List.of(Items.IRON_INGOT, Items.COPPER_INGOT);
    }

    public static boolean isOffering(God god, ItemStack stack) {
        for (Item i : offeringItems(god)) if (stack.is(i)) return true;
        // Lightning rod (semua varian karat/wax) juga diterima Thor
        return god == God.THOR && BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("lightning_rod");
    }

    public static boolean offeredToday(Player p) {
        return p.getAttachedOrElse(ModAttachments.OFFER_DAY, 0L) == today();
    }

    public static int streak(Player p) {
        return p.getAttachedOrElse(ModAttachments.STREAK, 0);
    }

    public static void offer(ServerPlayer p, God god, ItemStack held) {
        long today = today();
        long last = p.getAttachedOrElse(ModAttachments.OFFER_DAY, 0L);
        if (last == today) {
            p.sendSystemMessage(Component.literal("Persembahan hari ini sudah diterima " + god.displayName
                    + ". Kembali besok (reset 00.00 WIB)."), true);
            return;
        }
        int streak = (last == today - 1) ? streak(p) + 1 : 1;
        ModConfig cfg = ModConfig.get();
        int bonus = Math.min(cfg.offeringStreakBonusMax, (streak - 1) * cfg.offeringStreakBonus);
        if (!p.isCreative()) held.shrink(1);
        p.setAttached(ModAttachments.OFFER_DAY, today);
        p.setAttached(ModAttachments.STREAK, streak);
        addXp(p, cfg.offeringXp + bonus);
        p.sendSystemMessage(Component.literal("Persembahan diterima " + god.displayName + ". +"
                + (cfg.offeringXp + bonus) + " Faith XP (streak " + streak + " hari)."));
    }

    private Faith() {}
}
