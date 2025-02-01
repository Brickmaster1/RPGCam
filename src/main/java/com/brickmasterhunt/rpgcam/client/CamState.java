package com.brickmasterhunt.rpgcam.client;

import com.brickmasterhunt.rpgcam.client.config.CamConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2d;
import org.lwjgl.glfw.GLFW;

public class CamState {
    public static final MinecraftClient client = MinecraftClient.getInstance();

    public static CamConfig CONFIG = null;

    public static final KeyBinding GRAB_CAMERA_KEY = RpgCam.createKeybinding("grab_camera", GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
    public static final KeyBinding MOVE_CAMERA_FORWARD_KEY = RpgCam.createKeybinding("move_camera_forward", GLFW.GLFW_KEY_KP_ADD);
    public static final KeyBinding TOGGLE_CAMERA_KEY = RpgCam.createKeybinding("toggle_camera", GLFW.GLFW_KEY_F7);

    private static boolean isDetachedCameraEnabled = false;
    private static boolean isCameraGrabbed = false;
    private static boolean needsToInteract = false;
    private static boolean isInteracting = false;
    private static boolean isPlayerIdle = true;
    private static float currentMovementAngle = 0F;
    private static double mouseDeltaX = 0D;
    private static double mouseDeltaY = 0D;
    private static double prevMousePosX = 0D;
    private static double savedMousePosX = 0D;
    private static double savedMousePosY = 0D;
    private static Vec3d currentMovementVector = Vec3d.ZERO;
    private static BlockPos highlightedBlockPos = null;


    public static boolean isDetachedCameraEnabled() {
        return isDetachedCameraEnabled;
    }

    public static void onToggleDetachedCamera() {
        isDetachedCameraEnabled = !isDetachedCameraEnabled;
    }

    public static boolean isCameraGrabbed() {
        return isCameraGrabbed;
    }

    public static void evaluateCameraGrabState() {
        if (!isCameraGrabbed && GRAB_CAMERA_KEY.isPressed()) {
            isCameraGrabbed = true;
            savedMousePosX = client.mouse.getX();
            savedMousePosY = client.mouse.getY();
            client.mouse.lockCursor();
        } else if (isCameraGrabbed && !GRAB_CAMERA_KEY.isPressed()) {
            // Set mouse back to where user left it before interaction
            var savedMousePos = getMousePos();
            client.mouse.unlockCursor();
            GLFW.glfwSetCursorPos(client.getWindow().getHandle(), savedMousePos.x, savedMousePos.y);
            isCameraGrabbed = false;
        }
    }

    public static boolean shouldStartInteraction() {
        return needsToInteract;
    }

    public static void shouldStartInteraction(boolean interactionState) {
        needsToInteract = interactionState;
    }

    public static void setPlayerInteractionState(boolean interactionState) {
         isInteracting = interactionState;
    }

    public static boolean isPlayerIdle() {
        return isPlayerIdle;
    }

    public static void setPlayerIdleState(boolean isPlayerIdle) {
        CamState.isPlayerIdle = isPlayerIdle;
    }

    public static Vec3d getRelativeMovementVector() {
        return currentMovementVector;
    }

    public static float getRelativeMovementAngle() {
        return currentMovementAngle;
    }

    public static void onRelativeMovement(Vec3d vector, float angle) {
        currentMovementVector = vector;
        currentMovementAngle = angle;
    }

    public static float getCameraPitch() {
        return client.gameRenderer.getCamera().pitch;
    }

    public static float getCameraYaw() {
        return client.gameRenderer.getCamera().yaw;
    }

    public static void setCameraRotation(float pitch, float yaw) {
        Camera camera = client.gameRenderer.getCamera();
        camera.pitch = pitch;
        camera.yaw = yaw;
        camera.getRotation().rotationYXZ(-camera.yaw * 0.017453292F, camera.pitch * 0.017453292F, 0.0F);
        camera.horizontalPlane.set(0.0F, 0.0F, 1.0F).rotate(camera.getRotation());
        camera.verticalPlane.set(0.0F, 1.0F, 0.0F).rotate(camera.getRotation());
        camera.diagonalPlane.set(1.0F, 0.0F, 0.0F).rotate(camera.getRotation());
    }

    public static void setRadialCameraAngle(Vec3d center) {
        Vec3d cameraPos = getCameraPosition();

        Vec3d direction = center.subtract(cameraPos).normalize();
        double towardsCenterPitch = Math.toDegrees(-Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z)));
        double towardsCenterYaw = Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0f;

        setCameraRotation((float) towardsCenterPitch, (float) towardsCenterYaw);
    }

    public static Vec3d getCameraPosition() {
        if (client.cameraEntity == null) { return null; }

        return client.gameRenderer.getCamera().getPos();
    }

    public static void setCameraPosition(Vec3d pos) {
        client.gameRenderer.getCamera().setPos(pos.x, pos.y, pos.z);
    }

    public static void setRadialCameraPosition(Vec3d center, float distance, float pitch, float yaw) {
        double pitchRadians = Math.toRadians(pitch);
        double yawRadians = Math.toRadians(yaw);

        setCameraPosition(new Vec3d(
            center.x + (distance * Math.cos(pitchRadians) * Math.cos(yawRadians)),
            center.y + (distance * Math.sin(pitchRadians)),
            center.z + (distance * Math.cos(pitchRadians) * Math.sin(yawRadians))
            )
        );
    }

    public static Vector2d getMousePos() {
        if (isCameraGrabbed) {
            return new Vector2d(savedMousePosX, savedMousePosY);
        }

        return new Vector2d(client.mouse.getX(), client.mouse.getY());
    }

    public static Vector2d getMouseRelativeDelta() {
        return new Vector2d(mouseDeltaX, mouseDeltaY);
    }

    public static void onMouseRelativeMove(double y) {
        double mousePosX = (client.mouse.getX() - ((double) client.getWindow().getWidth() / 2));
        mouseDeltaX = mousePosX - prevMousePosX;
        mouseDeltaY = y;

        prevMousePosX = mousePosX;
        System.out.println("mousePosX" + mousePosX);
        System.out.println("prevMousePosX" + prevMousePosX);
        System.out.println("mouseDeltaX" + mouseDeltaX);
    }

    public static BlockPos getHighlightedBlockPos() {
        return highlightedBlockPos;
    }

    public static void highlightBlockAt(BlockPos pos) {
        highlightedBlockPos = pos;
    }
}
