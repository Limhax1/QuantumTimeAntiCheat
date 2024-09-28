package com.gladurbad.medusa.check.impl.movement.jesus;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

@CheckInfo(name = "Jesus (A)", description = "Checks for Jesus hacks", experimental = true)
public class JesusA extends Check {

    private static final ConfigValue max_buffer = new ConfigValue(ConfigValue.ValueType.DOUBLE, "max_buffer");
    private static final ConfigValue buffer_decay = new ConfigValue(ConfigValue.ValueType.DOUBLE, "buffer_decay");
    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");

    public JesusA(final PlayerData data) {
        super(data);
    }
    private double speedBuffer;
    private double verticalBuffer;
    private static final double MAX_BUFFER = 2;
    private static final double BUFFER_DECREMENT = 0.25;
    private static final double MAX_WATER_JUMP = 0.6;
    private static final double MIN_WATER_FALL = -0.1;
    private static final double MAX_WATER_SPEED = 0.24;

    @Override
    public void handle(Packet packet) {
        if (packet.isPosition() || packet.isPosLook()) {
            Location location = data.getPlayer().getLocation();
            boolean posAir = location.getBlock().isEmpty();
            boolean onLiquid = location.clone().subtract(0, 0.5, 0).getBlock().isLiquid();

            if (onLiquid && posAir && !isNearSolidBlock(location)) {
                double deltaY = data.getPositionProcessor().getDeltaY();
                double deltaXZ = data.getPositionProcessor().getDeltaXZ();

                if (deltaY == 0 || deltaY == 0.33319999363422426 || 
                    (deltaY > 0 && deltaY <= MAX_WATER_JUMP) || 
                    (deltaY < 0 && deltaY >= MIN_WATER_FALL)) {
                    if ((verticalBuffer += 1) > MAX_BUFFER) {
                        if(setback.getBoolean()) {
                            setback();
                        }
                        fail("Suspicious vertical movement over water: " + deltaY);
                        verticalBuffer = 0;
                    }
                } else {
                    verticalBuffer = Math.max(0, verticalBuffer - BUFFER_DECREMENT);
                }

                if (deltaXZ > MAX_WATER_SPEED) {
                    if ((speedBuffer += 1) > max_buffer.getDouble()) {
                        if(setback.getBoolean()) {
                            setback();
                        }
                        fail("Suspicious horizontal movement over water: " + deltaXZ);
                        speedBuffer = 0;
                    }
                } else {
                    speedBuffer = Math.max(0, speedBuffer - buffer_decay.getDouble());
                }
            } else {
                verticalBuffer = Math.max(0, verticalBuffer - buffer_decay.getDouble());
                speedBuffer = Math.max(0, speedBuffer - buffer_decay.getDouble());
            }

            debug("DY: " + data.getPositionProcessor().getDeltaY() + 
                  " DXZ: " + data.getPositionProcessor().getDeltaXZ() + 
                  " VerticalBuffer: " + verticalBuffer +
                  " SpeedBuffer: " + speedBuffer);
        }
    }

    private boolean isNearSolidBlock(Location location) {
        for (double x = -0.3; x <= 0.3; x += 0.3) {
            for (double z = -0.3; z <= 0.3; z += 0.3) {
                Block block = location.clone().add(x, -0.1, z).getBlock();
                if (block.getType().isSolid()) {
                    return true;
                }
            }
        }
        return false;
    }
}

/*
    private boolean isOverWater(Location location) {
        boolean foundWater = false;
        boolean onlyAirAndWater = true;

        for (int i = 0; i < WATER_CHECK_DEPTH; i++) {
            Block block = location.clone().subtract(0, i, 0).getBlock();
            Material type = block.getType();

            if (type == Material.WATER || type == Material.STATIONARY_WATER) {
                foundWater = true;
            } else if (type != Material.AIR) {
                onlyAirAndWater = false;
                break;
            }
        }

        return foundWater && onlyAirAndWater;
    }
 */

/*
package com.gladurbad.medusa.check.impl.movement.jesus;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

@CheckInfo(name = "Jesus (B)", description = "Checks for abnormal jumps and speed over water", experimental = true)
public class JesusB extends Check {

    private static double MAX_JUMP_HEIGHT = 0.12;
    private static final double MAX_WATER_SPEED = 0.26;
    private static final int WATER_CHECK_DEPTH = 4;
    private double speedBuffer;
    private double verticalBuffer;
    private static final double MAX_BUFFER = 2;
    private static final double BUFFER_DECREMENT = 0.25;
    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");

    public JesusB(final PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        if (packet.isFlying()) {
            final Location location = data.getPlayer().getLocation();
            final double deltaY = data.getPositionProcessor().getDeltaY();
            final double deltaXZ = data.getPositionProcessor().getDeltaXZ();

            if(isNearSolidBlock(location)) {
                MAX_JUMP_HEIGHT = 0.42;
            } else {
                MAX_JUMP_HEIGHT = 0.12;
            }

            if (isOverWater(location) && !isNearSolidBlock(location)) {
                if (deltaY > MAX_JUMP_HEIGHT || deltaY < -0.7 || deltaY == 0.41999998688697815 && data.getPlayer().getFallDistance() <= 4 ) {
                    if ((verticalBuffer += 1) > MAX_BUFFER) {
                        if(setback.getBoolean()) {
                            setback();
                        }
                        fail("Abnormal vertical movement over liquids: DY=" + deltaY);
                        verticalBuffer = 0;
                    }
                } else {
                    verticalBuffer = Math.max(0, verticalBuffer - BUFFER_DECREMENT);
                }

                if (deltaXZ > MAX_WATER_SPEED) {
                    if ((speedBuffer += 1) > MAX_BUFFER) {
                        if(setback.getBoolean()) {
                            setback();
                        }
                        fail("Abnormal horizontal movement over liquids: DXZ=" + deltaXZ);
                        speedBuffer = 0;
                    }
                } else {
                    speedBuffer = Math.max(0, speedBuffer - BUFFER_DECREMENT);
                }
            } else {
                verticalBuffer = Math.max(0, verticalBuffer - BUFFER_DECREMENT);
                speedBuffer = Math.max(0, speedBuffer - BUFFER_DECREMENT);
            }

            debug("DY: " + deltaY + " DXZ: " + deltaXZ +
                    " VerticalBuffer: " + verticalBuffer +
                    " SpeedBuffer: " + speedBuffer);
        }
    }

    private boolean isOverWater(Location location) {
        boolean foundWater = false;
        boolean onlyAirAndWater = true;

        for (int i = 0; i < WATER_CHECK_DEPTH; i++) {
            Block block = location.clone().subtract(0, i, 0).getBlock();
            Material type = block.getType();

            if (type == Material.WATER || type == Material.STATIONARY_WATER) {
                foundWater = true;
            } else if (type != Material.AIR) {
                onlyAirAndWater = false;
                break;
            }
        }

        return foundWater && onlyAirAndWater;
    }

    private boolean isNearSolidBlock(Location location) {
        for (double x = -0.3; x <= 0.3; x += 0.3) {
            for (double z = -0.3; z <= 0.3; z += 0.3) {
                Block block = location.clone().add(x, -0.1, z).getBlock();
                if (block.getType().isSolid()) {
                    return true;
                }
            }
        }
        return false;
    }
}
 */