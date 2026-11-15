package dev.lapergods.kontrakdewa.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.lapergods.kontrakdewa.KontrakDewa;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

public final class ModKeys {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(KontrakDewa.id("main"));

    public static final KeyMapping BATTLE = reg("battle_mode", InputConstants.KEY_B);
    public static final KeyMapping[] SKILLS = {
            reg("skill_1", InputConstants.KEY_Z),
            reg("skill_2", InputConstants.KEY_C),
            reg("skill_3", InputConstants.KEY_V),
            reg("skill_4", InputConstants.KEY_X),
            reg("skill_5", InputConstants.KEY_R),
            reg("skill_6", InputConstants.KEY_G),
            reg("skill_special", InputConstants.KEY_H),
    };

    private static KeyMapping reg(String name, int key) {
        return KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key." + KontrakDewa.MOD_ID + "." + name, key, CATEGORY));
    }

    public static void init() {
        // memaksa class ter-load
    }

    private ModKeys() {}
}
