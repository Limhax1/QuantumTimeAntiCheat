package com.gladurbad.medusa.check.impl.combat.aimassist;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;

@CheckInfo(name = "AimAssist (F)", description = "Detects suspicious aiming behavior based on GCD errors and aim patterns.")
public class AimAssistF extends Check {

    private static final int SAMPLE_SIZE = 20;
    private static final double GCD_THRESHOLD = 0.001;
    private static final double PATTERN_THRESHOLD = 0.995;
    private static final double BUFFER_LIMIT = 10.0;

    private final Deque<Float> yawChanges = new ArrayDeque<>();
    private final Deque<Float> pitchChanges = new ArrayDeque<>();
    private double buffer = 0.0;
    private float lastYaw = 0.0f;
    private float lastPitch = 0.0f;

    public AimAssistF(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        if (packet.isRotation()) {
            Player player = data.getPlayer();

            float yaw = data.getRotationProcessor().getYaw();
            float pitch = data.getRotationProcessor().getPitch();

            float deltaYaw = Math.abs(yaw - lastYaw);
            float deltaPitch = Math.abs(pitch - lastPitch);

            // Normalize yaw
            if (deltaYaw > 180.0f) {
                deltaYaw = 360.0f - deltaYaw;
            }

            yawChanges.addLast(deltaYaw);
            pitchChanges.addLast(deltaPitch);

            if (yawChanges.size() > SAMPLE_SIZE) {
                yawChanges.removeFirst();
                pitchChanges.removeFirst();

                // Check for GCD errors
                double gcdYaw = getGcd(yawChanges);
                double gcdPitch = getGcd(pitchChanges);

                if (gcdYaw < GCD_THRESHOLD || gcdPitch < GCD_THRESHOLD) {
                    buffer += 1.0;
                }

                // Check for aim patterns
                double yawSimilarity = calculateSimilarity(yawChanges);
                double pitchSimilarity = calculateSimilarity(pitchChanges);

                if (yawSimilarity > PATTERN_THRESHOLD || pitchSimilarity > PATTERN_THRESHOLD) {
                    buffer += 0.5;
                }

                // Trigger fail if buffer exceeds limit
                if (buffer > BUFFER_LIMIT) {
                    fail(String.format("GCD Flaw. [GCD Yaw=%.5f, GCD Pitch=%.5f, Yaw Similarity=%.3f, Pitch Similarity=%.3f]",
                            gcdYaw, gcdPitch, yawSimilarity, pitchSimilarity));
                    buffer = BUFFER_LIMIT / 2;
                } else {
                    buffer = Math.max(0, buffer - 0.25);
                }

                debug(String.format("GCD Yaw=%.5f, GCD Pitch=%.5f, Yaw Similarity=%.3f, Pitch Similarity=%.3f, Buffer=%.2f",
                        gcdYaw, gcdPitch, yawSimilarity, pitchSimilarity, buffer));
            }

            lastYaw = yaw;
            lastPitch = pitch;
        }
    }

    private double getGcd(Deque<Float> values) {
        double result = values.getFirst();
        for (double value : values) {
            result = gcd(result, value);
            if (result < GCD_THRESHOLD) return result;
        }
        return result;
    }

    private double gcd(double a, double b) {
        if (Math.abs(b) < GCD_THRESHOLD) return Math.abs(a);
        return gcd(b, a % b);
    }

    private double calculateSimilarity(Deque<Float> values) {
        double sum = 0;
        double squareSum = 0;
        int n = values.size();

        for (double value : values) {
            sum += value;
            squareSum += value * value;
        }

        double mean = sum / n;
        double variance = (squareSum - (sum * sum) / n) / (n - 1);
        double stdDev = Math.sqrt(variance);

        return 1 - (stdDev / mean);
    }
}