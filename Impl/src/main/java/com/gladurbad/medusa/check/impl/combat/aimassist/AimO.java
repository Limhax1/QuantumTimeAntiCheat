package com.gladurbad.medusa.check.impl.combat.aimassist;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.entity.Entity;

import java.util.ArrayDeque;
import java.util.Deque;

@CheckInfo(name = "Aim (O)", description = "Checks for consistent aiming patterns and sudden snaps.", complextype = "Pattern")
public class AimO extends Check {

    private static final int SAMPLE_SIZE = 25;
    private static final double ANGLE_THRESHOLD = 0.3;
    private static final double SNAP_THRESHOLD = 16;
    private static final double CONSISTENCY_THRESHOLD = 0.38;
    private static final double BUFFER_LIMIT = 10;
    private final Deque<Double> yawChanges = new ArrayDeque<>();
    private final Deque<Double> pitchChanges = new ArrayDeque<>();
    private double lastYaw = 0.0;
    private double lastPitch = 0.0;
    private double buffer = 0.0;

    public AimO(PlayerData data) {
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

            if (deltaYaw > 180.0) {
                deltaYaw = 360.0 - deltaYaw;
            }

            yawChanges.addLast(deltaYaw);
            pitchChanges.addLast(deltaPitch);

            if (yawChanges.size() > SAMPLE_SIZE) {
                yawChanges.removeFirst();
                pitchChanges.removeFirst();

                double averageYawChange = calculateAverage(yawChanges);
                double averagePitchChange = calculateAverage(pitchChanges);
                double yawConsistency = calculateConsistency(yawChanges);
                double pitchConsistency = calculateConsistency(pitchChanges);

                boolean exempt = isExempt(ExemptType.TELEPORT, ExemptType.JOINED); // who puts a velocity exemption in an aim check 

                if (!exempt) {
                    if (yawConsistency > CONSISTENCY_THRESHOLD) {
                        buffer += 1.0;

                        if (buffer > BUFFER_LIMIT) {
                            fail(String.format("PatternO. AvgYaw=%.2f, AvgPitch=%.2f, YawCons=%.2f, PitchCons=%.2f, DeltaYaw=%.2f, DeltaPitch=%.2f",
                                    averageYawChange, averagePitchChange, yawConsistency, pitchConsistency, deltaYaw, deltaPitch));
                            buffer = BUFFER_LIMIT / 2; // Részleges buffer reset
                        }
                    } else {
                        buffer = Math.max(0, buffer - 0.75);
                    }
                }

                debug("Y " + yawConsistency + " P " + pitchConsistency + " B " + buffer);
            }

            lastYaw = yaw;
            lastPitch = pitch;
        }
    }

    private double calculateAverage(Deque<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double calculateConsistency(Deque<Double> values) {
        double mean = calculateAverage(values);
        double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0.0);
        double stdDev = Math.sqrt(variance);
        return 1.0 - (stdDev / (mean + 0.1));
    }
}
