package com.gladurbad.medusa.check.impl.combat.aimassist;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.Deque;

@CheckInfo(name = "AimAssist (G)", description = "Checks for aimbots with high randomisation.", experimental = true)
public class AimAssistG extends Check {

    private static final int SAMPLE_SIZE = 50;
    private static final double ANGLE_THRESHOLD = 0.22;
    private static final double CONSISTENCY_THRESHOLD = 1.75;
    private static final double SNAP_THRESHOLD = 25.0;
    private static final double BUFFER_LIMIT = 100;

    private final Deque<Double> yawChanges = new ArrayDeque<>();
    private final Deque<Double> pitchChanges = new ArrayDeque<>();
    private final Deque<Double> angleChanges = new ArrayDeque<>();
    private double lastYaw, lastPitch;
    private double buffer = 0.0;

    public AimAssistG(PlayerData data) {
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

            Vector playerLook = data.getPlayer().getLocation().getDirection();
            Vector toTarget = target.getLocation().toVector().subtract(data.getPlayer().getLocation().toVector()).normalize();
            double angle = playerLook.angle(toTarget);
            angleChanges.addLast(angle);

            if (yawChanges.size() > SAMPLE_SIZE) {
                yawChanges.removeFirst();
                pitchChanges.removeFirst();
                angleChanges.removeFirst();

                double consistencyScore = calculateConsistencyScore();
                double snapScore = calculateSnapScore();
                double angleVariation = calculateAngleVariation();

                boolean exempt = isExempt(ExemptType.TELEPORT, ExemptType.INSIDE_VEHICLE, ExemptType.JOINED, ExemptType.VELOCITY, ExemptType.FLYING);

                if (!exempt) {
                    int flags = 0;
                    if (consistencyScore > CONSISTENCY_THRESHOLD) flags++;
                    if (snapScore > SNAP_THRESHOLD) flags++;
                    if (angleVariation < ANGLE_THRESHOLD) flags++;

                    if (flags >= 2) {
                        buffer += 1;

                        if (buffer > BUFFER_LIMIT) {
                            fail(String.format("Suspicious aiming pattern detected. CS: %.2f, SS: %.2f, AV: %.2f", 
                                               consistencyScore, snapScore, angleVariation));
                            buffer = 0;
                        }
                    } else {
                        buffer = Math.max(0, buffer - 1);
                    }
                }

                debug(String.format("CS: %.2f, SS: %.2f, AV: %.2f, Buffer: %.2f", 
                                    consistencyScore, snapScore, angleVariation, buffer));
            }

            lastYaw = yaw;
            lastPitch = pitch;
        }
    }

    private double calculateConsistencyScore() {
        double yawMean = yawChanges.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double pitchMean = pitchChanges.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double yawVariance = yawChanges.stream().mapToDouble(y -> Math.pow(y - yawMean, 2)).average().orElse(0.0);
        double pitchVariance = pitchChanges.stream().mapToDouble(p -> Math.pow(p - pitchMean, 2)).average().orElse(0.0);

        return 1.0 / (1.0 + Math.sqrt(yawVariance + pitchVariance));
    }

    private double calculateSnapScore() {
        return yawChanges.stream().mapToDouble(Double::doubleValue).max().orElse(0.0) +
               pitchChanges.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    }

    private double calculateAngleVariation() {
        double mean = angleChanges.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return Math.sqrt(angleChanges.stream().mapToDouble(a -> Math.pow(a - mean, 2)).average().orElse(0.0));
    }
}
