package com.breakpointinteractive.helicopter.commands;

import com.breakpointinteractive.helicopter.ActiveHelicopter;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

import java.util.HashSet;
import java.util.Objects;

public class HelicopterCommand implements CommandExecutor {
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if(sender instanceof Player player && args.length > 0){
            switch(args[0]){
                case "summon" ->{
                    if(args[1].equals("default")){
                        player.sendMessage("Spawning helicopter \"default\"");

                        Location location = new Location(player.getWorld(), player.getX(), player.getY(), player.getZ());
                        Quaternionf quaternionf = new Quaternionf();
                        double wrappedYaw = player.getYaw() < 0 ? player.getYaw() + 360 : player.getYaw();
                        quaternionf.rotateY((float) -Math.toRadians(wrappedYaw));
                        new ActiveHelicopter(quaternionf, location);
                    }
                }case "remove" ->{
                    try{
                        int entityID = Integer.parseInt(args[1]);
                        //remove active helicopter, if present
                        ActiveHelicopter.getActiveHelicopters().remove(entityID);
                        //remove from world, if loaded in
                        for(Entity target : player.getWorld().getEntities()){
                            if(target.getEntityId() == entityID && Objects.equals(target.customName(), Component.text("helicopter"))){
                                for(Entity passenger : target.getPassengers()){
                                    passenger.remove();
                                }
                                target.remove();
                                player.sendMessage("Removed helicopter with the ID " + entityID);

                                return true;
                            }
                        }
                        player.sendMessage("There is no loaded helicopter with the ID " + entityID);
                    }catch(Exception exception){
                        player.sendMessage(args[1] + " is an Invalid ID");
                    }
                }
                case "list" ->{
                    HashSet<Entity> loadedHelicopter = new HashSet<>();
                    for(Entity target : player.getWorld().getEntities()){
                        if(Objects.equals(target.customName(), Component.text("helicopter"))){
                            loadedHelicopter.add(target);
                        }
                    }
                    player.sendMessage("Physic active helicopters:");
                    for(ActiveHelicopter helicopter : ActiveHelicopter.getActiveHelicopters().values()){
                        loadedHelicopter.remove(helicopter.getEntitiesBase()[0]);

                        Entity target = helicopter.getEntitiesBase()[0];
                        player.sendMessage(getHelicopterToText(target));
                    }
                    if(ActiveHelicopter.getActiveHelicopters().isEmpty()){
                        player.sendMessage("    None");
                    }

                    player.sendMessage("Other chunk loaded helicopters:");
                    for (Entity target : loadedHelicopter) {
                        player.sendMessage(getHelicopterToText(target));
                    }
                    if(loadedHelicopter.isEmpty()){
                        player.sendMessage("    None");
                    }
                }
            }
        }

        return true;
    }

    private String getHelicopterToText(Entity target){
        return "Helicopter " + target.getEntityId() + ": \n    "
                + "X: " + (int)target.getLocation().getX() + " Y: " + (int)target.getLocation().getY() + " Z: " + (int)target.getLocation().getZ();
    }
}