package dev.lapergods.kontrakdewa.client;

import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Kamera sinematik 8 detik untuk interaksi patung. SATU jam (elapsed()) dipakai oleh kamera, efek, konstelasi, dan panel.
 *
 * Jalur kamera (dalam bingkai patung: f = maju dari depan patung, r = ke kanan, h = tinggi dari dasar):
 *   0.0-1.5  pembuka: patung utuh dari depan-kiri, dolly masuk pelan
 *   1.5-3.5  orbit halus ke depan-kanan sambil naik sedikit
 *   3.5-5.0  shot lebar: mundur dan naik (crane), lalu turun menetap di depan patung
 *   5.0-8.0  tahan, dorongan pelan maju; komposisi memberi ruang untuk sosok konstelasi di kiri
 * Jalur dibuat dengan spline Hermite (kecepatan mulus, tanpa berhenti-jalan di tiap titik).
 */
public final class CutsceneCamera {
    public static final double TOTAL_SECONDS = 8.0;

    /** Kalau ternyata depan model terbalik di game, ubah ke true (memutar arah "depan" 180 derajat). */
    public static final boolean FLIP_FRONT = false;

    // t, posisi kamera (f, r, h), titik bidik (f, r, h)
    private static final double[][] KEYS = {
            {0.0, 9.5, -4.0, 2.0, 0, 0, 3.2},
            {1.5, 8.6, -3.0, 2.3, 0, 0, 3.5},
            {2.6, 9.6, 1.5, 3.2, 0, 0, 3.6},
            {3.5, 11.5, 2.0, 4.6, 0, 0, 3.2},
            {4.3, 14.0, 0.5, 6.0, 0, 0, 2.8},
            {5.0, 13.2, 0.0, 3.4, 0, 0, 3.0},
            {6.8, 12.4, 0.0, 2.3, 0, 0, 3.0},
            {8.0, 11.8, 0.0, 2.0, 0, 0, 3.0},
    };
    private static final double BLEND_FROM_PLAYER = 0.6;   // detik: peralihan mulus dari kamera pemain
    private static final double COMPOSE_START = 3.5, COMPOSE_END = 5.1;

    public record Pose(Vec3 pos, float yaw, float pitch) {}

    private static boolean active = false;
    private static long startNanos;
    private static Vec3 center = Vec3.ZERO;           // pusat patung (setinggi dada)
    private static Vec3 front = new Vec3(0, 0, -1);
    private static Vec3 playerEye = Vec3.ZERO;
    private static float playerYaw, playerPitch;
    private static float yawOffset, pitchOffset;      // komposisi akhir: patung berada di kanan-bawah area kiri panel
    private static double tanV = 0.7, tanH = 1.2;

    // posisi sosok raksasa (dihitung dari pose akhir)
    private static Vec3 giantAnchor = Vec3.ZERO;
    private static double giantScale = 2.4;

    // keadaan yang harus dikembalikan
    private static boolean hudWasHidden;
    private static CameraType oldCameraType;
    private static boolean changedUi = false;

    private CutsceneCamera() {}

    /**
     * @param freeFrac lebar area kosong di kiri panel dibanding lebar layar (0..1)
     */
    public static void begin(Vec3 statueCenter, Direction facing, Vec3 eye, float yaw, float pitch,
                             double fovDeg, double aspect, double freeFrac) {
        Minecraft mc = Minecraft.getInstance();
        center = statueCenter;
        Direction f = FLIP_FRONT ? facing.getOpposite() : facing;
        front = new Vec3(f.getStepX(), 0, f.getStepZ());
        playerEye = eye;
        playerYaw = yaw;
        playerPitch = pitch;
        tanV = Math.tan(Math.toRadians(fovDeg) / 2.0);
        tanH = tanV * aspect;

        // ---- komposisi akhir ----
        double sxS = 0.80 * freeFrac, syS = 0.70;       // letak patung di layar
        double sxG = 0.30 * freeFrac, syG = 1.04;       // letak kaki sosok raksasa
        Vec3 finalPos = positionAt(TOTAL_SECONDS);
        Vec3 look = lookAt(TOTAL_SECONDS);
        double[] base = angles(finalPos, look);
        yawOffset = (float) Math.toDegrees(Math.atan(((0.5 - sxS) / 0.5) * tanH));
        pitchOffset = (float) -Math.toDegrees(Math.atan(((syS - 0.5) / 0.5) * tanV));

        float fy = (float) base[0] + yawOffset, fp = (float) base[1] + pitchOffset;
        Vec3[] axes = axes(fy, fp);                      // forward, right, up
        double ds = finalPos.distanceTo(center);
        double dg = ds + 2.8;
        double xo = ((sxG - 0.5) * 2) * tanH * dg;
        double yo = -((syG - 0.5) * 2) * tanV * dg;
        giantAnchor = finalPos.add(axes[0].scale(dg)).add(axes[1].scale(xo)).add(axes[2].scale(yo));
        giantScale = (2 * dg * tanV * 1.08) / 8.4;

        // ---- UI: sembunyikan HUD dan pakai kamera orang ketiga supaya model pemain terlihat ----
        hudWasHidden = mc.gui.hud.isHidden();
        if (!hudWasHidden) mc.gui.hud.toggle();
        oldCameraType = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        changedUi = true;

        startNanos = System.nanoTime();
        active = true;
    }

    /** Hentikan cutscene dan kembalikan semua yang diubah (dipanggil saat layar ditutup, pemain keluar, atau error). */
    public static void stop() {
        active = false;
        if (!changedUi) return;
        changedUi = false;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (!hudWasHidden && mc.gui.hud.isHidden()) mc.gui.hud.toggle();
            if (oldCameraType != null) mc.options.setCameraType(oldCameraType);
        } catch (Throwable ignored) {
            // pembersihan tidak boleh melempar error
        }
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

    public static Vec3 front() {
        return front;
    }

    /** Arah "kanan" layar pada tampilan akhir (kamera menghadap patung dari depan). */
    public static Vec3 right() {
        return new Vec3(front.z, 0, -front.x);
    }

    public static Vec3 center() {
        return center;
    }

    public static Vec3 base() {
        return center.subtract(0, 3.0, 0);
    }

    public static Vec3 giantAnchor() {
        return giantAnchor;
    }

    public static double giantScale() {
        return giantScale;
    }

    // ------------------------------------------------------------ jalur kamera

    private static Vec3 world(double f, double r, double h) {
        return base().add(front.scale(f)).add(right().scale(r)).add(0, h, 0);
    }

    private static double[] keyValues(int i, boolean look) {
        double[] k = KEYS[i];
        return look ? new double[]{k[4], k[5], k[6]} : new double[]{k[1], k[2], k[3]};
    }

    /** Spline Hermite (tangen selisih-terpusat) pada komponen posisi atau titik bidik. */
    private static double[] spline(double t, boolean look) {
        int n = KEYS.length;
        if (t <= KEYS[0][0]) return keyValues(0, look);
        if (t >= KEYS[n - 1][0]) return keyValues(n - 1, look);
        int i = 0;
        while (i < n - 2 && t > KEYS[i + 1][0]) i++;
        double t0 = KEYS[i][0], t1 = KEYS[i + 1][0], dt = t1 - t0, s = (t - t0) / dt;
        double[] p0 = keyValues(i, look), p1 = keyValues(i + 1, look);
        double[] m0 = tangent(i, look), m1 = tangent(i + 1, look);
        double h00 = 2 * s * s * s - 3 * s * s + 1, h10 = s * s * s - 2 * s * s + s;
        double h01 = -2 * s * s * s + 3 * s * s, h11 = s * s * s - s * s;
        double[] out = new double[3];
        for (int c = 0; c < 3; c++) out[c] = h00 * p0[c] + h10 * dt * m0[c] + h01 * p1[c] + h11 * dt * m1[c];
        return out;
    }

    private static double[] tangent(int i, boolean look) {
        int n = KEYS.length;
        int a = Math.max(0, i - 1), b = Math.min(n - 1, i + 1);
        double[] pa = keyValues(a, look), pb = keyValues(b, look);
        double dt = KEYS[b][0] - KEYS[a][0];
        double[] m = new double[3];
        for (int c = 0; c < 3; c++) m[c] = dt == 0 ? 0 : (pb[c] - pa[c]) / dt;
        return m;
    }

    private static Vec3 positionAt(double t) {
        double[] p = spline(t, false);
        Vec3 v = world(p[0], p[1], p[2]);
        if (t > TOTAL_SECONDS) { // napas pelan saat menahan
            double d = t - TOTAL_SECONDS;
            v = v.add(right().scale(0.12 * Math.sin(d * 0.6))).add(0, 0.05 * Math.sin(d * 0.9), 0);
        }
        return v;
    }

    private static Vec3 lookAt(double t) {
        double[] p = spline(t, true);
        return world(p[0], p[1], p[2]);
    }

    /** {yaw, pitch} dari posisi ke titik bidik. */
    private static double[] angles(Vec3 pos, Vec3 target) {
        double dx = target.x - pos.x, dy = target.y - pos.y, dz = target.z - pos.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        return new double[]{Math.atan2(dz, dx) * 180.0 / Math.PI - 90.0, -(Math.atan2(dy, horiz) * 180.0 / Math.PI)};
    }

    /** {maju, kanan, atas} untuk yaw/pitch Minecraft. */
    private static Vec3[] axes(float yawDeg, float pitchDeg) {
        double y = Math.toRadians(yawDeg), p = Math.toRadians(pitchDeg);
        Vec3 fwd = new Vec3(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
        Vec3 right = new Vec3(-Math.cos(y), 0, -Math.sin(y));
        Vec3 up = right.cross(fwd).normalize();
        return new Vec3[]{fwd, right, up};
    }

    private static float wrap(float d) {
        d = d % 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }

    /** Pose kamera untuk detik sekarang; null kalau cutscene tidak aktif. */
    public static Pose pose() {
        if (!active) return null;
        Minecraft mc = Minecraft.getInstance();
        double t = elapsed();
        Vec3 pos = positionAt(t);
        Vec3 look = lookAt(t);

        // jangan tembus blok: potong jarak kalau ada penghalang antara patung dan kamera (setelah peralihan awal)
        if (t > 0.8 && mc.level != null && mc.player != null) {
            Vec3 from = center.add(0, 0.5, 0);
            BlockHitResult hit = mc.level.clip(new ClipContext(from, pos, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mc.player));
            if (hit.getType() == HitResult.Type.BLOCK && hit.getLocation().distanceTo(from) > 3.0) {
                pos = hit.getLocation().subtract(pos.subtract(from).normalize().scale(0.5));
            }
        }

        double[] a = angles(pos, look);
        float w = FxSprites.smooth((t - COMPOSE_START) / (COMPOSE_END - COMPOSE_START));
        float yaw = (float) a[0] + yawOffset * w;
        float pitch = (float) a[1] + pitchOffset * w;

        // peralihan mulus dari kamera pemain pada awal cutscene
        if (t < BLEND_FROM_PLAYER) {
            float b = FxSprites.smooth(t / BLEND_FROM_PLAYER);
            pos = playerEye.add(pos.subtract(playerEye).scale(b));
            yaw = playerYaw + wrap(yaw - playerYaw) * b;
            pitch = playerPitch + (pitch - playerPitch) * b;
        }
        return new Pose(pos, yaw, pitch);
    }

    // ------------------------------------------------------------ proyeksi

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

    /** Berapa piksel GUI tingginya satu blok di titik dunia tersebut (0 kalau di belakang kamera). */
    public static float pxPerBlock(Vec3 world, int guiWidth, int guiHeight) {
        float[] a = project(world, guiWidth, guiHeight), b = project(world.add(0, 1, 0), guiWidth, guiHeight);
        if (a == null || b == null) return 0f;
        return Math.abs(b[1] - a[1]);
    }
}
