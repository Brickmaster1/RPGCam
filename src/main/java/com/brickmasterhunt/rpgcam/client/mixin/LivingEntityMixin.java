package com.brickmasterhunt.rpgcam.client.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Redirect(
            method = "getRotationVector",
            at = @At(value = "INVOKE", target = "")
    )
    Vec3d getRotationVector() {

    }
}
