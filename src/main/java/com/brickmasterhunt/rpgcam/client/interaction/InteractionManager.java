package com.brickmasterhunt.rpgcam.client.interaction;

import com.brickmasterhunt.rpgcam.client.player.movement.RelativeMovement;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import static com.brickmasterhunt.rpgcam.client.CamState.*;
import static com.brickmasterhunt.rpgcam.client.camera.raycast.RaycastUtils.*;

public class InteractionManager {
    private static long lastInteractionTime = System.currentTimeMillis();

    public static boolean isPlayerInteracting() {
        return System.currentTimeMillis() - lastInteractionTime < NOT_INTERACTING_WAIT_TIME;
    }

    public static void updatePlayerInteractionStatus() {
        if ((!client.options.attackKey.wasPressed() && client.options.attackKey.isPressed()) ||     // Maybe make a configurable list of actions here to consider "interacting"?
            (!client.options.useKey.wasPressed() && client.options.useKey.isPressed()) ||
            (!client.options.pickItemKey.wasPressed() && client.options.pickItemKey.isPressed()) ||
            (!client.options.dropKey.wasPressed() && client.options.dropKey.isPressed())
        ) {
            shouldStartInteraction(true);
        }

        if (client.options.attackKey.isPressed() ||
                client.options.useKey.isPressed()
        ) {
            lastInteractionTime = System.currentTimeMillis();
        }
    }

    public static void toggleDetachedCamera() {
        onToggleDetachedCamera();

        if (isDetachedCameraEnabled()) {
            client.mouse.unlockCursor();
            client.options.setPerspective(Perspective.THIRD_PERSON_BACK);

            Vec3d playerPos = client.player.getPos();

            // Initialize camera positioning (45 degrees, 5 meters away) (add config vals for everything later)
            setRadialCameraPosition(playerPos, PLAYER_CAMERA_DISTANCE, INITIAL_CAMERA_ANGLE_Y, INITIAL_CAMERA_ANGLE_XZ);
            setRadialCameraAngle(playerPos);
        } else {
            client.mouse.lockCursor();
            client.options.setPerspective(Perspective.FIRST_PERSON);
        }
    }

    public static void updatePlayerRotationToCursor(double maxDistance, boolean lookThoughFluids, boolean includeEntities, double mouseX, double mouseY) {
        HitResult raycast = raycastAtCursor(lookThoughFluids, includeEntities, maxDistance, mouseX, mouseY);

        switch(raycast.getType()) {
            case MISS:
                //nothing near enough
                break;
            case ENTITY:
                EntityHitResult entityHit = (EntityHitResult) raycast;
                Entity entity = entityHit.getEntity();
                RelativeMovement.lerpPlayerLookAt(EntityAnchorArgumentType.EntityAnchor.EYES, entityHit.getEntity().getPos(), LOOK_CURSOR_LERP_FACTOR);
                break;
            case BLOCK:
                BlockHitResult blockHit = (BlockHitResult) raycast;
                BlockPos blockPos = blockHit.getBlockPos();
                highlightBlockAt(blockPos);
                RelativeMovement.lerpPlayerLookAt(EntityAnchorArgumentType.EntityAnchor.EYES, blockHit.withSide(blockHit.getSide()).getPos(), LOOK_CURSOR_LERP_FACTOR);
                break;
        }
    }

    public static void handleCameraRotateInteraction() {
        Camera camera = client.gameRenderer.getCamera();
        if (GRAB_CAMERA_KEY.isPressed()) {
            var mouseDelta = getMouseRelativeDelta();

            if (Math.abs(mouseDelta.x) <= 1.0d && Math.abs(mouseDelta.y) <= 1.0d) {
                return;
            }

            var newCameraAngleYaw = MathHelper.wrapDegrees(
                    ((float) mouseDelta.x * SENSITIVITY) + getCameraYaw()
            );
            var newCameraAnglePitch = MathHelper.clamp(
                    ((float) mouseDelta.y * SENSITIVITY) + getCameraPitch(),
                    -89.99f,
                    89.99f
            );

            Vec3d playerPos = client.player.getPos();

            setRadialCameraPosition(playerPos, PLAYER_CAMERA_DISTANCE, newCameraAnglePitch, newCameraAngleYaw);
            setRadialCameraAngle(playerPos);
        } else if (isCameraGrabbed()) {
            // Set mouse back to where user left it before interaction
            var savedMousePos = getMousePos();
            onToggleGrabbedCamera();
            client.mouse.unlockCursor();
            GLFW.glfwSetCursorPos(client.getWindow().getHandle(), savedMousePos.x, savedMousePos.y);
        }
    }
    
    public static void onInteractionStart() {

    }
}
