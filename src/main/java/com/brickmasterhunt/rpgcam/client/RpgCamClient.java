package com.brickmasterhunt.rpgcam.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

import static com.brickmasterhunt.rpgcam.client.CameraUtils.getMouseBasedViewVector;

@Environment(EnvType.CLIENT)
public class RpgCamClient implements ClientModInitializer {
    public static final String MOD_ID = "rpgcam";
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static boolean isDetachedCameraEnabled = false;
    private static boolean isCameraGrabbed = false;
    private  static boolean initialDelta = false;
    private static double savedMousePosX = 0D;
    private static double savedMousePosY = 0D;
    static double mouseDeltaX = 0.0D;
    static double prevMousePosX = 0.0D;
    private static float prevCameraAngleYaw = 0F;
    private static float prevCameraAnglePitch = 0F;
    private static final KeyBinding MOVE_CAMERA_FORWARD_KEY = createKeybinding("move_camera_forward", GLFW.GLFW_KEY_KP_ADD);
    private static final KeyBinding TOGGLE_CAMERA_KEY = createKeybinding("toggle_camera", GLFW.GLFW_KEY_F7);
    public static final KeyBinding GRAB_CAMERA_KEY = createKeybinding("grab_camera", GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
    private static final double PLAYER_CAMERA_DISTANCE = 7.0D;
    private static final float SENSITIVITY = 0.15F;
    private static final float INITIAL_CAMERA_ANGLE_XZ = 45.0F;
    private static final float INITIAL_CAMERA_ANGLE_Y = 45.0F;
    private static final float LERP_FACTOR = 5.0F;
    private static final double RAYCAST_MAX_DISTANCE = 100.0D;

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
            if (GRAB_CAMERA_KEY.isPressed()) {
                if (!isCameraGrabbed) {
                    isCameraGrabbed = true;
                    initialDelta = true;
                    savedMousePosX = client.mouse.getX();
                    savedMousePosY = client.mouse.getY();

                    client.mouse.lockCursor();
                }
            }

            if (!isCameraGrabbed) {
                updatePlayerRotationToCursor(RAYCAST_MAX_DISTANCE, client.player.isHolding(Items.BUCKET));
            }
        }
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

            prevCameraAngleYaw = INITIAL_CAMERA_ANGLE_XZ;
            prevCameraAnglePitch = INITIAL_CAMERA_ANGLE_Y;

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

    private static void faceTarget(Vec3d targetPos) {
        //assert client.player != null;
        //ClientPlayerEntity player = client.player;

//        Vec3d vec3d = EntityAnchorArgumentType.EntityAnchor.EYES.positionAt(player);
//        double d = targetPos.x - vec3d.x;
//        double e = targetPos.y - vec3d.y;
//        double f = targetPos.z - vec3d.z;
//        double g = Math.sqrt(d * d + f * f);
//
//        float desiredPitch = MathHelper.wrapDegrees((float)(-(MathHelper.atan2(e, g) * 57.2957763671875)));
//        float desiredYaw = MathHelper.wrapDegrees((float)(MathHelper.atan2(f, d) * 57.2957763671875) - 90.0F);
//
//        // Interpolate yaw and pitch
//        player.setYaw(MathHelper.lerp(client.getTickDelta(), player.getYaw(), desiredYaw));
//        player.setPitch(MathHelper.lerp(client.getTickDelta(), player.getPitch(), desiredPitch));
//
//        player.setHeadYaw(player.getYaw());
//        player.prevPitch = player.getPitch();
//        player.prevYaw = player.getYaw();
//
//        player.prevHeadYaw = player.headYaw;
//        player.bodyYaw = player.headYaw;
//        player.prevBodyYaw = player.bodyYaw;
        playerLookAt(EntityAnchorArgumentType.EntityAnchor.EYES, targetPos);
    }

    private static void playerLookAt(EntityAnchorArgumentType.EntityAnchor anchorPoint, Vec3d target) {
        assert client.player != null;
        ClientPlayerEntity player = client.player;

        Vec3d vec3d = anchorPoint.positionAt(player);
        double d = target.x - vec3d.x;
        double e = target.y - vec3d.y;
        double f = target.z - vec3d.z;
        double g = Math.sqrt(d * d + f * f);

        float pitchToSet = MathHelper.wrapDegrees((float)(-(MathHelper.atan2(e, g) * 57.2957763671875)));
        float yawToSet = MathHelper.wrapDegrees((float)(MathHelper.atan2(f, d) * 57.2957763671875) - 90.0F);

        player.setPitch(MathHelper.lerpAngleDegrees(MathHelper.clamp(LERP_FACTOR * client.getTickDelta(), 0.0F, 1.0F), player.getPitch(), pitchToSet));
        player.setYaw(MathHelper.lerpAngleDegrees(MathHelper.clamp(LERP_FACTOR * client.getTickDelta(), 0.0F, 1.0F), player.getYaw(), yawToSet));
        player.setHeadYaw(player.getYaw());
        player.prevPitch = player.getPitch();
        player.prevYaw = player.getYaw();

        player.prevHeadYaw = player.headYaw;
        player.bodyYaw = player.headYaw;
        player.prevBodyYaw = player.bodyYaw;
    }

    public static void handleCameraRotateInteraction(double mouseDeltaY) {
        Camera camera = client.gameRenderer.getCamera();
        if (GRAB_CAMERA_KEY.isPressed()) {
            double mousePosX = (client.mouse.getX() - ((double) client.getWindow().getWidth() / 2));
            mouseDeltaX = mousePosX - prevMousePosX;
//            if (initialDelta) {
//                mouseDeltaX = client.mouse.getX() - ((double) client.getWindow().getWidth() / 2);
//                if (client.mouse.getX() >= ((double) client.getWindow().getWidth() / 2) + 1.0 ||
//                    client.mouse.getX() <= ((double) client.getWindow().getWidth() / 2) - 1.0)
//                {
//                    mouseDeltaX = client.mouse.getX() - ((double) client.getWindow().getWidth() / 2);
//                    initialDelta = false;
//                } else {
//                    System.out.println("prevCameraAngleYaw: " + prevCameraAngleYaw);
//                }
//            }

            if (Math.abs(mouseDeltaX) <= 1.0D && Math.abs(mouseDeltaY) <= 1.0D) {
                return;
            }

            float newCameraAngleYaw = ((float) mouseDeltaX * SENSITIVITY) + prevCameraAngleYaw;
            System.out.println("newCameraAngleYaw: " + newCameraAngleYaw);
            float newCameraAnglePitch = ((float) mouseDeltaY * SENSITIVITY) + prevCameraAnglePitch;
            newCameraAnglePitch = MathHelper.clamp(newCameraAnglePitch, -89.99F, 89.99F);

            //newCameraAngleYaw = newCameraAngleYaw - prevCameraAngleYaw;
            //newCameraAngleYaw = (cameraAngleYawDifference + 540) % 360 - 180; // Normalize to [-180, 180]
            newCameraAngleYaw = MathHelper.wrapDegrees(newCameraAngleYaw);

            Vec3d playerPos = client.player.getPos();

            double pitchRadians = Math.toRadians(newCameraAnglePitch);
            double yawRadians = Math.toRadians(newCameraAngleYaw);

            camera.setPos(
                playerPos.x + (PLAYER_CAMERA_DISTANCE * Math.cos(pitchRadians) * Math.cos(yawRadians)),
                playerPos.y + (PLAYER_CAMERA_DISTANCE * Math.sin(pitchRadians)),
                playerPos.z + (PLAYER_CAMERA_DISTANCE * Math.cos(pitchRadians) * Math.sin(yawRadians))
            );

            prevCameraAngleYaw = newCameraAngleYaw;
            prevCameraAnglePitch = newCameraAnglePitch;

            prevMousePosX = mousePosX;

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
            prevMousePosX = 0D;
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
