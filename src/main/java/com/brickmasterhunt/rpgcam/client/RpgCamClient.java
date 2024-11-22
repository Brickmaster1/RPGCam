package com.brickmasterhunt.rpgcam.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

import static com.brickmasterhunt.rpgcam.client.CameraUtils.getMouseBasedViewVector;

@Environment(EnvType.CLIENT)
public class RpgCamClient implements ClientModInitializer {
    public static final String MOD_ID = "rpgcam";
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static boolean isDetachedCameraEnabled = false;
    private static boolean isCameraGrabbed = false;
    private static final KeyBinding MOVE_CAMERA_FORWARD_KEY = createKeybinding("move_camera_forward", GLFW.GLFW_KEY_KP_ADD);
    private static final KeyBinding TOGGLE_CAMERA_KEY = createKeybinding("toggle_camera", GLFW.GLFW_KEY_F7);
    private static final KeyBinding GRAB_CAMERA_KEY = createKeybinding("grab_camera", GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
    private static final double PLAYER_CAMERA_DISTANCE = 7.0D;
    private static final double SENSITIVITY = 0.35D;
    private static final double INITIAL_CAMERA_ANGLE_XZ = 45.0D;
    private static final double INITIAL_CAMERA_ANGLE_Y = 45.0D;
    private static final float LERP_FACTOR = 0.25f;

    private static BlockPos highlightedBlockPos = null;

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(MOVE_CAMERA_FORWARD_KEY);
        KeyBindingHelper.registerKeyBinding(TOGGLE_CAMERA_KEY);
        KeyBindingHelper.registerKeyBinding(GRAB_CAMERA_KEY);

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        WorldRenderEvents.AFTER_ENTITIES.register(this::onWorldRender);
    }

    private void onWorldRender(WorldRenderContext context) {
        if (highlightedBlockPos != null) {
            Camera camera = context.camera();
            Vec3d camPos = camera.getPos();
            Box box = new Box(highlightedBlockPos).expand(0.002).offset(-camPos.x, -camPos.y, -camPos.z);

            MatrixStack matrixStack = context.matrixStack();
            VertexConsumerProvider.Immediate vertexConsumers = (VertexConsumerProvider.Immediate) context.consumers();

            // Use a VertexConsumer for the outline
            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());

            // Draw the box outline
            WorldRenderer.drawBox(
                    matrixStack,
                    vertexConsumer,
                    box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ,
                    1.0f, 1.0f, 1.0f, 1.0f
            );

            vertexConsumers.draw();
        }
    }

    private void onTick(MinecraftClient client) {
        if (TOGGLE_CAMERA_KEY.wasPressed()) {
            toggleDetachedCamera();
        }

        if (isDetachedCameraEnabled) {
            updateCamera();
            if (!isCameraGrabbed) {
                updatePlayerRotationToCursor(100.0D, client.player.isHolding(Items.BUCKET));
            }
        }
    }

    private void updateCamera() {
        if (client.player == null) return;

        handleCameraRotateInteraction();
    }

    private static void toggleDetachedCamera() {
        isDetachedCameraEnabled = !isDetachedCameraEnabled;

        if (isDetachedCameraEnabled) {
            client.mouse.unlockCursor();
            client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            Camera camera = client.gameRenderer.getCamera();

            // Initialize camera positioning (45 degrees, 5 meters away) (add config vals for everything later)

            Vec3d playerPos = client.player.getPos();

            camera.setPos(
                playerPos.x + (PLAYER_CAMERA_DISTANCE * Math.cos(Math.toRadians(INITIAL_CAMERA_ANGLE_XZ))),
                playerPos.y + (PLAYER_CAMERA_DISTANCE * Math.sin(Math.toRadians(INITIAL_CAMERA_ANGLE_Y))),
                playerPos.z + (PLAYER_CAMERA_DISTANCE * Math.sin(Math.toRadians(INITIAL_CAMERA_ANGLE_XZ)))
            );

            Vec3d direction = playerPos.subtract(camera.getPos()).normalize();
            double towardsPlayerYaw = Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90;
            double towardsPlayerPitch = Math.toDegrees(-Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z)));

            setCameraRotation((float) towardsPlayerYaw, (float) towardsPlayerPitch);
        } else {
            client.mouse.lockCursor();
            client.options.setPerspective(Perspective.FIRST_PERSON);
        }
    }

    private void updatePlayerRotationToCursor(double maxDistance, boolean lookThoughFluids) {
        assert client.world != null;
        assert client.cameraEntity != null;

        Vec3d cameraPosVector = client.gameRenderer.getCamera().getPos();

        Vec3d mouseBasedViewVector = getMouseBasedViewVector(client, client.mouse.getX(), client.mouse.getY());
        Vec3d farCameraVector = cameraPosVector.add(mouseBasedViewVector.x * maxDistance, mouseBasedViewVector.y * maxDistance, mouseBasedViewVector.z * maxDistance);

        HitResult result = client.world.raycast(
            new RaycastContext(
                    cameraPosVector,
                    farCameraVector,
                    RaycastContext.ShapeType.OUTLINE,
                    (!lookThoughFluids) ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE,
                    client.cameraEntity
            )
        );

        switch(result.getType()) {
            case MISS:
                //nothing near enough
                break;
            case BLOCK:
                BlockHitResult blockHit = (BlockHitResult) result;
                BlockPos blockPos = blockHit.getBlockPos();
                //client.player.sendMessage(Text.of("Block hit at: (" + blockPos.getX() + ", " + blockPos.getY() + ", " + blockPos.getZ() + ")"));
                highlightedBlockPos = blockPos;
                faceTarget(blockHit.withSide(blockHit.getSide()).getPos());
                break;
            case ENTITY:
                EntityHitResult entityHit = (EntityHitResult) result;
                Entity entity = entityHit.getEntity();
                break;
        }
    }

    private void faceTarget(Vec3d targetPos) {
        Vec3d playerPos = client.player.getPos();

        // Calculate direction vector from player to target
        Vec3d direction = targetPos.subtract(playerPos);

        // Calculate desired yaw and pitch
        double desiredYaw = Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90;

        // Calculate pitch based on y-difference and horizontal distance
        double horizontalDistance = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        double desiredPitch = Math.toDegrees(-Math.atan2(direction.y, horizontalDistance));

        float currentYaw = client.player.getYaw();
        float currentPitch = client.player.getPitch();

        // Correctly handle yaw wrapping to avoid spinning the long way around
        float yawDifference = (float) (desiredYaw - currentYaw);
        yawDifference = (yawDifference + 540) % 360 - 180; // Normalize to [-180, 180]

        // Interpolate yaw and pitch
        float newYaw = currentYaw + LERP_FACTOR * yawDifference;
        float newPitch = (float) (currentPitch + LERP_FACTOR * (desiredPitch - currentPitch));

        // Apply the updated yaw and pitch
        client.player.setYaw(newYaw);
        client.player.setPitch(newPitch);
    }


    private static double savedMousePosX = 0D;
    private static double savedMousePosY = 0D;
    private void handleCameraRotateInteraction() {
        Camera camera = client.gameRenderer.getCamera();

        if (GRAB_CAMERA_KEY.isPressed()) {
            if (!isCameraGrabbed) {
                isCameraGrabbed = true;
                savedMousePosX = client.mouse.getX();
                savedMousePosY = client.mouse.getY();
                client.mouse.lockCursor();
            }

            double mouseDeltaX = client.mouse.getX();
            double mouseDeltaY = client.mouse.getY();

            client.mouse.lockCursor();

            if (mouseDeltaX == 0D && mouseDeltaY == 0D) {
                return;
            }

            double newCameraAngleYaw = (SENSITIVITY * mouseDeltaX) + camera.getYaw();
            double newCameraAnglePitch = (SENSITIVITY * mouseDeltaY) + camera.getPitch();
            if(newCameraAnglePitch > 89.99D) { newCameraAnglePitch = 89.99D; }
            else if (newCameraAnglePitch < -89.99D) { newCameraAnglePitch = -89.99D; }

            // Allow camera to continuously spin without large angle
            float cameraAngleYawDifference = (float) (newCameraAngleYaw - camera.getYaw());
            newCameraAngleYaw = (cameraAngleYawDifference + 540) % 360 - 180; // Normalize to [-180, 180]

            Vec3d playerPos = client.player.getPos();

            double pitchRadians = Math.toRadians(newCameraAnglePitch);
            double yawRadians = Math.toRadians(newCameraAngleYaw);

            camera.setPos(
                playerPos.x + (PLAYER_CAMERA_DISTANCE * Math.cos(pitchRadians) * Math.cos(yawRadians)),
                playerPos.y + (PLAYER_CAMERA_DISTANCE * Math.sin(pitchRadians)),
                playerPos.z + (PLAYER_CAMERA_DISTANCE * Math.cos(pitchRadians) * Math.sin(yawRadians))
            );

            Vec3d direction = playerPos.subtract(camera.getPos()).normalize();
            double towardsPlayerYaw = Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90;
            double towardsPlayerPitch = Math.toDegrees(-Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z)));

            setCameraRotation((float) towardsPlayerYaw, (float) towardsPlayerPitch);
        } else if (isCameraGrabbed) {
            // Reset state when the key is released
            isCameraGrabbed = false;
            client.mouse.unlockCursor();
            GLFW.glfwSetCursorPos(client.getWindow().getHandle(), savedMousePosX, savedMousePosY);
            savedMousePosX = 0D;
            savedMousePosY = 0D;
        }
    }

    private static void setCameraRotation(float yaw, float pitch) {
        Camera camera = client.gameRenderer.getCamera();
        camera.pitch = pitch;
        camera.yaw = yaw;
        camera.getRotation().rotationYXZ(-camera.yaw * 0.017453292F, camera.pitch * 0.017453292F, 0.0F);
        camera.horizontalPlane.set(0.0F, 0.0F, 1.0F).rotate(camera.getRotation());
        camera.verticalPlane.set(0.0F, 1.0F, 0.0F).rotate(camera.getRotation());
        camera.diagonalPlane.set(1.0F, 0.0F, 0.0F).rotate(camera.getRotation());
    }

    public static boolean isDetachedCameraEnabled() {
        return isDetachedCameraEnabled;
    }

    public static boolean isCameraGrabbed() {
        return isCameraGrabbed;
    }

    private static KeyBinding createKeybinding(String name, int key) {
        return new KeyBinding(
                "rpgcam.key." + name,
                InputUtil.Type.KEYSYM,
                key,
                MOD_ID
        );
    }
}
