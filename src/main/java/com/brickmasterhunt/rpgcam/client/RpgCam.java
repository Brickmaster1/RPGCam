package com.brickmasterhunt.rpgcam.client;

import com.brickmasterhunt.rpgcam.client.movement.RelativeMovement;
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
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

import static com.brickmasterhunt.rpgcam.client.camera.CameraUtils.getMouseBasedViewVector;

@Environment(EnvType.CLIENT)
public class RpgCam implements ClientModInitializer {
    public static final String MOD_ID = "rpgcam";


    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(CamState.MOVE_CAMERA_FORWARD_KEY);
        KeyBindingHelper.registerKeyBinding(CamState.TOGGLE_CAMERA_KEY);
        KeyBindingHelper.registerKeyBinding(CamState.GRAB_CAMERA_KEY);

        ClientTickEvents.END_CLIENT_TICK.register(this::onTickEnd);

        WorldRenderEvents.AFTER_ENTITIES.register(this::onWorldRender);
    }

    private void onWorldRender(WorldRenderContext context) {
        if (CamState.highlightedBlockPos != null) {
            Camera camera = context.camera();
            Vec3d camPos = camera.getPos();
            Box box = new Box(CamState.highlightedBlockPos).expand(0.002).offset(-camPos.x, -camPos.y, -camPos.z);

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

    private void onTickEnd(MinecraftClient client) {
        if (CamState.TOGGLE_CAMERA_KEY.wasPressed()) {
            toggleDetachedCamera();
        }

        if (CamState.isDetachedCameraEnabled) {
            if (CamState.GRAB_CAMERA_KEY.isPressed()) {
                if (!CamState.isCameraGrabbed) {
                    CamState.isCameraGrabbed = true;
                    CamState.savedMousePosX = client.mouse.getX();
                    CamState.savedMousePosY = client.mouse.getY();

                    client.mouse.lockCursor();
                }
            }

            updateInteractionStatus();

            if (isInteracting()) {
                if (!CamState.isCameraGrabbed) {
                    updatePlayerRotationToCursor(CamState.RAYCAST_MAX_DISTANCE, client.player.isHolding(Items.BUCKET), true);
                } else {
                    updatePlayerRotationToCursor(CamState.RAYCAST_MAX_DISTANCE, client.player.isHolding(Items.BUCKET), true, CamState.savedMousePosX, CamState.savedMousePosY);
                }
            } else {
                double mouseX;
                double mouseY;

                if (CamState.isCameraGrabbed) {
                    mouseX = CamState.savedMousePosX;
                    mouseY = CamState.savedMousePosY;
                } else {
                    mouseX = client.mouse.getX();
                    mouseY = client.mouse.getY();
                }

                HitResult raycast = raycastAtCursor(client.player.isHolding(Items.BUCKET), CamState.RAYCAST_MAX_DISTANCE, mouseX, mouseY);
                if (raycast.getType() == HitResult.Type.BLOCK) {
                    CamState.highlightedBlockPos = ((BlockHitResult) raycast).getBlockPos();
                }

                RelativeMovement.handleCameraRelativeMovement(CamState.PLAYER_ROTATE_SPEED, CamState.ROTATE_LERP_FACTOR);
            }
        }
    }

    private static void toggleDetachedCamera() {
        CamState.isDetachedCameraEnabled = !CamState.isDetachedCameraEnabled;

        if (CamState.isDetachedCameraEnabled) {
            CamState.client.mouse.unlockCursor();
            CamState.client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            Camera camera = CamState.client.gameRenderer.getCamera();

            // Initialize camera positioning (45 degrees, 5 meters away) (add config vals for everything later)

            Vec3d playerPos = CamState.client.player.getPos();

            camera.setPos(
                playerPos.x + (CamState.PLAYER_CAMERA_DISTANCE * Math.cos(Math.toRadians(CamState.INITIAL_CAMERA_ANGLE_XZ))),
                playerPos.y + (CamState.PLAYER_CAMERA_DISTANCE * Math.sin(Math.toRadians(CamState.INITIAL_CAMERA_ANGLE_Y))),
                playerPos.z + (CamState.PLAYER_CAMERA_DISTANCE * Math.sin(Math.toRadians(CamState.INITIAL_CAMERA_ANGLE_XZ)))
            );

            CamState.oldCameraAngleYaw = CamState.INITIAL_CAMERA_ANGLE_XZ;
            CamState.oldCameraAnglePitch = CamState.INITIAL_CAMERA_ANGLE_Y;
            CamState.currentCameraAngleYaw = CamState.INITIAL_CAMERA_ANGLE_XZ;
            CamState.currentCameraAnglePitch = CamState.INITIAL_CAMERA_ANGLE_Y;

            Vec3d direction = playerPos.subtract(camera.getPos()).normalize();
            double towardsPlayerYaw = Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0f;
            double towardsPlayerPitch = Math.toDegrees(-Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z)));

            setCameraRotation((float) towardsPlayerYaw, (float) towardsPlayerPitch);
        } else {
            CamState.client.mouse.lockCursor();
            CamState.client.options.setPerspective(Perspective.FIRST_PERSON);
        }
    }

    private void updatePlayerRotationToCursor(double maxDistance, boolean lookThroughFluids, boolean includeEntities) {
        updatePlayerRotationToCursor(maxDistance, lookThroughFluids, includeEntities, CamState.client.mouse.getX(), CamState.client.mouse.getY());
    }

    private void updatePlayerRotationToCursor(double maxDistance, boolean lookThoughFluids, boolean includeEntities, double mouseX, double mouseY) {
        HitResult raycast = null;

        if (includeEntities) {
            raycast = raycastEntity(CamState.client.player, maxDistance);
        }

        if (raycast == null) {
            raycast = raycastAtCursor(lookThoughFluids, maxDistance, mouseX, mouseY);
        }

        switch(raycast.getType()) {
            case MISS:
                //nothing near enough
                break;
            case ENTITY:
                EntityHitResult entityHit = (EntityHitResult) raycast;
                Entity entity = entityHit.getEntity();
                RelativeMovement.lerpPlayerLookAt(EntityAnchorArgumentType.EntityAnchor.EYES, entityHit.getEntity().getPos(), CamState.LOOK_CURSOR_LERP_FACTOR);
                break;
            case BLOCK:
                BlockHitResult blockHit = (BlockHitResult) raycast;
                BlockPos blockPos = blockHit.getBlockPos();
                //client.player.sendMessage(Text.of("Block hit at: (" + blockPos.getX() + ", " + blockPos.getY() + ", " + blockPos.getZ() + ")"));
                CamState.highlightedBlockPos = blockPos;
                RelativeMovement.lerpPlayerLookAt(EntityAnchorArgumentType.EntityAnchor.EYES, blockHit.withSide(blockHit.getSide()).getPos(), CamState.LOOK_CURSOR_LERP_FACTOR);
                break;
        }
    }

    private HitResult raycastAtCursor(boolean throughFluids, double maxDistance, double mouseX, double mouseY) {
        assert CamState.client.world != null;
        assert CamState.client.cameraEntity != null;

        Vec3d cameraPosVector = CamState.client.gameRenderer.getCamera().getPos();
        Vec3d mouseBasedViewVector = getMouseBasedViewVector(CamState.client, mouseX, mouseY);
        Vec3d farCameraVector = cameraPosVector.add(mouseBasedViewVector.x * maxDistance, mouseBasedViewVector.y * maxDistance, mouseBasedViewVector.z * maxDistance);

        HitResult result = CamState.client.world.raycast(
            new RaycastContext(
                    cameraPosVector,
                    farCameraVector,
                    RaycastContext.ShapeType.OUTLINE,
                    (!throughFluids) ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE,
                    CamState.client.cameraEntity
            )
        );

        return result;
    }

    public static HitResult raycastEntity(PlayerEntity player, double maxDistance) {
        Entity cameraEntity = MinecraftClient.getInstance().cameraEntity;
        if (cameraEntity != null) {
            Vec3d cameraPos = player.getCameraPosVec(1.0f);
            Vec3d rot = player.getRotationVec(1.0f);
            Vec3d rayCastContext = cameraPos.add(rot.x * maxDistance, rot.y * maxDistance, rot.z * maxDistance);
            Box box = cameraEntity.getBoundingBox().stretch(rot.multiply(maxDistance)).expand(1d, 1d, 1d);
            return ProjectileUtil.raycast(cameraEntity, cameraPos, rayCastContext, box, (entity -> /* any custom parameters here */ !entity.isSpectator() && entity.canHit()), maxDistance);
        }
        return null;
    }

    private boolean isInteracting() {
        // Returns true if the player has interacted recently
        return System.currentTimeMillis() - CamState.lastInteractionTime < CamState.NOT_INTERACTING_WAIT_TIME;
    }

    private void updateInteractionStatus() {
        if ((!CamState.client.options.attackKey.wasPressed() && CamState.client.options.attackKey.isPressed()) ||
            (!CamState.client.options.useKey.wasPressed() && CamState.client.options.useKey.isPressed())
        ) {
            CamState.needsToInteract = true;
        }

        if (CamState.client.options.attackKey.isPressed() ||
            CamState.client.options.useKey.isPressed()
        ) {
            CamState.lastInteractionTime = System.currentTimeMillis();
        }
    }

    private static void setCameraRotation(float yaw, float pitch) {
        Camera camera = CamState.client.gameRenderer.getCamera();
        camera.pitch = pitch;
        camera.yaw = yaw;
        camera.getRotation().rotationYXZ(-camera.yaw * 0.017453292F, camera.pitch * 0.017453292F, 0.0F);
        camera.horizontalPlane.set(0.0F, 0.0F, 1.0F).rotate(camera.getRotation());
        camera.verticalPlane.set(0.0F, 1.0F, 0.0F).rotate(camera.getRotation());
        camera.diagonalPlane.set(1.0F, 0.0F, 0.0F).rotate(camera.getRotation());
    }

    public static Vec2f getCameraRotation() {
        return new Vec2f(CamState.currentCameraAngleYaw, CamState.currentCameraAnglePitch);
    }

    public static boolean isDetachedCameraEnabled() {
        return CamState.isDetachedCameraEnabled;
    }

    public static boolean isCameraGrabbed() {
        return CamState.isCameraGrabbed;
    }

    static KeyBinding createKeybinding(String name, int key) {
        return new KeyBinding(
                "rpgcam.key." + name,
                InputUtil.Type.KEYSYM,
                key,
                MOD_ID
        );
    }
}
