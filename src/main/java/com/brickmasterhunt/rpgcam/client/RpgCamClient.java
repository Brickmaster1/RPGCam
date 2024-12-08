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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;

import static com.brickmasterhunt.rpgcam.client.CameraUtils.getMouseBasedViewVector;

@Environment(EnvType.CLIENT)
public class RpgCamClient implements ClientModInitializer {
    public static final String MOD_ID = "rpgcam";
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static boolean isDetachedCameraEnabled = false;
    private static boolean isCameraGrabbed = false;
    private static long lastInteractionTime = System.currentTimeMillis();
    private static double savedMousePosX = 0D;
    private static double savedMousePosY = 0D;
    static double mouseDeltaX = 0.0D;
    static double prevMousePosX = 0.0D;
    private static float oldCameraAngleYaw = 0F;
    private static float oldCameraAnglePitch = 0F;
    private static float currentCameraAngleYaw = 0F;
    private static float currentCameraAnglePitch = 0F;
    private static final KeyBinding MOVE_CAMERA_FORWARD_KEY = createKeybinding("move_camera_forward", GLFW.GLFW_KEY_KP_ADD);
    private static final KeyBinding TOGGLE_CAMERA_KEY = createKeybinding("toggle_camera", GLFW.GLFW_KEY_F7);
    public static final KeyBinding GRAB_CAMERA_KEY = createKeybinding("grab_camera", GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
    private static final double PLAYER_CAMERA_DISTANCE = 7.0D;
    private static final float SENSITIVITY = 0.15F;
    private static final float INITIAL_CAMERA_ANGLE_XZ = 45.0F;
    private static final float INITIAL_CAMERA_ANGLE_Y = 45.0F;
    private static final float LERP_FACTOR = 5.0F;
    private static final double RAYCAST_MAX_DISTANCE = 100.0D;
    private static final int MOVEMENT_HISTORY_SIZE = 10;
    private static final double ALIGNMENT_THRESHOLD = 0.05;
    private final Deque<Vec3d> movementHistory = new ArrayDeque<>();

    private static BlockPos highlightedBlockPos = null;

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(MOVE_CAMERA_FORWARD_KEY);
        KeyBindingHelper.registerKeyBinding(TOGGLE_CAMERA_KEY);
        KeyBindingHelper.registerKeyBinding(GRAB_CAMERA_KEY);

        //ClientTickEvents.START_CLIENT_TICK.register(this::onTickBegin);
        ClientTickEvents.END_CLIENT_TICK.register(this::onTickEnd);

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

    private void onTickEnd(MinecraftClient client) {
        if (TOGGLE_CAMERA_KEY.wasPressed()) {
            toggleDetachedCamera();
        }

        if (isDetachedCameraEnabled) {
            if (GRAB_CAMERA_KEY.isPressed()) {
                if (!isCameraGrabbed) {
                    isCameraGrabbed = true;
                    savedMousePosX = client.mouse.getX();
                    savedMousePosY = client.mouse.getY();

                    client.mouse.lockCursor();
                }
            }

            handleCameraRelativeMovement(client.getTickDelta(), 120.0f);

//            if (isInteracting()) {
//                if (!isCameraGrabbed) {
//                    updatePlayerRotationToCursor(RAYCAST_MAX_DISTANCE, client.player.isHolding(Items.BUCKET));
//                } else {
//                    updatePlayerRotationToCursor(RAYCAST_MAX_DISTANCE, client.player.isHolding(Items.BUCKET), savedMousePosX, savedMousePosY);
//                }
//            }
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

            oldCameraAngleYaw = INITIAL_CAMERA_ANGLE_XZ;
            oldCameraAnglePitch = INITIAL_CAMERA_ANGLE_Y;
            currentCameraAngleYaw = INITIAL_CAMERA_ANGLE_XZ;
            currentCameraAnglePitch = INITIAL_CAMERA_ANGLE_Y;

            Vec3d direction = playerPos.subtract(camera.getPos()).normalize();
            double towardsPlayerYaw = Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0f;
            double towardsPlayerPitch = Math.toDegrees(-Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z)));

            setCameraRotation((float) towardsPlayerYaw, (float) towardsPlayerPitch);
        } else {
            client.mouse.lockCursor();
            client.options.setPerspective(Perspective.FIRST_PERSON);
        }
    }

    private void updatePlayerRotationToCursor(double maxDistance, boolean lookThoughFluids) {
        updatePlayerRotationToCursor(maxDistance, lookThoughFluids, client.mouse.getX(), client.mouse.getY());
    }

    private void updatePlayerRotationToCursor(double maxDistance, boolean lookThoughFluids, double mouseX, double mouseY) {
        assert client.world != null;
        assert client.cameraEntity != null;

        Vec3d cameraPosVector = client.gameRenderer.getCamera().getPos();

        Vec3d mouseBasedViewVector = getMouseBasedViewVector(client, mouseX, mouseY);
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
                playerLookAt(EntityAnchorArgumentType.EntityAnchor.EYES, blockHit.withSide(blockHit.getSide()).getPos(), LERP_FACTOR);
                break;
            case ENTITY:
                EntityHitResult entityHit = (EntityHitResult) result;
                Entity entity = entityHit.getEntity();
                break;
        }
    }

    private void handleCameraRelativeMovement(float deltaTime, float rotationSpeed /* degrees per second */) {
        PlayerEntity player = client.player;

        if (player == null) return;

        float playerYaw = MathHelper.wrapDegrees(player.getYaw());
        float cameraYaw = MathHelper.wrapDegrees(currentCameraAngleYaw);

//        // Get WASD inputs
//        float forwardInput = (client.options.forwardKey.isPressed() ? 1.0f : 0.0f) - (client.options.backKey.isPressed() ? 1.0f : 0.0f);
//        float strafeInput = (client.options.rightKey.isPressed() ? 1.0f : 0.0f) - (client.options.leftKey.isPressed() ? 1.0f : 0.0f);
//
//        // Combine inputs into a single vector
//        Vec2f inputVector = new Vec2f(strafeInput, forwardInput);
//        if (inputVector.lengthSquared() > 0.01f) {
//            inputVector = inputVector.normalize(); // Normalize input for consistent movement
//        } else {
//            return; // No meaningful input
//        }
//
//        // Rotate input vector into camera-relative space
//        float inputAngle = (float) Math.atan2(inputVector.x, inputVector.y); // Angle of the input vector
        if (
            !client.options.forwardKey.isPressed() &&
            !client.options.backKey.isPressed() &&
            !client.options.rightKey.isPressed() &&
            !client.options.leftKey.isPressed()

        ) {
            return;
        }

        float inputAngle = (
                (client.options.forwardKey.isPressed() && client.options.rightKey.isPressed()) ? 45.0f :
                (client.options.forwardKey.isPressed() && client.options.leftKey.isPressed()) ? 135.0f :
                (client.options.backKey.isPressed() && client.options.leftKey.isPressed()) ? 225.0f :
                (client.options.backKey.isPressed() && client.options.rightKey.isPressed()) ? 315.0f :
                client.options.rightKey.isPressed() ? 0.0f :
                client.options.forwardKey.isPressed() ? 90.0f :
                client.options.leftKey.isPressed() ? 180.0f :
                270.0f // client.options.backKey.isPressed()
            );

        float movementYaw = MathHelper.wrapDegrees(cameraYaw + ((float) Math.toDegrees(inputAngle) + 90.0f)); // Camera-relative movement yaw

        // Calculate motion vector based on movement yaw
        double radianYaw = Math.toRadians(movementYaw);
        double motionX = -Math.sin(radianYaw);
        double motionZ = Math.cos(radianYaw);

//        // Ensure consistent speed regardless of direction
        Vec3d movementVector = new Vec3d(motionX, 0, motionZ).normalize();
//        cacheMovementHistory(movementVector);
//
//        // Adjust speed if the player is interacting or idle
//        double speed = isInteracting() ? 0.05 : 0.1; // Slower speed during interaction
//        Vec3d averageDirection = calculateAverageDirection();
//        if (averageDirection != null && averageDirection.lengthSquared() > ALIGNMENT_THRESHOLD) {
//            movementVector = averageDirection.normalize();
//        }

        // Apply velocity
        player.setVelocity(movementVector.x * 1.0f/*speed*/, player.getVelocity().y, movementVector.z * 1.0f/*speed*/);

        // solve rotation issues later
//        // Smoothly rotate the player towards the movement direction
//        float targetPlayerYaw = (float) Math.toDegrees(Math.atan2(-movementVector.x, movementVector.z));
//        rotationSpeed = rotationSpeed * deltaTime; // Degrees per second
//        float newPlayerYaw = MathHelper.stepUnwrappedAngleTowards(playerYaw, targetPlayerYaw, rotationSpeed);
//
//        player.setYaw(newPlayerYaw);
//        player.setBodyYaw(newPlayerYaw);
    }

    private Vec3d calculateAverageDirection() {
        if (movementHistory.isEmpty()) return Vec3d.ZERO;

        Vec3d sum = Vec3d.ZERO;
        for (Vec3d vec : movementHistory) {
            sum = sum.add(vec);
        }
        return sum.multiply(1.0 / movementHistory.size());
    }

    private void cacheMovementHistory(Vec3d movementVector) {
        if (movementHistory.size() >= MOVEMENT_HISTORY_SIZE) {
            movementHistory.pollFirst(); // Remove oldest entry
        }
        movementHistory.addLast(movementVector);
    }

    private boolean isInteracting() {
        // Returns true if the player has interacted recently
        return System.currentTimeMillis() - lastInteractionTime < 3000;
    }

    private void updateLastInteractionTime() {
        lastInteractionTime = System.currentTimeMillis();
    }

    private static void playerLookAt(EntityAnchorArgumentType.EntityAnchor anchorPoint, Vec3d target, float lerp) {
        assert client.player != null;
        ClientPlayerEntity player = client.player;

        Vec3d vec3d = anchorPoint.positionAt(player);
        double d = target.x - vec3d.x;
        double e = target.y - vec3d.y;
        double f = target.z - vec3d.z;
        double g = Math.sqrt(d * d + f * f);

        float pitchToSet = MathHelper.wrapDegrees((float)(-(MathHelper.atan2(e, g) * 57.2957763671875)));
        float yawToSet = MathHelper.wrapDegrees((float)(MathHelper.atan2(f, d) * 57.2957763671875) - 90.0F);

        player.setPitch(MathHelper.lerpAngleDegrees(MathHelper.clamp(lerp * client.getTickDelta(), 0.0F, 1.0F), player.getPitch(), pitchToSet));
        player.setYaw(MathHelper.lerpAngleDegrees(MathHelper.clamp(lerp * client.getTickDelta(), 0.0F, 1.0F), player.getYaw(), yawToSet));
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

            if (Math.abs(mouseDeltaX) <= 1.0d && Math.abs(mouseDeltaY) <= 1.0d) {
                return;
            }

            currentCameraAngleYaw = ((float) mouseDeltaX * SENSITIVITY) + oldCameraAngleYaw;
            System.out.println("currentCameraAngleYaw: " + currentCameraAngleYaw);
            currentCameraAnglePitch = ((float) mouseDeltaY * SENSITIVITY) + oldCameraAnglePitch;
            currentCameraAnglePitch = MathHelper.clamp(currentCameraAnglePitch, -89.99f, 89.99f);

            currentCameraAngleYaw = MathHelper.wrapDegrees(currentCameraAngleYaw);

            Vec3d playerPos = client.player.getPos();

            double pitchRadians = Math.toRadians(currentCameraAnglePitch);
            double yawRadians = Math.toRadians(currentCameraAngleYaw);

            camera.setPos(
                playerPos.x + (PLAYER_CAMERA_DISTANCE * Math.cos(pitchRadians) * Math.cos(yawRadians)),
                playerPos.y + (PLAYER_CAMERA_DISTANCE * Math.sin(pitchRadians)),
                playerPos.z + (PLAYER_CAMERA_DISTANCE * Math.cos(pitchRadians) * Math.sin(yawRadians))
            );

            oldCameraAngleYaw = currentCameraAngleYaw;
            oldCameraAnglePitch = currentCameraAnglePitch;

            prevMousePosX = mousePosX;

            Vec3d direction = playerPos.subtract(camera.getPos()).normalize();
            double towardsPlayerYaw = Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0f;
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
