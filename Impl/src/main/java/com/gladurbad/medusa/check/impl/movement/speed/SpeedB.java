package com.gladurbad.medusa.check.impl.movement.speed;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import com.gladurbad.medusa.util.MathUtil;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@CheckInfo(name = "Speed (B)", description = "Checks for speed.", experimental = true, complextype = "Ground")
public class SpeedB extends Check {

    private static final ConfigValue max_buffer = new ConfigValue(ConfigValue.ValueType.DOUBLE, "max_buffer");
    private static final ConfigValue buffer_decay = new ConfigValue(ConfigValue.ValueType.DOUBLE, "buffer_decay");
    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");

    private static final double BASE_GROUND_SPEED = 0.2873;
    private static final double ICE_BOOST = 0.3;
    private static final double BLOCK_BOOST = 0.3;
    private static final double VELOCITY_BOOST_BASE = 0.05;
    private static final double VELOCITY_BOOST_MULTIPLIER = 2.0;
    private static final int VELOCITY_TICKS_MAX = 35;

    private double lastValidDeltaXZ = 0.0;
    private int ignoreTicks = 0;

    public SpeedB(final PlayerData data) {
        super(data);
    }

    @Override
    public void handle(final Packet packet) {
        if (packet.isPosition()) {
            final Player player = data.getPlayer();
            final boolean onGround = data.getPositionProcessor().isOnGround();
            final double deltaXZ = data.getPositionProcessor().getDeltaXZ();
            final double lastDeltaXZ = data.getPositionProcessor().getLastDeltaXZ();
            final int groundTicks = data.getPositionProcessor().getGroundTicks();
            final int sinceIceTicks = data.getPositionProcessor().getSinceIceTicks();
            final int sinceSlimeTicks = data.getPositionProcessor().getSinceSlimeTicks();
            final int sinceVelocityTicks = data.getVelocityProcessor().getTicksSinceVelocity();
            boolean exempt = isExempt(
                    ExemptType.BUBBLE_COLUMN,
                    ExemptType.ELYTRA, ExemptType.TELEPORT,
                    ExemptType.FLYING
            );
            double maxSpeed = BASE_GROUND_SPEED;

            double speedEffectMultiplier = getSpeedPotionCorrection(player);
            maxSpeed *= speedEffectMultiplier;

            if(isExempt(ExemptType.UNDER_BLOCK)) {
                maxSpeed += 0.3;
            }

            if (sinceIceTicks < 40) {
                maxSpeed += ICE_BOOST;
            }

            if (sinceSlimeTicks < 40) {
                maxSpeed += BLOCK_BOOST;
            }

            if (data.getPositionProcessor().isNearStairs() || data.getPositionProcessor().isNearSlab()) {
                maxSpeed += BLOCK_BOOST;
            }

            if (sinceVelocityTicks < VELOCITY_TICKS_MAX) {
                double velocityX = data.getVelocityProcessor().getVelocityX();
                double velocityZ = data.getVelocityProcessor().getVelocityZ();
                double velocityXZ = MathUtil.hypot(velocityX, velocityZ);
                double velocityBoost = VELOCITY_BOOST_BASE + (velocityXZ * VELOCITY_BOOST_MULTIPLIER);
                maxSpeed += velocityBoost * (1 - (sinceVelocityTicks / (double) VELOCITY_TICKS_MAX));
            }

            if (deltaXZ > lastDeltaXZ) {
                maxSpeed += 0.01 + (groundTicks * 0.0005);
            } else {
                maxSpeed += 0.08;
            }

            if (groundTicks == 1 && data.getPositionProcessor().getDeltaY() > 0) {
                maxSpeed += 0.1;
            }

            if (deltaXZ == 0 && lastValidDeltaXZ > 0) {
                ignoreTicks = 2;
                debug("Right-click detected, ignoring next 2 ticks");
            }

            if (ignoreTicks > 0) {
                ignoreTicks--;
                debug("Ignoring tick, remaining: " + ignoreTicks);
                return;
            }

            if (deltaXZ > 0) {
                lastValidDeltaXZ = deltaXZ;
            }

            if (deltaXZ > maxSpeed && !exempt) {

                if(buffer++ > max_buffer.getDouble() / 2 && setback.getBoolean()) {
                    setback();
                }

                if (buffer++ > max_buffer.getDouble()) {
                    fail(String.format("(%.2f > %.2f)", deltaXZ, maxSpeed));
                    buffer = 0;
                }
            } else {
                buffer = Math.max(buffer - buffer_decay.getDouble(), 0);
            }

            debug(String.format("deltaXZ=%.4f, maxSpeed=%.4f, speedEffect=%.2f, groundTicks=%d, buffer=%.2f, ignoreTicks=%d",
                    deltaXZ, maxSpeed, speedEffectMultiplier, groundTicks, buffer, ignoreTicks));
        }
    }

    private double getSpeedPotionCorrection(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(PotionEffectType.SPEED)) {
                int speedAmplifier = effect.getAmplifier() + 1;
                return 1.0 + (0.2 * speedAmplifier);
            }
        }
        return 1.0;
    }
}