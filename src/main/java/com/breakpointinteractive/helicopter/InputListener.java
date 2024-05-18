package com.breakpointinteractive.helicopter;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerVehicle;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

import java.util.Objects;

public class InputListener implements Listener, PacketListener {
    @EventHandler
    public void onRightClick(PlayerInteractAtEntityEvent event){
        if(event.getRightClicked().getVehicle() != null
                && Objects.equals(event.getRightClicked().getVehicle().customName(), Component.text("helicopter"))){
            Player player = event.getPlayer();

            ItemDisplay helicopter = (ItemDisplay) event.getRightClicked().getVehicle();
            helicopter.addPassenger(player);

            Physics.simulateHelicopter(new ActiveHelicopter(helicopter));
        }
    }

    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType().equals(PacketType.Play.Client.STEER_VEHICLE)) {
            Player player = (Player) event.getPlayer();
            if(player.getVehicle() != null && Objects.equals(player.getVehicle().customName(), Component.text("helicopter"))){
                WrapperPlayClientSteerVehicle packet = new WrapperPlayClientSteerVehicle(event);

                ActiveHelicopter helicopter = ActiveHelicopter.getActiveHelicopters().get(player.getVehicle().getEntityId());
                //limit values from 0 to 100 for collective percentage
                if(helicopter != null){
                    helicopter.setCollective(Math.min(100, Math.max(0, helicopter.getCollective()+Math.round(packet.getForward()))));
                }
            }
        }
    }
}
