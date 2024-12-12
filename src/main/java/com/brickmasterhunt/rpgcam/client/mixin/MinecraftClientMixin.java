package com.brickmasterhunt.rpgcam.client.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow public abstract void handleBlockBreaking(boolean breaking);

//    @Inject(
//        method = "doAttack",
//        at = @At("HEAD"),
//        cancellable = true
//    )
//    public void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
//        PlayerAdapter player = new PlayerAdapter(MinecraftClient.getInstance().player);
//        if (DelayedActionManager.onMouseAction(player, new MouseAction(this::method_1536))) {
//            cir.setReturnValue(false);
//        }
//
//    }

//    @Inject(
//        method = "handleBlockBreaking",
//        at = @At("HEAD"),
//        cancellable = true
//    )
//    public void onBlockBreaking(boolean pressed, CallbackInfo ci) {
//        if (pressed) {
//            this.handleBlockBreaking(true);
//        } else {
//            ci.cancel();
//        }
//
//    }

//    @Redirect(
//        method = {"handleInputEvents"},
//        at = @At(
//                value = "INVOKE",
//                target = "Lnet/minecraft/client/MinecraftClient;doItemUse()V",
//                ordinal = 0
//        )
//    )
//    public void onDoItemUse(MinecraftClient client) {
//        if (!BetterThirdPerson.getCameraManager().onMouseAction(player, new MouseAction(this::method_1583))) {
//            this.method_1583();
//        }
//
//    }

//    @Redirect(
//        method = "handleInputEvents",
//        at = @At(
//                value = "INVOKE",
//                target = "Lnet/minecraft/client/MinecraftClient;doItemUse()V",
//                ordinal = 1
//        )
//    )
//    public void onItemUseRepeatable(MinecraftClient client) {
//        PlayerAdapter player = new PlayerAdapter(client.field_1724);
//        if (!BetterThirdPerson.getCameraManager().onMouseAction(player, new ItemRepeatableUseAction(ClientAdapter.INSTANCE, () -> {
//            return this.field_1752;
//        }, this::method_1583))) {
//            this.method_1583();
//        }
//
//    }
}
