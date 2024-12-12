package com.brickmasterhunt.rpgcam.client.mixin;

import com.brickmasterhunt.rpgcam.client.RpgCamClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @ModifyArg(
            method = "travel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;travel(Lnet/minecraft/util/math/Vec3d;)V"
            )
    )
    private Vec3d travel(Vec3d movementInput) {
        if (RpgCamClient.isDetachedCameraEnabled()) {
            MinecraftClient client = MinecraftClient.getInstance();

            float inputAngle = ( //Why is inverted, idk? but it works?
                (client.options.backKey.isPressed() && client.options.rightKey.isPressed()) ? 45.0f :
                (client.options.backKey.isPressed() && client.options.leftKey.isPressed()) ? 135.0f :
                (client.options.forwardKey.isPressed() && client.options.leftKey.isPressed()) ? 225.0f :
                (client.options.forwardKey.isPressed() && client.options.rightKey.isPressed()) ? 315.0f :
                client.options.rightKey.isPressed() ? 0.0f :
                client.options.backKey.isPressed() ? 90.0f :
                client.options.leftKey.isPressed() ? 180.0f :
                270.0f // client.options.forwardKey.isPressed()
            );

            double length = movementInput.horizontalLength();

            float movementYaw = RpgCamClient.getCameraRotation().x + inputAngle;
            double radianYaw = Math.toRadians(movementYaw);

            double motionX = Math.sin(radianYaw) * length;
            double motionZ = -Math.cos(radianYaw) * length;

            Vec3d movementVector = new Vec3d(motionX, movementInput.y, motionZ);

            RpgCamClient.cacheMovementHistory(movementVector);
            return movementVector;
        }

        return movementInput;
    }
}
