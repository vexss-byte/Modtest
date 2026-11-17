package dev.lapergods.kontrakdewa.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Kamera sinematik: mengitari titik pusat (patung) lalu berhenti. Dipakai CameraMixin untuk menimpa posisi/arah
 * kamera game saat cutscene aktif, dan oleh efek konstelasi untuk memproyeksikan titik 3D ke layar.
 */
public final class CutsceneCamera {
    // ---- penyetelan ----
    public static final double ORBIT_SECONDS = 3.2;
    private static final double SWEEP_DEGREES = 70.0;   // seberapa jauh kamera berputar mengitari patung
    private static final double RADIUS = 11.0;          // jarak kamera dari pusat patung (blok)
    private static final double HEIGHT_START = 2.6;     // tinggi kamera di atas pusat patung saat mulai
    private static final double HEIGHT_END = 0.4;       // dan saat selesai berputar

    public record Pose(Vec3 pos, float yaw, float pitch) {}

    private static boolean active = false;
    private static long startNanos;
    private static Vec3 center = Vec3.ZERO;
    private static double startAngle;      // radian, sudut kamera terhadap pusat saat mulai
    private static float yawOffset = 0f;   // geser arah pandang ke kanan supaya patung berada di sisi kiri layar

    private CutsceneCamera() {}

    public static void begin(Vec3 statueCenter, Vec3 playerPos, float framingYawOffsetDeg) {
        center = statueCenter;
        // Mulai dari belakang pemain (arah dari patung ke pemain) supaya pemain terlihat di depan kamera.
        startAngle = Math.atan2(playerPos.z - statueCenter.z, playerPos.x - statueCenter.x);
        yawOffset = framingYawOffsetDeg;
        startNanos = System.nanoTime();
        active = true;
    }

    public static void end() {
        active = false;
    }

    public static boolean isActive() {
        return active;
    }

    public static double elapsed() {
        return (System.nanoTime() - startNanos) / 1_000_000_000.0;
    }

    /** Lompat ke detik tertentu (dipakai tombol skip). */
    public static void skipTo(double seconds) {
        if (elapsed() < seconds) startNanos = System.nanoTime() - (long) (seconds * 1_000_000_000.0);
    }

    private static double ease(double x) {
        double c = Math.max(0, Math.min(1, x));
        return c * c * (3 - 2 * c); // smoothstep
    }

    private static Vec3 idealPosition(double progress) {
        double e = ease(progress);
        double a = startAngle + Math.toRadians(SWEEP_DEGREES) * e;
        double h = HEIGHT_START + (HEIGHT_END - HEIGHT_START) * e;
        return new Vec3(center.x + Math.cos(a) * RADIUS, center.y + h, center.z + Math.sin(a) * RADIUS);
    }

    /** Posisi akhir kamera (tanpa tabrakan) — dipakai untuk menghadapkan bidang konstelasi ke kamera. */
    public static Vec3 finalCameraPosition() {
        return idealPosition(1.0);
    }

    public static Pose pose() {
        if (!active) return null;
        Minecraft mc = Minecraft.getInstance();
        Vec3 ideal = idealPosition(elapsed() / ORBIT_SECONDS);
        Vec3 pos = ideal;

        // Jangan tembus blok: potong jarak kalau ada penghalang antara pusat dan posisi ideal.
        if (mc.level != null && mc.player != null) {
            Vec3 from = center.add(0, 1.0, 0);
            BlockHitResult hit = mc.level.clip(new ClipContext(from, ideal, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mc.player));
            if (hit.getType() != HitResult.Type.MISS) {
                Vec3 dir = ideal.subtract(from).normalize();
                pos = hit.getLocation().subtract(dir.scale(0.5));
            }
        }

        Vec3 target = center;
        double dx = target.x - pos.x, dy = target.y - pos.y, dz = target.z - pos.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f + yawOffset;
        float pitch = (float) -(Math.atan2(dy, horiz) * 180.0 / Math.PI);
        return new Pose(pos, yaw, pitch);
    }

    /**
     * Proyeksikan titik dunia ke koordinat layar GUI. Mengembalikan null kalau titik berada di belakang kamera.
     */
    public static float[] project(Vec3 world, int guiWidth, int guiHeight) {
        Camera cam = Minecraft.getInstance().gameRenderer.mainCamera();
        Vec3 cp = cam.position();
        Matrix4f m = cam.getViewRotationProjectionMatrix(new Matrix4f());
        Vector4f v = new Vector4f((float) (world.x - cp.x), (float) (world.y - cp.y), (float) (world.z - cp.z), 1.0f);
        m.transform(v);
        if (v.w <= 0.05f) return null;
        float nx = v.x / v.w, ny = v.y / v.w;
        return new float[]{(nx * 0.5f + 0.5f) * guiWidth, (0.5f - ny * 0.5f) * guiHeight};
    }
}
