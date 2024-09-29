package com.gladurbad.medusa.check.impl.combat.aimassist;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.entity.Entity;

import java.util.ArrayDeque;
import java.util.Deque;

@CheckInfo(name = "Aim (N)", description = "Checks for unnatural aiming patterns and micro-adjustments.", complextype = "Micro")
public class AimN extends Check {

    private static final int SAMPLE_SIZE = 40;
    private static final double MICRO_ADJUSTMENT_THRESHOLD = 0.1;
    private static final double CONSISTENCY_THRESHOLD = 0.92;
    private static final double BUFFER_LIMIT = 8.0;

    private final Deque<Double> yawChanges = new ArrayDeque<>();
    private final Deque<Double> pitchChanges = new ArrayDeque<>();
    private double lastYaw = 0.0;
    private double lastPitch = 0.0;
    private double buffer = 0.0;
    private int microAdjustments = 0;

    public AimN(PlayerData data) {
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

            if (deltaYaw > 0 && deltaYaw < MICRO_ADJUSTMENT_THRESHOLD) microAdjustments++;
            if (deltaPitch > 0 && deltaPitch < MICRO_ADJUSTMENT_THRESHOLD) microAdjustments++;

            if (yawChanges.size() > SAMPLE_SIZE) {
                yawChanges.removeFirst();
                pitchChanges.removeFirst();

                double yawConsistency = calculateConsistency(yawChanges);
                double pitchConsistency = calculateConsistency(pitchChanges);
                double overallConsistency = (yawConsistency + pitchConsistency) / 2;

                double microAdjustmentRatio = (double) microAdjustments / (SAMPLE_SIZE * 2);

                boolean exempt = isExempt(ExemptType.TELEPORT, ExemptType.VELOCITY, ExemptType.JOINED);

                if (!exempt && overallConsistency > CONSISTENCY_THRESHOLD && microAdjustmentRatio > 0.4) {
                    buffer += 1.0;

                    if (buffer > BUFFER_LIMIT) {
                        fail(String.format("Unnatural aiming. Consistency=%.2f, MicroAdj=%.2f",
                                overallConsistency, microAdjustmentRatio));
                        buffer = BUFFER_LIMIT * 0.75; // Részleges buffer reset
                    }
                } else {
                    buffer = Math.max(0, buffer - 0.75);
                }

                debug(String.format("YawCons=%.2f, PitchCons=%.2f, MicroAdj=%.2f, Buffer=%.2f",
                        yawConsistency, pitchConsistency, microAdjustmentRatio, buffer));

                microAdjustments = 0; // Reset micro-adjustments counter
            }

            lastYaw = yaw;
            lastPitch = pitch;
        }
    }

    private double calculateConsistency(Deque<Double> values) {
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0.0);
        double stdDev = Math.sqrt(variance);
        return 1.0 - (stdDev / (mean + 0.1));
    }
}
