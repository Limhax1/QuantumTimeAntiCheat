package com.gladurbad.medusa.check.impl.movement.speed;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;

@CheckInfo(name = "Speed (D)", description = "Checks for abnormal vertical movements", complextype = "VerticalPrediction")
public class SpeedD extends Check {

    private static final ConfigValue max_buffer = new ConfigValue(ConfigValue.ValueType.DOUBLE, "max_buffer");
    private static final ConfigValue buffer_decay = new ConfigValue(ConfigValue.ValueType.DOUBLE, "buffer_decay");
    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");

    private static final double GRAVITY = 0.08;
    private static final double DRAG = 0.02;
    private static final double JUMP_UPWARDS_MOTION = 0.42;

    private double lastDeltaY = 0.0;
    private double buffer = 0;
    private boolean wasInAir = false;
    private int airTicks = 0;

    public SpeedD(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        if (packet.isPosition()) {
            double deltaY = data.getPositionProcessor().getDeltaY();
            boolean onGround = data.getPositionProcessor().isOnGround();
            boolean exempt = isExempt(
                ExemptType.TELEPORT, ExemptType.FLYING,
                ExemptType.LIQUID, ExemptType.SLIME, ExemptType.ANYVELOCITY,
                ExemptType.CLIMBABLE, ExemptType.BOAT, ExemptType.NEAR_VEHICLE,
                ExemptType.PISTON, ExemptType.UNDER_BLOCK, ExemptType.HIGHPING,
                ExemptType.ELYTRA, ExemptType.BUBBLE_COLUMN, ExemptType.SLOW_FALLING,
                ExemptType.LEVITATION, ExemptType.POWDER_SNOW, ExemptType.WEB,
                ExemptType.STAIRS_ABOVE, ExemptType.PLACING, ExemptType.STAIRS,
                ExemptType.STAIRS
            );



            if (!exempt) {
                if (!onGround && deltaY != 0) {
                    airTicks++;
                    double predictedDeltaY = predictNextMotionY(lastDeltaY);
                    double difference = Math.abs(deltaY - predictedDeltaY);

                    if (difference > 1e-3 && airTicks > 2) {
                        if ((buffer += difference) > max_buffer.getDouble()) {
                            fail("Abnormal vertical movement. DeltaY: " + deltaY + ", Predicted: " + predictedDeltaY + ", Diff: " + difference);
                            if (setback.getBoolean()) {
                                setback();
                            }

                            buffer = 0;
                        }
                    } else {
                        buffer = Math.max(0, buffer - buffer_decay.getDouble());
                    }

                    debug( difference + " " + buffer);
                } else {
                    airTicks = 0;
                    buffer = Math.max(0, buffer - buffer_decay.getDouble());
                }
            } else {
                buffer = Math.max(0, buffer - buffer_decay.getDouble());
            }

            lastDeltaY = deltaY;
        }
    }

    private double predictNextMotionY(double currentMotionY) {
        if (data.getPositionProcessor().isInWeb()) {
            return 0.05 * (currentMotionY - 0.05);
        }

        double predictedMotionY = (currentMotionY - GRAVITY) * 0.9800000190734863;

        if (Math.abs(predictedMotionY) < DRAG) {
            predictedMotionY = 0;
        }

        return predictedMotionY;
    }
}
