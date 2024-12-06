package com.brickmasterhunt.rpgcam.client.mixin;

import com.brickmasterhunt.rpgcam.client.RpgCamClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Unique
    private boolean cachedBl3 = false;

    @Inject(
            method = "handleInputEvents",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/option/KeyBinding;wasPressed()Z",
                    ordinal = 0
            ),
            cancellable = true
    )
    private void interceptAttackKey(CallbackInfo ci) {
        if (RpgCamClient.isDetachedCameraEnabled()) {
            // Custom logic to replace the while block
            while (MinecraftClient.getInstance().options.attackKey.wasPressed()) {
                cachedBl3 |= MinecraftClient.getInstance().doAttack();
            }
        }
    }

    @Inject(
            method = "handleInputEvents",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/MinecraftClient;handleBlockBreaking(Z)V"
            ),
            cancellable = true
    )
    private void onHandleBlockBreaking(CallbackInfo ci) {
        if (RpgCamClient.isDetachedCameraEnabled()) {
            MinecraftClient.getInstance().handleBlockBreaking(MinecraftClient.getInstance().currentScreen == null && !cachedBl3 && MinecraftClient.getInstance().options.attackKey.isPressed());
            ci.cancel();
        }
    }
}
