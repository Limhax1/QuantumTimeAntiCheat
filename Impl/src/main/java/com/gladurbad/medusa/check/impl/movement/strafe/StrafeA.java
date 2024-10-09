package com.gladurbad.medusa.check.impl.movement.strafe;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.util.Vector;

@CheckInfo(name = "Strafe (A)", description = "Checks for sudden direction changes while in air.", complextype = "Invalid Angle")
public class StrafeA extends Check {

    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");
    private static final ConfigValue max_buffer = new ConfigValue(ConfigValue.ValueType.DOUBLE, "max_buffer");
    private static final ConfigValue buffer_decay = new ConfigValue(ConfigValue.ValueType.DOUBLE, "buffer_decay");


    private double DIRECTION_CHANGE_THRESHOLD = 13;

    private Vector lastVelocity;
    private boolean wasInAir;
    private double buffer;

    public StrafeA(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        if (packet.isPosition()) {
            double velocityX = data.getVelocityProcessor().getVelocityX();
            double velocityY = data.getVelocityProcessor().getVelocityY();
            double velocityZ = data.getVelocityProcessor().getVelocityZ();
            Vector currentVelocity = new Vector(velocityX, velocityY, velocityZ);
            double DeltaXZ = data.getPositionProcessor().getDeltaXZ();
            double DeltaYaw = data.getRotationProcessor().getDeltaYaw();
            double lastX = data.getPositionProcessor().getLastX();
            double lastY = data.getPositionProcessor().getLastY();
            double lastZ = data.getPositionProcessor().getLastZ();
            double currentX = data.getPositionProcessor().getX();
            double currentY = data.getPositionProcessor().getY();
            double currentZ = data.getPositionProcessor().getZ();

            Vector movement = new Vector(currentX - lastX, currentY - lastY, currentZ - lastZ);

            boolean inAir = !data.getPositionProcessor().isOnGround() && !data.getPositionProcessor().isInLiquid() && !data.getPositionProcessor().isOnClimbable();
            boolean exempt = isExempt(ExemptType.TELEPORT, ExemptType.ANYVELOCITY, ExemptType.FLYING, ExemptType.ELYTRA);

            if(DeltaYaw < 30) {
                DIRECTION_CHANGE_THRESHOLD = 13;
            } else {
                DIRECTION_CHANGE_THRESHOLD = 50;
            }

            if (inAir && wasInAir && !exempt && lastVelocity != null && DeltaXZ > 0.21) {
                double angle = calculateAngle(lastVelocity, movement);

                if (angle > DIRECTION_CHANGE_THRESHOLD) {
                    buffer++;

                    if (buffer > max_buffer.getDouble()) {
                        if(setback.getBoolean()) {
                            setback();
                        }
                        fail(String.format("Angle=%.2f", angle));
                        buffer = 0;
                    }
                } else {
                    buffer = Math.max(0, buffer - buffer_decay.getDouble());
                }

                debug(String.format("Angle=%.2f, Buffer=%.1f, MaxAngle=%.2f, DY=%.2f", angle, buffer, DIRECTION_CHANGE_THRESHOLD, DeltaYaw));
            } else {
                buffer = Math.max(0, buffer - buffer_decay.getDouble());
            }

            lastVelocity = movement;
            wasInAir = inAir;
        }
    }

    private double calculateAngle(Vector v1, Vector v2) {
        double dot = v1.getX() * v2.getX() + v1.getZ() * v2.getZ();
        double v1Mag = Math.sqrt(v1.getX() * v1.getX() + v1.getZ() * v1.getZ());
        double v2Mag = Math.sqrt(v2.getX() * v2.getX() + v2.getZ() * v2.getZ());
        double cos = dot / (v1Mag * v2Mag);
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cos))));
    }
}
