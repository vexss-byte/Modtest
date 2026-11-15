package dev.lapergods.kontrakdewa.client;

import com.geckolib.renderer.GeoBlockRenderer;
import dev.lapergods.kontrakdewa.GodStatueBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Renderer GeckoLib untuk patung. Model/animasi/texture dicari otomatis dari id block entity type
 * (kontrakdewa:ra_statue / kontrakdewa:thor_statue).
 */
public class GodStatueRenderer extends GeoBlockRenderer<GodStatueBlockEntity, BlockEntityRenderState> {
    public GodStatueRenderer(BlockEntityRendererProvider.Context context, BlockEntityType<? extends GodStatueBlockEntity> type) {
        super(context, type);
    }
}
