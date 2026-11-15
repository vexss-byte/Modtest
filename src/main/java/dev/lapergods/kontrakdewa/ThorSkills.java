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
import net.minecraft.world.phys.AABB;
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

    private ThorSkills() {}
}
