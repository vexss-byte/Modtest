package dev.lapergods.kontrakdewa.client;

import dev.lapergods.kontrakdewa.God;
import dev.lapergods.kontrakdewa.KontrakDewa;
import dev.lapergods.kontrakdewa.ModBlocks;
import dev.lapergods.kontrakdewa.net.OpenContractPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class KontrakDewaClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(ModBlocks.RA_BE, ctx -> new GodStatueRenderer<>(KontrakDewa.id("ra_statue")));
        BlockEntityRenderers.register(ModBlocks.THOR_BE, ctx -> new GodStatueRenderer<>(KontrakDewa.id("thor_statue")));

        ClientPlayNetworking.registerGlobalReceiver(OpenContractPayload.TYPE, (payload, context) -> {
            God god = God.byId(payload.godId());
            if (god == null) return;
            context.client().gui.setScreen(new ContractScreen(god, payload.currentGodId()));
        });
    }
}
