package com.brickmasterhunt.rpgcam.client.config;

import com.brickmasterhunt.rpgcam.client.RpgCam;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = RpgCam.MOD_ID)
public class CamConfig implements ConfigData {
    public long PLAYER_IDLE_WAIT_TIME = 4000;
    public long NOT_INTERACTING_WAIT_TIME = 3000;

    public float INITIAL_PLAYER_CAMERA_DISTANCE = 7.0F;
    public float INITIAL_CAMERA_ANGLE_XZ = 45.0F;
    public float INITIAL_CAMERA_ANGLE_Y = 45.0F;
    public float GRAB_CAMERA_MOUSE_SENSITIVITY = 0.15F;
    public float PLAYER_RELATIVE_MOVE_ROTATE_LERP = 2.5F;
    public float PLAYER_INTERACT_FOLLOW_CURSOR_LERP = 5.0F;
    public float PLAYER_ROTATE_SPEED = 20.0F;

    public double RAYCAST_MAX_DISTANCE = 100.0D;
}
