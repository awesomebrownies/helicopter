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
        return (int) (leftBottomForward.z()-rightTopBackward.z());
    }

    public Vector3f getLeftBottomForward(){
        return leftBottomForward;
    }
    public Vector3f getRightTopBackward(){
        return rightTopBackward;
    }

    public LinkedList<Vector3f> getForwardFace(Location location, Quaternionf quaternion, int height, int width){
        LinkedList<Vector3f> linkedList = new LinkedList<>();
        Vector3f upVector = getTopVector(quaternion);
        Vector3f rightVector = getRightVector(quaternion);
        for(int i = 0; i < height; i++){
            for(int j = 0; j < width; j++){
                Location newLocation = location.clone()
                        .add(Vector.fromJOML(upVector).multiply(i))
                        .add(Vector.fromJOML(rightVector).multiply(j));
                linkedList.add(new Vector3f((float) newLocation.getX(), (float)newLocation.getY(), (float)newLocation.getZ()));
            }
        }
        return linkedList;
    }
    public LinkedList<Vector3f> getBackwardFace(Location location, Quaternionf quaternion, int height, int width){
        LinkedList<Vector3f> linkedList = new LinkedList<>();
        Vector3f upVector = getTopVector(quaternion);
        Vector3f rightVector = getRightVector(quaternion);
        for(int i = 0; i < height; i++){
            for(int j = 0; j < width; j++){
                Location newLocation = location.clone()
                        .subtract(Vector.fromJOML(upVector).multiply(i))
                        .subtract(Vector.fromJOML(rightVector).multiply(j));
                linkedList.add(new Vector3f((float) newLocation.getX(), (float)newLocation.getY(), (float)newLocation.getZ()));
            }
        }
        return linkedList;
    }
    public LinkedList<Vector3f> getLeftFace(Location location, Quaternionf quaternion, int height, int depth){
        LinkedList<Vector3f> linkedList = new LinkedList<>();
        Vector3f upVector = getTopVector(quaternion);
        Vector3f forwardVector = getForwardVector(quaternion);
        for(int i = 0; i < height; i++){
            for(int j = 0; j < depth; j++){
                Location newLocation = location.clone()
                        .add(Vector.fromJOML(upVector).multiply(i))
                        .subtract(Vector.fromJOML(forwardVector).multiply(j));
                linkedList.add(new Vector3f((float) newLocation.getX(), (float)newLocation.getY(), (float)newLocation.getZ()));
            }
        }
        return linkedList;
    }
    public LinkedList<Vector3f> getRightFace(Location location, Quaternionf quaternion, int height, int depth){
        LinkedList<Vector3f> linkedList = new LinkedList<>();
        Vector3f upVector = getTopVector(quaternion);
        Vector3f forwardVector = getForwardVector(quaternion);
        for(int i = 0; i < height; i++){
            for(int j = 0; j < depth; j++){
                Location newLocation = location.clone()
                        .subtract(Vector.fromJOML(upVector).multiply(i))
                        .add(Vector.fromJOML(forwardVector).multiply(j));
                linkedList.add(new Vector3f((float) newLocation.getX(), (float)newLocation.getY(), (float)newLocation.getZ()));
            }
        }
        return linkedList;
    }
    public LinkedList<Vector3f> getTopFace(Location location, Quaternionf quaternion, int width, int depth){
        LinkedList<Vector3f> linkedList = new LinkedList<>();
        Vector3f rightVector = getRightVector(quaternion);
        Vector3f forwardVector = getForwardVector(quaternion);
        for(int i = 0; i < width; i++){
            for(int j = 0; j < depth; j++){
                Location newLocation = location.clone()
                        .subtract(Vector.fromJOML(rightVector).multiply(i))
                        .add(Vector.fromJOML(forwardVector).multiply(j));
                linkedList.add(new Vector3f((float) newLocation.getX(), (float)newLocation.getY(), (float)newLocation.getZ()));
            }
        }
        return linkedList;
    }
    public LinkedList<Vector3f> getBottomFace(Location location, Quaternionf quaternion, int width, int depth){
        LinkedList<Vector3f> linkedList = new LinkedList<>();
        Vector3f rightVector = getRightVector(quaternion);
        Vector3f forwardVector = getForwardVector(quaternion);
        for(int i = 0; i < width; i++){
            for(int j = 0; j < depth; j++){
                Location newLocation = location.clone()
                        .add(Vector.fromJOML(rightVector).multiply(i))
                        .subtract(Vector.fromJOML(forwardVector).multiply(j));
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
