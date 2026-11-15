package dev.lapergods.kontrakdewa;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Alat bantu skill: raycast, daftar target di sepanjang garis, partikel, api. */
final class SkillUtil {
    static ServerLevel level(ServerPlayer p) {
        return (ServerLevel) p.level();
    }

    /** Jarak dari mata pemain ke blok padat pertama di arah pandang (maksimal max). */
    static double blockDistance(ServerPlayer p, double max) {
        Vec3 from = p.getEyePosition();
        Vec3 to = from.add(p.getLookAngle().scale(max));
        BlockHitResult r = level(p).clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return r.getType() == HitResult.Type.MISS ? max : from.distanceTo(r.getLocation());
    }

    /** Makhluk hidup di sepanjang garis pandang sejauh dist, diurutkan dari yang terdekat. */
    static List<LivingEntity> along(ServerPlayer p, double dist, double radius) {
        Vec3 from = p.getEyePosition();
        Vec3 dir = p.getLookAngle();
        AABB box = new AABB(from, from.add(dir.scale(dist))).inflate(radius + 1.0);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : level(p).getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive() && !e.isSpectator())) {
            Vec3 c = e.getBoundingBox().getCenter();
            double t = c.subtract(from).dot(dir);
            if (t < 0 || t > dist) continue;
            if (c.distanceTo(from.add(dir.scale(t))) <= radius + e.getBbWidth() / 2.0) out.add(e);
        }
        out.sort(Comparator.comparingDouble(e -> e.getBoundingBox().getCenter().distanceTo(from)));
        return out;
    }

    /** Luka magis-fisik dengan kredit pembunuhan ke pemain. */
    static boolean hurt(ServerPlayer p, LivingEntity target, double amount) {
        ServerLevel level = level(p);
        return target.hurtServer(level, level.damageSources().playerAttack(p), (float) amount);
    }

    static void particleLine(ServerLevel level, ParticleOptions type, Vec3 from, Vec3 to, double step) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 1e-4) return;
        Vec3 n = d.scale(1.0 / len);
        for (double t = 0; t <= len; t += step) {
            Vec3 pt = from.add(n.scale(t));
            level.sendParticles(type, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
        }
    }

    /** Nyalakan api di titik tabrak (kalau config mengizinkan dan blok tempatnya kosong). */
    static void igniteAt(ServerLevel level, Vec3 hit, Vec3 dir) {
        if (!ModConfig.get().allowBlockDamage) return;
        BlockPos pos = BlockPos.containing(hit.subtract(dir.scale(0.1)));
        BlockState fire = Blocks.FIRE.defaultBlockState();
        if (level.getBlockState(pos).isAir() && fire.canSurvive(level, pos)) level.setBlockAndUpdate(pos, fire);
    }

    private SkillUtil() {}
}
