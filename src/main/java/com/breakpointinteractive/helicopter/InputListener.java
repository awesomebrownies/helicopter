package com.breakpointinteractive.helicopter;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerVehicle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.util.Objects;

public class InputListener implements Listener, PacketListener {
    @EventHandler
    public void onRightClick(PlayerInteractAtEntityEvent event){
        if(event.getRightClicked().getVehicle() != null
                && Objects.equals(event.getRightClicked().getVehicle().customName(), Component.text("helicopter"))
                && event.getRightClicked().getVehicle().getPassengers().size() != 4){
            Player player = event.getPlayer();

            ItemDisplay helicopter = (ItemDisplay) event.getRightClicked().getVehicle();
            helicopter.addPassenger(player);

            Vector3f eulerAngles = new Vector3f();
            helicopter.getTransformation().getLeftRotation().getEulerAnglesYXZ(eulerAngles);

            WrapperPlayServerPlayerPositionAndLook positionAndLook =
                    new WrapperPlayServerPlayerPositionAndLook(0,0,0,
                            (float) Math.toDegrees(eulerAngles.y),(float) Math.toDegrees(eulerAngles.x),(byte) 0b00111, 0, false);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, positionAndLook);

            if(!ActiveHelicopter.getActiveHelicopters().containsKey(event.getRightClicked().getEntityId())){
                Physics.simulateHelicopter(new ActiveHelicopter(helicopter));
            }
        }
    }

    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType().equals(PacketType.Play.Client.STEER_VEHICLE)) {
            Player player = (Player) event.getPlayer();
            if(player.getVehicle() != null && Objects.equals(player.getVehicle().customName(), Component.text("helicopter"))){
                WrapperPlayClientSteerVehicle packet = new WrapperPlayClientSteerVehicle(event);

                ActiveHelicopter helicopter = ActiveHelicopter.getActiveHelicopters().get(player.getVehicle().getEntityId());
                if(helicopter != null && packet.getSideways() != 0){
                    helicopter.getPlayerRotation().y += Math.round(packet.getSideways());
                    CameraHandler.getQueuedShiftAmount().put(player, CameraHandler.getQueuedShiftAmount().getOrDefault(player, new Vector(0,0,0)).add(new Vector(0,Math.round(-packet.getSideways()), 0)));
                }

                //limit values from 0 to 100 for collective percentage
                if(helicopter != null && packet.getForward() != 0){
                    TextDisplay display = (TextDisplay) helicopter.getEntitiesBase()[2];
                    updateCollective(helicopter, display, Math.min(100, Math.max(0, helicopter.getCollective()+Math.round(packet.getForward()))));

                    final int prevCollective = helicopter.getCollective();
                    new BukkitRunnable(){
                        public void run(){
                            if(prevCollective < helicopter.getCollective()){
                                updateCollective(helicopter, display, Math.min(100, helicopter.getCollective()+(helicopter.getCollective()-prevCollective)));
                            }else if(prevCollective > helicopter.getCollective()){
                                updateCollective(helicopter, display, Math.max(0, helicopter.getCollective()+(helicopter.getCollective()-prevCollective)));
                            }
                        }
                    }.runTaskLater(Helicopter.getInstance(), 2L);
                }
            }
        }
    }

    private void updateCollective(ActiveHelicopter helicopter, TextDisplay display, int collective){
        helicopter.setCollective(collective);
        String underscores = "";
        if(helicopter.getCollective() < 100){
            underscores += "_";
            if(helicopter.getCollective() < 10){
                underscores += "_";
            }
        }
        display.text(Component.text(underscores + (helicopter.getCollective() == 0 ? "_" : helicopter.getCollective()) + "%").color(TextColor.color(150,255,150)));
    }
}
