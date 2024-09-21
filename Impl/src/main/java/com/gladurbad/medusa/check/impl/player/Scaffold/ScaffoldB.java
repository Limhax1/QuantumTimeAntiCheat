package com.gladurbad.medusa.check.impl.player.Scaffold;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Set;

@CheckInfo(name = "Scaffold (B)", description = "Detects suspicious block placement patterns based on player movement and rotation.")
public class ScaffoldB extends Check {

    private final Deque<Long> placeTimes = new LinkedList<>();
    private final int SAMPLE_SIZE = 10;
    private final double MAX_PLACE_SPEED = 22.0; // blocks per second
    private final double MIN_PITCH = 45.0; // degrees
    private final double MAX_REACH = 5.0; // blocks

    private Location lastLocation;
    private long lastPlaceTime;
    private int placedBlocks;
    private int airTicks;

    public ScaffoldB(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        Player player = data.getPlayer();

        if (packet.isBlockPlace()) {
            long currentTime = System.currentTimeMillis();
            
            // Check placement speed
            if (lastPlaceTime != 0) {
                long timeDiff = currentTime - lastPlaceTime;
                placeTimes.addLast(timeDiff);

                if (placeTimes.size() > SAMPLE_SIZE) {
                    placeTimes.removeFirst();
                }

                double averagePlaceSpeed = 1000.0 * placeTimes.size() / placeTimes.stream().mapToLong(Long::longValue).sum();

                debug("Place Speed: " + String.format("%.2f", averagePlaceSpeed) + " blocks/s, Buffer: " + String.format("%.2f", buffer));

                if (averagePlaceSpeed > MAX_PLACE_SPEED) {
                    buffer += 1;
                    if (buffer > 5) {
                        fail("Suspicious block placement speed. Speed: " + String.format("%.2f", averagePlaceSpeed));
                    }
                } else {
                    buffer = Math.max(0, buffer - 0.5);
                }
            }

            // Check player pitch
            float pitch = player.getLocation().getPitch();
            debug("Player Pitch: " + String.format("%.2f", pitch));

            if (pitch < MIN_PITCH && !isExempt(ExemptType.TELEPORT)) {
                buffer += 0.5;
                if (buffer > 5) {
                    fail("Suspicious player pitch while scaffolding. Pitch: " + String.format("%.2f", pitch));
                }
            }

            // Check reach distance
            Block placedBlock = getTargetBlock(player, 5);
            if (placedBlock != null && placedBlock.getType() != Material.AIR) {
                double distance = player.getLocation().distance(placedBlock.getLocation());
                debug("Place Distance: " + String.format("%.2f", distance));

                if (distance > MAX_REACH) {
                    buffer += 1;
                    if (buffer > 5) {
                        fail("Suspicious reach distance while scaffolding. Distance: " + String.format("%.2f", distance));
                    }
                }
            }

            lastPlaceTime = currentTime;
            placedBlocks++;
            airTicks = 0;
        } else if (packet.isFlying()) {
            // Check player movement
            Location currentLocation = player.getLocation();
            if (lastLocation != null) {
                Vector movement = currentLocation.toVector().subtract(lastLocation.toVector());
                Block blockBelow = currentLocation.getBlock().getRelative(0, -1, 0);

                if (blockBelow.getType() == Material.AIR) {
                    airTicks++;
                } else {
                    airTicks = 0;
                }

                debug("Air Ticks: " + airTicks + ", Movement: " + String.format("%.2f", movement.length()));

            }
            lastLocation = currentLocation;

            // Reset placed blocks counter
            placedBlocks = 0;
        }
    }

    private Block getTargetBlock(Player player, int maxDistance) {
        Location location = player.getEyeLocation();
        Vector direction = location.getDirection().normalize();
        
        for (int i = 0; i <= maxDistance; i++) {
            Block block = location.add(direction).getBlock();
            if (block.getType() != Material.AIR) {
                return block;
            }
        }
        
        return null;
    }
}