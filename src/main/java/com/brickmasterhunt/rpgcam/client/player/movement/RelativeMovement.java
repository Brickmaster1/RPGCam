package com.brickmasterhunt.rpgcam.client.player.movement;

import com.brickmasterhunt.rpgcam.client.CamState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import static com.brickmasterhunt.rpgcam.client.CamState.*;
import static com.brickmasterhunt.rpgcam.client.player.PlayerUtils.*;

public class RelativeMovement {
    public static float getAngleFromKeys() {
        return ( //Why is inverted, idk? but it works?
            (client.options.backKey.isPressed() && client.options.rightKey.isPressed()) ? 45.0f :
            (client.options.backKey.isPressed() && client.options.leftKey.isPressed()) ? 135.0f :
            (client.options.forwardKey.isPressed() && client.options.leftKey.isPressed()) ? 225.0f :
            (client.options.forwardKey.isPressed() && client.options.rightKey.isPressed()) ? 315.0f :
            client.options.rightKey.isPressed() ? 0.0f :
            client.options.backKey.isPressed() ? 90.0f :
            client.options.leftKey.isPressed() ? 180.0f :
            270.0f // client.options.forwardKey.isPressed()
        );
    }

    public static Vec3d calculateCameraRelativeMovementVector(Vec3d movementInput, float inputAngle) {
        MinecraftClient client = MinecraftClient.getInstance();



        double length = movementInput.horizontalLength();

        float movementYaw = getCameraYaw() + inputAngle - client.player.getYaw() - 90.0f;
        double radianYaw = Math.toRadians(movementYaw);

        double motionX = Math.sin(radianYaw) * length;
        double motionZ = -Math.cos(radianYaw) * length;

        return new Vec3d(motionX, movementInput.y, motionZ);
    }

    public static void handleMovementRelativeToCamera(float rotateSpeed, float lerp) {
        ClientPlayerEntity player = CamState.client.player;

        float cameraAngle = getCameraYaw();
        if (getRelativeMovementVector().horizontalLength() < 0.01f) return; // No movement, no update

        // Interpolate smoothly towards the target yaw
        float smoothedYaw = MathHelper.stepUnwrappedAngleTowards(
                //MathHelper.clamp(lerp * client.getTickDelta(), 0.0f, 1.0f),
                player.getYaw(),
                MathHelper.wrapDegrees(getRelativeMovementAngle() + cameraAngle + 90.0f),
                rotateSpeed
        );

        float lerpedYaw = MathHelper.lerpAngleDegrees(MathHelper.clamp(lerp * CamState.client.getTickDelta(), 0.0F, 1.0F), player.getYaw(), smoothedYaw);

        setPlayerRotation(player, player.getPitch(), lerpedYaw);
    }

    public static void lerpPlayerLookAt(EntityAnchorArgumentType.EntityAnchor anchorPoint, Vec3d target, float lerp) {
        assert CamState.client.player != null;
        ClientPlayerEntity player = CamState.client.player;

        Vec3d vec3d = anchorPoint.positionAt(player);
        double d = target.x - vec3d.x;
        double e = target.y - vec3d.y;
        double f = target.z - vec3d.z;
        double g = Math.sqrt(d * d + f * f);

        float pitchToSet = MathHelper.wrapDegrees((float)(-(MathHelper.atan2(e, g) * 57.2957763671875)));
        float yawToSet = MathHelper.wrapDegrees((float)(MathHelper.atan2(f, d) * 57.2957763671875) - 90.0F);

        pitchToSet = MathHelper.lerpAngleDegrees(MathHelper.clamp(lerp * CamState.client.getTickDelta(), 0.0F, 1.0F), player.getPitch(), pitchToSet);
        yawToSet = MathHelper.lerpAngleDegrees(MathHelper.clamp(lerp * CamState.client.getTickDelta(), 0.0F, 1.0F), player.getYaw(), yawToSet);

        setPlayerRotation(player, pitchToSet, yawToSet);
    }
}
