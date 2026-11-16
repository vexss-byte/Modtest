package dev.lapergods.kontrakdewa;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

/** Weapon takdir. Stat sementara; skill & efek menyusul di tahap 3. */
public final class ModItems {
    public static final ToolMaterial DIVINE = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 100000, 1.0F, 0.0F, 15, ItemTags.NETHERITE_TOOL_MATERIALS);

    public static final Item SUN_STAFF = register("sun_staff", new Item.Properties().sword(DIVINE, 5.0F, -2.4F));
    public static final Item THUNDER_HAMMER = register("thunder_hammer", new Item.Properties().sword(DIVINE, 6.0F, -2.8F));

    /** Weapon dewa mana pun (dipakai untuk mencegah jatuh/tersimpan di luar Battle Mode). */
    public static boolean isDivine(net.minecraft.world.item.ItemStack stack) {
        return stack.is(SUN_STAFF) || stack.is(THUNDER_HAMMER);
    }

    public static Item weaponFor(God god) {
        return god == God.RA ? SUN_STAFF : THUNDER_HAMMER;
    }

    private static Item register(String path, Item.Properties props) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, KontrakDewa.id(path));
        return Registry.register(BuiltInRegistries.ITEM, key, new Item(props.setId(key)));
    }

    public static void init() {
        // memaksa class ter-load
    }

    private ModItems() {}
}
