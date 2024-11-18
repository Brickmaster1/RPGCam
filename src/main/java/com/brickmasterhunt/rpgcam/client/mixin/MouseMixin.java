package com.brickmasterhunt.rpgcam.client.mixin;

import com.brickmasterhunt.rpgcam.client.RpgCamClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "lockCursor", at = @At("HEAD"), cancellable = true)
    private void lockCursor(CallbackInfo ci) {
        if (RpgCamClient.isDetachedCameraEnabled()) {
            ci.cancel();
        }
    }
}
