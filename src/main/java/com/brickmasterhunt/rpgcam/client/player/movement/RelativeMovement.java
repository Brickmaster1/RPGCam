package com.brickmasterhunt.rpgcam.client.player.movement;

import com.brickmasterhunt.rpgcam.client.CamState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import static com.brickmasterhunt.rpgcam.client.CamState.*;
import static com.brickmasterhunt.rpgcam.client.player.PlayerUtils.*;

public class RelativeMovement {
    public static void handleMovementRelativeToCamera(float rotateSpeed, float lerp) {
        ClientPlayerEntity player = CamState.client.player;

        float cameraAngle = getCameraYaw();
        if (getRelativeMovementVector().horizontalLength() < 0.01f) return; // No movement, no update

        // Interpolate smoothly towards the target yaw
        float smoothedYaw = MathHelper.stepUnwrappedAngleTowards(
                //MathHelper.clamp(lerp * client.getTickDelta(), 0.0f, 1.0f),
                player.getYaw(),
                MathHelper.wrapDegrees(getRelativeMovementAngle() + cameraAngle + 180.0f),
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
