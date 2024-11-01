package com.gladurbad.medusa.check.impl.movement.fly;

import com.gladurbad.medusa.check.Check;
import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.LinkedList;

@CheckInfo(name = "Fly (B)", description = "Checks for jumping mid-air.", complextype = "Prediction")
public final class FlyB extends Check {

    private static final ConfigValue max_buffer = new ConfigValue(ConfigValue.ValueType.DOUBLE, "max_buffer");
    private static final ConfigValue buffer_decay = new ConfigValue(ConfigValue.ValueType.DOUBLE, "buffer_decay");
    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");

    private double lastAcceleration;
    private final LinkedList<Double> verticalMoves = new LinkedList<>();
    private static final int MOVE_HISTORY_SIZE = 20;
    double BUFFER;
    private double lastY;
    private int violations;
    private int consistentUpwardMoveTicks;
    private double lastDeltaY;
    private int ignoreTicks = 0;

    public FlyB(final PlayerData data) {
        super(data);
    }

    @Override
    public void handle(final Packet packet) {
        boolean exempt = isExempt(ExemptType.TELEPORT, ExemptType.LIQUID, ExemptType.FLYING,
                ExemptType.ANYVELOCITY, ExemptType.NEAR_VEHICLE, ExemptType.PISTON,
                ExemptType.ELYTRA, ExemptType.LEVITATION, ExemptType.POWDER_SNOW,
                ExemptType.BUBBLE_COLUMN, ExemptType.SLOW_FALLING, ExemptType.HONEY_BLOCK);

        if (packet.isPosition()) {

            if (!exempt) {
                final Location location = data.getPlayer().getLocation();
                final double y = location.getY();
                double DY = data.getPositionProcessor().getDeltaY();
                final double deltaY = y - lastY;
                final boolean onGround = data.getPositionProcessor().isOnGround();

                if (DY == 0 && lastDeltaY != 0) {
                    ignoreTicks = 1;
                    debug("Right-click detected, ignoring next tick");
                }

                if (ignoreTicks > 0) {
                    ignoreTicks--;
                    debug("Ignoring tick, remaining: " + ignoreTicks);
                    lastY = y;
                    lastDeltaY = DY;
                    return;
                }

                int airTicks = data.getPositionProcessor().getAirTicks();
                int groundTicks = data.getPositionProcessor().getGroundTicks();

                verticalMoves.addLast(deltaY);
                if (verticalMoves.size() > MOVE_HISTORY_SIZE) {
                    verticalMoves.removeFirst();
                }

                if (!nearClimbable(location)) {
                    checkConstantHeight(DY, airTicks);
                    checkAscension(deltaY);
                }

                if (onGround && violations > 0) {
                    violations--;
                }

                lastDeltaY = DY;
                lastY = y;
            }
        }
    }

    private void checkConstantHeight(double deltaY, int airTicks) {
        if (Math.abs(deltaY) < 0.1 && airTicks > 15 && !isExempt(ExemptType.TELEPORT, ExemptType.LIQUID, ExemptType.FLYING, ExemptType.ANYVELOCITY, ExemptType.NEAR_VEHICLE)) {
            violations++;
            if (violations > 6) {
                if(setback.getBoolean()) {
                    setback();
                }
                fail("Constant height in air for " + airTicks + " ticks " + Math.abs(deltaY));
            }
        }
    }

    private void checkAscension(double deltaY) {
        if (verticalMoves.size() == MOVE_HISTORY_SIZE) {
            int ascendingMoves = 0;
            for (double move : verticalMoves) {
                if (move > 0) {
                    ascendingMoves++;
                }
            }
            if (ascendingMoves > MOVE_HISTORY_SIZE * 0.8 && !isExempt(ExemptType.TELEPORT, ExemptType.LIQUID, ExemptType.FLYING, ExemptType.ANYVELOCITY, ExemptType.NEAR_VEHICLE)) {
                violations++;
                if (violations > 1) {
                    if(setback.getBoolean()) {
                        setback();
                    }
                    fail("Continuous ascension for " + ascendingMoves + " out of " + MOVE_HISTORY_SIZE + " moves");
                }
            }
        }
    }

    private boolean nearClimbable(Location location) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block block = location.getBlock().getRelative(x, y, z);
                    Material type = block.getType();
                    if (type == Material.LADDER || type == Material.VINE) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
