package com.brickmasterhunt.rpgcam.client.mixin;

import com.brickmasterhunt.rpgcam.client.CamState;
import com.brickmasterhunt.rpgcam.client.RpgCam;
import com.brickmasterhunt.rpgcam.client.interaction.InteractionManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static com.brickmasterhunt.rpgcam.client.CamState.*;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "lockCursor", at = @At("HEAD"), cancellable = true)
    private void lockCursor(CallbackInfo ci) {
        if (isDetachedCameraEnabled() && !isCameraGrabbed()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "updateMouse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"
            ),
            cancellable = true,
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void preventPlayerRotation(CallbackInfo ci, double d, double e, double k, double l, double f, double g, double h, int m) {
        if (isCameraGrabbed()) {
            onMouseRelativeMove(l * (double) m);
            //InteractionManager.handleCameraRotateInteraction(l * (double)m);
            ci.cancel(); // Skip only the call to changeLookDirection
        }
    }
}
