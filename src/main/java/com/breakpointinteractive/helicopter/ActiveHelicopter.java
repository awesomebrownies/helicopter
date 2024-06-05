package com.breakpointinteractive.helicopter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.HashMap;

public class ActiveHelicopter {
    public static final HashMap<Integer, ActiveHelicopter> activeHelicopters = new HashMap<>();

    private final Vector velocity = new Vector(0,0,0);
    private final Vector angularVelocity = new Vector(0,0,0);
    private Quaternionf attackAngle;
    private int collective = 0;
    private double rotorYRotation;
    private float rpm = 0;
    private Entity[] entitiesBase;
    private final Vector2f playerRotation = new Vector2f(0,0);
    private final CollisionBox[] collisionBoxes = new CollisionBox[1];
    private boolean isGrounded;

    public ActiveHelicopter(Entity helicopterBase){
        entitiesBase = new Entity[helicopterBase.getPassengers().size()];
        entitiesBase[0] = helicopterBase;
        for(int i = 1; i < helicopterBase.getPassengers().size(); i++){
            entitiesBase[i] = helicopterBase.getPassengers().get(i);
        }
        activeHelicopters.put(helicopterBase.getEntityId(), this);
    }

    //create instance of a helicopter and summon in necessary displays
    public ActiveHelicopter(Quaternionf attackAngle, Location location){
        this.attackAngle = attackAngle; //starting rotation
        initializeParts(attackAngle, location);
        Physics.simulateHelicopter(this);
    }

    public void initializeParts(Quaternionf attackAngle, Location location){
        //(helicopter body) spawn the helicopter display facing the attack angle. NOTE: HUD part 1 part of helicopter model
        //(helicopter rotor) spawn the rotor display facing the attack angle, but have an offset of a couple blocks on the y-axis.
        //helicopter collective + health (newline) text display
        //HUD part 2
        ItemDisplay body = initializePart(attackAngle, location, Material.MELON_SEEDS, new Vector(0,0.75,0));
        body.customName(Component.text("helicopter"));
        activeHelicopters.put(body.getEntityId(), this);

        ItemDisplay rotor = initializePart(attackAngle, location, Material.RABBIT_FOOT, new Vector(-0.53f, 3.6f, -4f));

        TextDisplay display = initializeDisplay(attackAngle, location, new Vector(0.25, 0.9, -1.1));

        Interaction hitbox = (Interaction) location.getWorld().spawnEntity(location, EntityType.INTERACTION);
        hitbox.setInteractionHeight(3);
        hitbox.setInteractionWidth(4);
        hitbox.setResponsive(true);

        entitiesBase = new Entity[] { body, rotor, display, hitbox };

        body.addPassenger(rotor);
        body.addPassenger(display);
        body.addPassenger(hitbox);
    }

    private TextDisplay initializeDisplay(Quaternionf attackAngle, Location location, Vector offset){
        TextDisplay display = (TextDisplay) location.getWorld().spawnEntity(location, EntityType.TEXT_DISPLAY);

        Transformation displayTransformation = display.getTransformation();
        attackAngle.rotateY((float) Math.toRadians(180));
        displayTransformation.getLeftRotation().set(attackAngle);
        Vector3f rotatedOffset = attackAngle.transformUnit(new Vector3f((float) offset.getX(), (float) offset.getY(), (float) offset.getZ()));

        displayTransformation.getTranslation().set(rotatedOffset.x, rotatedOffset.y, rotatedOffset.z);
        displayTransformation.getScale().set(0.25,0.25,0.25);
        display.setTransformation(displayTransformation);

        display.setShadowed(true);
        display.text(Component.text("--0%").color(TextColor.color(150,255,150)));
        display.setBackgroundColor(Color.fromARGB(0));

        return display;
    }

    //helper function to spawning in helicopter parts
    private ItemDisplay initializePart(Quaternionf attackAngle, Location location, Material itemType, Vector offset){
        ItemDisplay display = (ItemDisplay) location.getWorld().spawnEntity(location, EntityType.ITEM_DISPLAY);

        Transformation displayTransformation = display.getTransformation();
        displayTransformation.getLeftRotation().set(attackAngle);
        Vector3f rotatedOffset = attackAngle.transformUnit(new Vector3f((float) offset.getX(), (float) offset.getY(), (float) offset.getZ()));

        displayTransformation.getTranslation().set(rotatedOffset.x, rotatedOffset.y, rotatedOffset.z);
        displayTransformation.getScale().set(6,6,6);
        display.setTransformation(displayTransformation);

        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.THIRDPERSON_LEFTHAND);

        ItemStack itemStack = new ItemStack(itemType);
        display.setItemStack(itemStack);

        return display;
    }

    public Quaternionf getAttackAngle(){
        return attackAngle;
    }
    public void setAttackAngle(Quaternionf attackAngle){
        this.attackAngle = attackAngle;
    }
    public Vector getVelocity(){
        return velocity;
    }
    public Vector getAngularVelocity(){
        return angularVelocity;
    }
    public double getRotorYRotation(){
        return rotorYRotation;
    }
    public void setRotorYRotation(double rotorYRotation){
        this.rotorYRotation = rotorYRotation;
    }
    public Entity[] getEntitiesBase(){
        return entitiesBase;
    }
    public float getRPM(){
        return rpm;
    }
    public void setRPM(float rpm){
        this.rpm = rpm;
    }
    public void setIsGrounded(boolean statement){
        isGrounded = statement;
    }
    public boolean getIsGrounded(){
        return isGrounded;
    }
    public int getCollective(){
        return collective;
    }
    public void setCollective(int newCollective){
        collective = newCollective;
    }
    public Vector2f getPlayerRotation(){
        return playerRotation;
    }
    public CollisionBox[] getCollisionBoxes(){
        return collisionBoxes;
    }
    public static HashMap<Integer, ActiveHelicopter> getActiveHelicopters(){
        return activeHelicopters;
    }
}
