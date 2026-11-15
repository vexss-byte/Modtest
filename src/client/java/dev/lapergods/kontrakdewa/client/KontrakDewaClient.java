package dev.lapergods.kontrakdewa.client;

import dev.lapergods.kontrakdewa.God;
import dev.lapergods.kontrakdewa.ModBlocks;
import dev.lapergods.kontrakdewa.net.CooldownPayload;
import dev.lapergods.kontrakdewa.net.OpenContractPayload;
import dev.lapergods.kontrakdewa.net.ToggleBattlePayload;
import dev.lapergods.kontrakdewa.net.UseSkillPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;

public class KontrakDewaClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(ModBlocks.RA_BE, ctx -> new GodStatueRenderer(ctx, ModBlocks.RA_BE));
        BlockEntityRenderers.register(ModBlocks.THOR_BE, ctx -> new GodStatueRenderer(ctx, ModBlocks.THOR_BE));

        ModKeys.init();

        // Weapon di punggung pemain (saat tidak Battle Mode)
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityRenderer instanceof AvatarRenderer avatarRenderer) {
                registrationHelper.register(new BackWeaponLayer(avatarRenderer));
            }
        });
        SkillHud.init();

        ClientPlayNetworking.registerGlobalReceiver(OpenContractPayload.TYPE, (payload, context) -> {
            God god = God.byId(payload.godId());
            if (god == null) return;
            context.client().gui.setScreen(new ContractScreen(god, payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(CooldownPayload.TYPE,
                (payload, context) -> ClientCooldowns.set(payload.slot(), payload.ticks()));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            while (ModKeys.BATTLE.consumeClick()) ClientPlayNetworking.send(ToggleBattlePayload.INSTANCE);
            for (int i = 0; i < ModKeys.SKILLS.length; i++) {
                while (ModKeys.SKILLS[i].consumeClick()) ClientPlayNetworking.send(new UseSkillPayload(i));
            }
        });
    }
}
