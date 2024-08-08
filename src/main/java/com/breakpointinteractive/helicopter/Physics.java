package com.breakpointinteractive.helicopter;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.RelativeMovement;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.*;
import org.joml.Math;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class Physics {
    public static void simulateHelicopter(ActiveHelicopter helicopter){

        AtomicInteger count = new AtomicInteger(0);

        new BukkitRunnable(){
            @Override
            public void run(){
                if(helicopter.getEntitiesBase()[0].getPassengers().size() != 4 || helicopter.getEntitiesBase()[0].isDead()){
                    if((helicopter.getIsGrounded() && helicopter.getRPM() == 0) || helicopter.getEntitiesBase()[0].isDead()){
                        cancel();
                        ActiveHelicopter.getActiveHelicopters().remove(helicopter.getEntitiesBase()[0].getEntityId());
                        helicopter.getEntitiesBase()[0].getWorld().stopSound(SoundStop.named(Key.key("infiltration", "flying")));
                        return;
                    }
                    helicopter.setRPM(Math.max(0, helicopter.getRPM()-1));
                }
                if(helicopter.getRPM() == 258 && count.getAndAdd(1) % 198 == 0) {
                    helicopter.getEntitiesBase()[1].getWorld().playSound(Sound.sound(Key.key("infiltration", "flying"), Sound.Source.AMBIENT, 5, 1), helicopter.getEntitiesBase()[0]);
                }

                Player player = null;
                for(Entity entity : helicopter.getEntitiesBase()[0].getPassengers()){
                    if(entity instanceof Player target){
                        player = target;
                        break;
                    }
                }


                if(System.currentTimeMillis()-helicopter.getLastRightClick() > 250 && player != null){
                    if(helicopter.isFreeLooking()){
                        helicopter.setFreeLooking(false);
                        player.removePotionEffect(PotionEffectType.SLOWNESS);


                        Vector3f eulerAngles = new Vector3f();
                        ((ItemDisplay)helicopter.getEntitiesBase()[0]).getTransformation().getLeftRotation().getEulerAnglesYXZ(eulerAngles);
                        WrapperPlayServerPlayerPositionAndLook positionAndLook =
                                new WrapperPlayServerPlayerPositionAndLook(0,0,0,
                                        (float) Math.toDegrees(-eulerAngles.y+eulerAngles.z),(float) Math.toDegrees(eulerAngles.x),(byte) 0b00111, 0, false);
                        PacketEvents.getAPI().getPlayerManager().sendPacket(player, positionAndLook);
                    }
                }else{
                    if(!helicopter.isFreeLooking() && player != null){
                        helicopter.setFreeLooking(true);
                        //reset fov
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, PotionEffect.INFINITE_DURATION, 1, false, false, false));
                    }
                }
                float prevY = helicopter.getBodyRotation().y;

                handlePlayerRotation(helicopter, player);
                simulatePhysics(helicopter);
                rotateHelicopter(helicopter, prevY);
                animateRotors(helicopter);
                moveSeats(helicopter);

            }
        }.runTaskTimer(Helicopter.getInstance(), 0L, 1L);
        //calculate thrust from rotors, and apply forces necessary
        //teleport body to new position
    }

    private static void moveSeats(ActiveHelicopter helicopter){
        Vector3f offset = new Vector3f();
        for(int i = 0; i < 5; i++) {
            if (helicopter.getSeats()[i] != null) {
                if (i % 2 == 1) {
                    offset.x = 0;
                }else{
                    offset.x = -1.25f;
                }
                offset.z = (float) (((i+1) / 2) * (-1.2));

                Quaternionf attackAngle = ((ItemDisplay)helicopter.getEntitiesBase()[0]).getTransformation().getLeftRotation();
                attackAngle.transformUnit(offset);

                Location helicopterLocation = helicopter.getEntitiesBase()[0].getLocation();

                CraftEntity craftEntity = (CraftEntity) helicopter.getSeats()[i];
                craftEntity.getHandle().teleportTo(((CraftWorld)(helicopter.getEntitiesBase()[0].getWorld())).getHandle(),
                        helicopterLocation.getX() + offset.x(),
                        helicopterLocation.getY() + offset.y(),
                        helicopterLocation.getZ() + offset.z(), Collections.emptySet(), (float) Math.toDegrees(helicopter.getBodyRotation().y), 0);
            }
        }
    }

    public static double calculateDensity(double height) {
        if (height <= 200) {
            return 1.0;
        } else if (height >= 319) {
            return 0.0;
        } else {
            // Linear interpolation
            return 1.0 - (height - 200) / (319 - 200);
        }
    }

    private static void simulatePhysics(ActiveHelicopter helicopter){
        ItemDisplay helicopterBody = (ItemDisplay) helicopter.getEntitiesBase()[0];
        World world = helicopterBody.getLocation().getWorld();

        if(helicopter.getRPM() < 258 && helicopter.getEntitiesBase()[0].getPassengers().size() == 4){
            helicopter.setRPM(helicopter.getRPM() + 1);
        }
        if(helicopter.getRPM() > 100){
            for(int i = 1; i < 15; i++){
                //if there is a solid block within 15 blocks below the helicopter, then spawn particles
                if(helicopterBody.getLocation().add(0,-i,0).getBlock().isSolid() || helicopterBody.getLocation().add(0,-i,0).getBlock().isLiquid()){
                    Vector3f offset = new Vector3f(0, 0, -4);
                    helicopterBody.getTransformation().getLeftRotation().transformUnit(offset);

                    double x = ThreadLocalRandom.current().nextDouble(-1, 1);
                    double z = ThreadLocalRandom.current().nextDouble(-1, 1);
                    double lengthSquared = x * x + z * z;
                    double invSqrt = invSqrt(lengthSquared);
                    x *= invSqrt*6;
                    z *= invSqrt*6;

                    world.spawnParticle(Particle.CLOUD,
                            helicopterBody.getLocation().add(x, 0, z).add(Vector.fromJOML(offset)), 0,
                            x*0.1, ThreadLocalRandom.current().nextDouble(-0.5, -0.1), z*0.1);
                    break;
                }
            }
        }

        Quaternionf bodyRotation = new Quaternionf().rotateY(helicopter.getBodyRotation().y).rotateX(helicopter.getBodyRotation().x).rotateZ(helicopter.getBodyRotation().z);
        Vector3f direction = new Vector3f(0, 1, 0);
        bodyRotation.transformUnit(direction);

        float airDensity = (float) calculateDensity(helicopter.getEntitiesBase()[0].getLocation().y());
        direction.mul((float) (helicopter.getCollective()/3200.*helicopter.getRPM()/258*airDensity));

        helicopter.getVelocity().add(Vector.fromJOML(direction)).subtract(new Vector(0, 9.8/400, 0));

        //max is 20 meters per second total combined magnitude
        double combined = helicopter.getVelocity().dot(helicopter.getVelocity());
        if(combined > 2){ //if exceeding the maximum speed, multiply vector by inverse square root to normalize
            double inverseSquareRoot = invSqrt(combined);
            helicopter.getVelocity().multiply(inverseSquareRoot*1.41);
        }else{
            //simulate wind resistance by multiplying velocity down
            helicopter.getVelocity().multiply(0.995);
        }

        Vector3f forwardVector = CollisionBox.getForwardVector(bodyRotation);
        Vector3f leftVector = CollisionBox.getLeftVector(bodyRotation);
        Vector3f topVector = CollisionBox.getTopVector(bodyRotation);

        Vector3f offset = new Vector3f(0.5f, 1f, -6f);
        bodyRotation.transformUnit(offset);

        CollisionBox collisionBox = new CollisionBox(helicopterBody.getLocation().toVector().toVector3f().add(offset), new Vector3f(1.3f, 2f, 8f));

        PositionRotation positionRotation;

        helicopter.setIsGrounded(false);

        if(helicopter.getVelocity().dot(Vector.fromJOML(topVector)) > 0) {
            positionRotation = collisionBox.getFace(CollisionBox.Face.TOP, helicopter, forwardVector, leftVector, topVector, bodyRotation);
        }else{
            positionRotation = collisionBox.getFace(CollisionBox.Face.BOTTOM, helicopter, forwardVector, leftVector, topVector, bodyRotation);
        }
        helicopter.getBodyRotation().x = positionRotation.getPitch();
        helicopter.getBodyRotation().y = positionRotation.getYaw();
        helicopter.getBodyRotation().z = positionRotation.getRoll();
        bodyRotation = new Quaternionf().rotateY(positionRotation.getYaw()).rotateX(positionRotation.getPitch()).rotateZ(positionRotation.getRoll());
        collisionBox.setCenter(positionRotation.getPosition());
        if(helicopter.getVelocity().dot(Vector.fromJOML(forwardVector)) > 0){
            positionRotation = collisionBox.getFace(CollisionBox.Face.FORWARD, helicopter, forwardVector, leftVector, topVector, bodyRotation);
        }else{
            positionRotation = collisionBox.getFace(CollisionBox.Face.BACKWARD, helicopter, forwardVector, leftVector, topVector, bodyRotation);
        }
        collisionBox.setCenter(positionRotation.getPosition());
        if(helicopter.getVelocity().dot(Vector.fromJOML(leftVector)) > 0) {
            positionRotation = collisionBox.getFace(CollisionBox.Face.LEFT, helicopter, forwardVector, leftVector, topVector, bodyRotation);
        }else{
            positionRotation = collisionBox.getFace(CollisionBox.Face.RIGHT, helicopter, forwardVector, leftVector, topVector, bodyRotation);
        }
        collisionBox.setCenter(positionRotation.getPosition());

        Vector3f seatOffset = new Vector3f(-0.5f, -1f, 6f);
        bodyRotation.transformUnit(seatOffset);

        helicopter.getBodyRotation().x = positionRotation.getPitch();
        helicopter.getBodyRotation().y = positionRotation.getYaw();
        helicopter.getBodyRotation().z = positionRotation.getRoll();
        CraftEntity craftEntity = (CraftEntity) helicopterBody;
        craftEntity.getHandle().teleportTo(((CraftWorld) helicopterBody.getLocation().getWorld()).getHandle(),
                positionRotation.getPosition().getX() + seatOffset.x,
                positionRotation.getPosition().getY() + seatOffset.y,
                positionRotation.getPosition().getZ() + seatOffset.z, Collections.emptySet(), 0, 0);
    }

    public static Vector fastNormalize(Vector vector){
        double x = vector.getX();
        double y = vector.getY();
        double z = vector.getZ();
        double inverseSquareRoot =invSqrt(x*x + y*y + z*z);
        return new Vector(vector.getX()*inverseSquareRoot, vector.getY()*inverseSquareRoot, vector.getZ()*inverseSquareRoot);
    }

    //https://stackoverflow.com/questions/11513344/how-to-implement-the-fast-inverse-square-root-in-java
    //originally from Quake III, converted over from C to java

    public static double invSqrt(double x) {
        double xhalf = 0.5d * x;
        long i = Double.doubleToLongBits(x);
        i = 0x5fe6ec85e7de30daL - (i >> 1);
        x = Double.longBitsToDouble(i);
        x *= (1.5d - xhalf * x * x);
        return x;
    }


    private static void animateRotors(ActiveHelicopter helicopter){
        ItemDisplay body = (ItemDisplay) helicopter.getEntitiesBase()[0];
        Quaternionf bodyRotation = body.getTransformation().getLeftRotation();

        Vector3f rotatedOffset = bodyRotation.transformUnit(new Vector3f(-0.67f, 1.9f, -4f));

        helicopter.setRotorYRotation(helicopter.getRotorYRotation()-(50*helicopter.getRPM()/258.));
        Quaternionf rotorRotation = bodyRotation.rotateY((float) Math.toRadians(helicopter.getRotorYRotation()));

        ItemDisplay rotor = (ItemDisplay) helicopter.getEntitiesBase()[1];
        Transformation rotorTransformation = rotor.getTransformation();
        rotorTransformation.getTranslation().set(rotatedOffset.x, rotatedOffset.y+0.75, rotatedOffset.z);
        rotorTransformation.getLeftRotation().set(rotorRotation);
        rotor.setInterpolationDelay(0);
        rotor.setInterpolationDuration(1);
        if(!rotor.getTransformation().equals(rotorTransformation)){
            rotor.setTransformation(rotorTransformation);
        }
    }

    private static void handlePlayerRotation(ActiveHelicopter helicopter, Player player){
        if(player != null && !helicopter.isFreeLooking()){
            double rotationPower = Math.min(1, helicopter.getCollective()/60.)*helicopter.getRPM()/258.;
            if(!helicopter.getIsGrounded()){
                rotationPower = 1;
            }

            double yDifference = Math.toRadians(player.getYaw())+helicopter.getBodyRotation().y();

            if (yDifference > Math.toRadians(180)) {
                yDifference -= Math.toRadians(360);
            }
            if (yDifference <= Math.toRadians(-180)) {
                yDifference += Math.toRadians(360);
            }

            double originalYDifference = yDifference;

            if(yDifference > Math.toRadians(20)){
                yDifference = Math.toRadians(20);
            }else if(yDifference < Math.toRadians(-20)){
                yDifference = Math.toRadians(-20);
            }

            double zDegrees = helicopter.getBodyRotation().z;
            if(yDifference - zDegrees > Math.toRadians(12)){
                yDifference = zDegrees+Math.toRadians(3)*rotationPower;
            }else if(yDifference - zDegrees > 0){
                yDifference = zDegrees+(yDifference-zDegrees)/4*rotationPower;
            }else if(yDifference - zDegrees < Math.toRadians(-12)){
                yDifference = zDegrees-Math.toRadians(3)*rotationPower;
            }else if(yDifference - zDegrees < 0){
                yDifference = zDegrees+(yDifference-zDegrees)/4*rotationPower;
            }
            helicopter.getBodyRotation().z = (float) yDifference;

            double xDifference = Math.toRadians(player.getPitch());

            if(xDifference > Math.toRadians(60)){
                xDifference = Math.toRadians(60);
            }else if(xDifference < Math.toRadians(-60)){
                xDifference = Math.toRadians(-60);
            }

            double xDegrees = helicopter.getBodyRotation().x;
            if(xDifference - xDegrees > Math.toRadians(12)) {
                xDifference = xDegrees+Math.toRadians(3)*rotationPower;
            }else if(xDifference - xDegrees > 0){
                xDifference = xDegrees+(xDifference - xDegrees)/4*rotationPower;
            }else if(xDifference - xDegrees < Math.toRadians(-12)){
                xDifference = xDegrees-Math.toRadians(3)*rotationPower;
            } else if(xDifference - xDegrees < 0){
                xDifference = xDegrees+(xDifference - xDegrees)/4*rotationPower;
            }
            helicopter.getBodyRotation().x = (float) xDifference;

            double extra = 0;
            if(originalYDifference > Math.toRadians(20)){
                extra = originalYDifference - Math.toRadians(20);
            }else if(originalYDifference < Math.toRadians(-20)){
                extra = originalYDifference + Math.toRadians(20);
            }
            if(Math.abs(extra) < Math.toRadians(12)){
                extra /= 4;
            }else{
                extra = Math.clamp(Math.toRadians(-3), Math.toRadians(3), extra);
            }

            if(Math.abs(helicopter.getBodyRotation().y - extra*rotationPower) > Math.toRadians(180)){
                helicopter.getBodyRotation().y = (float) ((helicopter.getBodyRotation().y + extra*rotationPower) * -1);
            }else{
                helicopter.getBodyRotation().y = (float) (helicopter.getBodyRotation().y - extra*rotationPower);
            }
        }
    }
    public static void rotateHelicopter(ActiveHelicopter helicopter, float prevY){
        ItemDisplay body = (ItemDisplay) helicopter.getEntitiesBase()[0];
        Quaternionf bodyRotation;
        Transformation bodyTransformation = body.getTransformation();

        bodyRotation = new Quaternionf().rotateY(helicopter.getBodyRotation().y).rotateX(helicopter.getBodyRotation().x).rotateZ(helicopter.getBodyRotation().z);

        float yDifference = (float) Math.toDegrees(prevY-helicopter.getBodyRotation().y);
        if (yDifference > 180) {
            yDifference -= 360;
        }
        if (yDifference <= -180) {
            yDifference += 360;
        }

        for(Entity seat : helicopter.getSeats()){
            if(seat != null){
                seat.getPassengers().get(0).setRotation((float) -Math.toDegrees(helicopter.getBodyRotation().y), 0);
                CameraHandler.getQueuedShiftAmount().put((Player) seat.getPassengers().get(0).getPassengers().get(0),
                        CameraHandler.getQueuedShiftAmount().getOrDefault((Player) seat.getPassengers().get(0).getPassengers().get(0), new Vector(0,yDifference,0)).add(new Vector(0,yDifference,0)));
            }
        }



        bodyTransformation.getLeftRotation().set(bodyRotation);
        if(!body.getTransformation().equals(bodyTransformation)){
            body.setTransformation(bodyTransformation);
            body.setInterpolationDelay(0);
            body.setInterpolationDuration(1);
        }

        TextDisplay display = (TextDisplay) helicopter.getEntitiesBase()[2];
        Quaternionf displayRotation = (new Quaternionf().mul(bodyRotation)).rotateY(Math.toRadians(180));

        Vector3f displayRotatedOffset = bodyRotation.transformUnit(new Vector3f(0.65f, 0.3f, 0.8f));
        Transformation displayTransformation = display.getTransformation();
        displayTransformation.getTranslation().set(displayRotatedOffset.x, displayRotatedOffset.y+0.75, displayRotatedOffset.z);
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(1);
        displayTransformation.getLeftRotation().set(displayRotation);

        if(!display.getTransformation().equals(displayTransformation)){
            display.setTransformation(displayTransformation);
        }
    }
}
