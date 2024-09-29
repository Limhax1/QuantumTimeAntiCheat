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

@CheckInfo(name = "Speed (B)", description = "Checks for Speed.", complextype = "Speed")
public final class SpeedB extends Check {

    private static final ConfigValue max_buffer = new ConfigValue(ConfigValue.ValueType.DOUBLE, "max_buffer");
    private static final ConfigValue buffer_decay = new ConfigValue(ConfigValue.ValueType.DOUBLE, "buffer_decay");
    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");

    private static final double JUMP_BOOST = 0.42;
    private static final double LANDING_LENIENCY = 0.1;

    private static final double WALK_SPEED = 0.221;
    private static final double SPRINT_SPEED = 0.296;
    private static final double AIR_SPEED = 0.36;

    private double buffer = 0.0;
    private boolean lastOnGround = true;
    private boolean lastSprinting = false;

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

            if (!data.getPositionProcessor().isInLiquid() && !data.getPositionProcessor().isOnClimbable()) {
                double expectedSpeed = getExpectedSpeed(onGround, sprinting);

                // Speed effect kezelése
                Player player = data.getPlayer();
                double speedCorrection = getSpeedPotionCorrection(player);
                expectedSpeed *= speedCorrection;

                // Lépcső vagy félblokk közeli ellenőrzés
                if (isNearStairOrSlab(player)) {
                    expectedSpeed += 0.3;
                }

                boolean Exempt = isExempt(ExemptType.TELEPORT, ExemptType.SLIME, ExemptType.UNDER_BLOCK, ExemptType.JOINED, ExemptType.ICE, ExemptType.VELOCITY, ExemptType.FLYING);

                if (!onGround && lastOnGround && Math.abs(deltaY - JUMP_BOOST) < 1E-5) {
                    expectedSpeed *= 1.1;
                } else if (onGround && !lastOnGround) {
                    expectedSpeed += LANDING_LENIENCY;
                }

                if (deltaXZ > expectedSpeed * 1.001 && !Exempt) {
                    buffer += 1;
                } else {
                    buffer = Math.max(buffer - buffer_decay.getDouble(), 0);
                }

                if(expectedSpeed > 0.49 && data.getActionProcessor().isSprinting() && data.getPositionProcessor().isInAir() && getSpeedPotionCorrection(data.getPlayer()) > 1) {
                    expectedSpeed = 0.37;
                }

                if (buffer > max_buffer.getDouble() && !Exempt) {
                    if(setback.getBoolean()) {
                        setback();
                    }
                    fail(String.format("Going too quick - deltaXZ=%.2f, expectedSpeed=%.2f, buffer=%.2f",
                            deltaXZ, expectedSpeed, buffer));
                    buffer = 0;
                }

                debug(String.format("DeltaXZ: %.2f, ExpectedSpeed: %.2f, Buffer: %.2f, DeltaY: %.2f, OnGround: %b, Sprinting: %b, SpeedCorrection: %.2f", 
                        deltaXZ, expectedSpeed, buffer, deltaY, onGround, sprinting, speedCorrection));

                lastOnGround = onGround;
                lastSprinting = sprinting;
            }
        }
    }

    private double getExpectedSpeed(boolean onGround, boolean sprinting) {
        if (onGround) {
            return sprinting ? SPRINT_SPEED : WALK_SPEED;
        } else {
            return AIR_SPEED;
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