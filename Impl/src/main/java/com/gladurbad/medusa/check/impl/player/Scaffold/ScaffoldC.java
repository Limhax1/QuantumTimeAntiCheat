package com.gladurbad.medusa.check.impl.player.Scaffold;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

import java.util.ArrayDeque;
import java.util.Deque;

@CheckInfo(name = "Scaffold (C)", description = "Detects suspicious rotation patterns during block placement.")
public class ScaffoldC extends Check {

    private static final int SAMPLE_SIZE = 15;
    private static final double GCD_THRESHOLD = 0.0001;
    private static final double SNAP_THRESHOLD = 0.3;
    private static final double BUFFER_LIMIT = 5.0;
    private static final double MIN_ROTATION = 0.1;
    private static final double MAX_ROTATION = 20.0;
    private static final long MAX_PLACE_TIME_DIFF = 100L; // milliseconds

    private final Deque<Float> yawChanges = new ArrayDeque<>();
    private final Deque<Float> pitchChanges = new ArrayDeque<>();
    private double buffer = 0.0;
    private float lastYaw = 0.0f;
    private float lastPitch = 0.0f;
    private long lastPlaceTime = 0L;
    private int consistentRotations = 0;
    private Block lastPlacedBlock = null;

    public ScaffoldC(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        if (packet.isRotation()) {
            float yaw = data.getRotationProcessor().getYaw();
            float pitch = data.getRotationProcessor().getPitch();

            float deltaYaw = Math.abs(yaw - lastYaw);
            float deltaPitch = Math.abs(pitch - lastPitch);

            // Normalize yaw
            if (deltaYaw > 180.0f) {
                deltaYaw = 360.0f - deltaYaw;
            }

            yawChanges.addLast(deltaYaw);
            pitchChanges.addLast(deltaPitch);

            if (yawChanges.size() > SAMPLE_SIZE) {
                yawChanges.removeFirst();
                pitchChanges.removeFirst();

                long timeSinceLastPlace = System.currentTimeMillis() - lastPlaceTime;
                
                if (timeSinceLastPlace < MAX_PLACE_TIME_DIFF && lastPlacedBlock != null && lastPlacedBlock.getType() != Material.AIR) {
                    double gcdYaw = getGcd(yawChanges);
                    double gcdPitch = getGcd(pitchChanges);
                    double snapPercentage = calculateSnapPercentage();
                    boolean consistentRotation = checkConsistentRotation(deltaYaw, deltaPitch);

                    boolean suspicious = (gcdYaw < GCD_THRESHOLD || gcdPitch < GCD_THRESHOLD) 
                                         || snapPercentage > SNAP_THRESHOLD 
                                         || consistentRotation;

                    if (suspicious && !isExempt(ExemptType.TELEPORT, ExemptType.INSIDE_VEHICLE)) {
                        buffer += 1.0;
                        if (buffer > BUFFER_LIMIT) {
                            fail(String.format("Suspicious rotation. GCD: %.5f, %.5f | Snap: %.2f | Consistent: %b | TimeDiff: %d", 
                                               gcdYaw, gcdPitch, snapPercentage, consistentRotation, timeSinceLastPlace));
                            buffer = BUFFER_LIMIT / 2;
                        }
                    } else {
                        buffer = Math.max(0, buffer - 0.5);
                    }

                    debug(String.format("GCD: %.5f, %.5f | Snap: %.2f | Consistent: %b | TimeDiff: %d | Buffer: %.2f", 
                                        gcdYaw, gcdPitch, snapPercentage, consistentRotation, timeSinceLastPlace, buffer));
                    
                    lastPlacedBlock = null; // Reset the last placed block after checking
                }
            }

            lastYaw = yaw;
            lastPitch = pitch;
        } else if (packet.isBlockPlace()) {
            lastPlaceTime = System.currentTimeMillis();
            lastPlacedBlock = getTargetBlock(data.getPlayer(), 5);
        }
    }

    private double getGcd(Deque<Float> values) {
        double result = values.getFirst();
        for (double value : values) {
            result = gcd(result, value);
            if (result < GCD_THRESHOLD) return result;
        }
        return result;
    }

    private double gcd(double a, double b) {
        if (Math.abs(b) < GCD_THRESHOLD) return Math.abs(a);
        return gcd(b, a % b);
    }

    private double calculateSnapPercentage() {
        int snaps = 0;
        for (int i = 0; i < yawChanges.size() - 1; i++) {
            float currentYaw = (Float) yawChanges.toArray()[i];
            float nextYaw = (Float) yawChanges.toArray()[i + 1];
            float currentPitch = (Float) pitchChanges.toArray()[i];
            float nextPitch = (Float) pitchChanges.toArray()[i + 1];

            if ((currentYaw == 0 && nextYaw > 0) || (currentPitch == 0 && nextPitch > 0)) {
                snaps++;
            }
        }
        return (double) snaps / SAMPLE_SIZE;
    }

    private boolean checkConsistentRotation(float deltaYaw, float deltaPitch) {
        if (deltaYaw > MIN_ROTATION && deltaYaw < MAX_ROTATION && 
            deltaPitch > MIN_ROTATION && deltaPitch < MAX_ROTATION) {
            consistentRotations++;
            if (consistentRotations > SAMPLE_SIZE / 2) {
                return true;
            }
        } else {
            consistentRotations = Math.max(0, consistentRotations - 1);
        }
        return false;
    }

    private Block getTargetBlock(Player player, int range) {
        BlockIterator iter = new BlockIterator(player, range);
        Block lastBlock = iter.next();
        while (iter.hasNext()) {
            lastBlock = iter.next();
            if (lastBlock.getType() == Material.AIR) {
                continue;
            }
            break;
        }
        return lastBlock;
    }
}