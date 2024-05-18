package com.breakpointinteractive.helicopter;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.RelativeMovement;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Physics {
    public static void simulateHelicopter(ActiveHelicopter helicopter){
        final Vector playerRotation = new Vector(0,0,0);
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
                if(helicopter == null || helicopter.getEntitiesBase()[0].getPassengers().size() != 3){
                    cancel();
                    assert helicopter != null;
                    ActiveHelicopter.getActiveHelicopters().remove(helicopter.getEntitiesBase()[0].getEntityId());
                    return;
                }

                //simulateRotors(helicopter);

                handlePlayerRotation(helicopter, player, playerRotation);

                simulatePhysics(helicopter);

            }
        }.runTaskTimer(Helicopter.getInstance(), 0L, 1L);
        //calculate thrust from rotors, and apply forces necessary
        //teleport body to new position
    }

    private static void simulatePhysics(ActiveHelicopter helicopter){
        ItemDisplay body = (ItemDisplay) helicopter.getEntitiesBase()[0];
        Quaternionf bodyRotation = body.getTransformation().getLeftRotation();
        Vector3f eulerAngles = new Vector3f();
        bodyRotation.rotateX(Math.toRadians(-90)).getEulerAnglesYXZ(eulerAngles);

        //https://stackoverflow.com/questions/1568568/how-to-convert-euler-angles-to-directional-vector
        Vector3f direction = new Vector3f(-Math.cos(eulerAngles.y)*Math.sin(eulerAngles.x)*Math.sin(eulerAngles.z)-Math.sin(eulerAngles.y)*Math.cos(eulerAngles.z),
                -Math.sin(eulerAngles.y)*Math.sin(eulerAngles.x)*Math.sin(eulerAngles.z)+Math.cos(eulerAngles.y)*Math.cos(eulerAngles.z),
                Math.sin(eulerAngles.x)*Math.cos(eulerAngles.z));

        Bukkit.broadcastMessage("X: " + Math.toDegrees(eulerAngles.x) + " Y: " + Math.toDegrees(eulerAngles.y) + " Z: " + Math.toDegrees(eulerAngles.z));

        //helicopter.getVelocity().add(new Vector(bodyRotation.x, bodyRotation.y, bodyRotation.z).multiply(helicopter.getCollective()/100).subtract(new Vector(0, 9.8/200, 0)));
        WrapperPlayServerEntityMetadata positionRotationInterpolation = new WrapperPlayServerEntityMetadata(body.getEntityId(), List.of(new EntityData(10, EntityDataTypes.INT, 1)));
        for(Player player : Bukkit.getOnlinePlayers()) {
            if (player.getLocation().distanceSquared(body.getLocation()) < body.getViewRange() * body.getViewRange()) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, positionRotationInterpolation);
            }
        }
        CraftEntity craftEntity = (CraftEntity) body;
        craftEntity.getHandle().teleportTo(((CraftWorld) body.getLocation().getWorld()).getHandle(), body.getX() + direction.x/10,
                body.getY() + direction.y/10,
                body.getZ() + direction.z/10, Collections.emptySet(), 0, 0);
    }

    private static void handlePlayerRotation(ActiveHelicopter helicopter, Player player, Vector playerRotation){
        //get difference between yaw of player and y axis of helicopter
        ItemDisplay body = (ItemDisplay) helicopter.getEntitiesBase()[0];
        Quaternionf bodyRotation = body.getTransformation().getLeftRotation();
        Transformation bodyTransformation = body.getTransformation();
        Vector3f eulerAngles = new Vector3f();
        bodyRotation.getEulerAnglesYXZ(eulerAngles);

        if(player != null){

            double yDifference = player.getYaw()+playerRotation.getY();
            if (yDifference > 180) {
                yDifference -= 360;
            }
            if (yDifference <= -180) {
                yDifference += 360;
            }
            double originalYDifference = yDifference;

            if(yDifference > 20){
                yDifference = 20;
            }else if(yDifference < -20){
                yDifference = -20;
            }

            double xDifference = player.getPitch()-playerRotation.getX();

            if(xDifference > 70){
                xDifference = 70;
            }else if(xDifference < -70){
                xDifference = -70;
            }


            if(Math.abs(xDifference) > 0.01 || Math.abs(yDifference) > 0.01){

                bodyRotation = new Quaternionf().rotateY((float) Math.toRadians(playerRotation.getY())).rotateX((float) Math.toRadians(xDifference)).rotateZ((float) Math.toRadians(yDifference));

                double extra = 0;
                if(originalYDifference > 20){
                    extra = originalYDifference - 20;
                }else if(originalYDifference < -20){
                    extra = originalYDifference + 20;
                }
                if(Math.abs(playerRotation.getY() - extra) > 180){
                    playerRotation.setY((playerRotation.getY() + extra) * -1);
                }else{
                    playerRotation.setY(playerRotation.getY() - extra);
                }


                bodyTransformation.getLeftRotation().set(bodyRotation);
                if(!body.getTransformation().equals(bodyTransformation)){
                    body.setTransformation(bodyTransformation);
                    body.setInterpolationDelay(0);
                    body.setInterpolationDuration(1);
                }

                ItemDisplay rotor = (ItemDisplay) helicopter.getEntitiesBase()[1];
                Quaternionf rotorRotation = rotor.getTransformation().getLeftRotation();

                Vector3f eulerAngleRotors = new Vector3f();
                rotorRotation.getEulerAnglesZXY(eulerAngleRotors);
                Vector3f rotatedOffset = bodyRotation.transformUnit(new Vector3f(-0.53f, 3.75f, -4f));

                helicopter.setRotorYRotation(helicopter.getRotorYRotation()-helicopter.getCollective()/3.3);
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
}
