package com.breakpointinteractive.helicopter;

import org.bukkit.util.Vector;
import org.joml.Vector3f;

public class PositionRotation {
    private Vector position;
    private float pitch;
    private float yaw;
    private float roll;

    public PositionRotation(Vector position, float yaw, float pitch, float roll){
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
    }

    public Vector getPosition(){
        return position;
    }
    public float getPitch(){
        return pitch;
    }
    public float getYaw(){
        return yaw;
    }
    public float getRoll() {
        return roll;
    }
}
