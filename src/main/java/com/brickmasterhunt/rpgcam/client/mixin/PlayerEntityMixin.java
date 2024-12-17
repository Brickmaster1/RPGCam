package com.brickmasterhunt.rpgcam.client.mixin;

import com.brickmasterhunt.rpgcam.client.CamState;
import com.brickmasterhunt.rpgcam.client.RpgCam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @ModifyArg(
            method = "travel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;travel(Lnet/minecraft/util/math/Vec3d;)V"
            )
    )
    private Vec3d travel(Vec3d movementInput) {
        if (RpgCam.isDetachedCameraEnabled()) {
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

            float movementYaw = RpgCam.getCameraRotation().x + inputAngle - client.player.getYaw();
            double radianYaw = Math.toRadians(movementYaw);

            double motionX = Math.sin(radianYaw) * length;
            double motionZ = -Math.cos(radianYaw) * length;

            Vec3d movementVector = new Vec3d(motionX, movementInput.y, motionZ);

            CamState.currentMovementAngle = inputAngle;
            CamState.currentMovementVector = movementVector;
            return movementVector;
        }

        return movementInput;
    }
}
