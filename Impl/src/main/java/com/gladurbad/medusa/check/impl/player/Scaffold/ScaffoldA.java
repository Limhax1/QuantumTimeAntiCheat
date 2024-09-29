package com.gladurbad.medusa.check.impl.player.Scaffold;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;
import com.gladurbad.medusa.util.raytrace.RayTrace;
import com.gladurbad.medusa.util.raytrace.RayTraceResult;
import io.github.retrooper.packetevents.packetwrappers.play.in.blockplace.WrappedPacketInBlockPlace;
import io.github.retrooper.packetevents.utils.vector.Vector3i;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Deque;
import java.util.LinkedList;

@CheckInfo(name = "Scaffold (A)", description = "Checks for Invalid yaw/pitch when bridging.", complextype = "RayCast")
public class ScaffoldA extends Check {

    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");
    private static final ConfigValue max_buffer = new ConfigValue(ConfigValue.ValueType.DOUBLE, "max_buffer");
    private static final ConfigValue buffer_decay = new ConfigValue(ConfigValue.ValueType.DOUBLE, "buffer_decay");

    public boolean placedBlock;
    private long lastSwingTime;
    private static final long SWING_TIMEOUT = 500;
    private double BUFFER;
    private double MAX_BUFFER = 15;
    private static final double MAX_PLACE_DISTANCE = 4.5;
    private Location lastPlacedBlockLocation;

    public ScaffoldA(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(final Packet packet) {
        if (packet.isFlying()) {
            placedBlock = false;
            lastPlacedBlockLocation = null;
        } else if (packet.isBlockPlace()) {
            WrappedPacketInBlockPlace wrappedPacket = new WrappedPacketInBlockPlace(packet.getRawPacket());
            Vector3i blockPosition = wrappedPacket.getBlockPosition();
            Block placedBlock = data.getPositionProcessor().getBlockat(data.getPlayer().getWorld(), blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());
            Player player = data.getPlayer();

            if (player.getItemInHand().getType().isBlock()) {
                Material handMaterial = player.getItemInHand().getType();
                Material blockMaterial = placedBlock.getType();

                boolean recentSwing = (System.currentTimeMillis() - lastSwingTime) <= SWING_TIMEOUT;
                if (blockMaterial == handMaterial && recentSwing) {
                    this.placedBlock = true;
                    lastPlacedBlockLocation = placedBlock.getLocation();

                    Location eyeLocation = player.getEyeLocation();
                    Vector direction = eyeLocation.getDirection();
                    RayTrace rayTrace = new RayTrace(player, eyeLocation, direction, MAX_PLACE_DISTANCE, 0.01);
                    RayTraceResult result = rayTrace.trace();

                    if (result.getHitType() == RayTraceResult.HitType.BLOCK &&
                        result.getHitLocation().getBlock().equals(placedBlock)) {
                        debug("Block placed legitimately at: " + lastPlacedBlockLocation + ", Material: " + blockMaterial);
                    } else {
                        if(BUFFER++ > max_buffer.getDouble()) {
                            fail("RayTrace fail (distance: " +
                                    String.format("%.2f", result.getDistance()) + ")");
                            if (setback.getBoolean()) {
                                player.teleport(player.getLocation().subtract(0, 1, 0));
                            }
                        } else {
                            BUFFER = Math.max(0, BUFFER - buffer_decay.getDouble());
                        }
                    }
                } else {
                    //debug("Block interaction detected, but not placed or no recent swing. Hand: " + handMaterial +
                          //", Block: " + blockMaterial + ", Recent swing: " + recentSwing);
                }
            }
        } else if (packet.isArmAnimation()) {
            lastSwingTime = System.currentTimeMillis();
            //debug("Swing detected at: " + lastSwingTime);
        }
    }
}
