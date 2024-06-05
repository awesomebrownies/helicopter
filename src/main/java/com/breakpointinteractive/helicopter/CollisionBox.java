package com.breakpointinteractive.helicopter;

import org.bukkit.*;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.LinkedList;

public class CollisionBox {
    private final Vector3f leftBottomForward; //left bottom forward
    private final Vector3f rightTopBackward; //right top backward

    public CollisionBox(Vector3f position1, Vector3f position2){
        this.leftBottomForward = position1;
        this.rightTopBackward = position2;
    }

    public int getHeight(){
        return (int) (rightTopBackward.y()-leftBottomForward.y());
    }
    public int getWidth(){
        return (int) (rightTopBackward.x()-leftBottomForward.x());
    }
    public int getDepth(){
        return (int) (leftBottomForward.z()-rightTopBackward.z());
    }

    public Vector3f getLeftBottomForward(){
        return leftBottomForward;
    }
    public Vector3f getRightTopBackward(){
        return rightTopBackward;
    }

    public LinkedList<Vector3f> getFace(Location location, int length, int secondLength, Vector3f vector, Vector3f secondVector){
        LinkedList<Vector3f> linkedList = new LinkedList<>();
        for(int i = 0; i < length; i++){
            for(int j = 0; j < secondLength; j++){
                Location newLocation = location.clone()
                        .add(Vector.fromJOML(vector).multiply(i))
                        .add(Vector.fromJOML(secondVector).multiply(j));
                linkedList.add(new Vector3f((float) newLocation.getX(), (float)newLocation.getY(), (float)newLocation.getZ()));
            }
        }
        return linkedList;
    }
    public static Vector3f getForwardVector(Quaternionf quaternion){
        return quaternion.transformUnit(new Vector3f(0,0,1));
    }
    public static Vector3f getBackwardVector(Quaternionf quaternion){
        return quaternion.transformUnit(new Vector3f(0,0,-1));
    }
    public static Vector3f getLeftVector(Quaternionf quaternion){
        return quaternion.transformUnit(new Vector3f(-1,0,0));
    }
    public static Vector3f getRightVector(Quaternionf quaternion){
        return quaternion.transformUnit(new Vector3f(1,0,0));
    }
    public static Vector3f getTopVector(Quaternionf quaternion){
        return quaternion.transformUnit(new Vector3f(0,1,0));
    }
    public static Vector3f getBottomVector(Quaternionf quaternion){
        return quaternion.transformUnit(new Vector3f(0,-1,0));
    }

    public void transformUnit(Quaternionf quaternion){
        quaternion.transformUnit(leftBottomForward);
        quaternion.transformUnit(rightTopBackward);
    }
}
