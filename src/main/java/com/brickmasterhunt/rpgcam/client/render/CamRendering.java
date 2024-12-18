package com.brickmasterhunt.rpgcam.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import static com.brickmasterhunt.rpgcam.client.CamState.*;

public class CamRendering {
    public static void highlightSelection(WorldRenderContext context) {
        if (getHighlightedBlockPos() != null) {
            Camera camera = context.camera();
            Vec3d camPos = camera.getPos();
            Box box = new Box(getHighlightedBlockPos()).expand(0.002).offset(-camPos.x, -camPos.y, -camPos.z);

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
}
