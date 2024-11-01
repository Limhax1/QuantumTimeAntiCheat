package com.gladurbad.medusa.check.impl.movement.speed;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.data.processor.PositionProcessor;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@CheckInfo(name = "Speed (C)", description = "Checks for invalid Y motions.", experimental = true, complextype = "InvalidY")
public class SpeedC extends Check {

    private static final ConfigValue max_buffer = new ConfigValue(ConfigValue.ValueType.DOUBLE, "max_buffer");
    private static final ConfigValue buffer_decay = new ConfigValue(ConfigValue.ValueType.DOUBLE, "buffer_decay");
    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");

    private static final int JUMP_THRESHOLD = 2;
    private static final double EPSILON = 1E-13;

    private double lastMaxYMotion = 0.0;
    private int sameMotionCount = 0;
    private double buffer = 0;

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
            boolean exempt = isExempt(ExemptType.TELEPORT, ExemptType.ANYVELOCITY, ExemptType.SLIME,
                    ExemptType.FLYING, ExemptType.UNDER_BLOCK, ExemptType.LIQUID,
                    ExemptType.PISTON, ExemptType.CLIMBABLE, ExemptType.NEAR_VEHICLE,
                    ExemptType.ELYTRA, ExemptType.BUBBLE_COLUMN, ExemptType.HONEY_BLOCK, ExemptType.WEB);

            if (!onGround && lastOnGround && deltaY > 0 && !exempt) {
                double expectedYMotion = getExpectedYMotion(data.getPlayer());
                
                if (deltaY > expectedYMotion && isNearStairOrSlab(data.getPlayer())) {
                    expectedYMotion = deltaY;
                }

                if (Math.abs(deltaY - expectedYMotion) < EPSILON) {
                    buffer = Math.max(0, buffer - buffer_decay.getDouble());
                } else if (Math.abs(deltaY - lastMaxYMotion) < EPSILON) {
                    sameMotionCount++;
                    if (sameMotionCount >= max_buffer.getDouble()) {
                        if(setback.getBoolean()) {
                            setback();
                        }
                        if (++buffer > 2) {
                            fail("Repeated Y motion, DY " + deltaY  + " Expected " + expectedYMotion + " Times Repeated " + sameMotionCount);
                            buffer = 0;
                        }
                    }
                } else {
                    sameMotionCount = 0;
                }

                lastMaxYMotion = Math.max(lastMaxYMotion, deltaY);

                debug("Y motion: %.4f, Expected: %.4f, Count: %d, Buffer: %d", deltaY, expectedYMotion, sameMotionCount, buffer);
                
                if (deltaY > expectedYMotion && !isExempt(ExemptType.SLIME)) {
                    if(setback.getBoolean()) {
                        setback();
                    }
                    if(buffer++ > max_buffer.getDouble()) {
                        fail("Jumped too high: " + deltaY + ", Expected: " + expectedYMotion);
                        buffer = 0;
                    }
                } else if (deltaY < expectedYMotion * 0.99 && !isExempt(ExemptType.SLIME, ExemptType.ANYVELOCITY)) {
                    if(setback.getBoolean()) {
                        setback();
                    }
                    if(buffer++ > max_buffer.getDouble()) {
                        fail("Jumped too low: " + deltaY + ", Expected: " + expectedYMotion);
                        buffer = 0;
                    }
                }
            } else if (onGround) {
                if (deltaY == 0) {
                    lastMaxYMotion = 0.0;
                    sameMotionCount = 0;
                    buffer = Math.max(0, buffer - buffer_decay.getDouble());
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

    private boolean isNearStairOrSlab(Player player) {
        int radius = 1;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = player.getLocation().add(x, y, z).getBlock();
                    Material type = block.getType();
                    if (type.name().contains("STAIRS") || type.name().contains("SLAB") || type.name().contains("STEP")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
