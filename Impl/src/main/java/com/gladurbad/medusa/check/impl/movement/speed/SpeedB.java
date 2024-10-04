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

@CheckInfo(name = "Speed (B)", description = "Ellenőrzi a játékos sebességét.", complextype = "Speed")
public final class SpeedB extends Check {

    private static final ConfigValue max_buffer = new ConfigValue(ConfigValue.ValueType.DOUBLE, "max_buffer");
    private static final ConfigValue buffer_decay = new ConfigValue(ConfigValue.ValueType.DOUBLE, "buffer_decay");
    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");

    private static final double WALK_SPEED = 0.221;
    private static final double SPRINT_SPEED = 0.2865;
    private static final double AIR_SPEED = 0.36;
    private static final double JUMP_BOOST = 0.42;
    private static final double AIR_FRICTION = 0.98;
    private static final double GROUND_FRICTION = 0.91;
    private static final double MOMENTUM_MULTIPLIER = 1.03;
    private static final double MOMENTUM_DECAY = 0.07;
    private static final double MAX_MOMENTUM = 0.08;
    private static final double MAX_SPEED = 0.48;
    private static final double MAX_ACCELERATION = 0.06;
    private static final double ACCELERATION = 0.1;
    private static final double TOLERANCE = 1.0003;
    private static final double BUFFER_INCREASE_MULTIPLIER = 4.0;
    private static final double BUFFER_DECAY_MULTIPLIER = 0.95;

    private double lastDeltaXZ = 0.0;
    private double momentum = 0.0;
    private boolean wasOnGround = true;
    private boolean wasSprinting = false;
    private int airTicks = 0;
    private double lastVelocityBoost = 0.0;
    private double lastExpectedSpeed = 0.0;
    private int consecutiveJumps = 0;
    private long lastJumpTime = 0;

    public SpeedB(final PlayerData data) {
        super(data);
    }

    @Override
    public void handle(final Packet packet) {
        if (packet.isPosition()) {
            final double deltaXZ = data.getPositionProcessor().getDeltaXZ();
            final double deltaY = data.getPositionProcessor().getDeltaY();
            final boolean onGround = data.getPositionProcessor().isOnGround();
            final boolean sprinting = data.getActionProcessor().isSprinting();

            double velocityBoost = getVelocityBoost();
            lastVelocityBoost = Math.max(lastVelocityBoost, velocityBoost);

            double expectedSpeed = predictMaxSpeed(onGround, sprinting, deltaY, deltaXZ);
            double acceleration = deltaXZ - lastDeltaXZ;

            boolean exempt = isExempt(ExemptType.TELEPORT, ExemptType.VELOCITY, ExemptType.FLYING);

            if(isNearStairOrSlab(data.getPlayer())) {
                expectedSpeed *= 1.25;
            }

            // Kezeljük az egymás utáni ugrásokat
            if (!onGround && wasOnGround && Math.abs(deltaY - JUMP_BOOST) < 1E-5) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastJumpTime < 500) { // 500 ms-on belüli ugrások
                    consecutiveJumps++;
                    expectedSpeed *= (1 + (consecutiveJumps * 0.02)); // Növeljük az elvárt sebességet
                } else {
                    consecutiveJumps = 0;
                }
                lastJumpTime = currentTime;
            }

            if (Math.abs(acceleration) > MAX_ACCELERATION && !exempt) {
                buffer += Math.abs(acceleration) * BUFFER_INCREASE_MULTIPLIER;
            }

            if (deltaXZ > expectedSpeed * TOLERANCE && !exempt) {
                buffer += (deltaXZ - expectedSpeed) * BUFFER_INCREASE_MULTIPLIER;
            } else {
                buffer *= BUFFER_DECAY_MULTIPLIER;
            }

            if (deltaXZ > lastExpectedSpeed * TOLERANCE && deltaXZ > expectedSpeed * TOLERANCE && !exempt) {
                buffer += (deltaXZ - expectedSpeed) * BUFFER_INCREASE_MULTIPLIER;
            }

            buffer = Math.max(buffer - buffer_decay.getDouble(), 0);

            if (buffer > max_buffer.getDouble() && !exempt) {
                if(setback.getBoolean()) {
                    setback();
                }
                fail(String.format("Túl gyors mozgás - deltaXZ=%.4f, expectedSpeed=%.4f, buffer=%.2f, momentum=%.4f, acc=%.4f, consJumps=%d",
                        deltaXZ, expectedSpeed, buffer, momentum, acceleration, consecutiveJumps));
                buffer = 0;
                consecutiveJumps = 0;
            }

            String debugInfo = String.format(
                "DXZ: %.4f, EXP: %.4f, BUF: %.2f, AT: %d, OG: %b, SPR: %b, DY: %.4f, MOM: %.4f, ACC: %.4f, VB: %.4f, CJ: %d",
                deltaXZ, expectedSpeed, buffer, airTicks, onGround, sprinting, deltaY, momentum, acceleration, lastVelocityBoost, consecutiveJumps
            );
            debug(debugInfo);

            lastDeltaXZ = deltaXZ;
            lastExpectedSpeed = expectedSpeed;
            wasOnGround = onGround;
            wasSprinting = sprinting;
            lastVelocityBoost *= 0.99;
        }
    }

    private double predictMaxSpeed(boolean onGround, boolean sprinting, double deltaY, double deltaXZ) {
        double baseSpeed = getBaseSpeed(onGround, sprinting);
        double speedMultiplier = getSpeedPotionMultiplier(data.getPlayer());
        double jumpBoost = getJumpBoost(onGround, deltaY);

        updateMomentum(onGround, sprinting);

        double expectedSpeed = (baseSpeed * speedMultiplier) + jumpBoost + lastVelocityBoost + momentum;

        if (!onGround) {
            airTicks++;
            expectedSpeed *= Math.pow(AIR_FRICTION, Math.min(airTicks, 10)); // Korlátozzuk a légellenállás hatását
        } else {
            airTicks = 0;
            expectedSpeed *= GROUND_FRICTION;
        }

        expectedSpeed += ACCELERATION;
        expectedSpeed = Math.min(expectedSpeed, MAX_SPEED + momentum + lastVelocityBoost);

        return expectedSpeed;
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
                int amplifier = effect.getAmplifier() + 1;
                return 1.0 + (0.2 * amplifier);
            }
        }
        return 1.0;
    }

    private double getJumpBoost(boolean onGround, double deltaY) {
        if (!onGround && wasOnGround && Math.abs(deltaY - JUMP_BOOST) < 1E-5) {
            return JUMP_BOOST;
        }
        return 0;
    }

    private double getVelocityBoost() {
        double velocityX = data.getVelocityProcessor().getVelocityX();
        double velocityZ = data.getVelocityProcessor().getVelocityZ();
        int ticksSinceVelocity = data.getVelocityProcessor().getTicksSinceVelocity();
        
        double velocityXZ = Math.hypot(velocityX, velocityZ);
        
        // A velocity hatása csökken az idő múlásával
        double decayFactor = Math.max(0, 1 - (ticksSinceVelocity / 20.0)); // 1 másodperc után teljesen lecseng
        
        return velocityXZ * decayFactor;
    }

    private void updateMomentum(boolean onGround, boolean sprinting) {
        if (sprinting && onGround) {
            momentum = Math.min(momentum + (MOMENTUM_MULTIPLIER - 1), MAX_MOMENTUM);
        } else {
            momentum = Math.max(momentum - MOMENTUM_DECAY, 0);
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