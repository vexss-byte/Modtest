package dev.lapergods.kontrakdewa;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;

/** Hukuman dewa karena meninggalkan kontrak: Faith -50%, weapon lama hancur, Weakness + Slowness 30 menit, murka dewa lama, terkunci 24 jam. */
public final class Punishment {
    public static final long LOCK_MS = 24L * 60 * 60 * 1000;
    public static final int DEBUFF_TICKS = 30 * 60 * 20;

    public static void apply(ServerPlayer p, God oldGod, God newGod) {
        ServerLevel level = (ServerLevel) p.level();

        p.setAttached(ModAttachments.BATTLE_MODE, false);
        p.setAttached(ModAttachments.CHARGED, 0);
        BattleService.refresh(p);
        WeaponService.removeAll(p, oldGod);
        p.setAttached(ModAttachments.FAITH_XP, Faith.xp(p) / 2);
        p.setAttached(ModAttachments.LOCK_UNTIL, System.currentTimeMillis() + LOCK_MS);

        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, DEBUFF_TICKS, 0));
        p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, DEBUFF_TICKS, 0));

        // Murka dewa lama (tidak sampai membunuh)
        if (oldGod == God.THOR) {
            LightningBolt b = new LightningBolt(EntityTypes.LIGHTNING_BOLT, level);
            b.setPos(p.position());
            b.setVisualOnly(true);
            level.addFreshEntity(b);
            float dmg = Math.min(8.0f, p.getHealth() - 1.0f);
            if (dmg > 0) p.hurtServer(level, level.damageSources().lightningBolt(), dmg);
        } else {
            level.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 60, 0.5, 0.8, 0.5, 0.1);
            if (p.getHealth() > 6.0f) p.igniteForSeconds(5);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.5f, 0.5f);
        }

        p.setAttached(ModAttachments.CONTRACT, newGod.id);

        p.sendSystemMessage(Component.literal("Murka " + oldGod.displayName + " menimpamu! Faith turun 50%, "
                + "Weakness + Slowness 30 menit. Kontrak baru dengan " + newGod.displayName + " aktif, "
                + "dan kamu tidak bisa berganti kontrak lagi selama 24 jam."));
        KontrakDewa.LOGGER.info("{} meninggalkan {} dan berkontrak dengan {}", p.getName().getString(), oldGod.displayName, newGod.displayName);
    }

    private Punishment() {}
}
