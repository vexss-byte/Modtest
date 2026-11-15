package dev.lapergods.kontrakdewa;

import java.util.List;

/** Daftar dewa. Teks mengikuti layar kontrak di video referensi. */
public enum God {
    RA("ra", "Patung Ra", "Ra", 0xFFE0A030,
            List.of("Weapon takdir dan 3 skill pertama", "3 skill lagi saat Faith naik"),
            "Malam: damage -25%. Siang cerah +20%. Hanya saat Battle Mode."),
    THOR("thor", "Patung Thor", "Thor", 0xFF3F7FE0,
            List.of("Weapon takdir dan 3 skill pertama", "3 skill lagi saat Faith naik", "Special Magic di Faith 10"),
            "Armor kamu tinggal setengah saat Battle Mode.");

    public final String id;
    public final String statueName;
    public final String displayName;
    public final int accent; // ARGB
    public final List<String> gives;
    public final String price;

    God(String id, String statueName, String displayName, int accent, List<String> gives, String price) {
        this.id = id;
        this.statueName = statueName;
        this.displayName = displayName;
        this.accent = accent;
        this.gives = gives;
        this.price = price;
    }

    public static God byId(String id) {
        for (God g : values()) if (g.id.equals(id)) return g;
        return null;
    }
}
