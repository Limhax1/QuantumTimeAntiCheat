package com.gladurbad.medusa.check.impl.player.Scaffold;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;

@CheckInfo(name = "Scaffold (A)", description = "Detects suspicious block placement patterns and timings.")
public class ScaffoldA extends Check {

    private final Deque<Long> placeTimes = new LinkedList<>();
    private final int SAMPLE_SIZE = 20;
    private final double MAX_ANGLE_CHANGE = 30.0;
    private final long MIN_PLACE_DELAY = 40L; // milliseconds

    private Vector lastPlaceDirection;
    private long lastPlaceTime;

    public ScaffoldA(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        if (packet.isBlockPlace()) {
            Player player = data.getPlayer();
            Block placedBlock = player.getTargetBlock((HashSet<Byte>) null, 5);

            if (placedBlock == null || placedBlock.getType() == Material.AIR) return;

            long currentTime = System.currentTimeMillis();
            Vector placeDirection = placedBlock.getLocation().toVector().subtract(player.getLocation().toVector());

            // Check placement timing
            if (lastPlaceTime != 0) {
                long timeDiff = currentTime - lastPlaceTime;
                placeTimes.addLast(timeDiff);

                if (placeTimes.size() > SAMPLE_SIZE) {
                    placeTimes.removeFirst();
                }

                if (timeDiff < MIN_PLACE_DELAY) {
                    buffer += 0.5;
                    if (buffer > 5) {
                        fail("Suspicious block placement timing. TimeDiff: " + timeDiff);
                    }
                } else {
                    buffer = Math.max(0, buffer - 0.1);
                }
            }

            // Check placement angle
            if (lastPlaceDirection != null) {
                double angle = placeDirection.angle(lastPlaceDirection);
                if (angle > Math.toRadians(MAX_ANGLE_CHANGE) && !isExempt(ExemptType.TELEPORT, ExemptType.VELOCITY)) {
                    buffer += 1;
                    if (buffer > 3) {
                        fail("Suspicious block placement angle. Angle: " + Math.toDegrees(angle));
                    }
                } else {
                    buffer = Math.max(0, buffer - 0.5);
                }
            }

            // Check consistency of placement timings
            if (placeTimes.size() == SAMPLE_SIZE) {
                double average = placeTimes.stream().mapToLong(Long::longValue).average().orElse(0);
                double variance = placeTimes.stream().mapToDouble(time -> Math.pow(time - average, 2)).average().orElse(0);
                double stdDev = Math.sqrt(variance);

                if (stdDev < 10) {
                    buffer += 1;
                    if (buffer > 5) {
                        fail("Suspiciously consistent block placement timings. StdDev: " + stdDev);
                    }
                } else {
                    buffer = Math.max(0, buffer - 0.5);
                }
            }

            // Check if player is placing blocks below them (typical scaffold behavior)
            Block blockBelow = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
            if (blockBelow.getType() == Material.AIR && placedBlock.equals(blockBelow)) {
                buffer += 1;
                if (buffer > 3) {
                    fail("Suspicious downward block placement");
                }
            }

            lastPlaceDirection = placeDirection;
            lastPlaceTime = currentTime;
        }
    }
}