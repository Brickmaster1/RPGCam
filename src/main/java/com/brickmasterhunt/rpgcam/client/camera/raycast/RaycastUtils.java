package com.brickmasterhunt.rpgcam.client.camera.raycast;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import static com.brickmasterhunt.rpgcam.client.CamState.*;
import static com.brickmasterhunt.rpgcam.client.camera.CameraUtils.getMouseBasedViewVector;

public class RaycastUtils {
    public static HitResult raycastAtCursor(boolean throughFluids, boolean includeEntities, double maxDistance, double mouseX, double mouseY) {
        assert client.world != null;
        assert client.cameraEntity != null;

        Vec3d cameraPosVector = client.gameRenderer.getCamera().getPos();
        Vec3d mouseBasedViewVector = getMouseBasedViewVector(client, mouseX, mouseY);
        Vec3d farCameraVector = cameraPosVector.add(mouseBasedViewVector.x * maxDistance, mouseBasedViewVector.y * maxDistance, mouseBasedViewVector.z * maxDistance);

        HitResult result = null;

        if (includeEntities) {
            result = raycastEntity(
                maxDistance,
                cameraPosVector,
                farCameraVector
            );
        }

        if (result == null) {
            result = client.world.raycast(
                new RaycastContext(
                    cameraPosVector,
                    farCameraVector,
                    RaycastContext.ShapeType.OUTLINE,
                    (!throughFluids) ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE,
                    client.cameraEntity
                )
            );
        }

        return result;
    }

    public static boolean isRayToBlockUnobstructed(PlayerEntity player, BlockPos block) {
        return true;
    }

    private static HitResult raycastEntity(double maxDistance, Vec3d cameraPos, Vec3d farCameraVector) {
        Entity cameraEntity = client.cameraEntity;
        assert client.player != null;

        if (cameraEntity != null) {
            Vec3d rot = client.cameraEntity.getRotationVec(1.0f);
            Box box = cameraEntity.getBoundingBox().stretch(rot.multiply(maxDistance)).expand(1d, 1d, 1d);

            return ProjectileUtil.raycast(cameraEntity, cameraPos, farCameraVector, box, (entity -> /* any custom parameters here */ !entity.isSpectator() && entity.canHit()), maxDistance);
        }

        return null;
    }
}
