package com.breakpointinteractive.helicopter;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerVehicle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Objects;

public class InputListener implements Listener, PacketListener {
    @EventHandler
    public void onDismount(EntityDismountEvent event){
        if(event.getEntity() instanceof Player player && Objects.equals(event.getDismounted().customName(), Component.text("helicopter"))){
            player.removePotionEffect(PotionEffectType.HASTE);
        }

        Entity vehicle = event.getDismounted();
        if(vehicle.getVehicle() != null && vehicle.getVehicle().getCustomName() != null && vehicle.getVehicle().getCustomName().contains("seat")){
            vehicle.getVehicle().remove();
            vehicle.remove();
            int i = Integer.parseInt(vehicle.getVehicle().getCustomName().substring(vehicle.getVehicle().getCustomName().indexOf("-")+1, vehicle.getVehicle().getCustomName().indexOf(":")));

            try{
                int entityId = Integer.parseInt(vehicle.getVehicle().getCustomName().substring(0, vehicle.getVehicle().getCustomName().indexOf("-")));
                ActiveHelicopter helicopter = ActiveHelicopter.getActiveHelicopters().get(entityId);
                helicopter.getSeats()[i] = null;
            }catch( Exception ignored){}
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractAtEntityEvent event){
        if(event.getRightClicked().getVehicle() != null
                && Objects.equals(event.getRightClicked().getVehicle().customName(), Component.text("helicopter"))
                && event.getRightClicked().getVehicle().getPassengers().size() != 4){
            Player player = event.getPlayer();

            ItemDisplay helicopter = (ItemDisplay) event.getRightClicked().getVehicle();

            helicopter.addPassenger(player);

            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, PotionEffect.INFINITE_DURATION, 9, false, false, false));



            Vector3f eulerAngles = new Vector3f();
            helicopter.getTransformation().getLeftRotation().getEulerAnglesYXZ(eulerAngles);
            WrapperPlayServerPlayerPositionAndLook positionAndLook =
                    new WrapperPlayServerPlayerPositionAndLook(0,0,0,
                            (float) Math.toDegrees(-eulerAngles.y+eulerAngles.z),(float) Math.toDegrees(eulerAngles.x),(byte) 0b00111, 0, false);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, positionAndLook);

            if(!ActiveHelicopter.getActiveHelicopters().containsKey(event.getRightClicked().getVehicle().getEntityId())){
                ActiveHelicopter activeHelicopter = new ActiveHelicopter(helicopter);
                Physics.simulateHelicopter(activeHelicopter);
                for(Entity target : event.getPlayer().getWorld().getEntities()){
                    if(target.getCustomName() != null && target.getCustomName().contains("seat")){
                        try{
                            int entityId = Integer.parseInt(target.getCustomName().substring(0, target.getCustomName().indexOf("-")));
                            if(entityId == helicopter.getEntityId()){
                                int i = Integer.parseInt(target.getCustomName().substring(target.getCustomName().indexOf("-")+1, target.getCustomName().indexOf(":")));
                                activeHelicopter.getSeats()[i] = target;
                            }
                        }catch(Exception ignored){}
                    }
                }
            }
        }else if(event.getPlayer().getVehicle() != null && event.getPlayer().getVehicle().getCustomName() != null
                && event.getPlayer().getVehicle().getCustomName().equals("helicopter")){
            Entity vehicle = event.getRightClicked().getVehicle();
            if(vehicle != null && vehicle.getCustomName() != null){
                if(vehicle.getCustomName().equals("helicopter")){
                    ActiveHelicopter.getActiveHelicopters().get(event.getRightClicked().getVehicle().getEntityId()).setLastRightClick(System.currentTimeMillis());
                }else{
                    try{
                        int entityId = Integer.parseInt(vehicle.getCustomName().substring(0, vehicle.getCustomName().indexOf("-")));
                        if(entityId == event.getPlayer().getVehicle().getEntityId()){
                            ActiveHelicopter.getActiveHelicopters().get(entityId).setLastRightClick(System.currentTimeMillis());
                        }
                    }catch(Exception ignored){}
                }

            }
        }else if (event.getRightClicked().getVehicle() != null
                && Objects.equals(event.getRightClicked().getVehicle().customName(), Component.text("helicopter"))){
            //summon seats here
            ActiveHelicopter helicopter = ActiveHelicopter.getActiveHelicopters().get(event.getRightClicked().getVehicle().getEntityId());
            Vector3f offset = new Vector3f();

            for(int i = 0; i < 5; i++){
                if(helicopter.getSeats()[i] == null){
                    if (i % 2 == 1) {
                        offset.x = 0;
                    }else{
                        offset.x = -1.25f;
                    }
                    offset.z = (float) (((i+1)/2)*(-1.2));

                    Quaternionf attackAngle = ((ItemDisplay)helicopter.getEntitiesBase()[0]).getTransformation().getLeftRotation();
                    attackAngle.transformUnit(offset);


                    ItemDisplay itemDisplay = (ItemDisplay) event.getPlayer().getWorld().spawnEntity(helicopter.getEntitiesBase()[0].getLocation().add(Vector.fromJOML(offset)),
                            EntityType.ITEM_DISPLAY);
                    helicopter.getSeats()[i] = itemDisplay;

                    ArmorStand armorStand = (ArmorStand) event.getPlayer().getWorld().spawnEntity(helicopter.getEntitiesBase()[0].getLocation(), EntityType.ARMOR_STAND);
                    armorStand.setMarker(true);
                    armorStand.setInvisible(true);

                    helicopter.getSeats()[i].addPassenger(armorStand);
                    armorStand.addPassenger(event.getPlayer());
                    helicopter.getSeats()[i].customName(Component.text(helicopter.getEntitiesBase()[0].getEntityId()+"-" + i + ":seat"));
                    break;
                }
            }
        }
    }

    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType().equals(PacketType.Play.Client.STEER_VEHICLE)) {
            Player player = (Player) event.getPlayer();
            if(player.getVehicle() != null && Objects.equals(player.getVehicle().customName(), Component.text("helicopter"))){
                WrapperPlayClientSteerVehicle packet = new WrapperPlayClientSteerVehicle(event);

                ActiveHelicopter helicopter = ActiveHelicopter.getActiveHelicopters().get(player.getVehicle().getEntityId());
                if(helicopter != null && packet.getSideways() != 0 && !helicopter.isFreeLooking()){
                    helicopter.getBodyRotation().y += (float) Math.toRadians(Math.round(packet.getSideways()));
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
