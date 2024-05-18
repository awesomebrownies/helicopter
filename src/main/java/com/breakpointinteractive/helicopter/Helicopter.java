package com.breakpointinteractive.helicopter;

import com.breakpointinteractive.helicopter.commands.HelicopterCommand;
import com.breakpointinteractive.helicopter.commands.TabCompleter;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;

public final class Helicopter extends JavaPlugin {

    public static Helicopter getInstance() {
        return getPlugin(Helicopter.class);
    }
    @Override
    public void onEnable() {
        List.of(
                new InputListener(),
                new CameraHandler()).forEach(e -> getServer().getPluginManager().registerEvents(e, this));
        List.of(
                new InputListener()).forEach(e -> PacketEvents.getAPI().getEventManager().registerListener(e, PacketListenerPriority.LOW));
        PacketEvents.getAPI().init();

        TabCompleter tabCompleter = new TabCompleter();

        Objects.requireNonNull(this.getCommand("helicopter")).setExecutor(new HelicopterCommand());
        Objects.requireNonNull(this.getCommand("helicopter")).setTabCompleter(tabCompleter);
    }
}