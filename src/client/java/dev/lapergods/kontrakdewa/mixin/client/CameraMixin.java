package dev.lapergods.kontrakdewa.mixin.client;

import dev.lapergods.kontrakdewa.client.ContractScreen;
import dev.lapergods.kontrakdewa.client.CutsceneCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Saat cutscene aktif, timpa posisi dan arah kamera setelah kamera disejajarkan dengan pemain.
 * Pembersihan otomatis: kalau layar kontrak sudah tidak terbuka, dunia belum dimuat (pemain keluar), atau terjadi error,
 * cutscene dihentikan dan HUD/kamera dikembalikan.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void kontrakdewa$cutsceneOverride(float partialTicks, CallbackInfo ci) {
        if (!CutsceneCamera.isActive()) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null || !ContractScreen.isOpen()) {
                CutsceneCamera.stop();
                return;
            }
            CutsceneCamera.Pose pose = CutsceneCamera.pose();
            if (pose == null) return;
            this.setRotation(pose.yaw(), pose.pitch());
            this.setPosition(pose.pos());
        } catch (Throwable t) {
            CutsceneCamera.stop();
        }
    }
}
