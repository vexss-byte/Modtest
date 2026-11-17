package dev.lapergods.kontrakdewa.mixin.client;

import dev.lapergods.kontrakdewa.client.CutsceneCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Saat cutscene aktif, timpa posisi dan arah kamera setelah kamera disejajarkan dengan pemain. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void kontrakdewa$cutsceneOverride(float partialTicks, CallbackInfo ci) {
        CutsceneCamera.Pose pose = CutsceneCamera.pose();
        if (pose == null) return;
        this.setRotation(pose.yaw(), pose.pitch());
        this.setPosition(pose.pos());
    }
}
