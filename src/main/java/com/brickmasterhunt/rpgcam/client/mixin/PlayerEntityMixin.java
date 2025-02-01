package com.brickmasterhunt.rpgcam.client.mixin;

import com.brickmasterhunt.rpgcam.client.RpgCam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import static com.brickmasterhunt.rpgcam.client.CamState.*;
import static com.brickmasterhunt.rpgcam.client.player.movement.RelativeMovement.*;

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
        if (isDetachedCameraEnabled()) {
            float inputAngle = getAngleFromKeys();
            Vec3d movementVector = calculateCameraRelativeMovementVector(movementInput, inputAngle);
            onRelativeMovement(movementVector, inputAngle);

            return movementVector;
        }

        return movementInput;
    }
}
