package com.brickmasterhunt.rpgcam.client.config;

import com.brickmasterhunt.rpgcam.client.RpgCam;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = RpgCam.MOD_ID)
public class CamConfig implements ConfigData {
    float DEFAULT_PLAYER_CAMERA_DISTANCE;
    float DEFAULT_INITIAL_CAMERA_ANGLE_XZ;
    float DEFAULT_INITIAL_CAMERA_ANGLE_Y;
    float GRAB_CAMERA_MOUSE_SENSITIVITY;
    float PLAYER_RELATIVE_MOVE_ROTATE_LERP;
    float PLAYER_ROTATE_SPEED;
    float PLAYER_INTERACT_FOLLOW_CURSOR_LERP;
}
