package dev.lapergods.kontrakdewa;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Battle Mode + "harga" tiap dewa. Dipakai server (logika) dan client (HUD). */
public final class BattleService {
    private static final Identifier DAMAGE_MOD = KontrakDewa.id("battle_damage");
    private static final Identifier ARMOR_MOD = KontrakDewa.id("battle_armor");

    public static God godOf(Player p) {
        String id = p.getAttached(ModAttachments.CONTRACT);
        return id == null ? null : God.byId(id);
    }

    public static boolean inBattle(Player p) {
        return p.getAttachedOrElse(ModAttachments.BATTLE_MODE, false);
    }

    /** Siang cerah = terang di luar dan tidak hujan/badai. */
    public static boolean isClearDay(Level level) {
        return level.isBrightOutside() && !level.isRaining() && !level.isThundering();
    }

    /**
     * Pengali damage dari "harga" dewa. 1.0 = normal.
     * Ra (hanya saat Battle Mode): siang cerah +20%, malam -25%.
     * Skill tahap 3 juga akan memakai pengali ini.
     */
    public static double damageMultiplier(Player p) {
        if (godOf(p) != God.RA || !inBattle(p)) return 1.0;
        Level level = p.level();
        if (isClearDay(level)) return 1.20;
        if (!level.isBrightOutside()) return 0.75;
        return 1.0;
    }

    /** Teks status untuk HUD, atau null kalau tidak ada efek. */
    public static String priceLabel(Player p) {
        God god = godOf(p);
        if (god == null || !inBattle(p)) return null;
        if (god == God.RA) {
            double m = damageMultiplier(p);
            if (m > 1.0) return "Noon +20%";
            if (m < 1.0) return "Night -25%";
            return "Damage normal";
        }
        return "Armor -50%";
    }

    public static void toggle(ServerPlayer player) {
        God god = godOf(player);
        if (god == null) {
            player.sendSystemMessage(Component.literal("Kamu belum punya kontrak dewa. Klik kanan patung dewa dulu."), true);
            return;
        }
        boolean now = !inBattle(player);
        player.setAttached(ModAttachments.BATTLE_MODE, now);
        if (now) WeaponService.enforce(player, god); else WeaponService.removeAll(player, god);
        refresh(player);
        player.sendSystemMessage(Component.literal(now
                ? "Battle Mode AKTIF - " + god.weaponName + " muncul di tanganmu."
                : "Battle Mode mati."), true);
    }

    /** Pasang/cabut modifier atribut sesuai dewa + status Battle Mode. Dipanggil tiap detik dan saat toggle. */
    public static void refresh(ServerPlayer player) {
        God god = godOf(player);
        boolean battle = god != null && inBattle(player);

        AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            double m = battle ? damageMultiplier(player) : 1.0;
            if (m != 1.0) {
                attack.addOrReplacePermanentModifier(new AttributeModifier(DAMAGE_MOD, m - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else {
                attack.removeModifier(DAMAGE_MOD);
            }
        }

        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            if (battle && god == God.THOR) {
                armor.addOrReplacePermanentModifier(new AttributeModifier(ARMOR_MOD, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else {
                armor.removeModifier(ARMOR_MOD);
            }
        }
    }

    private BattleService() {}
}
