package com.brickmasterhunt.rpgcam.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class CamState {
    public static final MinecraftClient client = MinecraftClient.getInstance();

    public static final KeyBinding GRAB_CAMERA_KEY = RpgCam.createKeybinding("grab_camera", GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
    public static final KeyBinding MOVE_CAMERA_FORWARD_KEY = RpgCam.createKeybinding("move_camera_forward", GLFW.GLFW_KEY_KP_ADD);
    public static final KeyBinding TOGGLE_CAMERA_KEY = RpgCam.createKeybinding("toggle_camera", GLFW.GLFW_KEY_F7);

    public static final double PLAYER_CAMERA_DISTANCE = 7.0D;
    public static final float SENSITIVITY = 0.15F;
    public static final float INITIAL_CAMERA_ANGLE_XZ = 45.0F;
    public static final float INITIAL_CAMERA_ANGLE_Y = 45.0F;
    public static final float LOOK_CURSOR_LERP_FACTOR = 5.0F;
    public static final float ROTATE_LERP_FACTOR = 2.5F;
    public static final float PLAYER_ROTATE_SPEED = 20.0F;
    public static final double RAYCAST_MAX_DISTANCE = 100.0D;
    public static final int MOVEMENT_HISTORY_SIZE = 20;
    public static final long PLAYER_IDLE_WAIT_TIME = 4000;
    public static final long NOT_INTERACTING_WAIT_TIME = 3000;

    private static boolean isCameraGrabbed = false;
    private static boolean needsToInteract = false;
    private static boolean isSprintAllowed = false;
    private static boolean isPlayerIdle = true;
    private static float currentMovementAngle = 0F;
    private static Vec3d currentMovementVector = Vec3d.ZERO;
    private static boolean isDetachedCameraEnabled = false;
    private static double mouseDeltaX = 0D;
    private static double prevMousePosX = 0D;
    private static long lastInteractionTime = System.currentTimeMillis();
    private static double savedMousePosX = 0D;
    private static double savedMousePosY = 0D;
    private static float oldCameraAngleYaw = 0F;
    private static float oldCameraAnglePitch = 0F;
    private static float currentCameraAngleYaw = 0F;
    private static float currentCameraAnglePitch = 0F;
    private static BlockPos highlightedBlockPos = null;


    public static boolean isCameraGrabbed() {
        return isCameraGrabbed;
    }

    public static void grabCamera() {
        isCameraGrabbed = true;
    }

    public static boolean isInter

    public static boolean isPlayerIdle() {
        return isPlayerIdle;
    }

    public static void setPlayerIdleState(boolean isPlayerIdle) {
        CamState.isPlayerIdle = isPlayerIdle;
    }

    public static Vec3d getMovementVector() {
        return
    }
}
