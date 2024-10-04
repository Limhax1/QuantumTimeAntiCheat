package com.gladurbad.medusa.check.impl.combat.aimassist;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import java.util.ArrayDeque;
import java.util.Deque;

@CheckInfo(name = "Aim (C)", description = "Checks for constant rotation speeds.", experimental = true, complextype = "Constant")
public class AimC extends Check {

    private static final int SAMPLE_SIZE = 25;
    private static final double SIMILARITY_THRESHOLD = 0.02;
    private static final int CONSISTENT_ROTATIONS_THRESHOLD = 13;
    private double MinYawDiff = 2.3;
    private double MinPitchDiff = 5;
    private final Deque<Float> yawSpeeds = new ArrayDeque<>();
    private final Deque<Float> pitchSpeeds = new ArrayDeque<>();
    private float lastYaw = 0.0f;
    private float lastPitch = 0.0f;
    private int consistentRotations = 0;
    private double BUFFER;
    private static final double BUFFER_LIMIT = 3;
    private static final double BUFFER_DECAY = 0.1;



    public AimC(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        if (packet.isRotation() && data.getCombatProcessor().isInCombat() && data.getPositionProcessor().getDeltaXZ() != 0) {
            float yaw = data.getRotationProcessor().getYaw();
            float pitch = data.getRotationProcessor().getPitch();

            float deltaYaw = Math.abs(yaw - lastYaw);
            float deltaPitch = Math.abs(pitch - lastPitch);

            // Normalize yaw
            if (deltaYaw > 180.0f) {
                deltaYaw = 360.0f - deltaYaw;
            }

            yawSpeeds.addLast(deltaYaw);
            pitchSpeeds.addLast(deltaPitch);

            if (yawSpeeds.size() > SAMPLE_SIZE)
            {
                yawSpeeds.removeFirst();
                pitchSpeeds.removeFirst();

                double yawVariance = calculateVariance(yawSpeeds);
                double pitchVariance = calculateVariance(pitchSpeeds);

                boolean exempt = isExempt(ExemptType.TELEPORT, ExemptType.INSIDE_VEHICLE);
                final boolean invalid = data.getRotationProcessor().getDeltaYaw() > MinYawDiff;
                final boolean invalid2 = data.getRotationProcessor().getDeltaPitch() > MinPitchDiff;

                if (!exempt && (yawVariance < SIMILARITY_THRESHOLD || pitchVariance < SIMILARITY_THRESHOLD) && invalid || invalid2) {
                    if (++consistentRotations > CONSISTENT_ROTATIONS_THRESHOLD) {
                        BUFFER += 1.0;

                        if (BUFFER > BUFFER_LIMIT) {
                            fail(String.format("Consistent Rotations. [YawVar=%.5f, PitchVar=%.5f, Consistent=%d]",
                                    yawVariance, pitchVariance, consistentRotations));
                            BUFFER = 0;
                            consistentRotations = 0;
                            yawSpeeds.clear();
                            pitchSpeeds.clear();
                        }
                    } else {
                        BUFFER = Math.max(0, BUFFER - BUFFER_DECAY);
                    }
                } else {
                    consistentRotations = 0;
                    BUFFER = Math.max(0, BUFFER - BUFFER_DECAY * 3);
                }

                debug(String.format("YawVar=%.5f, PitchVar=%.5f, Consistent=%d, Buffer=%.2f",
                        yawVariance, pitchVariance, consistentRotations, BUFFER));
            }

            lastYaw = yaw;
            lastPitch = pitch;
        }
    }

    private double calculateVariance(Deque<Float> samples) {
        double mean = samples.stream().mapToDouble(Float::doubleValue).average().orElse(0.0);
        return samples.stream().mapToDouble(sample -> Math.pow(sample - mean, 2)).average().orElse(0.0);
    }
}