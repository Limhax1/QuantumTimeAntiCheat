package com.gladurbad.medusa.check.impl.movement.fly;

import com.gladurbad.medusa.check.Check;
import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.LinkedList;

@CheckInfo(name = "Fly (B)", description = "Checks for jumping mid-air.")
public final class FlyB extends Check {

    private double lastAcceleration;
    private final LinkedList<Double> verticalMoves = new LinkedList<>();
    private static final int MOVE_HISTORY_SIZE = 20;

    private double lastY;
    private int violations;
    private int consistentUpwardMoveTicks;
    private double lastDeltaY;

    public FlyB(final PlayerData data) {
        super(data);
    }

    @Override
    public void handle(final Packet packet) {
        if (packet.isPosition()) {
            boolean exempt = isExempt(ExemptType.TELEPORT, ExemptType.LIQUID, ExemptType.FLYING, ExemptType.VELOCITY, ExemptType.NEAR_VEHICLE);

            if (!exempt) {
                final Location location = data.getPlayer().getLocation();
                final double y = location.getY();
                double DY = data.getPositionProcessor().getDeltaY();
                final double deltaY = y - lastY;
                final boolean onGround = data.getPositionProcessor().isOnGround();

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

                if(!onGround && !nearClimbable(location)) {
                    double prediction = (lastDeltaY -0.08 ) * 0.9800000190734863;

                    if(!(DY - prediction < 0.2) && lastDeltaY > 0 && DY != 0 && !isExempt(ExemptType.PISTON)) {
                        fail("Gravity prediction " + (DY - prediction));
                    }
                }
                lastDeltaY = DY;
                lastY = y;
            }
        }
    }

    private void checkConstantHeight(double deltaY, int airTicks) {
        if (Math.abs(deltaY) < 0.1 && airTicks > 15) {
            violations++;
            if (violations > 3) {
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
            if (ascendingMoves > MOVE_HISTORY_SIZE * 0.8) {
                violations++;
                if (violations > 2) {
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
