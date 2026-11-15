package dev.lapergods.kontrakdewa.client;

/** Cooldown skill di sisi client (hanya untuk tampilan HUD; server yang menentukan). */
public final class ClientCooldowns {
    private static final long[] END_MS = new long[6];

    public static void set(int slot, int ticks) {
        if (slot >= 0 && slot < END_MS.length) END_MS[slot] = System.currentTimeMillis() + ticks * 50L;
    }

    /** Sisa cooldown dalam detik (0 kalau siap). */
    public static double remaining(int slot) {
        if (slot < 0 || slot >= END_MS.length) return 0;
        return Math.max(0, (END_MS[slot] - System.currentTimeMillis()) / 1000.0);
    }

    private ClientCooldowns() {}
}
