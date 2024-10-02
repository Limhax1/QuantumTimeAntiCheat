package com.gladurbad.medusa.check.impl.movement.speed;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

@CheckInfo(name = "Speed (B)", description = "Checks for Speed.", complextype = "Speed")
public final class SpeedB extends Check {

    private static final ConfigValue max_buffer = new ConfigValue(ConfigValue.ValueType.DOUBLE, "max_buffer");
    private static final ConfigValue buffer_decay = new ConfigValue(ConfigValue.ValueType.DOUBLE, "buffer_decay");
    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");

    private static final double JUMP_BOOST = 0.42;
    private static final double WALK_SPEED = 0.221;
    private static final double SPRINT_SPEED = 0.2865;
    private static final double AIR_SPEED = 0.36;
    private static final double POST_JUMP_BOOST = 0.31;

    private static final double VELOCITY_DECAY = 0.99;

    private static final int VELOCITY_THRESHOLD = 15;
    private double buffer = 0.0;
    private boolean lastOnGround = true;
    private boolean lastSprinting = false;
    private int ticksSinceJump = 0;
    private double lastVelocityBoost = 0.0;
    private int lastTicksSinceVelocity = 999;

    public SpeedB(final PlayerData data) {
        super(data);
    }

    @Override
    public void handle(final Packet packet) {
        if (packet.isPosition()) {
            final double deltaY = data.getPositionProcessor().getDeltaY();
            final double deltaXZ = data.getPositionProcessor().getDeltaXZ();
            final boolean onGround = data.getPositionProcessor().isOnGround();
            final boolean sprinting = data.getActionProcessor().isSprinting();

            int ticksSinceVelocity = data.getVelocityProcessor().getTicksSinceVelocity();

            updateVelocityState(ticksSinceVelocity);

            double expectedSpeed = predictMaxSpeed(onGround, sprinting, deltaY);

            boolean exempt = isExempt(ExemptType.TELEPORT, ExemptType.SLIME, ExemptType.UNDER_BLOCK, ExemptType.JOINED, ExemptType.ICE, ExemptType.FLYING);

            if(isNearStairOrSlab(data.getPlayer())) {
                expectedSpeed *= 1.6;
            }

            if (deltaXZ > expectedSpeed * 1.001 && !exempt) {
                buffer += 1;
            } else {
                buffer = Math.max(buffer - buffer_decay.getDouble(), 0);
            }

            if (buffer > max_buffer.getDouble() && !exempt) {
                if(setback.getBoolean()) {
                    setback();
                }
                fail(String.format("Going too quick - deltaXZ=%.4f, expectedSpeed=%.4f, buffer=%.2f",
                        deltaXZ, expectedSpeed, buffer));
                buffer = 0;
            }

            debug(String.format("VB: %.4f, TSV: %d", lastVelocityBoost, ticksSinceVelocity));

            lastTicksSinceVelocity = ticksSinceVelocity;

            updateJumpTicks(onGround, deltaY);
            lastOnGround = onGround;
            lastSprinting = sprinting;
        }
    }

    private void updateVelocityState(int ticksSinceVelocity) {
        if (ticksSinceVelocity < lastTicksSinceVelocity) {
            lastVelocityBoost = getVelocityBoost();
        } else if (ticksSinceVelocity < VELOCITY_THRESHOLD) {
            lastVelocityBoost *= VELOCITY_DECAY;
        } else {
            lastVelocityBoost = 0;
        }
    }

    private double predictMaxSpeed(boolean onGround, boolean sprinting, double deltaY) {
        double baseSpeed = getBaseSpeed(onGround, sprinting);
        double speedMultiplier = getSpeedPotionMultiplier(data.getPlayer());
        double jumpBoost = getJumpBoost(onGround, deltaY);

        return (baseSpeed * speedMultiplier) + jumpBoost + lastVelocityBoost;
    }

    private double getBaseSpeed(boolean onGround, boolean sprinting) {
        if (onGround) {
            return sprinting ? SPRINT_SPEED : WALK_SPEED;
        } else {
            return AIR_SPEED;
        }
    }

    private double getSpeedPotionMultiplier(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(PotionEffectType.SPEED)) {
                int speedAmplifier = effect.getAmplifier() + 1;
                return 1.0 + (0.19 * speedAmplifier);
            }
        }
        return 1.0;
    }

    private double getVelocityBoost() {
        double velocityX = data.getVelocityProcessor().getVelocityX();
        double velocityZ = data.getVelocityProcessor().getVelocityZ();
        return Math.hypot(velocityX, velocityZ);
    }

    private double getJumpBoost(boolean onGround, double deltaY) {
        if (!onGround && lastOnGround && Math.abs(deltaY - JUMP_BOOST) < 1E-5) {
            ticksSinceJump = 0;
            return 0.1;
        } else if (ticksSinceJump < 2) {
            return POST_JUMP_BOOST;
        }
        return 0;
    }

    private void updateJumpTicks(boolean onGround, double deltaY) {
        if (!onGround && lastOnGround && Math.abs(deltaY - JUMP_BOOST) < 1E-5) {
            ticksSinceJump = 0;
        } else if (!onGround) {
            ticksSinceJump++;
        } else {
            ticksSinceJump = 999; // Reset when on ground
        }
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