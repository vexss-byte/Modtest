package dev.lapergods.kontrakdewa.client;

import dev.lapergods.kontrakdewa.GodStatueBlockEntity;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.resources.Identifier;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;

/** Renderer GeckoLib untuk patung. Model/animasi/texture dicari dari nama (ra_statue / thor_statue). */
public class GodStatueRenderer<R extends BlockEntityRenderState & GeoRenderState>
        extends GeoBlockRenderer<GodStatueBlockEntity, R> {
    public GodStatueRenderer(Identifier modelId) {
        super(new DefaultedBlockGeoModel<>(modelId));
    }
}
