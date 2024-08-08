package com.breakpointinteractive.helicopter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.craftbukkit.entity.CraftDisplay;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.UUID;

public class ActiveHelicopter {
    public static final HashMap<Integer, ActiveHelicopter> activeHelicopters = new HashMap<>();

    private final Vector velocity = new Vector(0,0,0);
    private int collective = 0;
    private double rotorYRotation;
    private float rpm = 0;
    private Entity[] entitiesBase;
    private Vector2f playerRotation = new Vector2f(0,0);
    private Vector3f bodyRotation = new Vector3f();
    private final CollisionBox[] collisionBoxes = new CollisionBox[1];
    private boolean isGrounded;
    private boolean isFreeLooking;
    private Long lastRightClick = 0L;
    private final Entity[] seats = new Entity[5];

    public ActiveHelicopter(ItemDisplay helicopterBase){
        entitiesBase = new Entity[helicopterBase.getPassengers().size()];
        entitiesBase[0] = helicopterBase;
        for(int i = 1; i < helicopterBase.getPassengers().size(); i++){
            entitiesBase[i] = helicopterBase.getPassengers().get(i);
        }
        helicopterBase.getTransformation().getLeftRotation().getEulerAnglesYXZ(bodyRotation);
        playerRotation = new Vector2f(bodyRotation.x,bodyRotation.y);

        activeHelicopters.put(helicopterBase.getEntityId(), this);
    }

    //create instance of a helicopter and summon in necessary displays
    public ActiveHelicopter(Quaternionf attackAngle, Location location){
        attackAngle.getEulerAnglesYXZ(bodyRotation);
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
        ((CraftDisplay) body).getHandle().getEntityData().set(net.minecraft.world.entity.Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID, 1);
        activeHelicopters.put(body.getEntityId(), this);

        ItemDisplay rotor = initializePart(attackAngle, location, Material.RABBIT_FOOT, new Vector(-0.53f, 3.25f, -4f));

        TextDisplay display = initializeDisplay(attackAngle, location, new Vector(-0.5, 0.9, -1.1));

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
        display.text(Component.text("___%").color(TextColor.color(150,255,150)));
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

    public static boolean destroyParts(World world, int entityID){
        ActiveHelicopter.getActiveHelicopters().remove(entityID);

        boolean helicopterFound = false;
        for(Entity target : world.getEntities()){
            if(target.getEntityId() == entityID){
                helicopterFound = true;
                for(Entity base : target.getPassengers()){
                    if(!(base instanceof Player)){
                        base.remove();
                    }
                }
                target.eject();
                target.remove();
            }else if(target.getCustomName() != null && target.getCustomName().contains(String.valueOf(entityID))){
                target.eject();
                target.remove();
            }
        }
        return helicopterFound;
    }

    public Vector getVelocity(){
        return velocity;
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
    public void setLastRightClick(Long value){
        lastRightClick = value;
    }
    public Long getLastRightClick(){
        return lastRightClick;
    }
    public void setFreeLooking(boolean value){
        isFreeLooking = value;
    }
    public boolean isFreeLooking(){
        return isFreeLooking;
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
    public Vector3f getBodyRotation(){
        return bodyRotation;
    }
    public void setBodyRotation(Vector3f vector3f){
        bodyRotation = vector3f;
    }
    public Entity[] getSeats(){
        return seats;
    }
    public CollisionBox[] getCollisionBoxes(){
        return collisionBoxes;
    }
    public static HashMap<Integer, ActiveHelicopter> getActiveHelicopters(){
        return activeHelicopters;
    }
}
