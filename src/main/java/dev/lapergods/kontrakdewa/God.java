package dev.lapergods.kontrakdewa;

import java.util.List;

/** Daftar dewa. Teks mengikuti layar kontrak di video referensi. */
public enum God {
    RA("ra", "Patung Ra", "Ra", 0xFFE0A030,
            List.of("Weapon takdir dan 3 skill pertama", "3 skill lagi saat Faith naik"),
            "Malam: damage -25%. Siang cerah +20%. Hanya saat Battle Mode.",
            List.of("Sunspear", "Noon Beam", "Mirage Step", "Solar Flare", "Scarab Guard", "Call the Sun"), "Sun Staff", null),
    THOR("thor", "Patung Thor", "Thor", 0xFF3F7FE0,
            List.of("Weapon takdir dan 3 skill pertama", "3 skill lagi saat Faith naik", "Special Magic di Faith 10"),
            "Armor kamu tinggal setengah saat Battle Mode.",
            List.of("Thunder Strike", "Chain Lightning", "Hammer Throw", "Storm Leap", "Static Field", "Overcharge"), "Thunder Hammer", "Ragnarok Storm");

    public final String id;
    public final String statueName;
    public final String displayName;
    public final int accent; // ARGB
    public final List<String> gives;
    public final String price;
    /** 6 skill: 1-3 terbuka dari awal, 4-6 saat Faith naik. */
    public final List<String> skills;
    public final String weaponName;
    /** Special Magic (slot 7, Faith 10); null kalau dewa ini tidak punya. */
    public final String special;

    God(String id, String statueName, String displayName, int accent, List<String> gives, String price, List<String> skills, String weaponName, String special) {
        this.id = id;
        this.statueName = statueName;
        this.displayName = displayName;
        this.accent = accent;
        this.gives = gives;
        this.price = price;
        this.skills = skills;
        this.weaponName = weaponName;
        this.special = special;
    }

    /** Nama skill untuk slot 0..6 (6 = Special Magic). */
    public String skillName(int slot) {
        return slot == 6 ? special : skills.get(slot);
    }

    public static God byId(String id) {
        for (God g : values()) if (g.id.equals(id)) return g;
        return null;
    }
}
