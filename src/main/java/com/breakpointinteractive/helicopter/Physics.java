package com.breakpointinteractive.helicopter;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class Physics {
    public static void simulateHelicopter(ActiveHelicopter helicopter){
        Player playerChecker = null;
        for(Entity entity : helicopter.getEntitiesBase()[0].getPassengers()){
            if(entity instanceof Player target){
                playerChecker = target;
            }
        }
        final Player player = playerChecker;

        AtomicInteger count = new AtomicInteger(0);

        new BukkitRunnable(){
            @Override
            public void run(){
                if(helicopter.getEntitiesBase()[0].getPassengers().size() != 4 && helicopter.getIsGrounded()){
                    cancel();
                    ActiveHelicopter.getActiveHelicopters().remove(helicopter.getEntitiesBase()[0].getEntityId());
                    helicopter.getEntitiesBase()[0].getWorld().stopSound(SoundStop.named(Key.key("helicopter", "helicopter")));
                    return;
                }if(count.getAndAdd(1) % 198 == 0){
                    helicopter.getEntitiesBase()[1].getWorld().playSound(Sound.sound(Key.key("helicopter", "helicopter"), Sound.Source.AMBIENT, 5, 1), helicopter.getEntitiesBase()[0]);
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
        //summon particles
        if(helicopter.getRPM() < 258){
            helicopter.setRPM(helicopter.getRPM() + 1);
        }else{
            for(int i = 1; i < 10; i++){
                //if there is a solid block within 10 blocks below the helicopter, then spawn particles
                if(helicopter.getEntitiesBase()[0].getLocation().add(0,-i,0).getBlock().isSolid()){
                    double x = ThreadLocalRandom.current().nextDouble(1, 2)*(ThreadLocalRandom.current().nextBoolean() ? 1 : -1);
                    double z = ThreadLocalRandom.current().nextDouble(1, 2)*(ThreadLocalRandom.current().nextBoolean() ? 1 : -1);
                    helicopter.getEntitiesBase()[0].getWorld().spawnParticle(Particle.CLOUD,
                            helicopter.getEntitiesBase()[0].getLocation().add(x, 0, z), 0,
                            x*0.25, ThreadLocalRandom.current().nextDouble(-0.5, 0), z*0.25);
                    break;
                }
            }
        }

        ItemDisplay body = (ItemDisplay) helicopter.getEntitiesBase()[0];
        Quaternionf bodyRotation = body.getTransformation().getLeftRotation();
        Vector3f direction = new Vector3f(0, 1, 0);
        bodyRotation.transformUnit(direction);
        direction.mul((float) (helicopter.getCollective()/2400.*helicopter.getRPM()/258));

        helicopter.getVelocity().add(Vector.fromJOML(direction)).subtract(new Vector(0, 9.8/400, 0));

        //max is 20 meters per second total combined magnitude
        double x = helicopter.getVelocity().getX();
        double y = helicopter.getVelocity().getY();
        double z = helicopter.getVelocity().getZ();
        double combined = x*x + y*y + z*z;
        if(combined > 400){ //if exceeding the maximum speed, multiply vector by inverse square root to normalize
            double inverseSquareRoot = invSqrt(combined);
            helicopter.getVelocity().setX(helicopter.getVelocity().getX()*inverseSquareRoot*20);
            helicopter.getVelocity().setY(helicopter.getVelocity().getY()*inverseSquareRoot*20);
            helicopter.getVelocity().setZ(helicopter.getVelocity().getZ()*inverseSquareRoot*20);
        }else{
            //simulate wind resistance by multiplying velocity down
            helicopter.getVelocity().setX(helicopter.getVelocity().getX() * 0.995);
            helicopter.getVelocity().setY(helicopter.getVelocity().getY() * 0.995);
            helicopter.getVelocity().setZ(helicopter.getVelocity().getZ() * 0.995);
        }

        WrapperPlayServerEntityMetadata positionRotationInterpolation = new WrapperPlayServerEntityMetadata(body.getEntityId(), List.of(new EntityData(10, EntityDataTypes.INT, 1)));
        for(Player player : Bukkit.getOnlinePlayers()) {
            if (player.getLocation().distanceSquared(body.getLocation()) < body.getViewRange() * body.getViewRange()) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, positionRotationInterpolation);
            }
        }

        HashSet<Vector3f> blockLocation = new HashSet<>();
        CollisionBox defaultBox = new CollisionBox(new Vector3f(-1.5f,-1,1), new Vector3f(1.5f,3,-14));

        CollisionBox collisionBox = new CollisionBox(new Vector3f(-1.5f,-1,1), new Vector3f(1.5f,3,-14));
        collisionBox.transformUnit(bodyRotation);


        Vector3f forwardVector = CollisionBox.getForwardVector(bodyRotation);
        Vector3f backwardVector = new Vector3f(-forwardVector.x(), -forwardVector.y(), -forwardVector.z());
        Vector3f rightVector = CollisionBox.getRightVector(bodyRotation);
        Vector3f leftVector = new Vector3f(-rightVector.x(), -rightVector.y(), -rightVector.z());
        Vector3f topVector = CollisionBox.getTopVector(bodyRotation);
        Vector3f bottomVector = new Vector3f(-topVector.x(), -topVector.y(), -topVector.z());

        //if the dot product of the face and velocity is positive, then it is within 90 degrees of the other.
        //depending on the direction of the helicopter, only the faces in that direction should try to detect collisions.
        if(helicopter.getVelocity().dot(Vector.fromJOML(forwardVector)) > 0){
            LinkedList<Vector3f> forwardFace = collisionBox.getFace(helicopter.getEntitiesBase()[0].getLocation()
                    .add(Vector.fromJOML(collisionBox.getLeftBottomForward())), defaultBox.getHeight(), defaultBox.getWidth(), topVector, rightVector);
            blockLocation.addAll(forwardFace);
        }else{
            LinkedList<Vector3f> backwardFace = collisionBox.getFace(helicopter.getEntitiesBase()[0].getLocation()
                    .add(Vector.fromJOML(collisionBox.getRightTopBackward())), defaultBox.getHeight(), defaultBox.getWidth(), bottomVector, leftVector);
            blockLocation.addAll(backwardFace);
        }

        if(helicopter.getVelocity().dot(Vector.fromJOML(rightVector)) > 0) {
            LinkedList<Vector3f> rightFace = collisionBox.getFace(helicopter.getEntitiesBase()[0].getLocation()
                    .add(Vector.fromJOML(collisionBox.getRightTopBackward())), defaultBox.getHeight(), defaultBox.getDepth(), topVector, forwardVector);
            blockLocation.addAll(rightFace);
        }else{
            LinkedList<Vector3f> leftFace = collisionBox.getFace(helicopter.getEntitiesBase()[0].getLocation()
                    .add(Vector.fromJOML(collisionBox.getLeftBottomForward())), defaultBox.getHeight(), defaultBox.getDepth(), topVector, backwardVector);
            blockLocation.addAll(leftFace);
        }

        if(helicopter.getVelocity().dot(Vector.fromJOML(topVector)) > 0) {
            LinkedList<Vector3f> topFace = collisionBox.getFace(helicopter.getEntitiesBase()[0].getLocation()
                    .add(Vector.fromJOML(collisionBox.getRightTopBackward())), defaultBox.getWidth(), defaultBox.getDepth(), leftVector, forwardVector);
            blockLocation.addAll(topFace);
        }else {
            LinkedList<Vector3f> bottomFace = collisionBox.getFace(helicopter.getEntitiesBase()[0].getLocation()
                    .add(Vector.fromJOML(collisionBox.getLeftBottomForward())), defaultBox.getWidth(), defaultBox.getDepth(), rightVector, backwardVector);
            blockLocation.addAll(bottomFace);
        }

        World world = helicopter.getEntitiesBase()[0].getLocation().getWorld();

        //the nudge variable is used to make sure that elastic collisions are not required when
        Vector nudge = new Vector();

        boolean isGrounded = false;
        for(Vector3f blockVector : blockLocation){
            Location location = new Location(world, blockVector.x(), blockVector.y(), blockVector.z());
            Block block = world.getBlockAt(location.add(helicopter.getVelocity()).add(nudge));
            if(block.getType().isSolid() && !helicopter.getVelocity().isZero()){
                isGrounded = true;

                Location rayTracePosition = location.clone().subtract(fastNormalize(helicopter.getVelocity()));

                RayTraceResult rayTrace = block.rayTrace(rayTracePosition,
                        helicopter.getVelocity(), 1, FluidCollisionMode.NEVER);
                //additional check: if there is a solid block after adding the face vector and retrieving,
                //then raycast by that instead
                if(rayTrace != null){
                    Block newBlock = block.getLocation().add(rayTrace.getHitBlockFace().getDirection()).getBlock();
                    if(newBlock.isSolid()){
                        RayTraceResult secondRayTrace = newBlock.rayTrace(rayTracePosition,
                                helicopter.getVelocity(), 1, FluidCollisionMode.NEVER);
                        if(secondRayTrace != null && !newBlock.getLocation().add(secondRayTrace.getHitBlockFace().getDirection()).getBlock().isSolid()){
                            rayTrace = secondRayTrace;
                            block = newBlock;
                        }else{
                            //if there is a solid block afterwards, then just ignore it
                            // (or else helicopter will start phasing through blocks)
                            rayTrace = null;
                        }
                    }
                }

                if(rayTrace != null && rayTracePosition.getBlock() != block){ //to make sure the raytrace isn't inside the block
                    Vector finalVector = rayTrace.getHitPosition().subtract(location.toVector());
                    Vector negatedVelocity = new Vector();
                    if(rayTrace.getHitBlockFace() != BlockFace.UP && rayTrace.getHitBlockFace() != BlockFace.DOWN){
                        nudge.setX(nudge.getX()+finalVector.getX()+helicopter.getVelocity().getX());
                        negatedVelocity.setX(helicopter.getVelocity().getX());
                        helicopter.getVelocity().setX(0);

                        nudge.setZ(nudge.getZ()+finalVector.getZ()+helicopter.getVelocity().getZ());
                        negatedVelocity.setZ(helicopter.getVelocity().getZ());
                        helicopter.getVelocity().setZ(0);
                    }else{
                        nudge.setY(nudge.getY()+finalVector.getY()+helicopter.getVelocity().getY());
                        negatedVelocity.setY(helicopter.getVelocity().getY());
                        helicopter.getVelocity().setY(0);

                        helicopter.getVelocity().setX(helicopter.getVelocity().getX()*0.92);
                        helicopter.getVelocity().setZ(helicopter.getVelocity().getZ()*0.92);
                    }
                    if(negatedVelocity.getX()*negatedVelocity.getX() + negatedVelocity.getY()*negatedVelocity.getY() + negatedVelocity.getZ()*negatedVelocity.getZ() > 0.25){
                        world.createExplosion(helicopter.getEntitiesBase()[0], 6);
                        for(int i = 0; i < 15; i++){
                            helicopter.getEntitiesBase()[0].getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, helicopter.getEntitiesBase()[0].getLocation().add(ThreadLocalRandom.current().nextDouble(-2, 2),
                                    ThreadLocalRandom.current().nextDouble(-2, 2),
                                    ThreadLocalRandom.current().nextDouble(-2, 2)), 0, 0, 0, 0);
                            helicopter.getEntitiesBase()[0].getWorld().spawnParticle(Particle.LAVA, helicopter.getEntitiesBase()[0].getLocation().add(ThreadLocalRandom.current().nextDouble(-2, 2),
                                    ThreadLocalRandom.current().nextDouble(-2, 2),
                                    ThreadLocalRandom.current().nextDouble(-2, 2)), 0, 0, 0, 0);
                        }

                        helicopter.getEntitiesBase()[0].eject();
                        for(Entity target : helicopter.getEntitiesBase()){
                            target.remove();
                        }
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
            Vector3f rotatedOffset = bodyRotation.transformUnit(new Vector3f(-0.53f, 3.6f, -4f));

            helicopter.setRotorYRotation(helicopter.getRotorYRotation()-(100/3.3*helicopter.getRPM()/258.));
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
