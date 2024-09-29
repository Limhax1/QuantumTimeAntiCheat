package com.gladurbad.medusa.check.impl.combat.aimassist;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayDeque;
import java.util.Deque;

@CheckInfo(name = "Aim (E)", description = "Checks for Rotation speed flaws." , experimental = true, complextype = "Speed")
public class AimE extends Check {

    private static final int SAMPLE_SIZE = 70;
    private static final double SPEED_DISTANCE_RATIO_THRESHOLD = 1.495;
    private static final double BUFFER_LIMIT = 30;
    private static final double MIN_ROTATION_THRESHOLD = 3;
    private static final double MAX_DISTANCE = 8.0;
    private static final double YAW_WEIGHT = 1;
    private static final double PITCH_WEIGHT = 0.25;

    private final Deque<Double> rotationSpeeds = new ArrayDeque<>();
    private final Deque<Double> targetDistances = new ArrayDeque<>();
    private double lastYaw;
    private double lastPitch;
    private double buffer;
    private int consistentPatternCount;

    public AimE(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        if (packet.isRotation() && data.getCombatProcessor().isInCombat()) {
            Entity target = data.getCombatProcessor().getTarget();
            if (target == null) return;

            double yaw = data.getRotationProcessor().getYaw();
            double pitch = data.getRotationProcessor().getPitch();
            
            double deltaYaw = Math.abs(yaw - lastYaw);
            double deltaPitch = Math.abs(pitch - lastPitch);

            // Normalize yaw
            if (deltaYaw > 180.0) {
                deltaYaw = 360.0 - deltaYaw;
            }

            // Weighted rotation speed calculation
            double rotationSpeed = Math.sqrt(YAW_WEIGHT * deltaYaw * deltaYaw + PITCH_WEIGHT * deltaPitch * deltaPitch);
            double targetDistance = data.getPlayer().getLocation().distance(target.getLocation());

            // Apply speed potion correction
            double speedCorrection = getSpeedPotionCorrection(data.getPlayer());
            targetDistance *= speedCorrection;

            if (rotationSpeed > MIN_ROTATION_THRESHOLD && targetDistance <= MAX_DISTANCE) {
                rotationSpeeds.addLast(rotationSpeed);
                targetDistances.addLast(targetDistance);

                if (rotationSpeeds.size() > SAMPLE_SIZE) {
                    rotationSpeeds.removeFirst();
                    targetDistances.removeFirst();

                    double speedDistanceRatio = calculateSpeedDistanceRatio();
                    boolean patternDetected = detectConsistentPattern();

                    boolean exempt = isExempt(ExemptType.TELEPORT, ExemptType.INSIDE_VEHICLE, ExemptType.JOINED);

                    if (!exempt && speedDistanceRatio > SPEED_DISTANCE_RATIO_THRESHOLD && patternDetected) {
                        buffer += 0.5;

                        if (buffer > BUFFER_LIMIT) {
                            fail(String.format("PatternE. Ratio: %.2f, ConsistentPatterns: %d", speedDistanceRatio, consistentPatternCount));
                            buffer = 0;
                            rotationSpeeds.clear();
                            targetDistances.clear();
                            consistentPatternCount = 0;
                        }
                    } else {
                        buffer = Math.max(0, buffer - 1);
                    }

                    debug(String.format("Speed-Distance Ratio: %.2f, ConsistentPatterns: %d, Buffer: %.2f, SpeedCorrection: %.2f", 
                                        speedDistanceRatio, consistentPatternCount, buffer, speedCorrection));
                }
            }

            lastYaw = yaw;
            lastPitch = pitch;
        }
    }

    private double calculateSpeedDistanceRatio() {
        double avgSpeed = rotationSpeeds.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double avgDistance = targetDistances.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        
        if (avgDistance == 0) return 0;
        
        return avgSpeed / avgDistance;
    }

    private boolean detectConsistentPattern() {
        int patternCount = 0;
        double lastRatio = -1;
        
        for (int i = 0; i < rotationSpeeds.size(); i++) {
            double speed = (Double) rotationSpeeds.toArray()[i];
            double distance = (Double) targetDistances.toArray()[i];
            double ratio = speed / distance;
            
            if (lastRatio != -1) {
                if (Math.abs(ratio - lastRatio) < 0.25) {
                    patternCount++;
                } else {
                    patternCount = Math.max(0, patternCount - 1);
                }
            }
            
            lastRatio = ratio;
            
            if (patternCount >= 5) {
                consistentPatternCount++;
                return true;
            }
        }
        
        consistentPatternCount = Math.max(0, consistentPatternCount - 1);
        return consistentPatternCount > 1;
    }

    private double getSpeedPotionCorrection(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(PotionEffectType.SPEED)) {
                int speedAmplifier = effect.getAmplifier() + 1;
                return 1.0 + (0.2 * speedAmplifier);
            }
        }
        return 1.0;
    }
}
