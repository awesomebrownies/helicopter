package com.breakpointinteractive.helicopter;

import org.bukkit.*;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.RayTraceResult;
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
        return (int) (rightTopBackward.z()-leftBottomForward.z());
    }

    public Vector3f getLeftBottomForward(){
        return leftBottomForward;
    }

    public LinkedList<Vector3f> getForwardFace(Location location, Quaternionf quaternion, int height, int width){
        LinkedList<Vector3f> linkedList = new LinkedList<>();

        Vector3f upVector = getTopVector(quaternion);
        Vector3f rightVector = getRightVector(quaternion);
        Vector3f forwardVector = getForwardVector(quaternion);

        for(int i = 0; i < height; i++){
            for(int j = 0; j < width; j++){
                Location newLocation = location.clone()
                        .add(Vector.fromJOML(upVector).multiply(i))
                        .add(Vector.fromJOML(rightVector).multiply(j));
                linkedList.add(new Vector3f((float) newLocation.getX(), (float)newLocation.getY(), (float)newLocation.getZ()));
            }
        }

        return linkedList;


        //Vector3f rightTopForward = new Vector3f(rightTopBackward.x(), rightTopBackward.y(), leftBottomForward.z());
        //CollisionBox collisionBox = new CollisionBox(leftBottomForward, rightTopForward);
        //collisionBox.transformUnit(quaternion);
        //return collisionBox;
    }
    public CollisionBox getBackwardFace(Quaternionf quaternion){
        Vector3f leftBottomBackward = new Vector3f(leftBottomForward.x(), leftBottomForward.y(), rightTopBackward.z());
        CollisionBox collisionBox = new CollisionBox(leftBottomBackward, rightTopBackward);
        collisionBox.transformUnit(quaternion);
        return collisionBox;
    }
    public CollisionBox getLeftFace(Quaternionf quaternion){
        Vector3f leftTopBackward = new Vector3f(leftBottomForward.x(), rightTopBackward.y(), rightTopBackward.z());
        CollisionBox collisionBox = new CollisionBox(leftBottomForward, leftTopBackward);
        collisionBox.transformUnit(quaternion);
        return collisionBox;
    }
    public CollisionBox getRightFace(Quaternionf quaternion){
        Vector3f rightBottomForward = new Vector3f(rightTopBackward.x(), leftBottomForward.y(), leftBottomForward.z());
        CollisionBox collisionBox = new CollisionBox(rightBottomForward, rightTopBackward);
        collisionBox.transformUnit(quaternion);
        return collisionBox;
    }
    public CollisionBox getTopFace(Quaternionf quaternion){
        Vector3f leftTopForward = new Vector3f(leftBottomForward.x(), rightTopBackward.y(), leftBottomForward.z());
        CollisionBox collisionBox = new CollisionBox(leftTopForward, rightTopBackward);
        collisionBox.transformUnit(quaternion);
        return collisionBox;
    }
    public CollisionBox getBottomFace(Quaternionf quaternion){
        Vector3f rightBottomBackward = new Vector3f(rightTopBackward.x(), leftBottomForward.y(), rightTopBackward.z());
        CollisionBox collisionBox = new CollisionBox(leftBottomForward, rightBottomBackward);
        collisionBox.transformUnit(quaternion);
        return collisionBox;
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
