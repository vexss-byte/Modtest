package dev.lapergods.kontrakdewa;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Konfigurasi balance. File: config/kontrakdewa.json (dibuat otomatis saat pertama jalan). Perubahan berlaku setelah restart. */
public final class ModConfig {
    /** XP minimum untuk Faith 1..10. */
    public int[] faithThresholds = {0, 100, 250, 450, 700, 1000, 1400, 1900, 2500, 3200};
    /** XP dari memakai kekuatan: tiap N detik di Battle Mode, dan tiap skill yang mengenai target. */
    public int battleXpPerInterval = 1;
    public int battleXpIntervalSeconds = 10;
    public int skillHitXp = 3;
    /** Batas XP harian dari battle (anti AFK farming). Persembahan tidak kena batas ini. */
    public int dailyBattleXpCap = 600;
    public int offeringXp = 50;
    public int offeringStreakBonus = 10;
    public int offeringStreakBonusMax = 100;
    public int weaponClaimCooldownSeconds = 600;
    /** Skill boleh menyalakan api / merusak blok. Matikan kalau server tidak mau griefing. */
    public boolean allowBlockDamage = true;
    /** Shader GLSL untuk cutscene patung (langit badai). Matikan kalau GPU lemah atau ada masalah tampilan; efek sederhana tetap dipakai. */
    public boolean cutsceneShader = true;
    public Map<String, SkillCfg> skills = new LinkedHashMap<>();

    public static final class SkillCfg {
        public double cooldown;
        public double damage;
        public double range;

        public SkillCfg(double cooldown, double damage, double range) {
            this.cooldown = cooldown;
            this.damage = damage;
            this.range = range;
        }
    }

    private static ModConfig instance = withDefaults(new ModConfig());

    public static ModConfig get() {
        return instance;
    }

    public static SkillCfg skill(God god, int slot) {
        return instance.skills.get(god.id + "_" + (slot + 1));
    }

    private static ModConfig withDefaults(ModConfig c) {
        c.skills.putIfAbsent("ra_1", new SkillCfg(3, 7, 28));    // Sunspear
        c.skills.putIfAbsent("ra_2", new SkillCfg(12, 11, 24));  // Noon Beam
        c.skills.putIfAbsent("ra_3", new SkillCfg(8, 0, 9));     // Mirage Step (range = jarak dash)
        c.skills.putIfAbsent("thor_1", new SkillCfg(6, 9, 30));  // Thunder Strike
        c.skills.putIfAbsent("thor_2", new SkillCfg(10, 8, 24)); // Chain Lightning
        c.skills.putIfAbsent("thor_3", new SkillCfg(12, 10, 18));// Hammer Throw
        c.skills.putIfAbsent("ra_4", new SkillCfg(14, 6, 7));    // Solar Flare (range = radius)
        c.skills.putIfAbsent("ra_5", new SkillCfg(20, 0, 0));    // Scarab Guard (perisai)
        c.skills.putIfAbsent("ra_6", new SkillCfg(60, 20, 30));  // Call the Sun (range = jarak bidik; radius 7)
        c.skills.putIfAbsent("thor_4", new SkillCfg(14, 10, 5)); // Storm Leap (range = radius hantaman)
        c.skills.putIfAbsent("thor_5", new SkillCfg(18, 2, 6));  // Static Field (damage per 0,5 detik, range = radius)
        c.skills.putIfAbsent("thor_6", new SkillCfg(25, 5, 0));  // Overcharge (damage = bonus petir per pukulan)
        c.skills.putIfAbsent("thor_7", new SkillCfg(120, 12, 12)); // Ragnarok Storm (range = radius badai)
        if (c.faithThresholds == null || c.faithThresholds.length != 10) c.faithThresholds = new ModConfig().faithThresholds;
        return c;
    }

    public static void load() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Path path = FabricLoader.getInstance().getConfigDir().resolve("kontrakdewa.json");
        try {
            if (Files.exists(path)) {
                ModConfig loaded = gson.fromJson(Files.readString(path), ModConfig.class);
                if (loaded != null) instance = withDefaults(loaded);
            }
            Files.writeString(path, gson.toJson(instance)); // tulis ulang supaya kunci baru muncul
        } catch (Exception e) {
            KontrakDewa.LOGGER.error("Gagal membaca/menulis config, memakai default", e);
        }
    }

    private ModConfig() {}
}
