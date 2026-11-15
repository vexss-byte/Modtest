package dev.lapergods.kontrakdewa;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Stack "Charged" Thor: tiap pukulan senjata habiskan 1 stack dan tambah damage petir. Kedaluwarsa 15 detik. */
public final class ChargeService {
    private static final Map<UUID, Long> EXPIRES = new HashMap<>(); // dalam detik server (tick/20)
    private static boolean inBonus = false;

    public static void init() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(ChargeService::afterDamage);
    }

    public static void charge(ServerPlayer p, int stacks) {
        p.setAttached(ModAttachments.CHARGED, stacks);
        EXPIRES.put(p.getUUID(), (long) p.level().getGameTime() / 20 + 15);
    }

    /** Dipanggil tiap detik: hapus stack yang kedaluwarsa. */
    public static void refresh(ServerPlayer p) {
        if (p.getAttachedOrElse(ModAttachments.CHARGED, 0) <= 0) return;
        Long exp = EXPIRES.get(p.getUUID());
        if (exp == null || p.level().getGameTime() / 20 >= exp) p.setAttached(ModAttachments.CHARGED, 0);
    }

    private static void afterDamage(LivingEntity victim, DamageSource source, float baseDamage, float damageTaken, boolean blocked) {
        if (inBonus || blocked || damageTaken <= 0) return;
        if (!(source.getDirectEntity() instanceof ServerPlayer p) || p == victim) return;
        int stacks = p.getAttachedOrElse(ModAttachments.CHARGED, 0);
        if (stacks <= 0 || BattleService.godOf(p) != God.THOR || !WeaponService.holding(p, God.THOR)) return;
        if (!(victim.level() instanceof ServerLevel level)) return;

        p.setAttached(ModAttachments.CHARGED, stacks - 1);
        inBonus = true;
        try {
            victim.hurtServer(level, level.damageSources().lightningBolt(), (float) ModConfig.skill(God.THOR, 5).damage);
        } finally {
            inBonus = false;
        }
        var c = victim.getBoundingBox().getCenter();
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, 30, 0.4, 0.5, 0.4, 0.3);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8f, 1.6f);
    }

    private ChargeService() {}
}
