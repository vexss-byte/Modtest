package dev.lapergods.kontrakdewa;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Skill 1-3 Thor. Petir dibuat visual-only, damage dihitung manual supaya tidak mengenai pemakai. */
final class ThorSkills {
    static boolean cast(ServerPlayer p, int slot) {
        return switch (slot) {
            case 0 -> thunderStrike(p);
            case 1 -> chainLightning(p);
            case 2 -> hammerThrow(p);
            case 3 -> stormLeap(p);
            case 4 -> staticField(p);
            case 5 -> overcharge(p);
            case 6 -> ragnarokStorm(p);
            default -> false;
        };
    }

    private static void bolt(ServerLevel level, Vec3 pos) {
        LightningBolt b = new LightningBolt(EntityTypes.LIGHTNING_BOLT, level);
        b.setPos(pos);
        b.setVisualOnly(true);
        level.addFreshEntity(b);
    }

    /** Thunder Strike: sambaran petir ke makhluk di bidikan (atau titik tabrak), membakar. */
    private static boolean thunderStrike(ServerPlayer p) {
        ServerLevel level = SkillUtil.level(p);
        ModConfig.SkillCfg cfg = ModConfig.skill(God.THOR, 0);
        double dist = SkillUtil.blockDistance(p, cfg.range);
        List<LivingEntity> hits = SkillUtil.along(p, dist, 1.0);
        Vec3 dir = p.getLookAngle();
        Vec3 impact;
        if (!hits.isEmpty()) {
            LivingEntity t = hits.get(0);
            impact = t.position();
            bolt(level, impact);
            SkillUtil.hurt(p, t, cfg.damage);
            t.igniteForSeconds(3);
            Faith.addBattleXp(p, ModConfig.get().skillHitXp);
        } else {
            impact = p.getEyePosition().add(dir.scale(dist));
            bolt(level, impact);
            SkillUtil.igniteAt(level, impact, dir);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, impact.x, impact.y + 0.5, impact.z, 40, 0.6, 0.8, 0.6, 0.3);
        level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.2f, 1.2f);
        return true;
    }

    /** Chain Lightning: loncat dari target pertama ke makhluk terdekat berikutnya (maks 4 loncatan), damage turun 20% tiap loncatan. */
    private static boolean chainLightning(ServerPlayer p) {
        ServerLevel level = SkillUtil.level(p);
        ModConfig.SkillCfg cfg = ModConfig.skill(God.THOR, 1);
        double dist = SkillUtil.blockDistance(p, cfg.range);
        List<LivingEntity> hits = SkillUtil.along(p, dist, 1.2);
        if (hits.isEmpty()) {
            p.sendSystemMessage(Component.literal("Chain Lightning: tidak ada target."), true);
            return false;
        }
        LivingEntity current = hits.get(0);
        Set<LivingEntity> visited = new HashSet<>();
        Vec3 prev = p.getEyePosition().add(0, -0.25, 0);
        double dmg = cfg.damage;
        for (int jump = 0; jump < 5 && current != null; jump++) {
            visited.add(current);
            Vec3 c = current.getBoundingBox().getCenter();
            SkillUtil.particleLine(level, ParticleTypes.ELECTRIC_SPARK, prev, c, 0.35);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, 12, 0.3, 0.4, 0.3, 0.2);
            SkillUtil.hurt(p, current, dmg);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.5f, 1.8f);
            dmg *= 0.8;
            prev = c;

            LivingEntity next = null;
            double best = Double.MAX_VALUE;
            AABB box = current.getBoundingBox().inflate(7);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive() && !e.isSpectator() && !visited.contains(e))) {
                double d = e.getBoundingBox().getCenter().distanceTo(c);
                if (d < best && d <= 7) { best = d; next = e; }
            }
            current = next;
        }
        Faith.addBattleXp(p, ModConfig.get().skillHitXp);
        return true;
    }

    /** Hammer Throw (versi instan): hantaman di titik bidikan, damage + ledakan petir kecil di sekitarnya (dorongan dari damage serangan). */
    private static boolean hammerThrow(ServerPlayer p) {
        ServerLevel level = SkillUtil.level(p);
        ModConfig.SkillCfg cfg = ModConfig.skill(God.THOR, 2);
        double dist = SkillUtil.blockDistance(p, cfg.range);
        List<LivingEntity> hits = SkillUtil.along(p, dist, 1.0);
        Vec3 from = p.getEyePosition();
        Vec3 dir = p.getLookAngle();
        LivingEntity direct = hits.isEmpty() ? null : hits.get(0);
        if (direct != null) dist = Math.min(dist, direct.getBoundingBox().getCenter().distanceTo(from));
        Vec3 impact = from.add(dir.scale(dist));

        SkillUtil.particleLine(level, ParticleTypes.ELECTRIC_SPARK, from.add(0, -0.25, 0).add(dir), impact, 0.5);
        bolt(level, impact);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, impact.x, impact.y + 0.3, impact.z, 80, 1.2, 0.5, 1.2, 0.4);
        level.sendParticles(ParticleTypes.CLOUD, impact.x, impact.y + 0.2, impact.z, 25, 1.0, 0.2, 1.0, 0.1);
        level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.5f, 0.8f);

        boolean hitAny = false;
        AABB box = new AABB(impact, impact).inflate(2.5);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive() && !e.isSpectator())) {
            SkillUtil.hurt(p, e, e == direct ? cfg.damage : cfg.damage * 0.5);
            hitAny = true;
        }
        if (hitAny) Faith.addBattleXp(p, ModConfig.get().skillHitXp);
        return true;
    }


    private static void shock(ServerLevel level, Vec3 c, double r, double damage, ServerPlayer owner) {
        AABB box = new AABB(c, c).inflate(r, 3, r);
        boolean hit = false;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != owner && e.isAlive() && !e.isSpectator())) {
            if (Math.hypot(e.getX() - c.x, e.getZ() - c.z) > r) continue;
            SkillUtil.hurt(owner, e, damage);
            hit = true;
        }
        if (hit && owner.isAlive()) Faith.addBattleXp(owner, ModConfig.get().skillHitXp);
    }

    /** Storm Leap: melompat tinggi ke depan, sedetik kemudian menghantam tanah dengan gelombang petir. */
    private static boolean stormLeap(ServerPlayer p) {
        ServerLevel level = SkillUtil.level(p);
        ModConfig.SkillCfg cfg = ModConfig.skill(God.THOR, 3);
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1e-4 ? Vec3.ZERO : flat.normalize();

        Vec3 delta = null;
        for (double up = 6; up >= 2 && delta == null; up -= 1) {
            for (double fwd = 5; fwd >= 0; fwd -= 2.5) {
                Vec3 d = new Vec3(flat.x * fwd, up, flat.z * fwd);
                if (level.noCollision(p, p.getBoundingBox().move(d))) { delta = d; break; }
            }
        }
        if (delta == null) {
            p.sendSystemMessage(Component.literal("Storm Leap terhalang."), true);
            return false;
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 0.5, p.getZ(), 40, 0.5, 0.5, 0.5, 0.3);
        p.teleportTo(p.getX() + delta.x, p.getY() + delta.y, p.getZ() + delta.z);
        p.resetFallDistance();
        p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 4)); // anti damage jatuh saat menghantam
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8f, 1.6f);

        Scheduler.later(level.getServer(), 14, () -> {
            if (!p.isAlive() || p.level() != level) return;
            Vec3 from = p.position();
            BlockHitResult r = level.clip(new ClipContext(from, from.add(0, -48, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            Vec3 ground = r.getType() == HitResult.Type.MISS ? from : r.getLocation();
            p.teleportTo(ground.x, ground.y, ground.z);
            p.resetFallDistance();
            bolt(level, ground);
            RaSkills.ring(level, ParticleTypes.ELECTRIC_SPARK, ground.add(0, 0.2, 0), cfg.range, 48);
            level.sendParticles(ParticleTypes.CLOUD, ground.x, ground.y + 0.3, ground.z, 40, cfg.range / 2, 0.2, cfg.range / 2, 0.1);
            level.playSound(null, ground.x, ground.y, ground.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.5f, 0.7f);
            shock(level, ground, cfg.range, cfg.damage, p);
        });
        return true;
    }

    /** Static Field: area listrik di tempat pemakai berdiri selama 6 detik; melukai dan memperlambat musuh di dalamnya. */
    private static boolean staticField(ServerPlayer p) {
        ServerLevel level = SkillUtil.level(p);
        ModConfig.SkillCfg cfg = ModConfig.skill(God.THOR, 4);
        final Vec3 c = p.position();
        level.playSound(null, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0f, 1.5f);
        Scheduler.repeat(level.getServer(), 10, 12, i -> {
            RaSkills.ring(level, ParticleTypes.ELECTRIC_SPARK, c.add(0, 0.15, 0), cfg.range, 36);
            for (int k = 0; k < 6; k++) {
                double a = level.random.nextDouble() * Math.PI * 2, d = level.random.nextDouble() * cfg.range;
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x + Math.cos(a) * d, c.y + 0.3, c.z + Math.sin(a) * d, 2, 0.1, 0.4, 0.1, 0.1);
            }
            AABB box = new AABB(c, c).inflate(cfg.range, 3, cfg.range);
            boolean hit = false;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive() && !e.isSpectator())) {
                if (Math.hypot(e.getX() - c.x, e.getZ() - c.z) > cfg.range) continue;
                SkillUtil.hurt(p, e, cfg.damage);
                e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 1));
                hit = true;
            }
            if (hit && i == 0 && p.isAlive()) Faith.addBattleXp(p, ModConfig.get().skillHitXp);
        });
        return true;
    }

    /** Overcharge: 5 stack "Charged"; tiap pukulan senjata menghabiskan 1 stack dan menambah damage petir (lihat ChargeService). */
    private static boolean overcharge(ServerPlayer p) {
        ChargeService.charge(p, 5);
        ServerLevel level = SkillUtil.level(p);
        RaSkills.ring(level, ParticleTypes.ELECTRIC_SPARK, p.position().add(0, 1.0, 0), 1.0, 20);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 0.8, 0.4, 0.3);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0f, 1.2f);
        return true;
    }

    /** Ragnarok Storm (Special Magic, Faith 10): badai petir di titik bidikan selama 8 detik. */
    private static boolean ragnarokStorm(ServerPlayer p) {
        ServerLevel level = SkillUtil.level(p);
        ModConfig.SkillCfg cfg = ModConfig.skill(God.THOR, 6);
        double dist = SkillUtil.blockDistance(p, 24);
        final Vec3 c = p.getEyePosition().add(p.getLookAngle().scale(dist));
        level.playSound(null, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.0f, 0.5f);
        Scheduler.repeat(level.getServer(), 6, 27, i -> {
            if (i % 2 == 0) RaSkills.ring(level, ParticleTypes.ELECTRIC_SPARK, c.add(0, 0.2, 0), cfg.range, 60);
            double a = level.random.nextDouble() * Math.PI * 2, d = Math.sqrt(level.random.nextDouble()) * cfg.range;
            Vec3 strike = new Vec3(c.x + Math.cos(a) * d, c.y, c.z + Math.sin(a) * d);
            bolt(level, strike);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, strike.x, strike.y + 0.5, strike.z, 25, 0.5, 0.6, 0.5, 0.3);
            if (i % 3 == 0) SkillUtil.igniteAt(level, strike.add(0, 0.5, 0), new Vec3(0, 1, 0));
            shock(level, strike, 2.5, cfg.damage, p);
        });
        return true;
    }

    private ThorSkills() {}
}
