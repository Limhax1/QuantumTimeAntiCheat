package com.gladurbad.medusa.check.impl.movement.motion;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.data.processor.PositionProcessor;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@CheckInfo(name = "Speed (C)", description = "Checks for invalid Y motions.", experimental = true)
public class SpeedC extends Check {

    private static final int JUMP_THRESHOLD = 2;
    private static final double EPSILON = 1E-6;

    private double lastMaxYMotion = 0.0;
    private int sameMotionCount = 0;
    private int buffer = 0;
    private boolean isAscending = false;

    public SpeedC(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        if (packet.isPosition()) {
            PositionProcessor positionProcessor = data.getPositionProcessor();
            boolean onGround = positionProcessor.isOnGround();
            boolean lastOnGround = positionProcessor.isLastOnGround();
            double deltaY = positionProcessor.getDeltaY();
            double deltaXZ = Math.hypot(positionProcessor.getDeltaX(), positionProcessor.getDeltaZ());
            boolean exempt = isExempt(ExemptType.TELEPORT, ExemptType.VELOCITY, ExemptType.SLIME, ExemptType.FLYING, ExemptType.UNDER_BLOCK);

            if (!onGround && lastOnGround && deltaY > 0) {
                // Hump
                isAscending = true;
                double expectedYMotion = getExpectedYMotion(data.getPlayer());

                if (Math.abs(deltaY - expectedYMotion) < EPSILON) {
                    buffer = Math.max(0, buffer - 1);
                } else if (Math.abs(deltaY - lastMaxYMotion) < EPSILON && !exempt) {
                    sameMotionCount++;
                    if (sameMotionCount >= JUMP_THRESHOLD) {
                        if (++buffer > 3) {
                            fail("Invalid Y motion. DY " + deltaY  + " Expected " + expectedYMotion + " Times Repeated " + sameMotionCount);
                            buffer = 0;
                        }
                    }
                } else {
                    sameMotionCount = 0;
                }

                if(deltaY > 0.63 && !isExempt(ExemptType.SLIME)) {
                    fail("Jumped too high: " + deltaY);
                }

                lastMaxYMotion = Math.max(lastMaxYMotion, deltaY);

                debug("Y motion: %.4f, Expected: %.4f, Times Repeated: %d, Buffer: %d", deltaY, expectedYMotion, sameMotionCount, buffer);
            } else if (!onGround && deltaY <= 0 && isAscending) {
                isAscending = false;
                if (checkHorizontalCollision(positionProcessor)) {
                    buffer = 0;
                    sameMotionCount = 0;
                    debug("Collided vertically. Buffer reset.");
                }
            } else if (onGround) {
                if (deltaY == 0) {
                    lastMaxYMotion = 0.0;
                    sameMotionCount = 0;
                    buffer = 0;
                    isAscending = false;
                }
            }
        }
    }

    private boolean checkHorizontalCollision(PositionProcessor positionProcessor) {
        double deltaXZ = Math.hypot(positionProcessor.getDeltaX(), positionProcessor.getDeltaZ());
        return deltaXZ < 0.01;
    }

    private double getExpectedYMotion(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(PotionEffectType.JUMP)) {
                int jumpAmplifier = effect.getAmplifier() + 1;
                if (jumpAmplifier == 1) return 0.5199999883770943;
                if (jumpAmplifier == 2) return 0.6199999898672104;
            }
        }
        return 0.41999998688697815;
    }
}
