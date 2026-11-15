package dev.lapergods.kontrakdewa;

import dev.lapergods.kontrakdewa.net.CooldownPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Semua pemeriksaan pemakaian skill (di server): kontrak, Battle Mode, weapon di tangan, Faith, cooldown. */
public final class SkillService {
    private static final Map<UUID, long[]> COOLDOWNS = new HashMap<>();

    public static void use(ServerPlayer p, int slot) {
        God god = BattleService.godOf(p);
        if (god == null || slot < 0 || slot > 5) return;
        if (!BattleService.inBattle(p)) {
            p.sendSystemMessage(Component.literal("Aktifkan Battle Mode dulu (tekan B)."), true);
            return;
        }
        if (!WeaponService.holding(p, god)) {
            p.sendSystemMessage(Component.literal("Pegang " + god.weaponName + " untuk memakai skill."), true);
            return;
        }
        int need = Faith.requiredFaithForSlot(slot);
        if (Faith.level(p) < need) {
            p.sendSystemMessage(Component.literal(god.skills.get(slot) + " terkunci: butuh Faith " + need + "."), true);
            return;
        }
        if (slot >= 3) {
            p.sendSystemMessage(Component.literal(god.skills.get(slot) + " menyusul di tahap 3b."), true);
            return;
        }

        long now = p.level().getGameTime();
        long[] cds = COOLDOWNS.computeIfAbsent(p.getUUID(), k -> new long[6]);
        if (cds[slot] > now) {
            p.sendSystemMessage(Component.literal(god.skills.get(slot) + " cooldown "
                    + String.format("%.1f", (cds[slot] - now) / 20.0) + " detik."), true);
            return;
        }

        boolean cast = god == God.RA ? RaSkills.cast(p, slot) : ThorSkills.cast(p, slot);
        if (!cast) return;

        int ticks = (int) Math.round(ModConfig.skill(god, slot).cooldown * 20);
        cds[slot] = now + ticks;
        ServerPlayNetworking.send(p, new CooldownPayload(slot, ticks));
    }

    private SkillService() {}
}
