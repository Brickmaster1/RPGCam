package com.brickmasterhunt.rpgcam.client.player;

import net.minecraft.client.network.ClientPlayerEntity;

import static com.brickmasterhunt.rpgcam.client.CamState.*;

public class PlayerUtils {
    private static boolean isSprintAllowed() {
        return Math.abs(getRelativeMovementAngle()) <= 45.0f;
    }

    public static void setPlayerRotation(ClientPlayerEntity player, float pitch, float yaw) {
        player.setPitch(pitch);
        player.setYaw(yaw);
        player.setHeadYaw(player.getYaw());

        player.prevPitch = player.getPitch();
        player.prevYaw = player.getYaw();

        player.prevHeadYaw = player.headYaw;

        player.bodyYaw = player.headYaw;
        player.prevBodyYaw = player.bodyYaw;
    }
}
