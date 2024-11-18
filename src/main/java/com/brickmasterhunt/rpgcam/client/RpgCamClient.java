package com.brickmasterhunt.rpgcam.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import static com.brickmasterhunt.rpgcam.client.RaycastUtils.map;
import static com.brickmasterhunt.rpgcam.client.RaycastUtils.raycastInDirection;

@Environment(EnvType.CLIENT)
public class RpgCamClient implements ClientModInitializer {
    public static final String MOD_ID = "rpgcam";
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static boolean isDetachedCameraEnabled = false;
    private static final KeyBinding MOVE_CAMERA_FORWARD_KEY = createKeybinding("move_camera_forward", GLFW.GLFW_KEY_W);
    private static KeyBinding TOGGLE_CAMERA_KEY = createKeybinding("toggle_camera", GLFW.GLFW_KEY_F7);

    // Initialize keybinding for toggling camera
    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(MOVE_CAMERA_FORWARD_KEY);
        KeyBindingHelper.registerKeyBinding(TOGGLE_CAMERA_KEY);

        // Ensure the logic is checked every tick
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    // This method is called every game tick (update loop)
    private void onTick(MinecraftClient client) {
        if (TOGGLE_CAMERA_KEY.wasPressed()) {
            toggleDetachedCamera();
        }

        if (isDetachedCameraEnabled) {
            updateCamera();
            updatePlayerRotationToCursor();
        }
    }

    // Method to toggle detached camera
    private static void toggleDetachedCamera() {
        isDetachedCameraEnabled = !isDetachedCameraEnabled;

        if (isDetachedCameraEnabled) {
            client.mouse.unlockCursor();
            client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        } else {
            client.mouse.lockCursor();
            client.options.setPerspective(Perspective.FIRST_PERSON);
        }
    }

    private void updatePlayerRotationToCursor() {
        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        assert client.cameraEntity != null;
        Vec3d cameraDirection = client.cameraEntity.getRotationVec(client.getTickDelta());
        double fov = client.options.getFov().getValue();
        double angleSize = fov/height;
        Vector3f verticalRotationAxis = cameraDirection.toVector3f();
        verticalRotationAxis.cross(client.gameRenderer.getCamera().getVerticalPlane());

        Vector3f horizontalRotationAxis = cameraDirection.toVector3f();
        horizontalRotationAxis.cross(verticalRotationAxis);
        horizontalRotationAxis.normalize();

        verticalRotationAxis = cameraDirection.toVector3f();
        verticalRotationAxis.cross(horizontalRotationAxis);

        Vec3d direction = map(
                (float) angleSize,
                cameraDirection,
                horizontalRotationAxis,
                verticalRotationAxis,
                (int) client.mouse.getX(),
                (int) client.mouse.getY(),
                width,
                height
        );
        HitResult hit = raycastInDirection(client, client.getTickDelta(), direction);

        switch(hit.getType()) {
            case MISS:
                //nothing near enough
                break;
            case BLOCK:
                BlockHitResult blockHit = (BlockHitResult) hit;
                BlockPos blockPos = blockHit.getBlockPos();
                BlockState blockState = client.world.getBlockState(blockPos);
                Block block = blockState.getBlock();
                break;
            case ENTITY:
                EntityHitResult entityHit = (EntityHitResult) hit;
                Entity entity = entityHit.getEntity();
                break;
        }

        // Update player rotation based on the calculated direction
        if (hit.getType() == HitResult.Type.BLOCK || hit.getType() == HitResult.Type.ENTITY) {
            // Adjust player's yaw and pitch based on the direction to the target
            Vec3d targetPosition = hit.getPos();
            double deltaX = targetPosition.x - client.player.getX();
            double deltaY = targetPosition.y - client.player.getY();
            double deltaZ = targetPosition.z - client.player.getZ();

            // Calculate new yaw and pitch based on target's position
            float newYaw = (float) (Math.atan2(deltaZ, deltaX) * (180 / Math.PI)) - 90.0F;
            float newPitch = (float) (Math.atan2(deltaY, Math.sqrt(deltaX * deltaX + deltaZ * deltaZ)) * (180 / Math.PI));

            // Apply the rotation to the player
            client.player.setYaw(newYaw);
            client.player.setPitch(newPitch);
        }
    }

    // Method to update the camera position and rotation (client-side)
    private static void updateCamera() {
        // Access the current camera
        if (client.player == null) return;  // Ensure we have a player to work with

        var camera = client.gameRenderer.getCamera();

        // Update camera movement
        if (MOVE_CAMERA_FORWARD_KEY.isPressed()) {
            // Calculate new camera position based on movement
            Vec3d cameraPos = camera.getPos().add(new Vec3d(0, 0, -0.1f).rotateY(camera.getYaw()));
            camera.setPos(cameraPos.x, cameraPos.y, cameraPos.z);
        }

        // Update the camera's rotation (yaw, pitch)
        camera.setRotation(camera.getYaw(), camera.getPitch());
    }

    // Get the current state of the detached camera mode
    public static boolean isDetachedCameraEnabled() {
        return isDetachedCameraEnabled;
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
