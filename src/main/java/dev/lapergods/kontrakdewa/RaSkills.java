package dev.lapergods.kontrakdewa;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Skill 1-3 Ra. Mengembalikan true kalau skill benar-benar terpakai (cooldown jalan). Damage dikali harga siang/malam. */
final class RaSkills {
    static boolean cast(ServerPlayer p, int slot) {
        return switch (slot) {
            case 0 -> sunspear(p);
            case 1 -> noonBeam(p);
            case 2 -> mirageStep(p);
            default -> false;
        };
    }

    /** Sunspear: tombak cahaya lurus, kena target pertama, membakar. */
    private static boolean sunspear(ServerPlayer p) {
        ServerLevel level = SkillUtil.level(p);
        ModConfig.SkillCfg cfg = ModConfig.skill(God.RA, 0);
        Vec3 from = p.getEyePosition().add(0, -0.25, 0);
        Vec3 dir = p.getLookAngle();
        double dist = SkillUtil.blockDistance(p, cfg.range);
        List<LivingEntity> hits = SkillUtil.along(p, dist, 0.8);
        LivingEntity target = hits.isEmpty() ? null : hits.get(0);
        if (target != null) dist = Math.min(dist, target.getBoundingBox().getCenter().distanceTo(p.getEyePosition()));
        Vec3 end = p.getEyePosition().add(dir.scale(dist));

        SkillUtil.particleLine(level, ParticleTypes.END_ROD, from.add(dir), end, 0.5);
        SkillUtil.particleLine(level, ParticleTypes.FLAME, from.add(dir), end, 1.0);
        level.sendParticles(ParticleTypes.FLAME, end.x, end.y, end.z, 25, 0.4, 0.4, 0.4, 0.05);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 1.5f);

        if (target != null) {
            SkillUtil.hurt(p, target, cfg.damage * BattleService.damageMultiplier(p));
            target.igniteForSeconds(4);
            Faith.addBattleXp(p, ModConfig.get().skillHitXp);
        }
        return true;
    }

    /** Noon Beam: sinar lebar menembus semua makhluk di jalurnya, membakar, menyalakan api di titik tabrak. */
    private static boolean noonBeam(ServerPlayer p) {
        ServerLevel level = SkillUtil.level(p);
        ModConfig.SkillCfg cfg = ModConfig.skill(God.RA, 1);
        Vec3 from = p.getEyePosition().add(0, -0.25, 0);
        Vec3 dir = p.getLookAngle();
        double dist = SkillUtil.blockDistance(p, cfg.range);
        Vec3 end = p.getEyePosition().add(dir.scale(dist));

        SkillUtil.particleLine(level, ParticleTypes.END_ROD, from.add(dir), end, 0.3);
        SkillUtil.particleLine(level, ParticleTypes.FLAME, from.add(dir), end, 0.6);
        level.sendParticles(ParticleTypes.FLAME, end.x, end.y, end.z, 60, 0.8, 0.8, 0.8, 0.08);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2f, 0.7f);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.6f);

        boolean hitAny = false;
        for (LivingEntity e : SkillUtil.along(p, dist, 1.4)) {
            SkillUtil.hurt(p, e, cfg.damage * BattleService.damageMultiplier(p));
            e.igniteForSeconds(5);
            hitAny = true;
        }
        SkillUtil.igniteAt(level, end, dir);
        if (hitAny) Faith.addBattleXp(p, ModConfig.get().skillHitXp);
        return true;
    }

    /** Mirage Step: melesat ke depan (berhenti di depan tembok), meninggalkan bayangan, lalu Speed sebentar. */
    private static boolean mirageStep(ServerPlayer p) {
        ServerLevel level = SkillUtil.level(p);
        ModConfig.SkillCfg cfg = ModConfig.skill(God.RA, 2);
        Vec3 dir = p.getLookAngle();
        double max = Math.max(0, SkillUtil.blockDistance(p, cfg.range) - 0.8);

        Vec3 delta = null;
        for (double t = max; t >= 1.0; t -= 0.5) {
            Vec3 d = dir.scale(t);
            if (level.noCollision(p, p.getBoundingBox().move(d))) {
                delta = d;
                break;
            }
        }
        if (delta == null) {
            p.sendSystemMessage(Component.literal("Mirage Step terhalang."), true);
            return false;
        }

        Vec3 start = p.position().add(0, 1.0, 0);
        Vec3 end = start.add(delta);
        SkillUtil.particleLine(level, ParticleTypes.CLOUD, start, end, 0.6);
        SkillUtil.particleLine(level, ParticleTypes.END_ROD, start, end, 1.0);
        level.sendParticles(ParticleTypes.FLAME, start.x, start.y, start.z, 20, 0.3, 0.6, 0.3, 0.02);
        p.teleportTo(p.getX() + delta.x, p.getY() + delta.y, p.getZ() + delta.z);
        p.resetFallDistance();
        p.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 1));
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.5f);
        return true;
    }

    private RaSkills() {}
}
