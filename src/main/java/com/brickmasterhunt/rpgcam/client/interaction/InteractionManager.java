package com.brickmasterhunt.rpgcam.client.interaction;

import com.brickmasterhunt.rpgcam.client.CamState;
import com.brickmasterhunt.rpgcam.client.RpgCam;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class InteractionManager {
    public static void handleCameraRotateInteraction(double mouseDeltaY) {
        Camera camera = CamState.client.gameRenderer.getCamera();
        if (CamState.GRAB_CAMERA_KEY.isPressed()) {
            double mousePosX = (CamState.client.mouse.getX() - ((double) CamState.client.getWindow().getWidth() / 2));
            CamState.mouseDeltaX = mousePosX - CamState.prevMousePosX;

            if (Math.abs(CamState.mouseDeltaX) <= 1.0d && Math.abs(mouseDeltaY) <= 1.0d) {
                return;
            }

            CamState.currentCameraAngleYaw = ((float) CamState.mouseDeltaX * CamState.SENSITIVITY) + CamState.oldCameraAngleYaw;
            CamState.currentCameraAnglePitch = ((float) mouseDeltaY * CamState.SENSITIVITY) + CamState.oldCameraAnglePitch;
            CamState.currentCameraAnglePitch = MathHelper.clamp(CamState.currentCameraAnglePitch, -89.99f, 89.99f);

            CamState.currentCameraAngleYaw = MathHelper.wrapDegrees(CamState.currentCameraAngleYaw);

            Vec3d playerPos = CamState.client.player.getPos();

            double pitchRadians = Math.toRadians(CamState.currentCameraAnglePitch);
            double yawRadians = Math.toRadians(CamState.currentCameraAngleYaw);

            camera.setPos(
                playerPos.x + (CamState.PLAYER_CAMERA_DISTANCE * Math.cos(pitchRadians) * Math.cos(yawRadians)),
                playerPos.y + (CamState.PLAYER_CAMERA_DISTANCE * Math.sin(pitchRadians)),
                playerPos.z + (CamState.PLAYER_CAMERA_DISTANCE * Math.cos(pitchRadians) * Math.sin(yawRadians))
            );

            CamState.oldCameraAngleYaw = CamState.currentCameraAngleYaw;
            CamState.oldCameraAnglePitch = CamState.currentCameraAnglePitch;

            CamState.prevMousePosX = mousePosX;

            Vec3d direction = playerPos.subtract(camera.getPos()).normalize();
            double towardsPlayerYaw = Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0f;
            double towardsPlayerPitch = Math.toDegrees(-Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z)));

            RpgCam.setCameraRotation((float) towardsPlayerYaw, (float) towardsPlayerPitch);
        } else if (CamState.isCameraGrabbed) {
            // Reset state when the key is released
            CamState.isCameraGrabbed = false;
            CamState.client.mouse.unlockCursor();
            GLFW.glfwSetCursorPos(CamState.client.getWindow().getHandle(), CamState.savedMousePosX, CamState.savedMousePosY);
            CamState.savedMousePosX = 0D;
            CamState.savedMousePosY = 0D;
            CamState.prevMousePosX = 0D;
        }
    }
}
