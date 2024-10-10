package com.gladurbad.medusa.check.impl.combat.aimassist;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;

@CheckInfo(name = "Aim (D)", description = "Checks for consistent aiming.", experimental = true, complextype = "Consistent")
public class AimD extends Check {

    private static final int SAMPLE_SIZE = 20;
    private static final double MAX_SNAP_ANGLE = 30.0;
    private static final double MIN_SNAP_ANGLE = 3.0;
    private static final double SNAP_THRESHOLD = 0.90;
    private static final double CONSISTENT_MOVE_THRESHOLD = 0.01;

    private final Deque<Float> yawChanges = new ArrayDeque<>();
    private final Deque<Float> pitchChanges = new ArrayDeque<>();
    private float lastYaw = 0.0f;
    private float lastPitch = 0.0f;
    private double buffer = 0.0;

    public AimD(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        if (packet.isRotation() && data.getCombatProcessor().isInCombat() && data.getPositionProcessor().getDeltaXZ() != 0) {
            float yaw = data.getRotationProcessor().getYaw();
            float pitch = data.getRotationProcessor().getPitch();

            Entity target = data.getCombatProcessor().getTarget();

            float deltaYaw = Math.abs(yaw - lastYaw);
            float deltaPitch = Math.abs(pitch - lastPitch);

            if (deltaYaw > 180.0f) {
                deltaYaw = 360.0f - deltaYaw;
            }

            yawChanges.addLast(deltaYaw);
            pitchChanges.addLast(deltaPitch);

            if (yawChanges.size() > SAMPLE_SIZE) {
                yawChanges.removeFirst();
                pitchChanges.removeFirst();

                boolean exempt = isExempt(ExemptType.TELEPORT, ExemptType.INSIDE_VEHICLE);
                //boolean validenemy = target.getLocation().distance(target.getLocation()) != lastloc;
                // TODO fix falses when target standing still for all aim checks

                if (!exempt) {
                    double snapPercentage = calculateSnapPercentage();
                    boolean consistentSmallMoves = checkConsistentSmallMoves();
                    boolean linearMovement = checkLinearMovement();

                    boolean suspicious = snapPercentage > SNAP_THRESHOLD || (consistentSmallMoves && linearMovement);

                    if (suspicious) {
                        buffer += 1.0;
                        if (buffer > 24) {
                            fail(String.format("SnapPercentage=%.2f, ConsistentSmallMoves=%s, LinearMovement=%s", snapPercentage, consistentSmallMoves, linearMovement));
                            snapPercentage = 0;
                            consistentSmallMoves = false;
                            linearMovement = false;
                            buffer = 0;
                        }
                    } else {
                        buffer = Math.max(0, buffer - 0.5);
                    }

                    debug(String.format("SnapPercentage=%.2f, ConsistentSmallMoves=%s, LinearMovement=%s, Buffer=%.2f",
                            snapPercentage, consistentSmallMoves, linearMovement, buffer));
                }
            }

            lastYaw = yaw;
            lastPitch = pitch;
        }
    }

    private double calculateSnapPercentage() {
        int snaps = 0;
        for (int i = 0; i < yawChanges.size(); i++) {
            float yawChange = (Float) yawChanges.toArray()[i];
            float pitchChange = (Float) pitchChanges.toArray()[i];
            double totalChange = Math.sqrt(yawChange * yawChange + pitchChange * pitchChange);
            if (totalChange > MIN_SNAP_ANGLE && totalChange < MAX_SNAP_ANGLE) {
                snaps++;
            }
        }
        return (double) snaps / SAMPLE_SIZE;
    }

    private boolean checkConsistentSmallMoves() {
        int consistentMoves = 0;
        for (float change : yawChanges) {
            if (change > 0 && change < CONSISTENT_MOVE_THRESHOLD) {
                consistentMoves++;
            }
        }
        for (float change : pitchChanges) {
            if (change > 0 && change < CONSISTENT_MOVE_THRESHOLD) {
                consistentMoves++;
            }
        }
        return consistentMoves > SAMPLE_SIZE * 1.5;
    }

    private boolean checkLinearMovement() {
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int n = yawChanges.size();

        Float[] yawArray = yawChanges.toArray(new Float[0]);
        Float[] pitchArray = pitchChanges.toArray(new Float[0]);

        for (int i = 0; i < n; i++) {
            double x = yawArray[i].doubleValue();
            double y = pitchArray[i].doubleValue();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double denominator = n * sumX2 - sumX * sumX;
        if (denominator == 0) return false;

        double slope = (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;

        double rSquared = Math.pow(n * sumXY - sumX * sumY, 2) / 
                          (denominator * (n * (sumY * sumY) - sumY * sumY));

        return rSquared > 0.9; // Erős lineáris kapcsolat
    }
}
