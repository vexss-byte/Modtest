package dev.lapergods.kontrakdewa;

import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** Penjadwal sederhana berbasis tick server (untuk skill yang punya jeda / berulang). */
public final class Scheduler {
    private record Task(long runAt, Runnable action) {}

    private static final List<Task> TASKS = new ArrayList<>();

    public static void later(MinecraftServer server, int delayTicks, Runnable action) {
        TASKS.add(new Task(server.getTickCount() + Math.max(1, delayTicks), action));
    }

    /** Jalankan action(i) sebanyak times kali, tiap periodTicks tick; i mulai dari 0. */
    public static void repeat(MinecraftServer server, int periodTicks, int times, IntConsumer action) {
        for (int i = 0; i < times; i++) {
            final int idx = i;
            later(server, periodTicks * (i + 1), () -> action.accept(idx));
        }
    }

    public static void tick(MinecraftServer server) {
        if (TASKS.isEmpty()) return;
        long now = server.getTickCount();
        List<Task> due = new ArrayList<>();
        TASKS.removeIf(t -> {
            if (t.runAt() <= now) { due.add(t); return true; }
            return false;
        });
        for (Task t : due) {
            try {
                t.action().run();
            } catch (Exception e) {
                KontrakDewa.LOGGER.error("Task terjadwal gagal", e);
            }
        }
    }

    public static void clear() {
        TASKS.clear();
    }

    private Scheduler() {}
}
