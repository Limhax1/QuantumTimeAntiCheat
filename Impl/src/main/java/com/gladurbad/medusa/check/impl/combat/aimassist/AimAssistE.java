package com.gladurbad.medusa.check.impl.combat.aimassist;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import java.util.ArrayDeque;
import java.util.Deque;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@CheckInfo(name="AimAssist (E)", description="Detects aimbots that adjust rotation speed based on target distance.")
public class AimAssistE
        extends Check {
    private static final int SAMPLE_SIZE = 35;
    private static final double SPEED_DISTANCE_RATIO_THRESHOLD = 1.4;
    private static final double BUFFER_LIMIT = 10.0;
    private static final double MIN_ROTATION_THRESHOLD = 2.5;
    private static final double MAX_DISTANCE = 8.0;
    private static final double YAW_WEIGHT = 0.9;
    private static final double PITCH_WEIGHT = 0.1;
    private final Deque<Double> rotationSpeeds = new ArrayDeque<Double>();
    private final Deque<Double> targetDistances = new ArrayDeque<Double>();
    private double lastYaw;
    private double lastPitch;
    private double buffer;
    private int consistentPatternCount;

    public AimAssistE(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        if (packet.isRotation()) {
            Entity target = this.data.getCombatProcessor().getTarget();
            if (target == null) {
                return;
            }
            double yaw = this.data.getRotationProcessor().getYaw();
            double pitch = this.data.getRotationProcessor().getPitch();
            double deltaYaw = Math.abs(yaw - this.lastYaw);
            double deltaPitch = Math.abs(pitch - this.lastPitch);
            if (deltaYaw > 180.0) {
                deltaYaw = 360.0 - deltaYaw;
            }
            double rotationSpeed = Math.sqrt(0.9 * deltaYaw * deltaYaw + 0.1 * deltaPitch * deltaPitch);
            double targetDistance = this.data.getPlayer().getLocation().distance(target.getLocation());
            double speedCorrection = this.getSpeedPotionCorrection(this.data.getPlayer());
            targetDistance *= speedCorrection;
            if (rotationSpeed > 2.5 && targetDistance <= 8.0) {
                this.rotationSpeeds.addLast(rotationSpeed);
                this.targetDistances.addLast(targetDistance);
                if (this.rotationSpeeds.size() > 35) {
                    this.rotationSpeeds.removeFirst();
                    this.targetDistances.removeFirst();
                    double speedDistanceRatio = this.calculateSpeedDistanceRatio();
                    boolean patternDetected = this.detectConsistentPattern();
                    boolean exempt = this.isExempt(ExemptType.TELEPORT, ExemptType.INSIDE_VEHICLE, ExemptType.JOINED);
                    if (!exempt && speedDistanceRatio > 1.4 && patternDetected) {
                        this.buffer += 1.0;
                        if (this.buffer > 10.0) {
                            this.fail(String.format("Robotic Rotation Pattern. Ratio: %.2f, ConsistentPatterns: %d", speedDistanceRatio, this.consistentPatternCount));
                            this.consistentPatternCount = 0;
                            this.buffer = 0.0;
                            this.rotationSpeeds.clear();
                        }
                    } else {
                        this.buffer = Math.max(0.0, this.buffer - 0.75);
                    }
                    this.debug(String.format("Speed-Distance Ratio: %.2f, ConsistentPatterns: %d, Buffer: %.2f, SpeedCorrection: %.2f", speedDistanceRatio, this.consistentPatternCount, this.buffer, speedCorrection));
                }
            }
            this.lastYaw = yaw;
            this.lastPitch = pitch;
        }
    }

    private double calculateSpeedDistanceRatio() {
        double avgSpeed = this.rotationSpeeds.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double avgDistance = this.targetDistances.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        if (avgDistance == 0.0) {
            return 0.0;
        }
        return avgSpeed / avgDistance;
    }

    private boolean detectConsistentPattern() {
        int patternCount = 0;
        double lastRatio = -1.0;
        for (int i = 0; i < this.rotationSpeeds.size(); ++i) {
            double speed = (Double)this.rotationSpeeds.toArray()[i];
            double distance = (Double)this.targetDistances.toArray()[i];
            double ratio = speed / distance;
            if (lastRatio != -1.0) {
                patternCount = Math.abs(ratio - lastRatio) < 0.25 ? ++patternCount : Math.max(0, patternCount - 1);
            }
            lastRatio = ratio;
            if (patternCount < 4) continue;
            ++this.consistentPatternCount;
            return true;
        }
        this.consistentPatternCount = Math.max(0, this.consistentPatternCount - 1);
        return this.consistentPatternCount > 1;
    }

    private double getSpeedPotionCorrection(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (!effect.getType().equals(PotionEffectType.SPEED)) continue;
            int speedAmplifier = effect.getAmplifier() + 1;
            return 1.0 + 0.2 * (double)speedAmplifier;
        }
        return 1.0;
    }
}
