package com.brickmasterhunt.rpgcam.client;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

import com.brickmasterhunt.rpgcam.client.config.CamConfig;

import static com.brickmasterhunt.rpgcam.client.CamState.*;
import static com.brickmasterhunt.rpgcam.client.interaction.InteractionManager.*;
import static com.brickmasterhunt.rpgcam.client.player.movement.RelativeMovement.*;
import static com.brickmasterhunt.rpgcam.client.render.CamRendering.*;
import static com.brickmasterhunt.rpgcam.client.camera.raycast.RaycastUtils.*;

@Environment(EnvType.CLIENT)
public class RpgCam implements ClientModInitializer {
    public static final String MOD_ID = "rpgcam";

    @Override
    public void onInitializeClient() {
        AutoConfig.register(CamConfig.class, Toml4jConfigSerializer::new);

        CONFIG = AutoConfig.getConfigHolder(CamConfig.class).getConfig();

        KeyBindingHelper.registerKeyBinding(MOVE_CAMERA_FORWARD_KEY);
        KeyBindingHelper.registerKeyBinding(TOGGLE_CAMERA_KEY);
        KeyBindingHelper.registerKeyBinding(GRAB_CAMERA_KEY);

        ClientTickEvents.END_CLIENT_TICK.register(this::onTickEnd);

        WorldRenderEvents.AFTER_ENTITIES.register(this::onWorldRender);
    }

    private void onWorldRender(WorldRenderContext context) {
        highlightSelection(context);
    }

    private void onTickEnd(MinecraftClient client) {
        if (TOGGLE_CAMERA_KEY.wasPressed()) {
            toggleDetachedCamera();
        }

        if (isDetachedCameraEnabled()) {
            updatePlayerInteractionStatus(); // Check if player is trying to interact with entities/blocks

            evaluateCameraGrabState(); // Rotate camera about player if pressed
            handleCameraRotateInteraction(); // Check and activate camera grabbing if player uses assigned key

            var mousePos = getMousePos();
            if (isPlayerInteracting()) {
                updatePlayerRotationToCursor(CONFIG.RAYCAST_MAX_DISTANCE, client.player.isHolding(Items.BUCKET), true, mousePos.x, mousePos.y);
            } else {
                HitResult raycast = raycastAtCursor(client.player.isHolding(Items.BUCKET), true, CONFIG.RAYCAST_MAX_DISTANCE, mousePos.x, mousePos.y);
                if (raycast.getType() == HitResult.Type.BLOCK) {
                    highlightBlockAt(((BlockHitResult) raycast).getBlockPos());
                }

                handleMovementRelativeToCamera(CONFIG.PLAYER_ROTATE_SPEED, CONFIG.PLAYER_RELATIVE_MOVE_ROTATE_LERP);
            }
        }
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
