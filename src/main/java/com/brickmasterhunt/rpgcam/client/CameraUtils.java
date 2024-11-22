package com.brickmasterhunt.rpgcam.client;

/* code taken from MineFortress */

import com.brickmasterhunt.rpgcam.client.render.GLU;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class CameraUtils {
    public static Vec3d getMouseBasedViewVector(MinecraftClient client, double xpos, double ypos) {
        int winWidth = client.getWindow().getWidth();
        int winHeight = client.getWindow().getHeight();

        FloatBuffer resultingViewBuffer = MemoryUtil.memAllocFloat(3);
        resultingViewBuffer.position(0);
        FloatBuffer modelViewBuffer = getModelViewMatrix(client);
        FloatBuffer projectionBuffer = getProjectionMatrix(client);
        IntBuffer viewport = getViewport(winWidth, winHeight);

        GLU.gluUnProject((float) xpos, (float)(winHeight - ypos) , 1.0f, modelViewBuffer, projectionBuffer, viewport, resultingViewBuffer);

        return calculateResultingVector(resultingViewBuffer);
    }

    private static IntBuffer getViewport(int winWidth, int winHeight) {
        IntBuffer viewport = MemoryUtil.memAllocInt(4);
        viewport.position(0);
        viewport.put(0);
        viewport.put(0);
        viewport.put(winWidth);
        viewport.put(winHeight);
        viewport.rewind();
        return viewport;
    }

    private static Vec3d calculateResultingVector(FloatBuffer resultingViewBuffer) {
        Vector3f resultingViewVector = new Vector3f(resultingViewBuffer.get(0), resultingViewBuffer.get(1), resultingViewBuffer.get(2));
        resultingViewVector.normalize();
        return new Vec3d(resultingViewVector);
    }

    private static FloatBuffer getModelViewMatrix(MinecraftClient client) {
        final var modelViewMatrix = new Matrix4f(RenderSystem.getModelViewMatrix());

        final var xRads = (float) Math.toRadians(client.gameRenderer.getCamera().getPitch());
        modelViewMatrix.rotate(xRads, new Vector3f(1, 0,0));
        final float yRads = (float) Math.toRadians(client.gameRenderer.getCamera().getYaw() + 180f);
        modelViewMatrix.rotate(yRads, new Vector3f(0, 1,0));

        FloatBuffer modelViewBuffer = MemoryUtil.memAllocFloat(16);
        modelViewBuffer.position(0);
        modelViewMatrix.get(modelViewBuffer);

        modelViewBuffer.rewind();
        return modelViewBuffer;
    }

    private static FloatBuffer getProjectionMatrix(MinecraftClient client) {
        Matrix4f projectionMatrix = getProjectionMatrix4f(client);
        FloatBuffer projectionBuffer = MemoryUtil.memAllocFloat(16);
        projectionBuffer.position(0);
        projectionMatrix.get(projectionBuffer);
        projectionBuffer.rewind();
        return projectionBuffer;
    }

    @NotNull
    public static Matrix4f getProjectionMatrix4f(MinecraftClient client) {
        final GameRenderer gameRenderer = client.gameRenderer;

        double fov = gameRenderer.getFov(gameRenderer.getCamera(), client.getTickDelta(), true);
        return getProjectionMatrix4f(client, fov);
    }

    @NotNull
    private static Matrix4f getProjectionMatrix4f(MinecraftClient client, double fov) {
        final GameRenderer gameRenderer = client.gameRenderer;
        return new Matrix4f(gameRenderer.getBasicProjectionMatrix(fov));
    }
}
