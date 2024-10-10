package com.gladurbad.medusa.check.impl.combat.aimassist;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

@CheckInfo(name = "Aim (P)", description = "Checks for high accuracy", complextype = "Accuracy")
public class AimP extends Check {

    private static final double ANGLE_THRESHOLD = 6.0;
    private static final double ACCURACY_THRESHOLD = 0.4;
    private static final int SAMPLE_SIZE = 20;

    private double buffer = 0.0;
    private int sampleCount = 0;
    private double accurateAims = 0;

    public AimP(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        if (packet.isRotation() && data.getCombatProcessor().isInCombat()) {
            Entity target = data.getCombatProcessor().getTarget();
            if (target == null) return;

            Vector playerLook = data.getPlayer().getLocation().getDirection();
            Vector toTarget = target.getLocation().toVector().subtract(data.getPlayer().getLocation().toVector()).normalize();

            double angle = playerLook.angle(toTarget);
            double angleDegrees = Math.toDegrees(angle);

            double deltaYaw = Math.abs(data.getRotationProcessor().getDeltaYaw());
            double deltaPitch = Math.abs(data.getRotationProcessor().getDeltaPitch());

            if (deltaYaw > 1.0 || deltaPitch > 1.0) {
                if (angleDegrees < ANGLE_THRESHOLD) {
                    accurateAims++;
                }
                sampleCount++;

                if (sampleCount >= SAMPLE_SIZE) {
                    double accuracy = accurateAims / sampleCount;

                    if (accuracy > ACCURACY_THRESHOLD) {
                        buffer += 1.0;

                        if (buffer > 4.0) {
                            fail(String.format("Too high accuracy: %.2f", accuracy));
                            accurateAims = 0;
                            buffer = 0;
                        }
                    } else {
                        buffer = Math.max(0, buffer - 0.05);
                        accurateAims = Math.max(0, accurateAims - 0.9);
                    }

                    debug(String.format("Accuracy: %.2f, Buffer: %.2f", accuracy, buffer));

                    sampleCount = 0;
                }
            }
        }
    }
}