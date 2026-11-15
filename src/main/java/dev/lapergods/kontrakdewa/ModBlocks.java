package dev.lapergods.kontrakdewa;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {
    public static final Block RA_STATUE = block("ra_statue", God.RA);
    public static final Block THOR_STATUE = block("thor_statue", God.THOR);

    public static final Item RA_STATUE_ITEM = blockItem("ra_statue", RA_STATUE);
    public static final Item THOR_STATUE_ITEM = blockItem("thor_statue", THOR_STATUE);

    public static final BlockEntityType<GodStatueBlockEntity.Ra> RA_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, KontrakDewa.id("ra_statue"),
            FabricBlockEntityTypeBuilder.<GodStatueBlockEntity.Ra>create(GodStatueBlockEntity.Ra::new, RA_STATUE).build());

    public static final BlockEntityType<GodStatueBlockEntity.Thor> THOR_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, KontrakDewa.id("thor_statue"),
            FabricBlockEntityTypeBuilder.<GodStatueBlockEntity.Thor>create(GodStatueBlockEntity.Thor::new, THOR_STATUE).build());

    private static Block block(String path, God god) {
        Identifier id = KontrakDewa.id(path);
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
                .setId(key)
                .strength(3.0f, 1200.0f)
                .noOcclusion();
        return Registry.register(BuiltInRegistries.BLOCK, key, new GodStatueBlock(god, props));
    }

    private static Item blockItem(String path, Block block) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, KontrakDewa.id(path));
        Item.Properties props = new Item.Properties().setId(key).useBlockDescriptionPrefix();
        return Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(block, props));
    }

    public static void init() {
        // memaksa class ter-load
    }

    private ModBlocks() {}
}
