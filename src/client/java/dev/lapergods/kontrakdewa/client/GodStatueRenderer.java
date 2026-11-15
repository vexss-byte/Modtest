package dev.lapergods.kontrakdewa.client;

import dev.lapergods.kontrakdewa.GodStatueBlockEntity;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.resources.Identifier;
import com.geckolib.model.DefaultedBlockGeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;

/** Renderer GeckoLib untuk patung. Model/animasi/texture dicari dari nama (ra_statue / thor_statue). */
public class GodStatueRenderer<R extends BlockEntityRenderState & GeoRenderState>
        extends GeoBlockRenderer<GodStatueBlockEntity, R> {
    public GodStatueRenderer(Identifier modelId) {
        super(new DefaultedBlockGeoModel<>(modelId));
    }
}
