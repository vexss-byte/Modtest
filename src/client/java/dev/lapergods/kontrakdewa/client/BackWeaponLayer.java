package dev.lapergods.kontrakdewa.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.lapergods.kontrakdewa.BattleService;
import dev.lapergods.kontrakdewa.God;
import dev.lapergods.kontrakdewa.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Menggambar weapon takdir di punggung pemain yang punya kontrak tapi tidak sedang Battle Mode
 * (terlihat pemain lain). Posisi/rotasi/skala dikumpulkan di konstanta supaya mudah disetel
 * setelah model senjata final dari kamu jadi.
 */
public class BackWeaponLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    // Penyetelan posisi (satuan blok, di ruang model pemain: +Y ke bawah, +Z ke belakang)
    private static final float OFFSET_X = 0.0F;
    private static final float OFFSET_Y = 0.55F;
    private static final float OFFSET_Z = 0.22F;
    private static final float TILT_DEGREES = 40.0F;
    private static final float SCALE = 1.0F;

    private final ItemStackRenderState itemState = new ItemStackRenderState();

    public BackWeaponLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state,
                       float limbAngle, float limbDistance) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity entity = mc.level.getEntity(state.id);
        if (!(entity instanceof Player player)) return;

        God god = BattleService.godOf(player);
        if (god == null || BattleService.inBattle(player)) return;
        Item weapon = ModItems.weaponFor(god);
        if (player.getMainHandItem().is(weapon)) return; // sedang dipegang, jangan dobel

        ItemStack stack = new ItemStack(weapon);
        ItemModelResolver resolver = mc.getItemModelResolver();
        resolver.updateForTopItem(itemState, stack, ItemDisplayContext.FIXED, mc.level, null, 0);
        if (itemState.isEmpty()) return;

        poseStack.pushPose();
        poseStack.translate(OFFSET_X, OFFSET_Y, OFFSET_Z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F + TILT_DEGREES)); // layer berada di ruang terbalik: putar tegak lalu miringkan
        poseStack.scale(SCALE, SCALE, SCALE);
        itemState.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
