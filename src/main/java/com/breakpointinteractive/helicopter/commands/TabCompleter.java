package com.breakpointinteractive.helicopter.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TabCompleter implements org.bukkit.command.TabCompleter {
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if(sender instanceof Player player){
            if(command.getName().equalsIgnoreCase("helicopter")){
                if(args.length == 1){
                    return List.of("list", "summon", "remove");
                }else if(args.length == 2 && (args[0].equals("give") || args[0].equals("summon"))){
                    return List.of("default");
                }else if(args.length == 2 && (args[0].equals("remove"))){
                    ArrayList<String> list = new ArrayList<>();
                    for(Entity entity : player.getWorld().getEntities()){
                        if(Objects.equals(entity.customName(), Component.text("helicopter"))){
                            list.add(String.valueOf(entity.getEntityId()));
                        }
                    }
                    return list;
                }
            }
        }
        return null;
    }
}
