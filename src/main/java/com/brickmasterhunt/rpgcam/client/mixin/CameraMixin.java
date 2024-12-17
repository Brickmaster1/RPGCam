package com.brickmasterhunt.rpgcam.client.mixin;

import com.brickmasterhunt.rpgcam.client.RpgCam;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class CameraMixin {

    @Shadow private Vec3d pos;

    // Prevent setPos from causing recursion when the camera is detached
    @Inject(method = "setPos(DDD)V", at = @At("HEAD"), cancellable = true)
    public void setPos(double x, double y, double z, CallbackInfo ci) {
        if (RpgCam.isDetachedCameraEnabled()) {
            this.pos = new Vec3d(x, y, z);
            ci.cancel();  // Prevent default camera position handling
        }
    }
    // Prevent setRotation from causing recursion when the camera is detached
    @Inject(method = "setRotation(FF)V", at = @At("HEAD"), cancellable = true)
    public void setRotation(float yaw, float pitch, CallbackInfo ci) {
        if (RpgCam.isDetachedCameraEnabled()) {
            // Prevent the default rotation handling to maintain custom camera behavior
            ci.cancel();
        }
    }
}