package dev.lapergods.kontrakdewa;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Skill 1-3 Ra. Mengembalikan true kalau skill benar-benar terpakai (cooldown jalan). Damage dikali harga siang/malam. */
final class RaSkills {
    static boolean cast(ServerPlayer p, int slot) {
        return switch (slot) {
            case 0 -> sunspear(p);
            case 1 -> noonBeam(p);
            case 2 -> mirageStep(p);
            case 3 -> solarFlare(p);
            case 4 -> scarabGuard(p);
            case 5 -> callTheSun(p);
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


    /** Solar Flare: ledakan cahaya di sekeliling pemakai. Membakar dan membutakan semua makhluk di radius. */
    private static boolean solarFlare(ServerPlayer p) {
        ServerLevel level = SkillUtil.level(p);
        ModConfig.SkillCfg cfg = ModConfig.skill(God.RA, 3);
        double r = cfg.range;
        level.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 120, r / 2, 0.6, r / 2, 0.12);
        level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 80, r / 2, 0.8, r / 2, 0.15);
        ring(level, ParticleTypes.FLAME, p.position().add(0, 0.2, 0), r, 48);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.5f, 0.6f);

        boolean hitAny = false;
        AABB box = p.getBoundingBox().inflate(r);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive() && !e.isSpectator())) {
            if (e.distanceTo(p) > r) continue;
            SkillUtil.hurt(p, e, cfg.damage * BattleService.damageMultiplier(p));
            e.igniteForSeconds(5);
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
            hitAny = true;
        }
        if (hitAny) Faith.addBattleXp(p, ModConfig.get().skillHitXp);
        return true;
    }

    /** Scarab Guard: perisai emas (Absorption) + Resistance sebentar. */
    private static boolean scarabGuard(ServerPlayer p) {
        ServerLevel level = SkillUtil.level(p);
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 4));
        p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 0));
        ring(level, ParticleTypes.END_ROD, p.position().add(0, 1.0, 0), 1.3, 24);
        ring(level, ParticleTypes.FLAME, p.position().add(0, 0.3, 0), 1.0, 20);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 1.3f);
        return true;
    }

    /** Call the Sun: tandai lingkaran di titik bidikan, 2 detik kemudian kolom cahaya menghantam. */
    private static boolean callTheSun(ServerPlayer p) {
        ServerLevel level = SkillUtil.level(p);
        ModConfig.SkillCfg cfg = ModConfig.skill(God.RA, 5);
        double dist = SkillUtil.blockDistance(p, cfg.range);
        Vec3 target = p.getEyePosition().add(p.getLookAngle().scale(dist));
        final double radius = 7.0;
        // jatuhkan titik ke permukaan tanah di bawah titik bidikan
        BlockPos bp = BlockPos.containing(target);
        for (int i = 0; i < 8 && level.getBlockState(bp.below()).isAir(); i++) bp = bp.below();
        final Vec3 center = new Vec3(bp.getX() + 0.5, bp.getY(), bp.getZ() + 0.5);
        final double mult = BattleService.damageMultiplier(p);
        final var server = level.getServer();

        level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0f, 0.5f);
        // peringatan: lingkaran + kolom tipis naik selama 2 detik
        Scheduler.repeat(server, 4, 9, i -> {
            ring(level, ParticleTypes.END_ROD, center.add(0, 0.2, 0), radius, 40);
            ring(level, ParticleTypes.FLAME, center.add(0, 0.2, 0), radius * (0.3 + 0.07 * i), 24);
            for (int h = 0; h < 4 + i * 3; h++) level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + h, center.z, 1, 0.1, 0, 0.1, 0);
        });
        Scheduler.later(server, 40, () -> {
            for (int h = 0; h < 40; h++) {
                level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + h, center.z, 4, 0.5, 0, 0.5, 0.02);
                level.sendParticles(ParticleTypes.FLAME, center.x, center.y + h, center.z, 3, 0.8, 0, 0.8, 0.05);
            }
            level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 1, center.z, 150, radius / 2, 0.6, radius / 2, 0.15);
            level.playSound(null, center.x, center.y, center.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0f, 0.7f);
            level.playSound(null, center.x, center.y, center.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 2.0f, 0.5f);
            boolean hitAny = false;
            AABB box = new AABB(center, center).inflate(radius, 12, radius);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive() && !e.isSpectator())) {
                if (Math.hypot(e.getX() - center.x, e.getZ() - center.z) > radius) continue;
                SkillUtil.hurt(p, e, cfg.damage * mult);
                e.igniteForSeconds(8);
                hitAny = true;
            }
            for (int k = 0; k < 8; k++) {
                double a = level.getRandom().nextDouble() * Math.PI * 2, d = level.getRandom().nextDouble() * radius;
                SkillUtil.igniteAt(level, center.add(Math.cos(a) * d, 0.5, Math.sin(a) * d), new Vec3(0, 1, 0));
            }
            if (hitAny && p.isAlive()) Faith.addBattleXp(p, ModConfig.get().skillHitXp);
        });
        return true;
    }

    static void ring(ServerLevel level, net.minecraft.core.particles.ParticleOptions type, Vec3 c, double r, int n) {
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            level.sendParticles(type, c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    private RaSkills() {}
}
