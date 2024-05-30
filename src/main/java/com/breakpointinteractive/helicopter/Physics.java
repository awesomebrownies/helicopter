package com.breakpointinteractive.helicopter;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.RelativeMovement;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class Physics {
    public static void simulateHelicopter(ActiveHelicopter helicopter){
        Player playerChecker = null;
        for(Entity entity : helicopter.getEntitiesBase()[0].getPassengers()){
            if(entity instanceof Player target){
                playerChecker = target;
            }
        }
        final Player player = playerChecker;

        new BukkitRunnable(){
            @Override
            public void run(){
                if(helicopter.getEntitiesBase()[0].getPassengers().size() != 4 && helicopter.getIsGrounded()){
                    cancel();
                    ActiveHelicopter.getActiveHelicopters().remove(helicopter.getEntitiesBase()[0].getEntityId());
                    return;
                }

                if(helicopter.getEntitiesBase()[0].getPassengers().size() == 4){
                    handlePlayerRotation(helicopter, player, helicopter.getPlayerRotation());
                }
                simulatePhysics(helicopter);

            }
        }.runTaskTimer(Helicopter.getInstance(), 0L, 1L);
        //calculate thrust from rotors, and apply forces necessary
        //teleport body to new position
    }

    private static void simulatePhysics(ActiveHelicopter helicopter){
        ItemDisplay body = (ItemDisplay) helicopter.getEntitiesBase()[0];
        Quaternionf bodyRotation = body.getTransformation().getLeftRotation();
        Vector3f direction = new Vector3f(0, 1, 0);
        bodyRotation.transformUnit(direction);
        direction.mul((float) helicopter.getCollective()/2400);

        helicopter.getVelocity().add(Vector.fromJOML(direction)).subtract(new Vector(0, 9.8/400, 0));
        helicopter.getVelocity().setX(Math.max(-1, Math.min(1, helicopter.getVelocity().getX()))*0.995);
        helicopter.getVelocity().setY(Math.max(-1, Math.min(1, helicopter.getVelocity().getY()))*0.995);
        helicopter.getVelocity().setZ(Math.max(-1, Math.min(1, helicopter.getVelocity().getZ()))*0.995);
        WrapperPlayServerEntityMetadata positionRotationInterpolation = new WrapperPlayServerEntityMetadata(body.getEntityId(), List.of(new EntityData(10, EntityDataTypes.INT, 1)));
        for(Player player : Bukkit.getOnlinePlayers()) {
            if (player.getLocation().distanceSquared(body.getLocation()) < body.getViewRange() * body.getViewRange()) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, positionRotationInterpolation);
            }
        }

        HashSet<Vector3f> blockLocation = new HashSet<>();
        CollisionBox defaultBox = new CollisionBox(new Vector3f(-1.5f,-1,2), new Vector3f(1.5f,3,-5));

        CollisionBox collisionBox = new CollisionBox(new Vector3f(-1.5f,-1,2), new Vector3f(1.5f,3,-5));
        collisionBox.transformUnit(bodyRotation);

        Vector3f helicopterMovementVector = helicopter.getVelocity().toVector3f();

        LinkedList<Vector3f> list = collisionBox.getForwardFace(helicopter.getEntitiesBase()[0].getLocation().add(Vector.fromJOML(collisionBox.getLeftBottomForward())), bodyRotation, defaultBox.getHeight(), defaultBox.getWidth());
        blockLocation.addAll(list);

        World world = helicopter.getEntitiesBase()[0].getLocation().getWorld();

        Vector3f upVector = CollisionBox.getTopVector(bodyRotation);
        Vector3f rightVector = CollisionBox.getRightVector(bodyRotation);
        Vector3f forwardVector = CollisionBox.getForwardVector(bodyRotation);

        Vector nudge = new Vector();

        boolean isGrounded = false;
        for(Vector3f blockVector : blockLocation){
            Location location = new Location(world, blockVector.x(), blockVector.y(), blockVector.z());
            Block block = world.getBlockAt(location.add(helicopter.getVelocity()).add(nudge));
            if(block.getType().isSolid() && !helicopter.getVelocity().isZero()){
                isGrounded = true;

                Location rayTracePosition = location.clone().subtract(helicopter.getVelocity().clone().normalize());

                RayTraceResult rayTrace = block.rayTrace(rayTracePosition,
                        helicopter.getVelocity(), 2, FluidCollisionMode.NEVER);
                if(rayTrace != null && rayTracePosition.getBlock() != block){
                    Vector finalVector = rayTrace.getHitPosition().subtract(location.toVector());
                    if(Math.abs(helicopter.getVelocity().getX()+finalVector.getX()) < helicopter.getVelocity().getX() ){
                        helicopter.getVelocity().setX(helicopter.getVelocity().getX()+finalVector.getX());
                    }else{
                        nudge.setX(nudge.getX()+finalVector.getX());
                    }
                    if(Math.abs(helicopter.getVelocity().getY()+finalVector.getY()) < helicopter.getVelocity().getY() ){
                        helicopter.getVelocity().setY(helicopter.getVelocity().getY()+finalVector.getY());
                    }else{
                        nudge.setY(nudge.getY()+finalVector.getY());
                    }

                    if(Math.abs(helicopter.getVelocity().getZ()+finalVector.getZ()) < helicopter.getVelocity().getZ() ){
                        helicopter.getVelocity().setZ(helicopter.getVelocity().getZ()+finalVector.getZ());
                    }else{
                        nudge.setZ(nudge.getZ()+finalVector.getZ());
                    }
                }
            }
        }

        helicopter.setIsGrounded(isGrounded);

        CraftEntity craftEntity = (CraftEntity) body;
        craftEntity.getHandle().teleportTo(((CraftWorld) body.getLocation().getWorld()).getHandle(),
                body.getX() + helicopter.getVelocity().getX() + nudge.getX(),
                body.getY() + helicopter.getVelocity().getY() + nudge.getY(),
                body.getZ() + helicopter.getVelocity().getZ() + nudge.getZ(), Collections.emptySet(), 0, 0);
    }

    private static void handlePlayerRotation(ActiveHelicopter helicopter, Player player, Vector2f playerRotation){
        //get difference between yaw of player and y axis of helicopter
        ItemDisplay body = (ItemDisplay) helicopter.getEntitiesBase()[0];
        Quaternionf bodyRotation = body.getTransformation().getLeftRotation();
        Transformation bodyTransformation = body.getTransformation();
        Vector3f eulerAngles = new Vector3f();
        bodyRotation.getEulerAnglesYXZ(eulerAngles);

        if(player != null){

            double yDifference = player.getYaw()+playerRotation.y();
            if (yDifference > 180) {
                yDifference -= 360;
            }
            if (yDifference <= -180) {
                yDifference += 360;
            }

            double originalYDifference = yDifference;

            if(yDifference - Math.toDegrees(eulerAngles.z) > 5){
                yDifference = Math.toDegrees(eulerAngles.z)+5;
            }else if(yDifference - Math.toDegrees(eulerAngles.z) < -5){
                yDifference = Math.toDegrees(eulerAngles.z)-5;
            }

            if(yDifference > 20){
                yDifference = 20;
            }else if(yDifference < -20){
                yDifference = -20;
            }

            double xDifference = player.getPitch()-playerRotation.x();

            if(xDifference > 70){
                xDifference = 70;
            }else if(xDifference < -70){
                xDifference = -70;
            }

            if(xDifference - Math.toDegrees(eulerAngles.x) > 5){
                xDifference = Math.toDegrees(eulerAngles.x)+5;
            }else if(xDifference - Math.toDegrees(eulerAngles.x) < -5){
                xDifference = Math.toDegrees(eulerAngles.x)-5;
            }

            bodyRotation = new Quaternionf().rotateY(Math.toRadians(playerRotation.y())).rotateX((float) Math.toRadians(xDifference)).rotateZ((float) Math.toRadians(yDifference));

            double extra = 0;
            if(originalYDifference > 20){
                extra = originalYDifference - 20;
            }else if(originalYDifference < -20){
                extra = originalYDifference + 20;
            }
            extra = Math.min(5, Math.max(-5, extra));

            if(Math.abs(playerRotation.y() - extra) > 180){
                playerRotation.y = (float) ((playerRotation.y() + extra) * -1);
            }else{
                playerRotation.y = (float) (playerRotation.y() - extra);
            }


            bodyTransformation.getLeftRotation().set(bodyRotation);
            if(!body.getTransformation().equals(bodyTransformation)){
                body.setTransformation(bodyTransformation);
                body.setInterpolationDelay(0);
                body.setInterpolationDuration(1);
            }

            TextDisplay display = (TextDisplay) helicopter.getEntitiesBase()[2];
            Quaternionf displayRotation = (new Quaternionf().mul(bodyRotation)).rotateY(Math.toRadians(180));

            Vector3f displayRotatedOffset = bodyRotation.transformUnit(new Vector3f(0.5f, 0.9f, 1.1f));
            Transformation displayTransformation = display.getTransformation();
            displayTransformation.getTranslation().set(displayRotatedOffset.x, displayRotatedOffset.y, displayRotatedOffset.z);
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(1);
            displayTransformation.getLeftRotation().set(displayRotation);

            if(!display.getTransformation().equals(displayTransformation)){
                display.setTransformation(displayTransformation);
            }

            ItemDisplay rotor = (ItemDisplay) helicopter.getEntitiesBase()[1];
            Quaternionf rotorRotation = rotor.getTransformation().getLeftRotation();

            Vector3f eulerAngleRotors = new Vector3f();
            rotorRotation.getEulerAnglesZXY(eulerAngleRotors);
            Vector3f rotatedOffset = bodyRotation.transformUnit(new Vector3f(-0.53f, 3.25f, -4f));

            helicopter.setRotorYRotation(helicopter.getRotorYRotation()-100/3.3);
            rotorRotation = bodyRotation.rotateY((float) Math.toRadians(helicopter.getRotorYRotation()));


            Transformation rotorTransformation = rotor.getTransformation();
            rotorTransformation.getTranslation().set(rotatedOffset.x, rotatedOffset.y, rotatedOffset.z);
            rotorTransformation.getLeftRotation().set(rotorRotation);
            rotor.setInterpolationDelay(0);
            rotor.setInterpolationDuration(1);
            if(!rotor.getTransformation().equals(rotorTransformation)){
                rotor.setTransformation(rotorTransformation);
            }
        }
    }
}
