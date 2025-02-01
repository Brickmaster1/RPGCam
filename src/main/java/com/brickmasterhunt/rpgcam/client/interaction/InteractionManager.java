package com.brickmasterhunt.rpgcam.client.interaction;

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
import static com.brickmasterhunt.rpgcam.client.player.movement.RelativeMovement.*;

public class InteractionManager {
    private static long lastInteractionTime = System.currentTimeMillis();

    public static boolean isPlayerInteracting() {
        return System.currentTimeMillis() - lastInteractionTime < CONFIG.NOT_INTERACTING_WAIT_TIME;
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
            setRadialCameraPosition(playerPos, CONFIG.INITIAL_PLAYER_CAMERA_DISTANCE, CONFIG.INITIAL_CAMERA_ANGLE_Y, CONFIG.INITIAL_CAMERA_ANGLE_XZ);
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
                lerpPlayerLookAt(EntityAnchorArgumentType.EntityAnchor.EYES, entityHit.getEntity().getPos(), CONFIG.PLAYER_INTERACT_FOLLOW_CURSOR_LERP);
                break;
            case BLOCK:
                BlockHitResult blockHit = (BlockHitResult) raycast;
                BlockPos blockPos = blockHit.getBlockPos();
                highlightBlockAt(blockPos);
                lerpPlayerLookAt(EntityAnchorArgumentType.EntityAnchor.EYES, blockHit.withSide(blockHit.getSide()).getPos(), CONFIG.PLAYER_INTERACT_FOLLOW_CURSOR_LERP);
                break;
        }
    }

    public static void handleCameraRotateInteraction() {
        if (isCameraGrabbed()) {
            var mouseDelta = getMouseRelativeDelta();

            if (Math.abs(mouseDelta.x) < 1.0d && Math.abs(mouseDelta.y) < 1.0d) {
                return;
            }

            //System.out.println("mouseDelta with sensitivity " + mouseDelta.x * CONFIG.GRAB_CAMERA_MOUSE_SENSITIVITY);

            var newCameraAngleYaw = MathHelper.wrapDegrees(
                ((float) mouseDelta.x * CONFIG.GRAB_CAMERA_MOUSE_SENSITIVITY) + getCameraYaw()
            );
            var newCameraAnglePitch = MathHelper.clamp(
                ((float) mouseDelta.y * CONFIG.GRAB_CAMERA_MOUSE_SENSITIVITY) + getCameraPitch(),
                -89.99f,
                89.99f
            );

            Vec3d playerPos = client.player.getPos();

            setRadialCameraPosition(playerPos, CONFIG.INITIAL_PLAYER_CAMERA_DISTANCE, newCameraAnglePitch, newCameraAngleYaw);
            setRadialCameraAngle(playerPos);
        }
    }
    
    public static void onInteractionStart() {

    }
}
