package dev.lapergods.kontrakdewa.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Kamera sinematik di DEPAN patung: mulai dekat dan rendah di kaki patung, lalu mundur sambil mengayun sedikit
 * sampai berhenti tepat menghadap bagian depan patung. Dipakai CameraMixin untuk menimpa kamera game, dan oleh
 * efek konstelasi untuk memproyeksikan titik 3D ke layar.
 */
public final class CutsceneCamera {
    // ---- penyetelan ----
    public static final double ORBIT_SECONDS = 3.6;
    private static final double START_FRONT = 2.8;     // jarak awal dari patung (blok), di depan
    private static final double START_SIDE = -1.8;     // geser awal ke kiri kamera
    private static final double START_HEIGHT = 0.8;    // tinggi awal dari dasar patung
    private static final double FINAL_FRONT = 7.5;    // jarak akhir di depan patung
    private static final double FINAL_HEIGHT = 1.3;    // tinggi akhir
    private static final double ARC = 2.0;             // ayunan samping di tengah gerakan
    /** Kalau ternyata depan model terbalik di game, ubah ke true (memutar arah "depan" 180 derajat). */
    public static final boolean FLIP_FRONT = false;

    public record Pose(Vec3 pos, float yaw, float pitch) {}

    private static boolean active = false;
    private static long startNanos;
    private static Vec3 center = Vec3.ZERO;   // pusat patung (setinggi dada)
    private static Vec3 front = new Vec3(0, 0, -1); // vektor datar dari patung ke arah depan
    private static float yawOffset = 0f;

    private CutsceneCamera() {}

    public static void begin(Vec3 statueCenter, Direction facing, float framingYawOffsetDeg) {
        center = statueCenter;
        Direction f = FLIP_FRONT ? facing.getOpposite() : facing;
        front = new Vec3(f.getStepX(), 0, f.getStepZ());
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

    /** Lompat ke detik tertentu (tombol skip). */
    public static void skipTo(double seconds) {
        if (elapsed() < seconds) startNanos = System.nanoTime() - (long) (seconds * 1_000_000_000.0);
    }

    /** Vektor datar dari patung ke arah depan (arah kamera akhir). */
    public static Vec3 front() {
        return front;
    }

    /** Arah "kanan" layar pada tampilan akhir (kamera menghadap patung dari depan). */
    public static Vec3 right() {
        return new Vec3(front.z, 0, -front.x);
    }

    private static double ease(double x) {
        double c = Math.max(0, Math.min(1, x));
        return c * c * (3 - 2 * c);
    }

    private static Vec3 base() {
        return center.subtract(0, 3.0, 0);
    }

    /** Dorongan pelan maju setelah kamera berhenti (blok, maksimum). */
    private static final double PUSH_IN = 0.9;

    private static Vec3 idealPosition(double e) {
        Vec3 right = right();
        Vec3 base = base();
        Vec3 start = base.add(front.scale(START_FRONT)).add(right.scale(START_SIDE)).add(0, START_HEIGHT, 0);
        Vec3 end = base.add(front.scale(FINAL_FRONT)).add(0, FINAL_HEIGHT, 0);
        Vec3 p = start.add(end.subtract(start).scale(Math.min(1.0, e)));
        if (e > 1.0) p = p.subtract(front.scale(Math.min(PUSH_IN, (e - 1.0) * PUSH_IN)));
        return p.add(right.scale(ARC * Math.sin(Math.PI * Math.min(1.0, e))));
    }

    public static Pose pose() {
        if (!active) return null;
        Minecraft mc = Minecraft.getInstance();
        double raw = elapsed() / ORBIT_SECONDS;
        double e = ease(Math.min(1.0, raw));
        Vec3 ideal = idealPosition(raw > 1.0 ? 1.0 + Math.min(1.0, (raw - 1.0) / 1.5) : e);
        Vec3 pos = ideal;

        // Jangan tembus blok: potong jarak kalau ada penghalang antara patung dan posisi ideal.
        if (mc.level != null && mc.player != null) {
            Vec3 from = center.add(0, 0.5, 0);
            BlockHitResult hit = mc.level.clip(new ClipContext(from, ideal, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mc.player));
            if (hit.getType() != HitResult.Type.MISS) {
                Vec3 dir = ideal.subtract(from).normalize();
                pos = hit.getLocation().subtract(dir.scale(0.5));
            }
        }

        // titik bidik naik dari kaki ke dada patung selama kamera mundur
        Vec3 target = base().add(0, 1.6, 0).add(center.add(0, 0.5, 0).subtract(base().add(0, 1.6, 0)).scale(e));
        double dx = target.x - pos.x, dy = target.y - pos.y, dz = target.z - pos.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f + (float) (yawOffset * e);
        float pitch = (float) -(Math.atan2(dy, horiz) * 180.0 / Math.PI);
        return new Pose(pos, yaw, pitch);
    }

    /** Proyeksikan titik dunia ke koordinat layar GUI. Mengembalikan null kalau titik di belakang kamera. */
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
