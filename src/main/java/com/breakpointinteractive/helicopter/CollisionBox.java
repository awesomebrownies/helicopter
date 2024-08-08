package com.breakpointinteractive.helicopter;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.LinkedList;

public class CollisionBox {
    public enum Face{
        FORWARD,
        BACKWARD,
        LEFT,
        RIGHT,
        TOP,
        BOTTOM
    }

    private Vector3f center; //left bottom forward
    private final Vector3f halfLength; //right top backward

    public CollisionBox(Vector3f center, Vector3f halfLength){
        this.center = center;
        this.halfLength = halfLength;
    }

    public float getHeight(){
        return halfLength.y*2;
    }
    public float getWidth(){
        return halfLength.x*2;
    }
    public float getDepth(){
        return halfLength.z*2;
    }

    public Vector3f getCenter(){
        return center;
    }

    private Location getMultipliedDirections(Location startLocation, Vector3f firstDirection, Vector3f secondDirection, double i, double j){
        return startLocation.clone().add(Vector.fromJOML(firstDirection).multiply(i)).add(Vector.fromJOML(secondDirection).multiply(j));
    }

    public Vector3f getHighestTargetBlock(Vector3f highestLocation, Location targetBlock, Vector3f planeVector, ActiveHelicopter helicopter){
        if(targetBlock.getBlock().isSolid()){
            //determine the highest vertex point on block from opposite signs of vector
            Vector3f highestVertexPoint = highestVertexPoint(planeVector, targetBlock.getBlock());
            Vector3f highestVertexPointDirection = new Vector3f(highestVertexPoint.x, highestVertexPoint.y, highestVertexPoint.z).sub(highestLocation);
            if(highestVertexPointDirection.dot(planeVector) < 0){ //dot product to determine if block is higher than current highest
                helicopter.setIsGrounded(true);
                return highestVertexPoint;
            }
        }
        return highestLocation;
    }

    static Vector3f equation_plane(Vector3f v1, Vector3f v2, Vector3f v3){
        float a1 = v2.x - v1.x;
        float b1 = v2.y - v1.y;
        float c1 = v2.z - v1.z;
        float a2 = v3.x - v1.x;
        float b2 = v3.y - v1.y;
        float c2 = v3.z - v1.z;
        float a = b1 * c2 - b2 * c1;
        float b = a2 * c1 - a1 * c2;
        float c = a1 * b2 - b1 * a2;
        //float d = (- a * x1 - b * y1 - c * z1);
        return new Vector3f(a, b, c);
    }

    public void setCenter(Vector newCenter){
        center = newCenter.toVector3f();
    }

    public PositionRotation calculatePositionRotation(Face face, ActiveHelicopter helicopter, Vector3f planeVector, Location startLocation, float firstLength, float secondLength, Vector3f firstDirection, Vector3f secondDirection, Quaternionf bodyRotation, float localHeightOffset){

        Vector3f topLeft = new Vector3f(firstDirection.x*firstLength+secondDirection.x*secondLength,firstDirection.y*firstLength+secondDirection.y*secondLength,firstDirection.z*firstLength+secondDirection.z*secondLength)
                .add(startLocation.toVector().toVector3f());
        Vector3f topRight = new Vector3f(-firstDirection.x*firstLength+secondDirection.x*secondLength,-firstDirection.y*firstLength+secondDirection.y*secondLength,-firstDirection.z*firstLength+secondDirection.z*secondLength)
                .add(startLocation.toVector().toVector3f());
        Vector3f bottomLeft = new Vector3f(firstDirection.x*firstLength-secondDirection.x*secondLength,firstDirection.y*firstLength-secondDirection.y*secondLength,firstDirection.z*firstLength-secondDirection.z*secondLength)
                .add(startLocation.toVector().toVector3f());
        Vector3f bottomRight = new Vector3f(-firstDirection.x*firstLength-secondDirection.x*secondLength,-firstDirection.y*firstLength-secondDirection.y*secondLength,-firstDirection.z*firstLength-secondDirection.z*secondLength)
                .add(startLocation.toVector().toVector3f());

        for(double i = 0.75; i < firstLength; i += 1.){
            for(double j = 0.75; j< secondLength; j += 1.){
                //double m = 1-(i / firstLength + j / secondLength)/2.;
                double m = 1;
                Location topLeftTarget = getMultipliedDirections(startLocation.clone().subtract(helicopter.getVelocity().clone().multiply(m)), firstDirection, secondDirection, i, j);
                Location topRightTarget = getMultipliedDirections(startLocation.clone().subtract(helicopter.getVelocity().clone().multiply(m)), firstDirection, secondDirection, -i, j);
                Location bottomLeftTarget = getMultipliedDirections(startLocation.clone().subtract(helicopter.getVelocity().clone().multiply(m)), firstDirection, secondDirection, i, -j);
                Location bottomRightTarget = getMultipliedDirections(startLocation.clone().subtract(helicopter.getVelocity().clone().multiply(m)), firstDirection, secondDirection, -i, -j);
                //determine the highest point of block depending on the planeVector
                topLeft = getHighestTargetBlock(topLeft, topLeftTarget, planeVector, helicopter);
                topRight = getHighestTargetBlock(topRight, topRightTarget, planeVector, helicopter);
                bottomLeft = getHighestTargetBlock(bottomLeft, bottomLeftTarget, planeVector, helicopter);
                bottomRight = getHighestTargetBlock(bottomRight, bottomRightTarget, planeVector, helicopter);
            }
        }
        Vector3f[] vertices = {topLeft, topRight, bottomRight, bottomLeft};
        int lowest = 0;
        for(int vertex = 1; vertex < 4; vertex++){
            Vector3f vertexDirection = new Vector3f(vertices[vertex].x, vertices[vertex].y, vertices[vertex].z);
            if(vertexDirection.sub(vertices[lowest]).dot(planeVector) > 0){
                lowest = vertex;
            }
        }

        //get plane
        //rotate by - helicopter y axis
        //get x value for pitch
        //get z value for roll
        Vector3f newPlaneNormal = equation_plane(vertices[(lowest+1)%4], vertices[(lowest+2)%4], vertices[(lowest+3)%4]).normalize();
        Quaternionf quaternion = bodyRotation;

        float pitch = (float) (Math.acos(newPlaneNormal.dot(firstDirection))-Math.PI/2);
        float roll = (float) (Math.acos(newPlaneNormal.dot(secondDirection))-Math.PI/2);

        Vector3f axis = new Vector3f(0,0,1);
        quaternion.transformUnit(axis);

        quaternion.rotateX(pitch).rotateZ(roll);


        // Calculate the rotation
        Vector3f newRotation = new Vector3f();
        quaternion.getEulerAnglesYXZ(newRotation);


        Vector intersection = lineIntersection(Vector.fromJOML(vertices[(lowest+2)%4]),Vector.fromJOML(newPlaneNormal), startLocation.toVector(), Vector.fromJOML(planeVector));
        Vector intersectionDistance = intersection.clone().subtract(startLocation.toVector());

        if (helicopter.getVelocity().getX() * (helicopter.getVelocity().getX() + intersectionDistance.getX()) < 0) {
            helicopter.getVelocity().setX(0);
        }else if(Math.abs(helicopter.getVelocity().getX() + intersectionDistance.getX()) < Math.abs(helicopter.getVelocity().getX())){
            helicopter.getVelocity().setX(intersectionDistance.getX()+helicopter.getVelocity().getX());
        }
        if (helicopter.getVelocity().getY() * (helicopter.getVelocity().getY() + intersectionDistance.getY()) < 0) {
            helicopter.getVelocity().setY(0);
        }else if(Math.abs(helicopter.getVelocity().getY() + intersectionDistance.getY()) < Math.abs(helicopter.getVelocity().getY())){
            helicopter.getVelocity().setY(intersectionDistance.getY()+helicopter.getVelocity().getY());
        }
        if (helicopter.getVelocity().getZ() * (helicopter.getVelocity().getZ() + intersectionDistance.getZ()) < 0) {
            helicopter.getVelocity().setZ(0);
        }else if(Math.abs(helicopter.getVelocity().getZ() + intersectionDistance.getZ()) < Math.abs(helicopter.getVelocity().getZ())){
            helicopter.getVelocity().setZ(intersectionDistance.getZ()+helicopter.getVelocity().getZ());
        }
        //gather it all up and return it
        return new PositionRotation(intersection.add(new Vector(-planeVector.x*localHeightOffset, -planeVector.y*localHeightOffset, -planeVector.z*localHeightOffset)), newRotation.y, newRotation.x, newRotation.z);
    }

    public Vector getOffset(Location target, Vector3f planeVector, ActiveHelicopter helicopter, Vector offset){
        target.add(offset);
        if(target.getBlock().isSolid()){
            Vector3f highestVertexPoint = highestVertexPoint(planeVector, target.getBlock());
            Vector intersection = lineIntersection(Vector.fromJOML(highestVertexPoint), Vector.fromJOML(planeVector), target.toVector(), Vector.fromJOML(planeVector));
            return intersection.subtract(target.toVector());
        }
        return new Vector(0,0,0);
    }

    public PositionRotation calculatePosition(Face face, ActiveHelicopter helicopter, Vector3f planeVector, Location startLocation, float firstLength, float secondLength, Vector3f firstDirection, Vector3f secondDirection, Quaternionf bodyRotation, float localHeightOffset){
        Vector offset = new Vector();
        for(double i = 0.25; i < firstLength/1.25; i += 0.5) {
            for (double j = 0.25; j < secondLength/1.25; j += 0.5) {
                //get position
                //check if there is a block there
                //calculate vertex
                //perform line intersection test, and add difference to offset

                Location topLeftTarget = getMultipliedDirections(startLocation, firstDirection, secondDirection, i, j);
                Location topRightTarget = getMultipliedDirections(startLocation, firstDirection, secondDirection, -i, j);
                Location bottomLeftTarget = getMultipliedDirections(startLocation, firstDirection, secondDirection, i, -j);
                Location bottomRightTarget = getMultipliedDirections(startLocation, firstDirection, secondDirection, -i, -j);
                //determine the highest point of block depending on the planeVector
                offset.add(getOffset(topLeftTarget, planeVector, helicopter, offset));
                offset.add(getOffset(topRightTarget, planeVector, helicopter, offset));
                offset.add(getOffset(bottomLeftTarget, planeVector, helicopter, offset));
                offset.add(getOffset(bottomRightTarget, planeVector, helicopter, offset));
            }
        }

        if (helicopter.getVelocity().getX() * (helicopter.getVelocity().getX() + offset.getX()) < 0) {
            helicopter.getVelocity().setX(0);
        }else if(Math.abs(helicopter.getVelocity().getX() + offset.getX()) < Math.abs(helicopter.getVelocity().getX())){
            helicopter.getVelocity().setX(offset.getX()+helicopter.getVelocity().getX());
        }
        if (helicopter.getVelocity().getY() * (helicopter.getVelocity().getY() + offset.getY()) < 0) {
            helicopter.getVelocity().setY(0);
        }else if(Math.abs(helicopter.getVelocity().getY() + offset.getY()) < Math.abs(helicopter.getVelocity().getY())){
            helicopter.getVelocity().setY(offset.getY()+helicopter.getVelocity().getY());
        }
        if (helicopter.getVelocity().getZ() * (helicopter.getVelocity().getZ() + offset.getZ()) < 0) {
            helicopter.getVelocity().setZ(0);
        }else if(Math.abs(helicopter.getVelocity().getZ() + offset.getZ()) < Math.abs(helicopter.getVelocity().getZ())){
            helicopter.getVelocity().setZ(offset.getZ()+helicopter.getVelocity().getZ());
        }


        return new PositionRotation(startLocation.clone().toVector().add(offset)
                .add(new Vector(-planeVector.x*localHeightOffset, -planeVector.y*localHeightOffset, -planeVector.z*localHeightOffset)),
                helicopter.getBodyRotation().y, helicopter.getBodyRotation().x, helicopter.getBodyRotation().z);
    }

    public static Vector lineIntersection(Vector planePoint, Vector planeNormal, Vector linePoint, Vector lineDirection) {
        if (planeNormal.dot(lineDirection.normalize()) == 0) {
            return linePoint;
        }

        double t = (planeNormal.dot(planePoint) - planeNormal.dot(linePoint)) / planeNormal.dot(lineDirection.normalize());
        return linePoint.add(lineDirection.normalize().multiply(t));
    }

    public Vector3f highestVertexPoint(Vector3f planeVector, Block block){
        return block.getLocation().toVector().toVector3f().add(new Vector3f(
                0.5f+(0.5f*(planeVector.x > 0 ? -1 : 1)),
                0.5f+(0.5f*(planeVector.y > 0 ? -1 : 1)),
                0.5f+(0.5f*(planeVector.z > 0 ? -1 : 1)))
        );
    }

    public PositionRotation getFace(Face face, ActiveHelicopter helicopter, Vector3f forwardVector, Vector3f leftVector, Vector3f topVector, Quaternionf bodyRotation){
        switch(face){
            case FORWARD, BACKWARD ->{
                if(face == Face.BACKWARD){forwardVector.mul(-1);}
                Location startLocation = new Location(helicopter.getEntitiesBase()[0].getWorld(), center.x+ forwardVector.x*halfLength.z,
                        center.y+ forwardVector.y*halfLength.z, center.z+ forwardVector.z*halfLength.z);
                return calculatePosition(face, helicopter, forwardVector, startLocation, halfLength.x, halfLength.y, leftVector, topVector, bodyRotation, halfLength.z);
            }case LEFT, RIGHT ->{
                if(face == Face.RIGHT){leftVector.mul(-1);}
                Location startLocation = new Location(helicopter.getEntitiesBase()[0].getWorld(), center.x+ leftVector.x*halfLength.x,
                        center.y+ leftVector.y*halfLength.x, center.z+ leftVector.z*halfLength.x);
                return calculatePosition(face, helicopter, leftVector, startLocation, halfLength.y, halfLength.z, topVector, forwardVector, bodyRotation, halfLength.x);
            }case BOTTOM, TOP ->{
                if(face == Face.BOTTOM){topVector.mul(-1);}
                Location startLocation = new Location(helicopter.getEntitiesBase()[0].getWorld(), center.x+ (topVector.x*halfLength.y),
                        center.y+ (topVector.y*halfLength.y), center.z+ (topVector.z*halfLength.y)).add(helicopter.getVelocity());

                if(face == Face.BOTTOM){
                    return calculatePositionRotation(face, helicopter, topVector, startLocation, halfLength.z, halfLength.x, forwardVector, leftVector, bodyRotation, halfLength.y);
                }else{
                    return calculatePosition(face, helicopter, topVector, startLocation, halfLength.z, halfLength.x, forwardVector, leftVector, bodyRotation, halfLength.y);
                }
            }
        }
        return null;
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
}
